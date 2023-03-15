package com.bakdata.conquery.models.query.preview;

import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import com.bakdata.conquery.apiv1.FullExecutionStatus;
import com.bakdata.conquery.models.query.ColumnDescriptor;
import com.bakdata.conquery.models.types.SemanticType;
import lombok.Data;

@Data
public class EntityPreviewStatus extends FullExecutionStatus {
	private List<Info> infos;
	private List<ChronoInfos> chronoInfos;


	/**
	 * Bundles ColumnDescriptor immediately with a value for EntityPreview.
	 */
	@Data
	public static class Info extends ColumnDescriptor {

		private final Object value;

		public Info(String label, Object value, String typeInfo, String description, Set<SemanticType> semantics) {
			super(label, description, label, typeInfo, semantics);
			this.value = value;
		}
	}

	/**
	 *
	 * @param label Display label of the infos.
	 * @param description In depth description of the infos.
	 * @param columns Typed description of the infos.
	 * @param years Structured results, also contains QuarterEntries.
	 */
	public record ChronoInfos(String label, String description, List<ColumnDescriptor> columns, List<EntityPreviewStatus.YearEntry> years) {
	}

	public record QuarterEntry(int quarter, Map<String, Object> values) {
	}

	public record YearEntry(int year, Map<String, Object> values, @Nullable List<QuarterEntry> quarters) {
	}
}
