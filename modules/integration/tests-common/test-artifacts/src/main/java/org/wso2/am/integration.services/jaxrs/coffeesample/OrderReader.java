/*
* Copyright 2004,2005 The Apache Software Foundation.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*      http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package org.wso2.am.integration.services.jaxrs.coffeesample;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.wso2.am.integration.services.jaxrs.coffeesample.bean.Order;

import javax.ws.rs.Consumes;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

@Provider
@Consumes(MediaType.APPLICATION_JSON)
public class OrderReader implements MessageBodyReader<Order> {

	@Override
	public Order readFrom(Class<Order> type,
	                      Type type1,
	                      Annotation[] antns,
	                      MediaType mt, MultivaluedMap<String, String> mm,
	                      InputStream in) throws IOException, WebApplicationException {

		Order order = new Order();
		InputStreamReader reader = new InputStreamReader(in);

		JsonElement json = new JsonParser().parse(reader);
		JsonObject jsonObject = json.getAsJsonObject();
		if (jsonObject.get("additions") != null) {
			order.setAdditions(jsonObject.get("additions").getAsString());
		} else {
			order.setAdditions("");
		}
		if (jsonObject.get("drinkName") != null) {
			order.setDrinkName(jsonObject.get("drinkName").getAsString());
		} else {
			order.setDrinkName("");
		}
		if (jsonObject.get("cost") != null) {
			order.setCost(jsonObject.get("cost").getAsDouble());
		}
		if (jsonObject.get("orderId") != null) {
			order.setOrderId(jsonObject.get("orderId").getAsString());
		}
		order.setLocked(false);

		return order;
	}

	@Override
	public boolean isReadable(Class<?> type, Type type1, Annotation[] antns, MediaType mt) {
		return Order.class.isAssignableFrom(type);
	}

}
