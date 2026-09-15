import Foundation
import WebKit
import NitroModules

final class HybridBrowserSession: HybridBrowserSessionSpec, MessageDispatcher {
    var onShouldStartLoadWithRequest: ((ShouldStartLoadRequest) -> NitroModules.Promise<Bool>)?
    var onMessage: ((WebViewMessageEvent) -> Void)?
    func loadhtml(html: String) {
        DispatchQueue.main.async { [self] in
            self.webview.loadHTMLString(html, baseURL: nil)
        }
    }
    func postMessage(data: String) throws {
        DispatchQueue.main.async { [self] in
            self.webview.evaluateJavaScript(NitroWebViewPostMessage.buildStatement(data), completionHandler: nil)
        }
    }
    
    
    private let messageHandler = MessageHandler()
    private let navigatedelegate = NavigationDelegate()
    
    private func initWebview() -> WKWebView{
        messageHandler.dispatcher = self
        let configuration = WKWebViewConfiguration()
        let controller = configuration.userContentController
        controller.add(messageHandler, name: "ReactNativeWebView")
        controller.addUserScript(WKUserScript(source: Self.bridgeBootstrapScript, injectionTime: .atDocumentStart, forMainFrameOnly: false))
        let wv = WKWebView(
            frame: .zero,
            configuration: configuration
        )
        wv.isOpaque = false
        wv.backgroundColor = .clear
        wv.scrollView.backgroundColor = .clear
        navigatedelegate.owner = self
        wv.navigationDelegate = navigatedelegate
        
        return wv
    }

    lazy var webview: WKWebView = initWebview()
    
    func dispatchMessage(_ event: WebViewMessageNativeEvent) {
        let payload = WebViewMessageEvent(
          nativeEvent: event
        )
        onMessage?(payload)
    }
    
    fileprivate static let bridgeBootstrapScript: String = """
    ;(function () {
      var __bridge = window.ReactNativeWebView;
      if (__bridge && typeof __bridge.postMessage === 'function') {
        return;
      }
      if (!__bridge) {
        __bridge = {};
        window.ReactNativeWebView = __bridge;
      }
      __bridge.postMessage = function (data) {
        var __payload = (typeof data === 'string') ? data : String(data);
        var __wk = window.webkit;
        if (__wk && __wk.messageHandlers && __wk.messageHandlers.ReactNativeWebView) {
          __wk.messageHandlers.ReactNativeWebView.postMessage(__payload);
        }
      };
    })();
    """
    
    fileprivate func dispatchShouldStart(
      _ payload: ShouldStartLoadRequest,
      complete: @escaping (Bool) -> Void
    ) {
      guard let hook = onShouldStartLoadWithRequest else {
        complete(true)
        return
      }
      let promise = hook(payload)
      promise
        .then { allow in complete(allow) }
        .catch { error in
            print("error: ", error)
            complete(true) }
    }
    
}

final class NavigationDelegate: NSObject, WKNavigationDelegate{
    weak var owner: HybridBrowserSession?
    private var pendingDecisions: [ObjectIdentifier: (WKNavigationActionPolicy) -> Void] = [:]
    
    func webView(
      _ webView: WKWebView,
      decidePolicyFor navigationAction: WKNavigationAction,
      decisionHandler: @escaping (WKNavigationActionPolicy) -> Void
    ){
          // New-window handling must run BEFORE the should-start parking below.
          // A `target=_blank` / `window.open` with no target frame that is
          // cancelled here never reaches `createWebViewWith`, so onOpenWindow has
          // to fire from this spot too (react-native-webview parity). When
          // onOpenWindow is unset, fall through to the normal path so the default
          // in-place load still happens.
//          if let owner = owner,
//             owner.onOpenWindow != nil,
//             navigationAction.targetFrame == nil {
//            let url = navigationAction.request.url?.absoluteString ?? ""
//            owner.emitOpenWindow(targetUrl: url)
//            decisionHandler(.cancel)
//            return
//          }
        guard let owner = owner, owner.onShouldStartLoadWithRequest != nil else {
            decisionHandler(.allow)
            return
        }
        let key = ObjectIdentifier(navigationAction)
        pendingDecisions[key] = decisionHandler
        let payload = getInfo.getShouldStart(navigationAction, view: webView)
        owner.dispatchShouldStart(payload) { [weak self] allow in
            guard let self = self else { return }
            let handler = self.pendingDecisions.removeValue(forKey: key)
            handler?(allow ? .allow : .cancel)
        }
    }
}

struct NativeEvent: Equatable{
    let url: String
    let loading: Bool
    let canGoBack: Bool
    let canGoForward: Bool
    let title: String
    
    init(url: String, loading: Bool, canGoBack: Bool, canGoForward: Bool, title: String) {
        self.url = url
        self.loading = loading
        self.canGoBack = canGoBack
        self.canGoForward = canGoForward
        self.title = title
    }
}

struct getInfo{
    static func getNativeEvent(view: WKWebView) -> NativeEvent{
        return NativeEvent(url: view.url?.absoluteString ?? "",
                           loading: view.isLoading,
                           canGoBack: view.canGoBack,
                           canGoForward: view.canGoForward,
                           title: view.title ?? "")
    }
    
    static func navigationType(from raw: WKNavigationType) -> WebViewNavigationType {
        switch raw {
            case .linkActivated: return .click
            case .formSubmitted: return .formsubmit
            case .backForward: return .backforward
            case .reload: return .reload
            case .formResubmitted: return .formresubmit
            case .other: return .other
            @unknown default: return .other
        }
    }
    
    static func getShouldStart(
        _ navigationAction: WKNavigationAction,
        view: WKWebView
    ) -> ShouldStartLoadRequest{
        let request = navigationAction.request
        let url = request.url?.absoluteString ?? ""
        let mainDoc = request.mainDocumentURL?.absoluteString
        let target = navigationAction.targetFrame
        return ShouldStartLoadRequest(
            isTopFrame: target?.isMainFrame ?? false,
            hasTargetFrame: target != nil,
            navigationType: navigationType(from: navigationAction.navigationType),
            mainDocumentURL: mainDoc,
            url: url,
            loading: view.isLoading,
            title: view.title ?? "",
            canGoBack: view.canGoBack,
            canGoForward: view.canGoForward
        )
    }
}
