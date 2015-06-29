/*
 * Copyright 2011-2012 WSO2, Inc. (http://wso2.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wso2.carbon.apimgt.payfriend.rest;

import com.google.gson.Gson;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import java.util.*;

@Path("/payments/payment")
public class Payment {

    private static final Log log = LogFactory.getLog(Payment.class);

    @POST
    @Produces("application/json")
    public Map<String, Object> doPayment(String paymentInfo){
        return buildPaymentResponse(paymentInfo);
    }

    private Map<String, Object> buildPaymentResponse(String paymentInfo){

        Gson gson = new Gson();
        Map<String, Object> payment = gson.fromJson(paymentInfo, LinkedHashMap.class);

        Map<String, Object> response = new LinkedHashMap<String, Object>();

        response.put("id", payment.get("id"));
        response.put("create_time", new Date().toString());
        response.put("update_time", new Date().toString());
        response.put("state", "approved");

        // NOTE : Commenting out to get rid of an ESB JSON data type conversion issue.
//        response.put("intent", payment.get("intent"));
//        response.put("payer", getPayerForResponse(payment));
//        response.put("transactions", payment.get("transactions"));

        return response;
    }

    private Map<String, Object> getPayerForResponse(Map<String, Object> payment) {

        Map<String, Object> payer = (Map<String, Object>) payment.get("payer");

        // Remove cvv2
        List fundingInstruments = (List) payer.get("funding_instruments");
        ((Map<String, Object>)((Map<String, Object>)fundingInstruments.get(0)).get("credit_card")).remove("cvv2");

        // Mask the credit card number
        String creditCardNumber = (String) ((Map<String, Object>)((Map<String, Object>)fundingInstruments.get(0)).get("credit_card")).get("number");
        String maskedCreditCardNumber = maskCreditCardNumber(creditCardNumber);
        ((Map<String, Object>)((Map<String, Object>)fundingInstruments.get(0)).get("credit_card")).put("number", maskedCreditCardNumber);

        return payer;
    }

    private String maskCreditCardNumber(String creditCardNumber) {

        int unmaskedCharacterLength = 4;
        char maskCharacter = 'x';

        StringBuffer maskedCreditCardNumber = new StringBuffer();
        for(int i = 0; i < creditCardNumber.length() - 4; i++){
            maskedCreditCardNumber.append(maskCharacter);
        }

        maskedCreditCardNumber.append(creditCardNumber.substring(creditCardNumber.length() - unmaskedCharacterLength));

        return maskedCreditCardNumber.toString();

    }


}