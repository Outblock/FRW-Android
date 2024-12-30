package com.flowfoundation.wallet.manager.account

import com.flowfoundation.wallet.manager.account.model.AccountInfo
import com.flowfoundation.wallet.manager.account.model.AccountInfoInner
import com.flowfoundation.wallet.manager.account.model.ValidateTransactionResult
import com.flowfoundation.wallet.manager.account.model.getByName
import com.flowfoundation.wallet.manager.config.AppConfig
import com.flowfoundation.wallet.manager.config.isGasFree
import com.flowfoundation.wallet.manager.flowjvm.Cadence
import com.flowfoundation.wallet.manager.flowjvm.executeCadence
import com.flowfoundation.wallet.manager.notification.WalletNotificationManager
import com.flowfoundation.wallet.manager.wallet.WalletManager
import com.flowfoundation.wallet.utils.format
import com.flowfoundation.wallet.utils.ioScope
import com.flowfoundation.wallet.utils.logd
import com.flowfoundation.wallet.utils.loge
import com.flowfoundation.wallet.utils.uiScope
import com.google.gson.Gson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.math.BigDecimal


object AccountInfoManager {

    private val TAG = AccountInfoManager::class.java.simpleName
    private val FIXED_MOVE_FEE = BigDecimal("0.001")
    private val MIN_FLOW_BALANCE = BigDecimal("0.001")
    private val AVERAGE_TX_FEE = BigDecimal("0.0005")
    private const val MINIMUM_STORAGE_THRESHOLD: Long = 10000

    private val _accountResultFlow = MutableStateFlow<AccountInfo?>(null)
    val accountResultFlow: StateFlow<AccountInfo?> = _accountResultFlow.asStateFlow()

    fun refreshAccountInfo() {
        ioScope {
            try {
                val walletAddress = WalletManager.wallet()?.walletAddress() ?: return@ioScope

                val result = fetchOnChainAccountInfo(walletAddress)

                _accountResultFlow.value = result

            } catch (e: Exception) {
                _accountResultFlow.value = null
            }
            WalletNotificationManager.onWalletUpdate()
        }
    }

    suspend fun validateFlowTokenTransaction(
        amount: BigDecimal,
        isMoveToken: Boolean
    ): ValidateTransactionResult {
        return validateTransaction(amount, isMoveToken)
    }

    suspend fun validateOtherTransaction(isMove: Boolean): ValidateTransactionResult {
        return validateTransaction(BigDecimal.ZERO, isMove)
    }

    private fun isShowWarning(): Boolean {
        return WalletManager.isChildAccountSelected().not()
                && WalletManager.isEVMAccountSelected().not()
                && AppConfig.showTXWarning()
    }

    private suspend fun validateTransaction(amount: BigDecimal, isMove: Boolean): ValidateTransactionResult {
        if (isShowWarning().not()) {
            return ValidateTransactionResult.FAILURE
        }
        var transferAmount = amount
        val currentAccount = _accountResultFlow.value ?: return ValidateTransactionResult.FAILURE

        if (isStorageInsufficient()) {
            return ValidateTransactionResult.STORAGE_INSUFFICIENT
        }

        if (isBalanceInsufficient()) {
            return ValidateTransactionResult.BALANCE_INSUFFICIENT
        }

        if (isMove) {
            transferAmount += FIXED_MOVE_FEE
        }

        val noStorageAfterAction = currentAccount.availableBalance - transferAmount < getAverageTXFee()

        if (noStorageAfterAction) {
            return ValidateTransactionResult.STORAGE_INSUFFICIENT
        }

//        val insufficientFlowAfter = currentAccount.balance - transferAmount < MIN_FLOW_BALANCE
//
//        if (insufficientFlowAfter) {
//            return ValidateTransactionResult.STORAGE_INSUFFICIENT_AFTER_ACTION
//        }

        return ValidateTransactionResult.SUCCESS
    }

    fun getLeastFlowBalance(): String {
        val leastFlow = _accountResultFlow.value?.let {
            it.storageFlow + MIN_FLOW_BALANCE
        } ?: MIN_FLOW_BALANCE
        return leastFlow.format() + " FLOW"
    }

    private suspend fun getAverageTXFee(): BigDecimal {
        return if (isGasFree()) {
            BigDecimal.ZERO
        } else {
            AVERAGE_TX_FEE
        }
    }

    fun isBalanceInsufficient(): Boolean {
        val currentAccount = _accountResultFlow.value ?: return false
        return currentAccount.balance < MIN_FLOW_BALANCE
    }

    fun isStorageInsufficient(): Boolean {
        val currentAccount = _accountResultFlow.value ?: return false
        return (currentAccount.storageCapacity - currentAccount.storageUsed) < MINIMUM_STORAGE_THRESHOLD
    }

    fun getCurrentFlowBalance(): BigDecimal? {
        return _accountResultFlow.value?.availableBalance
    }

    fun getTotalFlowBalance(): String {
        return (_accountResultFlow.value?.balance?.format() ?: "0") + " FLOW"
    }

    fun getStorageUsageFlow(): String {
        return (_accountResultFlow.value?.storageFlow?.format() ?: "0") + " FLOW"
    }

    fun getStorageUsageProgress(): Float {
        val currentAccount = _accountResultFlow.value ?: return 0f
        if (currentAccount.storageCapacity == 0L) {
            return 1f
        }
        return currentAccount.storageUsed.toFloat() / currentAccount.storageCapacity
    }

    fun getStorageUsed(): Long {
        val currentAccount = _accountResultFlow.value ?: return 0
        return currentAccount.storageUsed
    }

    fun getStorageCapacity(): Long {
        val currentAccount = _accountResultFlow.value ?: return 0
        return currentAccount.storageCapacity
    }

    private fun fetchOnChainAccountInfo(address: String): AccountInfo? {
        try {
            val response = Cadence.CADENCE_GET_ACCOUNT_INFO.executeCadence {
                arg { address(address) }
            }
            logd(TAG, "getAccountInfo response:${String(response?.bytes ?: byteArrayOf())}")
            return parseAccountInfoResult(String(response?.bytes ?: byteArrayOf()))
        } catch (e: Throwable) {
            loge(e)
            return null
        }
    }

    private fun parseAccountInfoResult(json: String): AccountInfo? {
        if (json.isEmpty()) {
            return null
        }
        try {
            val info = Gson().fromJson(json, AccountInfoInner::class.java)
            return AccountInfo(
                address = info.value.getByName("address").orEmpty(),
                balance = BigDecimal(info.value.getByName("balance") ?: "0"),
                availableBalance = BigDecimal(info.value.getByName("availableBalance") ?: "0"),
                storageUsed = info.value.getByName("storageUsed")?.toLongOrNull() ?: 0,
                storageCapacity = info.value.getByName("storageCapacity")?.toLongOrNull() ?: 0,
                storageFlow = BigDecimal(info.value.getByName("storageFlow") ?: "0")
            )
        } catch (e: Exception) {
            loge(e)
            return null
        }
    }
}