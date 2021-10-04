package com.bakdata.conquery.models.forms.configs;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import com.bakdata.conquery.apiv1.FormConfigPatch;
import com.bakdata.conquery.io.jackson.serializer.MetaIdRef;
import com.bakdata.conquery.io.jackson.serializer.NsIdRef;
import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.models.auth.entities.Group;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.auth.entities.Subject;
import com.bakdata.conquery.models.auth.permissions.Ability;
import com.bakdata.conquery.models.auth.permissions.ConqueryPermission;
import com.bakdata.conquery.models.auth.permissions.FormConfigPermission;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.execution.Labelable;
import com.bakdata.conquery.models.execution.Owned;
import com.bakdata.conquery.models.execution.Shareable;
import com.bakdata.conquery.models.execution.Taggable;
import com.bakdata.conquery.models.identifiable.IdentifiableImpl;
import com.bakdata.conquery.models.identifiable.ids.specific.FormConfigId;
import com.bakdata.conquery.models.identifiable.ids.specific.GroupId;
import com.bakdata.conquery.util.VariableDefaultValue;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.ToString;
import lombok.experimental.FieldNameConstants;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.shiro.authz.Permission;

@Slf4j
@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString
@EqualsAndHashCode(callSuper = false)
@FieldNameConstants
public class FormConfig extends IdentifiableImpl<FormConfigId> implements Shareable, Labelable, Taggable, Owned {

	@NsIdRef
	protected Dataset dataset;
	@NotEmpty
	private String formType;
	@VariableDefaultValue @NonNull
	private UUID formId = UUID.randomUUID();
	private String label;
	@NotNull
	private String[] tags = ArrayUtils.EMPTY_STRING_ARRAY;
	private boolean shared = false;
	
	/**
	 * This is a blackbox for us at the moment, where the front end saves the state of the 
	 * formular, when the user saved it.
	 */
	@NotNull
	private JsonNode values;
	@MetaIdRef
	private User owner;
	@VariableDefaultValue
	private LocalDateTime creationTime = LocalDateTime.now();
	
	
	public FormConfig(String formType, JsonNode values) {
		this.formType = formType;
		this.values = values;
	}

	@Override
	public FormConfigId createId() {
		return new FormConfigId(dataset.getId(), formType, formId);
	}

	/**
	 * Provides an overview (meta data) of this form configuration without the
	 * actual form field values.
	 */
	public FormConfigOverviewRepresentation overview(Subject user) {
		String ownerName = Optional.ofNullable(owner).map(User::getLabel).orElse(null);

		return FormConfigOverviewRepresentation.builder()
			.id(getId())
			.formType(formType)
			.label(label)
			.tags(tags)
			.ownerName(ownerName)
			.own(user.isOwner(this))
			.createdAt(getCreationTime().atZone(ZoneId.systemDefault()))
			.shared(shared)
			// system?
			.build();
	}

	/**
	 * Return the full representation of the configuration with the configured form fields and meta data.
	 */
	public FormConfigFullRepresentation fullRepresentation(MetaStorage storage, Subject requestingUser){
		String ownerName = Optional.ofNullable(owner).map(User::getLabel).orElse(null);

		/* Calculate which groups can see this query.
		 * This is usually not done very often and should be reasonable fast, so don't cache this.
		 */

		List<GroupId> permittedGroups = new ArrayList<>();
		for(Group group : storage.getAllGroups()) {
			for(Permission perm : group.getPermissions()) {
				if(perm.implies(createPermission(Ability.READ.asSet()))) {
					permittedGroups.add(group.getId());
					continue;
				}
			}
		}

		return FormConfigFullRepresentation.builder()
										   .id(getId()).formType(formType)
										   .label(label)
										   .tags(tags)
			.ownerName(ownerName)
			.own(requestingUser.isOwner(this))
										   .createdAt(getCreationTime().atZone(ZoneId.systemDefault()))
										   .shared(shared)
										   .groups(permittedGroups)
										   // system? TODO discuss how system is determined (may check if owning user is in a special system group or so)
										   .values(values)
										   .build();
	}

	@Override
	public ConqueryPermission createPermission(Set<Ability> abilities) {
		return FormConfigPermission.onInstance(abilities, getId());
	}

	/**
	 * API representation for the overview of all {@link FormConfig}s which does not
	 * include the form fields an their values.
	 */
	@Getter
	@SuperBuilder
	@ToString
	@EqualsAndHashCode(callSuper = false)
	@FieldNameConstants
	public static class FormConfigOverviewRepresentation {

		private FormConfigId id;
		private String formType;
		private String label;
		private String[] tags;

		private String ownerName;
		private ZonedDateTime createdAt;
		private boolean own;
		private boolean shared;
		private boolean system;

	}

	/**
	 * API representation for a single {@link FormConfig} which includes the form
	 * fields an their values.
	 */
	@Getter
	@SuperBuilder
	@ToString(callSuper = true)
	@EqualsAndHashCode(callSuper = true)
	@FieldNameConstants
	public static class FormConfigFullRepresentation extends FormConfigOverviewRepresentation {

		/**
		 * The groups this config is shared with.
		 */
		private Collection<GroupId> groups;

		private JsonNode values;
	}

	public Consumer<FormConfigPatch> valueSetter() {
		return (patch) -> {setValues(patch.getValues());};
	}

}
