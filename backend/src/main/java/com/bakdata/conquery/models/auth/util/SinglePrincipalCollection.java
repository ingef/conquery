package com.bakdata.conquery.models.auth.util;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.shiro.subject.PrincipalCollection;

import com.bakdata.conquery.ConqueryConstants;
import com.bakdata.conquery.models.identifiable.ids.specific.PermissionOwnerId;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;

public class SinglePrincipalCollection implements PrincipalCollection{

	private static final long serialVersionUID = -1801050265305362978L;

	private final String realm = ConqueryConstants.AuthenticationUtil.REALM_NAME;
	private final List<PermissionOwnerId<?>> principal;
	
	public SinglePrincipalCollection(PermissionOwnerId<?> principal) {
		if(principal == null) {
			throw new IllegalArgumentException("Principal is not allowed to be null");
		}
		this.principal = Arrays.asList(principal);
	}
	
	@JsonCreator
	public SinglePrincipalCollection(List<PermissionOwnerId<?>> principal) {
		if(principal.isEmpty()) {
			throw new IllegalArgumentException("Principal is not allowed to be empty");
		}
		this.principal = principal;
	}

	@Override @JsonIgnore
	public Iterator<Object> iterator() {
		return new Iterator<>() {
			private boolean notCalled = true;
			@Override
			public boolean hasNext() {
				boolean ret = notCalled;
				notCalled = false;
				return ret;
			}

			@Override
			public Object next() {
				return principal.get(0);
			}
		};
	}

	@Override @JsonIgnore
	public Object getPrimaryPrincipal() {
		return principal.get(0);
	}

	@Override
	public <T> T oneByType(Class<T> type) {
		if(type.isInstance(principal.get(0))) {
			return (T) principal.get(0);
		}
		return null;
	}

	@Override
	public <T> Collection<T> byType(Class<T> type) {
		if(type.isInstance(principal.get(0))) {
			return (Collection<T>) principal;
		}
		return Collections.emptyList();
	}

	@Override
	public List asList() {
		return  principal;
	}

	@Override
	public Set asSet() {
		return new HashSet<>(principal);
	}

	@Override
	public Collection fromRealm(String realmName) {
		if(realm.equals(realmName)){
			return principal;
		}
		return Collections.emptyList();
	}

	@Override @JsonIgnore
	public Set<String> getRealmNames() {
		if(realm != null) {
			return new HashSet<String>(Arrays.asList(realm));
		}
		return Collections.emptySet();
	}

	@Override @JsonIgnore
	public boolean isEmpty() {
		return principal.isEmpty();
	}

}
