package com.bakdata.conquery.models.query.resultinfo;

import java.util.Collections;
import java.util.Set;

import com.bakdata.conquery.apiv1.query.concept.specific.CQConcept;
import com.bakdata.conquery.models.datasets.concepts.select.Select;
import com.bakdata.conquery.models.query.ColumnDescriptor;
import com.bakdata.conquery.models.query.PrintSettings;
import com.bakdata.conquery.models.types.ResultType;
import com.bakdata.conquery.models.types.SemanticType;
import com.google.common.collect.ImmutableSet;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Getter
@EqualsAndHashCode(callSuper = true)
@RequiredArgsConstructor
public class SelectResultInfo extends ResultInfo {
	@NonNull
	private final Select select;
	@NonNull
	private final CQConcept cqConcept;

	@Getter(AccessLevel.PACKAGE)
	private final Set<SemanticType> additionalSemantics;

	public SelectResultInfo(Select select, CQConcept cqConcept) {
		this(select, cqConcept, Collections.emptySet());
	}

	@Override
	public Set<SemanticType> getSemantics() {
		return ImmutableSet.<SemanticType>builder()
						   .addAll(additionalSemantics)
						   .add(new SemanticType.SelectResultT(select))
						   .build();
	}

	@Override
	public String getDescription() {
		return select.getDescription();
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
							   .type(getType().typeInfo())
							   .semantics(getSemantics())
							   .description(getSelect().getDescription())
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

		return label
			   + " "
			   + select.getColumnName();
	}

	@Override
	public String defaultColumnName(PrintSettings printSettings) {

		StringBuilder sb = new StringBuilder();
		String cqLabel = getCqConcept().defaultLabel(printSettings.getLocale());
		final String selectLabel = select.getColumnName();

		if (selectLabel.equals(cqLabel)) {
			// If the default name of concept and select is the same, output only one
			return selectLabel;
		}

		if (cqLabel != null) {
			// If these labels differ, the user might changed the label of the concept in the frontend, or a TreeChild was posted
			sb.append(cqLabel);
			sb.append(" ");
		}

		sb.append(selectLabel);

		return sb.toString();
	}

	@Override
	public String toString() {
		return "SelectResultInfo[" + select.getName() + ", " + select.getResultType() + "]";
	}
}
