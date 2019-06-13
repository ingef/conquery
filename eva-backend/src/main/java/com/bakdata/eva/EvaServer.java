package com.bakdata.eva;

import java.io.IOException;

import com.bakdata.conquery.Conquery;
import com.bakdata.conquery.io.jackson.Jackson;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.util.UrlRewriteBundle;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.mashape.unirest.http.ObjectMapper;
import com.mashape.unirest.http.Unirest;

import io.dropwizard.assets.AssetsBundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;

public class EvaServer extends Conquery {
	public static void main(String[] args) throws Exception {
		new EvaServer().run(args);
	}
	
	@Override
	public void run(ConqueryConfig config, Environment environment) throws Exception {
		//configure Unirest REST client
		Unirest.setObjectMapper(new ObjectMapper() {
			@Override
			public String writeValue(Object value) {
				try {
					return Jackson.MAPPER.writeValueAsString(value);
				} catch (JsonProcessingException e) {
					throw new RuntimeException("Failed to write '"+value+"' as JSON", e);
				}
			}
			
			@Override
			public <T> T readValue(String value, Class<T> valueType) {
				try {
					return Jackson.MAPPER.readValue(value, valueType);
				} catch (IOException e) {
					throw new RuntimeException("Failed to parse '"+value+"' as JSON", e);
				}
			}
		});
		Unirest.setTimeouts(10_000, Long.MAX_VALUE);
		
		super.run(config, environment);
	}
	

	protected void registerFrontend(Bootstrap<ConqueryConfig> bootstrap) {
		bootstrap.addBundle(new UrlRewriteBundle());
		bootstrap.addBundle(new AssetsBundle("/frontend/app/", "/app/", "static/index.html", "app"));
	}
}
