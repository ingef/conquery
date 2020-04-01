package com.bakdata.conquery.models.query;

import java.nio.charset.Charset;

import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.models.identifiable.mapping.IdMappingState;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class ResultGenerationContext {
	private ManagedExecution<?> execution;
	private IdMappingState mappingState;
	private PrintSettings settings;
	private Charset charset;
	private String lineSeparator;
}
