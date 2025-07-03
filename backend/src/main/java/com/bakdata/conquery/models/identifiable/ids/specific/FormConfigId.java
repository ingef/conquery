package com.bakdata.conquery.models.identifiable.ids.specific;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.bakdata.conquery.models.auth.permissions.Ability;
import com.bakdata.conquery.models.auth.permissions.ConqueryPermission;
import com.bakdata.conquery.models.auth.permissions.FormConfigPermission;
import com.bakdata.conquery.models.execution.Owned;
import com.bakdata.conquery.models.forms.configs.FormConfig;
import com.bakdata.conquery.models.identifiable.ids.Id;
import com.bakdata.conquery.models.identifiable.ids.IdIterator;
import com.bakdata.conquery.models.identifiable.ids.IdUtil;
import com.bakdata.conquery.models.identifiable.ids.MetaId;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@AllArgsConstructor
@Getter
@EqualsAndHashCode(callSuper = false)
public class FormConfigId extends MetaId<FormConfig> implements Owned {


	private final DatasetId dataset;
	private String formType;
	private UUID id;

	@Override
	public void collectComponents(List<Object> components) {
		components.add(dataset);
		components.add(formType);
		components.add(id);

	}

	@Override
	public void collectIds(Collection<Id<?, ?>> collect) {
		collect.add(this);
		dataset.collectIds(collect);
	}

	@Override
	public FormConfig get() {
		return getDomain().getFormConfig(this);
	}

	@Override
	public UserId getOwner() {
		return resolve().getOwner();
	}

	@Override
	public ConqueryPermission createPermission(Set<Ability> abilities) {
		return FormConfigPermission.onInstance(abilities, this);
	}

	public enum Parser implements IdUtil.Parser<FormConfigId> {
		INSTANCE;

		@Override
		public FormConfigId parseInternally(IdIterator parts) {
			UUID id = UUID.fromString(parts.next());
			String formType = parts.next();
			return new FormConfigId(DatasetId.Parser.INSTANCE.parse(parts), formType, id);
		}
	}
}
