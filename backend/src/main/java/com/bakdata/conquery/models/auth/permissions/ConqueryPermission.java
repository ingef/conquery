package com.bakdata.conquery.models.auth.permissions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.shiro.authz.Permission;

import com.bakdata.conquery.io.cps.CPSBase;
import com.bakdata.conquery.io.cps.CPSTypeIdResolver;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.JsonTypeIdResolver;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@JsonTypeInfo(use=JsonTypeInfo.Id.CUSTOM, include=JsonTypeInfo.As.PROPERTY, property="type")
@JsonTypeIdResolver(CPSTypeIdResolver.class)
@CPSBase
@Getter
@ToString
@EqualsAndHashCode(callSuper=false)
public abstract class ConqueryPermission implements Permission , HasTarget{
	protected final EnumSet<Ability> abilities = EnumSet.noneOf(Ability.class);
	
	private List<Object> classifier() {
		return Arrays.asList(this.getTarget());
	}
	
	/**
	 * Reduces a list of permissions, by combining permissions of the same owner and target by merging the access types
	 * of the individual permissions into one enum.
	 */
	public static final List<ConqueryPermission> reduceByOwnerAndTarget(List<ConqueryPermission> permissions) {
		Map<Object, List<ConqueryPermission>> groupById = permissions
				.stream()
				.collect(Collectors.groupingBy(ConqueryPermission::classifier));
		List<ConqueryPermission> combined = new ArrayList<>();
		groupById.forEach((k, v) -> {
			Iterator<ConqueryPermission> it = v.iterator();
			ConqueryPermission permission = null;
			EnumSet<Ability> accesses = EnumSet.noneOf(Ability.class);
			while(it.hasNext()) {
				permission = it.next();
				accesses.addAll(permission.getAbilities());
			}
			permission.getAbilities().addAll(accesses);
			combined.add(permission);
		});
		return combined;
	}
	
	public ConqueryPermission(Set<Ability> accesses) {
		this.abilities.addAll(accesses);
	}
	
	@Override
	public boolean implies(Permission permission) {
		// Check permission category
		if(!(permission instanceof ConqueryPermission)) {
			// Fast return on false
			return false;
		}
		
		ConqueryPermission cp = (ConqueryPermission) permission;
		
		// Check access
		return this.getAbilities().containsAll(cp.getAbilities());
	}

}