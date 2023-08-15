package com.bakdata.conquery.io.external.form;

import java.net.URI;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.bakdata.conquery.apiv1.forms.ExternalForm;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.auth.permissions.Ability;
import com.bakdata.conquery.models.config.auth.AuthenticationClientFilterProvider;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.i18n.I18n;
import com.codahale.metrics.health.HealthCheck;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.dropwizard.health.check.http.HttpHealthCheck;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ExternalFormBackendApi {

	public final static String TASK_ID = "task-id";
	// Custom headers for form post
	private static final String HTTP_HEADER_CQ_API_URL = "X-CQ-Api-Url";
	private static final String HTTP_HEADER_CQ_AUTHENTICATION = "X-CQ-Authentication";
	private static final String HTTP_HEADER_CQ_AUTHENTICATION_ORIGINAL = "X-CQ-Authentication-Original";
	// Custom query-params for form post
	private static final String QUERY_SCOPE = "scope";
	private static final String QUERY_DATASET = "dataset";
	private final Client client;
	private final WebTarget formConfigTarget;
	private final WebTarget postFormTarget;
	private final WebTarget getStatusTarget;
	private final WebTarget cancelTaskTarget;
	private final WebTarget getHealthTarget;
	private final Function<User, String> tokenCreator;
	private final WebTarget baseTarget;
	private final URL conqueryApiUrl;

	public ExternalFormBackendApi(Client client, URI baseURI, String formConfigPath, String postFormPath, String statusTemplatePath, String cancelTaskPath, String healthCheckPath, Function<User, String> tokenCreator, URL conqueryApiUrl, AuthenticationClientFilterProvider authFilterProvider) {

		this.client = client;
		this.tokenCreator = tokenCreator;
		this.conqueryApiUrl = conqueryApiUrl;

		client.register(authFilterProvider.getFilter());

		baseTarget = this.client.target(baseURI);

		formConfigTarget = baseTarget.path(formConfigPath);

		postFormTarget = baseTarget.path(postFormPath);

		getStatusTarget = baseTarget.path(statusTemplatePath);
		cancelTaskTarget = baseTarget.path(cancelTaskPath);

		getHealthTarget = baseTarget.path(healthCheckPath);
	}

	public List<ObjectNode> getFormConfigs() {
		log.debug("Getting form configurations from: {}", formConfigTarget);

		return formConfigTarget.request(MediaType.APPLICATION_JSON_TYPE)
							   .acceptLanguage(I18n.LOCALE.get())
							   .buildGet().invoke(new GenericType<>() {
				});
	}

	public ExternalTaskState postForm(ExternalForm form, User originalUser, User serviceUser, Dataset dataset) {
		log.debug("Posting form to: {}", postFormTarget);

		// Set headers
		WebTarget webTarget = postFormTarget.queryParam(QUERY_DATASET, dataset.getId());


		// If user is not allowed to download, only provide them with statistics.

		final DatasetDetail detail = originalUser.isPermitted(dataset, Ability.DOWNLOAD) ? DatasetDetail.FULL : DatasetDetail.STATISTIC;
		webTarget = webTarget.queryParam(QUERY_SCOPE, detail);

		final Invocation.Builder request = webTarget.request(MediaType.APPLICATION_JSON_TYPE);

		// Set Headers
		final String serviceUserToken = tokenCreator.apply(serviceUser);
		final String originalUserToken = tokenCreator.apply(originalUser);

		request.header(HTTP_HEADER_CQ_API_URL, conqueryApiUrl)
			   .header(HTTP_HEADER_CQ_AUTHENTICATION, serviceUserToken)
			   .header(HTTP_HEADER_CQ_AUTHENTICATION_ORIGINAL, originalUserToken)
			   .acceptLanguage(I18n.LOCALE.get())
		;

		ExternalTaskState post = request.post(Entity.entity(form.getExternalApiPayload(), MediaType.APPLICATION_JSON_TYPE), ExternalTaskState.class);
		return post;
	}

	public ExternalTaskState getFormState(UUID externalId) {

		final WebTarget getStatusTargetResolved = getStatusTarget.resolveTemplate(TASK_ID, externalId);
		log.debug("Getting status from: {}", getStatusTargetResolved);

		return getStatusTargetResolved.request(MediaType.APPLICATION_JSON_TYPE)
									  .acceptLanguage(I18n.LOCALE.get())
									  .get(ExternalTaskState.class);
	}

	public Response getResult(final URI resultURL) {
		log.debug("Query external form result from {}", resultURL);

		return client.target(baseTarget.getUri().resolve(resultURL)).request()
					 .acceptLanguage(I18n.LOCALE.get())
					 .get();

	}

	public HealthCheck createHealthCheck() {
		return new HttpHealthCheck(getHealthTarget.getUri().toString(), client);
	}

	public ExternalTaskState cancelTask(UUID taskId) {
		log.debug("Cancelling task {}", taskId);

		final ExternalTaskState taskState = cancelTaskTarget.resolveTemplates(Map.of(TASK_ID, taskId))
															.request()
															.post(null, ExternalTaskState.class);

		if (taskState.getStatus() != TaskStatus.CANCELLED) {
			log.warn("Task `{}` was cancelled, but is still in state {}", taskId, taskState.getStatus());
		}

		return taskState;
	}
}
