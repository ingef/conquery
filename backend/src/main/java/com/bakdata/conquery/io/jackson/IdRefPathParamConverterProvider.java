package com.bakdata.conquery.io.jackson;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.ws.rs.ext.ParamConverter;
import javax.ws.rs.ext.ParamConverterProvider;

import com.bakdata.conquery.models.identifiable.CentralRegistry;
import com.bakdata.conquery.models.identifiable.Identifiable;
import com.bakdata.conquery.models.identifiable.ids.AId;
import com.bakdata.conquery.models.identifiable.ids.IdUtil;
import com.bakdata.conquery.models.identifiable.ids.NamespacedId;
import com.bakdata.conquery.models.worker.DatasetRegistry;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class IdRefPathParamConverterProvider implements ParamConverterProvider {

	private final DatasetRegistry datasetRegistry;
	private final CentralRegistry metaRegistry;

	@Override
	public <T> ParamConverter<T> getConverter(Class<T> rawType, Type genericType, Annotation[] annotations) {
		if (!(Identifiable.class.isAssignableFrom(rawType))) {
			return null;
		}

		final Class<AId<T>> idClass = IdUtil.findIdClass(rawType);

		if(idClass == null){
			return null;
		}

		final IdUtil.Parser<AId<T>> parser = IdUtil.createParser(idClass);


		if (NamespacedId.class.isAssignableFrom(idClass)) {
			return new NamespacedIdRefParamConverter(parser, datasetRegistry);
		}

		return new MetaIdRefParamConverter(parser, metaRegistry);
	}
}
