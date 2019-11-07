package com.bakdata.conquery.resources.admin.ui;

import static com.bakdata.conquery.resources.ResourceConstants.USER_ID;

import java.lang.reflect.InvocationTargetException;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.bakdata.conquery.models.identifiable.ids.specific.UserId;
import com.bakdata.conquery.resources.admin.ui.model.UIView;
import com.bakdata.conquery.resources.hierarchies.HUsers;
import com.fasterxml.jackson.core.JsonProcessingException;

import io.dropwizard.views.View;


@Produces(MediaType.TEXT_HTML)
public class UserUIResource extends HUsers {


	@GET
	public View getUsers() {
		return new UIView<>("users.html.ftl", processor.getUIContext(), processor.getAllUsers());
	}
	
	/**
	 * End point for retrieving information about a specific role.
	 * @param roleId Unique id of the role.
	 * @return A view holding the information about the role.
	 * @throws JsonProcessingException 
	 * @throws InvocationTargetException 
	 * @throws IllegalArgumentException 
	 * @throws IllegalAccessException 
	 * @throws SecurityException 
	 * @throws NoSuchMethodException 
	 */
	@Path("{" + USER_ID + "}")
	@GET
	public View getUser(@PathParam(USER_ID) UserId userId) throws JsonProcessingException, NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		return new UIView<>("user.html.ftl", processor.getUIContext(), processor.getUserContent(userId));
	}
}
