package io.github.solaris.jaxrs.client.test.util;

import org.assertj.core.configuration.Configuration;

public class AssertJConfig extends Configuration {

    @Override
    public int maxStackTraceElementsDisplayed() {
        return Integer.MAX_VALUE;
    }
}
