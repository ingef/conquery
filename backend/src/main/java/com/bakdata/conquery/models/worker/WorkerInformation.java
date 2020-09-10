package com.bakdata.conquery.models.worker;

import javax.validation.constraints.NotNull;

import com.bakdata.conquery.io.mina.MessageSender;
import com.bakdata.conquery.models.identifiable.NamedImpl;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.identifiable.ids.specific.WorkerId;
import com.bakdata.conquery.models.messages.namespaces.WorkerMessage;
import com.bakdata.conquery.models.messages.network.SlaveMessage;
import com.bakdata.conquery.models.messages.network.specific.ForwardToWorker;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jetbrains.exodus.core.dataStructures.hash.IntHashSet;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class WorkerInformation extends NamedImpl<WorkerId> implements MessageSender.Transforming<WorkerMessage, SlaveMessage> {
	@NotNull
	private DatasetId dataset;
	@NotNull
	private IntHashSet includedBuckets = new IntHashSet();
	@JsonIgnore
	private transient SlaveInformation connectedSlave;

	@Override
	public WorkerId createId() {
		return new WorkerId(dataset, getName());
	}
	
	@JsonIgnore
	public int findLargestEntityId() {
		int max = -1;
		for(Integer bucket : includedBuckets) {			
			if(bucket > max) {
				max = bucket;
			}
		}
		return max;
	}

	@Override
	public SlaveInformation getMessageParent() {
		return connectedSlave;
	}

	@Override
	public SlaveMessage transform(WorkerMessage message) {
		return new ForwardToWorker(getId(), message);
	}
}
