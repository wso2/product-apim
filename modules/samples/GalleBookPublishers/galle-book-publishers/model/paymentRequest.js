
var init, getPaymentRequestInSession, addEmail, addTransactionInfo, clearTransactionInfo,
    getTransactionInfo, addShippingInfo, getShippingInfo, getShippingInfo, addBillingInfo,
    getBillingInfo;

(function () {

  init = function (template){

    var paymentRequest = template;

    // Set the transaction id.
    paymentRequest.id = uuid();

    // Add the payment request to the user session.
    session.put('paymentRequest', paymentRequest);
  }

  getPaymentRequestInSession = function(){
    return session.get('paymentRequest');
  }

  addTransactionInfo = function(transactionInfo){

      var transactionInfoInTemplate =  getPaymentRequestInSession().transactions[0];

      transactionInfoInTemplate.order.quantity = transactionInfo.quantity;
      transactionInfoInTemplate.amount.details.subtotal = transactionInfo.subtotal;
      transactionInfoInTemplate.amount.details.shipping = transactionInfo.shipping;
      transactionInfoInTemplate.amount.details.tax = transactionInfo.tax;
      transactionInfoInTemplate.amount.total = transactionInfo.total;

  }

  clearTransactionInfo = function(){

    var transactionInfoInTemplate =  getPaymentRequestInSession().transactions[0];

    transactionInfoInTemplate.order.quantity = "0";
    transactionInfoInTemplate.amount.details.subtotal = "0";
    transactionInfoInTemplate.amount.details.tax = "0";
    transactionInfoInTemplate.amount.total = "0";

  }

  getTransactionInfo = function(){

    var transactionInfoInTemplate =  getPaymentRequestInSession().transactions[0];
    var transactionInfo = {};

    transactionInfo.quantity = transactionInfoInTemplate.order.quantity;
    transactionInfo.subtotal = transactionInfoInTemplate.amount.details.subtotal;
    transactionInfo.shipping = transactionInfoInTemplate.amount.details.shipping;
    transactionInfo.tax = transactionInfoInTemplate.amount.details.tax;
    transactionInfo.total = transactionInfoInTemplate.amount.total;

    return transactionInfo;

  }

  addShippingInfo = function(shippingInfo){

    var shippingInfoInTemplate =  getPaymentRequestInSession().shipment.shipping_address;

    shippingInfoInTemplate.first_name = shippingInfo.first_name;
    shippingInfoInTemplate.last_name = shippingInfo.last_name;
    shippingInfoInTemplate.line1 = shippingInfo.line1;
    shippingInfoInTemplate.city = shippingInfo.city;
    shippingInfoInTemplate.state = shippingInfo.state;
    shippingInfoInTemplate.postal_code = shippingInfo.postal_code;
    shippingInfoInTemplate.country_code = shippingInfo.country_code;

  }

  getShippingInfo = function(){

    var shippingInfo = {};
    var shippingInfoInTemplate =  getPaymentRequestInSession().shipment.shipping_address;

    shippingInfo.first_name = shippingInfoInTemplate.first_name;
    shippingInfo.last_name = shippingInfoInTemplate.last_name;
    shippingInfo.line1 = shippingInfoInTemplate.line1;
    shippingInfo.city = shippingInfoInTemplate.city;
    shippingInfo.state = shippingInfoInTemplate.state;
    shippingInfo.postal_code = shippingInfoInTemplate.postal_code;
    shippingInfo.country_code = shippingInfoInTemplate.country_code;

    return shippingInfo;

  }

  addBillingInfo = function(billingInfo){

    var creditCard =  getPaymentRequestInSession().payer.funding_instruments[0].credit_card;
    var shippingInfo =  getPaymentRequestInSession().shipment.shipping_address;

    creditCard.first_name = shippingInfo.first_name;
    creditCard.last_name = shippingInfo.last_name;
    creditCard.number = billingInfo.number;
    creditCard.type = billingInfo.type;
    creditCard.cvv2 = billingInfo.cvv2;
    creditCard.expire_month = billingInfo.expire_month;
    creditCard.expire_year = billingInfo.expire_year;

    getPaymentRequestInSession().payer.email = billingInfo.email;

    creditCard.billing_address.line1 = billingInfo.line1;
    creditCard.billing_address.city = billingInfo.city;
    creditCard.billing_address.state = billingInfo.state;
    creditCard.billing_address.postal_code = billingInfo.postal_code;
    creditCard.billing_address.country_code = billingInfo.country_code;

  }

  getBillingInfo = function(){

    var billingInfo = {};

    var billingAddress = getPaymentRequestInSession().payer.funding_instruments[0].credit_card.billing_address;

    billingInfo.line1 = billingAddress.line1;
    billingInfo.city = billingAddress.city;
    billingInfo.state = billingAddress.state;
    billingInfo.postal_code = billingAddress.postal_code;
    billingInfo.country_code = billingAddress.country_code;
    billingInfo.email = getPaymentRequestInSession().payer.email;

    return billingInfo;

  }

  var uuid = function() {
    function s4() {
      return Math.floor((1 + Math.random()) * 0x10000)
        .toString(16)
        .substring(1);
    }
    return s4() + s4() + '-' + s4() + '-' + s4() + '-' +
      s4() + '-' + s4() + s4() + s4();
  }

}());
