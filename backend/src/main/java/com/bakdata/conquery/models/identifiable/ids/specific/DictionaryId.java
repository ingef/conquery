package com.bakdata.conquery.models.identifiable.ids.specific;

import java.util.List;

import javax.validation.constraints.NotNull;

import com.bakdata.conquery.models.dictionary.Dictionary;
import com.bakdata.conquery.models.identifiable.ids.Id;
import com.bakdata.conquery.models.identifiable.ids.IdUtil;
import com.bakdata.conquery.models.identifiable.ids.IdIterator;
import com.bakdata.conquery.models.identifiable.ids.NamespacedId;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@AllArgsConstructor
@Getter
@EqualsAndHashCode(callSuper = false)
public class DictionaryId extends Id<Dictionary> implements NamespacedId {

	@NotNull
	private final DatasetId dataset;
	@NotNull
	private final String name;

	@Override
	public void collectComponents(List<Object> components) {
		dataset.collectComponents(components);
		components.add(name);
	}

	public static enum Parser implements IdUtil.Parser<DictionaryId> {
		INSTANCE;

		@Override
		public DictionaryId parseInternally(IdIterator parts) {
			String dict = parts.next();
			return new DictionaryId(DatasetId.Parser.INSTANCE.parse(parts), dict);
		}
	}
}