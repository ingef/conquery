package com.bakdata.conquery.models.forms.frontendconfiguration;

import java.time.Instant;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.validation.Validator;

import com.bakdata.conquery.apiv1.FormConfigPatch;
import com.bakdata.conquery.apiv1.forms.FormConfigAPI;
import com.bakdata.conquery.io.jackson.Jackson;
import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.models.auth.AuthorizationHelper;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.auth.permissions.Ability;
import com.bakdata.conquery.models.auth.permissions.ConqueryPermission;
import com.bakdata.conquery.models.auth.permissions.FormConfigPermission;
import com.bakdata.conquery.models.auth.permissions.WildcardPermission;
import com.bakdata.conquery.models.exceptions.ValidatorHelper;
import com.bakdata.conquery.models.forms.configs.FormConfig;
import com.bakdata.conquery.models.forms.configs.FormConfig.FormConfigFullRepresentation;
import com.bakdata.conquery.models.forms.configs.FormConfig.FormConfigOverviewRepresentation;
import com.bakdata.conquery.models.identifiable.Identifiable;
import com.bakdata.conquery.models.identifiable.ids.NamespacedId;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.identifiable.ids.specific.FormConfigId;
import com.bakdata.conquery.models.worker.Namespace;
import com.bakdata.conquery.resources.api.FormConfigResource;
import com.bakdata.conquery.util.ResourceUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.TestOnly;

/**
 * Holds the logic that serves the endpoints defined in {@link FormConfigResource}.
 */
@RequiredArgsConstructor
@Slf4j
public class FormConfigProcessor {
	
	private final Validator validator;
	private final MetaStorage storage;
	@Getter(onMethod = @__({@TestOnly}))
	private final static ObjectMapper MAPPER = Jackson.MAPPER.copy().disable(SerializationFeature.WRITE_EMPTY_JSON_ARRAYS, SerializationFeature.WRITE_NULL_MAP_VALUES);;
	
	/**
	 * Return an overview of all form config available to the user. The selection can be reduced by setting a specific formType.
	 * The provided overview does not contain the configured values for the form, just the meta data.
	 * @param user The user vor which the overview is created.
	 * @param dataset 
	 * @param requestedFormType Optional form type to filter the overview to that specific type.
	 **/
	public Stream<FormConfigOverviewRepresentation> getConfigsByFormType(@NonNull User user, @NonNull DatasetId dataset, @NonNull Set<String> requestedFormType) {

		if (requestedFormType.isEmpty()) {
			// If no specific form type is provided, show all types the user is permitted to create.
			// However if a user queries for specific form types, we will show all matching regardless whether
			// the form config can be used by the user again.
			Set<String> allowedFormTypes = new HashSet<>();

			for (FormType formType : FormScanner.FRONTEND_FORM_CONFIGS.values()) {
				if (!user.isPermitted(formType, Ability.CREATE)) {
					continue;
				}

				allowedFormTypes.add(formType.getName());
			}
			requestedFormType = allowedFormTypes;
		}

		final Set<String> formTypesFinal = requestedFormType;

		Stream<FormConfig> stream = storage.getAllFormConfigs().stream()
										   .filter(c -> dataset.equals(c.getDataset()))
										   .filter(c -> formTypesFinal.contains(c.getFormType()))
										   .filter(c -> user.isPermitted(c, Ability.READ));


		return stream.map(c -> c.overview(storage, user));
	}

	/**
	 * Returns the full configuration of a configuration (meta data + configured values).
	 * It also tried to convert all {@link NamespacedId}s into the given dataset, so that the frontend can resolve them.
	 */
	public FormConfigFullRepresentation getConfig(DatasetId datasetId, User user, FormConfigId formId) {
		FormConfig form = storage.getFormConfig(formId);

		ResourceUtil.throwNotFoundIfNull(formId, form);

		user.authorize(form,Ability.READ);
		return form.fullRepresentation(storage, user);
	}

	/**
	 * Adds the provided config to the desired dataset and the datasets that the
	 * user has access to (has the READ ability on the Dataset), if the config is
	 * translatable to those.
	 */
	public FormConfigId addConfig(User user, DatasetId targetDataset, FormConfigAPI config) {

		//TODO clear this up
		final Namespace namespace = storage.getDatasetRegistry().get(targetDataset);

		user.authorize(namespace.getDataset(), Ability.READ);

		List<DatasetId> translateToDatasets = storage.getDatasetRegistry().getAllDatasets()
													 .stream()
													 .filter(dId -> user.isPermitted(dId, Ability.READ))
													 .map(Identifiable::getId)
													 .collect(Collectors.toList());

		translateToDatasets.remove(targetDataset);

		return addConfigAndTranslations(user, targetDataset, translateToDatasets, config);
	}
	
	/**
	 * Adds the config to the dataset it was submitted under and also to all other datasets it can be translated to.
	 * This method does not check permissions.
	 */
	public FormConfigId addConfigAndTranslations(User user, DatasetId targetDataset, Collection<DatasetId> translateTo, FormConfigAPI config) {
		FormConfig internalConfig = FormConfigAPI.intern(config, user.getId(), targetDataset);
		// Add the config immediately to the submitted dataset
		addConfigToDataset(internalConfig);

		return internalConfig.getId();
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
	public FormConfigFullRepresentation patchConfig(User user, DatasetId target, FormConfigId formId, FormConfigPatch patch) {
		FormConfig config = storage.getFormConfig(formId);

		ResourceUtil.throwNotFoundIfNull(formId, config);

		patch.applyTo(config, storage, user);
		
		storage.updateFormConfig(config);
		
		return config.fullRepresentation(storage, user);
	}

	/**
	 * Deletes a configuration from the storage and all permissions, that have this configuration as target.
	 */
	public void deleteConfig(User user, FormConfigId formId) {
		FormConfig config = storage.getFormConfig(formId);

		ResourceUtil.throwNotFoundIfNull(formId, config);
		user.authorize( config, Ability.DELETE);
		storage.removeFormConfig(formId);
		// Delete corresponding permissions (Maybe better to put it into a slow job)
		for(ConqueryPermission permission : user.getPermissions()) {

			WildcardPermission wpermission = (WildcardPermission) permission;

			if(!wpermission.getDomains().contains(FormConfigPermission.DOMAIN.toLowerCase())) {
				continue;
			}
			if(!wpermission.getInstances().contains(formId.toString().toLowerCase())) {
				continue;
			}
			
			if(!wpermission.getInstances().isEmpty()) {
				// Create new permission if it was a composite permission
				Set<String> instancesCleared = new HashSet<>(wpermission.getInstances());
				instancesCleared.remove(formId.toString());
				WildcardPermission clearedPermission =
						new WildcardPermission(List.of(wpermission.getDomains(), wpermission.getAbilities(), instancesCleared), Instant.now());
				user.addPermission(storage, clearedPermission);
			}
			
			user.removePermission(storage, wpermission);
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
