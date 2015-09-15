<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:xs="automation_mapping.xsd">
    <xsl:output omit-xml-declaration="yes" indent="yes"/>

    <xsl:template match="node()|@*">
        <xsl:copy>
            <xsl:apply-templates select="node()|@*"/>
        </xsl:copy>
    </xsl:template>

    <!--setting execution environment to standalone-->
    <xsl:template match="xs:executionEnvironment/text()">standalone</xsl:template>

    <!--setting coverage true-->
    <!--<xsl:template match="xs:coverage/text()">true</xsl:template>-->

    <!--setting host names-->
    <xsl:template match="xs:instance[@name='store']/xs:hosts/xs:host/text()">localhost</xsl:template>
    <xsl:template match="xs:instance[@name='store']/xs:ports/xs:port[@type='http']/text()">10263</xsl:template>
    <xsl:template match="xs:instance[@name='store']/xs:ports/xs:port[@type='https']/text()">9943</xsl:template>

    <xsl:template match="xs:instance[@name='publisher']/xs:hosts/xs:host/text()">localhost</xsl:template>
    <xsl:template match="xs:instance[@name='publisher']/xs:ports/xs:port[@type='http']/text()">10263</xsl:template>
    <xsl:template match="xs:instance[@name='publisher']/xs:ports/xs:port[@type='https']/text()">9943</xsl:template>

    <xsl:template match="xs:instance[@name='keyManager']/xs:hosts/xs:host/text()">localhost</xsl:template>
    <xsl:template match="xs:instance[@name='keyManager']/xs:ports/xs:port[@type='http']/text()">10263</xsl:template>
    <xsl:template match="xs:instance[@name='keyManager']/xs:ports/xs:port[@type='https']/text()">9943</xsl:template>

    <xsl:template match="xs:instance[@name='gateway-mgt']/xs:hosts/xs:host/text()">localhost</xsl:template>
    <xsl:template match="xs:instance[@name='gateway-mgt']/xs:ports/xs:port[@type='http']/text()">10263</xsl:template>
    <xsl:template match="xs:instance[@name='gateway-mgt']/xs:ports/xs:port[@type='https']/text()">9943</xsl:template>

    <xsl:template match="xs:instance[@name='gateway-wrk']/xs:hosts/xs:host/text()">localhost</xsl:template>
    <xsl:template match="xs:instance[@name='gateway-wrk']/xs:ports/xs:port[@type='http']/text()">10263</xsl:template>
    <xsl:template match="xs:instance[@name='gateway-wrk']/xs:ports/xs:port[@type='https']/text()">9943</xsl:template>
    <xsl:template match="xs:instance[@name='gateway-wrk']/xs:ports/xs:port[@type='nhttp']/text()">8780</xsl:template>
    <xsl:template match="xs:instance[@name='gateway-wrk']/xs:ports/xs:port[@type='nhttps']/text()">8743</xsl:template>

    <xsl:template match="xs:instance[@name='backend-server']/xs:hosts/xs:host/text()">localhost</xsl:template>
    <xsl:template match="xs:instance[@name='backend-server']/xs:ports/xs:port[@type='http']/text()">8080</xsl:template>
    <xsl:template match="xs:instance[@name='backend-server']/xs:ports/xs:port[@type='https']/text()">8082</xsl:template>

</xsl:stylesheet>