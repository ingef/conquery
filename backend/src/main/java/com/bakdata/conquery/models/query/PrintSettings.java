package com.bakdata.conquery.models.query;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Getter @Builder @AllArgsConstructor @NoArgsConstructor @ToString
public class PrintSettings {
	@Builder.Default
	private boolean prettyPrint = true;
}
