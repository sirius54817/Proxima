package com.sirius.proxima.sis

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.webkit.CookieManager
import android.webkit.URLUtil
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sirius.proxima.di.ServiceLocator
import com.sirius.proxima.ui.theme.ProximaTheme
import com.sirius.proxima.ui.theme.ThemeMode

private const val SIS_EXTERNAL_BROWSER_URL = "https://sis.kalasalingam.ac.in/"

class SisBrowserActivity : ComponentActivity() {

    companion object {
        private const val EXTRA_REGISTER_NO = "extra_register_no"
        private const val EXTRA_PASSWORD = "extra_password"
        private const val EXTRA_START_URL = "extra_start_url"
        private const val DEFAULT_START_URL = "https://sis.kalasalingam.ac.in/login"

        fun createIntent(
            context: Context,
            registerNo: String,
            password: String,
            startUrl: String = DEFAULT_START_URL
        ): Intent {
            return Intent(context, SisBrowserActivity::class.java)
                .putExtra(EXTRA_REGISTER_NO, registerNo)
                .putExtra(EXTRA_PASSWORD, password)
                .putExtra(EXTRA_START_URL, startUrl)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val registerNo = intent.getStringExtra(EXTRA_REGISTER_NO).orEmpty()
        val password = intent.getStringExtra(EXTRA_PASSWORD).orEmpty()
        val startUrl = intent.getStringExtra(EXTRA_START_URL).orEmpty().ifBlank { DEFAULT_START_URL }
        val settingsDataStore = ServiceLocator.getSettingsDataStore(applicationContext)

        setContent {
            val appThemeMode = settingsDataStore.themeMode.collectAsStateWithLifecycle(initialValue = ThemeMode.SYSTEM).value
            val useMaterial3 = settingsDataStore.useMaterial3.collectAsStateWithLifecycle(initialValue = false).value
            val useMaterialYou = settingsDataStore.useMaterialYou.collectAsStateWithLifecycle(initialValue = false).value

            ProximaTheme(
                themeMode = appThemeMode,
                useMaterial3 = useMaterial3,
                useMaterialYou = useMaterialYou
            ) {
                SisBrowserScreen(
                    registerNo = registerNo,
                    password = password,
                    startUrl = startUrl,
                    onClose = { finish() }
                )
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun SisBrowserScreen(
    registerNo: String,
    password: String,
    startUrl: String,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var currentUrl by remember { mutableStateOf(startUrl) }

    BackHandler {
        val webView = webViewRef
        if (webView != null && webView.canGoBack()) {
            webView.goBack()
        } else {
            onClose()
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = currentUrl,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        runCatching {
                            // Open the base SIS site in external browser without passing in-app session state.
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(SIS_EXTERNAL_BROWSER_URL)))
                        }.onFailure {
                            Toast.makeText(context, "No browser app found", Toast.LENGTH_SHORT).show()
                        }
                    }) {
                        Icon(Icons.Default.OpenInBrowser, contentDescription = "Open in browser app")
                    }
                    IconButton(onClick = { webViewRef?.reload() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Reload")
                    }
                }
            )
        }
    ) { padding ->
        AndroidView(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            factory = { webContext ->
                WebView(webContext).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                    settings.userAgentString = "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36"

                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView, url: String) {
                            currentUrl = url
                            if (url.contains("/login") && registerNo.isNotBlank() && password.isNotBlank()) {
                                val escapedReg = jsString(registerNo)
                                val escapedPwd = jsString(password)
                                view.evaluateJavascript(
                                    """
                                    (() => {
                                        const registerInput = document.querySelector('[name=register_no]');
                                        const passwordInput = document.querySelector('[name=password]');
                                        const submitBtn = document.querySelector('[type=submit]');
                                        if (!registerInput || !passwordInput || !submitBtn) return;
                                        registerInput.value = $escapedReg;
                                        passwordInput.value = $escapedPwd;
                                        const form = submitBtn.form || document.querySelector('form');
                                        if (form) form.submit(); else submitBtn.click();
                                    })();
                                    """.trimIndent(),
                                    null
                                )
                            }
                        }
                    }

                    setDownloadListener { url, userAgent, contentDisposition, mimeType, _ ->
                        enqueueDownload(context, url, userAgent, contentDisposition, mimeType)
                    }

                    loadUrl(startUrl)
                    webViewRef = this
                }
            },
            update = { webViewRef = it }
        )
    }
}

private fun enqueueDownload(
    context: Context,
    url: String,
    userAgent: String?,
    contentDisposition: String?,
    mimeType: String?
) {
    runCatching {
        val request = DownloadManager.Request(Uri.parse(url))
        val fileName = URLUtil.guessFileName(url, contentDisposition, mimeType)
        val cookie = CookieManager.getInstance().getCookie(url)

        request.setTitle(fileName)
        request.setDescription("Downloading from SIS")
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        request.setMimeType(mimeType)
        request.setAllowedOverMetered(true)
        request.setAllowedOverRoaming(true)

        if (!cookie.isNullOrBlank()) {
            request.addRequestHeader("Cookie", cookie)
        }
        if (!userAgent.isNullOrBlank()) {
            request.addRequestHeader("User-Agent", userAgent)
        }

        runCatching {
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
        }.getOrElse {
            request.setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, fileName)
        }

        val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        manager.enqueue(request)
        Toast.makeText(context, "Download started", Toast.LENGTH_SHORT).show()
    }.onFailure {
        Toast.makeText(context, "Download failed", Toast.LENGTH_SHORT).show()
    }
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




