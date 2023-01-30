package com.bakdata.conquery.models.query.preview;

import java.util.List;
import java.util.Set;

import com.bakdata.conquery.apiv1.execution.FullExecutionStatus;
import com.bakdata.conquery.models.query.ColumnDescriptor;
import com.bakdata.conquery.models.types.SemanticType;
import lombok.Data;

@Data
public class EntityPreviewStatus extends FullExecutionStatus {
	@Data
	public static class Info extends ColumnDescriptor {

		public Info(String label, Object value, String typeInfo, String description, Set<SemanticType> semantics) {
			super(label, description, label, typeInfo, semantics);
			this.value = value;
		}

		private final Object value;
	}

	private List<Info> infos;
}
