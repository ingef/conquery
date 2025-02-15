package com.bakdata.conquery.apiv1.forms;

import java.time.LocalDateTime;
import java.util.UUID;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import com.bakdata.conquery.models.forms.configs.FormConfig;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.identifiable.ids.specific.UserId;
import com.bakdata.conquery.util.VariableDefaultValue;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;
import org.apache.commons.lang3.ArrayUtils;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FormConfigAPI {
	@NotEmpty
	private String formType;
	private String label;
	@NotNull @Builder.Default
	private String[] tags = ArrayUtils.EMPTY_STRING_ARRAY;
	@NotNull
	private JsonNode values;

	@VariableDefaultValue @NonNull @Builder.Default
	private UUID formId = UUID.randomUUID();
	@VariableDefaultValue @Builder.Default
	private LocalDateTime creationTime = LocalDateTime.now();

	public FormConfig intern(UserId owner, DatasetId dataset) {
		FormConfig intern = new FormConfig();
		intern.setFormId(formId);
		intern.setFormType(formType);
		intern.setLabel(label);
		intern.setTags(tags);
		intern.setValues(values);
		intern.setCreationTime(creationTime);
		intern.setOwner(owner);
		intern.setDataset(dataset);
		return intern;

	}
}
