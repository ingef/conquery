package com.bakdata.conquery.apiv1;

import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.models.auth.entities.Group;
import com.bakdata.conquery.models.auth.entities.Subject;
import com.bakdata.conquery.models.auth.permissions.Ability;
import com.bakdata.conquery.models.auth.permissions.Authorized;
import com.bakdata.conquery.models.auth.permissions.ConqueryPermission;
import com.bakdata.conquery.models.execution.Labelable;
import com.bakdata.conquery.models.execution.Owned;
import com.bakdata.conquery.models.execution.Shareable;
import com.bakdata.conquery.models.execution.Shareable.ShareInformation;
import com.bakdata.conquery.models.execution.Taggable;
import com.bakdata.conquery.models.identifiable.Identifiable;
import com.bakdata.conquery.models.identifiable.ids.AId;
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
	private List<GroupId> groups;

	/**
	 * Patches the given {@link Identifiable} by checking if the subject holds the necessary Permission for that operation.
	 * Hence the patched instance must have a corresponding {@link Permission}-type.
	 * Tagging and Labeling only alters the state of the instance while sharing also alters the state of {@link Group}s.
	 *
	 * @param instance The instance to patch
	 * @param storage  Storage that persists the instance and also auth information.
	 * @param subject  The subject on whose behalf the patch is executed
	 * @param <INST>   Type of the instance that is patched
	 */
	public <T extends MetaDataPatch, ID extends AId<?>, INST extends Taggable & Shareable & Labelable & Identifiable<? extends ID> & Owned & Authorized> void applyTo(INST instance, MetaStorage storage, Subject subject) {
		buildChain(
				QueryUtils.getNoOpEntryPoint(),
				storage,
				subject,
				instance
		)
				.accept(this);
	}

	protected <T extends MetaDataPatch, ID extends AId<?>, INST extends Taggable & Shareable & Labelable & Identifiable<? extends ID> & Owned & Authorized> Consumer<T> buildChain(Consumer<T> patchConsumerChain, MetaStorage storage, Subject subject, INST instance) {
		if (getTags() != null && subject.isPermitted(instance, Ability.TAG)) {
			patchConsumerChain = patchConsumerChain.andThen(instance.tagger());
		}
		if (getLabel() != null && subject.isPermitted(instance, Ability.LABEL)) {
			patchConsumerChain = patchConsumerChain.andThen(instance.labeler());
		}
		if (getGroups() != null && subject.isPermitted(instance, Ability.SHARE)) {
			patchConsumerChain = patchConsumerChain.andThen(instance.sharer(storage, subject));
		}
		return patchConsumerChain;
	}


	@FunctionalInterface
	public interface PermissionCreator<ID extends AId<?>> extends BiFunction<Set<Ability>, ID, ConqueryPermission> {

	}
}
