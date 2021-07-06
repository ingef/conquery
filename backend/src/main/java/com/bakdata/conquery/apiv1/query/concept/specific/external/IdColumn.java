package com.bakdata.conquery.apiv1.query.concept.specific.external;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.identifiable.mapping.UnresolvedEntityId;

@CPSType(id = "ID", base = FormatColumn.class)
public class IdColumn extends FormatColumn {


	public UnresolvedEntityId read(String[] row) {
		return new UnresolvedEntityId(row[getPosition()]);
	}
}
