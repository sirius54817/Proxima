package com.sirius.proxima.data.sis

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject
import java.util.concurrent.Semaphore
import kotlin.coroutines.resume

class WebViewSisSessionProvider(
    context: Context
) : SisSessionProvider {

    private companion object {
        private const val BASE_URL = "https://sis.kalasalingam.ac.in"
        private const val LOGIN_URL = "$BASE_URL/login"
    }

    private val appContext = context.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())
    private var sessionWebView: WebView? = null
    private val fetchGate = Semaphore(1, true)
    private val queuedFetches = ArrayDeque<QueuedFetch>()

    private data class QueuedFetch(
        val targetUrl: String,
        val headers: Map<String, String>,
        val onDone: (SisResult<String>) -> Unit
    )

    private fun debug(message: String) {
        val line = "[SIS-WebView] $message"
        Log.d("SISWebView", line)
        SisWebViewDebugLog.add(line)
    }

    override suspend fun login(registerNo: String, password: String): SisResult<Unit> {
        debug("login() start, regNoLen=${registerNo.length}")
        val completed = withTimeoutOrNull(45_000L) {
            suspendCancellableCoroutine<SisResult<Unit>> { continuation ->
                mainHandler.post {
                    runLoginFlow(registerNo, password) { result ->
                        if (continuation.isActive) {
                            continuation.resume(result)
                        }
                    }
                }
            }
        }

        return (completed ?: SisResult.Error("SIS login timed out. Please try again.")).also {
            when (it) {
                is SisResult.Success -> debug("login() completed: success")
                is SisResult.Error -> debug("login() completed: error=${it.message}")
            }
        }
    }

    override suspend fun fetch(url: String, headers: Map<String, String>): SisResult<String> {
        val targetUrl = normalizeUrl(url)
            ?: return SisResult.Error("Invalid SIS URL: $url")

        debug("fetch() start url=$targetUrl headers=${headers.keys.joinToString()}")

        val completed = withTimeoutOrNull(35_000L) {
            suspendCancellableCoroutine<SisResult<String>> { continuation ->
                mainHandler.post {
                    runFetchFlow(targetUrl, headers) { result ->
                        if (continuation.isActive) {
                            continuation.resume(result)
                        }
                    }
                }
            }
        }

        return (completed ?: SisResult.Error("SIS request timed out. Please try again.")).also {
            when (it) {
                is SisResult.Success -> debug("fetch() completed: success bytes=${it.data.length}")
                is SisResult.Error -> debug("fetch() completed: error=${it.message}")
            }
        }
    }

    override suspend fun clearSession() {
        debug("clearSession() invoked")
        suspendCancellableCoroutine<Unit> { continuation ->
            mainHandler.post {
                failQueuedFetches("Session cleared. Please login again.")
                sessionWebView?.destroy()
                sessionWebView = null
                val cookieManager = CookieManager.getInstance()
                cookieManager.removeAllCookies {
                    cookieManager.flush()
                    debug("clearSession() cookies cleared")
                    if (continuation.isActive) continuation.resume(Unit)
                }
            }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun runLoginFlow(
        registerNo: String,
        password: String,
        onDone: (SisResult<Unit>) -> Unit
    ) {
        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)
        debug("runLoginFlow() load=$LOGIN_URL")

        // Replace any previous session WebView with a fresh login session.
        failQueuedFetches("Session refreshed. Please retry SIS fetch.")
        sessionWebView?.destroy()
        sessionWebView = null

        var finished = false
        var loginSubmittedAtMs = 0L
        var pollRunnable: Runnable? = null

        val webView = WebView(appContext)

        fun complete(result: SisResult<Unit>, webView: WebView?) {
            if (finished) return
            finished = true
            pollRunnable?.let(mainHandler::removeCallbacks)
            webView?.stopLoading()
            cookieManager.flush()
            when (result) {
                is SisResult.Success -> {
                    sessionWebView = webView
                    debug("runLoginFlow() complete success (session WebView retained)")
                }
                is SisResult.Error -> {
                    webView?.webViewClient = WebViewClient()
                    webView?.destroy()
                    debug("runLoginFlow() complete error=${result.message}")
                }
            }
            onDone(result)
        }

        pollRunnable = object : Runnable {
            override fun run() {
                if (finished) return

                val currentUrl = webView.url.orEmpty()
                val cookies = cookieManager.getCookie(BASE_URL).orEmpty()
                if (!currentUrl.contains("/login") && cookies.isNotBlank()) {
                    debug("login poll success url=$currentUrl cookieLen=${cookies.length}")
                    complete(SisResult.Success(Unit), webView)
                    return
                }

                if (loginSubmittedAtMs > 0L) {
                    val elapsed = System.currentTimeMillis() - loginSubmittedAtMs
                    if (elapsed > 25_000L) {
                        debug("login poll timeout elapsedMs=$elapsed url=$currentUrl")
                        complete(
                            SisResult.Error("SIS login did not complete. Please check credentials or portal availability."),
                            webView
                        )
                        return
                    }
                }

                mainHandler.postDelayed(this, 1000L)
            }
        }
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            userAgentString = "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36"
            allowFileAccess = false
            allowContentAccess = false
            mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
        }

        var loginSubmitted = false

        fun checkLoginState(view: WebView, attemptsLeft: Int) {
            if (finished) return
            view.evaluateJavascript(
                """
                (() => {
                    const bodyText = (document.body && document.body.innerText) ? document.body.innerText : '';
                    const hasRegister = !!document.querySelector('[name=register_no]');
                    const hasPassword = !!document.querySelector('[name=password]');
                    const hasInvalid = /invalid|incorrect|wrong|failed/i.test(bodyText);
                    return JSON.stringify({
                        href: String(window.location.href || ''),
                        hasLoginForm: hasRegister && hasPassword,
                        hasInvalid
                    });
                })();
                """.trimIndent()
            ) { rawResult ->
                if (finished) return@evaluateJavascript
                val parsed = rawResult?.trim()?.removeSurrounding("\"")?.replace("\\\"", "\"")
                val json = runCatching { JSONObject(parsed ?: "") }.getOrNull()
                val href = json?.optString("href").orEmpty()
                val hasLoginForm = json?.optBoolean("hasLoginForm") == true
                val hasInvalid = json?.optBoolean("hasInvalid") == true
                debug("checkLoginState href=$href hasLoginForm=$hasLoginForm hasInvalid=$hasInvalid attemptsLeft=$attemptsLeft")

                if (href.isNotBlank() && !href.contains("/login")) {
                    complete(SisResult.Success(Unit), webView)
                    return@evaluateJavascript
                }

                if (hasInvalid) {
                    complete(SisResult.Error("Invalid register number or password."), webView)
                    return@evaluateJavascript
                }

                if (!hasLoginForm) {
                    // If form disappeared but no URL change yet, wait briefly for redirect completion.
                    if (attemptsLeft > 0) {
                        mainHandler.postDelayed({ checkLoginState(view, attemptsLeft - 1) }, 1000L)
                    } else {
                        complete(SisResult.Error("SIS login could not be confirmed. Please retry."), webView)
                    }
                    return@evaluateJavascript
                }

                if (attemptsLeft > 0) {
                    mainHandler.postDelayed({ checkLoginState(view, attemptsLeft - 1) }, 1000L)
                } else {
                    complete(
                        SisResult.Error("SIS login did not complete. The portal may require additional verification."),
                        webView
                    )
                }
            }
        }

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                return !isAllowedUrl(request.url)
            }

            override fun onPageFinished(view: WebView, url: String) {
                if (finished) return
                debug("onPageFinished(login) url=$url")

                if (!isAllowedUrl(Uri.parse(url))) {
                    complete(SisResult.Error("Unexpected redirect while logging in."), webView)
                    return
                }

                if (url.contains("/login") && !loginSubmitted) {
                    loginSubmitted = true
                    loginSubmittedAtMs = System.currentTimeMillis()
                    debug("login form submit triggered")
                    val escapedRegisterNo = jsString(registerNo)
                    val escapedPassword = jsString(password)
                    view.evaluateJavascript(
                        """
                        (() => {
                            const registerInput = document.querySelector('[name=register_no]');
                            const passwordInput = document.querySelector('[name=password]');
                            const submitBtn = document.querySelector('[type=submit]');
                            if (!registerInput || !passwordInput || !submitBtn) return 'FORM_NOT_FOUND';
                            registerInput.value = $escapedRegisterNo;
                            passwordInput.value = $escapedPassword;
                            const form = submitBtn.form || document.querySelector('form');
                            if (form) {
                                form.submit();
                            } else {
                                submitBtn.click();
                            }
                            return 'SUBMITTED';
                        })();
                        """.trimIndent(),
                        { submitResult ->
                            val status = submitResult
                                ?.trim()
                                ?.removeSurrounding("\"")
                                .orEmpty()
                            debug("login submit JS status=$status")
                            if (status == "FORM_NOT_FOUND") {
                                complete(SisResult.Error("SIS login form not found. The portal page may have changed."), webView)
                            } else {
                                pollRunnable?.let(mainHandler::post)
                                mainHandler.postDelayed({ checkLoginState(view, 12) }, 1200L)
                            }
                        }
                    )
                    return
                }

                if (!url.contains("/login")) {
                    val cookies = cookieManager.getCookie(BASE_URL).orEmpty()
                    debug("post-login url transition url=$url cookieLen=${cookies.length}")
                    if (cookies.isBlank()) {
                        complete(SisResult.Error("Login completed but no session cookie was found."), webView)
                    } else {
                        complete(SisResult.Success(Unit), webView)
                    }
                } else if (loginSubmitted) {
                    // After submit, keep checking page state before deciding this is a failure.
                    pollRunnable?.let(mainHandler::post)
                    mainHandler.postDelayed({ checkLoginState(view, 8) }, 800L)
                }
            }

            override fun onReceivedError(
                view: WebView,
                request: WebResourceRequest,
                error: android.webkit.WebResourceError
            ) {
                if (request.isForMainFrame) {
                    debug("onReceivedError(login) code=${error.errorCode} desc=${error.description}")
                    complete(SisResult.Error("WebView login failed: ${error.description}"), webView)
                }
            }
        }

        // Global guard to avoid leaving the coroutine unresolved.
        mainHandler.postDelayed({
            if (!finished) {
                debug("runLoginFlow() global watchdog timeout")
                complete(
                    SisResult.Error("SIS login timed out in WebView. Please try once more."),
                    webView
                )
            }
        }, 40_000L)

        webView.loadUrl(LOGIN_URL)
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun runFetchFlow(
        targetUrl: String,
        headers: Map<String, String>,
        onDone: (SisResult<String>) -> Unit
    ) {
        val request = QueuedFetch(targetUrl, headers, onDone)
        if (!fetchGate.tryAcquire()) {
            queuedFetches.addLast(request)
            debug("runFetchFlow() queued; inFlight=true queueSize=${queuedFetches.size}")
            return
        }

        runFetchFlowInternal(request)
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun runFetchFlowInternal(request: QueuedFetch) {
        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)
        val targetUrl = request.targetUrl
        val headers = request.headers
        val onDone = request.onDone

        var finished = false
        var timeoutRunnable: Runnable? = null
        var navigationFallbackStarted = false
        fun complete(result: SisResult<String>) {
            if (finished) return
            finished = true
            timeoutRunnable?.let(mainHandler::removeCallbacks)
            cookieManager.flush()
            when (result) {
                is SisResult.Success -> debug("runFetchFlow() complete success bytes=${result.data.length}")
                is SisResult.Error -> debug("runFetchFlow() complete error=${result.message}")
            }
            onDone(result)
            if (fetchGate.availablePermits() == 0) {
                fetchGate.release()
            }
            val next = queuedFetches.removeFirstOrNull()
            if (next != null) {
                debug("runFetchFlow() dequeued next request; remaining=${queuedFetches.size}")
                runFetchFlow(next.targetUrl, next.headers, next.onDone)
            }
        }

        val webView = sessionWebView
        if (webView == null) {
            debug("runFetchFlow() failed: no active session WebView")
            complete(SisResult.Error("No active SIS session. Please login again."))
            return
        }

        if (!isAllowedUrl(Uri.parse(targetUrl))) {
            complete(SisResult.Error("Blocked SIS URL: $targetUrl"))
            return
        }

        val currentUrl = webView.url.orEmpty()
        if (currentUrl.isBlank() || currentUrl.contains("/login")) {
            sessionWebView = null
            complete(SisResult.Error("Session expired. Please login again."))
            return
        }

        debug("runFetchFlow() using retained session WebView url=$currentUrl")

        val headersJs = headers.entries.joinToString(",") { (k, v) ->
            "${jsString(k)}:${jsString(v)}"
        }
        val js = """
            (() => {
              try {
                const xhr = new XMLHttpRequest();
                xhr.open('GET', ${jsString(targetUrl)}, false);
                xhr.withCredentials = true;
                const headers = { $headersJs };
                Object.keys(headers).forEach((key) => {
                  const lower = key.toLowerCase();
                  if (lower === 'referer' || lower === 'origin' || lower === 'host' || lower === 'cookie' || lower === 'content-length') {
                    return;
                  }
                  try {
                    xhr.setRequestHeader(key, headers[key]);
                  } catch (_e) {
                  }
                });
                xhr.send(null);
                return JSON.stringify({
                  ok: true,
                  href: String(window.location.href || ''),
                  status: xhr.status,
                  text: String(xhr.responseText || ''),
                  html: ''
                });
              } catch (e) {
                return JSON.stringify({
                  ok: false,
                  error: String(e),
                  href: String(window.location.href || ''),
                  status: 0,
                  text: '',
                  html: ''
                });
              }
            })();
        """.trimIndent()

        debug("runFetchFlow() injecting XHR JS into retained session")
        webView.evaluateJavascript(js) { rawResult ->
            if (finished) return@evaluateJavascript
            val decoded = decodeEvaluateJavascriptResult(rawResult)
            val payload = runCatching { JSONObject(decoded) }.getOrNull()
            if (payload == null) {
                complete(SisResult.Error("Failed to parse WebView XHR result."))
                return@evaluateJavascript
            }

            val ok = payload.optBoolean("ok")
            val status = payload.optInt("status", 0)
            val text = payload.optString("text")
            val error = payload.optString("error")
            debug("runFetchFlow() JS result ok=$ok status=$status textLen=${text.length}")

            if (!ok) {
                complete(SisResult.Error(if (error.isNotBlank()) error else "WebView XHR failed."))
                return@evaluateJavascript
            }

            val extracted = extractPayloadBody(decoded)
            if (extracted.isBlank()) {
                complete(SisResult.Error("WebView returned empty response body."))
            } else {
                complete(SisResult.Success(extracted))
            }
        }

        fun startNavigationFallback() {
            if (finished || navigationFallbackStarted) return
            navigationFallbackStarted = true
            debug("runFetchFlow() switching to navigation fallback for url=$targetUrl")
            webView.loadUrl(targetUrl)

            fun pollLoadedPage(attemptsLeft: Int) {
                if (finished) return
                val js = """
                    (() => {
                      const href = String(window.location.href || '');
                      const ready = String(document.readyState || '');
                      const text = (document.body && document.body.innerText) ? document.body.innerText : '';
                      const html = document.documentElement ? document.documentElement.outerHTML : '';
                      return JSON.stringify({ href, ready, text, html });
                    })();
                """.trimIndent()

                webView.evaluateJavascript(js) { raw ->
                    if (finished) return@evaluateJavascript
                    val decoded = decodeEvaluateJavascriptResult(raw)
                    val payload = runCatching { JSONObject(decoded) }.getOrNull()
                    if (payload == null) {
                        if (attemptsLeft > 0) {
                            mainHandler.postDelayed({ pollLoadedPage(attemptsLeft - 1) }, 1000L)
                        } else {
                            complete(SisResult.Error("Navigation fallback failed: invalid page payload."))
                        }
                        return@evaluateJavascript
                    }

                    val href = payload.optString("href")
                    val ready = payload.optString("ready")
                    val text = payload.optString("text")
                    val html = payload.optString("html")
                    val onTarget = href.contains("sis.kalasalingam.ac.in") && href.contains("attendance-details")
                    debug("runFetchFlow() fallback poll href=$href ready=$ready textLen=${text.length} attemptsLeft=$attemptsLeft")

                    if (onTarget && ready.equals("complete", ignoreCase = true) && (text.isNotBlank() || html.isNotBlank())) {
                        val extracted = extractPayloadBody(decoded)
                        if (extracted.isBlank()) {
                            complete(SisResult.Error("Navigation fallback returned empty body."))
                        } else {
                            complete(SisResult.Success(extracted))
                        }
                        return@evaluateJavascript
                    }

                    if (attemptsLeft > 0) {
                        mainHandler.postDelayed({ pollLoadedPage(attemptsLeft - 1) }, 1000L)
                    } else {
                        complete(SisResult.Error("SIS request timed out. Please try again."))
                    }
                }
            }

            pollLoadedPage(20)
        }

        timeoutRunnable = Runnable {
            if (!finished) {
                debug("runFetchFlow() timeout while waiting for JS result")
                startNavigationFallback()
            }
        }
        mainHandler.postDelayed(timeoutRunnable!!, 30_000L)
    }

    private fun failQueuedFetches(message: String) {
        if (queuedFetches.isEmpty()) return
        val pending = queuedFetches.toList()
        queuedFetches.clear()
        debug("failing ${pending.size} queued fetch(es): $message")
        pending.forEach { it.onDone(SisResult.Error(message)) }
    }

    private fun decodeEvaluateJavascriptResult(rawResult: String?): String {
        if (rawResult == null || rawResult == "null") return ""
        return runCatching {
            JSONObject("{\"v\":$rawResult}").optString("v")
        }.getOrElse {
            rawResult
                .removeSurrounding("\"")
                .replace("\\\"", "\"")
                .replace("\\n", "\n")
                .replace("\\r", "\r")
                .replace("\\t", "\t")
                .replace("\\\\", "\\")
        }
    }

    private fun extractPayloadBody(payload: String): String {
        val parsed = runCatching { JSONObject(payload) }.getOrNull()
        if (parsed == null) return payload

        val text = parsed.optString("text").trim()
        val html = parsed.optString("html").trim()

        if (text.startsWith("{") || text.startsWith("[")) return text
        if (text.isNotBlank() && !text.startsWith("{")) return text
        if (html.isNotBlank()) return html
        return text
    }

    private fun isAllowedUrl(uri: Uri): Boolean {
        val host = uri.host?.lowercase() ?: return false
        return host == "student.kalasalingam.ac.in" || host.endsWith(".kalasalingam.ac.in")
    }

    private fun normalizeUrl(url: String): String? {
        if (url.isBlank()) return null
        if (url.startsWith("http://") || url.startsWith("https://")) {
            val uri = Uri.parse(url)
            return if (isAllowedUrl(uri)) url else null
        }
        return "$BASE_URL${if (url.startsWith('/')) url else "/$url"}"
    }

    private fun jsString(value: String): String {
        return buildString {
            append('"')
            value.forEach { ch ->
                when (ch) {
                    '\\' -> append("\\\\")
                    '"' -> append("\\\"")
                    '\n' -> append("\\n")
                    '\r' -> append("\\r")
                    '\t' -> append("\\t")
                    else -> append(ch)
                }
            }
            append('"')
        }
    }

}





