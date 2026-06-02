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
        return yaml.dump(renderedRoot)
    }

    private fun loadMap(yamlBody: String): LinkedHashMap<String, Any?> {
        val loaded = runCatching { yaml.load<Any?>(yamlBody) }.getOrNull()
        return (loaded as? Map<*, *>)?.let(::copyMap) ?: LinkedHashMap()
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

    private fun copyMap(map: Map<*, *>): LinkedHashMap<String, Any?> =
        LinkedHashMap<String, Any?>().apply {
            map.forEach { (key, value) ->
                this[key.toString()] = when (value) {
                    is Map<*, *> -> copyMap(value)
                    is List<*> -> value.map { child ->
                        when (child) {
                            is Map<*, *> -> copyMap(child)
                            else -> child
                        }
                    }

                    else -> value
                }
            }
        }
}
