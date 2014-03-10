package io.userapp.client.android;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import com.google.gson.annotations.SerializedName;

public class User {

	@SerializedName("user_id")
	public String user_id;
	
	@SerializedName("first_name")
	public String first_name;
	
	@SerializedName("last_name")
	public String last_name;
	
	@SerializedName("email")
	public String email;
	
	@SerializedName("email_verified")
	public Boolean email_verified;
	
	@SerializedName("login")
	public String login;
	
	@SerializedName("password")
	public String password;
	
	@SerializedName("ip_address")
	public String ip_address;
	
	@SerializedName("last_login_at")
	public Date last_login_at;
	
	@SerializedName("updated_at")
	public Date updated_at;
	
	@SerializedName("created_at")
	public Date created_at;
	
	@SerializedName("properties")
	public HashMap<String, Property> properties = new HashMap<String, Property>();
	
	@SerializedName("permissions")
	public HashMap<String, Permission> permissions = new HashMap<String, Permission>();
	
	@SerializedName("features")
	public HashMap<String, Feature> features = new HashMap<String, Feature>();
	
	@SerializedName("locks")
	public ArrayList<Lock> locks = new ArrayList<Lock>();
	
	@SerializedName("subscription")
	public Subscription subscription;
	
}