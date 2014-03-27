package io.userapp.client.android;

import io.userapp.client.UserAppClient;
import io.userapp.client.UserAppClient.Result;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Set;
import java.util.UUID;
import com.google.gson.Gson;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.FragmentActivity;

public class UserApp {

	// Actions used to broadcast and receive changes in session state (login and logout)
	public static final String ACTION_SESSION_LOGIN = "io.userapp.client.android.SESSION_LOGIN";
	public static final String ACTION_SESSION_LOGOUT = "io.userapp.client.android.SESSION_LOGOUT";
	public static final String ACTION_USER_UPDATE = "io.userapp.client.android.USER_UPDATE";
	
	// Shared preference file key + keys
	public static final String PREFERENCE_KEY = "io.userapp.client.android.PREFERENCE_FILE_KEY";
	public static final String SESSION_TOKEN_KEY = "io.userapp.client.android.SESSION_TOKEN_KEY";
	public static final String TOKEN_ID_KEY = "io.userapp.client.android.TOKEN_ID_KEY";
	public static final String INSTALLATION_KEY = "io.userapp.client.android.INSTALLATION_KEY";
	public static final String USER_KEY = "io.userapp.client.android.USER_KEY";
	
	// URI to redirect to after an OAuth authorization
	public static final String OAUTH_REDIRECT_URI = "userapp-oauth:///";
	
	/** Print a message to the console if debugging is turned on */
	private static void log(String message) {
		//System.out.println(message);
	}
	
	/** 
	 * Class that keeps track of the user session by
	 * holding the current user and its state
	 */
	public static class Session {
		private FragmentActivity activity = null; // The activity the session lives in
		private boolean _isResumed = false; // Is the activity resumed or paused?
		private UserApp.UIHelper uiHelper = null; // The UIHelper that is attached to this session
		ArrayList<UserApp.Session.StatusCallback> callbacks = new ArrayList<UserApp.Session.StatusCallback>(); // Callbacks when the state changes (login/logout)
		ArrayList<UserApp.Session.UserCallback> userCallbacks = new ArrayList<UserApp.Session.UserCallback>(); // Callbacks when the user profile updates
		private LoginTask loginTask = null; // Async task to login
		private LogoutTask logoutTask = null; // Async task to end the session with UserApp
		private LoadUserTask loadUserTask = null; // Async task to reload the user profile
		private SaveUserTask saveUserTask = null; // Async task to save the user profile
		private GetOAuthUrlTask getOAuthUrlTask = null; // Async task to get an OAuth authorization url
		private SharedPreferences sharedPreferences = null; // Shared preferences to store state data (token, user object, etc.)
		public UserAppClient.API api; // Client API for UserApp
		private String installationKey = null; // Unique (UUID) key for this phone (used to name the token at UserApp)
		private Boolean isBroadcaster = false; // If this instance is the one broadcasting an event
		private Boolean isAuthenticated = false; // Is the session authenticated or not?
		public User user; // The logged in user
		public String token; // Session token
		
		/** Receiver for login events */
		private BroadcastReceiver loginReceiver = new BroadcastReceiver() {
		    @Override
		    public void onReceive(Context context, Intent intent) {
		    	String error = intent.getStringExtra("ERROR_MESSAGE");
		    	
		    	if (!isBroadcaster) {
					user = deserializeUser();
				} else {
		    		isBroadcaster = false;
		    	}
		    	
		    	// Sync the token
				setToken(sharedPreferences.getString(SESSION_TOKEN_KEY, null));
		    	
				isAuthenticated = true;
				
		    	if (error != null) {
		    		callCallbacks(true, new Exception(error));
		    	} else {
		    		callCallbacks(true, null);
		    	}
		    }
		};
		
		/** Receiver for logout events */
		private BroadcastReceiver logoutReceiver = new BroadcastReceiver() {
		    @Override
		    public void onReceive(Context context, Intent intent) {
		    	if (!isBroadcaster) {
			    	// Clear the API token
		    		setToken(null);
					
					// Clear the user
					serializeUser(null);
		    	} else {
		    		isBroadcaster = false;
		    	}
		    	
		    	isAuthenticated = false;
		    	callCallbacks(false, null);
		    }
		};
		
