package com.bakdata.conquery.io.jackson;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import jakarta.inject.Inject;
import jakarta.ws.rs.ext.ParamConverter;
import jakarta.ws.rs.ext.ParamConverterProvider;

import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.models.identifiable.Identifiable;
import com.bakdata.conquery.models.identifiable.ids.Id;
import com.bakdata.conquery.models.identifiable.ids.IdUtil;
import com.bakdata.conquery.models.identifiable.ids.NamespacedId;
import com.bakdata.conquery.models.worker.DatasetRegistry;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
public class IdRefPathParamConverterProvider implements ParamConverterProvider {

	@Inject
	private DatasetRegistry datasetRegistry;
	@Inject
	private MetaStorage metaStorage;

	@Override
	public <T> ParamConverter<T> getConverter(Class<T> rawType, Type genericType, Annotation[] annotations) {
		if (!(Identifiable.class.isAssignableFrom(rawType))) {
			return null;
		}

		final Class<Id<T>> idClass = IdUtil.findIdClass(rawType);

		if (idClass == null) {
			return null;
		}

		final IdUtil.Parser<Id<T>> parser = IdUtil.createParser(idClass);


		if (NamespacedId.class.isAssignableFrom(idClass)) {
			return new NamespacedIdRefParamConverter(parser, datasetRegistry);
		}

		return new MetaIdRefParamConverter(parser, metaStorage);
	}
}
