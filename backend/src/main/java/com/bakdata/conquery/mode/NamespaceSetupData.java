package com.bakdata.conquery.mode;

import com.bakdata.conquery.models.jobs.JobManager;
import com.bakdata.conquery.util.search.SearchProcessor;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Data required for the set-up of a namespace.
 */
public record NamespaceSetupData(ObjectMapper preprocessMapper, JobManager jobManager, SearchProcessor filterSearch) {
}
