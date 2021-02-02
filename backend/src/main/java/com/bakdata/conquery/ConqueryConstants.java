package com.bakdata.conquery;

import java.util.Arrays;

import c10n.C10N;
import com.bakdata.conquery.internationalization.ResultHeadersC10n;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.externalservice.ResultType;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.identifiable.ids.specific.DictionaryId;
import com.bakdata.conquery.models.query.resultinfo.LocalizedSimpleResultInfo;
import com.bakdata.conquery.models.query.resultinfo.ResultInfo;
import com.bakdata.conquery.models.query.resultinfo.SimpleResultInfo;
import lombok.experimental.UtilityClass;

@UtilityClass
public class ConqueryConstants {

	public static final String ALL_IDS_TABLE = "ALL_IDS";
	public static final String EXTENSION_PREPROCESSED = ".cqpp";
	public static final String EXTENSION_DESCRIPTION = ".import.json";
	public static final String INPUT_FILE_EXTENSION = ".csv.gz";
	public static final String ID_TYPE = "ID";

	public static final ResultInfo DATES_INFO = new LocalizedSimpleResultInfo((l) -> C10N.get(ResultHeadersC10n.class, l).dates(), ResultType.STRING);
	
	// Form related constants
	public static final String CONTEXT_INDEX = "index";
	public static final String SINGLE_RESULT_TABLE_NAME = "results";
	public static final ResultInfo CONTEXT_INDEX_INFO = new LocalizedSimpleResultInfo((l) -> C10N.get(ResultHeadersC10n.class, l).index(), ResultType.INTEGER);
	public static final ResultInfo DATE_RANGE_INFO = new LocalizedSimpleResultInfo((l) -> C10N.get(ResultHeadersC10n.class, l).dateRange(), ResultType.STRING);
	public static final ResultInfo RESOLUTION_INFO = new LocalizedSimpleResultInfo((l) -> C10N.get(ResultHeadersC10n.class, l).resolution(), ResultType.RESOLUTION);
	public static final ResultInfo EVENT_DATE_INFO = new LocalizedSimpleResultInfo((l) -> C10N.get(ResultHeadersC10n.class, l).eventDate(), ResultType.DATE);
	public static final ResultInfo FEATURE_DATE_RANGE_INFO = new LocalizedSimpleResultInfo((l) -> C10N.get(ResultHeadersC10n.class, l).featureDateRange(), ResultType.STRING);
	public static final ResultInfo OUTCOME_DATE_RANGE_INFO = new LocalizedSimpleResultInfo((l) -> C10N.get(ResultHeadersC10n.class, l).outcomeDateRange(), ResultType.STRING);

	public static class AuthenticationUtil {
		public static final String REALM_NAME = "CONQUERY";
	}
	
	public static DictionaryId getPrimaryDictionary(Dataset dataset) {
		return getPrimaryDictionary(dataset.getId());
	}
	
	public static DictionaryId getPrimaryDictionary(DatasetId dataset) {
		return DictionaryId.Parser.INSTANCE.parse(Arrays.asList(dataset.toString(), "primary_dictionary"));
	}
}
