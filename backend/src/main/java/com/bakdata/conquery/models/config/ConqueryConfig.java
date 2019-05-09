package com.bakdata.conquery.models.config;

import java.util.ArrayList;
import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.bakdata.conquery.models.auth.AuthConfig;
import com.bakdata.conquery.models.auth.DevAuthConfig;
import com.bakdata.conquery.models.identifiable.mapping.IdMappingConfig;
import com.bakdata.conquery.models.identifiable.mapping.NoIdMapping;
import com.bakdata.conquery.models.preproc.DateFormats;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.collect.ClassToInstanceMap;
import com.google.common.collect.ImmutableClassToInstanceMap;
import com.google.common.collect.ImmutableClassToInstanceMap.Builder;

import io.dropwizard.Configuration;
import io.dropwizard.server.DefaultServerFactory;
import io.dropwizard.server.ServerFactory;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class ConqueryConfig extends Configuration {
	
	@Getter
	private static ConqueryConfig instance = new ConqueryConfig();
	
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
	
	private List<PluginConfig> pluggedConfigs = new ArrayList<>();
	@JsonIgnore
	private ClassToInstanceMap<PluginConfig> pluggedInstances;
	/**
	 * null means here that we try to deduce from an attached agent
	 */
	private Boolean debugMode = null;

	//this is needed to force start the REST backend on /api/
	public ConqueryConfig() {
		((DefaultServerFactory)this.getServerFactory()).setJerseyRootPath("/api/");
		pluggedInstances = preparePluginMap(pluggedConfigs);
		ConqueryConfig.instance = this;
	}
	
	private static ImmutableClassToInstanceMap<PluginConfig> preparePluginMap(List<PluginConfig> configs) {
		Builder<PluginConfig> builder = ImmutableClassToInstanceMap.<PluginConfig>builder();
		for(PluginConfig config : configs) {
			builder.put(config.getClass(), config);
		}
		 return builder.build();
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
