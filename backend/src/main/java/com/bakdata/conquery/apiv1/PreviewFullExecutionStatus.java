package com.bakdata.conquery.apiv1;

import java.util.List;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.FieldNameConstants;

@Data
@EqualsAndHashCode(callSuper = true)
@FieldNameConstants
public class PreviewFullExecutionStatus extends FullExecutionStatus {

	@Data
	public static class Info {
		private final String label;
		private final String value;
		//TODO Semantics?
	}

	private List<Info> infos;

}
