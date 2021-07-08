package com.bakdata.conquery.apiv1.query.concept.specific.external;

import com.bakdata.conquery.io.cps.CPSType;

public class IgnoreColumn extends FormatColumn {

	public static final String HANDLE = "IGNORED";

	public String read(String[] row) {
		return null;
	}
}
