package com.bakdata.conquery.models.auth.permissions;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.shiro.authz.Permission;
import org.hibernate.validator.constraints.NotEmpty;

import com.bakdata.conquery.io.cps.CPSBase;
import com.bakdata.conquery.io.cps.CPSTypeIdResolver;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.JsonTypeIdResolver;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@JsonTypeInfo(use=JsonTypeInfo.Id.CUSTOM, include=JsonTypeInfo.As.PROPERTY, property="type")
@JsonTypeIdResolver(CPSTypeIdResolver.class)
@CPSBase
@ToString
@EqualsAndHashCode(callSuper=false)
public abstract class ConqueryPermission implements Permission , HasTarget {
	/**
	 * This getter is only used for the JSON serialization/deserialization.
	 */
	@NotEmpty
	@Getter(onMethod = @__({@Deprecated}))
	protected final EnumSet<Ability> abilities = EnumSet.noneOf(Ability.class);
	
	/**
	 * Reduces a  permission, by combining permissions of the same target by merging the access types
	 * of the individual permissions into one enum.
	 */
	public static final List<ConqueryPermission> reduceByTarget(List<ConqueryPermission> permissions) {
		Map<Object, List<ConqueryPermission>> groupById = permissions
				.stream()
				.collect(Collectors.groupingBy(ConqueryPermission::getTarget));
		List<ConqueryPermission> combined = new ArrayList<>();
		groupById.forEach((k, v) -> {
			ConqueryPermission permission = null;
			EnumSet<Ability> accesses = EnumSet.noneOf(Ability.class);
			Iterator<ConqueryPermission> it = v.iterator();
			while(it.hasNext()) {
				permission = it.next();
				accesses.addAll(permission.abilities);
			}
			permission.addAbilities(accesses);
			combined.add(permission);
		});
		return combined;
	}
	
	public ConqueryPermission(Set<Ability> abilities) {
		Set<Ability> allowed = allowedAbilities();
		Objects.requireNonNull(allowed);
		if(abilities.isEmpty() || !allowed.containsAll(abilities)) {
			throw new IllegalStateException(String.format("Cannot create permission, because illegal abilities where supplied.\n\tAllowed: %s\n\tSupplied: %s", allowed, abilities));
		}
		this.abilities.addAll(abilities);
	}
	
	@Override
	public boolean implies(Permission permission) {
		// Check exact permission category
		if(!(this.getClass().isAssignableFrom(permission.getClass()))) {
			// Fast return on false
			return false;
		}
		
		ConqueryPermission cp = (ConqueryPermission) permission;
		
		// Check access
		return abilities.containsAll(cp.abilities);
	}
	
	public boolean addAbilities(Set<Ability> accesses) {
		if(!allowedAbilities().containsAll(accesses)) {
			throw new IllegalStateException(String.format("Cannot modify permission, because illegal abilities where supplied.\n\tAllowed: %s\n\tSupplied: %s", allowedAbilities(), accesses));
		}
		return abilities.addAll(accesses);
	}
	
	public abstract Set<Ability> allowedAbilities();
	
	public boolean removeAllAbilities(Set<Ability> delAbilities){
		return abilities.removeAll(delAbilities);
	}

	@JsonIgnore
	public Set<Ability> getAbilitiesCopy(){
		return Set.copyOf(abilities);
	}
}