class PaymentStatus {
    constructor(status, payment = null) {
        this.Status = status; // Using 'Status' to match the JAXB @XmlElement
        this.Payment = payment; // Using 'Payment' to match the JAXB @XmlElement
    }
}

module.exports = PaymentStatus;