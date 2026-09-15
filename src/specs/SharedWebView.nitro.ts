import type { HybridView, HybridViewProps, HybridViewMethods } from 'react-native-nitro-modules'
import type {BrowserSession} from "./BrowserSession.nitro.ts"

export interface SharedWebViewProps extends HybridViewProps {
	session?: BrowserSession
}

export interface SharedWebViewMethods extends HybridViewMethods {
	reattach(): void
}

export type SharedWebView = HybridView<SharedWebViewProps, SharedWebViewMethods>
