package com.bakdata.conquery.models.forms.configs;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

import javax.validation.constraints.NotNull;

import com.bakdata.conquery.apiv1.FormConfigPatch;
import com.bakdata.conquery.io.xodus.MetaStorage;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.execution.Labelable;
import com.bakdata.conquery.models.execution.Shareable;
import com.bakdata.conquery.models.execution.Taggable;
import com.bakdata.conquery.models.identifiable.IdentifiableImpl;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.identifiable.ids.specific.FormConfigId;
import com.bakdata.conquery.models.identifiable.ids.specific.UserId;
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
import javax.validation.constraints.NotEmpty;

@Slf4j
@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString
@EqualsAndHashCode(callSuper = false)
@FieldNameConstants
public class FormConfig extends IdentifiableImpl<FormConfigId> implements Shareable, Labelable, Taggable{

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
	 * formular, when the user saved it.
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
	public FormConfigOverviewRepresentation overview(MetaStorage storage, User user) {
		String ownerName = Optional.ofNullable(storage.getUser(owner)).map(User::getLabel).orElse(null);

		return FormConfigOverviewRepresentation.builder()
			.id(getId())
			.formType(formType)
			.label(label)
			.tags(tags)
			.ownerName(ownerName)
			.own(owner.equals(user.getId()))
			.createdAt(getCreationTime().atZone(ZoneId.systemDefault()))
			.shared(shared)
			// system?
			.build();
	}

// TODO rework translation with rework of Id-System
//	/**
//	 * Tries to convert this form to the provided dataset. It does not
//	 * check whether the {@link NamespacedId} that are converted in this processes
//	 * are actually resolvable. Also, it tries to map the values to a subclass of
//	 * {@link Form}, for conversion. If that is not possible the an empty optional is returned.
//	 */
//	public Optional<FormConfig> tryTranslateToDataset(Namespaces namespaces, DatasetId target, ObjectMapper mapper) {
//		ObjectNode finalRep = (ObjectNode) values;
//		if(!finalRep.has("type")) {
//			finalRep.put("type", formType);			
//		}
//		try {
//			Form intermediateRep = mapper.readerFor(Form.class).readValue(values);
//			if (! NamespacedIdHolding.class.isAssignableFrom(intermediateRep.getClass())) {
//				log.trace("Not translating FormConfig ({}) with form type ({}) to dataset ({}), because it does not hold any namespaced ids for translation.", this.getId(), this.getFormType(), target);
//				return Optional.empty();
//			}
//			Form translatedRep = QueryTranslator.replaceDataset(namespaces, intermediateRep, target);
//			finalRep = mapper.valueToTree(translatedRep);
//		}
//		catch (IOException e) {
//			log.warn("Unable to translate form configuration {} to dataset {}.", getId(), target, e);
//			return Optional.empty();
//		}
//		
//		FormConfig translatedConf = new FormConfig(
//			target,
//			formType,
//			formId,
//			label,
//			tags,
//			shared,
//			finalRep,
//			owner,
//			creationTime
//			);
//
//		return Optional.of(translatedConf);
//	}

	/**
	 * Return the full representation of the configuration with the configured form fields and meta data.
	 */
	public FormConfigFullRepresentation fullRepresentation(MetaStorage storage, User requestingUser){
		String ownerName = Optional.ofNullable(storage.getUser(owner)).map(User::getLabel).orElse(null);
		return FormConfigFullRepresentation.builder()
			.id(getId()).formType(formType)
			.label(label)
			.tags(tags)
			.ownerName(ownerName)
			.own(requestingUser != null? requestingUser.getId().equals(owner) : false)
			.createdAt(getCreationTime().atZone(ZoneId.systemDefault()))
			.shared(shared)
			// system? TODO discuss how system is determined (may check if owning user is in a special system group or so)
			.values(values).build();
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

		private JsonNode values;
	}

	public Consumer<FormConfigPatch> valueSetter() {
		return (patch) -> {setValues(patch.getValues());};
	}

}
