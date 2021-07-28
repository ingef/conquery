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

	private final int size;
	private final int idPos;

	@Override
	public EntityPrintId createId(EntityResult entityResult){

		String csvEntityId = dictionary.getElement(entityResult.getEntityId());
		// The state may be uses by implementations of this class

		if (idMapping == null) {
			return EntityPrintId.from(csvEntityId);
		}

		EntityPrintId externalEntityId = idMapping.toExternal(csvEntityId);

		if (externalEntityId == null) {
			final String[] parts = new String[size];
			parts[idPos] = csvEntityId;

			return EntityPrintId.from(parts);
		}

		return externalEntityId;
	}

}
