package com.bakdata.conquery.models.config;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.bakdata.conquery.commands.ManagerNode;
import com.bakdata.conquery.commands.ShardNode;
import com.bakdata.conquery.io.jackson.serializer.CDateSetDeserializer;
import com.bakdata.conquery.io.jackson.serializer.CDateSetSerializer;
import com.bakdata.conquery.io.jackson.serializer.FormatedDateDeserializer;
import com.bakdata.conquery.io.result.ResultRender.ResultRendererProvider;
import com.bakdata.conquery.models.config.auth.AuthenticationConfig;
import com.bakdata.conquery.models.config.auth.AuthorizationConfig;
import com.bakdata.conquery.models.auth.develop.DevAuthConfig;
import com.bakdata.conquery.models.config.auth.DevelopmentAuthorizationConfig;
import com.bakdata.conquery.models.common.CDateSet;
import com.bakdata.conquery.models.identifiable.mapping.IdMappingConfig;
import com.bakdata.conquery.util.DateFormats;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.google.common.collect.MoreCollectors;
import io.dropwizard.Configuration;
import io.dropwizard.server.DefaultServerFactory;
import io.dropwizard.server.ServerFactory;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.With;

@Getter
@Setter
@AllArgsConstructor
@With
public class ConqueryConfig extends Configuration {

	@Valid
	@NotNull
	private ClusterConfig cluster = new ClusterConfig();
	@Valid
	@NotNull
	private PreprocessingConfig preprocessor = new PreprocessingConfig();
	@Valid
	@NotNull
	private CSVConfig csv = new CSVConfig();
	@Valid @NotNull
	private ArrowConfig arrow = new ArrowConfig();

	/**
	 * The order of this lists determines the ordner of the generated result urls in a query status.
	 */
	@Valid @NotNull
	private List<ResultRendererProvider> resultProviders = List.of(
			new XlsxResultProvider(),
			new CsvResultRendererProvider(),
			new ArrowFileResultProvider(),
			new ArrowStreamResultProvider()
	);
	@Valid @NotNull
	private LocaleConfig locale = new LocaleConfig();
	@Valid
	@NotNull
	private StandaloneConfig standalone = new StandaloneConfig();
	@Valid
	@NotNull
	private StoreFactory storage = new XodusStoreFactory();
	@Valid
	@NotNull
	private QueryConfig queries = new QueryConfig();
	@Valid
	@NotNull
	private APIConfig api = new APIConfig();
	@Valid
	@NotNull
	private FrontendConfig frontend = new FrontendConfig();

	private ConqueryMetricsConfig metricsConfig = new ConqueryMetricsConfig();

	@NotNull
	@Valid
	private IdMappingConfig idMapping = new NoIdMapping();
	@Valid
	@NotNull
	private List<AuthenticationConfig> authentication = List.of(new DevAuthConfig());

	@Valid
	@NotNull
	private AuthorizationConfig authorization = new DevelopmentAuthorizationConfig();
	@Valid
	@NotNull
	private ExcelConfig excel = new ExcelConfig();

	@Valid
	private List<PluginConfig> plugins = new ArrayList<>();
	/**
	 * null means here that we try to deduce from an attached agent
	 */
	private Boolean debugMode = null;

	private boolean failOnError = false;


	//this is needed to force start the REST backend on /api/
	public ConqueryConfig() {
		((DefaultServerFactory) this.getServerFactory()).setJerseyRootPath("/api/");
	}

	@Override
	public void setServerFactory(ServerFactory factory) {
		super.setServerFactory(factory);
		((DefaultServerFactory) this.getServerFactory()).setJerseyRootPath("/api/");
	}

	public void initialize(ManagerNode node) {
		storage.init(node);
		plugins.forEach(config -> config.initialize((node)));
	}

	public void initialize(ShardNode node) {
		storage.init(node);
	}

	public <T extends PluginConfig> Optional<T> getPluginConfig(Class<T> type) {
		return plugins.stream()
				.filter(c -> type.isAssignableFrom(c.getClass()))
				.map(type::cast)
				.collect(MoreCollectors.toOptional());
	}

	public ObjectMapper configureObjectMapper(ObjectMapper objectMapper) {
		return objectMapper.registerModule(new ConqueryConfig.ConfiguredModule(this));
	}

	public static class ConfiguredModule extends SimpleModule {
		public ConfiguredModule(ConqueryConfig config){
			DateFormats dateFormats = config.getPreprocessor().getParsers().getDateFormats();
			addDeserializer(LocalDate.class, new FormatedDateDeserializer(dateFormats));
			
			addDeserializer(CDateSet.class, new CDateSetDeserializer(dateFormats));
			addSerializer(CDateSet.class, new CDateSetSerializer());
		}
	}

}
