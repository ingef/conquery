package com.bakdata.conquery.util.io;

import org.slf4j.MDC;

public class ConqueryMDC {

	private static final String LOCATION = "location";

	public static void setLocation(String location) {
		MDC.put(LOCATION, location);
	}

	public static void clearLocation() {
		MDC.remove(LOCATION);
	}
}
