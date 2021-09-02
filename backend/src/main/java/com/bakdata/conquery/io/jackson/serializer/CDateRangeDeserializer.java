package com.bakdata.conquery.io.jackson.serializer;

import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.preproc.parser.specific.DateRangeParser;
import com.bakdata.conquery.util.DateReader;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import lombok.SneakyThrows;

import java.io.IOException;

public class CDateRangeDeserializer extends StdDeserializer<CDateRange> {

    private final DateReader dateReader;

    protected CDateRangeDeserializer(DateReader dateReader) {
        super(CDateRange.class);
        this.dateReader = dateReader;
    }

    @SneakyThrows
    @Override
    public CDateRange deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
        return dateReader.parseToCDateRange(p.getText());
    }
}