		/** Receiver for user update events */
		private BroadcastReceiver userUpdateReceiver = new BroadcastReceiver() {
		    @Override
		    public void onReceive(Context context, Intent intent) {
		    	UserApp.log("User has been updated.");
		    	
		    	if (!isBroadcaster) {
					// Reload the user
					user = deserializeUser();
		    	} else {
		    		isBroadcaster = false;
		    	}
		    	
		    	callUserCallbacks(user, null);
		    }
		};
		
		/** Interface for state callbacks */
		public static interface StatusCallback {
			public void call(Boolean authenticated, Exception exception);
		}
		
		/** Interface for user update callbacks */
		public static interface UserCallback {
			public void call(User user, Exception exception);
		}
		
		/** Interface for OAuth URL callbacks */
		public static interface OAuthUrlCallback {
			public void call(String url, Exception exception);
		}
		
		/** Result class to hold async task results */
		private static class AsyncResult {
			public Object value = null;
			public Exception exception = null;
			public AsyncResult(Object value, Exception exception) {
				this.value = value;
				this.exception = exception;
			}
		}
		
		/** Constructors for Session */
		public Session(FragmentActivity activity) {
			this(activity, null);
		}
		public Session(FragmentActivity activity, UserApp.Session.StatusCallback callback) {
			this.activity = activity;
			this.sharedPreferences = this.activity.getSharedPreferences(UserApp.PREFERENCE_KEY, Context.MODE_PRIVATE);
			
			// Load App Id from AndroidManifest.xml
			this.api = new UserAppClient.API(UserApp.getAppId(this.activity));
			
			if (callback != null) {
				this.addCallback(callback);
			}
		}
		
		/** Get a unique id for this installation, used to track tokens */
		private String getInstallationKey() {
			if (this.installationKey == null) {
				if (this.sharedPreferences.contains(UserApp.INSTALLATION_KEY)) {
					this.installationKey = this.sharedPreferences.getString(UserApp.INSTALLATION_KEY, "");
				} else {
					// Generate a new UUID
					this.installationKey = UUID.randomUUID().toString();
					
					// Save it in shared preferences
					SharedPreferences.Editor editor = this.sharedPreferences.edit();
					editor.putString(UserApp.INSTALLATION_KEY, this.installationKey);
					editor.commit();
				}
			}
			
			return this.installationKey;
		}
		
		/** 
		 * As this SDK uses fixed tokens to preserve a
		 * persistent session, we need to save the id of
		 * that token to later be able to delete it at logout
		 */
		private void setPersistentTokenId(String id) {
			// Save it in shared preferences
			SharedPreferences.Editor editor = this.sharedPreferences.edit();
			editor.putString(UserApp.TOKEN_ID_KEY, id);
			editor.commit();
		}
		
		private String getPersistentTokenId() {
			// Get it from shared preferences
			return this.sharedPreferences.getString(UserApp.TOKEN_ID_KEY, null);
		}
		
		/** Create a new UI helper and bind it to this session */
		public UserApp.UIHelper createUIHelper() {
			return this.createUIHelper(-1, -1);
		}
		
		public UserApp.UIHelper createUIHelper(int loginFragmentId, int mainFragmentId, int ... otherFragmentIds) {
			this.uiHelper = new UserApp.UIHelper(this, loginFragmentId, mainFragmentId, otherFragmentIds);
			return this.uiHelper;
		}
		
		/** Get the attached activity */
		public FragmentActivity getActivity() {
			return this.activity;
		}
		
		/** Invoke all callbacks */
		private void callCallbacks(Boolean authenticated, Exception exception) {
			synchronized (this.callbacks) {
				for (final UserApp.Session.StatusCallback callback : callbacks) {
					callback.call(authenticated, exception);
	            }
			}
		}
		
		/** Add a new callback */
	    public void addCallback(UserApp.Session.StatusCallback callback) {
	        synchronized (this.callbacks) {
	            if (callback != null && !this.callbacks.contains(callback)) {
	                this.callbacks.add(callback);
	            }
	        }
	    }
	    
	    /** Remove a callback */
	    public void removeCallback(UserApp.Session.StatusCallback callback) {
	        synchronized (this.callbacks) {
	            this.callbacks.remove(callback);
	        }
	    }
	    
	    /** Invoke all user callbacks */
		private void callUserCallbacks(User user, Exception exception) {
			synchronized (this.userCallbacks) {
				for (final UserApp.Session.UserCallback callback : userCallbacks) {
					callback.call(user, exception);
	            }
			}
		}
		
