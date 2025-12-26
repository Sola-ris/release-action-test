package io.github.solaris.jaxrs.client.test.util.extension.vendor;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.junit.jupiter.api.parallel.ExecutionMode.SAME_THREAD;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.Execution;

@TestTemplate
@Target(METHOD)
@Retention(RUNTIME)
@Execution(SAME_THREAD)
@ExtendWith(JaxRsVendorInvocationProvider.class)
public @interface JaxRsVendorTest {
    JaxRsVendor[] skipFor() default {};
}
