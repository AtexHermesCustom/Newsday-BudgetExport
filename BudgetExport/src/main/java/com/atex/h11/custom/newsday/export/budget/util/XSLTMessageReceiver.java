package com.atex.h11.custom.newsday.export.budget.util;

import java.util.logging.Logger;
import net.sf.saxon.event.PipelineConfiguration;
import net.sf.saxon.trans.XPathException;

/**
 * A class used to redirect output from xsl:message elements to OpenSyncro's
 * Transfer log.
 * 
 * Regular xsl:message's are written as debug (info) level messages while
 * xsl:message's with terminate="yes" attribute are written as error level
 * messages.
 */
public class XSLTMessageReceiver implements net.sf.saxon.event.Receiver {
	//private MessageLogger logger;
	//private Object caller;
	private Logger logger = null;
	private String msg = null;

    /** 
     * Contains information on whether the xsl:message element had attribute
     * terminate="yes" (ReceiverOptions.TERMINATE). Given by Saxon only at
     * the startDocument() call.
     */
	private int startElementProperties;

    // Specified as required by getter/setter methods in the Receiver interface
    private PipelineConfiguration pipelineConfig;
    private String systemId;
    
	/**
	 * Create new XSLTMessageReceiver that will output xsl:messages to Transfer log.
	 * 
	 * @param logger <code>Logger</code> used.
	 */
	public XSLTMessageReceiver(Logger logger) {
		//this.caller = caller;
		this.logger = logger;
        this.setStartElementProperties(0);
	}
	
    public void startDocument(int properties) throws XPathException {
        this.setStartElementProperties(properties);
        msg = "";	// init
    }
    
    public void endDocument() throws XPathException {
    	logger.info("XsltMsg: " + msg);	// output the message
    }    
    
    public void characters(CharSequence chars, int locationId, int properties)
    		throws XPathException {
        //if((this.startElementProperties & ReceiverOptions.TERMINATE) != 0) {
            //logMessageLevel = MessageLogger.ERROR;
        //} else {
            //logMessageLevel = MessageLogger.DEBUG;
        //}
    	msg += chars.toString();
    }
    
    // Obligatory PipelineConfiguration and SystemId getter/setter methods implemented
    public void setPipelineConfiguration(PipelineConfiguration pipe) {
        this.pipelineConfig = pipe;
    }
    
	public PipelineConfiguration getPipelineConfiguration() {
        return pipelineConfig;
    }
    
    public void setSystemId(String systemId) {
        this.systemId = systemId;
    }
    
    public String getSystemId() {
        return systemId;
    }
    
	public void setStartElementProperties(int startElementProperties) {
		this.startElementProperties = startElementProperties;
	}

	public int getStartElementProperties() {
		return startElementProperties;
	}    
    
    // The following methods are intentionally left empty
    public void attribute(int nameCode, int typeCode, CharSequence value, int locationId, int properties) 
    	throws XPathException {}
    
    public void comment(CharSequence content, int locationId, int properties)
    	throws XPathException {}
    
    public void namespace(int namespaceCode, int properties) 
    	throws XPathException {}
    
    public void processingInstruction(String name, CharSequence data, int locationId, int properties) 
    	throws XPathException {}
    
    public void setUnparsedEntity(String name, String systemID, String publicID)
    	throws XPathException {}
    
    public void startContent() 
    	throws XPathException {}
    
    public void startElement(int nameCode, int typeCode, int locationId, int properties) 
    	throws XPathException {}
    
    public void endElement() 
    	throws XPathException {}
       
    public void open() 
    	throws XPathException {}
    
    public void close() {}

}