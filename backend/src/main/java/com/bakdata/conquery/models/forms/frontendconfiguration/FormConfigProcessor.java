package com.bakdata.conquery.models.forms.frontendconfiguration;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.validation.Validator;

import com.bakdata.conquery.apiv1.FormConfigPatch;
import com.bakdata.conquery.apiv1.forms.FormConfigAPI;
import com.bakdata.conquery.io.jackson.Jackson;
import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.models.auth.entities.Subject;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.auth.permissions.Ability;
import com.bakdata.conquery.models.auth.permissions.ConqueryPermission;
import com.bakdata.conquery.models.auth.permissions.FormConfigPermission;
import com.bakdata.conquery.models.auth.permissions.WildcardPermission;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.exceptions.ValidatorHelper;
import com.bakdata.conquery.models.forms.configs.FormConfig;
import com.bakdata.conquery.models.forms.configs.FormConfig.FormConfigFullRepresentation;
import com.bakdata.conquery.models.forms.configs.FormConfig.FormConfigOverviewRepresentation;
import com.bakdata.conquery.models.identifiable.ids.NamespacedId;
import com.bakdata.conquery.models.identifiable.ids.specific.FormConfigId;
import com.bakdata.conquery.models.worker.DatasetRegistry;
import com.bakdata.conquery.models.worker.Namespace;
import com.bakdata.conquery.resources.api.FormConfigResource;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.TestOnly;

/**
 * Holds the logic that serves the endpoints defined in {@link FormConfigResource}.
 */
@Slf4j
@NoArgsConstructor
@AllArgsConstructor
public class FormConfigProcessor {

	@Getter(onMethod = @__({@TestOnly}))
	private static final ObjectMapper
			MAPPER =
			Jackson.MAPPER.copy().disable(SerializationFeature.WRITE_EMPTY_JSON_ARRAYS, SerializationFeature.WRITE_NULL_MAP_VALUES);
	@Inject
	private Validator validator;
	@Inject
	private MetaStorage storage;
	@Inject
	private DatasetRegistry datasetRegistry;

	/**
	 * Return an overview of all form config available to the subject. The selection can be reduced by setting a specific formType.
	 * The provided overview does not contain the configured values for the form, just the meta data.
	 *
	 * @param subject           The subject vor which the overview is created.
	 * @param dataset
	 * @param requestedFormType Optional form type to filter the overview to that specific type.
	 **/
	public Stream<FormConfigOverviewRepresentation> getConfigsByFormType(@NonNull Subject subject, Dataset dataset, @NonNull Set<String> requestedFormType) {

		if (requestedFormType.isEmpty()) {
			// If no specific form type is provided, show all types the subject is permitted to create.
			// However if a subject queries for specific form types, we will show all matching regardless whether
			// the form config can be used by the subject again.
			final Set<String> allowedFormTypes = new HashSet<>();

			for (FormType formType : FormScanner.FRONTEND_FORM_CONFIGS.values()) {
				if (!subject.isPermitted(formType, Ability.CREATE)) {
					continue;
				}

				allowedFormTypes.add(formType.getName());
			}
			requestedFormType = allowedFormTypes;
		}

		final Set<String> formTypesFinal = requestedFormType;

		final Stream<FormConfig> stream = storage.getAllFormConfigs().stream()
												 .filter(c -> dataset.equals(c.getDataset()))
												 .filter(c -> formTypesFinal.contains(c.getFormType()))
												 .filter(c -> subject.isPermitted(c, Ability.READ));


		return stream.map(c -> c.overview(subject));
	}

	/**
	 * Returns the full configuration of a configuration (meta data + configured values).
	 * It also tried to convert all {@link NamespacedId}s into the given dataset, so that the frontend can resolve them.
	 */
	public FormConfigFullRepresentation getConfig(Subject subject, FormConfig form) {

		subject.authorize(form, Ability.READ);
		return form.fullRepresentation(storage, subject);
	}

	/**
	 * Adds the provided config to the desired dataset and the datasets that the
	 * subject has access to (has the READ ability on the Dataset), if the config is
	 * translatable to those.
	 */
	public FormConfig addConfig(Subject subject, Dataset targetDataset, FormConfigAPI config) {

		//TODO clear this up
		final Namespace namespace = datasetRegistry.get(targetDataset.getId());

		subject.authorize(namespace.getDataset(), Ability.READ);

		final FormConfig internalConfig = config.intern(storage.getUser(subject.getId()), targetDataset);
		// Add the config immediately to the submitted dataset
		addConfigToDataset(internalConfig);

		return internalConfig;
	}

	/**
	 * Adds a formular configuration under a specific dataset to the storage and grants the user the rights to manage/patch it.
	 */
	private FormConfigId addConfigToDataset(FormConfig internalConfig) {

		ValidatorHelper.failOnError(log, validator.validate(internalConfig));
		storage.addFormConfig(internalConfig);

		return internalConfig.getId();
	}

	/**
	 * Applies a patch to a configuration that allows to change its label or tags or even share it.
	 */
	public FormConfigFullRepresentation patchConfig(Subject subject, FormConfig config, FormConfigPatch patch) {

		patch.applyTo(config, storage, subject);

		storage.updateFormConfig(config);

		return config.fullRepresentation(storage, subject);
	}

	/**
	 * Deletes a configuration from the storage and all permissions, that have this configuration as target.
	 */
	public void deleteConfig(Subject subject, FormConfig config) {
		final User user = storage.getUser(subject.getId());

		user.authorize(config, Ability.DELETE);
		storage.removeFormConfig(config.getId());
		// Delete corresponding permissions (Maybe better to put it into a slow job)
		for (ConqueryPermission permission : user.getPermissions()) {

			final WildcardPermission wpermission = (WildcardPermission) permission;

			if (!wpermission.getDomains().contains(FormConfigPermission.DOMAIN.toLowerCase())) {
				continue;
			}
			if (!wpermission.getInstances().contains(config.getId().toString().toLowerCase())) {
				continue;
			}

			if (!wpermission.getInstances().isEmpty()) {
				// Create new permission if it was a composite permission
				final Set<String> instancesCleared = new HashSet<>(wpermission.getInstances());
				instancesCleared.remove(config.getId().toString());
				final WildcardPermission clearedPermission =
						new WildcardPermission(List.of(wpermission.getDomains(), wpermission.getAbilities(), instancesCleared), Instant.now());
				user.addPermission(clearedPermission);
			}

			user.removePermission(wpermission);
		}
	}

	/**
	 * Simple container so that the frontend receives an JSON object instead of a string.
	 */
	@Data
	@AllArgsConstructor
	public static class PostResponse {
		private FormConfigId id;
	}
}
