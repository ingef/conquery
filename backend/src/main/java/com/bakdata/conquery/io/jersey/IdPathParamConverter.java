package com.bakdata.conquery.io.jersey;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import jakarta.ws.rs.ext.ParamConverter;
import jakarta.ws.rs.ext.ParamConverterProvider;

import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.models.identifiable.NamespacedStorageProvider;
import com.bakdata.conquery.models.identifiable.ids.Id;
import com.bakdata.conquery.models.identifiable.ids.IdUtil;
import com.bakdata.conquery.models.identifiable.ids.IdUtil.Parser;
import com.bakdata.conquery.models.identifiable.ids.MetaId;
import com.bakdata.conquery.models.identifiable.ids.NamespacedId;
import lombok.Data;

//TODO invert these classes
//TODO use IdInterner
public class IdPathParamConverter<T extends Id<?, STORAGE>, STORAGE> implements ParamConverter<T> {

	private final Parser<T> parser;
	private final STORAGE storage;

	public IdPathParamConverter(Class<T> type, STORAGE storage) {
		parser = IdUtil.createParser(type);
		this.storage = storage;
	}

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

	@Data
	public static class Provider implements ParamConverterProvider {

		private final MetaStorage metaStorage;
		private final NamespacedStorageProvider namespacedStorageProvider;

		@SuppressWarnings({ "rawtypes", "unchecked" })
		@Override
		public <T> ParamConverter<T> getConverter(Class<T> rawType, Type genericType, Annotation[] annotations) {
			if (MetaId.class.isAssignableFrom(rawType)) {
				return new IdPathParamConverter(rawType, getMetaStorage());
			}

			if (NamespacedId.class.isAssignableFrom(rawType)) {
				return new IdPathParamConverter(rawType, getNamespacedStorageProvider());
			}

			return null;
		}
		
	}
}
