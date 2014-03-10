package com.example.demo;

import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import io.userapp.client.android.AuthFragment;

public class LoginFragment extends AuthFragment {
	
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_login, container, false);
        
        // Setup the login form with bindings to UserApp
        super.setupLoginForm(view, R.id.login, R.id.password, R.id.login_button);
        
        // Attach the Facebook button
        super.setupSocialLogin(view, R.id.facebook_button, "facebook");
        
        // Attach the signup button link
		Button signupButton = (Button) view.findViewById(R.id.show_signup);
		signupButton.setOnClickListener(new OnClickListener() {
		    @Override
		    public void onClick(View v) {
		    	FragmentTransaction transaction = getFragmentManager().beginTransaction();
		    	transaction.hide(getFragmentManager().findFragmentById(R.id.loginFragment));
			    transaction.show(getFragmentManager().findFragmentById(R.id.signupFragment));
			    transaction.addToBackStack(null);
			    transaction.commit();
		    }
		});
		
        return view;
    }
    
    @Override
    public Boolean onLoginStart(String login, String password, Boolean isSocialLogin) {
    	// Show loader when waiting for server
    	getView().findViewById(R.id.login_form).setVisibility(View.GONE);
    	getView().findViewById(R.id.login_status).setVisibility(View.VISIBLE);
    	
    	// Return true to complete the login
		return true;
	}
    
	@Override
	public void onLoginCompleted(Boolean authenticated, Exception exception) {
		// Hide the loader
		getView().findViewById(R.id.login_form).setVisibility(View.VISIBLE);
		getView().findViewById(R.id.login_status).setVisibility(View.GONE);
		
		if (exception != null) {
			// Show an error message
			((TextView) getView().findViewById(R.id.error_text)).setText(exception.getMessage());
		} else {
			// Clear the message
			((TextView) getView().findViewById(R.id.error_text)).setText("");
		}
	}
	
}
