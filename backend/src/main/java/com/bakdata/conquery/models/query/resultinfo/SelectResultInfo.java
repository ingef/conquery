package com.bakdata.conquery.models.query.resultinfo;

import com.bakdata.conquery.models.concepts.select.Select;
import com.bakdata.conquery.models.externalservice.ResultType;
import com.bakdata.conquery.models.query.ColumnDescriptor;
import com.bakdata.conquery.models.query.PrintSettings;
import com.bakdata.conquery.models.query.concept.specific.CQConcept;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.util.Locale;

@Getter
@EqualsAndHashCode(callSuper = true)
@RequiredArgsConstructor
public class SelectResultInfo extends ResultInfo {
	@NonNull
	private final Select select;
	@NonNull
	private final CQConcept cqConcept;

	@Override
	public ResultType getType() {
		return select.getResultType();
	}

	@Override
	public ColumnDescriptor asColumnDescriptor(PrintSettings settings) {
		return ColumnDescriptor.builder()
				.label(getUniqueName(settings))
				.defaultLabel(defaultColumnName(settings))
				.userConceptLabel(userColumnName(settings.getLocale()))
				.type(getType().typeInfo())
				.selectId(select.getId())
				.build();
	}

	@Override
	public String userColumnName(Locale locale) {
		StringBuilder sb = new StringBuilder();
		String label = getCqConcept().getLabel(locale);

		return select.appendColumnName(sb, label);
	}

	@Override
	public String defaultColumnName(PrintSettings printSettings) {
		if (printSettings.getColumnNamer() != null) {
			return printSettings.getColumnNamer().apply(this);
		}


		StringBuilder sb = new StringBuilder();
		String cqLabel = getCqConcept().getDefaultLabel();

		return select.appendColumnName(sb, cqLabel);
	}

	@Override
	public String toString(){
		return "SelectResultInfo[" + select.getName() + ", " + select.getResultType() + "]";
	}
}
