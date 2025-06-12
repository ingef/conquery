package com.bakdata.conquery.io.jersey;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import jakarta.inject.Inject;
import jakarta.ws.rs.ext.ParamConverter;
import jakarta.ws.rs.ext.ParamConverterProvider;

import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.models.identifiable.NamespacedStorageProvider;
import com.bakdata.conquery.models.identifiable.ids.Id;
import com.bakdata.conquery.models.identifiable.ids.IdUtil;
import com.bakdata.conquery.models.identifiable.ids.MetaId;
import com.bakdata.conquery.models.identifiable.ids.NamespacedId;
import lombok.NonNull;

/**
 * Generic provider to allow strictly typed {@link Id} usage in resource definitions.
 * <p>
 * Registered into a server, it will hook into any Param definition for {@link Id}, use an appropriate parser and set their provided domain.
 */
public record IdPathParamConverterProvider(@Inject @NonNull MetaStorage metaStorage,
										   @Inject @NonNull NamespacedStorageProvider namespacedStorageProvider) implements ParamConverterProvider {

	@SuppressWarnings({"unchecked", "rawtypes"})
	@Override
	public <T> ParamConverter<T> getConverter(Class<T> rawType, Type genericType, Annotation[] annotations) {
		if (!Id.class.isAssignableFrom(rawType)) {
			return null;
		}

		if (MetaId.class.isAssignableFrom(rawType)) {
			return new IdPathParamConverter(IdUtil.createParser((Class<? extends MetaId<?>>) rawType), metaStorage());
		}

		if (NamespacedId.class.isAssignableFrom(rawType)) {
			return new IdPathParamConverter(IdUtil.createParser((Class<? extends NamespacedId<?>>) rawType), namespacedStorageProvider());
		}

		throw new IllegalStateException("Unsupported Id-type %s".formatted(rawType));
	}

	public record IdPathParamConverter<T extends Id<?, STORAGE>, STORAGE>(IdUtil.Parser<T> parser, STORAGE storage) implements ParamConverter<T> {

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
