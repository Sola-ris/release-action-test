package io.github.solaris.jaxrs.client.test.request;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class ExpectedCountTest {

    @Test
    void testTimesLessThanOne() {
        assertThatThrownBy(() -> ExpectedCount.times(0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("'count' must be >= 1");
    }

    @Test
    void testMinLessThanOne() {
        assertThatThrownBy(() -> ExpectedCount.min(0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("'min' must be >= 1");
    }

    @Test
    void testMaxLessThanOne() {
        assertThatThrownBy(() -> ExpectedCount.max(0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("'max' must be >= 1");
    }

    @Test
    void testLowerBoundLessThanZero() {
        assertThatThrownBy(() -> ExpectedCount.between(-1, 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("'min' must be >= 0");
    }

    @Test
    void testLowerBoundGreaterThanUpperBound() {
        assertThatThrownBy(() -> ExpectedCount.between(Integer.MAX_VALUE, Integer.MIN_VALUE))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("'max' must be >= 'min'");
    }
}
