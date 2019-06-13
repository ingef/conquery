package com.bakdata.eva.models.auth;

import javax.validation.constraints.NotNull;

import org.apache.shiro.realm.AuthorizingRealm;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.xodus.MasterMetaStorage;
import com.bakdata.conquery.models.auth.AuthConfig;
import com.bakdata.conquery.models.auth.TokenExtractor;
import com.bakdata.conquery.models.auth.UnknownUserHandler;
import com.bakdata.conquery.models.auth.subjects.Mandator;
import com.bakdata.conquery.models.auth.subjects.User;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Builder;
import lombok.Getter;

@CPSType(base=AuthConfig.class, id="INGEF")
@Getter @Builder
public class IngefAuthConfig extends AuthConfig {
	@NotNull
	private String secret;
	@NotNull
	private Mandator initialMandator;
	@NotNull
	private User initialUser;
	
	@JsonIgnore
	private AuthorizingRealm realm;
	@JsonIgnore
	private UnknownUserHandler handler;
	@JsonIgnore
	private final TokenExtractor  tokenExtractor = new IngefTokenExtractor();
	
	@Override
	public AuthorizingRealm getRealm(MasterMetaStorage storage) {
		if(realm == null) {
			realm = new IngefRealm(storage, secret);
		}
		return realm;
	}

	@Override
	public UnknownUserHandler getUnknownUserHandler(MasterMetaStorage storage) {
		if(handler == null) {
			handler = new IngefUnknownUserHandler(storage);
		}
		return handler;
	}

	@Override
	public void initializeAuthConstellation(MasterMetaStorage storage) {
		initialUser.addMandatorLocal(initialMandator);
		try {
			storage.updateMandator(initialMandator);
			storage.updateUser(initialUser);
		} catch (JSONException e) {
			throw new IllegalStateException("Failed to propagate initial authentication constellation to storage.", e);
		}
		
		
	}
}
