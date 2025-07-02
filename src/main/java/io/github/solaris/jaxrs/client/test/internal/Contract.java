package io.github.solaris.jaxrs.client.test.internal;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.CLASS;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Target(METHOD)
@Retention(CLASS)
@interface Contract {
    String value();
}
