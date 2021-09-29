package com.bakdata.conquery.models.auth.util;

import com.bakdata.conquery.models.auth.ConqueryAuthenticationRealm;
import com.bakdata.conquery.models.auth.entities.UserLike;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import org.apache.shiro.subject.PrincipalCollection;

import java.util.*;

@EqualsAndHashCode
public class UserishPrincipalCollection implements PrincipalCollection {

	private static final long serialVersionUID = -1801050265305362978L;

	private final UserLike principal;
	private final ConqueryAuthenticationRealm realm;

	public UserishPrincipalCollection(@NonNull UserLike userLike, ConqueryAuthenticationRealm realm) {
		this.principal = userLike;
		this.realm = realm;
	}

	@Override @JsonIgnore
	public Iterator<UserLike> iterator() {
		return new Iterator<>() {
			private boolean notCalled = true;
			@Override
			public boolean hasNext() {
				boolean ret = notCalled;
				notCalled = false;
				return ret;
			}

			@Override
			public UserLike next() {
				return principal;
			}
		};
	}

	@Override @JsonIgnore
	public UserLike getPrimaryPrincipal() {
		return principal;
	}

	@Override
	public <T> T oneByType(Class<T> type) {
		if(type.isAssignableFrom(principal.getClass())) {
			return (T) principal;
		}
		return null;
	}

	@Override
	public <T> Collection<T> byType(Class<T> type) {
		if(type.isAssignableFrom(principal.getClass())) {
			return List.of((T)principal);
		}
		return Collections.emptyList();
	}

	@Override
	public List<UserLike> asList() {
		return  List.of(principal);
	}

	@Override
	public Set<UserLike> asSet() {
		return Set.of(principal);
	}

	@Override
	public Collection<UserLike> fromRealm(String realmName) {
		if(realm.getName().equals(realmName)){
			return List.of(principal);
		}
		return Collections.emptyList();
	}

	@Override @JsonIgnore
	public Set<String> getRealmNames() {
		if(realm != null) {
			return Set.of(realm.getName());
		}
		return Collections.emptySet();
	}

	@Override @JsonIgnore
	public boolean isEmpty() {
		return false;
	}

}
