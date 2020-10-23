package com.bakdata.conquery.models.preproc;

import javax.validation.constraints.NotNull;

import com.bakdata.conquery.models.dictionary.DictionaryMapping;
import com.bakdata.conquery.models.types.CType;
import com.bakdata.conquery.models.types.parser.Decision;
import com.bakdata.conquery.models.types.parser.Parser;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Data @RequiredArgsConstructor @NoArgsConstructor
public class PPColumn {
	@NonNull
	private String name;

	@SuppressWarnings("rawtypes") @NotNull
	private CType type;
	@SuppressWarnings("rawtypes") @JsonIgnore
	private transient Parser parser = null;
	@JsonIgnore
	private transient DictionaryMapping valueMapping;
	@JsonIgnore
	private transient CType oldType;

	public void findBestType() {
		Decision typeDecision = parser.findBestType();
		type = typeDecision.getType();
	}
}
