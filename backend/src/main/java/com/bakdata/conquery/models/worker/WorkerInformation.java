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
import it.unimi.dsi.fastutil.ints.IntArrayList;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class WorkerInformation extends NamedImpl<WorkerId> implements MessageSender.Transforming<WorkerMessage, SlaveMessage> {
	@NotNull
	private DatasetId dataset;
	@NotNull
	private IntArrayList includedBuckets = new IntArrayList();
	@JsonIgnore
	private transient SlaveInformation connectedSlave;

	@Override
	public WorkerId createId() {
		return new WorkerId(dataset, getName());
	}
	
	@JsonIgnore
	public int findLargestEntityId() {
		int max = -1;
		for(int i=includedBuckets.size() - 1; i>=0; i--) {
			if(includedBuckets.getInt(i) > max) {
				max = includedBuckets.getInt(i);
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
