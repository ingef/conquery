package com.bakdata.conquery.models.query;

import java.text.NumberFormat;
import java.util.Locale;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;

import c10n.C10N;
import com.bakdata.conquery.internationalization.CQElementC10n;
import com.bakdata.conquery.models.concepts.Connector;
import com.bakdata.conquery.models.query.concept.specific.CQConcept;
import com.bakdata.conquery.models.query.resultinfo.SelectNameExtractor;
import com.bakdata.conquery.models.query.resultinfo.SelectResultInfo;
import com.bakdata.conquery.models.worker.DatasetRegistry;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;

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
	 * Use the registry to resolve ids to objects/labels where this was not done yet, such as {@link CQConcept#getElements()}.
	 */
	private final DatasetRegistry datasetRegistry;
	
	@NonNull
	private final Function<SelectResultInfo, String> columnNamer;

	private final String listElementDelimiter = ", ";
	private final String listElementEscaper = "\\";
	private final String listPrefix = "{";
	private final String listPostfix = "}";

	public PrintSettings(boolean prettyPrint, Locale locale, DatasetRegistry datasetRegistry, Function<SelectResultInfo, String> columnNamer) {
		this.prettyPrint = prettyPrint;
		this.locale = locale;
		this.datasetRegistry = datasetRegistry;

		this.columnNamer = Objects.requireNonNullElse(columnNamer, this::userColumnName);

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
		return columnNamer.apply(columnInfo);
	}


	private String userColumnName(SelectResultInfo columnInfo) {
		StringBuilder sb = new StringBuilder();
		String cqLabel = columnInfo.getCqConcept().getLabel(getLocale());

		return getColumnName(columnInfo, sb, cqLabel);
	}

	public String defaultColumnName(SelectResultInfo columnInfo) {
		StringBuilder sb = new StringBuilder();
		String cqLabel = columnInfo.getCqConcept().getDefaultLabel();

		return getColumnName(columnInfo, sb, cqLabel);
	}

	@NotNull
	private String getColumnName(SelectResultInfo columnInfo, StringBuilder sb, String cqLabel) {
		if (cqLabel != null) {
			// If these labels differ, the user might changed the label of the concept in the frontend, or a TreeChild was posted
			sb.append(cqLabel);
			sb.append(" - ");
		}
		if (columnInfo.getSelect().getHolder() instanceof Connector && columnInfo.getSelect().getHolder().findConcept().getConnectors().size() > 1) {
			// The select originates from a connector and the corresponding concept has more than one connector -> Print also the connector
			sb.append(((Connector) columnInfo.getSelect().getHolder()).getLabel());
			sb.append(' ');
		}
		sb.append(columnInfo.getSelect().getLabel());
		return sb.toString();
	}

	public CQElementC10n getC10N(Class<CQElementC10n> c10nInterface) {
		return C10N.get(c10nInterface, getLocale());
	}
}
