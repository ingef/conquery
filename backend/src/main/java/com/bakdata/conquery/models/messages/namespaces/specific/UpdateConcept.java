package com.bakdata.conquery.models.messages.namespaces.specific;

import javax.validation.Validator;
import javax.validation.constraints.NotNull;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.datasets.concepts.Concept;
import com.bakdata.conquery.models.exceptions.ValidatorHelper;
import com.bakdata.conquery.models.messages.namespaces.NamespacedMessage;
import com.bakdata.conquery.models.messages.namespaces.WorkerMessage;
import com.bakdata.conquery.models.worker.Worker;
import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.OptBoolean;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@CPSType(id="UPDATE_CONCEPT", base=NamespacedMessage.class)
@RequiredArgsConstructor(onConstructor_=@JsonCreator) @Getter @Setter @ToString
@Slf4j
public class UpdateConcept extends WorkerMessage.Slow {

	private final Concept<?> concept;

	@JacksonInject(useInput = OptBoolean.FALSE)
	@EqualsAndHashCode.Exclude
	@JsonIgnore
	@NotNull
	private Validator validator;
	
	@Override
	public void react(Worker context) throws Exception {
		ValidatorHelper.failOnError(log, validator.validate(concept));
		context.updateConcept(concept);
	}
}
