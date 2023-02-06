package com.bakdata.conquery.models.identifiable.ids.specific;

import java.util.List;

import com.bakdata.conquery.models.identifiable.ids.Id;
import com.bakdata.conquery.models.identifiable.ids.IdIterator;
import com.bakdata.conquery.models.identifiable.ids.IdUtil;
import com.bakdata.conquery.models.identifiable.ids.NamespacedId;
import com.bakdata.conquery.models.index.InternToExternMapper;
import com.bakdata.conquery.models.index.search.SearchIndex;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@EqualsAndHashCode
public class SearchIndexId extends Id<SearchIndex> implements NamespacedId {
	@Getter
	private final DatasetId dataset;
	private final String name;

	@Override
	public void collectComponents(List<Object> components) {
		dataset.collectComponents(components);
		components.add(name);
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
