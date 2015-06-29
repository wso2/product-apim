# Fraud Detection Data Publisher

This project contains transaction data publishers which publish data from __WSO2 API Manager__ to __WSO2 DAS__.

The publishers are in two forms.

1. API Manager mediation extension.
2. API handler.

###Class mediator configuration.

```
<sequence xmlns="http://ws.apache.org/ns/synapse" name="transaction_data_publisher">
    <log level="custom">
        <property name="Message" value="Publishing transcation data to WSO2 DAS"/>
    </log>
    <class name="org.wso2.carbon.apimgt.frauddetection.TransactionDataPublishingMediator">
      <property name="dasHost" value="localhost"/>
      <property name="dasPort" value="7611"/>
      <property name="dasUsername" value="admin"/>
      <property name="dasPassword" value="admin"/>
      <property name="streamName" value="transactionStream"/>
      <property name="streamVersion" value="1.0.0"/>
    </class>
</sequence>
```


###API handler configuration

```
.
.
<handler class="org.wso2.carbon.apimgt.frauddetection.TransactionDataPublishingMediatorHandler"/>

```

This hanlder reads the connection properties for DAS from the properties file located in **APIM_HOME/repository/conf/etc/fraud-detection/fraud-detection.properties**

#####A sample properties file

```
dasHost=localhost
dasPort=7611
dasUsername=admin
dasPassword=admin
streamName=transactionStream
streamVersion=1.0.0
```

###Sample request payload
```
{
  "id": "3a6077e0-2b82-432a-bce2-d2910d7aec74",
  "intent": "sale",
  "payer": {
    "email": "betsy@buyer.com",
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
            "line1": "2313 Grand Manor",
            "city": "Cleopatra",
            "state": "NY",
            "postal_code": "13961-1041",
            "country_code": "USA"
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
