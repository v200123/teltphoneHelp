package com.example.myservicecenter.core

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.net.http.SslError
import android.util.Log
import android.view.View
import android.webkit.ConsoleMessage
import android.webkit.RenderProcessGoneDetail
import android.webkit.SslErrorHandler
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient

object CommonWebViewSupport {

    @SuppressLint("SetJavaScriptEnabled")
    fun configure(
        webView: WebView,
        logTag: String,
        onPageCommitVisible: ((WebView?, String?) -> Unit)? = null,
        onPageFinished: ((WebView?, String?) -> Unit)? = null,
        shouldOverrideUrlLoading: ((WebView?, WebResourceRequest?) -> Boolean)? = null,
        onProgressChanged: ((WebView?, Int) -> Unit)? = null
    ) {
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            loadsImagesAutomatically = true
            useWideViewPort = false
            loadWithOverviewMode = true
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            allowFileAccess = true
            allowContentAccess = true
            blockNetworkLoads = false
            mediaPlaybackRequiresUserGesture = false
            allowFileAccessFromFileURLs = true
            allowUniversalAccessFromFileURLs = true
        }

        webView.overScrollMode = View.OVER_SCROLL_NEVER
        webView.isVerticalScrollBarEnabled = false
        webView.isHorizontalScrollBarEnabled = false

        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                Log.d(logTag, "progress=$newProgress url=${view?.url}")
                onProgressChanged?.invoke(view, newProgress)
                super.onProgressChanged(view, newProgress)
            }

            override fun onReceivedTitle(view: WebView?, title: String?) {
                Log.d(logTag, "onReceivedTitle title=$title url=${view?.url}")
                super.onReceivedTitle(view, title)
            }

            override fun onReceivedIcon(view: WebView?, icon: Bitmap?) {
                Log.d(logTag, "onReceivedIcon url=${view?.url} iconNull=${icon == null}")
                super.onReceivedIcon(view, icon)
            }

            override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                if (consoleMessage != null) {
                    Log.d(
                        logTag,
                        "console[${consoleMessage.messageLevel()}] ${consoleMessage.message()} @${consoleMessage.sourceId()}:${consoleMessage.lineNumber()}"
                    )
                }
                return super.onConsoleMessage(consoleMessage)
            }
        }

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
            ): Boolean {
                val targetUrl = request?.url?.toString().orEmpty()
                Log.d(logTag, "shouldOverrideUrlLoading url=$targetUrl")
                return shouldOverrideUrlLoading?.invoke(view, request) ?: false
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                Log.d(logTag, "onPageStarted url=$url")
                super.onPageStarted(view, url, favicon)
            }

            override fun onLoadResource(view: WebView?, url: String?) {
                Log.d(logTag, "onLoadResource url=$url")
                super.onLoadResource(view, url)
            }

            override fun shouldInterceptRequest(
                view: WebView?,
                request: WebResourceRequest?
            ): WebResourceResponse? {
                Log.d(
                    logTag,
                    "shouldInterceptRequest method=${request?.method} isMainFrame=${request?.isForMainFrame} url=${request?.url}"
                )
                return super.shouldInterceptRequest(view, request)
            }

            override fun onPageCommitVisible(view: WebView?, url: String?) {
                Log.d(logTag, "onPageCommitVisible url=$url")
                onPageCommitVisible?.invoke(view, url)
                super.onPageCommitVisible(view, url)
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                Log.d(logTag, "onPageFinished url=$url")
                onPageFinished?.invoke(view, url)
                super.onPageFinished(view, url)
            }

            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?
            ) {
                Log.e(
                    logTag,
                    "onReceivedError code=${error?.errorCode} desc=${error?.description} url=${request?.url}"
                )
                super.onReceivedError(view, request, error)
            }

            override fun onReceivedHttpError(
                view: WebView?,
                request: WebResourceRequest?,
                errorResponse: WebResourceResponse?
            ) {
                Log.e(
                    logTag,
                    "onReceivedHttpError status=${errorResponse?.statusCode} reason=${errorResponse?.reasonPhrase} url=${request?.url}"
                )
                super.onReceivedHttpError(view, request, errorResponse)
            }

            override fun onReceivedSslError(
                view: WebView?,
                handler: SslErrorHandler?,
                error: SslError?
            ) {
                Log.e(logTag, "onReceivedSslError error=$error url=${error?.url}")
                super.onReceivedSslError(view, handler, error)
            }

            override fun onRenderProcessGone(
                view: WebView?,
                detail: RenderProcessGoneDetail?
            ): Boolean {
                Log.e(
                    logTag,
                    "onRenderProcessGone didCrash=${detail?.didCrash()} priorityAtExit=${detail?.rendererPriorityAtExit()}"
                )
                return super.onRenderProcessGone(view, detail)
            }
        }
    }
}
