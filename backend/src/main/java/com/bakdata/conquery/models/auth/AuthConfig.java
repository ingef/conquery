package com.bakdata.conquery.models.auth;

import org.apache.shiro.realm.AuthorizingRealm;

import com.bakdata.conquery.io.cps.CPSBase;
import com.bakdata.conquery.io.xodus.MasterMetaStorage;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.CUSTOM, property = "type")
@CPSBase
public abstract class AuthConfig {
	public abstract AuthorizingRealm getRealm(MasterMetaStorage storage);
	public abstract UnknownUserHandler getUnknownUserHandler(MasterMetaStorage storage);
	public abstract void initializeAuthConstellation(MasterMetaStorage storage);
	public abstract TokenExtractor getTokenExtractor();
}
