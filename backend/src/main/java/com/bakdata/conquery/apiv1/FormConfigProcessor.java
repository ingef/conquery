package com.bakdata.conquery.apiv1;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.bakdata.conquery.apiv1.forms.FormConfig;
import com.bakdata.conquery.apiv1.forms.FormConfig.FormConfigFullRepresentation;
import com.bakdata.conquery.apiv1.forms.FormConfig.FormConfigOverviewRepresentation;
import com.bakdata.conquery.io.jackson.Jackson;
import com.bakdata.conquery.io.xodus.MasterMetaStorage;
import com.bakdata.conquery.models.auth.AuthorizationHelper;
import com.bakdata.conquery.models.auth.entities.Group;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.auth.permissions.Ability;
import com.bakdata.conquery.models.auth.permissions.AbilitySets;
import com.bakdata.conquery.models.auth.permissions.FormConfigPermission;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.identifiable.ids.specific.FormConfigId;
import com.bakdata.conquery.resources.api.StoredQueriesResource.QueryPatch;
import com.bakdata.conquery.util.QueryUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.NonNull;
import org.apache.commons.lang3.StringUtils;

public class FormConfigProcessor {
	
	private MasterMetaStorage storage;
	private final static ObjectMapper MAPPER = Jackson.MAPPER.copy();
	
	public Stream<FormConfigOverviewRepresentation> getConfigsByFormType(@NonNull User user, String formType){
		Stream<FormConfig> stream = storage.getAllFormConfigs().stream()
			.filter(c -> user.isPermitted(FormConfigPermission.onInstance(Ability.READ, c.getId())));
		if(!StringUtils.isEmpty(formType)) {
			stream = stream.filter(c -> c.getFormType().equals(formType));
		}
		
		return stream.map(c -> c.overview(storage.getNamespaces(), user));	
	}

	public  FormConfigFullRepresentation getConfig(DatasetId datasetId, User user, FormConfigId formId) {
		user.checkPermission(FormConfigPermission.onInstance(Ability.READ, formId));
		FormConfigFullRepresentation config = Objects.requireNonNull(storage.getFormConfig(formId), String.format("Could not find form config %s", formId))
			.tryTranslateToDataset(storage.getNamespaces(), datasetId, MAPPER, user);
		return config;
	}

	public FormConfigId addConfig(User user, FormConfig config) {
		storage.updateFormConfig(config);
		
		user.addPermission(storage, FormConfigPermission.onInstance(AbilitySets.FORM_CONFIG_CREATOR, config.getId()));
		
		return config.getId();
	}

	public FormConfigFullRepresentation patchConfig(User user, DatasetId target, FormConfigId formId, QueryPatch patch) {
		FormConfig config = Objects.requireNonNull(storage.getFormConfig(formId), String.format("Could not find form config %s", formId));
		
		if(patch.getTags() != null && user.isPermitted(FormConfigPermission.onInstance(Ability.TAG, formId))) {
			config.setTags(patch.getTags());
		}
		if(patch.getLabel() != null) {
			QueryUtils.updateInstance(
				config,
				user,
				storage::updateFormConfig,
				(c) -> user.isPermitted(FormConfigPermission.onInstance(Ability.LABEL, c.getId())),
				(c) -> c.setLabel(patch.getLabel()));
		}
		if(patch.getShared() != null && user.isPermitted(FormConfigPermission.onInstance(Ability.SHARE, formId))) {
			List<Group> groups;
			if(patch.getGroups() != null) {
				groups = patch.getGroups().stream().map(id -> storage.getGroup(id)).collect(Collectors.toList());
			}
			else {				
				groups = AuthorizationHelper.getGroupsOf(user, storage);
			}
			for(Group group : groups) {				
				QueryUtils.shareWithGroup(
					storage,
					user,
					config,
					storage::updateFormConfig,
					(c) -> user.isPermitted(FormConfigPermission.onInstance(Ability.SHARE, c.getId())),
					(c) -> FormConfigPermission.onInstance(Ability.READ, c.getId()), 
					group,
					patch.getShared());
			}
			config.setLabel(patch.getLabel());
		}
		
		return config.tryTranslateToDataset(storage.getNamespaces(), target, MAPPER, user);
	}

}
