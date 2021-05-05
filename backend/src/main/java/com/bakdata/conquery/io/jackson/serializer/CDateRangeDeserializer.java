package com.bakdata.conquery.io.jackson.serializer;

import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.preproc.parser.specific.DateRangeParser;
import com.bakdata.conquery.util.DateFormats;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import lombok.SneakyThrows;

import java.io.IOException;

public class CDateRangeDeserializer extends StdDeserializer<CDateRange> {

    private final DateFormats dateFormats;

    protected CDateRangeDeserializer(DateFormats dateFormats) {
        super(CDateRange.class);
        this.dateFormats = dateFormats;
    }

    @SneakyThrows
    @Override
    public CDateRange deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
        return DateRangeParser.parseISORange(p.getText(), dateFormats);
    }
}
