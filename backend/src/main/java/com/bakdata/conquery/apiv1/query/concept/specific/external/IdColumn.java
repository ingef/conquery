package com.bakdata.conquery.apiv1.query.concept.specific.external;

import com.bakdata.conquery.io.cps.CPSType;

@CPSType(id = "ID", base = FormatColumn.class)
public class IdColumn extends FormatColumn {

	public String read(String[] row) {
		return row[getPosition()];
	}
}
