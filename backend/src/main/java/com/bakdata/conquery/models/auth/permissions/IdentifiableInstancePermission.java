package com.bakdata.conquery.models.auth.permissions;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.shiro.authz.Permission;
import org.hibernate.validator.constraints.NotEmpty;

import com.bakdata.conquery.models.identifiable.ids.AId;
import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.FieldNameConstants;

@Getter
@ToString(callSuper=true)
@EqualsAndHashCode(callSuper=true)
@FieldNameConstants
public abstract class IdentifiableInstancePermission<ID extends AId<?>> extends ConqueryPermission implements HasCompactedAbilities, HasTarget{

	/**
	 * This getter is only used for the JSON serialization/deserialization.
	 */
	@NotEmpty
	@Getter(onMethod = @__({@Deprecated}))
	protected final Set<Ability> abilities = Collections.synchronizedSet(EnumSet.noneOf(Ability.class));
	protected final ID instanceId;
	
	public IdentifiableInstancePermission(Set<Ability> abilities,  ID instanceId) {
		super();
		Set<Ability> allowed = allowedAbilities();
		Objects.requireNonNull(allowed);
		if(abilities.isEmpty() || !allowed.containsAll(abilities)) {
			throw new IllegalStateException(String.format("Cannot create permission, because illegal abilities where supplied.\n\tAllowed: %s\n\tSupplied: %s", allowed, abilities));
		}
		this.abilities.addAll(abilities);
		Objects.requireNonNull(instanceId);
		this.instanceId = instanceId;
	}
	
	
	@Override
	public boolean implies(Permission permission) {
		// Check owner and accesses
		if(!super.implies(permission)) {
			return false;
		}
		
		// Check permission category
		if(!(permission instanceof IdentifiableInstancePermission)) {
			return false;
		}
		
		IdentifiableInstancePermission<?> ip = (IdentifiableInstancePermission<?>) permission;
		
		// Check instance
		if(!instanceId.equals(ip.getInstanceId())) {
			return false;
		}

		// Check access
		return abilities.containsAll(ip.abilities);
	}

	public ID getTarget() {
		return instanceId;
	}


	@Override
	public void setAbilities(Set<Ability> accesses) {
		if(!allowedAbilities().containsAll(accesses)) {
			throw new IllegalStateException(String.format("Cannot modify permission, because illegal abilities where supplied.\n\tAllowed: %s\n\tSupplied: %s", allowedAbilities(), accesses));
		}
		synchronized (abilities) {
			abilities.clear();
			abilities.addAll(accesses);
		}
	}
	
	public boolean addAbilities(Set<Ability> accesses) {
		if(!allowedAbilities().containsAll(accesses)) {
			throw new IllegalStateException(String.format("Cannot modify permission, because illegal abilities where supplied.\n\tAllowed: %s\n\tSupplied: %s", allowedAbilities(), accesses));
		}
		return abilities.addAll(accesses);
	}
		
	public boolean removeAllAbilities(Set<Ability> delAbilities){
		return abilities.removeAll(delAbilities);
	}

	@JsonIgnore
	public Set<Ability> copyAbilities(){
		return Set.copyOf(abilities);
	}
	
	/**
	 * Reduces a  permission, by combining permissions of the same target by merging the access types
	 * of the individual permissions into one enum.
	 */
	public static final List<IdentifiableInstancePermission<?>> reduceByTarget(List<IdentifiableInstancePermission<?>> permissions) {
		Map<Object, List<IdentifiableInstancePermission<?>>> groupById = permissions
				.stream()
				.collect(Collectors.groupingBy(IdentifiableInstancePermission::getTarget));
		List<IdentifiableInstancePermission<?>> combined = new ArrayList<>();
		groupById.forEach((k, v) -> {
			IdentifiableInstancePermission<?> permission = null;
			EnumSet<Ability> accesses = EnumSet.noneOf(Ability.class);
			Iterator<IdentifiableInstancePermission<?>> it = v.iterator();
			while(it.hasNext()) {
				permission = it.next();
				accesses.addAll(permission.abilities);
			}
			permission.addAbilities(accesses);
			combined.add(permission);
		});
		return combined;
	}
	
	@Override
	public Optional<ConqueryPermission> findSimilar(Collection<ConqueryPermission> permissions) {
		Iterator<ConqueryPermission> it = permissions.iterator();
		while (it.hasNext()) {
			ConqueryPermission perm = it.next();
			if(!getClass().isAssignableFrom(perm.getClass())) {
				continue;
			}
			if (!((HasTarget)perm).getTarget().equals(getTarget())) {
				continue;
			}
			return Optional.of(perm);
		}
		return Optional.empty();
	}
}
