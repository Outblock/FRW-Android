package com.flowfoundation.wallet.utils

import android.util.Log
import com.flowfoundation.wallet.BuildConfig
import com.flowfoundation.wallet.firebase.analytics.reportErrorToDebugView
import com.flowfoundation.wallet.firebase.analytics.reportException
import com.flowfoundation.wallet.utils.debug.fragments.debugViewer.DebugViewerDataSource
import com.nftco.flow.sdk.FlowException
import retrofit2.HttpException

fun logv(tag: String?, msg: Any?) {
    log(tag, msg, Log.VERBOSE)
}

fun logd(tag: String?, msg: Any?) {
    log(tag, msg, Log.DEBUG)
}

fun logi(tag: String?, msg: Any?) {
    log(tag, msg, Log.INFO)
}

fun logw(tag: String?, msg: Any?) {
    log(tag, msg, Log.WARN)
}

fun loge(tag: String?, msg: Any?) {
    log(tag, msg, Log.ERROR)
    reportErrorToDebugView(tag, mapOf("errorInfo" to (msg?.toString() ?: "")))
}

fun loge(msg: Throwable?, printStackTrace: Boolean = true, report: Boolean = true) {
    log("Exception", msg?.message ?: "", Log.ERROR)
    if (printLog() && printStackTrace) {
        msg?.printStackTrace()
    }
    if (report) {
        ioScope { msg?.let { reportException("exception_report", it) } }
    }
}

fun reportApiErrorToDebugView(api: String, throwable: Throwable?) {
    val title = throwable?.javaClass?.simpleName ?: "api_error"
    val params = mutableMapOf(
        "api" to api,
        "message" to throwable?.message.orEmpty(),
        "response" to (throwable as? HttpException)?.response().toString()
    )
    DebugViewerDataSource.error(title, params.toString())
}

fun reportCadenceErrorToDebugView(cadence: String, throwable: Throwable?) {
    val title = throwable?.javaClass?.simpleName ?: "cadence_error"
    val params = mutableMapOf(
        "cadence" to cadence,
        "message" to throwable?.message.orEmpty(),
        "cause" to (throwable as? FlowException)?.cause.toString()
    )
    DebugViewerDataSource.error(title, params.toString())
}

private fun log(tag: String?, msg: Any?, version: Int) {
    if (!printLog()) {
        return
    }

    val tag = "[${tag ?: ""}]"
    val text = msg?.toString() ?: return
    val maxLength = 3500
    if (text.length > maxLength) {
        val chunkCount: Int = text.length / maxLength // integer division
        for (i in 0..chunkCount) {
            val max = maxLength * (i + 1)
            if (max >= text.length) {
                print(tag, text.substring(maxLength * i), version)
            } else {
                print(tag, text.substring(maxLength * i, max), version)
            }
        }
    } else print(tag, text, version)
}

private fun print(tag: String, msg: String, version: Int) {
    when (version) {
        Log.VERBOSE -> Log.v(tag, msg)
        Log.DEBUG -> Log.d(tag, msg)
        Log.INFO -> Log.i(tag, msg)
        Log.WARN -> Log.w(tag, msg)
        Log.ERROR -> Log.e(tag, msg)
    }
}

private fun printLog() = BuildConfig.DEBUG || isDev()
//private fun printLog() = true