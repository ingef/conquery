package com.bakdata.conquery.models.messages.namespaces.specific;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.datasets.concepts.Concept;
import com.bakdata.conquery.models.exceptions.ValidatorHelper;
import com.bakdata.conquery.models.messages.namespaces.NamespacedMessage;
import com.bakdata.conquery.models.messages.namespaces.WorkerMessage;
import com.bakdata.conquery.models.worker.Worker;
import com.fasterxml.jackson.annotation.JsonCreator;
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
	
	@Override
	public void react(Worker context) throws Exception {
		ValidatorHelper.failOnError(log, context.getStorage().getValidator().validate(concept));
		context.updateConcept(concept);
	}
}
