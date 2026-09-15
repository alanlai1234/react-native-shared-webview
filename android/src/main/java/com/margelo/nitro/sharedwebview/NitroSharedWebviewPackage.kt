package com.margelo.nitro.sharedwebview

import com.facebook.react.bridge.NativeModule
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.module.model.ReactModuleInfoProvider
import com.facebook.react.BaseReactPackage
import com.margelo.nitro.sharedwebview.views.HybridSharedWebViewManager
import com.facebook.react.uimanager.ViewManager

class NitroSharedWebviewPackage : BaseReactPackage() {
    override fun getModule(name: String, reactContext: ReactApplicationContext): NativeModule? = null

    override fun getReactModuleInfoProvider(): ReactModuleInfoProvider = ReactModuleInfoProvider { HashMap() }

    override fun createViewManagers(reactContext: ReactApplicationContext): List<ViewManager<*, *>> {
        val viewManagers = ArrayList<ViewManager<*, *>>()
        viewManagers.add(HybridSharedWebViewManager())
        return viewManagers
    }

    companion object {
        init {
            NitroSharedWebviewOnLoad.initializeNative()
        }
    }
}
