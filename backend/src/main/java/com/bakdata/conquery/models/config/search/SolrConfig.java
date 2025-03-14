package com.bakdata.conquery.models.config.search;

import java.net.MalformedURLException;
import java.net.URI;
import java.util.Map;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.util.search.SearchProcessor;
import com.bakdata.conquery.util.search.solr.ManagedSolrClient;
import com.bakdata.conquery.util.search.solr.SolrProcessor;
import com.codahale.metrics.health.HealthCheck;
import io.dropwizard.core.setup.Environment;
import lombok.Data;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.request.HealthCheckRequest;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.common.util.NamedList;

@CPSType(id = "SOLR", base = SearchConfig.class)
@Data
public class SolrConfig implements SearchConfig {

	private final String baseSolrUrl;
	private final boolean compression;
	private final Map<String,String[]> params;
	private int connectionTimeout = 10000;
    private int socketTimeout = 60000;
	private final String username;
	private final String password;

	@Override
	public SearchProcessor createSearchProcessor(Environment environment) {
		try {
			SolrClient client = createManagedClient(environment);
			return new SolrProcessor(client);
		}
		catch (MalformedURLException e) {
			throw new RuntimeException(e);
		}
	}

	public synchronized ManagedSolrClient createManagedClient(Environment environment) throws MalformedURLException {
		ManagedSolrClient managedSolrClient = new ManagedSolrClient(createClient());
		environment.lifecycle().manage(managedSolrClient);
		return managedSolrClient;
	}

	public synchronized SolrClient createClient() throws MalformedURLException {
		ModifiableSolrParams solrParams = new ModifiableSolrParams(params);


		// Unfortunately we cannot use the metrics instrumented DropwizardClient here
		BasicCredentialsProvider credsProvider = new BasicCredentialsProvider();
		credsProvider.setCredentials(
				new AuthScope(HttpHost.create(URI.create(baseSolrUrl).toURL().getHost())),
				new UsernamePasswordCredentials(username, password)
		);

		// Create HTTP client with Basic Auth
		CloseableHttpClient httpClient = HttpClients.custom()
													.setDefaultCredentialsProvider(credsProvider)
													.build();


		HttpSolrClient.Builder builder = new HttpSolrClient.Builder()
//				.withHttpClient(httpClient)
				.withBaseSolrUrl(baseSolrUrl)
				.allowCompression(compression)
				.withInvariantParams(solrParams)
				.withSocketTimeout(socketTimeout)
				.withConnectionTimeout(connectionTimeout);

		SolrClient client = builder.build();

		return client;
	}

	public HealthCheck createHealthCheck(SolrClient client) {
		return new HealthCheck() {
			@Override
			protected Result check() throws Exception {
				NamedList<Object> response = client.request(new HealthCheckRequest());
				String status = (String) response.get("status");
				if ("healthy".equals(status)) {
					return Result.builder().healthy().build();
				}
				else {
					return Result.unhealthy(status);
				}
			}
		};
	}
}
