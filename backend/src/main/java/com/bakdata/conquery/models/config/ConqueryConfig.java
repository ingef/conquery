package com.bakdata.conquery.models.config;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.bakdata.conquery.commands.ManagerNode;
import com.bakdata.conquery.io.jackson.Injectable;
import com.bakdata.conquery.io.jackson.MutableInjectableValues;
import com.bakdata.conquery.io.jackson.serializer.CDateSetDeserializer;
import com.bakdata.conquery.io.jackson.serializer.CDateSetSerializer;
import com.bakdata.conquery.io.jackson.serializer.FormatedDateDeserializer;
import com.bakdata.conquery.io.result.ResultRender.ResultRendererProvider;
import com.bakdata.conquery.models.auth.develop.DevAuthConfig;
import com.bakdata.conquery.models.common.CDateSet;
import com.bakdata.conquery.models.config.auth.AuthenticationConfig;
import com.bakdata.conquery.models.config.auth.AuthenticationRealmFactory;
import com.bakdata.conquery.models.config.auth.AuthorizationConfig;
import com.bakdata.conquery.models.config.auth.DevelopmentAuthorizationConfig;
import com.bakdata.conquery.models.config.auth.MultiInstancePlugin;
import com.bakdata.conquery.util.DateReader;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.google.common.collect.MoreCollectors;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import io.dropwizard.Configuration;
import io.dropwizard.client.JerseyClientConfiguration;
import io.dropwizard.validation.ValidationMethod;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.With;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@With
public class ConqueryConfig extends Configuration implements Injectable {

	@Valid
	@NotNull
	private ClusterConfig cluster = new ClusterConfig();
	@Valid
	@NotNull
	private PreprocessingConfig preprocessor = new PreprocessingConfig();
	@Valid
	@NotNull
	private CSVConfig csv = new CSVConfig();

	/**
	 * The order of this list determines the order of the generated result urls in a query status.
	 */
	@Valid
	@NotNull
	private List<ResultRendererProvider> resultProviders = List.of(
			new ExcelResultProvider(),
			new CsvResultProvider(),
			new ArrowResultProvider(),
			new ParquetResultProvider(),
			new ExternalResultProvider()
	);
	@Valid
	@NotNull
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
	@Valid
	@NotNull
	private IdColumnConfig idColumns = new IdColumnConfig();

	@NotNull
	@Valid
	private IndexConfig index = new IndexConfig();

	private ConqueryMetricsConfig metricsConfig = new ConqueryMetricsConfig();

	@Valid
	@NotNull
	private AuthenticationConfig authentication = new AuthenticationConfig();

	@Valid
	@NotNull
	private List<AuthenticationRealmFactory> authenticationRealms = List.of(new DevAuthConfig());

	@Valid
	@NotNull
	private AuthorizationConfig authorizationRealms = new DevelopmentAuthorizationConfig();

	@Valid
	@NotNull
	private JerseyClientConfiguration jerseyClient = new JerseyClientConfiguration();

	@Valid
	@NotNull
	private List<PluginConfig> plugins = new ArrayList<>();

	@Valid
	@NotNull
	private SqlConnectorConfig sqlConnectorConfig = new SqlConnectorConfig();

	/**
	 * null means here that we try to deduce from an attached agent
	 */
	private Boolean debugMode = null;

	private boolean failOnError = false;

	@ValidationMethod(message = "Plugins are not unique")
	boolean isPluginUnique() {
		// Normal plugins only exist once per class
		Set<Class<? extends PluginConfig>> singleInstanceClasses = new HashSet<>();
		// MultiInstance plugins have a distinct id among their class
		Multimap<Class<? extends MultiInstancePlugin>, String> multiInstanceClasses = MultimapBuilder.hashKeys().hashSetValues().build();

		for (PluginConfig plugin : plugins) {
			if (plugin instanceof MultiInstancePlugin mu) {
				if (!multiInstanceClasses.put(mu.getClass(), mu.getId())) {
					return false;
				}
				continue;
			}

			if (!singleInstanceClasses.add(plugin.getClass())) {
				return false;
			}
		}
		return true;
	}

	public void initialize(ManagerNode node) {
		plugins.forEach(config -> config.initialize((node)));
	}

	public <T extends PluginConfig> Optional<T> getPluginConfig(Class<T> type) {
		return plugins.stream()
					  .filter(c -> type.isAssignableFrom(c.getClass()))
					  .map(type::cast)
					  .collect(MoreCollectors.toOptional());
	}

	public <T extends PluginConfig> Stream<T> getPluginConfigs(Class<T> type) {
		return plugins.stream()
					  .filter(c -> type.isAssignableFrom(c.getClass()))
					  .filter(c -> MultiInstancePlugin.class.isAssignableFrom(c.getClass()))
					  .map(type::cast);
	}

	public ObjectMapper configureObjectMapper(ObjectMapper objectMapper) {
		return objectMapper.registerModule(new ConqueryConfig.ConfiguredModule(this));
	}

	@Override
	public MutableInjectableValues inject(MutableInjectableValues values) {
		return values.add(ConqueryConfig.class, this);
	}

	public static class ConfiguredModule extends SimpleModule {
		public ConfiguredModule(ConqueryConfig config) {
			DateReader dateReader = config.getLocale().getDateReader();
			addDeserializer(LocalDate.class, new FormatedDateDeserializer(dateReader));

			addDeserializer(CDateSet.class, new CDateSetDeserializer(dateReader));
			addSerializer(CDateSet.class, new CDateSetSerializer());
		}
	}

}
