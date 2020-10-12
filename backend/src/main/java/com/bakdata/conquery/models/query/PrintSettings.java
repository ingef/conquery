package com.bakdata.conquery.models.query;

import java.util.Locale;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import com.bakdata.conquery.models.concepts.ConceptElement;
import com.bakdata.conquery.models.concepts.Connector;
import com.bakdata.conquery.models.query.resultinfo.SelectNameExtractor;
import com.bakdata.conquery.models.query.resultinfo.SelectResultInfo;
import com.bakdata.conquery.models.worker.DatasetRegistry;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

@Getter @RequiredArgsConstructor @AllArgsConstructor @ToString
public class PrintSettings implements SelectNameExtractor {

	private final boolean prettyPrint;
	private final Locale locale;
	/**
	 * Use the registry to resolve ids to objects/labels where this was not done yet, such as {@link CQConcept::getIds()}.
	 */
	private final DatasetRegistry datasetRegistry;
	
	@NonNull
	private BiFunction<SelectResultInfo, DatasetRegistry, String> columnNamer = PrintSettings::defaultColumnName;
	

	/**
	 * Generates the name for a query result column.
	 */
	@Override
	public String columnName(SelectResultInfo columnInfo) {
		if (columnNamer == null) {
			// Should never be reached
			throw new IllegalStateException("No column namer was supplied");
		}
		return columnNamer.apply(columnInfo, datasetRegistry);
	}


	private static String defaultColumnName(SelectResultInfo columnInfo, DatasetRegistry datasetRegistry) {
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
		else if(columnInfo.getCqConcept().getIds().size() > 0) {
			// When no Label was set within the query, get the labels of all ids that are in the CQConcept
			String concatElementLabels = columnInfo.getCqConcept().getIds().stream()
			.map(datasetRegistry::resolve)
			.map(ConceptElement.class::cast)
			.map(ConceptElement::getLabel)
			.collect(Collectors.joining("+"));
			
			if(!concatElementLabels.equalsIgnoreCase(conceptLabel)) {
				// Only add all child labels if they are different from the actual label of the concept
				sb.append(concatElementLabels);
				sb.append(" - ");
			}
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
