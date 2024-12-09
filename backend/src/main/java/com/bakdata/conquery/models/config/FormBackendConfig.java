package com.bakdata.conquery.models.config;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import javax.annotation.Nullable;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.ws.rs.client.Client;

import com.bakdata.conquery.apiv1.forms.ExternalForm;
import com.bakdata.conquery.apiv1.frontend.VersionContainer;
import com.bakdata.conquery.commands.ManagerNode;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.cps.CPSTypeIdResolver;
import com.bakdata.conquery.io.external.form.ExternalFormBackendApi;
import com.bakdata.conquery.io.external.form.ExternalFormMixin;
import com.bakdata.conquery.io.external.form.FormBackendVersion;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.auth.permissions.Ability;
import com.bakdata.conquery.models.config.auth.AuthenticationClientFilterProvider;
import com.bakdata.conquery.models.config.auth.MultiInstancePlugin;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.forms.frontendconfiguration.FormConfigProvider;
import com.bakdata.conquery.models.forms.frontendconfiguration.FormFrontendConfigInformation;
import com.bakdata.conquery.util.VersionInfo;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.common.collect.ImmutableCollection;
import io.dropwizard.client.ConfiguredCloseableHttpClient;
import io.dropwizard.client.DropwizardApacheConnector;
import io.dropwizard.client.JerseyClientBuilder;
import io.dropwizard.core.setup.Environment;
import io.dropwizard.jersey.jackson.JacksonMessageBodyProvider;
import lombok.Data;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

/**
 * {@link PluginConfig} for an external form backend.
 * The external form backend must implement the <a href="https://github.com/ingef/conquery/tree/develop/backend/src/main/resources/com/bakdata/conquery/external/openapi-form-backend.yaml">OpenAPI spec</a> for external form backend.
 */
@Data
@CPSType(id = "FORM_BACKEND", base = PluginConfig.class)
@Slf4j
@ToString(onlyExplicitlyIncluded = true)
public class FormBackendConfig implements PluginConfig, MultiInstancePlugin {

	@NotEmpty
	@ToString.Include
	private String id;

	@NotNull
	@ToString.Include
	private URI baseURI;

	@NotEmpty
	private String formConfigPath = "form-config";

	@NotEmpty
	private String postFormPath = "task";

	@NotEmpty
	@Pattern(regexp = ".+/\\{" + ExternalFormBackendApi.TASK_ID + "}")
	private String statusTemplatePath = "task/{" + ExternalFormBackendApi.TASK_ID + "}";

	private String cancelTaskPath = "task/{" + ExternalFormBackendApi.TASK_ID + "}/cancel";


	@NotEmpty
	private String healthCheckPath = "health";

	@NotEmpty
	private String versionPath = "version";

	@NotNull
	private URL conqueryApiUrl;

	@Valid
	private AuthenticationClientFilterProvider authentication;

	@JsonIgnore
	private ManagerNode managerNode;

	@JsonIgnore
	private Client client;

	/**
	 * @implNote HACK to close client AND remove its metrics
	 */
	@JsonIgnore
	private ConqueryClientBuilder conqueryClientBuilder;

	@JsonIgnore
	private Set<String> supportedFormTypes = Collections.emptySet();

	@Override
	public void initialize(ManagerNode managerNode) {
		this.managerNode = managerNode;

		// Setup client
		conqueryClientBuilder = new ConqueryClientBuilder(managerNode.getEnvironment());
		client = conqueryClientBuilder.using(managerNode.getConfig().getJerseyClient()).build(getId());



		final ObjectMapper om = configureObjectMapper(managerNode.getEnvironment().getObjectMapper().copy());
		client.register(new JacksonMessageBodyProvider(om));

		// Register health check
		final ExternalFormBackendApi externalApi = createApi();

		managerNode.getEnvironment().healthChecks().register(getId(), externalApi.createHealthCheck());

		// Register form configuration provider
		log.info("Registering frontend form config provider for '{}'", getId());
		managerNode.getFormScanner().registerFrontendFormConfigProvider(new FormConfigProvider(getId(), this::registerFormConfigs));
	}

	public static ObjectMapper configureObjectMapper(ObjectMapper om) {
		return om.addMixIn(ExternalForm.class, ExternalFormMixin.class);
	}

	public ExternalFormBackendApi createApi() {
		return new ExternalFormBackendApi(client, baseURI, formConfigPath, postFormPath, statusTemplatePath, cancelTaskPath, healthCheckPath, versionPath, this::createAccessToken, conqueryApiUrl, getAuthentication());
	}

