package com.bakdata.conquery;

import java.util.Set;

import com.bakdata.conquery.apiv1.forms.FeatureGroup;
import com.bakdata.conquery.internationalization.ResultHeadersC10n;
import com.bakdata.conquery.models.forms.util.Resolution;
import com.bakdata.conquery.models.query.C10nCache;
import com.bakdata.conquery.models.query.PrintSettings;
import com.bakdata.conquery.models.query.resultinfo.FixedLabelResultInfo;
import com.bakdata.conquery.models.query.resultinfo.ResultInfo;
import com.bakdata.conquery.models.query.resultinfo.printers.ResultPrinters;
import com.bakdata.conquery.models.types.ResultType;
import com.bakdata.conquery.models.types.SemanticType;
import lombok.experimental.UtilityClass;

@UtilityClass
public class ResultHeaders {
	public static ResultInfo datesInfo(PrintSettings settings) {
		final String label = C10nCache.getLocalized(ResultHeadersC10n.class, settings.getLocale()).dates();

		final ResultType.ListT<Object> type = new ResultType.ListT<>(ResultType.Primitive.DATE_RANGE);

		return new FixedLabelResultInfo(label, label, type, Set.of(new SemanticType.EventDateT()), settings, ResultPrinters.defaultPrinter(type, settings));
	}

	public static ResultInfo historyDatesInfo(PrintSettings settings) {
		final String label = C10nCache.getLocalized(ResultHeadersC10n.class, settings.getLocale()).dates();

		final ResultType.ListT<Object> type = new ResultType.ListT<>(ResultType.Primitive.DATE_RANGE);

		return new FixedLabelResultInfo(label, label, type, Set.of(new SemanticType.EventDateT(), new SemanticType.GroupT()), settings, ResultPrinters.defaultPrinter(type, settings));
	}

	public static ResultInfo sourceInfo(PrintSettings settings) {
		final String label = C10nCache.getLocalized(ResultHeadersC10n.class, settings.getLocale()).source();

		return new FixedLabelResultInfo(label, label, ResultType.Primitive.STRING, Set.of(new SemanticType.SourcesT(), new SemanticType.CategoricalT(), new SemanticType.GroupT()), settings, ResultPrinters.defaultPrinter(ResultType.Primitive.STRING, settings));
	}

	public static ResultInfo formContextInfo(PrintSettings settings) {
		final String label = C10nCache.getLocalized(ResultHeadersC10n.class, settings.getLocale()).index();

		return new FixedLabelResultInfo(label, label, ResultType.Primitive.INTEGER, Set.of(), settings, ResultPrinters.defaultPrinter(ResultType.Primitive.INTEGER, settings));
	}

	public static ResultInfo formDateRangeInfo(PrintSettings settings) {
		final String label = C10nCache.getLocalized(ResultHeadersC10n.class, settings.getLocale())
									  .dateRange();

		return new FixedLabelResultInfo(label, label, ResultType.Primitive.DATE_RANGE, Set.of(), settings, ResultPrinters.defaultPrinter(ResultType.Primitive.DATE_RANGE, settings));
	}

	public static ResultInfo formResolutionInfo(PrintSettings settings) {
		final String label = C10nCache.getLocalized(ResultHeadersC10n.class, settings.getLocale()).resolution();

		return new FixedLabelResultInfo(label, label, ResultType.Primitive.STRING, Set.of(), settings, new ResultPrinters.LocalizedEnumPrinter<>(settings, Resolution.class));
	}

	public static ResultInfo formEventDateInfo(PrintSettings settings) {
		final String label = C10nCache.getLocalized(ResultHeadersC10n.class, settings.getLocale())
									  .eventDate();

		return new FixedLabelResultInfo(label, label, ResultType.Primitive.DATE, Set.of(), settings, ResultPrinters.defaultPrinter(ResultType.Primitive.DATE, settings));
	}

	public static ResultInfo formObservationScopeInfo(PrintSettings settings) {
		final String label = C10nCache.getLocalized(ResultHeadersC10n.class, settings.getLocale()).observationScope();

		return new FixedLabelResultInfo(label, label, ResultType.Primitive.STRING, Set.of(), settings, new ResultPrinters.LocalizedEnumPrinter<>(settings, FeatureGroup.class));
	}
}
