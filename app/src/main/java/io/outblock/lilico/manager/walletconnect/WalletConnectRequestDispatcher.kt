package io.outblock.lilico.manager.walletconnect

import androidx.appcompat.app.AppCompatActivity
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.reflect.TypeToken
import com.nftco.flow.sdk.FlowAddress
import com.nftco.flow.sdk.hexToBytes
import com.walletconnect.sign.client.Sign
import com.walletconnect.sign.client.SignClient
import io.outblock.lilico.base.activity.BaseActivity
import io.outblock.lilico.manager.app.AppLifecycleObserver
import io.outblock.lilico.manager.config.AppConfig
import io.outblock.lilico.manager.flowjvm.lastBlockAccountKeyId
import io.outblock.lilico.manager.flowjvm.transaction.PayerSignable
import io.outblock.lilico.manager.flowjvm.transaction.SignPayerResponse
import io.outblock.lilico.manager.flowjvm.transaction.Signable
import io.outblock.lilico.manager.wallet.WalletManager
import io.outblock.lilico.manager.walletconnect.model.Identity
import io.outblock.lilico.manager.walletconnect.model.PollingData
import io.outblock.lilico.manager.walletconnect.model.PollingResponse
import io.outblock.lilico.manager.walletconnect.model.ResponseStatus
import io.outblock.lilico.manager.walletconnect.model.Service
import io.outblock.lilico.manager.walletconnect.model.WCRequest
import io.outblock.lilico.manager.walletconnect.model.WalletConnectMethod
import io.outblock.lilico.network.functions.FUNCTION_SIGN_AS_PAYER
import io.outblock.lilico.network.functions.executeHttpFunction
import io.outblock.lilico.page.main.MainActivity
import io.outblock.lilico.utils.Env
import io.outblock.lilico.utils.ioScope
import io.outblock.lilico.utils.logd
import io.outblock.lilico.utils.loge
import io.outblock.lilico.utils.logw
import io.outblock.lilico.utils.safeRun
import io.outblock.lilico.utils.uiScope
import io.outblock.lilico.wallet.hdWallet
import io.outblock.lilico.wallet.signData
import io.outblock.lilico.widgets.webview.fcl.dialog.FclSignMessageDialog
import io.outblock.lilico.widgets.webview.fcl.dialog.authz.FclAuthzDialog
import io.outblock.lilico.widgets.webview.fcl.dialog.checkAndShowNetworkWrongDialog
import io.outblock.lilico.widgets.webview.fcl.fclAuthzResponse
import io.outblock.lilico.widgets.webview.fcl.fclSignMessageResponse
import io.outblock.lilico.widgets.webview.fcl.model.FclDialogModel
import kotlinx.coroutines.delay
import okio.ByteString.Companion.decodeBase64
import java.io.ByteArrayOutputStream
import java.lang.reflect.Type
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

private const val TAG = "WalletConnectRequestDispatcher"

suspend fun WCRequest.dispatch() {
    when (method) {
        WalletConnectMethod.AUTHN.value -> respondAuthn()
        WalletConnectMethod.AUTHZ.value -> respondAuthz()
        WalletConnectMethod.PRE_AUTHZ.value -> respondPreAuthz()
        WalletConnectMethod.USER_SIGNATURE.value -> respondUserSign()
        WalletConnectMethod.SIGN_PAYER.value -> respondSignPayer()
        WalletConnectMethod.SIGN_PROPOSER.value -> respondSignProposer()
    }
}

private fun WCRequest.respondAuthn() {
    val address = WalletManager.selectedWalletAddress() ?: return
    val json = gson().fromJson<List<Signable>>(params, object : TypeToken<List<Signable>>() {}.type)
    val signable = json.firstOrNull() ?: return
    val services = walletConnectAuthnServiceResponse(address, signable.data?.get("nonce"), signable.data?.get("appIdentifier"), isFromFclSdk())
    val response = Sign.Params.Response(
        sessionTopic = topic,
        jsonRpcResponse = Sign.Model.JsonRpcResponse.JsonRpcResult(requestId, services.responseParse(this))
    )
    logd(TAG, "respondAuthn:\n${services}")

    SignClient.respond(response, onSuccess = { success ->
        logd(TAG, "success:${success}")
    }) { error -> loge(error.throwable) }
    redirectToSourceApp()
}

