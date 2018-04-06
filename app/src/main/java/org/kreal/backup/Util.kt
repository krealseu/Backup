package org.kreal.backup

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.CallLog
import android.provider.Telephony
import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonParser

object Util {
    fun loadSmsInfos(context: Context): List<SmsInfo> {
        val result = arrayListOf<SmsInfo>()
        val projection = arrayOf(
                Telephony.Sms.ADDRESS,
                Telephony.Sms.DATE,
                Telephony.Sms.SUBJECT,
                Telephony.Sms.READ,
                Telephony.Sms.DATE_SENT,
                Telephony.Sms.TYPE,
                Telephony.Sms.BODY
        )
        if (if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M) context.checkSelfPermission(Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED else true) {
            val c = context.contentResolver.query(Telephony.Sms.CONTENT_URI, projection, null, null, Telephony.Sms.DEFAULT_SORT_ORDER)
            while (c.moveToNext()) {
                val address = c.getString(c.getColumnIndex(Telephony.Sms.ADDRESS)) ?: continue
                val body = c.getString(c.getColumnIndex(Telephony.Sms.BODY))
                val subject = c.getString(c.getColumnIndex(Telephony.Sms.SUBJECT))
                val date = c.getLong(c.getColumnIndex(Telephony.Sms.DATE))
                val dateSend = c.getLong(c.getColumnIndex(Telephony.Sms.DATE_SENT))
                val read = c.getInt(c.getColumnIndex(Telephony.Sms.READ))
                val type = c.getInt(c.getColumnIndex(Telephony.Sms.TYPE))
                result.add(SmsInfo(address, body, subject, date, dateSend, read, type))
            }
            c.close()
        }
        return result
    }

    fun loadSmsInfosFromJson(json: String): List<SmsInfo> = parserJsonArrayList(json)

    fun loadCallLogs(context: Context): List<CallLogInfo> {
        val result = arrayListOf<CallLogInfo>()
        val projection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            arrayOf(CallLog.Calls.TYPE, CallLog.Calls.NUMBER,
                    CallLog.Calls.DATE, CallLog.Calls.DURATION,
                    CallLog.Calls.DATA_USAGE, CallLog.Calls.VIA_NUMBER,
                    CallLog.Calls.NUMBER_PRESENTATION, CallLog.Calls.NUMBER_PRESENTATION,
                    CallLog.Calls.POST_DIAL_DIGITS, CallLog.Calls.FEATURES

            )
        } else {
            arrayOf(CallLog.Calls.TYPE, CallLog.Calls.NUMBER,
                    CallLog.Calls.DATE, CallLog.Calls.DURATION,
                    CallLog.Calls.DATA_USAGE, CallLog.Calls.NUMBER_PRESENTATION,
                    CallLog.Calls.NUMBER_PRESENTATION, CallLog.Calls.FEATURES
            )
        }

        if (if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M) context.checkSelfPermission(Manifest.permission.READ_CALL_LOG) == PackageManager.PERMISSION_GRANTED else true) {
            val c = context.contentResolver.query(CallLog.Calls.CONTENT_URI, projection, null, null, CallLog.Calls.DEFAULT_SORT_ORDER)
            while (c.moveToNext()) {
                val type = c.getInt(c.getColumnIndex(CallLog.Calls.TYPE))
                val number = c.getString(c.getColumnIndex(CallLog.Calls.NUMBER))
                val date = c.getLong(c.getColumnIndex(CallLog.Calls.DATE))
                val duration = c.getLong(c.getColumnIndex(CallLog.Calls.DURATION))
                val dataUsage = c.getLong(c.getColumnIndex(CallLog.Calls.DATA_USAGE))
                val viaNumber = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) c.getString(c.getColumnIndex(CallLog.Calls.VIA_NUMBER)) else ""
                val numberPresentation = c.getInt(c.getColumnIndex(CallLog.Calls.NUMBER_PRESENTATION))
                val postDialDigits = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) c.getString(c.getColumnIndex(CallLog.Calls.POST_DIAL_DIGITS)) else ""
                val features = c.getInt(c.getColumnIndex(CallLog.Calls.FEATURES))
                result.add(CallLogInfo(number, postDialDigits, viaNumber, numberPresentation, type, features, date, duration, dataUsage))
            }
            c.close()
        }
        return result
    }

    fun loadCallLogsFromJson(json: String): List<CallLogInfo> = parserJsonArrayList(json)

    fun restoreCallLogs(context: Context, callLogInfos: List<CallLogInfo>): Int {
        if (if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M) context.checkSelfPermission(Manifest.permission.READ_CALL_LOG) == PackageManager.PERMISSION_DENIED else false)
            return 0
        val exist = loadCallLogs(context)
        val newValues = arrayListOf<ContentValues>()
        callLogInfos.forEach {
            if (!exist.contains(element = it)) {
                val values = ContentValues()
                values.apply {
                    put(CallLog.Calls.NUMBER, it.number)
                    put(CallLog.Calls.NUMBER_PRESENTATION, it.numberPresentation)
                    put(CallLog.Calls.TYPE, it.callType)
                    put(CallLog.Calls.FEATURES, it.features)
                    put(CallLog.Calls.DATE, it.date)
                    put(CallLog.Calls.DURATION, it.duration)
                    put(CallLog.Calls.DATA_USAGE, it.dataUsage)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        put(CallLog.Calls.POST_DIAL_DIGITS, it.postDialDigits)
                        put(CallLog.Calls.VIA_NUMBER, it.viaNumber)
                    }
                }
                newValues.add(values)
            }
        }
        return context.contentResolver.bulkInsert(CallLog.Calls.CONTENT_URI, newValues.toTypedArray())
    }

    fun restoreSms(context: Context, smsInfos: List<SmsInfo>): Int {
        if (if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M) context.checkSelfPermission(Manifest.permission.READ_SMS) == PackageManager.PERMISSION_DENIED else false)
            return 0
        val exist = loadSmsInfos(context)
        val newValues = arrayListOf<ContentValues>()
        smsInfos.forEach {
            if (!exist.contains(it)) {
                val values = ContentValues()
                values.apply {
                    put(Telephony.Sms.ADDRESS, it.address)
                    put(Telephony.Sms.BODY, it.body)
                    if (it.subject != null)
                        put(Telephony.Sms.SUBJECT, it.subject)
                    put(Telephony.Sms.DATE, it.date)
                    put(Telephony.Sms.DATE_SENT, it.dateSend)
                    put(Telephony.Sms.READ, it.read)
                    put(Telephony.Sms.TYPE, it.type)
                }
                newValues.add(values)
            }
        }
        return context.contentResolver.bulkInsert(Telephony.Sms.CONTENT_URI, newValues.toTypedArray())
    }

    fun smsInfoListsToJson(list: List<SmsInfo>): String = Gson().toJson(list)

    private inline fun <reified T> parserJsonArrayList(json: String): List<T> {
        val parser = JsonParser()
        val jsonArray = parser.parse(json).asJsonArray
        val result = arrayListOf<T>()
        val gson = Gson()
        jsonArray.forEach {
            result.add(gson.fromJson(it, T::class.java))
        }
        return result
    }
}