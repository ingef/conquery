package com.bakdata.eva.models.auth;

import java.util.List;

import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;

import com.bakdata.conquery.io.xodus.MasterMetaStorage;
import com.bakdata.conquery.models.auth.UnknownUserHandler;
import com.bakdata.conquery.models.auth.subjects.Mandator;
import com.bakdata.conquery.models.auth.subjects.User;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.identifiable.ids.specific.MandatorId;
import com.bakdata.conquery.models.identifiable.ids.specific.UserId;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class IngefUnknownUserHandler implements UnknownUserHandler{
	private final MasterMetaStorage storage;

	/**
	 * Creates a new user if a token is encountered, that
	 * describes an unknown user but a known mandator.
	 */
	@Override
	public User handle(AuthenticationInfo info) {
		List<?> principals = info.getPrincipals().asList();
		UserId userId = (UserId) principals.get(PrincipalIndex.EMAIL.IDX);
		MandatorId mandatorId = (MandatorId) principals.get(PrincipalIndex.MANDATOR.IDX);
		String name = (String) principals.get(PrincipalIndex.NAME.IDX);
		Mandator mandator = storage.getMandator(mandatorId);
		if(mandator == null) {
			throw new AuthenticationException("The supplied mandator id is unknown to the system");
		}
		if(name == null) {
			throw new AuthenticationException("No user name could be extracted");
		}
		
		User user = new User(userId.getEmail(), name);
		
		user.addMandatorLocal(mandator);
		try {
			storage.addUser(user);
			log.info("Added a new user to the storage.");
		} catch (JSONException e) {
			throw new IllegalStateException("Unable to add user to storage: " + user, e);
		}
		return user;
	}
}
