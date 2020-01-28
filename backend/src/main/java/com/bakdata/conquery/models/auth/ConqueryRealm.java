package com.bakdata.conquery.models.auth;

import javax.ws.rs.container.ContainerRequestContext;

import com.bakdata.conquery.io.cps.CPSBase;
import com.bakdata.conquery.io.xodus.MasterMetaStorage;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Getter;
import lombok.Setter;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.realm.AuthenticatingRealm;

@JsonTypeInfo(use=JsonTypeInfo.Id.CUSTOM, property="type")
@CPSBase
@Getter @Setter
@JsonIgnoreProperties({ "credentialsMatcher", "authenticationCache", "authenticationCachingEnabled", "authenticationCacheName", "authenticationTokenClass", "name", "cachingEnabled", "cacheManager", "authenticationInfo" })
public abstract class ConqueryRealm extends AuthenticatingRealm {
	
	@JsonIgnore
	private MasterMetaStorage storage;
		
	public abstract AuthenticationToken extractToken(ContainerRequestContext request);
	
}