		/** Add a new user callback */
	    public void addUserCallback(UserApp.Session.UserCallback callback) {
	        synchronized (this.userCallbacks) {
	            if (callback != null && !this.userCallbacks.contains(callback)) {
	                this.userCallbacks.add(callback);
	            }
	        }
	    }
	    
	    /** Remove a user callback */
	    public void removeUserCallback(UserApp.Session.UserCallback callback) {
	        synchronized (this.userCallbacks) {
	            this.userCallbacks.remove(callback);
	        }
	    }
		
	    /** Return true if the activity is resumed */
		public Boolean isResumed() {
			return this._isResumed;
		}
		
		/** onResume event, should be called from the main activity */
		public void onResume() {
		    this._isResumed = true;
		    
		    // Check in preferences if there is a token
			String token = this.sharedPreferences.getString(UserApp.SESSION_TOKEN_KEY, null);
			if (token != null) {
				// Use the fixed token with the API
				setToken(token);
				
				user = deserializeUser();
				this.callCallbacks(true, null);
			} else {
				this.callCallbacks(false, null);
			}
		    
	        LocalBroadcastManager.getInstance(this.activity).registerReceiver(this.loginReceiver, new IntentFilter(UserApp.ACTION_SESSION_LOGIN));
	        LocalBroadcastManager.getInstance(this.activity).registerReceiver(logoutReceiver, new IntentFilter(UserApp.ACTION_SESSION_LOGOUT));
	        LocalBroadcastManager.getInstance(this.activity).registerReceiver(userUpdateReceiver, new IntentFilter(UserApp.ACTION_USER_UPDATE));
		}
		
		/** onPause event, should be called from the main activity */
		public void onPause() {
			this._isResumed = false;
			
			LocalBroadcastManager.getInstance(this.activity).unregisterReceiver(this.loginReceiver);
			LocalBroadcastManager.getInstance(this.activity).unregisterReceiver(this.logoutReceiver);
			LocalBroadcastManager.getInstance(this.activity).unregisterReceiver(this.userUpdateReceiver);
		}
		
		/** Broadcast an action */
		private void postAction(String action, Exception exception) {
	        final Intent intent = new Intent(action);
	        
	        if (exception != null) {
	        	intent.putExtra("ERROR_MESSAGE", exception.getMessage());
	        }
	        
	    	this.isBroadcaster = true;
	        LocalBroadcastManager.getInstance(this.getActivity()).sendBroadcast(intent);
	    }
		
		/** Method to login a user and start the session */
		public void login(String login, String password) {
			this.login(login, password, null);
		}
		public void login(String login, String password, UserApp.Session.StatusCallback callback) {
			if (loginTask != null) {
				return;
			}
			
			loginTask = new LoginTask();
			loginTask.login = login;
			loginTask.password = password;
			loginTask.callback = callback;
			loginTask.execute();
		}
		
		/** Method to login with a token */
		public void loginWithToken(String token) {
			this.loginWithToken(token, null);
		}
		public void loginWithToken(String token, UserApp.Session.StatusCallback callback) {
			if (loginTask != null) {
				return;
			}
			
			loginTask = new LoginTask();
			loginTask.token = token;
			loginTask.callback = callback;
			loginTask.execute();
		}
		
		/** Login callback */
		private void onLoginCompleted(UserApp.Session.StatusCallback callback, AsyncResult result) {
			if (callback != null) {
				callback.call(result.value != null, result.exception);
			}
			
			if (result.value != null) {
				// Save token in preferences
				SharedPreferences.Editor editor = this.sharedPreferences.edit();
				editor.putString(UserApp.SESSION_TOKEN_KEY, (String) result.value);
				editor.commit();
				
				// Broadcast login success
				this.postAction(UserApp.ACTION_SESSION_LOGIN, result.exception);
				
				UserApp.log("Logged in.");
			} else if (result.exception != null) {
				UserApp.log("Login failed: " + result.exception.getMessage());
			}
		}
		
		/** An async login task used to authenticate a user */
		private class LoginTask extends AsyncTask<Void, Void, AsyncResult> {
			public String login, password;
			public String token = null;
			public UserApp.Session.StatusCallback callback;
			
