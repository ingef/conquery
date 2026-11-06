package com.bakdata.conquery.apiv1.query;

import java.util.Set;

import com.bakdata.conquery.apiv1.forms.FeatureGroup;
import com.bakdata.conquery.internationalization.ResultHeadersC10n;
import com.bakdata.conquery.models.forms.util.Resolution;
import com.bakdata.conquery.models.query.C10nCache;
import com.bakdata.conquery.models.query.PrintSettings;
import com.bakdata.conquery.models.query.resultinfo.FixedLabelResultInfo;
import com.bakdata.conquery.models.query.resultinfo.ResultInfo;
import com.bakdata.conquery.models.query.resultinfo.printers.Printer;
import com.bakdata.conquery.models.query.resultinfo.printers.PrinterFactory;
import com.bakdata.conquery.models.query.resultinfo.printers.common.LocalizedEnumPrinter;
import com.bakdata.conquery.models.types.ResultType;
import com.bakdata.conquery.models.types.SemanticType;
import com.bakdata.conquery.sql.execution.ResultSetProcessor;
import lombok.experimental.UtilityClass;

@UtilityClass
public class ResultHeaders {
	public static ResultInfo datesInfo() {

		final ResultType.ListT<Object> type = new ResultType.ListT<>(ResultType.Primitive.DATE_RANGE);

		return new FixedLabelResultInfo(type, Set.of(new SemanticType.EventDateT())){
			@Override
			public String userColumnName(PrintSettings printSettings) {
				return C10nCache.getLocalized(ResultHeadersC10n.class, printSettings.getLocale()).dates();
			}

			@Override
			public ResultSetProcessor.Reader<?> createReader(ResultSetProcessor resultSetProcessor) {
				return resultSetProcessor::getDateRangeList;
			}
		};
	}

	public static ResultInfo historyDatesInfo() {

		final ResultType.ListT<Object> type = new ResultType.ListT<>(ResultType.Primitive.DATE_RANGE);

		return new FixedLabelResultInfo(type, Set.of(new SemanticType.EventDateT(), new SemanticType.GroupT())) {
			@Override
			public String userColumnName(PrintSettings printSettings) {
				return C10nCache.getLocalized(ResultHeadersC10n.class, printSettings.getLocale()).dates();
			}

			@Override
			public ResultSetProcessor.Reader<?> createReader(ResultSetProcessor resultSetProcessor) {
				return resultSetProcessor::getDateRangeList;
			}
		};
	}

	public static ResultInfo sourceInfo() {
		return new FixedLabelResultInfo(ResultType.Primitive.STRING, Set.of(new SemanticType.SourcesT(), new SemanticType.CategoricalT(), new SemanticType.GroupT())) {
			@Override
			public String userColumnName(PrintSettings printSettings) {
				return C10nCache.getLocalized(ResultHeadersC10n.class, printSettings.getLocale()).source();
			}

			@Override
			public ResultSetProcessor.Reader<?> createReader(ResultSetProcessor resultSetProcessor) {
				return resultSetProcessor::getString;
			}
		};
	}

	public static ResultInfo formContextInfo() {

		return new FixedLabelResultInfo(ResultType.Primitive.INTEGER, Set.of()) {
			@Override
			public String userColumnName(PrintSettings printSettings) {
				return C10nCache.getLocalized(ResultHeadersC10n.class, printSettings.getLocale()).index();
			}

			@Override
			public ResultSetProcessor.Reader<?> createReader(ResultSetProcessor resultSetProcessor) {
				return resultSetProcessor::getInteger;
			}
		};
	}

	public static ResultInfo formDateRangeInfo() {

		return new FixedLabelResultInfo(ResultType.Primitive.DATE_RANGE, Set.of()) {
			@Override
			public String userColumnName(PrintSettings printSettings) {
				return C10nCache.getLocalized(ResultHeadersC10n.class, printSettings.getLocale()).dateRange();
			}

			@Override
			public ResultSetProcessor.Reader<?> createReader(ResultSetProcessor resultSetProcessor) {
				return resultSetProcessor::getDateRange;
			}
		};
	}

	public static ResultInfo formResolutionInfo() {

		return new FixedLabelResultInfo(ResultType.Primitive.STRING, Set.of()) {
			@Override
			public Printer createPrinter(PrinterFactory printerFactory, PrintSettings printSettings) {
				return new LocalizedEnumPrinter<>(printSettings, Resolution.class);
			}

			@Override
			public String userColumnName(PrintSettings printSettings) {
				return C10nCache.getLocalized(ResultHeadersC10n.class, printSettings.getLocale()).resolution();
			}

			@Override
			public ResultSetProcessor.Reader<?> createReader(ResultSetProcessor resultSetProcessor) {
				return resultSetProcessor::getString;
			}
		};
	}

	public static ResultInfo formEventDateInfo() {

		return new FixedLabelResultInfo(ResultType.Primitive.DATE, Set.of()) {
			@Override
			public String userColumnName(PrintSettings printSettings) {
				return C10nCache.getLocalized(ResultHeadersC10n.class, printSettings.getLocale()).eventDate();
			}

			@Override
			public ResultSetProcessor.Reader<?> createReader(ResultSetProcessor resultSetProcessor) {
				return resultSetProcessor::getDate;
			}
		};
	}

	public static ResultInfo formObservationScopeInfo() {

		return new FixedLabelResultInfo(ResultType.Primitive.STRING, Set.of()) {
			@Override
			public Printer createPrinter(PrinterFactory printerFactory, PrintSettings printSettings) {
				return new LocalizedEnumPrinter<>(printSettings, FeatureGroup.class);
			}

			@Override
			public String userColumnName(PrintSettings printSettings) {
				return C10nCache.getLocalized(ResultHeadersC10n.class, printSettings.getLocale()).observationScope();
			}

			@Override
			public ResultSetProcessor.Reader<?> createReader(ResultSetProcessor resultSetProcessor) {
				return resultSetProcessor::getString;
			}
		};
	}
}
