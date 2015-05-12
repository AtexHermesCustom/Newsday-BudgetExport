package com.atex.h11.custom.newsday.export.budget;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import net.sf.saxon.Controller;
import net.sf.saxon.event.Receiver;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import com.atex.h11.custom.common.DataSource;
import com.atex.h11.custom.common.StoryPackage;
import com.atex.h11.custom.newsday.export.budget.util.XSLTMessageReceiver;
import com.unisys.media.cr.adapter.ncm.common.data.pk.NCMObjectPK;
import com.unisys.media.cr.adapter.ncm.model.data.datasource.NCMDataSource;
import com.unisys.media.extension.common.serialize.xml.XMLSerializeWriterException;

public class Exporter {
	private static final String loggerName = Exporter.class.getName();
    private static final Logger logger = Logger.getLogger(loggerName);
    
	private static NCMDataSource ds = null;
	
	private static TransformerFactory tf = null;
	private static XPathFactory xpf = null;
	private static DocumentBuilderFactory dbf = null;		
	
	private static Templates cachedStylesheet = null;

	private Properties props = null;
	private String pub;
	private Integer pubDate;	
    
	public Exporter(Properties props, String user, String password, String pub, Integer pubDate) 
			throws FileNotFoundException, IOException {
		this.props = props;
		this.pub = pub;
		this.pubDate = pubDate;
		
		// get H11 data source
		ds = DataSource.newInstance(user, password);
	}
	
	public void run() 
			throws TransformerConfigurationException {
		logger.entering(loggerName, "run");

		
		

		
		
		
		logger.exiting(loggerName, "run");   
	}
	
	
	
	private void write(Map<Integer, String> packages) 
			throws ParserConfigurationException, UnsupportedEncodingException, IOException, 
			XMLSerializeWriterException, SAXException, TransformerException, XPathExpressionException {
		logger.entering(loggerName, "write");
		
		// load transform stylesheet
		File xslFile = new File(props.getProperty("transformStylesheet"));
		logger.finer("xslFile=" + xslFile.getPath());
		
		tf = TransformerFactory.newInstance();		
		cachedStylesheet = tf.newTemplates(new StreamSource(xslFile));				
		
		dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder docBuilder = dbf.newDocumentBuilder();			

		xpf = XPathFactory.newInstance();		
	    XPath xp = xpf.newXPath();    						
		
		// root elements
		Document doc = docBuilder.newDocument();
		Element rootElem = doc.createElement("budget");
		doc.appendChild(rootElem);	
		
		// pub elements
		Element pubInfoElem = doc.createElement("pubInfo");
		
		Element pubDateElem = doc.createElement("pubDate");
		pubDateElem.appendChild(doc.createTextNode(String.valueOf(pubDate)));
		pubInfoElem.appendChild(pubDateElem);
		
		Element pubElem = doc.createElement("pub");
		pubElem.appendChild(doc.createTextNode(pub));
		pubInfoElem.appendChild(pubElem);
		
		rootElem.appendChild(pubInfoElem);
		
		// packages
		Element packagesElem = doc.createElement("packages");
		
		StoryPackage sp = new StoryPackage(ds);
        sp.setConvertFormat(props.getProperty("convertFormat"));			
        sp.setDateFormat(Constants.DEFAULT_DATETIME_FORMAT);
		
		logger.info("Exporting packages...");
		for (int spId : packages.keySet()) {
			String spName = packages.get(spId);
			logger.info("Package: id=" + spId + ", name=" + spName);
            
            NCMObjectPK pk = new NCMObjectPK(spId);
            Document spDoc = sp.getDocument(pk);
            if (props.getProperty("debug").equalsIgnoreCase("true")) {
            	writeDocumentToFile(spDoc, 
        			new File(props.getProperty("debugDir"), Integer.toString(spId) + "-" + spName + "-orig.xml"),
        			props.getProperty("encoding"));
			}
            	            
            // transform package document
            Transformer t = cachedStylesheet.newTransformer();
    		Controller controller = (Controller) t;
    		Receiver receiver = new XSLTMessageReceiver(logger);	// for logging messages from XSLT
    		controller.setMessageEmitter(receiver);            

    		// parameters read from properties file
	        for (String prop : props.stringPropertyNames()) {
	            if (prop.startsWith("transform.param.")) {
	                t.setParameter(prop.replaceFirst("transform.param.", ""), props.getProperty(prop));
	            }
	        }
    		
			t.setOutputProperty(OutputKeys.METHOD, "xml");
			t.setOutputProperty(OutputKeys.INDENT, "no");
			t.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
			t.setOutputProperty(OutputKeys.ENCODING, props.getProperty("encoding"));
			DOMResult result = new DOMResult();
			t.transform(new DOMSource(spDoc), result);	     
			
			// resulting document
			Document resDoc = (Document) result.getNode();
            if (props.getProperty("debug").equalsIgnoreCase("true")) {
            	writeDocumentToFile(spDoc, 
        			new File(props.getProperty("debugDir"), Integer.toString(spId) + "-" + spName + "-transformed.xml"),
        			props.getProperty("encoding"));            	
			}			
                			
			boolean hasContent = (boolean) xp.evaluate("/package//text()", 
					resDoc.getDocumentElement(), XPathConstants.BOOLEAN);

			// append 
			if (hasContent) {	// only if there's content
				packagesElem.appendChild(doc.importNode(resDoc.getDocumentElement(), true));
				logger.info("Package exported: id=" + spId + ", name=" + spName);
			} else {
				logger.info("Package not exported: id=" + spId + ", name=" + spName + ". Reason: no content");
			}
		}
		
		rootElem.appendChild(packagesElem);
		
		// write the content into the final output file	
		File outputFile = new File(props.getProperty("outputDir"), "budget_" + pubDate + "_" + pub + ".xml");
		writeDocumentToFile(doc, outputFile, props.getProperty("encoding"));
		logger.info("Exported to output file: " + outputFile.getPath());
		
		logger.exiting(loggerName, "write");
	}	
		
	private void writeDocumentToFile(Document doc, File file, String encoding) 
			throws TransformerException {
		DOMSource source = new DOMSource(doc);
		StreamResult result = new StreamResult(file);

		Transformer t = tf.newTransformer();		
		t.setOutputProperty(OutputKeys.METHOD, "xml");
		t.setOutputProperty(OutputKeys.INDENT, "no");
		t.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
		if (encoding != null && !encoding.isEmpty()) {
			t.setOutputProperty(OutputKeys.ENCODING, encoding);
		} else {
			t.setOutputProperty(OutputKeys.ENCODING, Constants.DEFAULT_ENCODING);
		}
		t.transform(source, result);			
	}
}
