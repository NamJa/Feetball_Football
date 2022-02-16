package com.example.feetballfootball

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient


private const val TAG = "NewsFragment"
class NewsFragment : Fragment() {

    private lateinit var webView: WebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_news, container, false)
        initView(view)

        webView.apply {
            settings.javaScriptEnabled = true
            webViewClient = WebViewClient()
        }
        webView.loadUrl("https://sports.news.naver.com/wfootball/index")

        return view
    }

    private fun initView(view: View) {
        webView = view.findViewById(R.id.webview)
    }



    companion object {
        @JvmStatic
        fun newInstance() = NewsFragment()
    }
}