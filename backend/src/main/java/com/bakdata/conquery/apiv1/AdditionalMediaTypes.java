package com.bakdata.conquery.apiv1;

import javax.ws.rs.core.MediaType;

import org.apache.http.entity.ContentType;

public interface AdditionalMediaTypes {

        public static final String JSON = "application/json; charset=utf-8";
        public static final MediaType JSON_TYPE = MediaType.valueOf(JSON);
        public static final ContentType JSON_CT = ContentType.create("application/json", "utf-8");

        public static final String CSV = "text/csv; charset=utf-8";
        public static final MediaType CSV_TYPE = MediaType.valueOf(CSV);
        public static final ContentType CSV_CT = ContentType.create("text/csv", "utf-8");
}
