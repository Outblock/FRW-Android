package io.outblock.lilico.page.profile.subpage.backup

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.zackratos.ultimatebarx.ultimatebarx.UltimateBarX
import io.outblock.lilico.R
import io.outblock.lilico.base.activity.BaseActivity
import io.outblock.lilico.databinding.ActivityBackupSettingBinding
import io.outblock.lilico.manager.account.AccountManager
import io.outblock.lilico.manager.drive.ACTION_GOOGLE_DRIVE_DELETE_FINISH
import io.outblock.lilico.manager.drive.ACTION_GOOGLE_DRIVE_RESTORE_FINISH
import io.outblock.lilico.manager.drive.DriveItem
import io.outblock.lilico.manager.drive.EXTRA_CONTENT
import io.outblock.lilico.manager.drive.EXTRA_SUCCESS
import io.outblock.lilico.manager.drive.GoogleDriveAuthActivity
import io.outblock.lilico.page.security.recovery.SecurityRecoveryActivity
import io.outblock.lilico.utils.ioScope
import io.outblock.lilico.utils.isBackupGoogleDrive
import io.outblock.lilico.utils.isBackupManually
import io.outblock.lilico.utils.isNightMode
import io.outblock.lilico.utils.setBackupGoogleDrive
import io.outblock.lilico.utils.setBackupManually
import io.outblock.lilico.utils.uiScope
import io.outblock.lilico.widgets.ProgressDialog
import kotlinx.coroutines.delay

class BackupSettingActivity : BaseActivity() {

    private lateinit var binding: ActivityBackupSettingBinding

    private val driveDeleteReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent?) {
            val isSuccess = intent?.getBooleanExtra(EXTRA_SUCCESS, false) ?: return
            onDeleteCallback(isSuccess)
        }
    }

    private val driveRestoreReceiver by lazy {
        object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val data = intent?.getParcelableArrayListExtra<DriveItem>(EXTRA_CONTENT) ?: return
                ioScope {
                    setBackupGoogleDrive(data.firstOrNull { it.username.lowercase() == AccountManager.userInfo()?.username?.lowercase() } != null)
                    delay(100)
                    uiScope {
                        binding.drivePreference.setChecked(isBackupGoogleDrive())
                    }
                }
            }
        }
    }

    private val progressDialog by lazy { ProgressDialog(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBackupSettingBinding.inflate(layoutInflater)
        setContentView(binding.root)
        UltimateBarX.with(this).fitWindow(true).colorRes(R.color.background).light(!isNightMode(this)).applyStatusBar()

        setupToolbar()
        with(binding) {
            drivePreference.setOnClickListener {
                uiScope {
                    if (isBackupGoogleDrive()) {
                        GoogleDriveDeleteDialog(this@BackupSettingActivity) {
                            progressDialog.show()
                        }.show()
                    } else {
                        BackupGoogleDriveActivity.launch(this@BackupSettingActivity)
                    }
                }
            }
            manuallyPreference.setOnClickListener {
                SecurityRecoveryActivity.launch(this@BackupSettingActivity)
                setBackupManually()
            }
        }

        binding.drivePreference.showProgress()
        GoogleDriveAuthActivity.restoreMnemonic(this)
        LocalBroadcastManager.getInstance(this).registerReceiver(driveRestoreReceiver, IntentFilter(ACTION_GOOGLE_DRIVE_RESTORE_FINISH))
        LocalBroadcastManager.getInstance(this).registerReceiver(driveDeleteReceiver, IntentFilter(ACTION_GOOGLE_DRIVE_DELETE_FINISH))
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> finish()
            else -> super.onOptionsItemSelected(item)
        }
        return true
    }

    override fun onResume() {
        super.onResume()
        updateState()
    }

    override fun onRestart() {
        super.onRestart()
        uiScope { binding.drivePreference.setChecked(isBackupGoogleDrive()) }
    }

    override fun onDestroy() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(driveDeleteReceiver)
        LocalBroadcastManager.getInstance(this).unregisterReceiver(driveRestoreReceiver)
        super.onDestroy()
    }

    private fun onDeleteCallback(isSuccess: Boolean) {
        progressDialog.dismiss()
        if (isSuccess) {
            setBackupGoogleDrive(false)
            ioScope {
                delay(300)
                uiScope { binding.drivePreference.setChecked(false) }
            }
        } else {
            Toast.makeText(this, "Auth error", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateState() {
        uiScope {
            binding.manuallyPreference.setStateVisible(isBackupManually())
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
    }

    companion object {
        fun launch(context: Context) {
            context.startActivity(Intent(context, BackupSettingActivity::class.java))
        }
    }
}