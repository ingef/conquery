package com.bakdata.conquery.models.query;

import java.util.function.Function;

import com.bakdata.conquery.models.query.concept.SelectDescriptor;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Getter @Builder @AllArgsConstructor @NoArgsConstructor @ToString
public class PrintSettings {
	@Builder.Default
	private boolean prettyPrint = true;
	
	/**
	 * Defines the function that extracts the base for the resulting column name.
	 * Here we use the label of the select as standard.
	 */
	@Builder.Default
	private Function<SelectDescriptor, String> nameExtractor = sd -> sd.getSelect().getLabel();
}
