package com.bakdata.conquery.models.identifiable.mapping;

import com.bakdata.conquery.models.dictionary.EncodedDictionary;
import com.bakdata.conquery.models.query.results.EntityResult;
import lombok.RequiredArgsConstructor;

/**
 * Maker interface for implementation specific state object during query result to csv rendering.
 */
@RequiredArgsConstructor
public class FullIdPrinter implements IdPrinter {

	private final EncodedDictionary dictionary;
	private final EntityIdMap idMapping;

	@Override
	public EntityPrintId createId(EntityResult entityResult){

		String csvEntityId = dictionary.getElement(entityResult.getEntityId());
		// The state may be uses by implementations of this class

		if (idMapping == null) {
			return EntityPrintId.from(csvEntityId);
		}

		EntityPrintId externalEntityId = idMapping.toExternal(csvEntityId);

		if (externalEntityId == null) {
			return EntityPrintId.from(csvEntityId);
		}

		return externalEntityId;
	}

}
