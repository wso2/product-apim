package com.pizzashack.client.dto;


public class Order {
    private String address;
    private String pizzaType;
    private String customerName;
    private int quantity;
    private String creditCardNumber;
    boolean delivered;
    private String orderId;

    public Order(){

    }
    public Order(String address, String pizzaType, String customerName, String quantity, 
    		String creditCardNumber) {
        this.address = address;
        this.pizzaType = pizzaType;
        this.customerName = customerName;
        this.quantity = Integer.valueOf(quantity).intValue();
        this.creditCardNumber = creditCardNumber;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getPizzaType() {
        return pizzaType;
    }

    public void setPizzaType(String pizzaType) {
        this.pizzaType = pizzaType;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public String getCreditCardNumber() {
        return creditCardNumber;
    }

    public void setCreditCardNumber(String creditCardNumber) {
        this.creditCardNumber = creditCardNumber;
    }

    public boolean isDelivered() {
        return delivered;
    }

    public void setDelivered(boolean delivered) {
        this.delivered = delivered;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    @Override
    public String toString() {
        return "Order{" +
                "address='" + address + '\'' +
                ", pizzaType='" + pizzaType + '\'' +
                ", customerName='" + customerName + '\'' +
                ", quantity=" + quantity +
                ", creditCardNumber='" + creditCardNumber + '\'' +
                ", delivered=" + delivered +
                ", orderId='" + orderId + '\'' +
                '}';
    }
}
