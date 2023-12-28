package io.outblock.lilico.page.backup.multibackup.viewmodel

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.nftco.flow.sdk.FlowTransactionStatus
import io.outblock.lilico.manager.account.DeviceInfoManager
import io.outblock.lilico.manager.backup.BackupCryptoProvider
import io.outblock.lilico.manager.drive.ACTION_GOOGLE_DRIVE_UPLOAD_FINISH
import io.outblock.lilico.manager.drive.EXTRA_SUCCESS
import io.outblock.lilico.manager.drive.GoogleDriveAuthActivity
import io.outblock.lilico.manager.flowjvm.CADENCE_ADD_PUBLIC_KEY
import io.outblock.lilico.manager.flowjvm.transactionByMainWallet
import io.outblock.lilico.manager.flowjvm.ufix64Safe
import io.outblock.lilico.manager.transaction.TransactionState
import io.outblock.lilico.manager.transaction.TransactionStateManager
import io.outblock.lilico.network.ApiService
import io.outblock.lilico.network.model.AccountKey
import io.outblock.lilico.network.model.AccountSyncRequest
import io.outblock.lilico.network.retrofit
import io.outblock.lilico.page.backup.multibackup.model.BackupGoogleDriveState
import io.outblock.lilico.page.window.bubble.tools.pushBubbleStack
import io.outblock.lilico.utils.Env
import io.outblock.lilico.utils.ioScope
import wallet.core.jni.HDWallet


class BackupGoogleDriveViewModel : ViewModel() {

    val backupStateLiveData = MutableLiveData<BackupGoogleDriveState>()
    private var backupCryptoProvider: BackupCryptoProvider? = null

    private val uploadReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent?) {
            val isSuccess = intent?.getBooleanExtra(EXTRA_SUCCESS, false) ?: return
            backupStateLiveData.postValue(
                if (isSuccess) {
                    BackupGoogleDriveState.REGISTRATION_KEY_LIST
                } else {
                    BackupGoogleDriveState.UPLOAD_BACKUP_FAILURE
                }
            )
        }
    }

    init {
        LocalBroadcastManager.getInstance(Env.getApp()).registerReceiver(
            uploadReceiver, IntentFilter(
                ACTION_GOOGLE_DRIVE_UPLOAD_FINISH
            )
        )
    }

    fun createBackup() {
        backupCryptoProvider = BackupCryptoProvider(HDWallet(128, ""))
        backupStateLiveData.postValue(BackupGoogleDriveState.UPLOAD_BACKUP)
    }

    fun uploadToChain() {
        ioScope {
            backupCryptoProvider?.let {
                try {
                    val txId = CADENCE_ADD_PUBLIC_KEY.transactionByMainWallet {
                        arg { string(it.getPublicKey()) }
                        arg { uint8(it.getSignatureAlgorithm().index) }
                        arg { uint8(it.getHashAlgorithm().index) }
                        arg { ufix64Safe(500) }
                    }
                    val transactionState = TransactionState(
                        transactionId = txId!!,
                        time = System.currentTimeMillis(),
                        state = FlowTransactionStatus.PENDING.num,
                        type = TransactionState.TYPE_ADD_PUBLIC_KEY,
                        data = ""
                    )
                    TransactionStateManager.newTransaction(transactionState)
                    pushBubbleStack(transactionState)
                } catch (e: Exception) {
                    throw e
                }
            }
        }
    }

    fun uploadToGoogleDrive(context: Context) {
        val mnemonic = backupCryptoProvider?.getMnemonic() ?: throw RuntimeException("Mnemonic cannot be null")
        GoogleDriveAuthActivity.backupMnemonic(context, mnemonic)
    }

    override fun onCleared() {
        LocalBroadcastManager.getInstance(Env.getApp()).unregisterReceiver(uploadReceiver)
        super.onCleared()
    }

    fun registrationKeyList() {
        ioScope {
            backupCryptoProvider?.let {
                try {
                    val deviceInfo = DeviceInfoManager.getDeviceInfoRequest()
                    val service = retrofit().create(ApiService::class.java)
                    val resp = service.syncAccount(
                        AccountSyncRequest(
                            AccountKey(
                                publicKey = it.getPublicKey(),
                                signAlgo = it.getSignatureAlgorithm().index,
                                hashAlgo = it.getHashAlgorithm().index,
                                weight = it.getKeyWeight()
                            ),
                            deviceInfo
                        )
                    )
                    if (resp.status == 200) {
                        backupStateLiveData.postValue(BackupGoogleDriveState.BACKUP_SUCCESS)
                    } else {
                        backupStateLiveData.postValue(BackupGoogleDriveState.NETWORK_ERROR)
                    }
                } catch (e: Exception) {
                    backupStateLiveData.postValue(BackupGoogleDriveState.NETWORK_ERROR)
                }
            }
        }
    }

}