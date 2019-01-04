package com.bakdata.conquery.models.messages.network.specific;

import java.util.Collections;
import java.util.List;

import javax.validation.constraints.NotNull;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.jobs.JobStatus;
import com.bakdata.conquery.models.messages.network.MasterMessage;
import com.bakdata.conquery.models.messages.network.NetworkMessage;
import com.bakdata.conquery.models.messages.network.NetworkMessageContext.Master;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@CPSType(id="UPDATE_JOB_MANAGER_STATUS", base=NetworkMessage.class)
@NoArgsConstructor @AllArgsConstructor @Getter @Setter @ToString(of="slaveId", callSuper=true)
public class UpdateJobManagerStatus extends MasterMessage {
	@NotNull
	private List<JobStatus> status = Collections.emptyList();

	@Override
	public void react(Master context) throws Exception {
		context
			.getNamespaces()
			.getSlaves()
			.get(context.getRemoteAddress())
			.setJobManagerStatus(status);
	}
}
