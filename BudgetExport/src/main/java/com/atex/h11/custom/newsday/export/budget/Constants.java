package com.atex.h11.custom.newsday.export.budget;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

public interface Constants {
	// Default Hermes user and password
	public static final String DEFAULT_HERMES_USER = "BATCH";
	public static final String DEFAULT_HERMES_PASSWORD = "BATCH";
		
	public static final String DEFAULT_ENCODING = "UTF-8";
	public static final DateFormat DEFAULT_DATETIME_FORMAT = new SimpleDateFormat("yyyyMMddHHmmss");
}
