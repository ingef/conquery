package com.bakdata.conquery.io.result;

import java.util.Arrays;
import java.util.Collection;

import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.dictionary.DirectDictionary;
import com.bakdata.conquery.models.identifiable.mapping.CsvEntityId;
import com.bakdata.conquery.models.identifiable.mapping.ExternalEntityId;
import com.bakdata.conquery.models.identifiable.mapping.IdMappingConfig;
import com.bakdata.conquery.models.identifiable.mapping.IdMappingState;
import com.bakdata.conquery.models.query.results.ContainedEntityResult;
import com.bakdata.conquery.models.worker.Namespace;

public class ResultUtil {

	public static final IdMappingConfig ID_MAPPING = ConqueryConfig.getInstance().getIdMapping();
	public static final Collection<String> HEADER = Arrays.asList(ID_MAPPING.getPrintIdFields());
	
	public static ExternalEntityId createId(Namespace namespace, ContainedEntityResult cer, IdMappingState mappingState) {
		DirectDictionary dict = namespace.getStorage().getPrimaryDictionary();
		return ID_MAPPING
			.toExternal(
				new CsvEntityId(dict.getElement(cer.getEntityId())),
				namespace,
				mappingState);
	}

}
