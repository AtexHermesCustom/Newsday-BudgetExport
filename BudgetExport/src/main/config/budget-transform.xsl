<?xml version="1.0" encoding="UTF-8"?>

<!--
    Document   : budget-transform.xsl
    Description: 
    	Budget Export from H11
    Revision History:
        20150506 jpm - creation
-->

<xsl:stylesheet version="2.0" 
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
    xmlns:xs="http://www.w3.org/2001/XMLSchema"
    xmlns:fn="http://www.w3.org/2005/xpath-functions"
    xmlns:xdt="http://www.w3.org/2005/xpath-datatypes"
    xmlns:err="http://www.w3.org/2005/xqt-errors"
    xmlns:local="http://www.atex.com/local"
    exclude-result-prefixes="xsl xs xdt err fn local">
        
    <xsl:template match="/">
    	<xsl:variable name="spMeta" select="./ncm-object/extra-properties/SP"/>

		<package>
			<xsl:variable name="budgetHead" select="$spMeta/BUDGET_HEAD"/>

			<xsl:choose>
				<xsl:when test="string-length(normalize-space($budgetHead)) &gt; 0"><!-- check if there's a budget head -->
					<xsl:apply-templates select="./ncm-object"/>
					<xsl:message>Found budget head. Transformation called</xsl:message>
				</xsl:when>
				<xsl:otherwise>
					<ignore/><!-- exclude package -->
					<xsl:message>No budget head value</xsl:message>
				</xsl:otherwise>
			</xsl:choose>
		</package>
		
	</xsl:template>
	
	<xsl:template match="ncm-object">
	    <xsl:variable name="obj" select="."/>
    	<xsl:variable name="spMeta" select="$obj/extra-properties/SP"/>
	
		<name><xsl:value-of select="$obj/name"/></name>
		<id><xsl:value-of select="$obj/obj_id"/></id>
		<level><xsl:value-of select="$obj/level/@path"/></level>
		<xsl:variable name="relevance" select="$obj/upd_relevance"/>
		<relevance>
			<xsl:choose>
				<xsl:when test="string-length(normalize-space($relevance)) &gt; 0">
					<xsl:value-of select="$relevance"/>
				</xsl:when>
				<xsl:otherwise>
					<xsl:value-of select="'0'"/>
				</xsl:otherwise>
			</xsl:choose>
		</relevance>
		<desk><xsl:value-of select="$spMeta/DESK"/></desk>
		<budgetHead><xsl:value-of select="$spMeta/BUDGET_HEAD"/></budgetHead>
		<description><xsl:value-of select="$spMeta/DESCRIPTION"/></description>
		<photoDescription><xsl:value-of select="$spMeta/PHOTO_DESCRIPTION"/></photoDescription>
		<length><xsl:value-of select="$spMeta/ASSIGN_LEN"/></length>
		<reporter1>
			<name><xsl:value-of select="$spMeta/REPORTER1"/></name>
			<email><xsl:value-of select="$spMeta/REPORTER1_EMAIL"/></email>
		</reporter1>
		<reporter2>
			<name><xsl:value-of select="$spMeta/REPORTER2"/></name>
			<email><xsl:value-of select="$spMeta/REPORTER2_EMAIL"/></email>
		</reporter2>
		<reporter3>
			<name><xsl:value-of select="$spMeta/REPORTER3"/></name>
			<email><xsl:value-of select="$spMeta/REPORTER3_EMAIL"/></email>
		</reporter3>
		<contributor><xsl:value-of select="$spMeta/CONTRIBUTOR"/></contributor>
		<storyGroup><xsl:value-of select="$spMeta/STORY_GROUP"/></storyGroup>
		<storyType><xsl:value-of select="$spMeta/STORY_TYPE"/></storyType>
		<printExtra><xsl:value-of select="$spMeta/PRINT_EXTRA"/></printExtra>
		<digitalExtra1><xsl:value-of select="$spMeta/DIGITAL_EXTRA1"/></digitalExtra1>
		<digitalExtra2><xsl:value-of select="$spMeta/DIGITAL_EXTRA2"/></digitalExtra2>			
		<label><xsl:value-of select="$spMeta/LABEL"/></label>
		<priority><xsl:value-of select="$spMeta/PRIORITY"/></priority>
		<printSection><xsl:value-of select="$spMeta/PRINT_SECTION"/></printSection>
		<printSequence><xsl:value-of select="$spMeta/SEQUENCE"/></printSequence>
		<printPage><xsl:value-of select="$spMeta/PRINT_PAGE"/></printPage>
		<homePage><xsl:value-of select="$spMeta/HOMEPAGE"/></homePage>
		<arrivalStatus><xsl:value-of select="$spMeta/ARRIVAL_STATUS"/></arrivalStatus>
		<exclusiveFlag><xsl:value-of select="$spMeta/EXCLUSIVE_FLAG"/></exclusiveFlag>
		<embargo>
			<date><xsl:value-of select="$spMeta/EMBARGO_DATE"/></date>
			<time><xsl:value-of select="$spMeta/EMBARGO_TIME"/></time>
		</embargo>
		<categories>
			<xsl:for-each select="tokenize($spMeta/CATEGORIES, ',')">
				<category><xsl:value-of select="."/></category>
			</xsl:for-each>
		</categories>
		<communities>
			<xsl:for-each select="tokenize($spMeta/COMMUNITIES, ',')">
				<community><xsl:value-of select="."/></community>
			</xsl:for-each>			
		</communities>	
	</xsl:template>
</xsl:stylesheet>