			@Override
			protected AsyncResult doInBackground(Void... params) {
				try {
					if (this.token == null) {
						setToken(null);
						
						// Login with the regular method, and then create a
						// fixed token to keep the session alive forever
						UserApp.log("Logging in...");
						UserAppClient.Result result = api.method("user.login")
							.parameter("login", login)
							.parameter("password", password)
							.call();
						
						// Check locks...
						ArrayList locks = result.get("locks").toArray();
						if (!locks.isEmpty()) {
							return new AsyncResult(null, new Exception("Your account has been locked."));
						}
					} else {
						// Set token
						setToken(this.token);
					}
					
					UserApp.log("Logged in. Establishing a persistent session using a fixed token...");
					
					// Look for an existing fixed token
					String fixedToken = null;
					
					ArrayList tokens = api.method("token.search")
							.parameter("fields", new UserAppClient.Array("token_id", "name", "value"))
							.parameter("page_size", 100)
							.call().get("items").toArray();
					
					for (Object token : tokens) {
						String id = (String) ((HashMap) token).get("token_id");
						String name = (String) ((HashMap) token).get("name");
						String value = (String) ((HashMap) token).get("value");
						
						if (name.equalsIgnoreCase(getInstallationKey())) {
							// Found a token for this installation
							fixedToken = value;
							setPersistentTokenId(id);
							UserApp.log("Found an existing token with name '" + getInstallationKey() + "'.");
							break;
						}
					}
					
					if (fixedToken == null) {
						// Create a new fixed token and use that instead
						UserApp.log("Creating a new token with name '" + getInstallationKey() + "'.");
						UserAppClient.Result saveResult = api.method("token.save")
								.parameter("name", getInstallationKey())
								.parameter("enabled", true)
								.call();
						
						// Get the value, and save the id for later use (i.e. logout)
						if (saveResult.get("value").exists()) {
							fixedToken = saveResult.get("value").toString();
							setPersistentTokenId(saveResult.get("token_id").toString());
						}
					}
					
					if (fixedToken == null) {
						// Something went wrong, go with the session token instead
						fixedToken = api.getOptions().token;
					}
					
					// Load the current user profile
					UserAppClient.Result userResult = api.method("user.get")
							.parameter("user_id", "self")
							.call()
							.get(0);
					
					user = parseUser(userResult);
					serializeUser(user);
					
					return new AsyncResult(fixedToken, null);
				} catch (Exception exception) {
					return new AsyncResult(null, exception);
				}
			}

			@Override
			protected void onPostExecute(final AsyncResult result) {
				loginTask = null;
				onLoginCompleted(this.callback, result);
			}

			@Override
			protected void onCancelled() {
				loginTask = null;
				onLoginCompleted(this.callback, new AsyncResult(null, new Exception("Login task was canceled.")));
			}
		}
		
		/** Logout method */
		public void logout() {
			this.logout(false);
		}
		
		/** 
		 * Logout method with the flag `localOnly`
		 * @param localOnly - if true, no token gets removed from UserApp, just from the client
		 */
		public void logout(Boolean localOnly) {
			if (logoutTask != null) {
				return;
			}
			
			UserApp.log("Logging out...");
			
			// Broadcast logout success (before the logout task has finished for good UX)
			this.postAction(UserApp.ACTION_SESSION_LOGOUT, null);
			
			if (localOnly) {
				// Remove token from shared preferences
				onLogoutCompleted();
			} else {
				// Remove fixed token from UserApp (in background)
				logoutTask = new LogoutTask();
				logoutTask.tokenId = getPersistentTokenId();
				logoutTask.execute();
			}
		}
		
		/** Logout callback */
		private void onLogoutCompleted() {
			// Remove token from shared preferences
			SharedPreferences.Editor editor = this.sharedPreferences.edit();
			editor.remove(UserApp.SESSION_TOKEN_KEY);
			editor.remove(UserApp.TOKEN_ID_KEY);
			editor.commit();
			
			// Clear the API token
			setToken(null);
			
			// Clear user
			serializeUser(null);
			
			UserApp.log("Logged out.");
		}
		
		/** An async logout task used to clear the session from UserApp */
		private class LogoutTask extends AsyncTask<Void, Void, Boolean> {
			public String tokenId;
			
