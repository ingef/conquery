package com.bakdata.conquery.util.io;

import org.slf4j.MDC;

public enum ConqueryMDC {

	LOCATION,
	/**
	 * Use to set the node name (e.g. manager, shard-0, ...) in log message
	 */
	NODE,
	SUBJECT;

	public void set(String value) {
		MDC.put(name(), value);
	}

	public void clear() {
		MDC.remove(name());
	}

	public String get() {
		return MDC.get(name());
	}
}
