<?xml version="1.0" encoding="UTF-8"?>

<!--
    Document   : budget-transform.xsl
    Description: 
    	Export for Budget Report
    Revision History:
        20150416 jpm - creation
-->

<xsl:stylesheet version="2.0" 
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
    xmlns:xs="http://www.w3.org/2001/XMLSchema"
    xmlns:fn="http://www.w3.org/2005/xpath-functions"
    xmlns:xdt="http://www.w3.org/2005/xpath-datatypes"
    xmlns:err="http://www.w3.org/2005/xqt-errors"
    xmlns:local="http://www.atex.com/local"
    exclude-result-prefixes="xsl xs xdt err fn local">
        
	<xsl:import href="util.xsl"/>
        
    <xsl:output method="xml" indent="yes" encoding="UTF-8"/>
    
    <xsl:param name="channel" select="'PRINT'"/>
    <xsl:param name="isPrinted" select="'false'"/>
      	
  	<!-- <xsl:variable name="lineBreak" select="'&lt;br/&gt;'"/> -->
  	<xsl:variable name="lineBreak" select="'&#x0A;'"/>

    <xsl:template match="/">
        <items>
        	<!-- loop through physpages -->
            <xsl:for-each select="//ncm-physical-page[logical-pages-linked-to-ncm-physical-page/@count>0 
            	and ($isPrinted!='true' or ($isPrinted='true' and is_printed='true'))]">
            	<xsl:variable name="physPage" select="."/>

				<!-- loop through logpages -->
				<xsl:for-each select="./logical-pages-linked-to-ncm-physical-page/ncm-logical-page[layouts-in-ncm-logical-page/@count>0]">
			
	            	<!-- packages -->
	            	<xsl:for-each select=".//ncm-object[ncm-type-property/object-type/@id=17]">
		            	<xsl:apply-templates select="." mode="item">
		             	   <xsl:with-param name="physPage" select="$physPage"/>
		            	</xsl:apply-templates>
	            	</xsl:for-each>
				</xsl:for-each>
				            	
            </xsl:for-each>       
        </items>
    </xsl:template>
    
    <xsl:template match="ncm-object[ncm-type-property/object-type/@id=17]" mode="item">
    	<xsl:param name="physPage"/>
        <xsl:variable name="pkgId" select="./obj_id"/>
        <xsl:variable name="pkgName" select="./name"/>
        <xsl:variable name="pub" select="$physPage/edition/newspaper-level/level/@name"/>
        
		<item>
        	<!-- output filename -->
        	<xsl:processing-instruction name="file-name" 
        		select="concat($pub, '-', $pkgId, '-', local:getTimestamp(), '.xml')"/>
        			
        	<unique-id><xsl:value-of select="$pkgId"/></unique-id>
        	<start-date><xsl:value-of select="local:formatDateDelim($physPage/pub_date, '/')"/></start-date>
        	
        	<!-- values taken from metadata -->
        	<xsl:variable name="obitMetadata" select="./extra-properties/OBITS"/>
        	<city><xsl:value-of select="$obitMetadata/CITY"/></city>
        	<state><xsl:value-of select="$obitMetadata/STATE"/></state>
			<free><xsl:value-of select="$obitMetadata/FREE"/></free>
			<veteran><xsl:value-of select="$obitMetadata/VETERAN"/></veteran>
			        	
  		</item>
    </xsl:template>
    
</xsl:stylesheet>
