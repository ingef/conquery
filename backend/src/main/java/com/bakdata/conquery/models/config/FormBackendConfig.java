package com.bakdata.conquery.models.config;

import java.net.URI;
import java.net.URL;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.ws.rs.client.Client;

import com.bakdata.conquery.apiv1.forms.ExternalForm;
import com.bakdata.conquery.commands.ManagerNode;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.cps.CPSTypeIdResolver;
import com.bakdata.conquery.io.external.form.ExternalFormBackendApi;
import com.bakdata.conquery.io.external.form.ExternalFormMixin;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.auth.permissions.Ability;
import com.bakdata.conquery.models.config.auth.AuthenticationClientFilterProvider;
import com.bakdata.conquery.models.config.auth.MultiInstancePlugin;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.forms.frontendconfiguration.FormFrontendConfigInformation;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.common.collect.ImmutableCollection;
import io.dropwizard.client.JerseyClientBuilder;
import io.dropwizard.jersey.jackson.JacksonMessageBodyProvider;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * {@link PluginConfig} for an external form backend.
 * The external form backend must implement the <a href="https://github.com/ingef/conquery/tree/develop/backend/src/main/resources/com/bakdata/conquery/external/openapi-form-backend.yaml">OpenAPI spec</a> for external form backend.
 */
@NoArgsConstructor
@Setter
@Getter
@CPSType(id = "FORM_BACKEND", base = PluginConfig.class)
@Slf4j
public class FormBackendConfig implements PluginConfig, MultiInstancePlugin {

	@NotEmpty
	private String id;

	@NotNull
	private URI baseURI;

	@NotEmpty
	private String formConfigPath = "form-config";

	@NotEmpty
	private String postFormPath = "task";

	@NotEmpty
	@Pattern(regexp = ".+/\\{" + ExternalFormBackendApi.TASK_ID + "}")
	private String statusTemplatePath = "task/{" + ExternalFormBackendApi.TASK_ID + "}";

	@NotEmpty
	private String healthCheckPath = "health";

	@NotNull
	private URL conqueryApiUrl;

	@Valid
	@NotNull
	private AuthenticationClientFilterProvider authentication;

	@JsonIgnore
	private ManagerNode managerNode;

	@JsonIgnore
	private Client client;

	@JsonIgnore
	private Set<String> supportedFormTypes = Collections.emptySet();

	@Override
	public void initialize(ManagerNode managerNode) {
		this.managerNode = managerNode;

		// Setup client
		client = new JerseyClientBuilder(managerNode.getEnvironment()).using(managerNode.getConfig().getJerseyClient()).build(getId());

		final ObjectMapper om = configureObjectMapper(managerNode.getEnvironment().getObjectMapper().copy());
		client.register(new JacksonMessageBodyProvider(om));

		// Register health check
		final ExternalFormBackendApi externalApi = createApi();

		managerNode.getEnvironment().healthChecks().register(getId(), externalApi.createHealthCheck());

		// Register form configuration provider
		managerNode.getFormScanner().registerFrontendFormConfigProvider(this::registerFormConfigs);
	}


	public static ObjectMapper configureObjectMapper(ObjectMapper om) {
		return om.addMixIn(ExternalForm.class, ExternalFormMixin.class);
	}

	public ExternalFormBackendApi createApi() {
		return new ExternalFormBackendApi(client, baseURI, formConfigPath, postFormPath, statusTemplatePath, healthCheckPath, this::createAccessToken, conqueryApiUrl, getAuthentication());
	}

	public boolean supportsFormType(String formType) {
		return supportedFormTypes.contains(formType);
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

		for (ObjectNode formConfig : createApi().getFormConfigs()) {
			final String subType = formConfig.get("type").asText();
			final String formType = createSubTypedId(subType);

			// Override type with our subtype
			formConfig.set("type", new TextNode(formType));
			formConfigs.add(new FormFrontendConfigInformation(baseURI.toString(), formConfig));

			// register as supported FormType
			supportedFormTypes.add(formType);
		}

		this.supportedFormTypes = supportedFormTypes;
	}

	/**
	 * Prepends the type of {@link ExternalForm} so that, received form queries are mapped to that class
	 *
	 * @param subType the type that will be the subtype of {@link ExternalForm}
	 * @return
	 */
	public static String createSubTypedId(String subType) {
		return String.format("%s%s%s", ExternalForm.class.getAnnotation(CPSType.class).id(), CPSTypeIdResolver.SEPARATOR_SUB_TYPE, subType);
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

	private String createAccessToken(User user) {
		return managerNode.getAuthController().getConqueryTokenRealm().createTokenForUser(user.getId());
	}
}
