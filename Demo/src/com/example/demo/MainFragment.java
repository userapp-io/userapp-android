package com.example.demo;

import io.userapp.client.android.User;
import io.userapp.client.android.UserApp;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class MainFragment extends Fragment {
	UserApp.Session session;
	
	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        final View view = inflater.inflate(R.layout.fragment_main, container, false);
        
		// Listen for the login event so we could update the UI
        session = new UserApp.Session(this.getActivity(), new UserApp.Session.StatusCallback() {
		    @Override
		    public void call(Boolean authenticated, Exception exception) {
		        if (authenticated) {
		        	if (session.hasPermission("admin")) {
		        		((TextView) view.findViewById(R.id.welcome_text)).setText("Welcome Admin, " + session.user.first_name);
		        	} else {
		        		((TextView) view.findViewById(R.id.welcome_text)).setText("Welcome " + session.user.first_name);
		        	}
		        	
		        	// Change name and save
		        	session.user.first_name = "John";
		        	session.saveUser(session.user, new UserApp.Session.UserCallback() {
		    		    @Override
		    		    public void call(User user, Exception exception) {
		    		        if (exception == null) {
		    		        	System.out.println("USER SAVED");
		    		        } else {
		    		        	System.out.println("ERROR SAVING USER: " + exception.getMessage());
		    		        }
		    		    }
		    		});
		        }
		    }
		});
        
		return view;
    }

	@Override
	public void onResume() {
	    super.onResume();
	    session.onResume();
	}

	@Override
	public void onPause() {
	    super.onPause();
	    session.onPause();
	}
}
