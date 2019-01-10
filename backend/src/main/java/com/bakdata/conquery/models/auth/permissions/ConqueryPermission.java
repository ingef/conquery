package com.bakdata.conquery.models.auth.permissions;

import java.util.*;
import java.util.stream.Collectors;

import org.apache.shiro.authz.Permission;

import com.bakdata.conquery.io.cps.CPSBase;
import com.bakdata.conquery.io.cps.CPSTypeIdResolver;
import com.bakdata.conquery.models.identifiable.IdentifiableImpl;
import com.bakdata.conquery.models.identifiable.ids.specific.NullSubjectId;
import com.bakdata.conquery.models.identifiable.ids.specific.PermissionId;
import com.bakdata.conquery.models.identifiable.ids.specific.PermissionOwnerId;
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
public abstract class ConqueryPermission extends IdentifiableImpl<PermissionId> implements Permission , HasTarget{
	protected final PermissionOwnerId<?> ownerId;
	protected final EnumSet<Ability> abilities = EnumSet.noneOf(Ability.class);
	
	@EqualsAndHashCode.Exclude
	private  UUID jsonId = UUID.randomUUID();
	
	private List<Object> classifier() {
		return Arrays.asList(this.ownerId, this.getTarget());
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
			permission = permission.withOwner(permission.getOwnerId());
			permission.getAbilities().addAll(accesses);
			permission.getId(); // force ID creation
			combined.add(permission);
		});
		return combined;
	}
	
	public ConqueryPermission(PermissionOwnerId<?> ownerId, EnumSet<Ability> accesses) {
		if(ownerId == null) {
			this.ownerId = new NullSubjectId();
		} else {
			this.ownerId = ownerId;
		}
		this.abilities.addAll(accesses);
	}
	
	/*
	 * Used for deserialization from storage
	 */
	public ConqueryPermission(PermissionOwnerId<?> ownerId, EnumSet<Ability> accesses, UUID jsonId) {
		this(ownerId, accesses);
		if(jsonId != null) {
			this.jsonId = jsonId;
		}
	}
	
	@Override
	public boolean implies(Permission permission) {
		// Check permission category
		if(!(permission instanceof ConqueryPermission)) {
			return false; // Fast return on false
		}
		
		ConqueryPermission cp = (ConqueryPermission) permission;
		
		// Check access
		return this.getAbilities().containsAll(cp.getAbilities());
	}

	/**
	 * Returns a new permission with the owner swapped.
	 * This is useful, when the permissions of roles of a user need to be checked.
	 * The user object knows the impersonated roles and swaps the owner accordingly.
	 */
	public abstract ConqueryPermission withOwner(PermissionOwnerId<?> newOwner);
	
	@Override
	public PermissionId createId() {
		return new PermissionId(jsonId);
	}
}