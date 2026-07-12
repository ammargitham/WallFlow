package com.ammar.wallflow.ui.screens.redditauth

import android.annotation.SuppressLint
import android.view.View
import android.webkit.CookieManager
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.webkit.WebViewClientCompat
import com.ammar.wallflow.R
import com.ammar.wallflow.navigation.AppNavGraphs.RootNavGraph
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.ramcosta.composedestinations.spec.DestinationStyle

private const val REDDIT_LOGIN_URL = "https://www.reddit.com/login"
private val DIALOG_MAX_HEIGHT = 540.dp

@Destination<RootNavGraph>(
    style = DestinationStyle.Dialog::class,
)
@Composable
fun RedditAuthScreen(
    viewModel: RedditAuthViewModel = hiltViewModel(),
    navigator: DestinationsNavigator,
) {
    RedditAuthDialogContent(
        initialUrl = REDDIT_LOGIN_URL,
        onDone = { cookie ->
            viewModel.saveCookie(cookie)
            navigator.navigateUp()
        },
        onBack = { navigator.navigateUp() },
    )
}

@Composable
private fun RedditAuthDialogContent(
    initialUrl: String,
    onDone: (cookie: String) -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = Modifier
                .widthIn(max = 540.dp)
                .heightIn(max = DIALOG_MAX_HEIGHT, min = 480.dp)
                .align(Alignment.Center),
            shape = AlertDialogDefaults.shape,
            color = AlertDialogDefaults.containerColor,
            tonalElevation = AlertDialogDefaults.TonalElevation,
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = onBack) {
                        Text(text = stringResource(R.string.back))
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    Text(
                        text = stringResource(R.string.reddit_login),
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    TextButton(onClick = {
                        val cookies = CookieManager.getInstance().getCookie("https://www.reddit.com")
                        val sessionCookie = parseSessionCookie(cookies)
                        onDone(sessionCookie)
                    }) {
                        Text(text = stringResource(R.string.confirm))
                    }
                }

                HorizontalDivider()

                RedditWebView(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(max = 336.dp)
                        .fillMaxWidth()
                        .padding(8.dp),
                    context = context,
                    initialUrl = initialUrl,
                )
            }
        }
    }
}

@Composable
private fun RedditWebView(
    modifier: Modifier = Modifier,
    context: android.content.Context,
    initialUrl: String,
) {
    val webView = remember {
        WebView(context).apply {
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                loadWithOverviewMode = true
                useWideViewPort = true
                setSupportZoom(true)
                builtInZoomControls = true
                displayZoomControls = false
                cacheMode = WebSettings.LOAD_DEFAULT
                mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            }
            webViewClient = object : WebViewClientCompat() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    view?.visibility = View.VISIBLE
                    view?.requestFocus()
                }
            }
            visibility = View.GONE
            loadUrl(initialUrl)
            isFocusable = true
            isFocusableInTouchMode = true
        }
    }

    val focusReq = FocusRequester()

    AndroidView(
        modifier = modifier.focusRequester(focusReq),
        factory = { webView },
        update = { wv ->
            if (wv.url != initialUrl) {
                wv.loadUrl(initialUrl)
            }
            wv.requestFocus()
        },
    )

    LaunchedEffect(Unit) {
        focusReq.requestFocus()
    }
}

@SuppressLint("ObsoleteSdkInt")
private fun parseSessionCookie(cookies: String?): String {
    if (cookies.isNullOrEmpty()) return ""
    val parts = cookies.split(";")
    for (part in parts) {
        val trimmed = part.trim()
        if (trimmed.startsWith("session=", ignoreCase = true)) {
            return trimmed.substringAfter("=").trim()
        }
    }
    return cookies
}

