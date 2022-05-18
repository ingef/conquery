package com.bakdata.conquery.models.index;


import java.net.URL;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.bakdata.conquery.io.cps.CPSType;
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
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@CPSType(id = "CSV_MAP", base = InternToExternMapper.class)
@RequiredArgsConstructor
@ToString(onlyExplicitlyIncluded = true)
public class MapInternToExternMapper extends NamedImpl<InternToExternMapperId> implements InternToExternMapper, NamespacedIdentifiable<InternToExternMapperId> {


	// We inject the service as a non-final property so, jackson will never try to create a serializer for it (in contrast to constructor injection)
	@JsonIgnore
	@JacksonInject(useInput = OptBoolean.FALSE)
	private MapIndexService mapIndex;

	@JsonIgnore
	@JacksonInject(useInput = OptBoolean.FALSE)
	@Getter
	private Dataset dataset;

	@Getter
	@ToString.Include
	private final String name;
	@Getter
	@ToString.Include
	private final URL csv;
	@Getter
	@ToString.Include
	private final String internalColumn;
	@Getter
	@ToString.Include
	private final String externalTemplate;


	//Manager only
	@JsonIgnore
	private CompletableFuture<Map<String, String>> int2ext = null;


	@Override
	public void init() {
		if (mapIndex == null) {
			// Todo ensure we are on a Worker
			return;
		}
		// Manager
		int2ext = mapIndex.getMapping(csv, internalColumn, externalTemplate);
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
	}

	@Override
	public InternToExternMapperId createId() {
		return new InternToExternMapperId(getDataset().getId(), getName());
	}
}
