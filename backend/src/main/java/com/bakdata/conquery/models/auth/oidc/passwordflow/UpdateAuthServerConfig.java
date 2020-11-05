package com.bakdata.conquery.models.auth.oidc.passwordflow;

import java.io.PrintWriter;
import java.util.List;
import java.util.Map;

import io.dropwizard.servlets.tasks.Task;

public class UpdateAuthServerConfig extends Task {

	protected UpdateAuthServerConfig(String name) {
		super("update-oidc-config");
		
	}

	@Override
	public void execute(Map<String, List<String>> parameters, PrintWriter output) throws Exception {
		// TODO Auto-generated method stub
		
	}

}
