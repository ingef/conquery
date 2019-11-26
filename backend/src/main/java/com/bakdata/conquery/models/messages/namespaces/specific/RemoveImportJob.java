package com.bakdata.conquery.models.messages.namespaces.specific;

import com.bakdata.conquery.ConqueryConstants;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.identifiable.ids.specific.ImportId;
import com.bakdata.conquery.models.identifiable.ids.specific.TableId;
import com.bakdata.conquery.models.messages.namespaces.NamespacedMessage;
import com.bakdata.conquery.models.messages.namespaces.WorkerMessage;
import com.bakdata.conquery.models.worker.Worker;
import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;



@CPSType(id="REMOVE_IMPORT", base= NamespacedMessage.class)
@AllArgsConstructor(onConstructor_=@JsonCreator)  @Getter @Setter @ToString
public class RemoveImportJob extends WorkerMessage.Slow {

	private ImportId importId;

	@Override
	public void react(Worker context) throws Exception {
		context.getStorage().removeImport(importId);

		// Remove associated AllIdsImport
		context.getStorage().removeImport(new ImportId(new TableId(context.getStorage().getDataset().getId(), ConqueryConstants.ALL_IDS_TABLE), importId.toString()));

		//TODO Update WorkerInformation in Master
	}
}
