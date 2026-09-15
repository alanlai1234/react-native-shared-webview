package com.margelo.nitro.sharedwebview

import android.graphics.Bitmap
import android.graphics.Color
import android.util.Log
import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import com.facebook.react.bridge.UiThreadUtil
import com.margelo.nitro.NitroModules
import com.margelo.nitro.core.Promise
import org.json.JSONObject

class HybridBrowserSession: HybridBrowserSessionSpec() {
    var interceptSubframeNavigation: Boolean? = false
    override var onMessage: ((event: WebViewMessageEvent) -> Unit)? = null
    override var onShouldStartLoadWithRequest: ((event: ShouldStartLoadRequest) -> Promise<Boolean>)? =
        null

    override fun loadhtml(html: String) {
        runMain {
            webview.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
        }
    }

    override fun postMessage(data: String) {
        runMain {
            webview.evaluateJavascript(postMessageScript(data), null)
        }
    }

    private var _webview: WebView? = null
    val webview: WebView
        get() {
            _webview?.let { return it }
            UiThreadUtil.assertOnUiThread()
            val context = NitroModules.applicationContext
                ?: error("NitroModules.applicationContext is unavailable.")
            val wv = WebView(context)
            wv.setBackgroundColor(Color.TRANSPARENT)
            wv.settings.javaScriptEnabled = true
            wv.settings.domStorageEnabled = true
            wv.settings.allowFileAccess = true
            wv.settings.allowContentAccess = true
            wv.webViewClient = ClientImpl()
            wv.addJavascriptInterface(BridgeInterface(), "ReactNativeWebView")
            _webview = wv
            return wv
        }

    internal fun dispatchShouldStart(
        hook: (event: ShouldStartLoadRequest) -> Promise<Boolean>,
        payload: ShouldStartLoadRequest,
        timeoutMs: Long = SHOULD_OVERRIDE_URL_LOADING_TIMEOUT_MS,
    ): Boolean {
        return Companion.awaitShouldStart(hook, payload, timeoutMs)
    }

    private inner class ClientImpl : WebViewClient() {
        override fun shouldOverrideUrlLoading(
            view: WebView,
            request: WebResourceRequest,
        ): Boolean {
            val hook = onShouldStartLoadWithRequest ?: return false
            val url = request.url?.toString() ?: return false
            if (!request.isForMainFrame && interceptSubframeNavigation != true) {
                return false
            }
            val payload = ShouldStartLoadRequest(
                url = url,
                navigationType = WebViewNavigationType.OTHER,
                mainDocumentURL = null,
                isTopFrame = request.isForMainFrame, // was null; now meaningful
                hasTargetFrame = null, // Android has no target-frame concept here
                title = view.title ?: "",
                canGoBack = view.canGoBack(),
                canGoForward = view.canGoForward(),
                loading = view.progress < 100
            )
            val allow = dispatchShouldStart(hook, payload)
            return !allow
        }
    }

    companion object {
        @JvmStatic
        internal fun postMessageScript(message: String): String {
            val data = encodeJsStringLiteral(message)
            return "window.dispatchEvent(new MessageEvent('message',{data:$data}));"
        }

        @JvmStatic
        internal fun encodeJsStringLiteral(message: String): String {
            return JSONObject.quote(message)
                .replace(" ", "\\u2028")
                .replace(" ", "\\u2029")
        }

        internal const val SHOULD_OVERRIDE_URL_LOADING_TIMEOUT_MS: Long = 250L
        @JvmStatic
        internal fun awaitShouldStart(
            hook: (event: ShouldStartLoadRequest) -> Promise<Boolean>,
            payload: ShouldStartLoadRequest,
            timeoutMs: Long = SHOULD_OVERRIDE_URL_LOADING_TIMEOUT_MS,
        ): Boolean {
            val promise = hook(payload)
            return awaitBooleanWithTimeout(
                timeoutMs = timeoutMs,
                subscribe = { onResolve, onReject ->
                    promise.then { value -> onResolve(value) }
                    promise.catch { error -> onReject(error) }
                },
            )
        }

        @JvmStatic
        internal fun awaitBooleanWithTimeout(
            timeoutMs: Long,
            subscribe: (
                onResolve: (Boolean) -> Unit,
                onReject: (Throwable) -> Unit,
            ) -> Unit,
        ): Boolean {
            val lock = kotlin.Any()
            val result = arrayOfNulls<Boolean>(1)
            subscribe(
                { value ->
                    synchronized(lock) {
                        result[0] = value
                        @Suppress("PlatformExtensionReceiverOfInline")
                        (lock as Object).notifyAll()
                    }
                },
                { _ ->
                    synchronized(lock) {
                        // Mirror RNW: rejected Promises default to allow.
                        result[0] = true
                        @Suppress("PlatformExtensionReceiverOfInline")
                        (lock as Object).notifyAll()
                    }
                },
            )
            synchronized(lock) {
                val deadline = System.currentTimeMillis() + timeoutMs
                while (result[0] == null) {
                    val remaining = deadline - System.currentTimeMillis()
                    if (remaining <= 0L) break
                    try {
                        @Suppress("PlatformExtensionReceiverOfInline")
                        (lock as Object).wait(remaining)
                    } catch (e: InterruptedException) {
                        Thread.currentThread().interrupt()
                        break
                    }
                }
                // Default to allow when the wait window elapsed without resolution.
                return result[0] ?: true
            }
        }
    }

    private inner class BridgeInterface {
        @JavascriptInterface
        fun postMessage(data: String) {
//            val blob = parseBlobEnvelope(data)
//            if (blob != null) {
//                view.post {
//                    emitFileDownload(
//                        FileDownload(
//                            url = blob.dataUrl,
//                            mimeType = blob.mimeType.takeIf { it.isNotEmpty() },
//                            fileName = blob.fileName.takeIf { it.isNotEmpty() },
//                            contentLength = blob.size.takeIf { it > 0 },
//                            userAgent = null,
//                        ),
//                    )
//                }
//                return
//            }
            webview.post {
                val payload = WebViewMessageEvent(
                    WebViewMessageNativeEvent(
                        data = data,
                        url = webview.url ?: "",
                        title = webview.title ?: "",
                        canGoBack = webview.canGoBack(),
                        canGoForward = webview.canGoForward(),
                        loading = webview.progress < 100
                    ),
                )
                onMessage?.invoke(payload)
            }
        }
    }

    private inline fun runMain(crossinline block: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper())
            block()
        else {
            Handler(Looper.getMainLooper()).post {
                block()
            }
        }
    }
}