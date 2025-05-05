package com.bakdata.conquery.models.identifiable.ids;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import com.bakdata.conquery.models.identifiable.IdentifiableImpl;
import com.bakdata.conquery.models.identifiable.NamespacedStorageProvider;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.google.common.base.CharMatcher;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

public abstract class NamespacedIdentifiable<ID extends NamespacedId<?>>
		extends IdentifiableImpl<ID> {
	private static final CharMatcher DEFAULT_NAME_UNWANTED = CharMatcher.is(IdUtil.JOIN_CHAR).or(CharMatcher.whitespace());


	//TODO move into classes
	/**
	 * shown in the frontend
	 *
	 * @jsonExample "someLabel"
	 */
	private String label;

	//TODO move into classes
	@Getter(onMethod_ = {@ToString.Include, @NotBlank})
	@Setter
	private String name;

	private static String makeDefaultName(String label) {
		return DEFAULT_NAME_UNWANTED.replaceFrom(label.toLowerCase(), "_");
	}

	public abstract DatasetId getDataset();


	public final void setLabel(String label) {
		if (label == null) {
			return;
		}

		this.label = label;
		if (getName() == null) {
			setName(makeDefaultName(label));
		}
	}

	@NotEmpty
	@ToString.Include
	public String getLabel() {
		if (label == null) {
			return getName();
		}

		return label;
	}

	protected NamespacedStorageProvider getStorageProvider(){
		//TODO make field?
		return getDataset().getNamespacedStorageProvider();
	}

	@Override
	protected void injectStore(ID id) {
		NamespacedStorageProvider storageProvider = getStorageProvider();
		id.setNamespacedStorageProvider(storageProvider);
	}
}
