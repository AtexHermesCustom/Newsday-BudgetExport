package com.atex.h11.custom.newsday.export.budget;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
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
import com.atex.h11.custom.newsday.export.budget.util.MapUtil;
import com.atex.h11.custom.newsday.export.budget.util.XSLTMessageReceiver;
import com.unisys.media.cr.adapter.ncm.common.data.interfaces.query.INCMCondition;
import com.unisys.media.cr.adapter.ncm.common.data.pk.NCMObjectPK;
import com.unisys.media.cr.adapter.ncm.common.data.types.NCMObjectNodeType;
import com.unisys.media.cr.adapter.ncm.model.data.datasource.NCMDataSource;
import com.unisys.media.cr.adapter.ncm.model.data.values.NCMObjectValueClient;
import com.unisys.media.cr.common.data.query.Condition;
import com.unisys.media.cr.common.data.query.FetchMode;
import com.unisys.media.cr.model.data.query.IQueryClient;
import com.unisys.media.cr.model.data.query.QueryFilterClient;
import com.unisys.media.cr.model.data.query.QueryResultClient;
import com.unisys.media.cr.model.data.values.INodeValueClient;
import com.unisys.media.extension.common.serialize.xml.XMLSerializeWriterException;

public class Exporter {
	private static final String loggerName = Exporter.class.getName();
    private static final Logger logger = Logger.getLogger(loggerName);
    
	private static NCMDataSource ds = null;
	
	private static TransformerFactory tf = null;
	private static XPathFactory xpf = null;
	private static DocumentBuilderFactory dbf = null;		

	private Properties props = null;
	private String pub;
	private Date pubDate;	
    
	public Exporter(Properties props, String user, String password, String pub, Date pubDate) 
			throws FileNotFoundException, IOException {
		this.props = props;
		this.pub = pub;
		this.pubDate = pubDate;
		
		// get H11 data source
		ds = DataSource.newInstance(user, password);
	}
	
	public void run() 
			throws UnsupportedEncodingException, XPathExpressionException, ParserConfigurationException, IOException, 
			XMLSerializeWriterException, SAXException, TransformerException {
		logger.entering(loggerName, "run");

		// get packages to export
		Map<Integer, String> packages = getPackagesToExport();
		
		// sort packages by name
		Map<Integer, String> sortedPackages = MapUtil.sortMapByValue(packages);
		
		// export
		write(sortedPackages);		
		
		logger.exiting(loggerName, "run");   
	}
	
	private Map<Integer, String> getPackagesToExport() {
		logger.entering(loggerName, "getPackagesToExport");   
		
		String pubDateString = Constants.DEFAULT_DATE_FORMAT.format(pubDate);

		Map<Integer, String> packages = new HashMap<Integer, String>();
		QueryFilterClient query = (QueryFilterClient) ds.newQuery("ncm-object");
		Condition queryCondition;

		/*
		 * get paginated packages
		 */		
		logger.info("Find paginated packages");
		Condition isStoryPackage = query.newCondition(INCMCondition.OBJ_TYPE, INCMCondition.EQUAL, Integer.toString(NCMObjectNodeType.OBJ_STORY_PACKAGE));
		Condition isInPubLevel = query.newCondition(INCMCondition.LAY_LEVEL_ID, INCMCondition.LIKE, "03%");
		Condition isPaginated = query.newCondition(INCMCondition.LAY_PAGE_ID, INCMCondition.GREATER, 0);
		Condition isLayPubDateWithinRangeStart = query.newCondition(INCMCondition.LAY_PUB_DATE, INCMCondition.GREATEROREQUAL, pubDateString + " 00:00:00");
		Condition isLayPubDateWithinRangeEnd = query.newCondition(INCMCondition.LAY_PUB_DATE, INCMCondition.LESSOREQUAL, pubDateString + " 23:59:59");
		
		queryCondition = isStoryPackage;
		queryCondition = queryCondition.andCondition(isInPubLevel);
		queryCondition = queryCondition.andCondition(isPaginated);
		queryCondition = queryCondition.andCondition(isLayPubDateWithinRangeStart.andCondition(isLayPubDateWithinRangeEnd));
			
		addQueryResultsToMap(packages, query, queryCondition);	// run query
    					
		/*
		 * get non-paginated packages
		 */		
		logger.info("Find non-paginated packages");
		Condition isNotPaginated = query.newCondition(INCMCondition.LAY_PAGE_ID, INCMCondition.EQUAL, 0);
		Condition isExpPubDateWithinRangeStart = query.newCondition(INCMCondition.OBJ_EXP_PUBDATE, INCMCondition.LESSOREQUAL, pubDateString + " 23:59:59");
		Condition isExpPubDateWithinRangeEnd = query.newCondition(INCMCondition.OBJ_EXP_PUBDATE_TO, INCMCondition.GREATEROREQUAL, pubDateString + " 00:00:00");
				
		logger.exiting(loggerName, "getPackagesToExport");
		return packages;
	}
	
