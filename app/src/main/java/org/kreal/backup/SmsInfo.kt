package org.kreal.backup

/**
 * @param address 电话号码
 * @param date 短信接受
 * @param dateSend 短信发送时间
 * @param type 短信类型
 * @param body 短信类容
 */
data class SmsInfo(
        val address: String,
        val body: String,
        val subject: String?,
        val date: Long,
        val dateSend: Long,
        val read: Int,
        val type: Int
)