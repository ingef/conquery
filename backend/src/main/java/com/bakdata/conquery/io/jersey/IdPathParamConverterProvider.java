package com.bakdata.conquery.io.jersey;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import jakarta.ws.rs.ext.ParamConverter;
import jakarta.ws.rs.ext.ParamConverterProvider;

import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.models.identifiable.NamespacedStorageProvider;
import com.bakdata.conquery.models.identifiable.ids.Id;
import com.bakdata.conquery.models.identifiable.ids.IdUtil;
import com.bakdata.conquery.models.identifiable.ids.MetaId;
import com.bakdata.conquery.models.identifiable.ids.NamespacedId;

public record IdPathParamConverterProvider(MetaStorage metaStorage, NamespacedStorageProvider namespacedStorageProvider)
		implements ParamConverterProvider {

	@SuppressWarnings({"raw", "unchecked"})
	@Override
	public <T> ParamConverter<T> getConverter(Class<T> rawType, Type genericType, Annotation[] annotations) {
		if (!Id.class.isAssignableFrom(rawType)) {
			return null;
		}

		Object storage = null;

		if (MetaId.class.isAssignableFrom(rawType)) {
			storage = metaStorage();
		}

		if (NamespacedId.class.isAssignableFrom(rawType)) {
			storage = namespacedStorageProvider();
		}

		return new IdPathParamConverter(IdUtil.createParser((Class<? extends Id<?,?>>) rawType), storage);
	}

	public record IdPathParamConverter<T extends Id<?, STORAGE>, STORAGE>(IdUtil.Parser<T> parser, STORAGE storage)
			implements ParamConverter<T> {

		@Override
		public T fromString(String value) {
			T parsed = parser.parse(value);
			parsed.setDomain(storage);
			return parsed;
		}

		@Override
		public String toString(T value) {
			return value.toString();
		}

	}
}
