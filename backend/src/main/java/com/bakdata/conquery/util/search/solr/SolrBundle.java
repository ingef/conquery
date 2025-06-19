package com.bakdata.conquery.util.search.solr;

import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.config.search.solr.SolrConfig;
import io.dropwizard.core.ConfiguredBundle;
import io.dropwizard.core.setup.Environment;
import io.dropwizard.lifecycle.Managed;
import lombok.extern.slf4j.Slf4j;
import org.apache.solr.client.solrj.SolrClient;

@Slf4j
public class SolrBundle implements ConfiguredBundle<ConqueryConfig>, Managed {

	private Environment environment;
	private SolrConfig solrConfig;
	private SolrClient solrClient;

	@Override
	public void run(ConqueryConfig configuration, Environment environment) throws Exception {
		this.environment = environment;
		if (!(configuration.getSearch() instanceof SolrConfig config)) {
			log.trace("Solr is not configured. Skipping initialization");
			return;
		}
		this.solrConfig = config;

		// This client is not bound to any collection, because it just pings solr
		solrClient = config.createSearchClient(null);

		environment.healthChecks().register(config.getBaseSolrUrl(), config.createHealthCheck(solrClient));

		environment.lifecycle().manage(this);
	}

	@Override
	public void stop() throws Exception {
		log.info("Unregister health check for {}", solrConfig.getBaseSolrUrl());
		environment.healthChecks().unregister(solrConfig.getBaseSolrUrl());
		solrClient.close();
	}
}
