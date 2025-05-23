package com.bakdata.conquery.models.forms.managed;

import java.util.function.Consumer;

import com.bakdata.conquery.apiv1.forms.Form;
import com.bakdata.conquery.apiv1.forms.FormConfigAPI;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.models.forms.configs.FormConfig;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.identifiable.ids.specific.UserId;
import com.bakdata.conquery.models.query.PrintSettings;
import com.bakdata.conquery.models.query.Visitable;
import com.bakdata.conquery.models.worker.DatasetRegistry;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.DatabindContext;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

/**
 * Internal runtime representation of a form query.
 */
@ToString
@Slf4j
@EqualsAndHashCode(callSuper = true)
@CPSType(id = "MANAGED_FORM", base = ManagedExecution.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class ManagedForm<F extends Form> extends ManagedExecution {

	/**
	 * The submitted form for this execution.
	 *
	 * @implNote We use the type {@link Form} here rather than the type parameter F.
	 * Using F causes the class to have a concrete type at runtime which in turn skips
	 * the object inspection of Jackson to look at the actual <code>type</code> member of the object (see {@link com.bakdata.conquery.io.cps.CPSTypeIdResolver#typeFromId(DatabindContext, String)}).
	 * This causes a problem, when the object uses types with {@link com.bakdata.conquery.io.cps.SubTyped},
	 * as the subtype is only added to the {@link com.fasterxml.jackson.databind.DeserializationContext}, when the
	 * type is derived from the <pre>type</pre> member not when Jackson can just infer the deserializer from the type of
	 * this property.
	 */
	@Getter
	private Form submittedForm;

	protected ManagedForm(F submittedForm, UserId owner, DatasetId submittedDataset, MetaStorage storage, DatasetRegistry<?> datasetRegistry, ConqueryConfig config) {
		super(owner, submittedDataset, storage, datasetRegistry, config);
		this.submittedForm = submittedForm;
	}


	@Override
	public void start() {
		synchronized (this) {
			super.start();

			if (getSubmittedForm().getValues() != null) {
				// save as formConfig
				final FormConfigAPI build = FormConfigAPI.builder().formType(getSubmittedForm().getFormType())
														 .label(this.getLabelWithoutAutoLabelSuffix())
														 .tags(this.getTags())
														 .values(getSubmittedForm().getValues()).build();

				final FormConfig formConfig = build.intern(getOwner(), getDataset());

				getMetaStorage().addFormConfig(formConfig);
			}
		}
	}

	@Override
	public void visit(Consumer<Visitable> visitor) {
		submittedForm.visit(visitor);
	}


	@Override
	@JsonIgnore
	public F getSubmitted() {
		return (F) submittedForm;
	}




	@Override
	protected String makeDefaultLabel(PrintSettings cfg) {
		return getSubmittedForm().getLocalizedTypeLabel();
	}

}
