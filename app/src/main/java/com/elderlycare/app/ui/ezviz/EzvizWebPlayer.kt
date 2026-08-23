package com.elderlycare.app.ui.ezviz

import android.annotation.SuppressLint
import android.util.Log
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

/**
 * 萤石 EZUIKit JS 网页播放器（播放 ezopen 协议的加密视频流）。
 *
 * 直播与回放页共用：设备开启视频加密时，REST 直链协议（hls/flv/rtmp）接口会拒绝
 * （错误码 60019「加密已开启」），必须取 ezopen:// 协议地址（携带验证码），
 * 交给本地 assets/ez-player.html（EZUIKit JS SDK）解码播放。
 *
 * 页面内 console.error 会通过 WebChromeClient 转发到 logcat（tag: EzvizWebPlayer），
 * 便于真机排查参数错误与播放器错误。
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun EzvizWebPlayer(
    url: String,
    modifier: Modifier = Modifier,
    /** H5 页面 console 消息回调（如播放器错误）——默认空实现，调用方按需监听 */
    onConsoleMessage: (String) -> Unit = {}
) {
    // AndroidView factory 只执行一次，用 rememberUpdatedState 保证回调始终指向最新 lambda
    val currentOnConsoleMessage by rememberUpdatedState(onConsoleMessage)
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            WebView(ctx).apply {
                settings.javaScriptEnabled = true
                settings.mediaPlaybackRequiresUserGesture = false
                settings.domStorageEnabled = true
                settings.allowContentAccess = true
                settings.allowFileAccess = true
                settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(
                        view: WebView?,
                        request: android.webkit.WebResourceRequest?
                    ): Boolean {
                        val reqUrl = request?.url?.toString() ?: ""
                        // 拦截 SDK 可能触发的 ezopen:// 协议导航，其余（含 http(s) 跳转）放行给 WebView
                        return reqUrl.startsWith("ezopen://")
                    }

                    override fun onReceivedError(
                        view: WebView?,
                        request: android.webkit.WebResourceRequest?,
                        error: android.webkit.WebResourceError?
                    ) {
                        Log.e("EzvizWebPlayer", "页面加载失败: url=${request?.url} code=${error?.errorCode} desc=${error?.description}")
                    }

                    override fun onReceivedHttpError(
                        view: WebView?,
                        request: android.webkit.WebResourceRequest?,
                        errorResponse: android.webkit.WebResourceResponse?
                    ) {
                        Log.e("EzvizWebPlayer", "HTTP 错误: url=${request?.url} status=${errorResponse?.statusCode}")
                    }

                    override fun onPageFinished(view: WebView?, pageUrl: String?) {
                        Log.d("EzvizWebPlayer", "页面加载完成: $pageUrl")
                    }
                }
                webChromeClient = object : WebChromeClient() {
                    override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                        val msg = consoleMessage?.message() ?: return false
                        Log.d("EzvizWebPlayer", "H5 console [${consoleMessage.messageLevel()}]: $msg")
                        currentOnConsoleMessage(msg)
                        return true
                    }
                }
                // tag 记录已请求的 url，避免 Compose 重组期间对同一 url 反复 loadUrl 打断页面加载
                tag = url
                loadUrl(url)
            }
        },
        update = { webView ->
            // 只在 url 真正变化时重新加载；加载过程中 webView.url 可能是旧值，比较 tag 更可靠
            if (webView.tag != url) {
                webView.tag = url
                webView.loadUrl(url)
            }
        }
    )
}
