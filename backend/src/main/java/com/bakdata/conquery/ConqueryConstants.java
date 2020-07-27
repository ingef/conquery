package com.bakdata.conquery;

import java.util.Arrays;

import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.externalservice.ResultType;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.identifiable.ids.specific.DictionaryId;
import com.bakdata.conquery.models.query.resultinfo.SimpleResultInfo;
import lombok.experimental.UtilityClass;

@UtilityClass
public class ConqueryConstants {

	public static final String ALL_IDS_TABLE = "ALL_IDS";
	public static final String EXTENSION_PREPROCESSED = ".cqpp";
	public static final String EXTENSION_DESCRIPTION = ".import.json";
	public static final String INPUT_FILE_EXTENSION = ".csv.gz";
	public static final String ID_TYPE = "ID";

  public static final SimpleResultInfo DATES_INFO = new SimpleResultInfo("dates", ResultType.STRING);
	
	// Form related constants
	public static final String CONTEXT_INDEX = "index";
	public static final String DATE_RANGE = "date_range";
	public static final String EVENT_DATE = "event_date";
	public static final String SINGLE_RESULT_TABLE_NAME = "results";
	public static final String RESOLUTION = "resolution";
	public static final SimpleResultInfo CONTEXT_INDEX_INFO = new SimpleResultInfo(CONTEXT_INDEX, ResultType.INTEGER);
	public static final SimpleResultInfo DATE_RANGE_INFO = new SimpleResultInfo(DATE_RANGE, ResultType.STRING);
	public static final SimpleResultInfo RESOLUTION_INFO = new SimpleResultInfo(RESOLUTION, ResultType.RESOLUTION);
	public static final SimpleResultInfo EVENT_DATE_INFO = new SimpleResultInfo(EVENT_DATE, ResultType.DATE);
	public static final SimpleResultInfo FEATURE_DATE_RANGE_INFO = new SimpleResultInfo("feature_"+DATE_RANGE, ResultType.STRING);
	public static final SimpleResultInfo OUTCOME_DATE_RANGE_INFO = new SimpleResultInfo("outcome_"+DATE_RANGE, ResultType.STRING);

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
