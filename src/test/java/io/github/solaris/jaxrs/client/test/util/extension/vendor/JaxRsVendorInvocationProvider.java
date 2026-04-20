package io.github.solaris.jaxrs.client.test.util.extension.vendor;

import static io.github.solaris.jaxrs.client.test.util.extension.vendor.JaxRsVendor.CXF_JACKSON3;
import static io.github.solaris.jaxrs.client.test.util.extension.vendor.JaxRsVendor.RESTEASY_REACTIVE;
import static io.github.solaris.jaxrs.client.test.util.extension.vendor.JaxRsVendor.VENDORS;
import static java.util.Collections.singletonList;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestTemplateInvocationContext;
import org.junit.jupiter.api.extension.TestTemplateInvocationContextProvider;
import org.junit.platform.commons.support.AnnotationSupport;

@NullMarked
class JaxRsVendorInvocationProvider implements TestTemplateInvocationContextProvider {

    @Override
    public boolean supportsTestTemplate(ExtensionContext context) {
        return context.getTestMethod().isPresent();
    }

    @Override
    public Stream<TestTemplateInvocationContext> provideTestTemplateInvocationContexts(ExtensionContext context) {
        List<JaxRsVendor> skipFor = Arrays.asList(AnnotationSupport.findAnnotation(context.getRequiredTestMethod(), JaxRsVendorTest.class)
                .orElseThrow()
                .skipFor());
        return VENDORS.stream()
                .filter(vendor -> !skipFor.contains(vendor))
                .filter(vendor -> checkRunInQuarkus(context, vendor))
                .filter(vendor -> checkJackson3(context, vendor))
                .map(vendor -> new TestTemplateInvocationContext() {
                    @Override
                    public String getDisplayName(int invocationIndex) {
                        return vendor.name();
                    }

                    @Override
                    public List<Extension> getAdditionalExtensions() {
                        return singletonList(new JaxRsVendorTestExtension(vendor));
                    }
                });
    }

    private static boolean checkRunInQuarkus(ExtensionContext context, JaxRsVendor vendor) {
        if (vendor != RESTEASY_REACTIVE) {
            return true;
        }
        return !AnnotationSupport.isAnnotated(context.getRequiredTestClass(), RunInQuarkus.class);
    }

    private static boolean checkJackson3(ExtensionContext context, JaxRsVendor vendor) {
        if (vendor == CXF_JACKSON3) {
            return AnnotationSupport.isAnnotated(context.getRequiredTestClass(), EnableJackson3.class)
                    || AnnotationSupport.isAnnotated(context.getRequiredTestMethod(), EnableJackson3.class);
        }
        return true;
    }
}
