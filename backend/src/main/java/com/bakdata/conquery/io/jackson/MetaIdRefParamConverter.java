package com.bakdata.conquery.io.jackson;

import java.util.concurrent.TimeUnit;

import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.models.identifiable.Identifiable;
import com.bakdata.conquery.models.identifiable.ids.Id;
import com.bakdata.conquery.models.identifiable.ids.IdUtil;
import com.github.benmanes.caffeine.cache.CacheLoader;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import jakarta.ws.rs.ext.ParamConverter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.checkerframework.checker.nullness.qual.Nullable;

@RequiredArgsConstructor
public class MetaIdRefParamConverter<ID extends Id<VALUE>, VALUE extends Identifiable<ID>> implements ParamConverter<VALUE> {

	private final IdUtil.Parser<ID> idParser;
	@NonNull
	private final MetaStorage storage;

	LoadingCache<ID, VALUE> cache = Caffeine.newBuilder()
											.expireAfterAccess(10, TimeUnit.MINUTES)
											.build(new CacheLoader<>() {
												@Override
												public @Nullable VALUE load(ID id) {
													return storage.get(id);
												}
											});

	@Override
	public VALUE fromString(String value) {
		final ID id = idParser.parse(value);

		return cache.get(id);
	}

	@Override
	public String toString(VALUE value) {
		return value.getId().toString();
	}
}