private suspend fun WCRequest.respondAuthz() {
    val activity = topActivity() ?: return
    val json = gson().fromJson<List<Signable>>(params, object : TypeToken<List<Signable>>() {}.type)
    val signable = json.firstOrNull() ?: return
    val message = signable.message ?: return
    uiScope {
        val data = FclDialogModel(
            title = metaData?.name,
            logo = metaData?.icons?.firstOrNull(),
            url = metaData?.url,
            cadence = signable.cadence,
        )
        if (checkAndShowNetworkWrongDialog(activity.supportFragmentManager, data)) {
            reject()
            return@uiScope
        }

        FclAuthzDialog.show(
            activity.supportFragmentManager,
            data
        )

        FclAuthzDialog.observe { isApprove ->
            ioScope {
                val address = WalletManager.selectedWalletAddress() ?: return@ioScope
                val signature = hdWallet().signData(message.hexToBytes())
                val keyId = FlowAddress(address).lastBlockAccountKeyId()

                if (isApprove) approve(fclAuthzResponse(address, signature, keyId)) else reject()
                uiScope { FclAuthzDialog.dismiss() }
            }
        }
    }
    redirectToSourceApp()
}


private fun WCRequest.respondPreAuthz() {
    val walletAddress = WalletManager.selectedWalletAddress() ?: return
    val payerAddress = if (AppConfig.isFreeGas()) AppConfig.payer().address else walletAddress
    val response = PollingResponse(
        status = ResponseStatus.APPROVED,
        data = PollingData(
            proposer = Service(
                identity = Identity(address = walletAddress, keyId = 0),
                method = "WC/RPC",
                endpoint = WalletConnectMethod.AUTHZ.value,
            ),
            payer = listOf(
                Service(
                    identity = Identity(address = payerAddress, keyId = 0),
                    method = "WC/RPC",
                    endpoint = WalletConnectMethod.SIGN_PAYER.value,
                )
            ),
            authorization = listOf(
                Service(
                    identity = Identity(address = walletAddress, keyId = 0),
                    method = "WC/RPC",
                    endpoint = WalletConnectMethod.SIGN_PROPOSER.value,
                )
            ),
        )
    )
    approve(gson().toJson(response))
}

private suspend fun WCRequest.respondUserSign() {
    val activity = topActivity() ?: return
    val address = WalletManager.selectedWalletAddress() ?: return
    val param = gson().fromJson<List<SignableMessage>>(params, object : TypeToken<List<SignableMessage>>() {}.type)?.firstOrNull()
    val message = param?.message ?: return
    uiScope {
        val data = FclDialogModel(
            title = metaData?.name,
            logo = metaData?.icons?.firstOrNull(),
            url = metaData?.url,
            signMessage = message,
        )

        if (checkAndShowNetworkWrongDialog(activity.supportFragmentManager, data)) {
            reject()
            return@uiScope
        }

        FclSignMessageDialog.show(
            activity.supportFragmentManager,
            data
        )

        FclSignMessageDialog.observe { isApprove ->
            if (isApprove) approve(fclSignMessageResponse(message, address)) else reject()
            FclAuthzDialog.dismiss()
            redirectToSourceApp()
        }
    }
}

private suspend fun WCRequest.respondSignPayer() {
    val json = gson().fromJson<List<Signable>>(params, object : TypeToken<List<Signable>>() {}.type)
    val signable = json.firstOrNull() ?: return
    val server = executeHttpFunction(
        FUNCTION_SIGN_AS_PAYER, PayerSignable(
            transaction = signable.voucher!!,
            message = PayerSignable.Message(signable.message!!)
        )
    )

    safeRun {
        val sigs = gson().fromJson(server, SignPayerResponse::class.java).envelopeSigs
        val response = PollingResponse(
            status = ResponseStatus.APPROVED,
            data = PollingData(
                address = sigs.address,
                keyId = sigs.keyId,
                signature = sigs.sig,
            )
        )
        approve(gson().toJson(response))
        FclAuthzDialog.dismiss()
    }
    redirectToSourceApp()
}


