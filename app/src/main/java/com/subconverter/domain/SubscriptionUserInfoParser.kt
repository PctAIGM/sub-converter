package com.subconverter.domain

object SubscriptionUserInfoParser {
    fun parse(headerValue: String?): SubscriptionUserInfo? {
        if (headerValue.isNullOrBlank()) return null

        val values = headerValue
            .split(';')
            .mapNotNull { part ->
                val pieces = part.trim().split('=', limit = 2)
                if (pieces.size == 2) pieces[0].trim().lowercase() to pieces[1].trim() else null
            }
            .toMap()

        val info = SubscriptionUserInfo(
            uploadBytes = values["upload"]?.toLongOrNull(),
            downloadBytes = values["download"]?.toLongOrNull(),
            totalBytes = values["total"]?.toLongOrNull(),
            expireAtSeconds = values["expire"]?.toLongOrNull(),
        )

        return info.takeUnless {
            it.uploadBytes == null &&
                it.downloadBytes == null &&
                it.totalBytes == null &&
                it.expireAtSeconds == null
        }
    }
}
