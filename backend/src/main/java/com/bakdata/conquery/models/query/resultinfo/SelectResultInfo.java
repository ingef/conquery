package com.bakdata.conquery.models.query.resultinfo;

import java.util.Set;

import com.bakdata.conquery.apiv1.query.concept.specific.CQConcept;
import com.bakdata.conquery.models.datasets.concepts.select.Select;
import com.bakdata.conquery.models.query.PrintSettings;
import com.bakdata.conquery.models.query.resultinfo.printers.ResultPrinters;
import com.bakdata.conquery.models.types.ResultType;
import com.bakdata.conquery.models.types.SemanticType;
import com.google.common.collect.Sets;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;

@Getter
@EqualsAndHashCode(callSuper = true)
public class SelectResultInfo extends ResultInfo {
	@NonNull
	private final Select select;
	@NonNull
	private final CQConcept cqConcept;

	public SelectResultInfo(Select select, CQConcept cqConcept, Set<SemanticType> semantics, PrintSettings settings) {
		super(Sets.union(semantics, Set.of(new SemanticType.SelectResultT(select))), settings);
		this.select = select;
		this.cqConcept = cqConcept;
	}


	@Override
	public String getDescription() {
		return select.getDescription();
	}

	@Override
	public ResultPrinters.Printer getPrinter() {
		return select.createPrinter(getSettings());
	}

	@Override
	public ResultType getType() {
		return select.getResultType();
	}

	@Override
	public String userColumnName() {

		if (getSettings().getColumnNamer() != null) {
			// override user labels if column namer is set, TODO clean this up when userConceptLabel is removed
			return getSettings().getColumnNamer().apply(this);
		}

		String label = getCqConcept().getLabel();
		if (label == null) {
			return null;
		}

		return label + " " + select.getColumnName();
	}

	@Override
	public String defaultColumnName() {

		StringBuilder sb = new StringBuilder();
		String cqLabel = getCqConcept().defaultLabel(getSettings().getLocale());
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
