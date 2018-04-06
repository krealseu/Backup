package org.kreal.backup

import android.Manifest
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.support.v7.app.AppCompatActivity
import android.view.View
import com.google.gson.Gson
import kotlinx.android.synthetic.main.activity_main.*
import org.kreal.permissiongrant.PermissionGrant
import org.kreal.widget.filepickdialog.FilePickDialogFragment
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

private const val filePickDialogTag = "filePickDialogTag"

class MainActivity : AppCompatActivity(), View.OnClickListener, PermissionGrant.PermissionGrantListener {
    override fun onReject() {
        finish()
    }

    override fun onGrant() {
    }

    private val backupFile = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "CC")
    private val filePickListener: ((Array<out Uri>) -> Unit) = {
        if (!it.isEmpty()) {
            val path = it[0].path
            val file = File(path)
            if (file.exists() && file.isFile && file.canRead()) {
                try {
                    Util.restoreCallLogs(baseContext, Util.loadCallLogsFromJson(file.readText()))
                } catch (e: Exception) {
                }
            }
        }
    }

    override fun onClick(view: View?) {
        view?.also {
            val dateString = SimpleDateFormat("yyyy-MM-dd_HH:mm", Locale.CHINA).format(Date(System.currentTimeMillis()))
            if (backupFile.isFile)
                backupFile.delete()
            if (!backupFile.exists())
                backupFile.mkdir()
            when (it.id) {
                R.id.backupCallLog -> File(backupFile, "$dateString.callLog").writeText(Gson().toJson(Util.loadCallLogs(baseContext)))
                R.id.backupSms -> File(backupFile, "$dateString.sms").writeText(Gson().toJson(Util.loadSmsInfos(baseContext)))
                R.id.restoreSms -> RestoreSmsActivity.startRestore(baseContext)
                R.id.restoreCallLog -> FilePickDialogFragment().apply {
                    type = FilePickDialogFragment.FILE_PICK
                    setListener(filePickListener)
                }.show(fragmentManager, filePickDialogTag)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        (fragmentManager.findFragmentByTag(filePickDialogTag) as? FilePickDialogFragment)?.setListener(filePickListener)
        val permissionGrant = MyPermissionGrant()
        if (!permissionGrant.checkPermissions(baseContext))
            permissionGrant.show(fragmentManager, "storage")
    }

    override fun onResume() {
        super.onResume()
        backupCallLog.setOnClickListener(this)
        backupSms.setOnClickListener(this)
        restoreSms.setOnClickListener(this)
        restoreCallLog.setOnClickListener(this)
    }

    class MyPermissionGrant : PermissionGrant() {
        override val permissions: Array<String> = arrayOf(
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.READ_CALL_LOG,
                Manifest.permission.WRITE_CALL_LOG,
                Manifest.permission.READ_SMS
        )
    }
}
