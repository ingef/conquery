package com.bakdata.conquery.models.index;


import java.net.URI;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.identifiable.NamedImpl;
import com.bakdata.conquery.models.identifiable.ids.NamespacedIdentifiable;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.identifiable.ids.specific.InternToExternMapperId;
import com.bakdata.conquery.util.io.FileUtil;
import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.OptBoolean;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
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
public class MapInternToExternMapper extends NamedImpl<InternToExternMapperId> implements InternToExternMapper, NamespacedIdentifiable<InternToExternMapperId> {


	// We inject the service as a non-final property so, jackson will never try to create a serializer for it (in contrast to constructor injection)
	@JsonIgnore
	@JacksonInject(useInput = OptBoolean.FALSE)
	private IndexService mapIndex;

	@JsonIgnore
	@JacksonInject(useInput = OptBoolean.FALSE)
	private ConqueryConfig config;

	@Setter
	@NotNull
	private DatasetId dataset;

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
	public synchronized void init() {

		final URI resolvedURI = FileUtil.getResolvedUri(config.getIndex().getBaseUrl(), csv);
		log.trace("Resolved mapping reference csv url '{}': {}", this.getId(), resolvedURI);

		int2ext = mapIndex.getIndex(new MapIndexKey(resolvedURI, internalColumn, externalTemplate));
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
		return new InternToExternMapperId(getDataset(), getName());
	}
}
