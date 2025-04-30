package com.bakdata.conquery.models.identifiable.ids.specific;

import java.util.Collection;
import java.util.List;

import com.bakdata.conquery.io.storage.NamespacedStorage;
import com.bakdata.conquery.models.identifiable.NamespacedStorageProvider;
import com.bakdata.conquery.models.identifiable.ids.Id;
import com.bakdata.conquery.models.identifiable.ids.IdIterator;
import com.bakdata.conquery.models.identifiable.ids.IdUtil;
import com.bakdata.conquery.models.identifiable.ids.NamespacedId;
import com.bakdata.conquery.models.index.InternToExternMapper;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@EqualsAndHashCode
public class InternToExternMapperId extends NamespacedId<InternToExternMapper>  {
	@Getter
	private final DatasetId dataset;
	private final String name;

	@Override
	public void collectComponents(List<Object> components) {
		dataset.collectComponents(components);
		components.add(name);
	}

	@Override
	public void collectIds(Collection<Id<?,?>> collect) {
		collect.add(this);
		dataset.collectIds(collect);
	}

	@Override
	public InternToExternMapper get(NamespacedStorage storage) {
		return assertNamespaceStorage(storage).getInternToExternMapper(this);
	}

	@Override
	public NamespacedStorageProvider getNamespacedStorageProvider() {
		return dataset.getNamespacedStorageProvider();
	}

	public enum Parser implements IdUtil.Parser<InternToExternMapperId> {
		INSTANCE;

		@Override
		public InternToExternMapperId parseInternally(IdIterator parts) {
			String tag = parts.next();
			DatasetId parent = DatasetId.Parser.INSTANCE.parse(parts);
			return new InternToExternMapperId(parent, tag);
		}
	}
}
