package com.atex.h11.custom.newsday.export.budget;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
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

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import com.atex.h11.custom.common.DataSource;
import com.atex.h11.custom.common.StoryPackage;
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
	
	private static Templates cachedXSLT = null;

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
		
		File xslFile = new File(props.getProperty("transformStylesheet"));
		logger.finer("xslFile=" + xslFile.getPath());
		cachedXSLT = tf.newTemplates(new StreamSource(xslFile));		
		
		
		
		logger.exiting(loggerName, "run");   
	}
	
	
	
	private void write(Map<Integer, String> packages) 
			throws ParserConfigurationException, UnsupportedEncodingException, IOException, 
			XMLSerializeWriterException, SAXException, TransformerException, XPathExpressionException {
		logger.entering(loggerName, "write");
		
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
		
		System.out.println("Exporting packages...");
		for (int objId : packages.keySet()) {
			System.out.println("package: id=" + objId + ", name=" + packages.get(objId));
            
            NCMObjectPK pk = new NCMObjectPK(objId);
            Document spDoc = sp.getDocument(pk);
            //File outFile = new File(workDir, Integer.toString(objId) + "-" + convertFormat + ".xml");
            //sp.export(pk, outFile);
            	            
            Transformer t = cachedXSLT.newTransformer();
			t.setOutputProperty(OutputKeys.METHOD, "xml");
			t.setOutputProperty(OutputKeys.INDENT, "no");
			t.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
			t.setOutputProperty(OutputKeys.ENCODING, props.getProperty("encoding"));
			DOMResult result = new DOMResult();
			//StreamResult result = new StreamResult(new File(workDir, objId + "-transformed.xml"));	
			t.transform(new DOMSource(spDoc), result);	        
			
			Document resDoc = (Document) result.getNode();
                			
			String budgetHead = (String) xp.evaluate("/package/budgetHead", 
					resDoc.getDocumentElement(), XPathConstants.STRING);

			// append 
			if (budgetHead != null && !budgetHead.trim().isEmpty()) {	// only if there's a budget head
				System.out.println("include package. budget head=" + budgetHead);
				packagesElem.appendChild(doc.importNode(resDoc.getDocumentElement(), true));
			} else {
				System.out.println("skip package. no budget head");
			}
		}
		
		rootElem.appendChild(packagesElem);
		
		// write the content into xml file
		DOMSource source = new DOMSource(doc);
		StreamResult result = new StreamResult(
				new File(props.getProperty("outputDir"), "budget_" + pubDate + "_" + pub + "_jdbc.xml"));	
		Transformer t = tf.newTransformer();
		t.setOutputProperty(OutputKeys.METHOD, "xml");
		t.setOutputProperty(OutputKeys.INDENT, "no");
		t.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
		t.setOutputProperty(OutputKeys.ENCODING, props.getProperty("encoding"));		
		t.transform(source, result);		
		
		logger.exiting(loggerName, "write");
	}	
}
