package io.userapp.client.android;

import java.util.Date;
import com.google.gson.annotations.SerializedName;

public class Lock {

	@SerializedName("type")
	public String type;
	
	@SerializedName("reason")
	public String reason;
	
	@SerializedName("issued_by_user_id")
	public String issued_by_user_id;
	
	@SerializedName("created_at")
	public Date created_at;
	
}