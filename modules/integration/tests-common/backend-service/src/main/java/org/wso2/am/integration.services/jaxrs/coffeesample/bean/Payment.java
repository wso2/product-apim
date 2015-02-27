package org.wso2.am.integration.services.jaxrs.coffeesample.bean;

import javax.xml.bind.annotation.XmlRootElement;
import java.text.DecimalFormat;
import java.text.NumberFormat;

@XmlRootElement (name = "Payment")
public class Payment {

    private static final NumberFormat currencyFormat = new DecimalFormat("#.##");

    private String orderId;
    private String name;
    private String cardNumber;
    private String expiryDate;
    private Double amount;

    public Payment(String orderId) {
        this.orderId = orderId;
    }

    public Payment() {

    }

    public String getOrderId() {
        return orderId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCardNumber() {
        return cardNumber;
    }

    public void setCardNumber(String cardNumber) {
        this.cardNumber = cardNumber;
    }

    public String getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(String expiryDate) {
        this.expiryDate = expiryDate;
    }

    public Double getAmount() {
        return Double.valueOf(currencyFormat.format(amount));
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

}
