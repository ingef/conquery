package com.bakdata.conquery.models.query.preview;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.bakdata.conquery.apiv1.FullExecutionStatus;
import com.bakdata.conquery.models.query.ColumnDescriptor;
import com.bakdata.conquery.models.types.SemanticType;
import lombok.Data;

@Data
public class EntityPreviewStatus extends FullExecutionStatus {
	private List<Info> infos;
	private List<TimebasedInfos> timebasedInfos;

	@Data
	public static class Info extends ColumnDescriptor {

		private final Object value;

		public Info(String label, Object value, String typeInfo, String description, Set<SemanticType> semantics) {
			super(label, description, label, typeInfo, semantics);
			this.value = value;
		}
	}


	public record TimebasedInfos(String label, String description, List<ColumnDescriptor> columns, List<YearEntry> years) {

		public record QuarterEntry(int quarter, Map<String, Object> values) {
		}

		public record YearEntry(int year, Map<String, Object> values, List<QuarterEntry> quarters) {
		}
	}
}
