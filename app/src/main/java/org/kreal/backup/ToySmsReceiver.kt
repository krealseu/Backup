package org.kreal.backup

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class ToySmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Log.i("asd", "Sms")
    }
}
