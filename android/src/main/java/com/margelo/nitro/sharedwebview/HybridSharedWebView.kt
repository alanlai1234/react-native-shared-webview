package com.margelo.nitro.sharedwebview

import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import com.facebook.react.bridge.UiThreadUtil
import com.facebook.react.uimanager.ThemedReactContext

class HybridSharedWebView(val context: ThemedReactContext): HybridSharedWebViewSpec() {
    override var session: HybridBrowserSessionSpec? = null
        set(value) {
            field = value
            runMain{ attach() }
        }
    override fun reattach() {
        runMain{ attach() }
    }

    private val container = FrameLayout(context)
    override val view: View
        get() = container

    private fun attach(){
        val session = session as? HybridBrowserSession
        val webview = session?.webview ?: return
        (webview.parent as? ViewGroup)?.removeView(webview)
        container.removeAllViews()
        container.addView(webview, FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        ))
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