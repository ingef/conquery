package com.bakdata.conquery.models.forms.arx;

import java.util.HashSet;
import java.util.Set;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.deidentifier.arx.AttributeType;

/**
 * Helper classes that allow us to gather values for possible attribute hierarchies, while
 * we convert from the internal conquery result format to the ARX {@link org.deidentifier.arx.Data} format.
 */
public interface AttributeTypeBuilder {

	void register(String value);

	AttributeType build();

	/**
	 * Builds a two level hierarchy. Bottom level are the actual values. Top level is "*".
	 */
	class Flat implements AttributeTypeBuilder {

		private final Set<String> values = new HashSet<>();

		@Override
		public void register(String value) {
			values.add(value);
		}

		@Override
		public AttributeType build() {
			return AttributeType.Hierarchy.create(values.stream().map(v -> new String[]{v, "*"}).toArray(String[][]::new));
		}
	}

	/**
	 * Can be used in conjunction with {@link com.bakdata.conquery.models.types.SemanticType.IdentificationT}
	 *
	 * @implNote this might be only of use internal, because serialization might produce random objects.
	 * This can be useful for certain columns that are programmatically generated and then flagged with
	 * {@link AttributeType#INSENSITIVE_ATTRIBUTE} or {@link AttributeType#IDENTIFYING_ATTRIBUTE}.
	 * It can be also useful for select generated columns which want to provide their own {@link AttributeType.Hierarchy} and thereby becoming quasi-sensitive (see also {@link AttributeType#QUASI_IDENTIFYING_ATTRIBUTE}).
	 */
	@RequiredArgsConstructor
	class Fixed implements AttributeTypeBuilder {

		@NonNull
		private final AttributeType attributeType;

		@Override
		public void register(String value) {
			// Do nothing, this won't be a hierarchy
		}

		@Override
		public AttributeType build() {
			return attributeType;
		}
	}
}
