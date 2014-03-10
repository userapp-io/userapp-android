package io.userapp.client.android;

import java.util.ArrayList;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.support.v4.app.Fragment;

public class AuthFragment extends Fragment {
	private EditText loginView = null;
	private EditText passwordView = null;
	private ArrayList<EditText> signupViews = null;
	private String password = "";
	private OAuthFragment oauthFragment = null;
	public UserApp.Session session = null;
	
	public AuthFragment() {}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		session = new UserApp.Session(getActivity());
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
	
	/** Setup the login form with all bindings */
	public void setupLoginForm(View view, int loginViewId, int passwordViewId, int loginButtonViewId) {
		this.loginView = (EditText) view.findViewById(loginViewId);
		this.passwordView = (EditText) view.findViewById(passwordViewId);
		
		// Attach the login button
		Button button = (Button) view.findViewById(loginButtonViewId);
	    button.setOnClickListener(new OnClickListener() {
	        @Override
	        public void onClick(View v) {
	        	onLogin(v);
	        }
	    });
	}
	
	/** Setup the signup form with all bindings */
	public void setupSignupForm(View view, int signupButtonViewId, int ... viewIds) {
		this.signupViews = new ArrayList<EditText>();
		
		for (int i = 0; i < viewIds.length; ++i) {
			this.signupViews.add((EditText) view.findViewById(viewIds[i]));
		}
		
		// Attach the signup button
		Button button = (Button) view.findViewById(signupButtonViewId);
	    button.setOnClickListener(new OnClickListener() {
	        @Override
	        public void onClick(View v) {
	        	onSignup(v);
	        }
	    });
	}
	
	/** Setup social login (OAuth) */
	public void setupSocialLogin(View view, int oauthButtonViewId, final String providerId) {
		this.setupSocialLogin(view, oauthButtonViewId, providerId, "");
	}
	public void setupSocialLogin(View view, int oauthButtonViewId, final String providerId, final String scopes) {
		Button button = (Button) view.findViewById(oauthButtonViewId);
		
		// Attach the social login button
	    button.setOnClickListener(new OnClickListener() {
	        @Override
	        public void onClick(View v) {
	        	// Call the onLoginStart event
				if (onLoginStart("", "", true) == true) {
					// Get OAuth url
		        	session.getOAuthUrl(providerId, UserApp.OAUTH_REDIRECT_URI, scopes, new UserApp.Session.OAuthUrlCallback() {
						@Override
						public void call(String url, Exception exception) {
							if (exception == null) {
								if (oauthFragment == null) {
						        	oauthFragment = new OAuthFragment();
					        	}
					        	
								oauthFragment.setActivity(getActivity());
								oauthFragment.setSession(session);
								oauthFragment.authorize(url, new UserApp.Session.StatusCallback() {
								    @Override
								    public void call(Boolean authenticated, Exception exception) {
								    	onLoginCompleted(authenticated, exception);
								    }
								});
							} else {
								// Error getting OAuth url
								onLoginCompleted(false, exception);
							}
						}
					});
				}
	        }
	    });
	}
	
	/** Method to login from a layout */
	public void onLogin(View view) {
		if (loginView == null || passwordView == null) {
			onLoginCompleted(false, new Exception("Login and password view must be set."));
		} else {
			String login = loginView.getText().toString();
			String password = passwordView.getText().toString();
			
			// Clear the fields
			loginView.setText("");
			passwordView.setText("");
			
			// Call the onLoginStart event
			if (onLoginStart(login, password, false) == true) {
				// Start a new session
				session.login(login, password, new UserApp.Session.StatusCallback() {
				    @Override
				    public void call(Boolean authenticated, Exception exception) {
				    	onLoginCompleted(authenticated, exception);
				    }
				});
			}
		}
	}
	
	/** Login completed callback */
	public void onLoginCompleted(Boolean authenticated, Exception exception) {
		
	}
	
	/** Called when the login is about to start */
	public Boolean onLoginStart(String login, String password, Boolean isSocialLogin) {
		return true;
	}
	
	/** Method to signup from a layout */
	public void onSignup(View view) {
		if (this.signupViews == null) {
			onSignupCompleted(null, false, new Exception("The signup form doesn't contains any views."));
		} else {
			// Create a new user object
			User newUser = new User();			
						
			for (EditText field : this.signupViews) {
				String tag = (String)field.getTag();
				
				// Fill the user object
				if (tag.equals("login")) {
					newUser.login = field.getText().toString();
				} else if (tag.equals("email")) {
					newUser.email = field.getText().toString();
				} else if (tag.equals("first_name")) {
					newUser.first_name = field.getText().toString();
				} else if (tag.equals("last_name")) {
					newUser.last_name = field.getText().toString();
				} else if (tag.equals("password")) {
					newUser.password = field.getText().toString();
					this.password = newUser.password;
					
					// Clear the field
					field.setText("");
				}
			}
			
			newUser = onSignupStart(newUser);
			
			if (newUser != null) {
				// Save the new user
				session.saveUser(newUser, new UserApp.Session.UserCallback() {
				    @Override
				    public void call(User user, Exception exception) {
				    	user.password = password;
				    	password = "";
				    	onSignupCompleted(user, (user.locks.size() == 0), exception);
				    }
				});
			}
		}
	}
	
	/** Signup completed callback */
	public void onSignupCompleted(User user, Boolean verified, Exception exception) {
		if (verified && exception == null) {
			// Log in the signed up user
			session.login(user.login, user.password, new UserApp.Session.StatusCallback() {
			    @Override
			    public void call(Boolean authenticated, Exception exception) {
			    	onLoginCompleted(authenticated, exception);
			    }
			});
		}
	}
	
	/** Called when the user object is populated */
	public User onSignupStart(User user) {
		return user;
	}
		
}