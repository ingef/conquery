package com.bakdata.conquery.models.preproc;

import com.bakdata.conquery.models.preproc.outputs.Output;
import com.bakdata.conquery.models.types.CType;
import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Data @RequiredArgsConstructor @NoArgsConstructor
public class PPColumn {
	@NonNull
	private String name;
	@SuppressWarnings("rawtypes") @NonNull
	private CType type;
	@JsonIgnore @SuppressWarnings("rawtypes")
	private transient CType originalType = null;

	public PPColumn(Output output) {
		this(output.getName(), output.getResultType().createType());
	}

	public void findBestType() {
		originalType = type;
		type = type.bestSubType();
	}
}
