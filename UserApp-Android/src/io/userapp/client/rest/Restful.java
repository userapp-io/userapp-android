/*
The MIT License

Copyright (c) 2014 UserApp <https://www.userapp.io/>

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.
*/

package io.userapp.client.rest;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;
import android.util.Base64;
import io.userapp.client.rest.core.HttpHeaderCollection;
import io.userapp.client.rest.core.HttpResponse;
import io.userapp.client.rest.core.HttpResponseHead;
import io.userapp.client.rest.core.HttpResponseStatusHead;
import io.userapp.client.rest.core.HttpStatusCode;

public class Restful {

	private RestfulContext _context;
	
	public Restful(){
		this._context = new RestfulContext();
	}
	
	public Restful(RestfulContext context){
		this._context = context;
	}
	
	public void setContext(RestfulContext context){
		if(context == null){
			throw new NullPointerException();
		}
		
		this._context = context;
	}
	
	public RestfulContext getContext(){
		return this._context;
	}
	
	public HttpResponse post(String url, String body)
	{
		try {
	    	InputStream stream = null;
	        URL invocationAddress = new URL(url);
	        HttpResponse response = new HttpResponse();
	        HttpURLConnection connection = (HttpURLConnection) invocationAddress.openConnection();
		    
	        try
	        {
		        try
		        {
		        	connection.setUseCaches(false);
		        	connection.setAllowUserInteraction(false);
			        connection.setDoOutput(true);
			        connection.setInstanceFollowRedirects(false);
			        
			        connection.setRequestMethod("POST");
			        connection.setReadTimeout(this.getContext().getConnectionReadTimeout());
			        
			        connection.setRequestProperty("User-Agent", "UserApp Java/1.0.0");
			        connection.setRequestProperty("Content-Type", "application/json");
			        
			        UserCredentials credentials = this.getContext().getBasicAuthenticationCredentials();
			        
			        if(credentials != null){
			        	String autorizationValue = 	Base64.encodeToString((credentials.getUsername() + ":" + credentials.getPassword()).getBytes(), Base64.NO_WRAP);
			        	connection.addRequestProperty("Authorization", "Basic " + autorizationValue);
			        }
			        
			        OutputStreamWriter streamWriter = new OutputStreamWriter(connection.getOutputStream());
		        	streamWriter.write(body);
		        	streamWriter.close();
		        	
			        stream = connection.getInputStream();
			    }catch(IOException exception){
			        stream = connection.getErrorStream();
			    }
			    
			    response.head = new HttpResponseHead();			    
			    response.head.status = new HttpResponseStatusHead();
			    response.head.status.code = HttpStatusCode.getType(connection.getResponseCode());
			    response.head.status.message = connection.getResponseMessage();
			    response.head.headers = HttpHeaderCollection.FromMap(connection.getHeaderFields());
			    response.result = this.convertStreamToString(stream);
			    
			    return response;
	        }
	        finally
	        {
	        	if(stream != null){
	        		stream.close();
	        	}
	        }
	    } catch(Exception exception) { 
	        throw new RuntimeException(exception);
	    }
	}
	
	private String convertStreamToString(InputStream is) { 
	    return new Scanner(is).useDelimiter("\\A").next();
	}
}
