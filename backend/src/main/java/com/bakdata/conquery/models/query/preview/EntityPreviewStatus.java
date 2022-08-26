package com.bakdata.conquery.models.query.preview;

import java.util.List;
import java.util.Set;

import com.bakdata.conquery.apiv1.FullExecutionStatus;
import com.bakdata.conquery.models.types.SemanticType;
import lombok.Data;
import lombok.Value;

@Data
public class EntityPreviewStatus extends FullExecutionStatus {
	@Value
	public static class Info {
		private final String label;
		private final Object value;
		private final String type;
		private final Set<SemanticType> semantics;
	}

	private List<Info> infos;
}
