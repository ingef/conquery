package com.bakdata.conquery.models.identifiable.mapping;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * This class holds the csvId of an Entity.
 */
@Data @RequiredArgsConstructor(onConstructor_=@JsonCreator)
public class CsvEntityId {

	/**
	 * The csvId of an entity.
	 */
	@Getter(onMethod_=@JsonValue)
	private final String csvId;
}