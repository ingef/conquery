package com.bakdata.conquery.models.forms.frontendconfiguration;

import java.time.Instant;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.validation.Validator;

import com.bakdata.conquery.apiv1.FormConfigPatch;
import com.bakdata.conquery.apiv1.forms.FormConfigAPI;
import com.bakdata.conquery.io.jackson.Jackson;
import com.bakdata.conquery.io.xodus.MetaStorage;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.auth.permissions.Ability;
import com.bakdata.conquery.models.auth.permissions.AbilitySets;
import com.bakdata.conquery.models.auth.permissions.DatasetPermission;
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
import com.bakdata.conquery.resources.api.FormConfigResource;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.authz.Permission;
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
	 * @param formType Optional form type to filter the overview to that specific type.
	 **/
	public Stream<FormConfigOverviewRepresentation> getConfigsByFormType(@NonNull User user, @NonNull DatasetId dataset, @NonNull Optional<String> formType){
		Stream<FormConfig> stream = storage.getAllFormConfigs().stream()
			.filter(c -> dataset.equals(c.getDataset()))
			.filter(c -> user.isPermitted(FormConfigPermission.onInstance(Ability.READ, c.getId())));
		if(formType.isPresent()) {
			stream = stream.filter(c -> c.getFormType().equals(formType.get()));
		}
		
		return stream.map(c -> c.overview(storage, user));	
	}

	/**
	 * Returns the full configuration of a configuration (meta data + configured values).
	 * It also tried to convert all {@link NamespacedId}s into the given dataset, so that the frontend can resolve them.
	 */
	public FormConfigFullRepresentation getConfig(DatasetId datasetId, User user, FormConfigId formId) {
		user.checkPermission(FormConfigPermission.onInstance(Ability.READ, formId));
		return Objects.requireNonNull(storage.getFormConfig(formId), String.format("Could not find form config %s", formId))
			.fullRepresentation(storage, user);
	}
	
	/**
	 * Adds the provided config to the desired dataset and the datasets that the
	 * user has access to (has the READ ability on the Dataset), if the config is
	 * translatable to those.
	 */
	public FormConfigId addConfig(User user, DatasetId targetDataset, FormConfigAPI config) {
		user.checkPermission(DatasetPermission.onInstance(Ability.READ.asSet(), targetDataset));

		List<DatasetId> translateToDatasets = storage.getDatasetRegistry().getAllDatasets().stream()
			.map(Identifiable::getId)
			.filter(dId -> user.isPermitted(DatasetPermission.onInstance(Ability.READ.asSet(), dId)))
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
		addConfigToDataset(user, internalConfig);

		return internalConfig.getId();
	}

	/**
	 * Adds a formular configuration under a specific dataset to the storage and grants the user the rights to manage/patch it.
	 */
	private FormConfigId addConfigToDataset(User user, FormConfig internalConfig) {
		
		ValidatorHelper.failOnError(log, validator.validate(internalConfig));
		storage.addFormConfig(internalConfig);
		
		user.addPermission(storage, FormConfigPermission.onInstance(AbilitySets.FORM_CONFIG_CREATOR, internalConfig.getId()));
		
		return internalConfig.getId();
	}

	/**
	 * Applies a patch to a configuration that allows to change its label or tags or even share it.
	 */
	public FormConfigFullRepresentation patchConfig(User user, DatasetId target, FormConfigId formId, FormConfigPatch patch) {
		FormConfig config = Objects.requireNonNull(storage.getFormConfig(formId), String.format("Could not find form config %s", formId));
		
		patch.applyTo(config, storage, user, FormConfigPermission::onInstance);
		
		storage.updateFormConfig(config);
		
		return config.fullRepresentation(storage, user);
	}

	/**
	 * Deletes a configuration from the storage and all permissions, that have this configuration as target.
	 */
	public void deleteConfig(User user, FormConfigId formId) {
		user.checkPermission(FormConfigPermission.onInstance(Ability.DELETE, formId));
		storage.removeFormConfig(formId);
		// Delete corresponding permissions (Maybe better to put it into a slow job)
		for(Permission permission : user.getPermissions()) {
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
				WildcardPermission clearedPermission = new WildcardPermission(List.of(wpermission.getDomains(),wpermission.getAbilities(),instancesCleared), Instant.now());
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
