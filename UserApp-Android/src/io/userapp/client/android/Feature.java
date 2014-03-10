package io.userapp.client.android;

import com.google.gson.annotations.SerializedName;

public class Feature {

	@SerializedName("value")
	public Boolean value;
	
	@SerializedName("override")
	public Boolean override;
	
	public Feature() {}
	public Feature(Boolean value, Boolean override) {
		this.value = value;
		this.override = override;
	}
	
}