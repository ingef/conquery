package com.bakdata.conquery.commands;

import java.time.Clock;
import java.util.List;

import com.bakdata.conquery.mode.Manager;
import io.dropwizard.core.setup.Environment;

public interface StandaloneCommand {

	Manager getManager();

	List<ShardNode> getShardNodes();

	ManagerNode getManagerNode();

	Environment getEnvironment();

	void setClock(Clock clock);
	Clock getClock();

}
