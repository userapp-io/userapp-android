package io.userapp.client;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONWriter;

import io.userapp.client.exceptions.*;
import io.userapp.client.rest.Restful;
import io.userapp.client.rest.RestfulContext;
import io.userapp.client.rest.UserCredentials;
import io.userapp.client.rest.core.HttpResponse;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/*
 * Implementation of the UserApp API.
 * https://app.userapp.io/#/docs/
 */
public class UserAppClient {
	
	/* Configuration object */
	public static class ClientOptions {
		public int version = 1;
		public String appId = null;
		public String token = null;
		public boolean debug = false;
		public boolean secure = true;
		public String baseAddress = "api.userapp.io";
		public boolean throwErrors = true;
		
		public ClientOptions() {}
		
		public ClientOptions(String appId) {
			this(appId, null);
		}
		
		public ClientOptions(String appId, String token) {
			this.appId = appId;
			this.token = token;
		}
	}
	
	/* Representation of an input parameter */
	public static class Parameter {
		String name;
		Object value;
		
		public Parameter(String name, Object value) {
			this.name = name;
			this.value = value;
		}
	}
	
	/* Representation of an input parameter struct */
	public static class Struct {
		ArrayList<UserAppClient.Parameter> parameters = new ArrayList<UserAppClient.Parameter>();
		
		public Struct() {}
		public UserAppClient.Struct parameter(String name, Object value) {
			this.parameters.add(new UserAppClient.Parameter(name, value));
			return this;
		}
		
		/* Convert to JSON */
		private String toJSON() {
			String result = "";
			
			for (UserAppClient.Parameter parameter : this.parameters) {
				if (result.length() > 0) {
					result += ",";
				}
				result += UserAppClient.ObjectToJson(parameter);
			}
			
			return "{" + result + "}";
		}
	}
	
	/* Representation of an input parameter array */
	public static class Array {
		ArrayList<Object> items = new ArrayList<Object>();
		
		public Array(Object ... items) {
			for (Object item : items) {
				this.items.add(item);
			}
		}
		
		/* Convert to JSON */
		private String toJSON() {
			String result = "";
			
			for (Object item : this.items) {
				if (result.length() > 0) {
					result += ",";
				}
				result += UserAppClient.ObjectToJson(item);
			}
			
			return "[" + result + "]";
		}
	}
	
	/* Representation of a result */
	public static class Result extends HashMap<String, Object> {
		private static final long serialVersionUID = -911210576998047758L;
		public Object result = null;
		
		public Result(HashMap<String, Object> hashMap) {
			super(hashMap);
		}
		
		public UserAppClient.Result get(Object key) {
			UserAppClient.Result result = new UserAppClient.Result(this);
			
			if (this.result != null) {
				if (this.result.getClass().getSimpleName().equalsIgnoreCase("ArrayList")) {
					if (key.getClass().getSimpleName().equalsIgnoreCase("Integer")) {
						result.result = ((ArrayList)this.result).get((Integer)key);
					} else {
						result.result = null;
					}
				} else {
					result.result = ((HashMap)this.result).get(key);
				}
			} else {
				result.result = super.get(key);
			}
			
			return result;
		}
		
		public boolean exists() {
			return this.result != null;
		}
		
		public ArrayList toArray() {
			if (this.result != null) {
				return (ArrayList) this.result;
			} else {
				return null;
			}
		}
		
		public HashMap<String, Object> toHashMap() {
			if (this.result != null) {
				return (HashMap<String, Object>) this.result;
			} else {
				return (HashMap<String, Object>) this;
			}
		}
		
		public String toString() {
			if (this.result != null) {
				return this.result.toString();
			} else {
				return super.toString();
			}
		}
		
		public int toInteger() {
			if (this.result != null) {
				return (Integer) this.result;
			} else {
				return 0;
			}
		}
		
		public float toFloat() {
			if (this.result != null) {
				return (Float) this.result;
			} else {
				return 0F;
			}
		}
		
		public boolean toBoolean() {
			if (this.result != null) {
				return (Boolean) this.result;
			} else {
				return false;
			}
		}
	}

	/* API wrapper class */
	public static class API {
		private UserAppClient.ClientOptions options;
		RestfulContext restfulContext = new RestfulContext();
		URI serviceUrl;
		private String methodName;
		private ArrayList<UserAppClient.Parameter> parameters = new ArrayList<UserAppClient.Parameter>();
		
		public API(String appId) {
			this.setOptions(new UserAppClient.ClientOptions(appId));
		}
		
