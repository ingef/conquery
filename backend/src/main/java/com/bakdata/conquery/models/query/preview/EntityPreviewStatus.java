package com.bakdata.conquery.models.query.preview;

import java.util.List;

import com.bakdata.conquery.apiv1.FullExecutionStatus;
import lombok.Data;
import lombok.Value;

@Data
public class EntityPreviewStatus extends FullExecutionStatus {
	@Value
	public static class Info {
		private final String label;
		private final Object value;
		//TODO Semantics?
	}

	private List<Info> infos;
}
