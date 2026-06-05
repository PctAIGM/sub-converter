package com.subconverter.domain

import org.yaml.snakeyaml.DumperOptions
import org.yaml.snakeyaml.Yaml
import java.util.LinkedHashMap

class MihomoYamlService {
    private val yaml = Yaml(
        DumperOptions().apply {
            defaultFlowStyle = DumperOptions.FlowStyle.BLOCK
            isPrettyFlow = true
            indent = 2
            indicatorIndent = 0
        },
    )

    fun extractProxies(yamlBody: String): List<LinkedHashMap<String, Any?>> {
        val root = loadMap(yamlBody)
        val proxies = (root["proxies"] as? List<*>).orEmpty().mapNotNull { item ->
            (item as? Map<*, *>)?.let(::copyMap)
        }
        if (proxies.isNotEmpty()) return proxies

        return ShareLinkParser.parseToMihomoProxies(yamlBody)
    }

    fun transformProxies(
        proxies: List<LinkedHashMap<String, Any?>>,
        rules: TransformRules,
    ): List<LinkedHashMap<String, Any?>> =
        proxies.mapNotNull { proxy ->
            val originalName = proxy["name"]?.toString()?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            if (!matches(originalName, rules)) return@mapNotNull null

            LinkedHashMap(proxy).apply {
                this["name"] = rules.prefix + originalName
            }
        }

    fun renderTemplate(
        templateYaml: String,
        proxies: List<LinkedHashMap<String, Any?>>,
        overrideYamls: List<String> = emptyList(),
    ): String {
        val root = loadMap(templateYaml).ifEmpty { LinkedHashMap() }
        val uniqueProxies = makeNamesUnique(proxies)
        val proxyNames = uniqueProxies.mapNotNull { it["name"]?.toString() }

        val expanded = replacePlaceholders(root, proxyNames)
        val renderedRoot = if (expanded is MutableMap<*, *>) {
            @Suppress("UNCHECKED_CAST")
            expanded as MutableMap<String, Any?>
        } else {
            LinkedHashMap()
        }
        renderedRoot["proxies"] = uniqueProxies
        overrideYamls.forEach { overrideYaml ->
            val patch = parseOverrideMap(overrideYaml)
            val expandedPatch = replacePlaceholders(patch, proxyNames)
            if (expandedPatch is Map<*, *>) {
                deepMerge(renderedRoot, expandedPatch)
            }
        }
        return yaml.dump(renderedRoot)
    }

    fun validateOverrideYaml(yamlBody: String): String? {
        if (yamlBody.isBlank()) return null
        return runCatching {
            parseOverrideMap(yamlBody)
            null
        }.getOrElse { throwable ->
            throwable.message ?: "覆写 YAML 解析失败"
        }
    }

    private fun loadMap(yamlBody: String): LinkedHashMap<String, Any?> {
        val loaded = runCatching { yaml.load<Any?>(yamlBody) }.getOrNull()
        return (loaded as? Map<*, *>)?.let(::copyMap) ?: LinkedHashMap()
    }

    private fun parseOverrideMap(yamlBody: String): LinkedHashMap<String, Any?> {
        if (yamlBody.isBlank()) return LinkedHashMap()
        val loaded = yaml.load<Any?>(yamlBody) ?: return LinkedHashMap()
        return (loaded as? Map<*, *>)?.let(::copyMap)
            ?: throw IllegalArgumentException("覆写 YAML 必须是对象")
    }

    private fun matches(name: String, rules: TransformRules): Boolean {
        val included = rules.includeRegex.isBlank() ||
            runCatching { Regex(rules.includeRegex).containsMatchIn(name) }.getOrDefault(false)
        val excluded = rules.excludeRegex.isNotBlank() &&
            runCatching { Regex(rules.excludeRegex).containsMatchIn(name) }.getOrDefault(false)
        return included && !excluded
    }

    private fun makeNamesUnique(
        proxies: List<LinkedHashMap<String, Any?>>,
    ): List<LinkedHashMap<String, Any?>> {
        val seen = mutableMapOf<String, Int>()
        return proxies.map { proxy ->
            val name = proxy["name"]?.toString().orEmpty()
            val count = seen.getOrDefault(name, 0) + 1
            seen[name] = count

            if (count == 1) {
                LinkedHashMap(proxy)
            } else {
                LinkedHashMap(proxy).apply { this["name"] = "$name ($count)" }
            }
        }
    }

    private fun replacePlaceholders(value: Any?, proxyNames: List<String>): Any? =
        when (value) {
            is Map<*, *> -> LinkedHashMap<String, Any?>().apply {
                value.forEach { (key, child) ->
                    this[key.toString()] = replacePlaceholders(child, proxyNames)
                }
            }

            is List<*> -> value.map { replacePlaceholders(it, proxyNames) }
            "{{proxy_names}}" -> proxyNames
            "{{proxy_names_csv}}" -> proxyNames.joinToString(",")
            is String -> value
                .replace("{{proxy_names_csv}}", proxyNames.joinToString(","))
            else -> value
        }

    private fun deepMerge(target: MutableMap<String, Any?>, patch: Map<*, *>) {
        patch.forEach { (rawKeyValue, patchValue) ->
            val rawKey = rawKeyValue.toString()
            if (rawKey.endsWith("!")) {
                target[trimWrap(rawKey.dropLast(1))] = copyValue(patchValue)
                return@forEach
            }

            when (patchValue) {
                is Map<*, *> -> {
                    val key = trimWrap(rawKey)
                    val current = target[key]
                    val targetChild = if (current is MutableMap<*, *>) {
                        @Suppress("UNCHECKED_CAST")
                        current as MutableMap<String, Any?>
                    } else {
                        LinkedHashMap<String, Any?>().also { target[key] = it }
                    }
                    deepMerge(targetChild, patchValue)
                }

                is List<*> -> {
                    val patchList = patchValue.map(::copyValue)
                    when {
                        rawKey.startsWith("+") -> {
                            val key = trimWrap(rawKey.drop(1))
                            val current = target[key] as? List<*> ?: emptyList<Any?>()
                            target[key] = patchList + current.map(::copyValue)
                        }

                        rawKey.endsWith("+") -> {
                            val key = trimWrap(rawKey.dropLast(1))
                            val current = target[key] as? List<*> ?: emptyList<Any?>()
                            target[key] = current.map(::copyValue) + patchList
                        }

                        else -> {
                            target[trimWrap(rawKey)] = patchList
                        }
                    }
                }

                else -> {
                    target[trimWrap(rawKey)] = copyValue(patchValue)
                }
            }
        }
    }

    private fun trimWrap(key: String): String =
        if (key.startsWith("<") && key.endsWith(">")) key.substring(1, key.lastIndex) else key

    private fun copyValue(value: Any?): Any? =
        when (value) {
            is Map<*, *> -> copyMap(value)
            is List<*> -> value.map(::copyValue)
            else -> value
        }

    private fun copyMap(map: Map<*, *>): LinkedHashMap<String, Any?> =
        LinkedHashMap<String, Any?>().apply {
            map.forEach { (key, value) ->
                this[key.toString()] = copyValue(value)
            }
        }
}
