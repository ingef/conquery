package com.bakdata.conquery;

import java.util.Set;

import com.bakdata.conquery.apiv1.forms.FeatureGroup;
import com.bakdata.conquery.internationalization.ResultHeadersC10n;
import com.bakdata.conquery.models.forms.util.Resolution;
import com.bakdata.conquery.models.query.resultinfo.LocalizedDefaultResultInfo;
import com.bakdata.conquery.models.query.resultinfo.ResultInfo;
import com.bakdata.conquery.models.types.ResultType;
import com.bakdata.conquery.models.types.SemanticType;
import lombok.experimental.UtilityClass;

@UtilityClass
public class ConqueryConstants {

	public static final String ALL_IDS_TABLE = "ALL_IDS";
	public static final String EXTENSION_PREPROCESSED = ".cqpp";
	public static final String EXTENSION_DESCRIPTION = ".import.json";

	public static final ResultInfo DATES_INFO =
			new LocalizedDefaultResultInfo((l) -> l.getLocalized(ResultHeadersC10n.class).dates(),null, new ResultType.ListT<>(ResultType.Primitive.DATE_RANGE),null, Set.of(new SemanticType.EventDateT()));

	public static final ResultInfo DATES_INFO_HISTORY =
			new LocalizedDefaultResultInfo((l) -> l.getLocalized(ResultHeadersC10n.class).dates(), null,new ResultType.ListT<>(ResultType.Primitive.DATE_RANGE),null, Set.of(new SemanticType.EventDateT(), new SemanticType.GroupT()));


	public static final ResultInfo
			SOURCE_INFO =
			new LocalizedDefaultResultInfo((l) -> l.getLocalized(ResultHeadersC10n.class).source(),null, ResultType.Primitive.STRING,null, Set.of(new SemanticType.SourcesT(), new SemanticType.CategoricalT(), new SemanticType.GroupT()));

	// Form related constants
	public static final String SINGLE_RESULT_TABLE_NAME = "results";
	public static final ResultInfo CONTEXT_INDEX_INFO =
			new LocalizedDefaultResultInfo((l) -> l.getLocalized(ResultHeadersC10n.class).index(),null, ResultType.Primitive.INTEGER,null, Set.of());


	public static final ResultInfo DATE_RANGE_INFO =
			new LocalizedDefaultResultInfo((l) -> l.getLocalized(ResultHeadersC10n.class).dateRange(),null, ResultType.Primitive.DATE_RANGE,null, Set.of());

	public static final ResultInfo RESOLUTION_INFO =
			new LocalizedDefaultResultInfo((l) -> l.getLocalized(ResultHeadersC10n.class).resolution(),null, ResultType.Primitive.STRING,new Resolution.LocalizingPrinter(), Set.of());


	public static final ResultInfo EVENT_DATE_INFO =
			new LocalizedDefaultResultInfo((l) -> l.getLocalized(ResultHeadersC10n.class).eventDate(),null, ResultType.Primitive.DATE, null, Set.of());

	public static final ResultInfo OBSERVATION_SCOPE_INFO =
			new LocalizedDefaultResultInfo((l) -> l.getLocalized(ResultHeadersC10n.class).observationScope(),null, ResultType.Primitive.STRING, new FeatureGroup.LocalizingPrinter(), Set.of());

	/**
	 * Drawn from random.org
	 */
	public static final long RANDOM_SEED = 694011229L;
}
