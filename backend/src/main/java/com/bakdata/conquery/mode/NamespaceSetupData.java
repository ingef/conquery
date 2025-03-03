package com.bakdata.conquery.mode;

import java.util.List;

import com.bakdata.conquery.io.jackson.Injectable;
import com.bakdata.conquery.models.jobs.JobManager;
import com.bakdata.conquery.util.search.SearchProcessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Value;

/**
 * Data required for the set-up of a namespace.
 */
@Value
public class NamespaceSetupData {
	List<Injectable> injectables;
	ObjectMapper communicationMapper;
	ObjectMapper preprocessMapper;
	JobManager jobManager;
	SearchProcessor filterSearch;
}
