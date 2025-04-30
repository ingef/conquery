package com.bakdata.conquery.models.identifiable.ids.specific;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.models.auth.permissions.Ability;
import com.bakdata.conquery.models.auth.permissions.ConqueryPermission;
import com.bakdata.conquery.models.auth.permissions.ExecutionPermission;
import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.models.execution.Owned;
import com.bakdata.conquery.models.identifiable.ids.Id;
import com.bakdata.conquery.models.identifiable.ids.IdIterator;
import com.bakdata.conquery.models.identifiable.ids.IdUtil;
import com.bakdata.conquery.models.identifiable.ids.MetaId;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@AllArgsConstructor
@Getter
@EqualsAndHashCode(callSuper = false, doNotUseGetters = true)
public class ManagedExecutionId extends MetaId<ManagedExecution> implements Owned {

	private final DatasetId dataset;
	private final UUID execution;

	@Override
	public void collectComponents(List<Object> components) {
		dataset.collectComponents(components);
		components.add(execution);
	}

	@Override
	public void collectIds(Collection<Id<?,?>> collect) {
		collect.add(this);
		dataset.collectIds(collect);
	}

	@Override
	public ManagedExecution get(MetaStorage storage) {
		return storage.getExecution(this);
	}

	@Override
	public ConqueryPermission createPermission(Set<Ability> abilities) {
		return ExecutionPermission.onInstance(abilities, this);
	}

	@Override
	public UserId getOwner() {
		return resolve().getOwner();
	}

	public enum Parser implements IdUtil.Parser<ManagedExecutionId> {
		INSTANCE;

		@Override
		public ManagedExecutionId parseInternally(IdIterator parts) {
			UUID query = UUID.fromString(parts.next());
			return new ManagedExecutionId(DatasetId.Parser.INSTANCE.parse(parts), query);
		}
	}
}
