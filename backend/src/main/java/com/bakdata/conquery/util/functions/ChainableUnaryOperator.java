package com.bakdata.conquery.util.functions;

import java.util.Objects;
import java.util.function.Function;
import java.util.function.UnaryOperator;

/**
 * Adds convenience methods to chain {@link UnaryOperator}s and still have them typed as
 * an {@link UnaryOperator} instead of an {@link Function}.
 * This way the chain can be used with {@link List::replaceAll} for efficient replacement. 
 * 
 * @param <T> The input and output type of the operator
 */
@FunctionalInterface
public interface ChainableUnaryOperator<T> extends UnaryOperator<T>{
	
    /**
     * Returns a composed function that first applies the {@code before}
     * function to its input, and then applies this function to the result.
     * If evaluation of either function throws an exception, it is relayed to
     * the caller of the composed function.
     *
     * @param before the function to apply before this function is applied
     * @return a composed function that first applies the {@code before}
     * function and then applies this function
     * @throws NullPointerException if before is null
     *
     * @see #unaryAndThen(Function)
     */
    default ChainableUnaryOperator<T> unaryCompose(Function<? super T, ? extends T> before) {
        Objects.requireNonNull(before);
        return (T t) -> apply(before.apply(t));
    }

    /**
     * Returns a composed function that first applies this function to
     * its input, and then applies the {@code after} function to the result.
     * If evaluation of either function throws an exception, it is relayed to
     * the caller of the composed function.
     *
     * @param after the function to apply after this function is applied
     * @return a composed function that first applies this function and then
     * applies the {@code after} function
     * @throws NullPointerException if after is null
     *
     * @see #unaryCompose(Function)
     */
    default ChainableUnaryOperator<T> unaryAndThen(Function<? super T, ? extends T> after) {
        Objects.requireNonNull(after);
        return (T t) -> after.apply(apply(t));
    }

}
