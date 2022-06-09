package com.bakdata.conquery.models.index;


import java.net.URL;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Stream;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.jackson.serializer.NsIdRef;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.identifiable.IdentifiableImpl;
import com.bakdata.conquery.models.identifiable.Named;
import com.bakdata.conquery.models.identifiable.NamedImpl;
import com.bakdata.conquery.models.identifiable.ids.NamespacedIdentifiable;
import com.bakdata.conquery.models.identifiable.ids.specific.InternToExternMapperId;
import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.OptBoolean;
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
public class MapInternToExternMapper extends NamedImpl<InternToExternMapperId> implements InternToExternMapper, NamespacedIdentifiable<InternToExternMapperId> {


	// We inject the service as a non-final property so, jackson will never try to create a serializer for it (in contrast to constructor injection)
	@JsonIgnore
	@JacksonInject(useInput = OptBoolean.FALSE)
	private MapIndexService mapIndex;

	@NsIdRef
	@Getter
	@Setter
	@NotNull
	private Dataset dataset;

	@Getter
	@ToString.Include
	@NotEmpty
	private final String name;
	@Getter
	@ToString.Include
	@NotNull
	private final URL csv;
	@Getter
	@ToString.Include
	@NotEmpty
	private final String internalColumn;
	@Getter
	@ToString.Include
	@NotEmpty
	private final String externalTemplate;


	//Manager only
	@JsonIgnore
	@Getter(onMethod_ = {@TestOnly})
	private CompletableFuture<Map<String, String>> int2ext = null;


	@Override
	public synchronized void init() {
		// This class gets resolved only on the ManagerNode
		if (int2ext == null || int2ext.isCompletedExceptionally()) {
			// Either the mapping has not been initialized or it has been evicted
			int2ext = mapIndex.getMapping(csv, internalColumn, externalTemplate);
		}
	}

	@Override
	public boolean initialized() {
		return int2ext != null;
	}

	@Override
	public String external(String internalValue) {
		try {
			return int2ext.get(1, TimeUnit.MINUTES).getOrDefault(internalValue, "");
		}
		catch (ExecutionException | InterruptedException | TimeoutException e) {
			log.warn("Unable to get mapping for {} from {}. Returning nothing.", internalValue, this, e);
			return "";
		}
		catch (CancellationException e) {
			log.trace("Reinitializing mapper because previous mapping was cancelled/evicted", e);
			init();
			try {
				// Retry again
				return int2ext.get(1, TimeUnit.MINUTES).getOrDefault(internalValue, "");
			}
			catch (Exception ex) {
				log.warn("Reinitializing mapper did not work. Returning empty mapping", e);
				return "";
			}
		}
	}

	@Override
	public InternToExternMapperId createId() {
		return new InternToExternMapperId(getDataset().getId(), getName());
	}
}
