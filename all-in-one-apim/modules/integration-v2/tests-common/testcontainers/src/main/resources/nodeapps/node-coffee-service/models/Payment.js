class Payment {
    constructor(orderId, name, cardNumber, expiryDate, amount) {
        this.orderId = orderId;
        this.name = name;
        this.cardNumber = cardNumber;
        this.expiryDate = expiryDate;
        this.amount = amount;
    }
}

module.exports = Payment;