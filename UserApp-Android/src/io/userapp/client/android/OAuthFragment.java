package io.userapp.client.android;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class OAuthFragment extends Fragment {

	private WebView webView;
	private UserApp.Session session = null;
	private UserApp.Session.StatusCallback callback = null;
	private String url = null;
	private FragmentActivity activity = null;
	private OAuthFragment self = this;
	
	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		WebViewRelativeLayout popWindow = new WebViewRelativeLayout(this.getActivity());
		
	    webView = popWindow.getTheWebView();
	    
	    webView.setWebViewClient(new WebViewClient() {
        	public void onPageStarted(WebView view, String url, Bitmap favicon) {
        		if (url.startsWith(UserApp.OAUTH_REDIRECT_URI)) {
        			// Hide the webview
    		    	FragmentTransaction transaction = activity.getSupportFragmentManager().beginTransaction();
    	            transaction.remove(self);
    	            transaction.commit();
        			
        			// Parse the token
        			Pattern regex = Pattern.compile("ua\\_token=([a-z0-9\\-\\_]+)$", Pattern.CASE_INSENSITIVE);
        			Matcher matcher = regex.matcher(url);
        			
        			if (matcher.find()) {
        		        String token = matcher.group(1);
        		        
        		        // Login using the token
        		        session.loginWithToken(token, callback);
        		    } else {
        		    	callback.call(false, new Exception("UserApp token is missing."));
        		    }
        		}
        	}
    	});
	    
	    if (this.url != null) {
	    	webView.loadUrl(url);
	    }
	    
        return popWindow;
    }
	
	public void authorize(String url, UserApp.Session.StatusCallback callback) {
    	this.url = url;
    	this.callback = callback;
    	
    	// Show the webview
    	FragmentTransaction transaction = this.activity.getSupportFragmentManager().beginTransaction();
        transaction.add(android.R.id.content, this, "oauth_fragment");
        transaction.addToBackStack(null);
        transaction.commit();
    }
	
	public void setSession(UserApp.Session session) {
    	this.session = session;
    }
	
	public UserApp.Session getSession() {
    	return this.session;
    }
	
	public void setActivity(FragmentActivity activity) {
    	this.activity = activity;
    }
	
}
