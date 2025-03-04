package com.bakdata.conquery.models.config.search;

import java.net.MalformedURLException;
import java.net.URI;
import java.util.Map;

import com.bakdata.conquery.apiv1.FilterTemplate;
import com.bakdata.conquery.apiv1.LabelMap;
import com.bakdata.conquery.apiv1.frontend.FrontendValue;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.datasets.concepts.Searchable;
import com.bakdata.conquery.util.search.SearchProcessor;
import com.bakdata.conquery.util.search.solr.SolrProcessor;
import com.bakdata.conquery.util.search.solr.SolrSearch;
import io.dropwizard.core.setup.Environment;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Setter;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.common.params.ModifiableSolrParams;

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

	@Setter(AccessLevel.PRIVATE)
	private SolrClient solrClient;

	public SolrSearch createSearch(Searchable<FrontendValue> searchable) {
		if (searchable instanceof FilterTemplate temp) {

			return getFilterTemplateSearch(temp);
		}

		if (searchable instanceof LabelMap labelMap) {
			return getLabelMapSearch(labelMap);
		}

		return new SolrSearch(solrClient);
	}

	private SolrSearch getLabelMapSearch(LabelMap labelMap) {
		return null;
	}

	private SolrSearch getFilterTemplateSearch(FilterTemplate temp) {
		return null;
	}

	@Override
	public SearchProcessor createSearchProcessor() {
		return new SolrProcessor(this);
	}

	public synchronized SolrClient initClient(Environment environment) throws MalformedURLException {
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

		HttpSolrClient client = builder.build();

		solrClient = client;

		return client;
	}
}
