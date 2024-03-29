package com.flowfoundation.wallet.page.scan

import android.app.Activity
import android.content.Context
import android.net.Uri
import android.webkit.URLUtil
import com.flowfoundation.wallet.manager.coin.FlowCoin
import com.flowfoundation.wallet.manager.config.AppConfig
import com.flowfoundation.wallet.manager.wallet.WalletManager
import com.flowfoundation.wallet.manager.walletconnect.WalletConnect
import com.flowfoundation.wallet.network.model.AddressBookContact
import com.flowfoundation.wallet.page.browser.openBrowser
import com.flowfoundation.wallet.page.send.transaction.subpage.amount.SendAmountActivity
import com.flowfoundation.wallet.utils.addressPattern
import com.flowfoundation.wallet.utils.logd
import com.flowfoundation.wallet.wallet.toAddress


fun dispatchScanResult(context: Context, str: String) {
    val text = str.trim()
    if (text.isBlank()) {
        return
    }

    if ((text.startsWith("wc:") || text.startsWith("lilico://wc?") || text.startsWith("frw://wc?"))
        && AppConfig.walletConnectEnable()) {
        val wcUri = if (text.startsWith("wc:")) {
            text
        } else {
            Uri.parse(text).getQueryParameter("uri")
        } ?: return
        logd("wc", "wcUri: $wcUri")
        WalletConnect.get().pair(wcUri)
    } else if (addressPattern.matches(text)) {
        if (WalletManager.isChildAccountSelected()) {
            return
        }
        SendAmountActivity.launch(context as Activity, AddressBookContact(address = text.toAddress()), FlowCoin.SYMBOL_FLOW)
    } else if (URLUtil.isValidUrl(text.httpPrefix())) {
        openBrowser(context as Activity, text.httpPrefix())
    }
}

private fun String.httpPrefix(): String {
    if (startsWith("http")) return this

    return "https://$this"
}