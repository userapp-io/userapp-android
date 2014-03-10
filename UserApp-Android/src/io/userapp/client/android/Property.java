package io.userapp.client.android;

import com.google.gson.annotations.SerializedName;

public class Property {

	@SerializedName("value")
	public Object value;
	
	@SerializedName("override")
	public Boolean override;

	public Property() {}
	public Property(Object value, Boolean override) {
		this.value = value;
		this.override = override;
	}
	
}