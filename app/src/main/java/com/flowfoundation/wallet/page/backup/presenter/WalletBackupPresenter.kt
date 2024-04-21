package com.flowfoundation.wallet.page.backup.presenter

import androidx.recyclerview.widget.LinearLayoutManager
import com.flowfoundation.wallet.databinding.ActivityWalletBackupBinding
import com.flowfoundation.wallet.manager.app.isPreviewnet
import com.flowfoundation.wallet.manager.app.isTestnet
import com.flowfoundation.wallet.page.backup.BackupListAdapter
import com.flowfoundation.wallet.page.backup.WalletBackupActivity
import com.flowfoundation.wallet.page.backup.device.CreateDeviceBackupActivity
import com.flowfoundation.wallet.page.backup.multibackup.MultiBackupActivity
import com.flowfoundation.wallet.utils.extensions.gone
import com.flowfoundation.wallet.utils.extensions.invisible
import com.flowfoundation.wallet.utils.extensions.setVisible
import com.flowfoundation.wallet.utils.extensions.visible
import com.flowfoundation.wallet.utils.ioScope
import com.flowfoundation.wallet.utils.isMultiBackupCreated
import com.flowfoundation.wallet.utils.setMultiBackupCreated
import com.flowfoundation.wallet.utils.setMultiBackupDeleted
import com.flowfoundation.wallet.widgets.DialogType
import com.flowfoundation.wallet.widgets.SwitchNetworkDialog

class WalletBackupPresenter(
    val activity: WalletBackupActivity,
    val binding: ActivityWalletBackupBinding,
) {

    private val backupAdapter by lazy { BackupListAdapter() }
    private val deviceAdapter by lazy { BackupListAdapter() }

    private var isBackupListLoading = true
    private var isDeviceListLoading = true

    init {
        with(binding.rvMultiBackupList) {
            adapter = backupAdapter
            layoutManager = LinearLayoutManager(context)
        }
        with((binding.rvDeviceBackupList)) {
            adapter = deviceAdapter
            layoutManager = LinearLayoutManager(context)
        }
        with(binding) {
            cvCreateDeviceBackup.setOnClickListener {
                CreateDeviceBackupActivity.launch(activity)
            }
            cvCreateMultiBackup.setOnClickListener {
                if (isTestnet() || isPreviewnet()) {
                    SwitchNetworkDialog(activity, DialogType.BACKUP).show()
                } else {
                    MultiBackupActivity.launch(activity)
                }
            }
        }
    }

    fun showLoading() {
        isBackupListLoading = true
        isDeviceListLoading = true
        with(binding) {
            cvCreateDeviceBackup.gone()
            cvCreateMultiBackup.gone()
            llMultiBackupList.gone()
            llDeviceBackupList.gone()
            lavLoading.visible()
        }
    }

    fun bindBackupList(list: List<Any>) {
        with(binding) {
            cvCreateMultiBackup.setVisible(list.isEmpty())
            llMultiBackupList.setVisible(list.isNotEmpty())
            backupAdapter.setNewDiffData(list)
        }
        setMultiBackupStatus(list.isEmpty())
        isBackupListLoading = false
        checkLoadingStatus()
    }

    private fun setMultiBackupStatus(noBackup: Boolean) {
        if (noBackup) {
            setMultiBackupDeleted()
        } else {
            setMultiBackupCreated()
        }
    }

    fun bindDeviceList(list: List<Any>) {
        with(binding) {
            cvCreateDeviceBackup.setVisible(list.isEmpty())
            llDeviceBackupList.setVisible(list.isNotEmpty())
            deviceAdapter.setNewDiffData(list)
        }
        isDeviceListLoading = false
        checkLoadingStatus()
    }

    private fun checkLoadingStatus() {
        if (!isBackupListLoading && !isDeviceListLoading) {
            binding.lavLoading.gone()
        }
    }
}