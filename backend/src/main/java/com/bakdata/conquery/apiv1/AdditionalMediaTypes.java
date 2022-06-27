package com.bakdata.conquery.apiv1;

public interface AdditionalMediaTypes {

	static final String JSON = "application/json; charset=utf-8";

	static final String CSV = "text/csv; charset=utf-8";
	static final String EXCEL = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
	
	// As proposed in https://issues.apache.org/jira/browse/ARROW-7396
	static final String ARROW_STREAM = "application/vnd.apache.arrow.stream";
	static final String ARROW_FILE = "application/vnd.apache.arrow.file";
}
