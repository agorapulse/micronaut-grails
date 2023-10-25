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

import io.micronaut.context.Qualifier;
import io.micronaut.inject.BeanDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import io.micronaut.core.annotation.NonNull;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Adds Micronaut beans to a Grails' Spring application context.  This processor will
 * find all of the Micronaut beans of the specified types and add them as beans to the Spring application context.
 *
 * No property translation will happen.
 * <p>
 *
 * @since 2.0.3
 */
public class DefaultGrailsMicronautBeanProcessor implements BeanFactoryPostProcessor, DisposableBean, ApplicationContextAware {

    static final Logger LOGGER = LoggerFactory.getLogger(DefaultGrailsMicronautBeanProcessor.class);

    private static final String MICRONAUT_BEAN_TYPE_PROPERTY_NAME = "micronautBeanType";
    private static final String MICRONAUT_CONTEXT_PROPERTY_NAME = "micronautContext";
    private static final String MICRONAUT_QUALIFIER_PROPERTY_NAME = "micronautQualifier";
    private static final String MICRONAUT_SINGLETON_PROPERTY_NAME = "micronautSingleton";

    private io.micronaut.context.ApplicationContext micronautContext;
    private ApplicationContext springContext;
    private final Map<String, TypeAndQualifier<?>> micronautBeanQualifiers;

    /**
     * @param qualifiers the names and qualifiers of the Micronaut beans which should be added to the
     *                   Spring application context.
     */
    DefaultGrailsMicronautBeanProcessor(Map<String, TypeAndQualifier<?>> qualifiers) {
        this.micronautBeanQualifiers = qualifiers;
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        micronautContext = initializeMicronautContext();

        NoClassDefFoundError noClassDefFoundError = null;

        for (Map.Entry<String, TypeAndQualifier<?>> entry : micronautBeanQualifiers.entrySet()) {
            String name = entry.getKey();
            Class type = entry.getValue().getType();
            Qualifier micronautBeanQualifier = entry.getValue().getQualifier();
            try {
                Collection<BeanDefinition<?>> beanDefinitions = type == null
                    ? micronautContext.getBeanDefinitions(micronautBeanQualifier)
                    : micronautContext.getBeanDefinitions(type, micronautBeanQualifier);

                if (beanDefinitions.size() > 1) {
                    throw new IllegalArgumentException("There is too many candidates of type '" + type + "' for qualifier '" + micronautBeanQualifier + "'! Candidates: " + beanDefinitions);
                }

                Optional<BeanDefinition<?>> firstBean = beanDefinitions.stream().findFirst();
                BeanDefinition<?> definition = firstBean.orElseThrow(()-> {
                        String message = "There is no candidate for type " + type + " and qualifier " + micronautBeanQualifier + "\n"
                            + "Known beans:\n" + micronautContext.getAllBeanDefinitions().stream().map(d -> d.getBeanType().getName() + " " + d.getName()).collect(Collectors.joining("\n  "));

                        return new IllegalArgumentException(message);
                });

                final BeanDefinitionBuilder beanDefinitionBuilder = BeanDefinitionBuilder
                    .rootBeanDefinition(GrailsMicronautBeanFactory.class);
                beanDefinitionBuilder.addPropertyValue(MICRONAUT_BEAN_TYPE_PROPERTY_NAME, type == null ? definition.getBeanType() : type);
                beanDefinitionBuilder.addPropertyValue(MICRONAUT_QUALIFIER_PROPERTY_NAME, micronautBeanQualifier);
                beanDefinitionBuilder.addPropertyValue(MICRONAUT_CONTEXT_PROPERTY_NAME, micronautContext);
                beanDefinitionBuilder.addPropertyValue(MICRONAUT_SINGLETON_PROPERTY_NAME, definition.isSingleton());

                ((DefaultListableBeanFactory) beanFactory).registerBeanDefinition(name, beanDefinitionBuilder.getBeanDefinition());
            } catch (NoClassDefFoundError error) {
                LOGGER.error("Exception loading class for qualifier {}. Bean {} will not be available in the runtime", micronautBeanQualifier, name);
                LOGGER.error("Current class loader: {}", printClassLoader(getClass().getClassLoader()));
                LOGGER.error("Parent class loader: {}",  printClassLoader(getClass().getClassLoader().getParent()));
                LOGGER.error("Current class path: {}", System.getProperty("java.class.path"));
                noClassDefFoundError = error;
            }
        }

        if (noClassDefFoundError == null) {
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("Successfully added following beans to the spring contest {} ", micronautBeanQualifiers);
                LOGGER.debug("Current class loader: {}", printClassLoader(getClass().getClassLoader()));
                LOGGER.debug("Parent class loader: {}",  printClassLoader(getClass().getClassLoader().getParent()));
                LOGGER.trace("Current class path: {}", System.getProperty("java.class.path"));
            }
            return;
        }

        throw noClassDefFoundError;
    }

    protected io.micronaut.context.ApplicationContext initializeMicronautContext() {
        return springContext.getBean(MicronautContextHolder.class).getContext();
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
    public void setApplicationContext(@NonNull ApplicationContext applicationContext) throws BeansException {
        this.springContext = applicationContext;
    }
}