	private void addQueryResultsToMap(Map<Integer, String> packages, QueryFilterClient query, Condition queryCondition) {		
		logger.entering(loggerName, "addQueryResultsToMap");
		
		query.setCondition(queryCondition);
		logger.info("Query condition: " + query.toString());
		
		IQueryClient qc = query.run();
		FetchMode fm = new FetchMode(Integer.parseInt(props.getProperty("fetchMaxItems")));
		
		QueryResultClient res = (QueryResultClient) qc.fetch(fm);		
		qc.close();	// can close the query now
		
		Iterator <INodeValueClient> iter = res.getNodesArray().iterator();
		while (iter.hasNext()) {
			NCMObjectValueClient obj = (NCMObjectValueClient) iter.next();
			int objId = getObjIdFromPK(obj.getPK().toString());
			String objName =  obj.getNCMName();
			
			if (!packages.containsKey(objId)) {
				packages.put(objId, objName);
				logger.info("Found package: id=" + objId
					+ ", name=" + objName
					//+ ", type=" + obj.getType()
					+ ", expPubDate=" + obj.getExpPubDate()
					+ ", expPubDateTo=" + obj.getExpPubDateTo());
			}
		}
		
		logger.info("Found " + res.getCount() + " packages");
		
		logger.exiting(loggerName, "addQueryResultsToMap");
	}
	
	private int getObjIdFromPK(String pk) {
		return Integer.parseInt(pk.substring(0, pk.indexOf(":")));
	}
	
	private void write(Map<Integer, String> packages) 
			throws ParserConfigurationException, UnsupportedEncodingException, IOException, 
			XMLSerializeWriterException, SAXException, TransformerException, XPathExpressionException {
		logger.entering(loggerName, "write");
		
		// load transform stylesheet
		File xslFile = new File(props.getProperty("transformStylesheet"));
		logger.info("Transform stylesheet: " + xslFile.getPath());
		
		tf = TransformerFactory.newInstance();		
		Templates cachedStylesheet = tf.newTemplates(new StreamSource(xslFile));				
		
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
		
		logger.info("Exporting packages...");
		for (int spId : packages.keySet()) {
			String spName = packages.get(spId);
			logger.info("Package: id=" + spId + ", name=" + spName);
            			
            NCMObjectPK pk = new NCMObjectPK(spId);
			StoryPackage sp = new StoryPackage(ds);
	        sp.setConvertFormat(props.getProperty("convertFormat"));			
	        sp.setDateFormat(Constants.DEFAULT_DATETIME_FORMAT);			            
            Document spDoc = sp.getDocument(pk);
            
            if (props.getProperty("debug").equalsIgnoreCase("true")) {	// for debug: dump orig xml from H11 per package
            	writeDocumentToFile(spDoc, 
        			new File(props.getProperty("debugDir"), Integer.toString(spId) + "-" + spName + "-orig.xml"),
        			props.getProperty("encoding"));
			}
            	            
            // transform package document
            Transformer t = cachedStylesheet.newTransformer();
    		Controller controller = (Controller) t;
    		Receiver receiver = new XSLTMessageReceiver(logger);	// for logging messages from XSLT
    		controller.setMessageEmitter(receiver);            

    		// set parameters - read from properties file
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
            if (props.getProperty("debug").equalsIgnoreCase("true")) {	// for debug: dump transformed xml per package
            	writeDocumentToFile(resDoc, 
        			new File(props.getProperty("debugDir"), Integer.toString(spId) + "-" + spName + "-transformed.xml"),
        			props.getProperty("encoding"));            	
			}			

            // check that resulting xml has content
			boolean hasContent = (boolean) xp.evaluate("/package//text()", 
					resDoc.getDocumentElement(), XPathConstants.BOOLEAN);

			if (hasContent) {	// append only if there's content
				packagesElem.appendChild(doc.importNode(resDoc.getDocumentElement(), true));
				logger.info("Package exported: id=" + spId + ", name=" + spName);
			} else {
				logger.info("Package not exported: id=" + spId + ", name=" + spName + ". Reason: no content");
			}
		}
		
		rootElem.appendChild(packagesElem);
		
		
		// write the output into a file	
		File outputFile = new File(props.getProperty("outputDir"), 
			"budget_" + Constants.PARAM_DATE_FORMAT.format(pubDate) + "_" + pub + ".xml");
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
