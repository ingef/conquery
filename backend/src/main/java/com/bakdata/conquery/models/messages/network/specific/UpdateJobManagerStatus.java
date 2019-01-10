package com.bakdata.conquery.models.messages.network.specific;

import java.util.Collections;
import java.util.List;

import javax.validation.constraints.NotNull;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.jobs.JobStatus;
import com.bakdata.conquery.models.messages.network.MasterMessage;
import com.bakdata.conquery.models.messages.network.NetworkMessage;
import com.bakdata.conquery.models.messages.network.NetworkMessageContext.Master;
import com.bakdata.conquery.models.worker.SlaveInformation;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@CPSType(id="UPDATE_JOB_MANAGER_STATUS", base=NetworkMessage.class)
@NoArgsConstructor @AllArgsConstructor @Getter @Setter @ToString(of="slaveId", callSuper=true)
@Slf4j
public class UpdateJobManagerStatus extends MasterMessage {
	@NotNull
	private List<JobStatus> status = Collections.emptyList();

	@Override
	public void react(Master context) throws Exception {
		SlaveInformation slave = context
			.getNamespaces()
			.getSlaves()
			.get(context.getRemoteAddress());
		
		if(slave == null) {
			log.error("Could not find slave {}, I only know of {}", context.getRemoteAddress(), context.getNamespaces().getSlaves().keySet());
		} else {
			slave.setJobManagerStatus(status);
		}
	}
}
