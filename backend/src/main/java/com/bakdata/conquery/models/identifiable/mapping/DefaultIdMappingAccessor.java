package com.bakdata.conquery.models.identifiable.mapping;

import lombok.Getter;
import lombok.ToString;

@ToString
public abstract class DefaultIdMappingAccessor implements IdMappingAccessor {

	@Getter
	private final int[] idsUsed;
	@Getter
	private final String[] header;

	/**
	 * Select values from array in order by index.
	 * @param values The input values to select from
	 * @param indices The indices of the values to select and the order to select them in.
	 */
	public static String[] select(String[] values, int[] indices){
		String[] out = new String[indices.length];

		for (int index = 0; index < indices.length; index++) {
			out[index] = values[indices[index]];
		}

		return out;
	}

	public DefaultIdMappingAccessor(String[] header, int[] idsUsed) {
		this.idsUsed = idsUsed;
		this.header = select(header, idsUsed);
	}

	@Override
	public String[] extract(String[] dataLine) {
		return select(dataLine, idsUsed);
	}
}
