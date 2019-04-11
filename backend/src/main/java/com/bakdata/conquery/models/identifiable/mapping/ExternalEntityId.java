package com.bakdata.conquery.models.identifiable.mapping;

import lombok.Data;

/**
 * An external Id for a Entity.
 *
 */
@Data
public class ExternalEntityId implements EntityId{
	/**
	 * The external Entity Id.
	 */
	private final String[] externalId;

	/**
	 * Casts a given csv Entity Id into an ExternalEntityId.
	 * @param csvEntityId the given csvEntityId.
	 * @return the casted ExternalEntityId.
	 */
	public static ExternalEntityId from(CsvEntityId csvEntityId){
		return new ExternalEntityId(new String[]{ csvEntityId.getCsvId()});
	}
}