			@Override
			protected Boolean doInBackground(Void... params) {
				try {
					// Log out by deleting the fixed token from UserApp
					if (tokenId == null) {
						return false;
					}
					
					api.method("token.remove").parameter("token_id", tokenId).call();
					
					return true;
				} catch (Exception exception) {
					UserApp.log("Error: " + exception.getMessage());
					return false;
				}
			}

			@Override
			protected void onPostExecute(final Boolean result) {
				if (result) {
					UserApp.log("Removed token from UserApp.");
				} else {
					UserApp.log("Failed to remove token from UserApp.");
				}
				
				logoutTask = null;
				onLogoutCompleted();
			}

			@Override
			protected void onCancelled() {
				UserApp.log("Failed to remove token from UserApp: Task cancelled.");
				logoutTask = null;
				onLogoutCompleted();
			}
		}
		
		/** Parse user from UserApp response */
		private User parseUser(UserAppClient.Result result) {
			User user = new User();
			user.user_id = getStringResult(result, "user_id", "self");
			user.first_name = getStringResult(result, "first_name", "");
			user.last_name = getStringResult(result, "last_name", "");
			user.email = getStringResult(result, "email", "");
			user.email_verified = getBooleanResult(result, "email_verified", false);
			user.login = getStringResult(result, "login", "");
			user.ip_address = getStringResult(result, "ip_address", "");
			user.password = getStringResult(result, "password", "");
			user.last_login_at = new Date(getIntegerResult(result, "last_login_at", 0)*1000);
			user.updated_at = new Date(getIntegerResult(result, "updated_at", 0)*1000);
			user.created_at = new Date(getIntegerResult(result, "created_at", 0)*1000);
			
			// Fill properties
			Set<String> properties = result.get("properties").toHashMap().keySet();
			for (String propertyName : properties) {
				Property property = new Property();
				property.value = result.get("properties").get(propertyName).get("value").result;
				property.override = result.get("properties").get(propertyName).get("override").toBoolean();
				user.properties.put(propertyName, property);
			}
			
			// Fill permissions
			Set<String> permissions = result.get("permissions").toHashMap().keySet();
			for (String permissionName : permissions) {
				Permission permission = new Permission();
				permission.value = result.get("permissions").get(permissionName).get("value").toBoolean();
				permission.override = result.get("permissions").get(permissionName).get("override").toBoolean();
				user.permissions.put(permissionName, permission);
			}
			
			// Fill features
			Set<String> features = result.get("features").toHashMap().keySet();
			for (String featureName : features) {
				Feature feature = new Feature();
				feature.value = result.get("features").get(featureName).get("value").toBoolean();
				feature.override = result.get("features").get(featureName).get("override").toBoolean();
				user.features.put(featureName, feature);
			}
			
			// Fill locks
			ArrayList locks = result.get("locks").toArray();
			
			for (Object lock : locks) {
				Lock l = new Lock();
				l.type = (String) ((HashMap) lock).get("type").toString();
				l.reason = (String) ((HashMap) lock).get("reason").toString();
				l.issued_by_user_id = (String) ((HashMap) lock).get("issued_by_user_id").toString();
				l.created_at = new Date(((Result)((HashMap) lock).get("created_at")).toInteger()*1000);
				user.locks.add(l);
			}
			
			// Subscription
			if (result.get("subscription").exists()) {
				user.subscription = new Subscription();
				user.subscription.price_list_id = result.get("subscription").get("price_list_id").toString();
				user.subscription.plan_id = result.get("subscription").get("plan_id").toString();
				user.subscription.override = result.get("subscription").get("override").toBoolean();
			}
			
			return user;
		}
		
		private String getStringResult(UserAppClient.Result result, String key, String defaultValue) {
			UserAppClient.Result field = result.get(key);
			if (field.exists()) {
				return field.toString();
			} else {
				return defaultValue;
			}
		}
		
		private Integer getIntegerResult(UserAppClient.Result result, String key, Integer defaultValue) {
			UserAppClient.Result field = result.get(key);
			if (field.exists()) {
				return field.toInteger();
			} else {
				return defaultValue;
			}
		}
		
		private Boolean getBooleanResult(UserAppClient.Result result, String key, Boolean defaultValue) {
			UserAppClient.Result field = result.get(key);
			if (field.exists()) {
				return field.toBoolean();
			} else {
				return defaultValue;
			}
		}
		
