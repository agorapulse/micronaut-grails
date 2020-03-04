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
import io.micronaut.inject.BeanDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;

import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

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
 */
public class GrailsMicronautBeanProcessor implements BeanFactoryPostProcessor, DisposableBean, EnvironmentAware {

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

    private static final String MICRONAUT_BEAN_TYPE_PROPERTY_NAME = "micronautBeanType";
    private static final String MICRONAUT_CONTEXT_PROPERTY_NAME = "micronautContext";
    private static final String MICRONAUT_SINGLETON_PROPERTY_NAME = "micronautSingleton";

    private DefaultApplicationContext micronautContext;
    private final Map<String, Function<ApplicationContext, Optional<BeanDefinition<?>>>> suppliers;
    private final List<PropertyTranslatingCustomizer> customizers;
    private Environment environment;

    /**
     * @param suppliers the functions to retrieve beans from the application context
     * @param customizers properties translation customizer
     */
    GrailsMicronautBeanProcessor(Map<String, Function<ApplicationContext, Optional<BeanDefinition<?>>>> suppliers, List<PropertyTranslatingCustomizer> customizers) {
        this.suppliers = suppliers;
        this.customizers = customizers;
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        if (environment == null) {
            throw new IllegalStateException("Spring environment not set!");
        }

        micronautContext = new GrailsPropertyTranslatingApplicationContext(environment, of(collapse(customizers)));

        micronautContext.start();

        NoClassDefFoundError noClassDefFoundError = null;

        for (Map.Entry<String, Function<ApplicationContext, Optional<BeanDefinition<?>>>> entry : suppliers.entrySet()) {
            String name = entry.getKey();
            Function<ApplicationContext, Optional<BeanDefinition<?>>> supplier = entry.getValue();
            try {
                Optional<BeanDefinition<?>> firstBean = supplier.apply(micronautContext);

                BeanDefinition<?> definition = firstBean.orElseThrow(()-> new IllegalArgumentException("There is no candidate for bean " + name + " declared in " + supplier));

                final BeanDefinitionBuilder beanDefinitionBuilder = BeanDefinitionBuilder
                    .rootBeanDefinition(GrailsMicronautBeanFactory.class);
                beanDefinitionBuilder.addPropertyValue(MICRONAUT_BEAN_TYPE_PROPERTY_NAME, definition.getBeanType());
                beanDefinitionBuilder.addPropertyValue(MICRONAUT_CONTEXT_PROPERTY_NAME, micronautContext);
                beanDefinitionBuilder.addPropertyValue(MICRONAUT_SINGLETON_PROPERTY_NAME, definition.isSingleton());

                ((DefaultListableBeanFactory) beanFactory).registerBeanDefinition(name, beanDefinitionBuilder.getBeanDefinition());
            } catch (NoClassDefFoundError error) {
                LOGGER.error("Exception loading class for supplier {}. Bean {} will not be available in the runtime", supplier, name);
                LOGGER.error("Current class loader: {}", printClassLoader(getClass().getClassLoader()));
                LOGGER.error("Parent class loader: {}",  printClassLoader(getClass().getClassLoader().getParent()));
                LOGGER.error("Current class path: {}", System.getProperty("java.class.path"));
                noClassDefFoundError = error;
            }
        }

        if (noClassDefFoundError == null) {
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("Successfully added following beans to the spring contest {} ", suppliers);
                LOGGER.info("Current class loader: {}", printClassLoader(getClass().getClassLoader()));
                LOGGER.info("Parent class loader: {}",  printClassLoader(getClass().getClassLoader().getParent()));
                LOGGER.info("Current class path: {}", System.getProperty("java.class.path"));
            }
            return;
        }

        throw noClassDefFoundError;
    }

    private static String printClassLoader(ClassLoader classLoader) {
        if (classLoader instanceof URLClassLoader) {
            return "URLClassLoader for URLS:" + Arrays.toString(((URLClassLoader) classLoader).getURLs());
        }
        if (classLoader == null) {
            return null;
        }
        return classLoader.toString();
    }

    @Override
    public void destroy() {
        if (micronautContext != null) {
            micronautContext.close();
        }
    }

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

}

