package com.bakdata.conquery.models.preproc;

import com.bakdata.conquery.models.events.MajorTypeId;
import com.bakdata.conquery.models.events.stores.root.ColumnStore;
import com.bakdata.conquery.models.preproc.parser.Parser;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Data
@RequiredArgsConstructor(onConstructor_ = {@JsonCreator} )
public class PPColumn {
	@NonNull
	private final String name;

	private final MajorTypeId type;

	@SuppressWarnings("rawtypes")
	@JsonIgnore
	private transient Parser parser = null;

	public ColumnStore findBestType() {
		log.info("Compute best Subtype for  Column[{}] with {}", getName(), getParser());
		ColumnStore decision = parser.findBestType();
		// this only creates the headers

		log.debug("\t{}: {} -> {}", getName(), getParser(), decision);

		return decision;
	}
}
