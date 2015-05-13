package com.atex.h11.custom.newsday.export.budget;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

public interface Constants {
	// Default Hermes user and password
	public static final String DEFAULT_HERMES_USER = "BATCH";
	public static final String DEFAULT_HERMES_PASSWORD = "BATCH";
		
	public static final String DEFAULT_ENCODING = "UTF-8";

	public static final DateFormat NON_DELIMITED_DATE_FORMAT = new SimpleDateFormat("yyyyMMdd");
	public static final DateFormat NON_DELIMITED_DATETIME_FORMAT = new SimpleDateFormat("yyyyMMddHHmmss");
	public static final DateFormat DELIMITED_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
	public static final DateFormat DELIMITED_DATETIME_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
}
