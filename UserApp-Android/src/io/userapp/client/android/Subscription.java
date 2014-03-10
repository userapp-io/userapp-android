package io.userapp.client.android;

import com.google.gson.annotations.SerializedName;

public class Subscription {

	@SerializedName("price_list_id")
	public String price_list_id;

	@SerializedName("plan_id")
	public String plan_id;
	
	@SerializedName("override")
	public Boolean override;
	
}
