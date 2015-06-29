$('#checkoutButton').on('click', function () {

    var paymentRequestPayload = $('#paymentPayload').text();

    $.ajax({
      method: "POST",
      url: "payment",
      contentType: "application/json; charset=utf-8",
      dataType: "json",
      data: paymentRequestPayload
    })
    .done(function( msg ) {
      var transactionId;

      if(msg){
        transactionId = msg.transactionId;
      }

      alert("Transaction was successfully !\n Transaction ID : " + transactionId);
    });

});

$('#loadSampleButton').on('click', function () {

    var samplePayload = {
      "id": "3a6077e0-2b82-432a-bce2-d2910d7aec74",
      "intent": "sale",
      "payer": {
        "email": "mitsue_1_tollner@yahoo.com",
        "payment_method": "credit_card",
        "funding_instruments": [
          {
            "credit_card": {
              "number": "3772822463100050",
              "type": "visa",
              "expire_month": "11",
              "expire_year": "2018",
              "cvv2": "874",
              "first_name": "Betsy",
              "last_name": "Buyer",
              "billing_address": {
                "line1": "uuuu",
                "city": "uuuu",
                "state": "uuu",
                "postal_code": "uuu",
                "country_code": "uuuu"
              }
            }
          }
        ]
      },
      "shipment": {
        "shipping_address": {
          "line1": "2313 Grand Manor",
          "city": "Cleopatra",
          "state": "NY",
          "postal_code": "13961-1041",
          "country_code": "USA"
        }
      },
      "transactions": [
        {
          "amount": {
            "total": "5500",
            "currency": "USD",
            "details": {
              "subtotal": "7.41",
              "tax": "0.03",
              "shipping": "0.03"
            }
          },
          "order": {
            "item_number": "I0010",
            "quantity": "1"

          },
          "description": "This is the payment transaction description."
        }
      ]
    }

    $('#paymentPayload').text(JSON.stringify(samplePayload));

});