		public API(String appId, String token) {
			this.setOptions(new UserAppClient.ClientOptions(appId, token));
		}
		
		public API(UserAppClient.ClientOptions options) {
			this.setOptions(options);
		}
		
		public void setOptions(UserAppClient.ClientOptions options) {
			this.options = options;
			this.restfulContext.setBasicAuthenticationCredentials(new UserCredentials(
				this.options.appId, (this.options.token == null ? "" : this.options.token)
			));
			this.serviceUrl = URI.create(
				String.format("%s://%s/v%d/", (this.options.secure ? "https" : "http"), this.options.baseAddress, this.options.version)
			);
		}
		
		public UserAppClient.ClientOptions getOptions() {
			return this.options;
		}
		
		/* Set the API method */
		public UserAppClient.API method(String name) {
			this.methodName = name;
			this.parameters.clear();
			return this;
		}
		
		/* Add an input parameter */
		public UserAppClient.API parameter(String name, Object value) {
			this.parameters.add(new UserAppClient.Parameter(name, value));
			return this; 
		}
		
		/* Convert all input parameters to JSON */
		private String toJSON() {
			String result = "";
			
			for (UserAppClient.Parameter parameter : this.parameters) {
				if (result.length() > 0) {
					result += ",";
				}
				result += UserAppClient.ObjectToJson(parameter);
			}
			
			return "{" + result + "}";
		}
		
		/* Perform the API call */
		public UserAppClient.Result call() throws UserAppException, TransportException, ServiceException, InvalidServiceException, InvalidMethodException {
			HttpResponse response = null;
			
	        try {
	        	String endpoint = this.serviceUrl.toString() + this.methodName + (this.options.debug ? "?$debug":"");
	        	String encodedParameters = this.toJSON();
	        	
	        	this.log(String.format("Calling URL '%s' with parameters '%s'", endpoint, encodedParameters));
	        	
				Restful restClient = new Restful(this.restfulContext);
				response = restClient.post(endpoint, encodedParameters);
	        } 
	        catch (Exception exception)
		    {
	        	throw new TransportException(exception.getMessage(), exception);
		    }
	        
			try
	        {
				this.log(String.format("Recieved response '%s'", response.result));
				
				response.result = "{\"result\":" + response.result + "}";
				HashMap<String, Object> json = JsonHelper.getMap(new JSONObject(response.result));
				
				UserAppClient.Result result = new UserAppClient.Result(json);
				
				/* Set or remove session token? */
				if (this.methodName == "user.login" && this.options.token == null) {
					this.options.token = result.get("result").get("token").toString();
					this.setOptions(this.options);
				} else if (this.methodName == "user.logout") {
					this.options.token = null;
					this.setOptions(this.options);
				}
				
				/* Check for error */
				if (result.get("result").get("error_code").exists()) {
					Exception generatedException = null;
					String errorCode = result.get("result").get("error_code").toString();
					String message = result.get("result").get("message").toString();
					
					if (errorCode.equals("INVALID_SERVICE")) {
						generatedException = new InvalidServiceException(message);
					} else if (errorCode.equals("INVALID_METHOD")) {
						generatedException = new InvalidMethodException(message);
					} else {
                    	if (this.options.throwErrors) {
                    		generatedException = new ServiceException(errorCode, message);
                    	}
                    }
					
					if (generatedException != null) {
						throw generatedException;
					}
				}
				
				return result.get("result");
			}
			catch (UserAppException exception) {
		    	throw exception;
		    }
		    catch (Exception exception) {
		    	throw new UserAppException(exception.getMessage(), exception);
		    }
		}
		
		private void log(String message) {
			if (this.options.debug) {
				System.out.println("[UserApp Debug]: " + message);
			}
		}
	}
	
	private static String ObjectToJson(Object obj) {
		if (obj == null) {
			return "null";
		}
		
		String result;
		String className = obj.getClass().getSimpleName();
		
		if (className.equals("Struct")) {
			result = ((UserAppClient.Struct) obj).toJSON();
		} else if (className.equals("Array")) {
			result = ((UserAppClient.Array) obj).toJSON();
		} else if (className.equals("Parameter")) {
			result = String.format("%s:%s", ObjectToJson(((UserAppClient.Parameter) obj).name), ObjectToJson(((UserAppClient.Parameter) obj).value));
		} else if (className.equals("String")) {
			result = JsonHelper.quote((String)obj);
		} else {
			result = obj.toString();
		}
		
		return result;
	}
	
}