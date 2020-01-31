package com.bakdata.conquery.models.config;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.bakdata.conquery.models.auth.AuthenticationConfig;
import com.bakdata.conquery.models.auth.AuthorizationConfig;
import com.bakdata.conquery.models.auth.develop.DevAuthConfig;
import com.bakdata.conquery.models.auth.develop.DevelopmentAuthorizationConfig;
import com.bakdata.conquery.models.identifiable.mapping.IdMappingConfig;
import com.bakdata.conquery.models.identifiable.mapping.NoIdMapping;
import com.bakdata.conquery.models.preproc.DateFormats;
import com.bakdata.conquery.util.DebugMode;
import com.google.common.collect.MoreCollectors;
import io.dropwizard.Configuration;
import io.dropwizard.server.DefaultServerFactory;
import io.dropwizard.server.ServerFactory;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.ArrayUtils;

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
	private String[] additionalFormats = ArrayUtils.EMPTY_STRING_ARRAY;
	@Valid @NotNull
	private FrontendConfig frontend = new FrontendConfig();
	
	@NotNull @Valid
	private IdMappingConfig idMapping = new NoIdMapping();

	private List<AuthenticationConfig> authentication = List.of(new DevAuthConfig());
	
	private AuthorizationConfig authorization = new DevelopmentAuthorizationConfig();
	
	private List<PluginConfig> plugins = new ArrayList<>();
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

	public <T extends PluginConfig> T getPluginConfig(Class<T> type) {
		return (T) plugins.stream()
			.filter(c -> type.isAssignableFrom(c.getClass()))
			.collect(MoreCollectors.toOptional())
			.orElseThrow(()-> new NoSuchElementException("No plugin config of type "+type.getClass().getSimpleName()+" configured"));
	}

	public void initialize() {
		if(debugMode != null) {
			DebugMode.setActive(debugMode);
		}
		DateFormats.initialize(additionalFormats);
	}
}
