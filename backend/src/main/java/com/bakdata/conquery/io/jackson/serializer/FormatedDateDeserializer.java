package com.bakdata.conquery.io.jackson.serializer;

import com.bakdata.conquery.util.DateFormats;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import lombok.SneakyThrows;

import java.io.IOException;
import java.time.LocalDate;

public class FormatedDateDeserializer extends StdDeserializer<LocalDate> {

    private final DateFormats formats;

    public FormatedDateDeserializer(DateFormats formats) {
        super(LocalDate.class);
        this.formats = formats;
    }

    @SneakyThrows
    @Override
    public LocalDate deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        return formats.parseToLocalDate(p.getText());
    }
}
