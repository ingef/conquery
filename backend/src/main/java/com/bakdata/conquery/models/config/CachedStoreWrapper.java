package com.bakdata.conquery.models.config;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.storage.Store;
import com.bakdata.conquery.io.storage.xodus.stores.CachedStore;
import io.dropwizard.util.Duration;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@CPSType(id = "CACHED", base = CachingStoreWrapper.class)
@Data
public class CachedStoreWrapper implements CachingStoreWrapper {

	@NotNull
	private Duration cacheDuration = Duration.hours(48);

	@Override
	public <KEY, VALUE> Store<KEY, VALUE> wrap(Store<KEY, VALUE> store) {
		return new CachedStore<>(store, cacheDuration);
	}
}