		/** Save user in shared preferences */
		private void serializeUser(User user) {
			SharedPreferences.Editor editor = this.sharedPreferences.edit();
			editor.putString(UserApp.USER_KEY, new Gson().toJson(user));
			editor.commit();
		}
		
		/** Get user from shared preferences */
		private User deserializeUser() {
			return new Gson().fromJson(this.sharedPreferences.getString(UserApp.USER_KEY, ""), User.class);
		}
		
		/** Reload the logged in user */
		public void reloadUser(UserApp.Session.UserCallback callback) {
			if (loadUserTask != null) {
				return;
			}
			
			loadUserTask = new LoadUserTask();
			loadUserTask.callback = callback;
			loadUserTask.execute();
		}
		
		/** An async task to load the current user from UserApp */
		private class LoadUserTask extends AsyncTask<Void, Void, Exception> {
			public UserApp.Session.UserCallback callback;
			
			@Override
			protected Exception doInBackground(Void... params) {
				try {
					UserAppClient.Result result = api.method("user.get")
							.parameter("user_id", "self")
							.call()
							.get(0);
					
					user = parseUser(result);
					serializeUser(user);
					
					return null;
				} catch (Exception exception) {
					UserApp.log("Error: " + exception.getMessage());
					return exception;
				}
			}

			@Override
			protected void onPostExecute(final Exception exception) {
				loadUserTask = null;
				onLoadUserCompleted(callback, exception);
			}
			
			@Override
			protected void onCancelled() {
				loadUserTask = null;
				onLoadUserCompleted(callback, new Exception("Task canceled."));
			}
		}
		
		/** Load User callback */
		private void onLoadUserCompleted(UserApp.Session.UserCallback callback, Exception exception) {
			if (callback != null) {
				callback.call(this.user, exception);
			}
			
			// Broadcast user update success
			this.postAction(UserApp.ACTION_USER_UPDATE, exception);
		}
		
		/** Method to save a user */
		public void saveUser(User user, UserApp.Session.UserCallback callback) {
			if (saveUserTask != null) {
				return;
			}
			
			saveUserTask = new SaveUserTask();
			saveUserTask.newUser = user;			
			saveUserTask.callback = callback;
			saveUserTask.execute();
		}
		
		/** Save user callback */
		private void onSaveCompleted(UserApp.Session.UserCallback callback, AsyncResult result) {
			if (result.value != null) {
				this.user = (User) result.value;
				serializeUser(this.user);
				
				// Broadcast user update success
				this.postAction(UserApp.ACTION_USER_UPDATE, result.exception);
			}
			
			if (callback != null) {
				callback.call(this.user, result.exception);
			}
		}
		
		/** An async save user task used to save a user */
		private class SaveUserTask extends AsyncTask<Void, Void, AsyncResult> {
			public User newUser;
			public UserApp.Session.UserCallback callback;
			
			@Override
			protected AsyncResult doInBackground(Void... params) {
				try {
					// Save the user
					UserAppClient.API saveCall = api.method("user.save")
						.parameter("first_name", newUser.first_name)
						.parameter("last_name", newUser.last_name)
						.parameter("email", newUser.email)
						.parameter("email_verified", newUser.email_verified);
					
					if (newUser.login != null && !newUser.login.isEmpty()) {
						saveCall.parameter("login", newUser.login);
					}
					
					if (newUser.user_id != null && !newUser.user_id.isEmpty()) {
						saveCall.parameter("user_id", newUser.user_id);
					}
					
					if (newUser.ip_address != null && !newUser.ip_address.isEmpty()) {
						saveCall.parameter("ip_address", newUser.ip_address);
					}
					
					if (newUser.password != null && !newUser.password.isEmpty()) {
						saveCall.parameter("password", newUser.password);
					}
					
					// Properties
					if (newUser.properties != null) {
						UserAppClient.Struct propertyStruct = new UserAppClient.Struct();
						Set<String> propertyKeys = newUser.properties.keySet();
						for (String key : propertyKeys) {
							Property property = newUser.properties.get(key);
							propertyStruct.parameter(key, new UserAppClient.Struct()
								.parameter("value", property.value)
								.parameter("override", property.override)
							);
						}
						saveCall.parameter("properties", propertyStruct);
					}
					
					// Permissions
					if (newUser.permissions != null) {
						UserAppClient.Struct permissionStruct = new UserAppClient.Struct();
						Set<String> permissionKeys = newUser.permissions.keySet();
						for (String key : permissionKeys) {
							Permission permission = newUser.permissions.get(key);
							permissionStruct.parameter(key, new UserAppClient.Struct()
								.parameter("value", permission.value)
								.parameter("override", permission.override)
							);
						}
						saveCall.parameter("permissions", permissionStruct);
					}
					
					// Features
					if (newUser.features != null) {
						UserAppClient.Struct featureStruct = new UserAppClient.Struct();
						Set<String> featureKeys = newUser.features.keySet();
						for (String key : featureKeys) {
							Feature feature = newUser.features.get(key);
							featureStruct.parameter(key, new UserAppClient.Struct()
								.parameter("value", feature.value)
								.parameter("override", feature.override)
							);
						}
						saveCall.parameter("features", featureStruct);
					}
					
					// Subscription
					if (newUser.subscription != null) {
						saveCall.parameter("subscription", new UserAppClient.Struct()
							.parameter("price_list_id", newUser.subscription.price_list_id)
							.parameter("plan_id", newUser.subscription.plan_id)
							.parameter("override", newUser.subscription.override)
						);
					}
					
					UserAppClient.Result result = saveCall.call();
					User savedUser = parseUser(result);
					
					return new AsyncResult(savedUser, null);
				} catch (Exception exception) {
					return new AsyncResult(null, exception);
				}
			}