private suspend fun WCRequest.respondSignProposer() {
    val activity = topActivity() ?: return

    logd(TAG, "respondSignProposer param:${params}")
    val signable = params.toSignables(gson())
    val address = WalletManager.selectedWalletAddress() ?: return

    val data = FclDialogModel(
        title = metaData?.name,
        logo = metaData?.icons?.firstOrNull(),
        url = metaData?.url,
        cadence = signable?.voucher?.cadence,
    )

    if (checkAndShowNetworkWrongDialog(activity.supportFragmentManager, data)) {
        reject()
        return
    }

    FclAuthzDialog.show(
        activity.supportFragmentManager,
        data
    )
    FclAuthzDialog.observe { approve ->
        if (approve) {
            val response = PollingResponse(
                status = ResponseStatus.APPROVED,
                data = PollingData(
                    address = address,
                    keyId = 0,
                    signature = hdWallet().signData(signable?.message!!.hexToBytes())
                )
            )
            approve(gson().toJson(response))
        } else {
            reject()
        }
    }
}

private fun gson() = GsonBuilder().registerTypeAdapter(Signable::class.java, SignableDeserializer()).setLenient().create()

private class SignableDeserializer : JsonDeserializer<Signable> {
    override fun deserialize(json: JsonElement, typeOfT: Type?, context: JsonDeserializationContext?): Signable {
        val obj = json.asJsonObject

        if (obj.has("interaction")) {
            val interaction = obj.get("interaction").asJsonObject
            if (interaction.has("payer")) {
                val payer = obj.get("interaction").asJsonObject.get("payer").asString.trim()
                if (!payer.startsWith("[")) {
                    val ja = JsonArray().apply { add(payer) }
                    interaction.add("payer", ja)
                }
            }
        }
        return Gson().fromJson(obj, Signable::class.java)
    }
}


fun gzip(content: String): ByteArray {
    val bos = ByteArrayOutputStream()
    GZIPOutputStream(bos).bufferedWriter(Charsets.UTF_8).use { it.write(content) }
    return bos.toByteArray()
}

fun ungzip(content: ByteArray): String =
    GZIPInputStream(content.inputStream()).bufferedReader(Charsets.UTF_8).use { it.readText() }

fun String.toSignables(gson: Gson): Signable? {
    return try {
        val typeToken = object : TypeToken<List<Signable>>() {}.type
        val result: List<Signable> = gson.fromJson(this, typeToken)
        result.firstOrNull()
    } catch (e: Exception) {
        try {
            val stringListType = object : TypeToken<List<String>>() {}.type
            val arguments: List<String> = gson.fromJson(this, stringListType)
            arguments.firstOrNull()?.decodeBase64()?.toByteArray()?.run {
                val jsonString = ungzip(this)
                gson.fromJson(jsonString, Signable::class.java)
            }
        } catch (e: Exception) {
            null
        }
    }
}

private suspend fun topActivity() = suspendCoroutine<AppCompatActivity?> { continuation ->
    if (!AppLifecycleObserver.isForeground()) {
        uiScope {
            val context = BaseActivity.getCurrentActivity() ?: Env.getApp()
            MainActivity.launch(context)
//            context.startActivity(context.packageManager.getLaunchIntentForPackage(BuildConfig.APPLICATION_ID))
//            MainActivity.launch(context)
            delay(1000)
            continuation.resume(BaseActivity.getCurrentActivity())
            logw("xxx", "activity1:${BaseActivity.getCurrentActivity()}")
        }
    } else {
        continuation.resume(BaseActivity.getCurrentActivity())
    }
}