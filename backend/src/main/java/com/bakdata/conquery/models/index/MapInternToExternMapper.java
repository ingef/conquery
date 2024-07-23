package com.bakdata.conquery.models.index;


import java.net.URI;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.jackson.Initializing;
import com.bakdata.conquery.io.jackson.serializer.NsIdRef;
import com.bakdata.conquery.io.storage.NamespaceStorage;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.identifiable.NamedImpl;
import com.bakdata.conquery.models.identifiable.ids.NamespacedIdentifiable;
import com.bakdata.conquery.models.identifiable.ids.specific.InternToExternMapperId;
import com.bakdata.conquery.util.io.FileUtil;
import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.OptBoolean;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
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
@JsonDeserialize(converter = MapInternToExternMapper.Initializer.class )
public class MapInternToExternMapper extends NamedImpl<InternToExternMapperId> implements InternToExternMapper, NamespacedIdentifiable<InternToExternMapperId>, Initializing<MapInternToExternMapper> {


	// We inject the service as a non-final property so, jackson will never try to create a serializer for it (in contrast to constructor injection)
	@JsonIgnore
	@JacksonInject(useInput = OptBoolean.FALSE)
	private IndexService mapIndex;

	@JsonIgnore
	@JacksonInject(useInput = OptBoolean.FALSE)
	private ConqueryConfig config;

	@JsonIgnore
	@JacksonInject(useInput = OptBoolean.FALSE)
	@NotNull
	@Setter(onMethod_ = @TestOnly)
	private NamespaceStorage storage;

	@NsIdRef
	@Setter
	@NotNull
	private Dataset dataset;

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


	//Manager only
	@JsonIgnore
	@Getter(onMethod_ = {@TestOnly})
	private MapIndex int2ext = null;

	@Override
	public synchronized MapInternToExternMapper init() {

		// An alternative would be to register the converter programmatically for the internal mappers but not for the
		// test mapper.
		if (mapIndex == null && config == null) {
			log.trace("Injections were null. Skipping init, because class was deserialized by a test object mapper");
			return this;
		}

		if (dataset == null) {
			// TODO remove this as the dataset can be derived from the storage and does not need to be explicitly declared
			dataset = storage.getDataset();
		}

		final URI resolvedURI = FileUtil.getResolvedUri(config.getIndex().getBaseUrl(), csv);
		log.trace("Resolved mapping reference csv url '{}': {}", this.getId(), resolvedURI);

		MapIndexKey key = new MapIndexKey(resolvedURI, internalColumn, externalTemplate);
		try {
			int2ext = mapIndex.getIndex(key);
		} catch (Exception e) {
			log.warn("Unable to get index: {} (enable TRACE for exception)", key, (Exception) (log.isTraceEnabled() ? e : null));
		}
		return this;
	}


	@Override
	public boolean initialized() {
		return int2ext != null;
	}

	@Override
	public String external(String internalValue) {
		if(!initialized()){
			return internalValue;
		}

		return int2ext.getOrDefault(internalValue, internalValue);
	}

	@Override
	public InternToExternMapperId createId() {
		return new InternToExternMapperId(getDataset().getId(), getName());
	}

	public static class Initializer extends Initializing.Converter<MapInternToExternMapper> {}
}
