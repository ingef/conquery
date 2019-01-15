package com.bakdata.conquery.models.auth;

import org.apache.shiro.realm.AuthorizingRealm;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.xodus.MasterMetaStorage;
import com.bakdata.conquery.models.auth.subjects.User;
import com.bakdata.conquery.models.auth.util.SinglePrincipalCollection;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.identifiable.ids.specific.UserId;
import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Getter;

@CPSType(base=AuthConfig.class, id="DEVELOPMENT")
public class DevAuthConfig extends AuthConfig {

	private static final String PRINCIPAL = "SUPERUSER@ALLGRANTEDREALM.DE";
	protected static final UserId ID= new UserId(PRINCIPAL);
	private static final String LABEL = "SUPERUSER";
	private static final UnknownUserHandler U_U_HANDLER = new DefaultUnknownUserHandler();
	@Getter @JsonIgnore
	private final TokenExtractor tokenExtractor= new DefaultTokenExtractor();
	
	@Override
	public AuthorizingRealm getRealm(MasterMetaStorage storage) {
		return new AllGrantedRealm(storage);
	}
	@Override
	public UnknownUserHandler getUnknownUserHandler(MasterMetaStorage storage) {
		return U_U_HANDLER;
	}
	@Override
	public void initializeAuthConstellation(MasterMetaStorage storage) {
		User user = new User(new SinglePrincipalCollection(ID));
		user.setStorage(storage);
		user.setName(LABEL);
		user.setLabel(LABEL);
		try {
			storage.updateUser(user);
		} catch (JSONException e) {
			throw new IllegalStateException(e);
		}
	}

}
