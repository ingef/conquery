package com.bakdata.conquery.models.preproc;

import javax.validation.constraints.NotNull;

import com.bakdata.conquery.models.types.ColumnStore;
import com.bakdata.conquery.models.types.parser.Parser;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Data
@RequiredArgsConstructor
@NoArgsConstructor
public class PPColumn {
	@NonNull
	private String name;

	@SuppressWarnings("rawtypes")
	@NotNull
	private ColumnStore type;
	@SuppressWarnings("rawtypes")
	@JsonIgnore
	private transient Parser parser = null;

	public ColumnStore findBestType() {
		log.info("Compute best Subtype for  Column[{}] with {}", getName(), getParser());
		ColumnStore decision = parser.findBestType();
		// this only creates the headers
		// todo wrap in method
		type = decision.select(new int[0], new int[0]);

		log.info("\t{}: {} -> {}", getName(), getParser(), decision);

		return decision;
	}
}
