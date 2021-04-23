package com.bakdata.conquery.tasks;

import java.io.PrintWriter;
import java.util.List;
import java.util.Map;

import com.bakdata.conquery.apiv1.FilterSearch;
import io.dropwizard.servlets.tasks.Task;

/**
 * Cleans the source search mapping for filters. Executing this does not update the source search on existing concepts.
 * To do that, an {@link com.bakdata.conquery.models.messages.namespaces.specific.UpdateMatchingStatsMessage} needs to be invoked with will then trigger an update of all source searches.
 *
 */
public class ClearFilterSourceSearch extends Task {

	public ClearFilterSourceSearch() {
		super("clear-filter-source-search");
	}

	@Override
	public void execute(Map<String, List<String>> parameters, PrintWriter output) throws Exception {
		FilterSearch.clear();		
	}

}
