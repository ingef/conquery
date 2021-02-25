package com.bakdata.conquery.models.query;

import java.text.NumberFormat;
import java.util.Locale;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;

import c10n.C10N;
import com.bakdata.conquery.internationalization.CQElementC10n;
import com.bakdata.conquery.models.concepts.Connector;
import com.bakdata.conquery.models.query.resultinfo.SelectNameExtractor;
import com.bakdata.conquery.models.query.resultinfo.SelectResultInfo;
import com.bakdata.conquery.models.worker.DatasetRegistry;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;

@Getter @ToString(onlyExplicitlyIncluded = true)
public class PrintSettings implements SelectNameExtractor {

	private static final Function<Locale,NumberFormat> NUMBER_FORMAT = (locale) -> NumberFormat.getNumberInstance(locale);
	private static final Function<Locale,NumberFormat> DECIMAL_FORMAT = (locale) -> {
		NumberFormat fmt = NumberFormat.getNumberInstance(locale);
		fmt.setMaximumFractionDigits(Integer.MAX_VALUE);
		return fmt;
	};

	@ToString.Include
	private final boolean prettyPrint;
	@ToString.Include
	private final Locale locale;
	private final NumberFormat decimalFormat;
	private final NumberFormat integerFormat;
	
	/**
	 * Use the registry to resolve ids to objects/labels where this was not done yet, such as {@link CQConcept::getIds()}.
	 */
	private final DatasetRegistry datasetRegistry;
	
	@NonNull
	private final BiFunction<SelectResultInfo, DatasetRegistry, String> columnNamer;

	private final String listElementDelimiter = ", ";
	private final String listElementEscaper = "\\";
	private final String listPrefix = "{";
	private final String listPostfix = "}";

	public PrintSettings(boolean prettyPrint, Locale locale, DatasetRegistry datasetRegistry, BiFunction<SelectResultInfo, DatasetRegistry, String> columnNamer) {		
		this.prettyPrint = prettyPrint;
		this.locale = locale;
		this.datasetRegistry = datasetRegistry;

		this.columnNamer = Objects.requireNonNullElse(columnNamer, this::defaultColumnName);

		this.integerFormat = NUMBER_FORMAT.apply(locale);
		this.decimalFormat = DECIMAL_FORMAT.apply(locale);
	}
	
	public PrintSettings(boolean prettyPrint, Locale locale, DatasetRegistry datasetRegistry) {
		this(prettyPrint, locale, datasetRegistry, null);
	}
	

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


	private String defaultColumnName(SelectResultInfo columnInfo, DatasetRegistry datasetRegistry) {
		StringBuilder sb = new StringBuilder();
		String cqLabel = columnInfo.getCqConcept().getLabel(this);
		String conceptLabel = columnInfo.getSelect().getHolder().findConcept().getLabel();
		
		if (cqLabel != null) {
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

	public CQElementC10n getC10N(Class<CQElementC10n> c10nInterface) {
		return C10N.get(c10nInterface, getLocale());
	}
}
