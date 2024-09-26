package com.flowfoundation.wallet.page.restore

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import com.zackratos.ultimatebarx.ultimatebarx.UltimateBarX
import com.flowfoundation.wallet.R
import com.flowfoundation.wallet.base.activity.BaseActivity
import com.flowfoundation.wallet.databinding.ActivityWalletRestoreBinding
import com.flowfoundation.wallet.manager.app.isPreviewnet
import com.flowfoundation.wallet.manager.app.isTestnet
import com.flowfoundation.wallet.page.restore.keystore.KeyStoreRestoreActivity
import com.flowfoundation.wallet.page.restore.multirestore.MultiRestoreActivity
import com.flowfoundation.wallet.page.wallet.sync.WalletSyncActivity
import com.flowfoundation.wallet.utils.isNightMode
import com.flowfoundation.wallet.widgets.DialogType
import com.flowfoundation.wallet.widgets.SwitchNetworkDialog


class WalletRestoreActivity : BaseActivity() {

    private lateinit var binding: ActivityWalletRestoreBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWalletRestoreBinding.inflate(layoutInflater)
        setContentView(binding.root)
        UltimateBarX.with(this).fitWindow(true).colorRes(R.color.background).light(!isNightMode(this)).applyStatusBar()

        with(binding) {
            llImportFromDevice.setOnClickListener {
                if (isTestnet() || isPreviewnet()) {
                    SwitchNetworkDialog(this@WalletRestoreActivity, DialogType.RESTORE).show()
                } else {
                    WalletSyncActivity.launch(this@WalletRestoreActivity)
                }
            }

            llImportFromBackup.setOnClickListener {
                if (isTestnet() || isPreviewnet()) {
                    SwitchNetworkDialog(this@WalletRestoreActivity, DialogType.RESTORE).show()
                } else {
                    MultiRestoreActivity.launch(this@WalletRestoreActivity)
                }
            }

            llImportFromRecoveryPhrase.setOnClickListener {
                if (isTestnet() || isPreviewnet()) {
                    SwitchNetworkDialog(this@WalletRestoreActivity, DialogType.RESTORE).show()
                } else {
                    com.flowfoundation.wallet.page.walletrestore.WalletRestoreActivity.launch(this@WalletRestoreActivity)
                }
            }
            llImportFromKeyStore.setOnClickListener {
                if (isTestnet() || isPreviewnet()) {
                    SwitchNetworkDialog(this@WalletRestoreActivity, DialogType.RESTORE).show()
                } else {
                    KeyStoreRestoreActivity.launch(this@WalletRestoreActivity)
                }
            }
        }
        setupToolbar()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> finish()
            else -> super.onOptionsItemSelected(item)
        }
        return true
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        title = ""
    }

    companion object {
        fun launch(context: Context) {
            context.startActivity(Intent(context, WalletRestoreActivity::class.java))
        }
    }
}