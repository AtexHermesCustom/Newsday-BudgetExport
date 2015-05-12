<?xml version="1.0" encoding="UTF-8"?>

<!--
    Document   : util.xsl
    Description: 
    	Transform from Hermes to Merlin schema
    Revision History:
        20120710 jpm - creation
-->

<xsl:stylesheet version="2.0" 
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
    xmlns:xs="http://www.w3.org/2001/XMLSchema"
    xmlns:fn="http://www.w3.org/2005/xpath-functions"
    xmlns:xdt="http://www.w3.org/2005/xpath-datatypes"
    xmlns:err="http://www.w3.org/2005/xqt-errors"
    xmlns:local="http://www.atex.com/local"
    exclude-result-prefixes="xsl xs xdt err fn local">

	<xsl:function name="local:toUpper">
    	<xsl:param name="text"/>
		<xsl:value-of select="translate($text, 'abcdefghijklmnopqrstuvwxyz', 'ABCDEFGHIJKLMNOPQRSTUVWXYZ')"/>
  	</xsl:function>
  	
	<xsl:function name="local:toLower">
    	<xsl:param name="text"/>
		<xsl:value-of select="translate($text, 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz')"/>
  	</xsl:function>  	

    <xsl:function name="local:formatDateNum"><!-- yyyyMMdd -->
        <xsl:param name="dateStr"/>
		<xsl:value-of select="substring($dateStr, 1, 8)"/>
    </xsl:function>
    
    <xsl:function name="local:formatDateDelim"><!-- MM/dd/yyyy -->
        <xsl:param name="dateStr"/>
        <xsl:param name="delim"/>
        <xsl:variable name="month" select="substring($dateStr, 5, 2)"/>
        <xsl:variable name="day" select="substring($dateStr, 7, 2)"/>
        <xsl:variable name="year" select="substring($dateStr, 1, 4)"/>
        <xsl:value-of select="concat($month, $delim, $day, $delim, $year)"/>
    </xsl:function>
    
    <xsl:function name="local:formatTimestamp"><!-- yyyyMMddThhmmss -->
        <xsl:param name="dateStr"/>
		<xsl:value-of select="concat(substring($dateStr, 1, 8), 'T', substring($dateStr, 9, 6))"/>
    </xsl:function>
    
    <xsl:function name="local:getTimestamp"><!-- yyyyMMddhhmmss -->
		<xsl:value-of select="format-dateTime(current-dateTime(), '[Y][M][D][H][m][s]')"/>
    </xsl:function>        
    
    <xsl:function name="local:getSiteCode">
    	<xsl:param name="pub"/>
    	<xsl:param name="edition"/>
    	<xsl:choose>
		    <xsl:when test="$pub='PR_REPUBLICAN'">POTRH</xsl:when>
		    <xsl:when test="$pub='SH_NEWSITEM'">SHANI</xsl:when>
		    <xsl:when test="$pub='SC_TIMES_TRIB'">SCRTT</xsl:when>
		    <xsl:when test="$pub='WB_VOICE'">WBCV</xsl:when>
		    <xsl:when test="$pub='TW_REVIEW'">TOWDR</xsl:when>
		    <xsl:when test="$pub='PI_PROGINDEX'">PETPI</xsl:when>
		    <xsl:when test="$pub='HZ_STANDSPEAK'">HAZSS</xsl:when>
		    <xsl:when test="$pub='VI_DAILYNEWS'">VIDN</xsl:when>
		    <xsl:when test="$pub='TSWG'">
		    	<xsl:choose>
		    		<xsl:when test="$edition='WGNA'">TSWG</xsl:when>
		    		<xsl:when test="$edition='WGWA'">TSWG</xsl:when>
		    		<xsl:otherwise>UNKNOWN_SITE</xsl:otherwise>
		    	</xsl:choose>
		    </xsl:when>
		    <xsl:otherwise>UNKNOWN_SITE</xsl:otherwise>
    	</xsl:choose>
    </xsl:function>
    
    <xsl:function name="local:substring-before-last">
    	<xsl:param name="input" as="xs:string"/>
    	<xsl:param name="substr" as="xs:string"/>
    	<xsl:variable name="substrEsc" select="local:esc-regex-chars($substr)"/>
		<xsl:value-of 
			select="if ($substr) then 
						if (contains($input, $substr)) then string-join(tokenize($input, $substrEsc)[position() != last()], $substr) 
                  		else ''
               		else $input"/>
    </xsl:function>    
    
    <xsl:function name="local:substring-after-last">
    	<xsl:param name="input" as="xs:string"/>
    	<xsl:param name="substr" as="xs:string"/>
    	<xsl:variable name="substrEsc" select="local:esc-regex-chars($substr)"/>
 	  	<xsl:value-of 
    		select="if ($substr) then
						if (contains($input, $substr)) then tokenize($input, $substrEsc)[last()] 
               			else '' 
            		else $input"/>
    </xsl:function>
    
    <xsl:function name="local:esc-regex-chars">
    	<xsl:param name="input" as="xs:string"/>
    	<xsl:choose>
    		<xsl:when test="$input = '.'">\.</xsl:when>
    		<xsl:otherwise><xsl:value-of select="$input"/></xsl:otherwise>
    	</xsl:choose>
    </xsl:function>
    
    <xsl:function name="local:getTextContent">
    	<xsl:param name="nodes"/>
    	<xsl:variable name="text">
	    	<xsl:for-each select="$nodes">
		    	<xsl:if test="local-name(.) != 'DELETE'"><!-- exclude DELETE nodes -->
	    			<xsl:value-of select="./text()"/>
		      	</xsl:if>
	    	</xsl:for-each>
	    </xsl:variable>
	    <xsl:value-of select="$text"/>
    </xsl:function>
    
  	<xsl:function name="local:lookupTagRule">
    	<xsl:param name="tag"/>
    	<xsl:param name="tagRules"/>
    	<!-- <xsl:message>tag=<xsl:value-of select="$tag"/></xsl:message> -->
    	<!-- find matching tag rule for the specified tag -->
		<xsl:copy-of select="($tagRules[matches(local:toUpper($tag), local:toUpper(@pattern))])[1]"/>
		<!-- <xsl:message>tag rule=<xsl:copy-of select="($tagRules[matches(local:toUpper($tag), local:toUpper(@pattern))])[1]"/></xsl:message> -->
  	</xsl:function>    
        
</xsl:stylesheet>
