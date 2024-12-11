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
import lombok.Data;
import lombok.NonNull;

public class IdParamConverter<T extends Id<?>> implements ParamConverter<T> {
	@NonNull

	private final NamespacedStorageProvider namespacedStorageProvider;

	@NonNull
	private final MetaStorage metaStorage;

	private final Parser<T> parser;

	public IdParamConverter(NamespacedStorageProvider namespacedStorageProvider, MetaStorage metaStorage, Class<T> type) {
		this.namespacedStorageProvider = namespacedStorageProvider;
		this.metaStorage = metaStorage;
		parser = IdUtil.createParser(type);
	}

	@Override
	public T fromString(String value) {
		final T parsed = parser.parse(value);

//		parsed.setMetaStorage(metaStorage);
//		parsed.setNamespacedStorageProvider(namespacedStorageProvider);

		return parsed;
	}

	@Override
	public String toString(T value) {
		return value.toString();
	}

	@Data
	public static class Provider implements ParamConverterProvider {
		@NonNull
		private final NamespacedStorageProvider namespacedStorageProvider;
		@NonNull
		private final MetaStorage metaStorage;


		@SuppressWarnings({ "rawtypes", "unchecked" })
		@Override
		public <T> ParamConverter<T> getConverter(Class<T> rawType, Type genericType, Annotation[] annotations) {
			if (Id.class.isAssignableFrom(rawType)) {
				return new IdParamConverter(namespacedStorageProvider, metaStorage, rawType);
			}
			return null;
		}
	}
}

