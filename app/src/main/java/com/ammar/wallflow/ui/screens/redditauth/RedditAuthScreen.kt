package com.ammar.wallflow.ui.screens.redditauth

import android.annotation.SuppressLint
import android.view.View
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.view.isVisible
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.webkit.WebViewClientCompat
import com.ammar.wallflow.R
import com.ammar.wallflow.navigation.AppNavGraphs.RootNavGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.ramcosta.composedestinations.spec.DestinationStyle
import com.ramcosta.composedestinations.annotation.Destination

private const val REDDIT_LOGIN_URL = "https://www.reddit.com/login"

@Destination<RootNavGraph>(
    style = DestinationStyle.Dialog::class,
)
@Composable
fun RedditAuthScreen(
    viewModel: RedditAuthViewModel = hiltViewModel(),
    navigator: DestinationsNavigator,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    RedditAuthDialogContent(
        initialUrl = REDDIT_LOGIN_URL,
        isLoggedIn = uiState.redditCookie.isNotBlank(),
        onDone = { cookie ->
            viewModel.saveCookie(cookie)
            navigator.navigateUp()
        },
        onBack = { navigator.navigateUp() },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RedditAuthDialogContent(
    initialUrl: String,
    isLoggedIn: Boolean,
    onDone: (cookie: String) -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(text = stringResource(R.string.reddit_login))
                },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text(text = stringResource(R.string.back))
                    }
                },
                actions = {
                    TextButton(onClick = {
                        val cookies = CookieManager.getInstance().getCookie("https://www.reddit.com")
                        val sessionCookie = parseSessionCookie(cookies)
                        onDone(sessionCookie)
                    }) {
                        Text(text = stringResource(R.string.confirm))
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { paddingValues ->
        RedditWebView(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            context = context,
            initialUrl = initialUrl,
        )
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
            }
            webViewClient = object : WebViewClientCompat() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    view?.visibility = View.VISIBLE
                }
            }
            visibility = View.GONE
            loadUrl(initialUrl)
        }
    }

    androidx.compose.ui.viewinterop.AndroidView(
        modifier = modifier,
        factory = { webView },
        update = { wv ->
            if (wv.url != initialUrl) {
                wv.loadUrl(initialUrl)
            }
        },
    )
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
