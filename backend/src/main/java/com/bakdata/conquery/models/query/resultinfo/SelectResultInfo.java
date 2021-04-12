package com.bakdata.conquery.models.query.resultinfo;

import com.bakdata.conquery.models.concepts.Connector;
import com.bakdata.conquery.models.concepts.select.Select;
import com.bakdata.conquery.models.externalservice.ResultType;
import com.bakdata.conquery.models.query.ColumnDescriptor;
import com.bakdata.conquery.models.query.PrintSettings;
import com.bakdata.conquery.models.query.concept.specific.CQConcept;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;

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
				.defaultLabel(defaultColumnName(settings.getLocale()))
				.userConceptLabel(userColumnName(settings.getLocale()))
				.type(getType().typeInfo())
				.selectId(select.getId())
				.build();
	}




	@Override
	public String userColumnName(Locale locale) {
		StringBuilder sb = new StringBuilder();
		String label = getCqConcept().getLabel(locale);

		return getColumnName(sb, label);
	}

	@Override
	public String defaultColumnName(Locale locale) {
		StringBuilder sb = new StringBuilder();
		String cqLabel = getCqConcept().getDefaultLabel();

		return getColumnName(sb, cqLabel);
	}

	@NotNull
	private String getColumnName(StringBuilder sb, String cqLabel) {
		if (cqLabel != null) {
			// If these labels differ, the user might changed the label of the concept in the frontend, or a TreeChild was posted
			sb.append(cqLabel);
			sb.append(" - ");
		}
		if (getSelect().getHolder() instanceof Connector && getSelect().getHolder().findConcept().getConnectors().size() > 1) {
			// The select originates from a connector and the corresponding concept has more than one connector -> Print also the connector
			sb.append(((Connector) getSelect().getHolder()).getLabel());
			sb.append(' ');
		}
		sb.append(getSelect().getLabel());
		return sb.toString();
	}

	@Override
	public String toString(){
		return "SelectResultInfo[" + select.getName() + ", " + select.getResultType() + "]";
	}
}
