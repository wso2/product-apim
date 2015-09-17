<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:xs="automation_mapping.xsd">
    <xsl:output omit-xml-declaration="yes" indent="yes"/>

    <xsl:template match="node()|@*">
        <xsl:copy>
            <xsl:apply-templates select="node()|@*"/>
        </xsl:copy>
    </xsl:template>

    <!--setting execution environment to platform-->
    <xsl:template match="xs:executionEnvironment/text()">platform</xsl:template>

    <!--setting coverage false-->
    <xsl:template match="xs:coverage/text()">false</xsl:template>

    <!--setting host names and ports-->
    <xsl:template match="xs:instance[@name='store']/xs:hosts/xs:host/text()">store.am.wso2.com</xsl:template>
    <xsl:template match="xs:instance[@name='store']/xs:ports/xs:port[@type='http']/text()">80</xsl:template>
    <xsl:template match="xs:instance[@name='store']/xs:ports/xs:port[@type='https']/text()">443</xsl:template>

    <xsl:template match="xs:instance[@name='publisher']/xs:hosts/xs:host/text()">pub.am.wso2.com</xsl:template>
    <xsl:template match="xs:instance[@name='publisher']/xs:ports/xs:port[@type='http']/text()">80</xsl:template>
    <xsl:template match="xs:instance[@name='publisher']/xs:ports/xs:port[@type='https']/text()">443</xsl:template>

    <xsl:template match="xs:instance[@name='keyManager']/xs:hosts/xs:host/text()">keymanager.am.wso2.com</xsl:template>
    <xsl:template match="xs:instance[@name='keyManager']/xs:ports/xs:port[@type='http']/text()">80</xsl:template>
    <xsl:template match="xs:instance[@name='keyManager']/xs:ports/xs:port[@type='https']/text()">443</xsl:template>

    <xsl:template match="xs:instance[@name='gateway-mgt']/xs:hosts/xs:host/text()">mgt.gateway.am.wso2.com</xsl:template>
    <xsl:template match="xs:instance[@name='gateway-mgt']/xs:ports/xs:port[@type='http']/text()">80</xsl:template>
    <xsl:template match="xs:instance[@name='gateway-mgt']/xs:ports/xs:port[@type='https']/text()">443</xsl:template>

    <xsl:template match="xs:instance[@name='gateway-wrk']/xs:hosts/xs:host/text()">gateway.am.wso2.com</xsl:template>
    <xsl:template match="xs:instance[@name='gateway-wrk']/xs:ports/xs:port[@type='http']/text()">80</xsl:template>
    <xsl:template match="xs:instance[@name='gateway-wrk']/xs:ports/xs:port[@type='https']/text()">443</xsl:template>
    <xsl:template match="xs:instance[@name='gateway-wrk']/xs:ports/xs:port[@type='nhttp']/text()">8280</xsl:template>
    <xsl:template match="xs:instance[@name='gateway-wrk']/xs:ports/xs:port[@type='nhttps']/text()">8243</xsl:template>


    <xsl:template match="xs:instance[@name='backend-server']/xs:hosts/xs:host/text()">backend.server.wso2.com</xsl:template>
    <xsl:template match="xs:instance[@name='backend-server']/xs:ports/xs:port[@type='http']/text()">8080</xsl:template>
    <xsl:template match="xs:instance[@name='backend-server']/xs:ports/xs:port[@type='https']/text()">8082</xsl:template>

</xsl:stylesheet>