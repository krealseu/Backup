package org.kreal.backup

data class CallLogInfo(
        val number: String,
        val postDialDigits: String = "",
        val viaNumber: String = "",
        val numberPresentation: Int,
        val callType: Int,
        val features: Int,
        val date: Long,
        val duration: Long,
        val dataUsage: Long?
)