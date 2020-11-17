package com.bakdata.conquery.models.messages.namespaces.specific;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.dictionary.Dictionary;
import com.bakdata.conquery.models.messages.namespaces.NamespacedMessage;
import com.bakdata.conquery.models.messages.namespaces.WorkerMessage;
import com.bakdata.conquery.models.worker.Worker;
import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.AllArgsConstructor;
import lombok.ToString;

@CPSType(id="UPDATE_DICTIONARY", base=NamespacedMessage.class)
@AllArgsConstructor(onConstructor_=@JsonCreator)
@ToString
public class UpdateDictionary extends WorkerMessage.Slow {

	private final Dictionary dictionary;

	@Override
	public void react(Worker context) throws Exception {
		context.updateDictionary(dictionary);
	}
}
