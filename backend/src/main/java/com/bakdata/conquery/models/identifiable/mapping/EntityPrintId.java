package com.bakdata.conquery.models.identifiable.mapping;

import java.util.Arrays;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonCreator;

import lombok.AccessLevel;
import lombok.Data;
import lombok.RequiredArgsConstructor;

/**
 * An external Id for a Entity.
 */
@Data
@RequiredArgsConstructor(onConstructor_ = @JsonCreator, access = AccessLevel.PROTECTED)
public class EntityPrintId implements Comparable<EntityPrintId> {

	public static EntityPrintId from(String... parts) {
		return new EntityPrintId(parts);
	}

	/**
	 * The external Entity Id.
	 */
	@NotNull
	private final String[] externalId;


	@Override
	public int compareTo(EntityPrintId o) {
		return Arrays.compare(getExternalId(), o.getExternalId());
	}
}
