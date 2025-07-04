package com.bakdata.conquery.models.forms.configs;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import com.bakdata.conquery.apiv1.FormConfigPatch;
import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.models.auth.entities.Group;
import com.bakdata.conquery.models.auth.entities.Subject;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.auth.permissions.Ability;
import com.bakdata.conquery.models.auth.permissions.ConqueryPermission;
import com.bakdata.conquery.models.auth.permissions.FormConfigPermission;
import com.bakdata.conquery.models.execution.Labelable;
import com.bakdata.conquery.models.execution.Owned;
import com.bakdata.conquery.models.execution.Shareable;
import com.bakdata.conquery.models.execution.Taggable;
import com.bakdata.conquery.models.identifiable.MetaIdentifiable;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.identifiable.ids.specific.FormConfigId;
import com.bakdata.conquery.models.identifiable.ids.specific.GroupId;
import com.bakdata.conquery.models.identifiable.ids.specific.UserId;
import com.bakdata.conquery.util.VariableDefaultValue;
import com.fasterxml.jackson.annotation.JsonIgnore;
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
import org.jetbrains.annotations.Nullable;

@Slf4j
@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString
@EqualsAndHashCode(callSuper = false)
@FieldNameConstants
public class FormConfig extends MetaIdentifiable<FormConfigId> implements Shareable, Labelable, Taggable, Owned {

	protected DatasetId dataset;
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
	 * form, when the user saved it.
	 */
	@NotNull
	private JsonNode values;
	private UserId owner;
	@VariableDefaultValue
	private LocalDateTime creationTime = LocalDateTime.now();
	
	
	public FormConfig(String formType, JsonNode values) {
		this.formType = formType;
		this.values = values;
	}

	@Override
	public FormConfigId createId() {
		return new FormConfigId(dataset, formType, formId);
	}

	/**
	 * Provides an overview (meta data) of this form configuration without the
	 * actual form field values.
	 */
	public FormConfigOverviewRepresentation overview(Subject subject) {
		String ownerName = getOwnerName();

		return FormConfigOverviewRepresentation.builder()
											   .id(getId())
											   .formType(formType)
											   .label(label)
											   .tags(tags)
											   .ownerName(ownerName)
											   .own(subject.isOwner(this))
											   .createdAt(getCreationTime().atZone(ZoneId.systemDefault()))
											   .shared(shared)
											   // system?
											   .build();
	}

	@JsonIgnore
	@Nullable
	private String getOwnerName() {
		if (owner == null){
			return null;
		}

		User resolved = owner.get();

		if (resolved == null){
			return null;
		}

		return resolved.getLabel();
	}

	/**
	 * Return the full representation of the configuration with the configured form fields and meta data.
	 */
	public FormConfigFullRepresentation fullRepresentation(MetaStorage storage, Subject requestingUser){
		String ownerName = getOwnerName();

		/* Calculate which groups can see this query.
		 * This is usually not done very often and should be reasonable fast, so don't cache this.
		 */

		List<GroupId> permittedGroups = new ArrayList<>();
		for (Group group : storage.getAllGroups().toList()) {
			for(Permission perm : group.getPermissions()) {
				if(perm.implies(createPermission(Ability.READ.asSet()))) {
					permittedGroups.add(group.getId());
				}
			}
		}

		return FormConfigFullRepresentation.builder()
										   .id(getId())
										   .formType(formType)
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

	public Consumer<FormConfigPatch> valueSetter() {
		return (patch) -> setValues(patch.getValues());
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

}
