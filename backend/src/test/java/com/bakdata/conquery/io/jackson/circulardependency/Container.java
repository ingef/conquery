package com.bakdata.conquery.io.jackson.circulardependency;

import java.util.List;
import java.util.UUID;

import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.identifiable.Identifiable;
import com.bakdata.conquery.models.identifiable.ids.AId;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.hpsf.GUID;
import com.bakdata.conquery.models.*;
@Slf4j
@FieldNameConstants

@Setter
@Getter
@RequiredArgsConstructor(onConstructor_ = {@JsonCreator})

public class Container {


	@JsonManagedReference
	private  IChild[] containerChildren;




	/*
	@JsonCreator
	public static Container create(IChild[] containerChildren)
	{
		Container container = new Container();
		container.setContainerChildren(containerChildren);
		return container;
	}
	*/
}
