package com.bakdata.conquery.models.identifiable.mapping;

import com.bakdata.conquery.models.query.results.EntityResult;

/**
 * Implementing classes are used to create Printable Ids for results.
 */
public interface IdPrinter {
	EntityPrintId createId(EntityResult entityResult);
}
