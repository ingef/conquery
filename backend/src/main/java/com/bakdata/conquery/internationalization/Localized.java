package com.bakdata.conquery.internationalization;

import java.util.Locale;
import java.util.function.BiFunction;

import com.bakdata.conquery.io.cps.CPSBase;

/**
 * Interface for implementations, that can provide a description of themselves in an internationalized format.
 * Implementations that implement this interface should use the {@link c10n.C10N}-Library to produce their international
 * format. This allows automatically for a fallback locale without extra handling.
 *
 * Implementations should use this interface with the explicit localization provided, instead of overriding the {@link Object#toString()}-method
 * and retrieving the locale from a static thread-state in {@link com.bakdata.conquery.models.i18n.I18n}.
 * This is improves readability and intention of the code.
 */
public interface Localized {

    /**
     * Provide a localized string representation of the object, otherwise fallback to the default locale.
     * @param locale the localization to use
     * @return the localization or a fallback.
     */
    String toString(Locale locale);

	@CPSBase
    interface Provider extends BiFunction<Object,Locale,String>{}
}
