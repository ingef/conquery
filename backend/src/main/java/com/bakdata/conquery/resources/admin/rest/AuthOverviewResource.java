package com.bakdata.conquery.resources.admin.rest;

import javax.ws.rs.GET;

import com.bakdata.conquery.resources.hierarchies.HAdmin;


public class AuthOverviewResource extends HAdmin {

	@GET
	public String getPermissionOverviewAsCSV() {
		processor.getPermissionOverviewAsCSV();
		return null;
	}

}
