package com.bakdata.conquery.models.identifiable.mapping;

import lombok.Getter;
import lombok.ToString;

@ToString
public class DefaultIdMappingAccessor implements IdMappingAccessor {

	@Getter
	private final int[] idsUsed;
	@Getter
	@ToString.Exclude
	private final IdMappingConfig mapping;

	public DefaultIdMappingAccessor(IdMappingConfig mapping, int[] idsUsed) {
		this.idsUsed = idsUsed;
		this.mapping = mapping;
	}

	@Override
	public String[] getHeader() {
		return mapping.getHeader();
	}

	@Override
	public String[] extract(String[] dataLine) {
		String[] output = new String[idsUsed.length];
		for (int i = 0; i < idsUsed.length; i++) {
			output[i] = dataLine[idsUsed[i]];
		}
		return output;
	}
}
