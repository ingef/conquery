package com.bakdata.conquery.util.io;

import org.slf4j.MDC;

public class ConqueryMDC {

	private static final String LOCATION = "location";
	private static final String NODE = "node";

	public static void setLocation(String location) {
		MDC.put(LOCATION, location);
	}

	/**
	 * Use to set the node name (e.g. manager, shard-0, ...) in log message
	 */
	public static void setNode(String node) {
		MDC.put(NODE, node);
	}

	public static void clearLocation() {
		MDC.remove(LOCATION);
	}

	public static void clearNode() {
		MDC.remove(NODE);
	}

	public static String getNode() {
		return MDC.get(NODE);
	}
}
