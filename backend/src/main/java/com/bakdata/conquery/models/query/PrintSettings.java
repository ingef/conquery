package com.bakdata.conquery.models.query;

import com.bakdata.conquery.models.query.resultinfo.SelectResultInfo;

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
	private SelectNameExtractor selectNameExtractor = sd -> sd.getSelect().getLabel();
	
	public static interface SelectNameExtractor {
		String extract(SelectResultInfo descriptor);
	}
}
