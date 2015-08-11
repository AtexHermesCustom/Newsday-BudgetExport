package com.atex.h11.custom.newsday.export.budget;

import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.atex.h11.custom.newsday.export.budget.util.CustomException;

public class Main {
	private static final String loggerName = Main.class.getName();
    private static final Logger logger = Logger.getLogger(loggerName);
 
	public static void main(String[] args) {
		logger.entering(loggerName, "main");
		logger.info("Budget Export started. Arguments: " + Arrays.toString(args));
		
		Properties props = null;
		String credentials = null;
		List<Date> pubDates = new ArrayList<Date>();
		Date singlePubDate = null;
		String dateDeltaRange = null;
		String pub = null;
		String outputFilename = null;
		
		try {
            // Gather command line parameters.
            for (int i = 0; i < args.length; i++) {
            	// properties file
                if (args[i].equals("-p")) {
                	props = new Properties();
                    props.load(new FileInputStream(args[++i]));
                }
                else if (args[i].startsWith("-p")) {
                	props = new Properties();
                    props.load(new FileInputStream(args[i].substring(2).trim()));
                }
                
            	// credentials user and password
                else if (args[i].equals("-c"))
                	credentials = args[++i].trim();
                else if (args[i].startsWith("-c"))
                	credentials = args[i].substring(2).trim();
                
                // pubdate 
                else if (args[i].equals("-d"))
                	singlePubDate = Constants.NON_DELIMITED_DATE_FORMAT.parse(args[++i].trim());
                else if (args[i].startsWith("-d"))
                	singlePubDate = Constants.NON_DELIMITED_DATE_FORMAT.parse(args[i].substring(2).trim());

                // pubdate days delta
                else if (args[i].equals("-e"))
                	dateDeltaRange = args[++i].trim();
                else if (args[i].startsWith("-e"))
                	dateDeltaRange = args[i].substring(2).trim();                
                
                // pub level
                else if (args[i].equals("-l"))
                    pub = args[++i].trim().toUpperCase();
                else if (args[i].startsWith("-l"))
                	pub = args[i].substring(2).trim().toUpperCase();
                
            	// output file name
                else if (args[i].equals("-o"))
                	outputFilename = args[++i].trim();
                else if (args[i].startsWith("-o"))
                	outputFilename = args[i].substring(2).trim();                
            }
                        
            if (singlePubDate != null) {	// passed single pubdate
            	pubDates.add(singlePubDate);
            	
            } else if (dateDeltaRange != null && ! dateDeltaRange.isEmpty()) {		// determine pub date using days delta param
    	    	String[] range = dateDeltaRange.split(":");
    	    	
    	    	int deltaStart, deltaEnd;
    	    	deltaStart = 1;	// init value: 1 means tomorrow's date
    	    	if (range[0] != null) deltaStart = Integer.parseInt(range[0]);
    	    	deltaEnd = deltaStart; // init to same value as deltaStart
    	    	if (range.length == 2 && range[1] != null) deltaEnd = Integer.parseInt(range[1]);
    	    	
    	    	if (deltaEnd < deltaStart) deltaEnd = deltaStart;	// if invalid range, just set to same value
    	    	
    	    	for (int i = deltaStart; i <= deltaEnd; i++) {
    	    		// include all pubdates in range
	            	Calendar c = Calendar.getInstance(); 
	            	c.setTime(Constants.NON_DELIMITED_DATE_FORMAT.parse(Constants.NON_DELIMITED_DATE_FORMAT.format(new Date()))); 
	            	c.add(Calendar.DATE, i);	// add delta
	            	Date d = c.getTime();
	            	pubDates.add(d);
    	    	}
            }
            
            if (props == null) { 
            	throw new CustomException("Missing argument: properties");
            }
            if (pub == null) {
            	throw new CustomException("Missing argument: pub");
            }
            if (pubDates == null || pubDates.size() <= 0) {
            	throw new CustomException("Missing or invalid argument: pubDate or dateDeltaRange");
            }
            
            String pubLevels = props.getProperty(pub + ".levels");
            if (pubLevels == null || pubLevels.isEmpty()) {
            	throw new CustomException("Invalid argument: pub or Missing property value: \"" + pub + ".levels\"");
            }
            
            // credentials for connecting to the datasource
        	String user = Constants.DEFAULT_HERMES_USER;
        	String password = Constants.DEFAULT_HERMES_PASSWORD;
        	if (credentials != null && ! credentials.isEmpty()) {
    	    	String[] creds = credentials.split(":");
    	    	if (creds[0] != null) user = creds[0];
    	    	if (creds.length == 2 && creds[1] != null) password = creds[1];
        	}      
        	
        	// go
        	Exporter exporter = new Exporter(props, user, password, pub, pubDates);
        	if (outputFilename != null && !outputFilename.isEmpty()) {
        		exporter.setOutputFilename(outputFilename);
        	}
        	exporter.run();
        	
		    logger.info("Export completed.");
		    
		} catch (Exception e) {
        	logger.log(Level.SEVERE, "Error encountered", e);
        	
        } finally {
		    logger.exiting(loggerName, "main");        	        	        	
        }
	}
}
