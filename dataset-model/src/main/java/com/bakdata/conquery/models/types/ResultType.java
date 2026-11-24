package com.bakdata.conquery.models.types;

import java.util.Objects;

import com.bakdata.conquery.models.datasets.MajorTypeId;
import com.fasterxml.jackson.annotation.JsonCreator;

public sealed interface ResultType permits ResultType.Primitive, ResultType.ListT {

	static ResultType resolveResultType(MajorTypeId majorTypeId) {
		return switch (majorTypeId) {
			case STRING -> Primitive.STRING;
			case BOOLEAN -> Primitive.BOOLEAN;
			case DATE -> Primitive.DATE;
			case DATE_RANGE -> Primitive.DATE_RANGE;
			case INTEGER -> Primitive.INTEGER;
			case MONEY -> Primitive.MONEY;
			case DECIMAL, REAL -> Primitive.NUMERIC;
		};
	}

	String typeInfo();

	enum Primitive implements ResultType {
		BOOLEAN,
		INTEGER,
		NUMERIC,
		DATE,
		DATE_RANGE,
		STRING,
		MONEY;

		@Override
		public String toString() {
			return typeInfo();
		}

		@Override
		public String typeInfo() {
			return name();
		}
	}

	final class ListT implements ResultType {

		private final ResultType elementType;

		@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
		public ListT(ResultType elementType) {
			this.elementType = Objects.requireNonNull(elementType, "elementType is marked non-null but is null");
		}

		public ResultType getElementType() {
			return elementType;
		}

		@Override
		public boolean equals(Object other) {
			return this == other || other instanceof ListT list && elementType.equals(list.elementType);
		}

		@Override
		public int hashCode() {
			return elementType.hashCode();
		}

		@Override
		public String toString() {
			return typeInfo();
		}

		@Override
		public String typeInfo() {
			return "LIST[" + elementType.typeInfo() + "]";
		}
	}
}
