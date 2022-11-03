/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2020 Vladimir Orany.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.agorapulse.micronaut.grails;

import io.micronaut.context.ApplicationContext;
import io.micronaut.context.DefaultApplicationContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Map;

import static com.agorapulse.micronaut.grails.GrailsPropertyTranslatingCustomizer.collapse;
import static com.agorapulse.micronaut.grails.PropertyTranslatingCustomizer.grails;
import static com.agorapulse.micronaut.grails.PropertyTranslatingCustomizer.of;

/**
 * Adds Micronaut beans to a Grails' Spring application context.  This processor will
 * find all of the Micronaut beans of the specified types
 * and add them as beans to the Spring application context.
 * <p>
 * The Grails properties can be translated to Micronaut properties if required.
 *
 * @since 1.0
 *
 * @deprecated this class creates yet another {@link io.micronaut.context.ApplicationContext}, use the bridge or strict mode instead
 */
public class GrailsMicronautBeanProcessor extends DefaultGrailsMicronautBeanProcessor implements EnvironmentAware {

    static final Logger LOGGER = LoggerFactory.getLogger(GrailsMicronautBeanProcessor.class);

    /**
     * @deprecated please declare {@link MicronautBeanImporter} bean to avoid multiple Micronaut application context
     * inside single application.
     */
    public static class Builder extends MicronautBeanImporter {

        Builder(PropertyTranslatingCustomizer customizer) {
            customize(customizer);
        }

    }

    /**
     * Starts creation of bean processor using default {@link PropertyTranslatingCustomizer#grails()} customizer.
     * @return bean processor builder using {@link PropertyTranslatingCustomizer#grails()}
     * @deprecated please declare {@link MicronautBeanImporter} bean instead to avoid multiple Micronaut application context
     * inside single application.
     */
    public static GrailsMicronautBeanProcessor.Builder builder() {
        return builder(grails());
    }

    /**
     * Starts creation of bean processor using given customizer.
     * @param customizerBuilder customizer being used
     * @return bean processor builder using given cusotomizer
     * @deprecated please declare {@link MicronautBeanImporter} bean instead to avoid multiple Micronaut application context
     * inside single application.
     */
    public static GrailsMicronautBeanProcessor.Builder builder(PropertyTranslatingCustomizer.Builder customizerBuilder) {
        return builder(customizerBuilder.build());
    }

    /**
     * Starts creation of bean processor using given customizer.
     * @param customizer customizer being used
     * @return bean processor builder using given cusotomizer
     * @deprecated please declare {@link MicronautBeanImporter} bean instead to avoid multiple Micronaut application context
     * inside single application.
     */
    public static GrailsMicronautBeanProcessor.Builder builder(PropertyTranslatingCustomizer customizer) {
        return new Builder(customizer);
    }

    private final List<PropertyTranslatingCustomizer> customizers;
    private final List<String> expectedMapProperties;
    private Environment environment;

    /**
     * @param qualifiers the names and qualifiers of the Micronaut beans which should be added to the
     *                   Spring application context.
     * @param customizers properties translation customizer
     * @param expectedMapProperties list of properties' prefixes which should be converted to map
     */
    GrailsMicronautBeanProcessor(Map<String, TypeAndQualifier<?>> qualifiers, List<PropertyTranslatingCustomizer> customizers, List<String> expectedMapProperties) {
        super(qualifiers);
        this.customizers = customizers;
        this.expectedMapProperties = expectedMapProperties;
    }

    @Override
    protected ApplicationContext initializeMicronautContext() {
        if (environment == null) {
            throw new IllegalStateException("Spring environment not set!");
        }

        DefaultApplicationContext micronautContext = new GrailsPropertyTranslatingApplicationContext(environment, of(collapse(customizers)), expectedMapProperties);

        return micronautContext.start();
    }

    @Override
    public void setEnvironment(@Nonnull Environment environment) {
        this.environment = environment;
    }

}

