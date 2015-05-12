package com.atex.h11.custom.newsday.export.budget;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;
import java.util.logging.Logger;

import com.atex.h11.custom.common.DataSource;
import com.unisys.media.cr.adapter.ncm.model.data.datasource.NCMDataSource;

public class Exporter {
	private static final String loggerName = Exporter.class.getName();
    private static final Logger logger = Logger.getLogger(loggerName);
    
	private static NCMDataSource ds = null; 

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
	
	public void run() {
		logger.entering(loggerName, "run");
		
		logger.exiting(loggerName, "run");   
	}
}
