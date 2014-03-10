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

package io.userapp.client.rest.core;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HttpHeaderCollection extends HashMap<String, String> {
	private static final long serialVersionUID = 1L;
	
	public static HttpHeaderCollection FromMap(Map<String, List<String>> headers){
		HttpHeaderCollection result = new HttpHeaderCollection();

		for(Map.Entry<String, List<String>> entry : headers.entrySet()){
			if(entry.getKey() != null && entry.getValue().size() != 0){
				result.put(entry.getKey(), entry.getValue().get(0));
			}
		}

		return result;
	}
}
