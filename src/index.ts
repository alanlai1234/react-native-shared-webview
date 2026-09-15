import { NitroModules, getHostComponent, getHybridObjectConstructor } from 'react-native-nitro-modules'
import type { BrowserSession } from './specs/BrowserSession.nitro.ts'
import type { SharedWebViewProps, SharedWebViewMethods } from './specs/SharedWebView.nitro.ts'
import SharedWebViewConfig from '../nitrogen/generated/shared/json/SharedWebViewConfig.json'
import { useState, useEffect } from 'react'

export const SharedWebView = getHostComponent<SharedWebViewProps, SharedWebViewMethods>(
    "SharedWebView",
    () => SharedWebViewConfig
)

//export const BrowserSession = NitroModules.createHybridObject<BrowserSession>("BrowserSession")
export const BrowserSessionConstructor = getHybridObjectConstructor<BrowserSession>('BrowserSession')
export function useBrowserSession(html?: String): BrowserSession {
    const [session] = useState(() => new BrowserSessionConstructor())
	useEffect(() => {
		if (html != null) {
			session.loadhtml(html)
		}
	}, [html])
    return session
}
