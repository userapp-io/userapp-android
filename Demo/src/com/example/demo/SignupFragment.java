package com.example.demo;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import io.userapp.client.android.AuthFragment;
import io.userapp.client.android.Property;
import io.userapp.client.android.Subscription;
import io.userapp.client.android.User;

public class SignupFragment extends AuthFragment {
	
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_signup, container, false);
        
        // Setup the login form with bindings to UserApp
        super.setupSignupForm(view, R.id.signup_button, R.id.email, R.id.login, R.id.password);
        
        return view;
    }
    
    @Override
	public User onSignupStart(User user) {
    	// Show loader when waiting for server
    	getView().findViewById(R.id.signup_form).setVisibility(View.GONE);
    	getView().findViewById(R.id.signup_status).setVisibility(View.VISIBLE);
    	
    	// Set subscription
    	user.subscription = new Subscription();
    	user.subscription.price_list_id = "IEiVxVpxSxy1xE8oIzsSeA";
    	user.subscription.plan_id = "CBPJdFORQ-qsefa7LxrX-A";
    	
    	// Set age property
    	user.properties.put("age", new Property(42, true));
    
    	// Return the user to complete the signup
    	return user;
    }
    
	@Override
	public void onSignupCompleted(User user, Boolean verified, Exception exception) {
		if (exception != null) {
			// Hide the loader
			getView().findViewById(R.id.signup_form).setVisibility(View.VISIBLE);
			getView().findViewById(R.id.signup_status).setVisibility(View.GONE);
			
			// Show an error message
			((TextView) getView().findViewById(R.id.error_text)).setText(exception.getMessage());
		} else {
			// Clear the message
			((TextView) getView().findViewById(R.id.error_text)).setText("");
			
			// Need to verify the email address?
			if (verified == false) {
				// Hide the loader
				getView().findViewById(R.id.signup_form).setVisibility(View.VISIBLE);
				getView().findViewById(R.id.signup_status).setVisibility(View.GONE);
				
				((TextView) getView().findViewById(R.id.error_text)).setText("An email has been sent to your inbox.");
			} else {
				// Call the superclass to log in the signed up user
				super.onSignupCompleted(user, verified, exception);
			}
		}
	}
    
}
