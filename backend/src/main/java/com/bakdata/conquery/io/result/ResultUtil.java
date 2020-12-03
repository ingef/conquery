package com.bakdata.conquery.io.result;

import com.bakdata.conquery.models.dictionary.DirectDictionary;
import com.bakdata.conquery.models.identifiable.mapping.CsvEntityId;
import com.bakdata.conquery.models.identifiable.mapping.ExternalEntityId;
import com.bakdata.conquery.models.identifiable.mapping.IdMappingConfig;
import com.bakdata.conquery.models.identifiable.mapping.IdMappingState;
import com.bakdata.conquery.models.query.results.ContainedEntityResult;
import com.bakdata.conquery.models.worker.Namespace;

public class ResultUtil {

	
	public static ExternalEntityId createId(Namespace namespace, ContainedEntityResult cer, IdMappingConfig idMappingConfig, IdMappingState mappingState) {
		DirectDictionary dict = namespace.getStorage().getPrimaryDictionary();
		return idMappingConfig
			.toExternal(
				new CsvEntityId(dict.getElement(cer.getEntityId())),
				namespace,
				mappingState);
	}

}
