package com.bakdata.conquery.models.query.resultinfo;

import java.util.Set;

import com.bakdata.conquery.apiv1.query.concept.specific.CQConcept;
import com.bakdata.conquery.models.datasets.concepts.select.Select;
import com.bakdata.conquery.models.query.PrintSettings;
import com.bakdata.conquery.models.query.resultinfo.printers.Printer;
import com.bakdata.conquery.models.query.resultinfo.printers.PrinterFactory;
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

	public SelectResultInfo(@NonNull Select select, @NonNull CQConcept cqConcept, Set<SemanticType> semantics) {
		super(Sets.union(semantics, Set.of(new SemanticType.SelectResultT(select.getId()))));
		this.select = select;
		this.cqConcept = cqConcept;
	}


	@Override
	public String getDescription() {
		return select.getDescription();
	}

	@Override
	public Printer createPrinter(PrinterFactory printerFactory, PrintSettings printSettings) {
		return select.createPrinter(printerFactory, printSettings);
	}

	@Override
	public ResultType getType() {
		return select.getResultType();
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

		return label + " " + select.getColumnName();
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
			// If these labels differ, the user might have changed the label of the concept in the frontend, or a TreeChild was posted
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
