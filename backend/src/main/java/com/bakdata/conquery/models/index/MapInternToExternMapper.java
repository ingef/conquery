package com.bakdata.conquery.models.index;

import static com.bakdata.conquery.util.io.LogUtil.passExceptionOnTrace;

import java.net.URI;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.jackson.Initializing;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.util.io.FileUtil;
import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.OptBoolean;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.FieldNameConstants;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.TestOnly;

@Slf4j
@CPSType(id = "CSV_MAP", base = InternToExternMapper.class)
@ToString(onlyExplicitlyIncluded = true)
@FieldNameConstants
@Getter
@JsonDeserialize(converter = MapInternToExternMapper.Initializer.class)
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor(access = AccessLevel.PRIVATE, onConstructor_ = {@JsonCreator})
@Setter
public class MapInternToExternMapper extends InternToExternMapper implements Initializing {


	@ToString.Include
	@NotEmpty
	private String name;
	@ToString.Include
	@NotNull
	private URI csv;
	@ToString.Include
	@NotEmpty
	private String internalColumn;
	@ToString.Include
	@NotEmpty
	private String externalTemplate;
	private boolean allowMultiple;

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

	//Manager only
	@JsonIgnore
	@Getter(onMethod_ = {@TestOnly})
	@EqualsAndHashCode.Exclude
	private CompletableFuture<Index<String>> int2ext;

	public MapInternToExternMapper(@NotEmpty String name, @NotNull URI csv, @NotEmpty String internalColumn, @NotEmpty String externalTemplate, boolean allowMultiple) {
		this.name = name;
		this.csv = csv;
		this.internalColumn = internalColumn;
		this.externalTemplate = externalTemplate;
		this.allowMultiple = allowMultiple;
	}


	@Override
	public synchronized void init() {

		if (mapIndex == null && config == null) {
			log.trace("Injections were null. Skipping init, because class was deserialized by a test object mapper");
			return;
		}

		final URI resolvedURI = FileUtil.getResolvedUri(config.getIndex().getBaseUrl(), csv);
		log.trace("Resolved mapping reference csv url '{}': {}", getName(), resolvedURI);

		final IndexKey key = new MapIndexKey(resolvedURI, internalColumn, externalTemplate, allowMultiple);

		int2ext = CompletableFuture.supplyAsync(() -> {
			try {
				return mapIndex.<Index<String>>getIndex(key);
			}
			catch (IndexCreationException e) {
				throw new IllegalStateException(e);
			}
		}).whenComplete((m, e) -> {
			if (e != null) {
				log.warn("Unable to get index: {} (enable TRACE for exception)", key, passExceptionOnTrace(log,e));
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
				log.warn("Unable to resolve mapping for internal value {} (enable TRACE for exception)", internalValue, passExceptionOnTrace(log,e));
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
				log.warn("Unable to resolve mapping for internal value {} (enable TRACE for exception)", internalValue, passExceptionOnTrace(log,e));
			}
		}

		return internalValue;
	}


	public static class Initializer extends Initializing.Converter<MapInternToExternMapper> {
	}
}
