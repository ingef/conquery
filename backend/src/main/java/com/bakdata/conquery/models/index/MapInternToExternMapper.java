package com.bakdata.conquery.models.index;


import java.net.URI;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.jackson.Initializing;
import com.bakdata.conquery.io.storage.NamespaceStorage;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.identifiable.NamedImpl;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.identifiable.ids.specific.InternToExternMapperId;
import com.bakdata.conquery.util.io.FileUtil;
import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.OptBoolean;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.FieldNameConstants;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.TestOnly;

@Slf4j
@CPSType(id = "CSV_MAP", base = InternToExternMapper.class)
@RequiredArgsConstructor
@ToString(onlyExplicitlyIncluded = true)
@FieldNameConstants
@Getter
@JsonDeserialize(converter = MapInternToExternMapper.Initializer.class)
@EqualsAndHashCode(callSuper = true)
public class MapInternToExternMapper extends NamedImpl<InternToExternMapperId> implements InternToExternMapper, Initializing {


	@ToString.Include
	@NotEmpty
	private final String name;
	@ToString.Include
	@NotNull
	private final URI csv;
	@ToString.Include
	@NotEmpty
	private final String internalColumn;
	@ToString.Include
	@NotEmpty
	private final String externalTemplate;
	private final boolean allowMultiple;
	// We inject the service as a non-final property so, jackson will never try to create a serializer for it (in contrast to constructor injection)
	@JsonIgnore
	@JacksonInject(useInput = OptBoolean.FALSE)
	@NotNull
	@Setter(onMethod_ = @TestOnly)
	@EqualsAndHashCode.Exclude
	private IndexService mapIndex;
	@JsonIgnore
	@JacksonInject(useInput = OptBoolean.FALSE)
	@NotNull
	@Setter(onMethod_ = @TestOnly)
	@EqualsAndHashCode.Exclude
	private ConqueryConfig config;
	@JsonIgnore
	@JacksonInject(useInput = OptBoolean.FALSE)
	@NotNull
	@Setter(onMethod_ = @TestOnly)
	@EqualsAndHashCode.Exclude
	private NamespaceStorage storage;
	@JsonIgnore
	@NotNull
	private DatasetId dataset;
	//Manager only
	@JsonIgnore
	@Getter(onMethod_ = {@TestOnly})
	@EqualsAndHashCode.Exclude
	private CompletableFuture<Index<String>> int2ext;


	@Override
	public synchronized void init() {

		if (mapIndex == null && config == null) {
			log.trace("Injections were null. Skipping init, because class was deserialized by a test object mapper");
			return;
		}

		dataset = storage.getDataset().getId();

		final URI resolvedURI = FileUtil.getResolvedUri(config.getIndex().getBaseUrl(), csv);
		log.trace("Resolved mapping reference csv url '{}': {}", getId(), resolvedURI);

		final IndexKey key = new MapIndexKey(resolvedURI, internalColumn, externalTemplate, allowMultiple);

		int2ext = CompletableFuture.supplyAsync(() -> {
									   try {
										   return mapIndex.<Index<String>>getIndex(key);
									   }
									   catch (IndexCreationException e) {
										   throw new IllegalStateException(e);
									   }
								   })
								   .whenComplete((m, e) -> {
									   if (e != null) {
										   log.warn("Unable to get index: {} (enable TRACE for exception)", key, log.isTraceEnabled() ? e : null);
									   }
								   });
	}

	@Override
	public Collection<String> externalMultiple(String internalValue) {
		if (indexAvailable()) {
			try {
				final Collection<String> mapped = int2ext.get().externalMultiple(internalValue);

				if (mapped == null) {
					return Collections.singleton(internalValue);
				}

				return mapped;
			}
			catch (InterruptedException | ExecutionException e) {
				// Should never be reached
				log.warn("Unable to resolve mapping for internal value {} (enable TRACE for exception)", internalValue, log.isTraceEnabled() ? e : null);
			}
		}

		return Collections.singleton(internalValue);
	}

	private boolean indexAvailable() {
		if (!initialized()) {
			log.trace("Mapping {} not available, because mapper is not yet initialized", getId());
			return false;
		}

		if (int2ext.isCompletedExceptionally() || int2ext.isCancelled()) {
			log.trace("Mapping {} not available, because mapper could not be initialized", getId());
			return false;
		}
		return true;
	}

	@Override
	public boolean initialized() {
		return int2ext != null && int2ext.isDone();
	}

	@Override
	public String external(String internalValue) {
		if (indexAvailable()) {
			try {
				return int2ext.get().external(internalValue);
			}
			catch (InterruptedException | ExecutionException e) {
				// Should never be reached
				log.warn("Unable to resolve mapping for internal value {} (enable TRACE for exception)", internalValue, log.isTraceEnabled() ? e : null);
			}
		}

		return internalValue;
	}

	@Override
	public InternToExternMapperId createId() {
		return new InternToExternMapperId(getDataset(), getName());
	}

	public static class Initializer extends Initializing.Converter<MapInternToExternMapper> {
	}
}
