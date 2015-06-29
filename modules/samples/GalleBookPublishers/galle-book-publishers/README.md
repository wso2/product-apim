# APIM Book Store

This is a sample store app which run on a Jaggery server and invokes APIs published in WSO2 API Manager.

#### Endpoints

#### Settings
__URL : __apim-bookstore/settings

__Content-Type : __application/json

__Request Payload__

```
{
  paymentApiEndpoint: "<payment/endpoint/url>",
  accessToken : "<OAuth_access_token>"
}
```

__Response Payload__

```
{
    "status": "success"
}
```

#### Payment

__URL : __apim-bookstore/payment

__Content-Type : __application/json

__Payload__

```
{
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
          "expire_month": 11,
          "expire_year": 2018,
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
        "quantity": 1

      },
      "description": "This is the payment transaction description."
    }
  ]
}
```
__Response Payload__

```
{
    "status": "successful",
    "transactionId": "<transaction_id>"
}
```
