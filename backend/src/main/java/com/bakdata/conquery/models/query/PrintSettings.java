package com.bakdata.conquery.models.query;

import java.util.Locale;
import java.util.function.Function;

import com.bakdata.conquery.models.concepts.Connector;
import com.bakdata.conquery.models.query.resultinfo.SelectNameExtractor;
import com.bakdata.conquery.models.query.resultinfo.SelectResultInfo;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

@Getter @RequiredArgsConstructor @AllArgsConstructor @ToString
public class PrintSettings implements SelectNameExtractor {

	private final boolean prettyPrint;
	private final Locale locale;
	
	@NonNull
	private Function<SelectResultInfo, String> columnNamer = PrintSettings::defaultColumnName;
	

	/**
	 * Generates the name for a query result column.
	 */
	@Override
	public String columnName(SelectResultInfo columnInfo) {
		if (columnNamer == null) {
			// Should never be reached
			throw new IllegalStateException("No column namer was supplied");
		}
		return columnNamer.apply(columnInfo);
	}


	private static String defaultColumnName(SelectResultInfo columnInfo) {
		StringBuilder sb = new StringBuilder();
		String cqLabel = columnInfo.getCqConcept().getLabel();
		String conceptLabel = columnInfo.getSelect().getHolder().findConcept().getLabel();
		
		sb.append(conceptLabel);
		sb.append(" - ");
		if (cqLabel != null && !cqLabel.equalsIgnoreCase(conceptLabel)) {
			// If these labels differ, the user might changed the label of the concept in the frontend, or a TreeChild was posted
			sb.append(cqLabel);
			sb.append(" - ");
		}
		if (columnInfo.getSelect().getHolder() instanceof Connector && columnInfo.getSelect().getHolder().findConcept().getConnectors().size() > 1) {
			// The select originates from a connector and the corresponding concept has more than one connector -> Print also the connector
			sb.append(((Connector)columnInfo.getSelect().getHolder()).getLabel());
			sb.append(' ');
		}
		sb.append(columnInfo.getSelect().getLabel());
		return sb.toString();
	}
}
