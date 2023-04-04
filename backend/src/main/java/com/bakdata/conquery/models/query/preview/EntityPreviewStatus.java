package com.bakdata.conquery.models.query.preview;

import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import com.bakdata.conquery.apiv1.execution.FullExecutionStatus;
import com.bakdata.conquery.models.query.ColumnDescriptor;
import com.bakdata.conquery.models.types.SemanticType;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Data
public class EntityPreviewStatus extends FullExecutionStatus {
	private List<Info> infos;
	private List<TimeStratifiedInfos> timeStratifiedInfos;


	/**
	 * Bundles ColumnDescriptor immediately with a result value for EntityPreview.
	 */
	@EqualsAndHashCode(callSuper = true)
	@ToString(callSuper = true)
	@Getter
	public static class Info extends ColumnDescriptor {

		/**
		 * The result of the InfoCardSelect.
		 */
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
	public record TimeStratifiedInfos(String label, String description, List<ColumnDescriptor> columns, Map<String, Object> complete, List<EntityPreviewStatus.YearEntry> years) {
	}

	public record QuarterEntry(int quarter, Map<String, Object> values) {
	}

	public record YearEntry(int year, Map<String, Object> values, @Nullable List<QuarterEntry> quarters) {
	}
}
