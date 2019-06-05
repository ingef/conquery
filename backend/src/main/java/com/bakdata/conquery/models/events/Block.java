package com.bakdata.conquery.models.events;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Map;

import com.bakdata.conquery.models.common.CDateSet;
import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.Import;
import com.bakdata.conquery.util.io.SmallOut;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializable;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor 
public abstract class Block implements JsonSerializable{

	@JsonBackReference @Getter @Setter
	private Bucket bucket;
	
	public abstract int size();
	
	public abstract boolean has(int event, Column column);

	public abstract int getString(int event, Column column);
	public abstract long getInteger(int event, Column column);
	public abstract boolean getBoolean(int event, Column column);
	public abstract double getReal(int event, Column column);
	public abstract BigDecimal getDecimal(int event, Column column);
	public abstract long getMoney(int event, Column column);
	public abstract int getDate(int event, Column column);
	public abstract CDateRange getDateRange(int event, Column column);
	public abstract Object getRaw(int event, Column column);
	public abstract Object getAsObject(int event, Column column);

	public abstract boolean eventIsContainedIn(int event, Column column, CDateRange dateRange);
	public abstract boolean eventIsContainedIn(int event, Column column, CDateSet dateRanges);
	public abstract CDateRange getAsDateRange(int event, Column currentColumn);

	@Override
	public void serializeWithType(JsonGenerator gen, SerializerProvider serializers, TypeSerializer typeSer) throws IOException {
		this.serialize(gen, serializers);
	}
	
	public abstract Map<String, Object> calculateMap(int event, Import imp);
	public abstract void writeContent(SmallOut output) throws IOException;


}
