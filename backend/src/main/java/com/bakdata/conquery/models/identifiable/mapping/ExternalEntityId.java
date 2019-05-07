package com.bakdata.conquery.models.identifiable.mapping;

import com.fasterxml.jackson.annotation.JsonCreator;

import lombok.Data;
import lombok.RequiredArgsConstructor;

/**
 * An external Id for a Entity.
 */
@Data
@RequiredArgsConstructor(onConstructor_ = @JsonCreator)
public class ExternalEntityId implements EntityId {

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
	public static ExternalEntityId from(CsvEntityId csvEntityId) {
		return new ExternalEntityId(new String[] { csvEntityId.getCsvId() });
	}
}
