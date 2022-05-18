package com.bakdata.conquery.models.identifiable.ids.specific;

import java.util.List;
import java.util.UUID;

import com.bakdata.conquery.models.forms.configs.FormConfig;
import com.bakdata.conquery.models.identifiable.ids.AId;
import com.bakdata.conquery.models.identifiable.ids.IdUtil;
import com.bakdata.conquery.models.identifiable.ids.IdIterator;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@AllArgsConstructor @Getter @EqualsAndHashCode(callSuper=false)
public class FormConfigId extends AId<FormConfig> {
	

	private final DatasetId dataset;
	private String formType;
	private UUID id;

	@Override
	public void collectComponents(List<Object> components) {
		components.add(dataset);
		components.add(formType);
		components.add(id);
		
	}

	public static enum Parser implements IdUtil.Parser<FormConfigId> {
		INSTANCE;

		@Override
		public FormConfigId parseInternally(IdIterator parts) {
			UUID id = UUID.fromString(parts.next());
			String formType = parts.next();
			return new FormConfigId(DatasetId.Parser.INSTANCE.parse(parts), formType, id);
		}
	}
}
