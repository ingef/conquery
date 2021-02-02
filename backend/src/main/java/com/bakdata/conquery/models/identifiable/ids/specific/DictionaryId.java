package com.bakdata.conquery.models.identifiable.ids.specific;

import java.util.List;

import javax.validation.constraints.NotNull;

import com.bakdata.conquery.models.dictionary.Dictionary;
import com.bakdata.conquery.models.identifiable.ids.AId;
import com.bakdata.conquery.models.identifiable.ids.IId;
import com.bakdata.conquery.models.identifiable.ids.IdIterator;
import com.bakdata.conquery.models.identifiable.ids.NamespacedId;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@AllArgsConstructor @Getter @EqualsAndHashCode(callSuper=false)
public class DictionaryId extends AId<Dictionary> implements NamespacedId {

	@NotNull
	private final DatasetId dataset;
	@NotNull
	private final String dictionary;
	
	@Override
	public void collectComponents(List<Object> components) {
		dataset.collectComponents(components);
		components.add(dictionary);
	}
	
	public static enum Parser implements IId.Parser<DictionaryId> {
		INSTANCE;
		
		@Override
		public DictionaryId parseInternally(IdIterator parts) {
			String dict = parts.next();
			return new DictionaryId(DatasetId.Parser.INSTANCE.parse(parts), dict);
		}
	}
}