package com.bakdata.conquery.models.auth;

import javax.annotation.Nullable;
import javax.ws.rs.container.ContainerRequestContext;

import com.bakdata.conquery.io.cps.CPSBase;
import com.bakdata.conquery.io.xodus.MasterMetaStorage;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.realm.AuthenticatingRealm;

@JsonTypeInfo(use=JsonTypeInfo.Id.CUSTOM, property="type")
@CPSBase
@Getter @Setter
@AllArgsConstructor
@JsonIgnoreProperties({ "credentialsMatcher", "authenticationCache", "authenticationCachingEnabled", "authenticationCacheName", "authenticationTokenClass", "name", "cachingEnabled", "cacheManager", "authenticationInfo" })
public abstract class ConqueryRealm extends AuthenticatingRealm {
	
	@JsonIgnore
	private final MasterMetaStorage storage;
	
	@Nullable
	public abstract AuthenticationToken extractToken(ContainerRequestContext request);
	
}
