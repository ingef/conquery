package com.bakdata.conquery.apiv1;

import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import com.bakdata.conquery.io.xodus.MetaStorage;
import com.bakdata.conquery.models.auth.entities.Group;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.auth.permissions.Ability;
import com.bakdata.conquery.models.auth.permissions.ConqueryPermission;
import com.bakdata.conquery.models.execution.Labelable;
import com.bakdata.conquery.models.execution.Shareable;
import com.bakdata.conquery.models.execution.Shareable.ShareInformation;
import com.bakdata.conquery.models.execution.Taggable;
import com.bakdata.conquery.models.identifiable.Identifiable;
import com.bakdata.conquery.models.identifiable.ids.IId;
import com.bakdata.conquery.models.identifiable.ids.specific.GroupId;
import com.bakdata.conquery.util.QueryUtils;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.apache.shiro.authz.Permission;

@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
public class MetaDataPatch implements Taggable, Labelable, ShareInformation {

	private String[] tags;
	private String label;
	private Boolean shared;
	private List<GroupId> groups;
	
	/**
	 * Patches the given {@link Identifiable} by checking if the user holds the necessary Permission for that operation.
	 * Hence the patched instance must have a corresponding {@link Permission}-type.
	 * Tagging and Labeling only alters the state of the instance while sharing also alters the state of {@link Group}s.
	 * @param instance	The instance to patch
	 * @param storage	Storage that persists the instance and also auth information.
	 * @param user		The user on whose behalf the patch is executed
	 * @param permissionCreator	A function that produces a {@link Permission} that targets the given instance (e.g QueryPermission, FormConfigPermission).
	 * @param <INST>	Type of the instance that is patched
	 */
	public <T extends MetaDataPatch, ID extends IId<?>, INST extends Taggable & Shareable & Labelable & Identifiable<? extends ID>> void applyTo(INST instance, MetaStorage storage, User user, PermissionCreator<ID> permissionCreator){
		buildChain(
			QueryUtils.getNoOpEntryPoint(),
			storage,
			user,
			instance,
			permissionCreator)
			.accept(this);
	}
	
	protected <T extends MetaDataPatch, ID extends IId<?>, INST extends Taggable & Shareable & Labelable & Identifiable<? extends ID>> Consumer<T> buildChain(Consumer<T> patchConsumerChain,MetaStorage storage, User user, INST instance, PermissionCreator<ID> permissionCreator){
		if(getTags() != null && user.isPermitted(permissionCreator.apply(Ability.TAG.asSet(), instance.getId()))) {
			patchConsumerChain = patchConsumerChain.andThen(instance.tagger());
		}
		if(getLabel() != null && user.isPermitted(permissionCreator.apply(Ability.LABEL.asSet(), instance.getId()))) {
			patchConsumerChain = patchConsumerChain.andThen(instance.labeler());
		}
		if(getShared() != null && user.isPermitted(permissionCreator.apply(Ability.SHARE.asSet(), instance.getId()))) {
			patchConsumerChain = patchConsumerChain.andThen(instance.sharer(storage, user, permissionCreator));
		}
		return patchConsumerChain;
	}
	



	@FunctionalInterface
	public interface PermissionCreator<ID extends IId<?>> extends BiFunction<Set<Ability>,ID, ConqueryPermission> {
		
	}
}
