package com.bakdata.conquery.models.messages.namespaces.specific;

import com.bakdata.conquery.ConqueryConstants;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.datasets.Import;
import com.bakdata.conquery.models.events.BucketManager;
import com.bakdata.conquery.models.identifiable.ids.specific.BucketId;
import com.bakdata.conquery.models.identifiable.ids.specific.ImportId;
import com.bakdata.conquery.models.identifiable.ids.specific.TableId;
import com.bakdata.conquery.models.messages.namespaces.NamespacedMessage;
import com.bakdata.conquery.models.messages.namespaces.WorkerMessage;
import com.bakdata.conquery.models.worker.Worker;
import com.fasterxml.jackson.annotation.JsonCreator;
import it.unimi.dsi.fastutil.ints.IntListIterator;
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

		// Remove associated ALL_IDs-Import
		context.getStorage().removeImport(new ImportId(new TableId(context.getStorage().getDataset().getId(), ConqueryConstants.ALL_IDS_TABLE), importId.toString()));

		final BucketManager bucketManager = context.getStorage().getBucketManager();

		boolean changed = false;

		for (IntListIterator it = context.getInfo().getIncludedBuckets().iterator(); it.hasNext(); ) {
			int bucket = it.nextInt();
			if(context.getStorage().getAllImports().stream()
					  .map(imp -> new BucketId(imp.getId(), bucket))
					  .noneMatch(bucketManager::hasBucket)){
				it.remove();
				changed = true;
			}
		}

		if(changed){
			//TODO Update WorkerInformation in Master
		}
	}
}
