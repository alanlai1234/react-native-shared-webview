import type { HybridObject } from 'react-native-nitro-modules'

export interface WebViewMessageNativeEvent extends WebViewNativeEvent {
	data: string
}
export interface WebViewMessageEvent {
	nativeEvent: WebViewMessageNativeEvent
}
export interface WebViewNativeEvent {
	url: string;
	loading: boolean;
	title: string;
	canGoBack: boolean;
	canGoForward: boolean;
}
export type WebViewNavigationType =
  | 'click'
  | 'formsubmit'
  | 'backforward'
  | 'reload'
  | 'formresubmit'
  | 'other'
export interface WebViewNavigation extends WebViewNativeEvent {
	navigationType: WebViewNavigationType
	mainDocumentURL?: string;
}
export interface ShouldStartLoadRequest extends WebViewNavigation {
	isTopFrame: boolean;
	hasTargetFrame?: boolean;
}

export interface BrowserSession
	extends HybridObject<{
	ios: 'swift'
	android: 'kotlin'
	}> {

	loadhtml(html: string) : void
	onMessage?: (event: WebViewMessageEvent) => void
	postMessage(data: string) : void
	//onNavigationStateChange?: (state: WebViewNavigationState) => void
	//onLoadStart?: (event: WebViewLoadEvent) => void
	//onLoadEnd?: (event: WebViewLoadEvent) => void
	onShouldStartLoadWithRequest?: (event: ShouldStartLoadRequest) => boolean
}
