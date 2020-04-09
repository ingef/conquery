package com.bakdata.conquery.apiv1;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import com.bakdata.conquery.apiv1.forms.FormConfig;
import com.bakdata.conquery.apiv1.forms.FormConfig.FormConfigFullRepresentation;
import com.bakdata.conquery.apiv1.forms.FormConfig.FormConfigOverviewRepresentation;
import com.bakdata.conquery.io.jackson.Jackson;
import com.bakdata.conquery.io.xodus.MasterMetaStorage;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.auth.permissions.Ability;
import com.bakdata.conquery.models.auth.permissions.AbilitySets;
import com.bakdata.conquery.models.auth.permissions.FormConfigPermission;
import com.bakdata.conquery.models.auth.permissions.WildcardPermission;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.identifiable.ids.specific.FormConfigId;
import com.bakdata.conquery.resources.api.StoredQueriesResource.QueryPatch;
import com.bakdata.conquery.util.QueryUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.apache.shiro.authz.Permission;
import org.jetbrains.annotations.TestOnly;

@RequiredArgsConstructor
public class FormConfigProcessor {
	
	private final MasterMetaStorage storage;
	@Getter(onMethod = @__({@TestOnly}))
	private final static ObjectMapper MAPPER = Jackson.MAPPER.copy().disable(SerializationFeature.WRITE_EMPTY_JSON_ARRAYS, SerializationFeature.WRITE_NULL_MAP_VALUES);;
	
	public Stream<FormConfigOverviewRepresentation> getConfigsByFormType(@NonNull User user, @NonNull Optional<String> formType){
		Stream<FormConfig> stream = storage.getAllFormConfigs().stream()
			.filter(c -> user.isPermitted(FormConfigPermission.onInstance(Ability.READ, c.getId())));
		if(formType.isPresent()) {
			stream = stream.filter(c -> c.getFormType().equals(formType.get()));
		}
		
		return stream.map(c -> c.overview(storage, user));	
	}

	public  FormConfigFullRepresentation getConfig(DatasetId datasetId, User user, FormConfigId formId) {
		user.checkPermission(FormConfigPermission.onInstance(Ability.READ, formId));
		FormConfigFullRepresentation config = Objects.requireNonNull(storage.getFormConfig(formId), String.format("Could not find form config %s", formId))
			.tryTranslateToDataset(storage, datasetId, MAPPER, user);
		return config;
	}

	public FormConfigId addConfig(User user, FormConfig config) {
		config.setOwner(user.getId());
		storage.updateFormConfig(config);
		
		user.addPermission(storage, FormConfigPermission.onInstance(AbilitySets.FORM_CONFIG_CREATOR, config.getId()));
		
		return config.getId();
	}

	public FormConfigFullRepresentation patchConfig(User user, DatasetId target, FormConfigId formId, QueryPatch patch) {
		FormConfig config = Objects.requireNonNull(storage.getFormConfig(formId), String.format("Could not find form config %s", formId));
		
		QueryUtils.patchIdentifialble(storage, user, config, patch, (ability) -> FormConfigPermission.onInstance(ability, config.getId()));
		
		storage.updateFormConfig(config);
		
		return config.tryTranslateToDataset(storage, target, MAPPER, user);
	}

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
			
			if(wpermission.getInstances().size()>1) {
				// Create new permission if it was a composite permission
				Set<String> instancesCleared = new HashSet<>(wpermission.getInstances());
				instancesCleared.remove(formId.toString());
				WildcardPermission clearedPermission = new WildcardPermission(List.of(wpermission.getDomains(),wpermission.getAbilities(),instancesCleared), Instant.now());
				user.addPermission(storage, clearedPermission);
			}
			
			user.removePermission(storage, wpermission);
		}
	}

}
