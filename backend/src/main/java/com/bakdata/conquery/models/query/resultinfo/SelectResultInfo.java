package com.bakdata.conquery.models.query.resultinfo;

import javax.annotation.Nullable;

import com.bakdata.conquery.models.datasets.concepts.select.Select;
import com.bakdata.conquery.models.externalservice.ResultType;
import com.bakdata.conquery.models.query.ColumnDescriptor;
import com.bakdata.conquery.models.query.PrintSettings;
import com.bakdata.conquery.apiv1.query.concept.specific.CQConcept;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class SelectResultInfo extends ResultInfo {
	@NonNull
	private final Select select;
	@NonNull
	private final CQConcept cqConcept;

	@NonNull
	private SelectNameMode nameMode = SelectNameMode.SELECT;

	public enum SelectNameMode {
		SELECT(){
			@Override
			public String defaultColumnName(PrintSettings printSettings, SelectResultInfo info) {
				return info.getSelect().getLabel();
			}
		},
		CONCEPT_SELECT {
			@Override
			public String defaultColumnName(PrintSettings printSettings, SelectResultInfo info) {
				StringBuilder sb = new StringBuilder();
				String cqLabel = info.getCqConcept().getConcept().getLabel();

				if (cqLabel != null) {
					// If these labels differ, the user might changed the label of the concept in the frontend, or a TreeChild was posted
					sb.append(cqLabel);
					sb.append(" ");
				}

				sb.append(info.getSelect().getLabel());
				return sb.toString();
			}
		},
		CONCEPT_CHILDREN_SELECT {
			@Override
			public String defaultColumnName(PrintSettings printSettings, SelectResultInfo info) {
				StringBuilder sb = new StringBuilder();

				// Gets the concept label and child labels
				String cqLabel = info.getCqConcept().defaultLabel(printSettings.getLocale());

				if (cqLabel != null) {
					// If these labels differ, the user might changed the label of the concept in the frontend, or a TreeChild was posted
					sb.append(cqLabel);
					sb.append(" ");
				}

				sb.append(info.getSelect().getLabel());
				return sb.toString();
			}
		},
		CONCEPT_CHILDREN_CONNECTOR_SELECT {
			@Override
			public String defaultColumnName(PrintSettings printSettings, SelectResultInfo info) {
				StringBuilder sb = new StringBuilder();
				String cqLabel = info.getCqConcept().defaultLabel(printSettings.getLocale());

				if (cqLabel != null) {
					// If these labels differ, the user might changed the label of the concept in the frontend, or a TreeChild was posted
					sb.append(cqLabel);
					sb.append(" ");
				}

				info.select.appendColumnName(sb);
				return sb.toString();
			}
		};

		/**
		 * Get the next verbosity level from this naming mode.
		 * @return The next verbosity level.
		 */
		@Nullable
		public SelectNameMode nextEscalation() {
			final int ordinal = this.ordinal();
			if (SelectNameMode.values().length == (ordinal + 1)) {
				return null;
			}
			return SelectNameMode.values()[ordinal + 1];
		}

		public abstract String defaultColumnName(PrintSettings printSettings, SelectResultInfo info);
	}

	@Override
	public ResultType getType() {
		return select.getResultType();
	}

	@Override
	public ColumnDescriptor asColumnDescriptor(PrintSettings settings, UniqueNamer uniqueNamer) {
		return ColumnDescriptor.builder()
				.label(uniqueNamer.getUniqueName(this))
				.defaultLabel(defaultColumnName(settings))
				.userConceptLabel(userColumnName(settings))
				.type(getType().typeInfo())
				.selectId(select.getId())
				.build();
	}

	@Override
	public String userColumnName(PrintSettings printSettings) {

		if (printSettings.getColumnNamer() != null) {
			// override user labels if column namer is set, TODO clean this up when userConceptLabel is removed
			return printSettings.getColumnNamer().apply(this);
		}

		String label = getCqConcept().getLabel();
		if (label == null) {
			return null;
		}

		StringBuilder sb = new StringBuilder();

		sb.append(label);
		sb.append(" - ");

		select.appendColumnName(sb);
		return sb.toString();
	}

	@Override
	public String defaultColumnName(PrintSettings printSettings) {
		return nameMode.defaultColumnName(printSettings, this);
	}

	public boolean escalateNameMode(){
		final SelectNameMode next = nameMode.nextEscalation();
		if (next == null) {
			return false;
		}
		nameMode = next;
		return true;
	}

	@Override
	public String toString(){
		return "SelectResultInfo[" + select.getName() + ", " + select.getResultType() + "]";
	}
}
