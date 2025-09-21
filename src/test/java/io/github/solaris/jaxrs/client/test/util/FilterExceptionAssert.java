package io.github.solaris.jaxrs.client.test.util;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.ws.rs.ProcessingException;

import org.assertj.core.api.AbstractThrowableAssert;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;

public sealed interface FilterExceptionAssert {
    AbstractThrowableAssert<?, ? extends Throwable> assertThatThrownBy(ThrowingCallable throwingCallable);

    final class CxfFilterExceptionAssert implements FilterExceptionAssert {

        @Override
        public AbstractThrowableAssert<?, ? extends Throwable> assertThatThrownBy(ThrowingCallable throwingCallable) {
            try {
                throwingCallable.call();
            } catch (Throwable t) {
                if (t instanceof Error error) {
                    return assertThat(error);
                }
                return assertThat(t)
                        .isInstanceOf(ProcessingException.class)
                        .cause();
            }

            throw new AssertionError("Expected callable to throw an exception");
        }
    }

    final class CxfMicroProfileFilterExceptionAssert implements FilterExceptionAssert {

        @Override
        public AbstractThrowableAssert<?, ? extends Throwable> assertThatThrownBy(ThrowingCallable throwingCallable) {
            return Assertions.assertThatThrownBy(throwingCallable)
                    .isInstanceOf(RuntimeException.class)
                    .cause();
        }
    }

    final class DefaultFilterExceptionAssert implements FilterExceptionAssert {

        @Override
        public AbstractThrowableAssert<?, ? extends Throwable> assertThatThrownBy(ThrowingCallable throwingCallable) {
            return Assertions.assertThatThrownBy(throwingCallable)
                    .isInstanceOf(ProcessingException.class)
                    .cause();
        }
    }
}
