package io.userapp.client.android;

import android.annotation.SuppressLint;
import android.content.Context;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

public class WebViewRelativeLayout extends RelativeLayout {

    private WebView webView;

    @SuppressLint("SetJavaScriptEnabled")
	public WebViewRelativeLayout(Context context) {
        super(context);
        
        webView = new WebView(context);
        webView.setId(0X100);
        webView.setScrollContainer(true);
        
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
        params.addRule(RelativeLayout.CENTER_IN_PARENT);
        webView.setLayoutParams(params);
       
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        
        addView(webView);
    }

    public WebView getTheWebView() {
        return webView;
    }
    
    public void loadUrl(String url) {
    	webView.loadUrl(url);
    }

}