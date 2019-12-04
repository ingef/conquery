package com.bakdata.conquery.resources.admin.ui;

import javax.ws.rs.Path;

import com.bakdata.conquery.resources.admin.ui.model.UIView;
import com.bakdata.conquery.resources.hierarchies.HAdmin;

@Path("auth-overview")
public class AuthOverviewUIResource extends HAdmin {
	
	
	
	public void getOverview() {
		new UIView<>("authOverview.html.ftl", ctx)
	}

}
