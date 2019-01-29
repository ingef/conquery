package com.bakdata.conquery.models.messages.namespaces.specific;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.concepts.Concept;
import com.bakdata.conquery.models.messages.namespaces.NamespacedMessage;
import com.bakdata.conquery.models.messages.namespaces.WorkerMessage;
import com.bakdata.conquery.models.worker.Worker;
import com.fasterxml.jackson.annotation.JsonCreator;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@CPSType(id="UPDATE_CONCEPT", base=NamespacedMessage.class)
@AllArgsConstructor(onConstructor_=@JsonCreator) @Getter @Setter @ToString
public class UpdateConcept extends WorkerMessage.Slow {

	private Concept concept;
	
	@Override
	public void react(Worker context) throws Exception {
		synchronized (context.getStorage()) {
			concept.setDataset(context.getStorage().getDataset().getId());
			context.getStorage().updateConcept(concept);
		}
	}
}
