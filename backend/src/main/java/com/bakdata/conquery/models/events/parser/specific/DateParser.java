package com.bakdata.conquery.models.events.parser.specific;

import java.nio.IntBuffer;

import javax.annotation.Nonnull;

import com.bakdata.conquery.models.common.CDate;
import com.bakdata.conquery.models.config.ParserConfig;
import com.bakdata.conquery.models.events.parser.Parser;
import com.bakdata.conquery.models.events.stores.primitive.IntegerDateStore;
import com.bakdata.conquery.models.events.stores.root.DateStore;
import com.bakdata.conquery.models.events.stores.root.IntegerStore;
import com.bakdata.conquery.models.exceptions.ParsingException;
import com.bakdata.conquery.util.DateFormats;
import lombok.SneakyThrows;
import lombok.ToString;

@ToString(callSuper = true)
public class DateParser extends Parser<Integer, DateStore> {

	private IntegerParser subType;

	public DateParser(ParserConfig config) {
		super(config);
		subType = new IntegerParser(config);
	}

	@Override
	public void setLines(int lines) {
		super.setLines(lines);
		subType.setLines(lines);
	}

	@Override
	protected Integer parseValue(@Nonnull String value) throws ParsingException {
		return CDate.ofLocalDate(DateFormats.parseToLocalDate(value));
	}

	@Override
	public Integer addLine(Integer v) {
		if(v == null){
			subType.addLine(null);
			return super.addLine(null);
		}

		super.addLine(v);

		return subType.addLine(v.longValue()).intValue();
	}

	@Override
	protected DateStore decideType() {
		IntegerStore subDecision = subType.findBestType();
		return new IntegerDateStore(subDecision);
	}

	@Override
	public void setValue(DateStore store, int event, Integer value) {
		store.setDate(event, value);
	}

	@SneakyThrows
	@Override
	public ColumnValues createColumnValues() {
		return new ColumnValues<Integer>(0) {
			private final IntBuffer buffer = ColumnValues.allocateBuffer().asIntBuffer();

			@Override
			public Integer get(int event) {
				return buffer.get(event);
			}

			@Override
			protected void write(int event, Integer obj) {
				buffer.put(obj);
			}
		};
	}

}
