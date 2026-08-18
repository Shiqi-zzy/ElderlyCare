package com.elderlycare.app.ui.ezviz

import android.annotation.SuppressLint
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

/**
 * 萤石 JSSDK 网页播放器（播放 ezopen 协议的加密视频流）。
 *
 * 直播与回放页共用：设备开启视频加密时，REST 直链协议（hls/flv/rtmp）接口会拒绝
 * （错误码 60019「加密已开启」），必须取 ezopen:// 协议地址（携带验证码），
 * 交给萤石官方网页播放器（open.ys7.com/console/jssdk/pc.html + accessToken）解码播放。
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun EzvizWebPlayer(url: String, modifier: Modifier = Modifier) {
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            WebView(ctx).apply {
                settings.javaScriptEnabled = true
                settings.mediaPlaybackRequiresUserGesture = false
                settings.domStorageEnabled = true
                settings.allowContentAccess = true
                settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(
                        view: WebView?,
                        request: android.webkit.WebResourceRequest?
                    ): Boolean {
                        val reqUrl = request?.url?.toString() ?: ""
                        return reqUrl.startsWith("ezopen://")
                    }
                }
                loadUrl(url)
            }
        },
        update = { webView ->
            if (webView.url != url) {
                webView.loadUrl(url)
            }
        }
    )
}