			@Override
			protected void onPostExecute(final AsyncResult result) {
				saveUserTask = null;
				onSaveCompleted(this.callback, result);
			}

			@Override
			protected void onCancelled() {
				saveUserTask = null;
				onSaveCompleted(this.callback, new AsyncResult(null, new Exception("Save user task was canceled.")));
			}
		}
		
		/** 
		 * Method to get an OAuth authorization url
		 */
		public void getOAuthUrl(String providerId, String redirectURI, String scopes, UserApp.Session.OAuthUrlCallback callback) {
			if (getOAuthUrlTask != null) {
				return;
			}
			
			UserApp.log("Getting OAuth authorization url...");
			
			getOAuthUrlTask = new GetOAuthUrlTask();
			getOAuthUrlTask.providerId = providerId;
			getOAuthUrlTask.redirectURI = redirectURI;
			getOAuthUrlTask.scopes = scopes;
			getOAuthUrlTask.callback = callback;
			getOAuthUrlTask.execute();
		}
		
		/** getOAuthAuthorizationUrl callback */
		private void onGetOAuthUrlCompleted(UserApp.Session.OAuthUrlCallback callback, AsyncResult result) {
			if (callback != null) {
				callback.call((String) result.value, result.exception);
			}
		}
		
		/** An async task to get an OAuth authorization url from UserApp */
		private class GetOAuthUrlTask extends AsyncTask<Void, Void, AsyncResult> {
			public String providerId = null;
			public String redirectURI = null;
			public String scopes = "";
			public UserApp.Session.OAuthUrlCallback callback;
			
			@Override
			protected AsyncResult doInBackground(Void... params) {
				try {
					if (providerId == null || redirectURI == null) {
						onGetOAuthUrlCompleted(this.callback, new AsyncResult(null, new Exception("Missing providerId or redirectURI.")));
					}
					
					setToken(null);
					
					UserAppClient.Result result = api.method("oauth.getAuthorizationUrl")
							.parameter("provider_id", providerId)
							.parameter("redirect_uri", redirectURI)
							.parameter("scopes", scopes)
							.call();
					
					return new AsyncResult(result.get("authorization_url").toString(), null);
				} catch (Exception exception) {
					return new AsyncResult(null, exception);
				}
			}

			@Override
			protected void onPostExecute(final AsyncResult result) {
				getOAuthUrlTask = null;
				onGetOAuthUrlCompleted(this.callback, result);
			}

			@Override
			protected void onCancelled() {
				getOAuthUrlTask = null;
				onGetOAuthUrlCompleted(this.callback, new AsyncResult(null, new Exception("Get OAuth url task was canceled.")));
			}
		}
	
		/** Check if the logged in user has a specific permission */
		public Boolean hasPermission(String permissions) {
			if (this.user == null || permissions.isEmpty()) {
                return false;
            }
			
            String[] arr = permissions.split(" ");

            for (int i = 0; i < arr.length; ++i) {
                if (!(this.user.permissions.get(arr[i]) != null && this.user.permissions.get(arr[i]).value == true)) {
                    return false;
                }
            }

            return true;
		}
		
