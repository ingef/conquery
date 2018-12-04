package com.bakdata.conquery.oldmodel;

import java.util.HashMap;
import java.util.Map;

import javax.validation.constraints.Min;

import com.bakdata.conquery.models.preproc.outputs.AutoOutput;
import com.bakdata.conquery.models.preproc.outputs.DaysInRangeOutput;
import com.bakdata.conquery.models.preproc.outputs.DaysInRangeOutput.TypeId;
import com.bakdata.conquery.models.types.MajorTypeId;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Data;

@Data
public class Description {
	private String name;
	private OInput[] inputs;
	
	@Data
	public static class OInput {
		private String sourceFile;
		private String filter;
		private OOutput[] output;
		private OAutoOutput autoOutput;
	}
	
	@Data
	public static class OOutput {
		private String label;
		private String name;
		private String operation;
		private MajorTypeId inputType;
		@JsonIgnore
		private Map<String, Object> unknownFields = new HashMap<>();
		
		@JsonAnyGetter
		public Map<String, Object> otherFields() {
			return unknownFields;
		}

		@JsonAnySetter
		public void setOtherField(String name, Object value) {
			unknownFields.put(name, value);
		}
	}
	
	@Data
	public static class OAutoOutput {
		private OOutput[] identifiers;
		private String type;
		private int yearColumn;
		private DaysInRangeOutput.TypeId dayType;
		private int quarterColumn;
		
	}
}
