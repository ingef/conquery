package com.bakdata.conquery.models.externalservice;

import com.bakdata.conquery.io.cps.CPSBase;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.forms.util.DateContext;
import com.bakdata.conquery.models.query.PrintSettings;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.NonNull;

import java.math.BigDecimal;

@JsonTypeInfo(use = JsonTypeInfo.Id.CUSTOM, property = "type")
@CPSBase
public interface ResultType {

    default String printNullable(PrintSettings cfg, Object f) {
        if (f == null) {
            return "";
        }
        return print(cfg, f);
    }

    default String print(PrintSettings cfg, @NonNull Object f) {
        return f.toString();
    }


    String typeInfo();

    static abstract class AbstractSimpleResultType implements ResultType {
        @Override
        public String typeInfo() {
            return this.getClass().getAnnotation(CPSType.class).id();
        }
    }

    @CPSType(id = "BOOLEAN", base = ResultType.class)
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class BooleanT extends AbstractSimpleResultType {
        public final static BooleanT INSTANCE = new BooleanT();

        @Override
        public String print(PrintSettings cfg, Object f) {
            if (f instanceof java.lang.Boolean) {
                return (java.lang.Boolean) f ? "t" : "f";
            }
            return "";
        }


        @JsonCreator
        private static BooleanT getInstance() {
            return INSTANCE;
        }
    }


    @CPSType(id = "INTEGER", base = ResultType.class)
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class IntegerT extends AbstractSimpleResultType {
        public final static IntegerT INSTANCE = new IntegerT();

        @Override
        public String print(PrintSettings cfg, Object f) {
            if (cfg.isPrettyPrint()) {
                return cfg.getIntegerFormat().format(((Number) f).longValue());
            }
            return f.toString();
        }


        @JsonCreator
        private static IntegerT getInstance() {
            return INSTANCE;
        }
    }

    @CPSType(id = "NUMERIC", base = ResultType.class)
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class NumericT extends AbstractSimpleResultType {
        public final static NumericT INSTANCE = new NumericT();

        @Override
        public String print(PrintSettings cfg, Object f) {
            if(cfg.isPrettyPrint()) {
                return cfg.getDecimalFormat().format(f);
            }
            return f.toString();
        }


        @JsonCreator
        private static NumericT getInstance() {
            return INSTANCE;
        }
    }

    @CPSType(id = "CATEGORICAL", base = ResultType.class)
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class CategoricalT extends AbstractSimpleResultType {
        public final static CategoricalT INSTANCE = new CategoricalT();


        @JsonCreator
        private static CategoricalT getInstance() {
            return INSTANCE;
        }
    }

    @CPSType(id = "RESOLUTION", base = ResultType.class)
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class ResolutionT extends AbstractSimpleResultType {
        public final static ResolutionT INSTANCE = new ResolutionT();

        @Override
        public String print(PrintSettings cfg, Object f) {
            if (f instanceof DateContext.Resolution) {
                return ((DateContext.Resolution) f).toString(cfg.getLocale());
            }
            try {
                // If the object was parsed as a simple string, try to convert it to a
                // DateContextMode to get Internationalization
                return DateContext.Resolution.valueOf(f.toString()).toString(cfg.getLocale());
            } catch (Exception e) {
                throw new IllegalArgumentException(f + " is not a valid resolution.", e);
            }
        }

        @JsonCreator
        private static ResolutionT getInstance() {
            return INSTANCE;
        }
    }

    @CPSType(id = "DATE", base = ResultType.class)
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class DateT extends AbstractSimpleResultType {
        public final static DateT INSTANCE = new DateT();

    }

    @CPSType(id = "STRING", base = ResultType.class)
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class StringT extends AbstractSimpleResultType {
        public final static StringT INSTANCE = new StringT();

        @JsonCreator
        private static StringT getInstance() {
            return INSTANCE;
        }
    }

    @CPSType(id = "MONEY", base = ResultType.class)
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class MoneyT extends AbstractSimpleResultType {
        private static final int CURRENCY_DIGITS = ConqueryConfig.getInstance().getLocale().getCurrency().getDefaultFractionDigits();

        public final static MoneyT INSTANCE = new MoneyT();

        @Override
        public String print(PrintSettings cfg, Object f) {
            if (cfg.isPrettyPrint()) {
                return cfg.getDecimalFormat().format(new BigDecimal(((Number) f).longValue()).movePointLeft(CURRENCY_DIGITS));
            }
            return IntegerT.INSTANCE.print(cfg, f);
        }

        @JsonCreator
        private static MoneyT getInstance() {
            return INSTANCE;
        }
    }

    @CPSType(id = "LIST", base = ResultType.class)
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    public static class ListT implements ResultType {
        @NonNull
        private final ResultType elementType;

        @Override
        public String typeInfo() {
            return this.getClass().getAnnotation(CPSType.class).id() + "[" + elementType.typeInfo() + "]";
        }

        @JsonCreator
        public static ListT ofType(@NonNull ResultType elementType) {
            return new ListT(elementType);
        }
    }
}
