package com.bakdata.conquery.models.config;

import com.bakdata.conquery.util.DateReader;
import lombok.Data;
import lombok.Setter;

import javax.validation.constraints.NotNull;

import java.util.*;

@Data
@Setter
public class ParserConfig {
    /**
     * Minimum required double precision to switch from floats to doubles.
     * If set to a positive value, preprocessing will select float values for columns with at least minPrecision ulp.
     *
     * @see com.bakdata.conquery.models.preproc.parser.specific.RealParser#decideType()
     * @see Math#ulp(float)
     */
    private double minPrecision = Double.MIN_VALUE;

    /**
     * The currency type of currency values that are parsed and processed. For now there can only be one currency in
     * an instance.
     */
    @NotNull
    private Currency currency = Currency.getInstance("EUR");
}
