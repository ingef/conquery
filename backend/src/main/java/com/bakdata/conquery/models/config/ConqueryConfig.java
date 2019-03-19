package com.bakdata.conquery.models.config;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.bakdata.conquery.models.auth.AuthConfig;
import com.bakdata.conquery.models.auth.DevAuthConfig;
import com.bakdata.conquery.models.identifiable.mapping.IdMappingConfig;
import com.bakdata.conquery.models.identifiable.mapping.NoIdMapping;
import com.bakdata.conquery.models.preproc.DateFormats;

import io.dropwizard.Configuration;
import io.dropwizard.server.DefaultServerFactory;
import io.dropwizard.server.ServerFactory;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class ConqueryConfig extends Configuration {
	
	@Getter
	private static ConqueryConfig instance;
	
	@Valid @NotNull
	private ClusterConfig cluster = new ClusterConfig();
	@Valid @NotNull
	private PreprocessingConfig preprocessor = new PreprocessingConfig();
	@Valid @NotNull
	private CSVConfig csv = new CSVConfig();
	@Valid @NotNull
	private LocaleConfig locale = new LocaleConfig();
	@Valid @NotNull
	private StandaloneConfig standalone = new StandaloneConfig();
	@Valid @NotNull
	private StorageConfig storage = new StorageConfig();
	@Valid @NotNull
	private QueryConfig queries = new QueryConfig();
	@Valid @NotNull
	private APIConfig api = new APIConfig();
	@NotNull
	private String[] additionalFormats = new String[0];
	@Valid @NotNull
	private FrontendConfig frontend = new FrontendConfig();
	
	@NotNull @Valid
	private IdMappingConfig idMapping = new NoIdMapping();

	private AuthConfig authentication = new DevAuthConfig();
	/**
	 * null means here that we try to deduce from an attached agent
	 */
	private Boolean debugMode = null;

	//this is needed to force start the REST backend on /api/
	public ConqueryConfig() {
		((DefaultServerFactory)this.getServerFactory()).setJerseyRootPath("/api/");
		ConqueryConfig.instance = this;
	}
	
	@Override
	public void setServerFactory(ServerFactory factory) {
		super.setServerFactory(factory);
		((DefaultServerFactory)this.getServerFactory()).setJerseyRootPath("/api/");
	}

	public void initializeDatePatterns() {
		DateFormats.initialize(additionalFormats);
	}
}
