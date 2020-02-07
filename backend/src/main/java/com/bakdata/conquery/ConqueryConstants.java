package com.bakdata.conquery;

import java.util.Arrays;
import java.util.function.Function;

import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.externalservice.ResultType;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.identifiable.ids.specific.DictionaryId;
import com.bakdata.conquery.models.query.PrintSettings;
import com.bakdata.conquery.models.query.resultinfo.SimpleResultInfo;
import lombok.experimental.UtilityClass;

@UtilityClass
public class ConqueryConstants {

	public static final String EXTENSION_PREPROCESSED = ".cqpp";
	public static final String EXTENSION_DESCRIPTION = ".import.json";
	public static final String ALL_IDS_TABLE = "ALL_IDS_TABLE";
	public static final String ALL_IDS_TABLE___ID = "id";
	public static final Function<PrintSettings, SimpleResultInfo> DATES_INFO = settings -> new SimpleResultInfo(settings, "dates", ResultType.STRING);

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
