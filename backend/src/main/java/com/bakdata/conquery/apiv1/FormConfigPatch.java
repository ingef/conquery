package com.bakdata.conquery.apiv1;

import java.util.function.Consumer;

import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.models.auth.entities.Subject;
import com.bakdata.conquery.models.auth.permissions.Ability;
import com.bakdata.conquery.models.forms.configs.FormConfig;
import com.bakdata.conquery.util.QueryUtils;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Specific class to also patch the values stored in a {@link FormConfig}.
 */
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
public class FormConfigPatch extends MetaDataPatch {
	private JsonNode values;
	
	public void applyTo(FormConfig instance, MetaStorage storage, Subject subject){
		chain(QueryUtils.getNoOpEntryPoint(), storage, subject, instance)
			.accept(this);		
	}
	
	protected Consumer<FormConfigPatch> chain(Consumer<FormConfigPatch> patchConsumerChain, MetaStorage storage, Subject subject, FormConfig instance) {
		patchConsumerChain = super.buildChain(patchConsumerChain, storage, subject, instance);

		if(getValues() != null && subject.isPermitted(instance,Ability.MODIFY)) {
			patchConsumerChain = patchConsumerChain.andThen(instance.valueSetter());
		}
		return patchConsumerChain;
	}
}
