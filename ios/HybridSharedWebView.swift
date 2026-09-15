import Foundation
import WebKit

final class WebviewContainer: UIView {
    var webview: WKWebView? {
        didSet {
            oldValue?.removeFromSuperview()
            attach()
        }
    }
    
    func attach(){
        guard let webview else{
            return
        }
        DispatchQueue.main.async { [self] in
            webview.translatesAutoresizingMaskIntoConstraints = true
            addSubview(webview)
            setNeedsLayout()
        }
    }

    override func layoutSubviews() {
        super.layoutSubviews()
        webview?.frame = bounds
    }
}

class HybridSharedWebView : HybridSharedWebViewSpec {
    func reattach() throws {
        container.attach()
    }
    
    var session: (any HybridBrowserSessionSpec)? = nil{
        didSet {
            guard let session = session as? HybridBrowserSession else {
                return
            }
            container.webview = session.webview
        }
    }
    
    private let container = WebviewContainer()
    var view: UIView{
        container
    }
}
