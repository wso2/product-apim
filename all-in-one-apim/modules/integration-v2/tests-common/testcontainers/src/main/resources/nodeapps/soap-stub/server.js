/*
 *  Copyright (c) 2026, WSO2 LLC. (http://www.wso2.org) All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

// Minimal SOAP mock backend for gateway SOAP-invocation integration tests. It accepts any SOAP 1.1/1.2
// request (text/xml or application/soap+xml) on any path and returns a static, well-formed SOAP envelope
// so a SOAP API proxied through the APIM gateway can be invoked end-to-end without an external service.
const express = require('express');
const bodyParser = require('body-parser');

const app = express();
const port = 3019;

app.use(bodyParser.text({ type: ['text/xml', 'application/soap+xml', 'application/xml'] }));

// Health check (plain GET) so readiness/debugging is easy.
app.get('/health', (req, res) => res.status(200).send('OK'));

// Serve a valid WSDL over HTTP so APIM can create an API by WSDL-URL import (the in-network equivalent of
// the legacy WireMock-hosted WSDL). The soap:address points back at this stub so the served definition is
// self-consistent; the import overrides endpointConfig anyway. Served on both /wsdl and /service?wsdl.
const HELLO_WSDL =
  '<?xml version="1.0" encoding="UTF-8"?>' +
  '<wsdl:definitions xmlns:wsdl="http://schemas.xmlsoap.org/wsdl/" ' +
  'xmlns:soap="http://schemas.xmlsoap.org/wsdl/soap/" xmlns:xsd="http://www.w3.org/2001/XMLSchema" ' +
  'xmlns:tns="http://hello.soap.wso2.org/" targetNamespace="http://hello.soap.wso2.org/" name="HelloService">' +
  '<wsdl:types><xsd:schema targetNamespace="http://hello.soap.wso2.org/" elementFormDefault="qualified">' +
  '<xsd:element name="sayHello"><xsd:complexType><xsd:sequence>' +
  '<xsd:element name="name" type="xsd:string"/></xsd:sequence></xsd:complexType></xsd:element>' +
  '<xsd:element name="sayHelloResponse"><xsd:complexType><xsd:sequence>' +
  '<xsd:element name="greeting" type="xsd:string"/></xsd:sequence></xsd:complexType></xsd:element>' +
  '</xsd:schema></wsdl:types>' +
  '<wsdl:message name="sayHelloRequest"><wsdl:part name="parameters" element="tns:sayHello"/></wsdl:message>' +
  '<wsdl:message name="sayHelloResponse"><wsdl:part name="parameters" element="tns:sayHelloResponse"/></wsdl:message>' +
  '<wsdl:portType name="HelloPortType"><wsdl:operation name="sayHello">' +
  '<wsdl:input message="tns:sayHelloRequest"/><wsdl:output message="tns:sayHelloResponse"/>' +
  '</wsdl:operation></wsdl:portType>' +
  '<wsdl:binding name="HelloBinding" type="tns:HelloPortType">' +
  '<soap:binding style="document" transport="http://schemas.xmlsoap.org/soap/http"/>' +
  '<wsdl:operation name="sayHello"><soap:operation soapAction="http://hello.soap.wso2.org/sayHello"/>' +
  '<wsdl:input><soap:body use="literal"/></wsdl:input><wsdl:output><soap:body use="literal"/></wsdl:output>' +
  '</wsdl:operation></wsdl:binding>' +
  '<wsdl:service name="HelloService"><wsdl:port name="HelloPort" binding="tns:HelloBinding">' +
  '<soap:address location="http://nodebackend:3019/service"/></wsdl:port></wsdl:service>' +
  '</wsdl:definitions>';
app.get('/wsdl', (req, res) => {
  res.set('Content-Type', 'text/xml');
  res.status(200).send(HELLO_WSDL);
});

// Respond to any POST with a fixed SOAP response envelope.
app.post('*', (req, res) => {
  const responseEnvelope =
    '<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" ' +
    'xmlns:ns="http://ws.cdyne.com/PhoneVerify/query">' +
    '<soapenv:Body>' +
    '<ns:CheckPhoneNumberResponse>' +
    '<ns:CheckPhoneNumberResult>' +
    '<ns:Valid>true</ns:Valid>' +
    '<ns:Company>SOAP Stub</ns:Company>' +
    '</ns:CheckPhoneNumberResult>' +
    '</ns:CheckPhoneNumberResponse>' +
    '</soapenv:Body>' +
    '</soapenv:Envelope>';
  res.set('Content-Type', 'text/xml');
  res.status(200).send(responseEnvelope);
});

app.listen(port, () => {
  console.log(`SOAP stub backend running at http://nodebackend:${port}`);
});
