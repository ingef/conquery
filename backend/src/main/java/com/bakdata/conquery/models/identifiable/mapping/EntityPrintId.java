package com.bakdata.conquery.models.identifiable.mapping;

import java.util.Arrays;

import com.fasterxml.jackson.annotation.JsonCreator;

import lombok.Data;
import lombok.RequiredArgsConstructor;

/**
 * An external Id for a Entity.
 */
@Data
@RequiredArgsConstructor(onConstructor_ = @JsonCreator)
public class EntityPrintId implements Comparable<EntityPrintId> {

	/**
	 * The external Entity Id.
	 */
	private final String[] externalId;

	/**
	 * Casts a given csv Entity Id into an ExternalEntityId.
	 *
	 * @param csvEntityId the given csvEntityId.
	 * @return the casted ExternalEntityId.
	 */
	public static EntityPrintId from(CsvEntityId csvEntityId) {
		return new EntityPrintId(new String[] {csvEntityId.getCsvId() });
	}

	@Override
	public int compareTo(EntityPrintId o) {
		return Arrays.compare(getExternalId(), o.getExternalId());
	}
}
