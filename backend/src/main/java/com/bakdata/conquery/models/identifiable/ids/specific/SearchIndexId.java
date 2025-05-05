package com.bakdata.conquery.models.identifiable.ids.specific;

import java.util.Collection;
import java.util.List;

import com.bakdata.conquery.models.identifiable.NamespacedStorageProvider;
import com.bakdata.conquery.models.identifiable.ids.Id;
import com.bakdata.conquery.models.identifiable.ids.IdIterator;
import com.bakdata.conquery.models.identifiable.ids.IdUtil;
import com.bakdata.conquery.models.identifiable.ids.NamespacedId;
import com.bakdata.conquery.models.index.search.SearchIndex;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@EqualsAndHashCode
public class SearchIndexId extends NamespacedId<SearchIndex>  {
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
	public SearchIndex get(NamespacedStorageProvider storage) {
		return assertNamespaceStorage(storage.getStorage(getDataset())).getSearchIndex(this);
	}

	@Override
	public NamespacedStorageProvider getNamespacedStorageProvider() {
		return dataset.getNamespacedStorageProvider();
	}

	public enum Parser implements IdUtil.Parser<SearchIndexId> {
		INSTANCE;

		@Override
		public SearchIndexId parseInternally(IdIterator parts) {
			String tag = parts.next();
			DatasetId parent = DatasetId.Parser.INSTANCE.parse(parts);
			return new SearchIndexId(parent, tag);
		}
	}
}
