package com.bakdata.conquery.models.auth.permissions;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

import org.apache.shiro.authz.Permission;

import com.bakdata.conquery.io.cps.CPSBase;
import com.bakdata.conquery.io.cps.CPSTypeIdResolver;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.JsonTypeIdResolver;

import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import lombok.ToString;	

@JsonTypeInfo(use=JsonTypeInfo.Id.CUSTOM, include=JsonTypeInfo.As.PROPERTY, property="type")
@JsonTypeIdResolver(CPSTypeIdResolver.class)
@CPSBase
@ToString
@EqualsAndHashCode(callSuper=false)
@RequiredArgsConstructor
public abstract class ConqueryPermission implements Permission {
	
	
	@Override
	public boolean implies(Permission permission) {
		// Check exact permission category
		return this.getClass().isAssignableFrom(permission.getClass());
		
	}
	
	public Optional<ConqueryPermission> subtract(ConqueryPermission subtrahend) {
		if (!getClass().isAssignableFrom(subtrahend.getClass())) {
			throw new IllegalStateException(String.format("Cannot subtract differing classes, which are: %s nd %s", getClass(), subtrahend.getClass()));
		}
		return Optional.empty();
	}
	
	public abstract Optional<ConqueryPermission> findSimilar(Collection<ConqueryPermission> permissions);
}