package com.bakdata.conquery.apiv1;

import lombok.experimental.UtilityClass;

@UtilityClass
public class AdditionalHeaders {

	public static final String HTTP_HEADER_REAL_IP = "X-Forwarded-For";
	public static final String HTTP_HEADER_REAL_HOST = "X-Forwarded-Host";
	public static final String HTTP_HEADER_REAL_PROTO = "X-Forwarded-Proto";
}
