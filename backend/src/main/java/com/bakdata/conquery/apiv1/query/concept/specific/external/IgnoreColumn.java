package com.bakdata.conquery.apiv1.query.concept.specific.external;

import com.bakdata.conquery.io.cps.CPSType;

@CPSType(id = "IGNORE", base = FormatColumn.class)
public class IgnoreColumn extends FormatColumn {

	public String read(String[] row) {
		return null;
	}
}
