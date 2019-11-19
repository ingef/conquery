package com.bakdata.conquery.models.messages.namespaces.specific;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.identifiable.ids.specific.ConceptId;
import com.bakdata.conquery.models.messages.namespaces.NamespacedMessage;
import com.bakdata.conquery.models.messages.namespaces.WorkerMessage;
import com.bakdata.conquery.models.worker.Worker;
import com.fasterxml.jackson.annotation.JsonCreator;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@CPSType(id="REMOVE_CONCEPT", base=NamespacedMessage.class)
@AllArgsConstructor(onConstructor_=@JsonCreator) @Getter @Setter @ToString
public class RemoveConcept extends WorkerMessage.Slow {

	private ConceptId concept;
	
	@Override
	public void react(Worker context) throws Exception {
		synchronized (context.getStorage()) {
			context.getStorage().removeConcept(concept);
		}
	}
}
