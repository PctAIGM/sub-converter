package com.subconverter.domain

import com.quickjs.JSObject
import com.quickjs.QuickJS
import com.quickjs.QuickJSException
import org.json.JSONArray
import org.json.JSONObject

class JsOverrideService {
    fun execute(script: String, config: Map<String, Any?>): Map<String, Any?> {
        val runtime = QuickJS.createRuntime()
        val context = runtime.createContext()
        try {
            context.executeVoidScript(script, "override.js")
            val jsConfig = JSObject(context, jsonObjectFrom(config))
            context.set("__config", jsConfig)
            val result = runCatching {
                context.executeObjectScript("main(__config)", "invoke.js")
            }.getOrElse { throw JsOverrideException(it) }
            val configObject = result as? JSObject
                ?: throw JsOverrideException(
                    IllegalArgumentException("main 必须返回对象，实际返回 ${describe(result)}"),
                )
            val converted = fromJson(configObject.toJSONObject())
            @Suppress("UNCHECKED_CAST")
            return converted as? Map<String, Any?>
                ?: throw JsOverrideException(IllegalArgumentException("main 必须返回对象"))
        } catch (e: QuickJSException) {
            throw JsOverrideException(e)
        } finally {
            context.close()
            runtime.close()
        }
    }

    fun validate(script: String): String? {
        if (script.isBlank()) return null
        val runtime = QuickJS.createRuntime()
        val context = runtime.createContext()
        return try {
            runCatching { context.executeVoidScript(script, "override.js") }
                .getOrElse { return it.message ?: "JavaScript 解析失败" }
            val hasMain = runCatching {
                context.executeBooleanScript("typeof main === 'function'", "check.js")
            }.getOrDefault(false)
            if (!hasMain) {
                return "缺少入口函数 main(config)"
            }
            null
        } catch (e: QuickJSException) {
            e.message ?: "JavaScript 校验失败"
        } finally {
            context.close()
            runtime.close()
        }
    }

    private fun describe(value: Any?): String =
        when (value) {
            null -> "null（脚本未 return 对象，或返回了 undefined/null）"
            is JSObject -> "对象 (${value.javaClass.simpleName})"
            else -> "${value::class.java.simpleName}: $value"
        }

    private fun toJson(value: Any?): Any =
        when (value) {
            is Map<*, *> -> jsonObjectFrom(value)
            is List<*> -> JSONArray().apply {
                value.forEach { child -> put(toJson(child)) }
            }
            null -> JSONObject.NULL
            is Boolean -> value
            is Number -> normalizeNumber(value)
            is String -> value
            else -> value.toString()
        }

    private fun jsonObjectFrom(value: Map<*, *>): JSONObject =
        JSONObject().apply {
            value.entries.forEach { entry ->
                put(entry.key.toString(), toJson(entry.value))
            }
        }

    private fun normalizeNumber(value: Number): Number {
        val asDouble = value.toDouble()
        val asLong = value.toLong()
        return if (asDouble == asLong.toDouble()) asLong else asDouble
    }

    private fun fromJson(value: Any?): Any? =
        when (value) {
            is JSONObject -> {
                linkedMapOf<String, Any?>().apply {
                    val keys = value.keys()
                    while (keys.hasNext()) {
                        val key = keys.next()
                        this[key] = fromJson(value.get(key))
                    }
                }
            }
            is JSONArray -> {
                (0 until value.length()).map { index ->
                    fromJson(value.get(index))
                }
            }
            JSONObject.NULL -> null
            else -> value
        }
}

class JsOverrideException(cause: Throwable) : Exception(cause) {
    override val message: String
        get() = cause?.message ?: "JavaScript 覆写执行失败"
}
