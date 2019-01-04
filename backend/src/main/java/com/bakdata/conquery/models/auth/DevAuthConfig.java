package com.bakdata.conquery.models.auth;

import org.apache.shiro.realm.AuthorizingRealm;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.xodus.MasterMetaStorage;

@CPSType(base=AuthConfig.class, id="DEVELOPMENT")
public class DevAuthConfig extends AuthConfig {

	@Override
	public AuthorizingRealm getRealm(MasterMetaStorage storage) {
		return new AllGrantedRealm(storage);
	}

}