	/**
	 * This registers the form configs of the external form backend and overrides their type id, so that the produced
	 * forms are mapped to {@link ExternalForm}. The overriding adds a special prefix to the form backends type id.
	 * This prefix is transparent to the form backend, as it is removed, when the form is forwarded to it (see {@link ExternalFormMixin.Serializer})
	 *
	 * @param formConfigs Collection to add received form configs to.
	 */
	private void registerFormConfigs(ImmutableCollection.Builder<FormFrontendConfigInformation> formConfigs) {
		final Set<String> supportedFormTypes = new HashSet<>();

		final ExternalFormBackendApi api = createApi();
		for (ObjectNode formConfig : api.getFormConfigs()) {
			final String subType = formConfig.get("type").asText();
			final String formType = createSubTypedId(subType);

			// Override type with our subtype
			formConfig.set("type", new TextNode(formType));
			formConfigs.add(new FormFrontendConfigInformation(baseURI.toString(), formConfig));

			// register as supported FormType
			supportedFormTypes.add(formType);
		}

		this.supportedFormTypes = supportedFormTypes;

		// Update version
		updateVersion(createApi());
	}

	private String createAccessToken(User user) {
		return managerNode.getAuthController().getConqueryTokenRealm().createTokenForUser(user.getId());
	}

	/**
	 * Prepends the type of {@link ExternalForm} so that, received form queries are mapped to that class
	 *
	 * @param subType the type that will be the subtype of {@link ExternalForm}
	 */
	public static String createSubTypedId(String subType) {
		return String.format("%s%s%s", ExternalForm.class.getAnnotation(CPSType.class).id(), CPSTypeIdResolver.SEPARATOR_SUB_TYPE, subType);
	}

	/**
	 * Retrieves the version information from the form backend and writes it to the {@link VersionInfo}
	 */
	private void updateVersion(ExternalFormBackendApi externalApi) {

		try {
			final FormBackendVersion versionInfo = externalApi.getVersion();
			final VersionContainer
					oldVersion =
					VersionInfo.INSTANCE.setFormBackendVersion(new VersionContainer(getId(), versionInfo.version(), versionInfo.buildTime()));
			if (!versionInfo.version().equals(oldVersion.version())) {
				log.info("Form Backend '{}' versionInfo update: {} -> {}", getId(), oldVersion, versionInfo);
			}
		}
		catch (Exception e) {
			log.warn("Unable to retrieve version from form backend '{}'. Enable trace logging for more info", getId(), (Exception) (log.isTraceEnabled()
																																	? e
																																	: null));

			VersionInfo.INSTANCE.setFormBackendVersion(new VersionContainer(getId(), null, null));
		}

	}

	@Override
	public void close() {
		log.info("Unregister {}", this);
		managerNode.getEnvironment().healthChecks().unregister(getId());
		managerNode.getFormScanner().unregisterFrontendFormConfigProvider(getId());
		client.close();

		try {
			// Workaround to remove metrics (see https://github.com/dropwizard/dropwizard/issues/9612)
			ConfiguredCloseableHttpClient internalHttpClient = conqueryClientBuilder.getHttpClient();
			if (internalHttpClient != null) {
				internalHttpClient.getClient().close();
			}
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public boolean supportsFormType(String formType) {
		return supportedFormTypes.contains(formType);
	}

	public User createServiceUser(User originalUser, Dataset dataset) {
		log.debug("Creating service user and calculating scopes per dataset");
		// We create a service user with elevated permissions (allow downloads, which a form backend relies on) so we don't
		// have to give the permission to the actual user. The service user will have a snapshot of the permissions of
		// the actual user and download permissions.
		final User
				serviceUser =
				managerNode.getAuthController().flatCopyUser(originalUser, String.format("%s_%s", getClass().getSimpleName().toLowerCase(), getId()));

		// The user is able to read the dataset, ensure that the service user can download results
		serviceUser.addPermission(dataset.createPermission(Ability.DOWNLOAD.asSet()));
		return serviceUser;
	}

	/**
	 * This is the way to internal httpClient and close it and thereby remove the metrics of this client.
	 */
	@Getter
	private static class ConqueryClientBuilder extends JerseyClientBuilder {

		// Is set upon the first request made by the client
		@Nullable
		private ConfiguredCloseableHttpClient httpClient;

		public ConqueryClientBuilder(Environment environment) {
			super(environment);
		}

		@Override
		protected DropwizardApacheConnector createDropwizardApacheConnector(ConfiguredCloseableHttpClient configuredClient) {
			httpClient = configuredClient;
			return new DropwizardApacheConnector(configuredClient.getClient(), configuredClient.getDefaultRequestConfig(),
												 true);
		}
	}
}
