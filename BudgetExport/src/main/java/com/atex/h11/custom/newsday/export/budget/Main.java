package com.atex.h11.custom.newsday.export.budget;

import java.io.FileInputStream;
import java.util.Arrays;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Main {
	private static final String loggerName = Main.class.getName();
    private static final Logger logger = Logger.getLogger(loggerName);
 
	public static void main(String[] args) {
		logger.entering(loggerName, "main");
		logger.info("Export started. Arguments: " + Arrays.toString(args));
		
		Properties props = new Properties();
		String credentials = null;
		Integer pubDate = null;		
		String pub = null;
		
		try {
            // Gather command line parameters.
            for (int i = 0; i < args.length; i++) {
            	// properties file
                if (args[i].equals("-p"))
                    props.load(new FileInputStream(args[++i]));
                else if (args[i].startsWith("-p"))
                    props.load(new FileInputStream(args[i].substring(2)));
            	// credentials user and password
                if (args[i].equals("-c"))
                	credentials = args[++i].trim();
                else if (args[i].startsWith("-c"))
                	credentials = args[++i].trim();                
                // pubdate 
                else if (args[i].equals("-d"))
                    pubDate = Integer.parseInt(args[++i].trim());
                else if (args[i].startsWith("-d"))
                	pubDate = Integer.parseInt(args[i].substring(2).trim());
                // pub level
                else if (args[i].equals("-l"))
                    pub = args[++i].trim().toUpperCase();
                else if (args[i].startsWith("-l"))
                	pub = args[i].substring(2).trim().toUpperCase();
            }
            
            // credentials for connecting to the datasource
        	String user = Constants.DEFAULT_HERMES_USER;
        	String password = Constants.DEFAULT_HERMES_PASSWORD;
        	if (credentials != null && ! credentials.equals("")) {
    	    	String[] creds = credentials.split(":");
    	    	if (creds[0] != null) user = creds[0];
    	    	if (creds[1] != null) password = creds[1];
        	}      
        	
        	Exporter exporter = new Exporter(props, user, password, pub, pubDate);
        	exporter.run();
        	
		    logger.info("Export completed.");
		    
		} catch (Exception e) {
        	logger.log(Level.SEVERE, "Error encountered", e);
        	
        } finally {
		    logger.exiting(loggerName, "main");        	        	        	
        }
	}
}