		/** Check if the logged in user has a specific feature */
		public Boolean hasFeature(String features) {
			if (this.user == null || features.isEmpty()) {
                return false;
            }
			
            String[] arr = features.split(" ");
            
            for (int i = 0; i < arr.length; ++i) {
                if (!(this.user.features.get(arr[i]) != null && this.user.features.get(arr[i]).value == true)) {
                    return false;
                }
            }

            return true;
		}
		
		/** Set UserApp token */
		private void setToken(String token) {
			UserAppClient.ClientOptions options = api.getOptions();
			options.token = token;
			api.setOptions(options);
			
			this.token = token;
		}
	}
	
	/**
	 * Class that facilitates UI changes such as
	 * hiding and showing fragments
	 */
	public static class UIHelper {
		private UserApp.Session session = null;
		private int loginFragmentId = -1;
		private int mainFragmentId = -1;
		private int[] otherFragmentIds;
		
		public UIHelper(UserApp.Session session) {
			this(session, -1, -1);
		}
		
		/** Create a new UIHelper and attach it to the session */
		public UIHelper(UserApp.Session session, int loginFragmentId, int mainFragmentId, int ... otherFragmentIds) {
			this.session = session;
			this.loginFragmentId = loginFragmentId;
			this.mainFragmentId = mainFragmentId;
			this.otherFragmentIds = otherFragmentIds;
			
			// Attach a callback to the session
			session.addCallback(new UserApp.Session.StatusCallback() {
			    @Override
			    public void call(Boolean authenticated, Exception exception) {
			        if (authenticated) {
			        	showMainFragment();
			        } else {
			        	showLoginFragment();
			        }
			    }
			});
		}
		
		/** Show the login fragment */
		public void showLoginFragment() {
			this.hideOtherFragments();
        	UserApp.UIHelper.hideFragmentsById(this.session.getActivity().getSupportFragmentManager(), this.mainFragmentId);
        	UserApp.UIHelper.showFragmentsById(this.session.getActivity().getSupportFragmentManager(), false, this.loginFragmentId);
		}
		
		/** Show the main fragment */
		public void showMainFragment() {
			this.hideOtherFragments();
			UserApp.UIHelper.hideFragmentsById(this.session.getActivity().getSupportFragmentManager(), this.loginFragmentId);
			UserApp.UIHelper.showFragmentsById(this.session.getActivity().getSupportFragmentManager(), false, this.mainFragmentId);
		}
		
		/** Hide all the other fragment */
		public void hideOtherFragments() {
			UserApp.UIHelper.hideFragmentsById(this.session.getActivity().getSupportFragmentManager(), this.otherFragmentIds);
		}
		
		/** Lifecycle events */
		public Boolean isResumed() {
			return this.session.isResumed();
		}
		
		public void onResume() {
		    this.session.onResume();
		}
		
		public void onPause() {
			this.session.onPause();
		}
		
		/** Hide all input fragments */
		public static void hideFragmentsById(FragmentManager fm, int ... fragmentIds) {
			FragmentTransaction transaction = fm.beginTransaction();
			
		    for (int i = 0; i < fragmentIds.length; ++i) {
		        transaction.hide(fm.findFragmentById(fragmentIds[i]));
		    }
		    
		    transaction.commit();
		}
		
		/** Show all input fragments */
		public static void showFragmentsById(FragmentManager fm, boolean addToBackStack, int ... fragmentIds) {
		    FragmentTransaction transaction = fm.beginTransaction();
		    
		    for (int i = 0; i < fragmentIds.length; ++i) {
		        transaction.show(fm.findFragmentById(fragmentIds[i]));
		    }
		    
		    if (addToBackStack) {
		        transaction.addToBackStack(null);
		    }
		    
		    transaction.commit();
		}	
	}
	
	/** Load App Id from AndroidManifest.xml */
	public static String getAppId(Context activity) {
		try {
			ApplicationInfo ai;
			ai = activity.getPackageManager().getApplicationInfo(activity.getPackageName(), PackageManager.GET_META_DATA);
			return ai.metaData.get("userapp.AppId").toString();
		} catch (Exception e) {
			UserApp.log("Error: Could not find App Id in AndroidManifest.xml.");
			return "";
		}
	}
	
}
