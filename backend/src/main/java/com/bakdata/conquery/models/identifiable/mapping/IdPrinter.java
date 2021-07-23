package com.bakdata.conquery.models.identifiable.mapping;

import com.bakdata.conquery.models.query.results.EntityResult;

public interface IdPrinter {
	EntityPrintId createId(EntityResult entityResult);
}
