import Foundation

#if canImport(WebKit)
  import WebKit
#endif

protocol MessageDispatcher: AnyObject {
    func dispatchMessage(_ event: WebViewMessageNativeEvent)
}

final class MessageHandler: NSObject, WKScriptMessageHandler {
    weak var dispatcher: MessageDispatcher?

    func userContentController(_ userContentController: WKUserContentController, didReceive message: WKScriptMessage) {
        guard let webView = message.webView else{
            return
        }
        let meta = getInfo.getNativeEvent(view: webView)
        let event = WebViewMessageNativeEvent(
            data: Self.stringifyBody(message.body),
            url: meta.url,
            loading: meta.loading,
            title: meta.title,
            canGoBack: meta.canGoBack,
            canGoForward: meta.canGoForward
        )
        dispatcher?.dispatchMessage(event)
    }
    
    internal static func stringifyBody(_ body: Any) -> String {
      if let s = body as? String { return s }
      if let s = body as? NSString { return s as String }
      if body is NSNull { return "" }
      if let n = body as? NSNumber { return n.stringValue }
      return String(describing: body)
    }
}
