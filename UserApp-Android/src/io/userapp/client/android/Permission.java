package io.userapp.client.android;

import com.google.gson.annotations.SerializedName;

public class Permission {

	@SerializedName("value")
	public Boolean value;
	
	@SerializedName("override")
	public Boolean override;
	
	public Permission() {}
	public Permission(Boolean value, Boolean override) {
		this.value = value;
		this.override = override;
	}
	
}