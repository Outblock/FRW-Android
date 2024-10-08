package com.flowfoundation.wallet.manager.walletconnect.model

import com.walletconnect.android.Core
import com.walletconnect.sign.client.Sign

class WCRequest(
    val metaData: Core.Model.AppMetaData?,
    val requestId: Long,
    val chainId: String?,
    val method: String,
    val params: String,
    val topic: String,
)

fun Sign.Model.PendingRequest.toWcRequest(metaData: Core.Model.AppMetaData?): WCRequest {
    return WCRequest(
        metaData = metaData,
        requestId = requestId,
        method = method,
        params = params,
        topic = topic,
        chainId = chainId,
    )
}

fun Sign.Model.SessionRequest.toWcRequest(): WCRequest {
    return WCRequest(
        metaData = peerMetaData,
        requestId = request.id,
        params = request.params,
        method = request.method,
        topic = topic,
        chainId = chainId,
    )
}