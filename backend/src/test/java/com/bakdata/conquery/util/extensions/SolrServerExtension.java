package com.bakdata.conquery.util.extensions;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.testcontainers.containers.SolrContainer;
import org.testcontainers.utility.DockerImageName;

@Slf4j
@RequiredArgsConstructor
public class SolrServerExtension implements BeforeAllCallback, AfterAllCallback {

	public final static String SOLR_BASE_URL_ENV = "SOLR_BASE_URL";

	private final String collection;

	@Getter
	private String solrBaseUrl;

	private SolrContainer solrContainer;

	@Override
	public void beforeAll(ExtensionContext context) throws Exception {
		solrBaseUrl = System.getenv(SOLR_BASE_URL_ENV);

		if (solrBaseUrl != null) {
			log.info("Environment variable {} was set to {}. Using external solr for testing.", SOLR_BASE_URL_ENV, solrBaseUrl);
			return;
		}

		log.info("Spin up local container");
		SolrContainer container = new SolrContainer(DockerImageName.parse("solr:9"));
		container.withCollection(collection);
		container.start();

		solrBaseUrl = "http://" + container.getHost() + ":" + container.getSolrPort() + "/solr";
	}

	@Override
	public void afterAll(ExtensionContext context) {
		if (solrContainer != null) {
			solrContainer.close();
		}
	}
}
