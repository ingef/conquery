package com.bakdata.conquery.util.search.solr;

import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.config.search.SolrConfig;
import io.dropwizard.core.ConfiguredBundle;
import io.dropwizard.core.setup.Environment;
import io.dropwizard.lifecycle.Managed;
import lombok.extern.slf4j.Slf4j;
import org.apache.solr.client.solrj.SolrClient;

@Slf4j
public class SolrBundle implements ConfiguredBundle<ConqueryConfig>, Managed {

	private SolrClient solrClient;

	@Override
	public void run(ConqueryConfig configuration, Environment environment) throws Exception {
		if (!(configuration.getSearch() instanceof SolrConfig config)) {
			log.trace("Solr is not configured. Skipping initialization");
			return;
		}

		// TODO maybe move to Managed#start
		solrClient = config.initClient(environment);

		environment.lifecycle().manage(this);
	}

	@Override
	public void stop() throws Exception {
		log.info("Stopping solr client");
		solrClient.close();
	}
}
