package com.atex.h11.custom.newsday.export.budget;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.sql.Timestamp;
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
import com.unisys.media.ncm.cfg.common.data.values.LevelValue;
import com.unisys.media.ncm.cfg.model.values.UserHermesCfgValueClient;

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
		
		String pubDateString = Constants.DELIMITED_DATE_FORMAT.format(pubDate);

		Map<Integer, String> packages = new HashMap<Integer, String>();

		/*
		 * get paginated packages
		 */		
		logger.info("Find paginated packages");
		QueryFilterClient paginatedQuery = (QueryFilterClient) ds.newQuery("ncm-object");
		
		// paginated package conditions
		Condition isPaginatedStoryPackage = paginatedQuery.newCondition(INCMCondition.OBJ_TYPE, INCMCondition.EQUAL, Integer.toString(NCMObjectNodeType.OBJ_STORY_PACKAGE));
		Condition isPaginated = paginatedQuery.newCondition(INCMCondition.LAY_PAGE_ID, INCMCondition.GREATER, 0);
		Condition isLayInPubLevel = getPubLevelCondition(paginatedQuery, INCMCondition.LAY_LEVEL_ID);		
		Condition isLayPubDateWithinRangeStart = paginatedQuery.newCondition(INCMCondition.LAY_PUB_DATE, INCMCondition.GREATEROREQUAL, pubDateString + " 00:00:00");
		Condition isLayPubDateWithinRangeEnd = paginatedQuery.newCondition(INCMCondition.LAY_PUB_DATE, INCMCondition.LESSOREQUAL, pubDateString + " 23:59:59");
		
		Condition paginatedCondition = isPaginatedStoryPackage;
		paginatedCondition = paginatedCondition.andCondition(isPaginated);
		paginatedCondition = paginatedCondition.andCondition(isLayInPubLevel);
		paginatedCondition = paginatedCondition.andCondition(isLayPubDateWithinRangeStart.andCondition(isLayPubDateWithinRangeEnd));
			
		addQueryResultsToMap(packages, paginatedQuery, paginatedCondition);	// run query
    					
		/*
		 * get non-paginated packages
		 */		
		logger.info("Find non-paginated packages");
		QueryFilterClient nonPaginatedQuery = (QueryFilterClient) ds.newQuery("ncm-object");
		
		// non-paginated package conditions		
		Condition isNonPaginatedStoryPackage = nonPaginatedQuery.newCondition(INCMCondition.OBJ_TYPE, INCMCondition.EQUAL, Integer.toString(NCMObjectNodeType.OBJ_STORY_PACKAGE));		
		Condition isNotPaginated = nonPaginatedQuery.newCondition(INCMCondition.LAY_PAGE_ID, INCMCondition.EQUAL, 0);
		Condition isObjInPubLevel = getPubLevelCondition(nonPaginatedQuery, INCMCondition.OBJ_LEVEL_ID);		
		Condition isExpPubDateWithinRangeStart = nonPaginatedQuery.newCondition(INCMCondition.OBJ_EXP_PUBDATE, INCMCondition.LESSOREQUAL, pubDateString + " 23:59:59");
		Condition isExpPubDateWithinRangeEnd = nonPaginatedQuery.newCondition(INCMCondition.OBJ_EXP_PUBDATE_TO, INCMCondition.GREATEROREQUAL, pubDateString + " 00:00:00");
				
		Condition nonPaginatedCondition = isNonPaginatedStoryPackage;
		nonPaginatedCondition = nonPaginatedCondition.andCondition(isObjInPubLevel);
		nonPaginatedCondition = nonPaginatedCondition.andCondition(isNotPaginated);
		nonPaginatedCondition = nonPaginatedCondition.andCondition(isExpPubDateWithinRangeStart.andCondition(isExpPubDateWithinRangeEnd));
			
		addQueryResultsToMap(packages, nonPaginatedQuery, nonPaginatedCondition);	// run query		
		
		logger.exiting(loggerName, "getPackagesToExport");
		return packages;
	}
	
	private Condition getPubLevelCondition(QueryFilterClient query, String queryPropertyDefName) {
		logger.entering(loggerName, "getPubLevelCondition");
		Condition cond = null;
		
        UserHermesCfgValueClient cfgVC = ds.getUserHermesCfg();
		
		String[] levels = props.getProperty(pub + ".levels").split(",");
		for (int i = 0; i < levels.length; i++) {
	        LevelValue levelV = cfgVC.findLevelByName(levels[i].trim());	
	        String levelWildCard = String.format("%02X", levelV.getId()[0]) + "%";		// use main level in hex as wildcard
	        if (i == 0) {
	        	cond = query.newCondition(queryPropertyDefName, INCMCondition.LIKE, levelWildCard);
	        } else {
	        	cond = cond.orCondition(query.newCondition(queryPropertyDefName, INCMCondition.LIKE, levelWildCard));	// or
	        }
	        	
		}
		
		logger.exiting(loggerName, "getPubLevelCondition");
		return cond;
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
		pubDateElem.appendChild(doc.createTextNode(Constants.NON_DELIMITED_DATE_FORMAT.format(pubDate)));
		pubInfoElem.appendChild(pubDateElem);
		
		Element pubElem = doc.createElement("pub");
		pubElem.appendChild(doc.createTextNode(pub));
		pubInfoElem.appendChild(pubElem);
		
		rootElem.appendChild(pubInfoElem);
		
		
		// packages
		Element packagesElem = doc.createElement("packages");
		int count = 0;
		
		logger.info("Exporting packages...");
		for (int spId : packages.keySet()) {
			String spName = packages.get(spId);
			logger.info("Package: id=" + spId + ", name=" + spName);
            			
            NCMObjectPK pk = new NCMObjectPK(spId);
			StoryPackage sp = new StoryPackage(ds);
	        sp.setConvertFormat(props.getProperty("convertFormat"));			
	        sp.setDateFormat(Constants.DELIMITED_DATETIME_FORMAT);			            
            Document spDoc = sp.getDocument(pk);
            
            if (props.getProperty("debug").equalsIgnoreCase("true")) {	// for debug: dump orig xml from H11 per package
            	writeDocumentToFile(spDoc, 
        			new File(props.getProperty("debugDir"), Integer.toString(spId) + "_" + spName + "_orig.xml"));
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
        			new File(props.getProperty("debugDir"), Integer.toString(spId) + "_" + spName + "_transformed.xml"));            	
			}			

            // check that resulting xml has content
			boolean hasContent = (Boolean) xp.evaluate("/package//text()", 
					resDoc.getDocumentElement(), XPathConstants.BOOLEAN);

			if (hasContent) {	// append only if there's content
				packagesElem.appendChild(doc.importNode(resDoc.getDocumentElement(), true));
				count++;
				logger.info("Package exported: id=" + spId + ", name=" + spName);
			} else {
				logger.info("Package not exported: id=" + spId + ", name=" + spName + ". Reason: no content");
			}
		}
		
		packagesElem.setAttribute("count", Integer.toString(count));
		rootElem.appendChild(packagesElem);
		logger.info("Packages exported count=" + count);
		
		
		// write the output into a file
		Properties outputProps = new Properties();
		outputProps.put(OutputKeys.METHOD, "xml");
		outputProps.put(OutputKeys.INDENT, "yes");
		outputProps.put(OutputKeys.OMIT_XML_DECLARATION, "no");
		outputProps.put(OutputKeys.ENCODING, props.getProperty("encoding"));	
		
		Timestamp currentTimestamp = new Timestamp(new Date().getTime());
		File outputFile = new File(props.getProperty("outputDir"), 
			"budget_" + Constants.NON_DELIMITED_DATE_FORMAT.format(pubDate) + "_" + pub
			+ "_" + Constants.NON_DELIMITED_DATETIME_FORMAT.format(currentTimestamp)
			+ ".xml");
		writeDocumentToFile(doc, outputFile, outputProps);
		logger.info("Exported to output file: " + outputFile.getPath());
		
		logger.exiting(loggerName, "write");
	}	
	
	private void writeDocumentToFile(Document doc, File file) 
			throws TransformerException {
		writeDocumentToFile(doc, file, null);
	}
	
	private void writeDocumentToFile(Document doc, File file, Properties outputProps) 
			throws TransformerException {
		DOMSource source = new DOMSource(doc);
		StreamResult result = new StreamResult(file);

		Transformer t = tf.newTransformer();		
		if (outputProps != null) {
			t.setOutputProperties(outputProps);
		} else {
			t.setOutputProperty(OutputKeys.METHOD, "xml");
			t.setOutputProperty(OutputKeys.INDENT, "no");
			t.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
			t.setOutputProperty(OutputKeys.ENCODING, Constants.DEFAULT_ENCODING);			
		}
		t.transform(source, result);			
	}
}
