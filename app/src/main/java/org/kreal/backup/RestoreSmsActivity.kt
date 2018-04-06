package org.kreal.backup

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Fragment
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.preference.PreferenceManager
import android.provider.Telephony
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_back_sms.*
import org.kreal.widget.filepickdialog.FilePickDialogFragment
import java.io.File


private const val taskFragmentTag = "taskFragmentTag"
private const val defaultSmsAppKey = "DefaultSmsAppKey"
private const val filePickDialogTag = "filePickDialogTag"

class RestoreSmsActivity : AppCompatActivity(), View.OnClickListener {
    private val filePickListener: ((Array<out Uri>) -> Unit) = {
        if (!it.isEmpty()) {
            val path = it[0].path
            val file = File(path)
            if (file.exists() && file.isFile && file.canRead()) {
                (fragmentManager.findFragmentByTag(taskFragmentTag) as? BackgroundTask)?.load(file.readText())
                updateView()
            }
        }
    }
    private val handler = Handler()

    override fun onClick(view: View?) {
        view?.also {
            when (it.id) {
                R.id.loadSmsFile -> {
                    FilePickDialogFragment().apply {
                        type = FilePickDialogFragment.FILE_PICK
                        setListener(filePickListener)
                    }.show(fragmentManager, filePickDialogTag)
                }
                R.id.backup -> {
                    if (getDefaultSmsApp() != baseContext.packageName) {
                        val intent = Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT)
                        intent.putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, packageName)
                        startActivityForResult(intent, 1)
                    } else (fragmentManager.findFragmentByTag(taskFragmentTag) as? BackgroundTask)?.restore()
                }
                R.id.restoreSmsApp -> actionRestoreDefaultSmsApp()
            }
        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_back_sms)
        val task = fragmentManager.findFragmentByTag(taskFragmentTag)
        if (task == null)
            fragmentManager.beginTransaction().add(BackgroundTask(), taskFragmentTag).commit()
        (fragmentManager.findFragmentByTag(filePickDialogTag) as? FilePickDialogFragment)?.setListener(filePickListener)

        loadSmsFile.setOnClickListener(this)
        backup.setOnClickListener(this)
        restoreSmsApp.setOnClickListener(this)
    }

    override fun onResume() {
        super.onResume()
        updateView()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (resultCode) {
            Activity.RESULT_OK -> (fragmentManager.findFragmentByTag(taskFragmentTag) as? BackgroundTask)?.restore()
            else -> Toast.makeText(baseContext, "Fail: Grant be deny", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onBackPressed() {
        val task = fragmentManager.findFragmentByTag(taskFragmentTag)
                ?: return super.onBackPressed()
        task as BackgroundTask
        if (!task.isRunning)
            super.onBackPressed()
        else Toast.makeText(baseContext, "wait restore finished", Toast.LENGTH_SHORT).show()
    }

    private fun updateView() {
        handler.post {
            val task = fragmentManager.findFragmentByTag(taskFragmentTag) as BackgroundTask
            loadSmsFile.isEnabled = !task.isRunning
            restoreSmsApp.isEnabled = !task.isRunning
            backup.isEnabled = !(task.isRunning || !task.isPreparing)
        }
    }

    private fun actionRestoreDefaultSmsApp() {
        val default = getDefaultSmsApp()
        if (default != Telephony.Sms.getDefaultSmsPackage(baseContext)) {
            val intent = Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT)
            intent.putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, default)
            baseContext.startActivity(intent)
        }

    }

    private fun getDefaultSmsApp(): String {
        val myPackageName = baseContext.packageName
        val currentSmsApp = Telephony.Sms.getDefaultSmsPackage(baseContext)
        return if (myPackageName == currentSmsApp)
            PreferenceManager.getDefaultSharedPreferences(baseContext).getString(defaultSmsAppKey, "")
        else {
            PreferenceManager.getDefaultSharedPreferences(baseContext).edit().putString(defaultSmsAppKey, currentSmsApp).apply()
            currentSmsApp
        }
    }

    class BackgroundTask : Fragment() {
        var isRunning: Boolean = false
        var isPreparing: Boolean = false
        private var newSmsInfo: ArrayList<SmsInfo> = arrayListOf()

        private lateinit var activity: RestoreSmsActivity

        init {
            retainInstance = true
        }

        override fun onAttach(context: Context?) {
            super.onAttach(context)
            if (context !is RestoreSmsActivity)
                throw Exception("Error")
            else {
                activity = context
            }
        }

        internal fun load(jsonString: String) {
            if (!isRunning) {
                newSmsInfo.clear()
                try {
                    val fileSmsInfos = Util.loadSmsInfosFromJson(jsonString)
                    val exist = Util.loadSmsInfos(activity)
                    fileSmsInfos.forEach {
                        if (!exist.contains(it))
                            newSmsInfo.add(it)
                    }
                } catch (e: Exception) {
                }
            }
            isPreparing = true
        }

        internal fun restore() {
            if (isRunning || !isPreparing)
                return
            isRunning = true
            activity.progressBar.max = newSmsInfo.size
            activity.progressBar.progress = 0
            activity.updateView()
            Thread {
                val contentResolver = activity.contentResolver
                newSmsInfo.forEach {
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
                    contentResolver.insert(Telephony.Sms.CONTENT_URI, values)
                    activity.progressBar.incrementProgressBy(1)
                }
                isRunning = false
                isPreparing = false
                activity.actionRestoreDefaultSmsApp()
            }.start()
        }

    }


    companion object {

        fun startRestore(context: Context) {
            val intent = Intent(context, RestoreSmsActivity::class.java)
            context.startActivity(intent)
        }
    }
}
