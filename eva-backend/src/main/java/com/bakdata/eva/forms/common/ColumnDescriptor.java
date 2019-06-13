package com.bakdata.eva.forms.common;

import com.bakdata.conquery.models.externalservice.ResultType;

import lombok.Builder;
import lombok.Data;

@Data @Builder
public class ColumnDescriptor {
	
	public static enum MatchingType {PSM, EXACT, DESCRIPTIVE, NONE, FIXED}
	public static enum ColumnType { FIXED, VARIABLE }
	
	private String label;
	private String description;
	private String column;
	private ResultType type;
	private String rootConcept;
	
	// Used by PSM
	private MatchingType matchingType;
	
	// Used by AU
	private Boolean baseCondition;
	
	// Used by Descriptive
	private ColumnType columnType;
}
