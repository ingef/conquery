package com.bakdata.conquery.apiv1;

import javax.ws.rs.core.MediaType;

import org.apache.http.entity.ContentType;

public interface AdditionalMediaTypes {

	static final String JSON = "application/json; charset=utf-8";
	static final MediaType JSON_TYPE = MediaType.valueOf(JSON);
	static final ContentType JSON_CT = ContentType.create("application/json", "utf-8");

	static final String CSV = "text/csv; charset=utf-8";
	static final MediaType CSV_TYPE = MediaType.valueOf(CSV);
	static final ContentType CSV_CT = ContentType.create("text/csv", "utf-8");
}
