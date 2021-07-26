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

import grails.boot.GrailsApp;
import io.micronaut.context.ApplicationContext;
import io.micronaut.context.ApplicationContextConfiguration;
import io.micronaut.context.DefaultApplicationContext;
import io.micronaut.context.env.DefaultEnvironment;
import io.micronaut.core.convert.ConversionService;
import io.micronaut.core.reflect.ClassUtils;
import io.micronaut.core.value.PropertyResolver;
import io.micronaut.spring.context.factory.MicronautBeanFactoryConfiguration;
import org.grails.core.util.BeanCreationProfilingPostProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanInstantiationException;
import org.springframework.beans.BeanUtils;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class MicronautGrailsApp extends GrailsApp {

    public static final String ENVIRONMENT_STRICT = "micronaut-grails-strict";
    public static final String ENVIRONMENT_BRIDGE = "micronaut-grails-bridge";
    public static final String ENVIRONMENT_LEGACY = "micronaut-grails-legacy";
    public static final String ENVIRONMENT = "micronaut-grails";

    private class MicronautGrailsAppContextConfiguration implements ApplicationContextConfiguration {
        private final ClassLoader applicationClassLoader;
        private final MicronautGrailsAutoConfiguration configuration;

        public MicronautGrailsAppContextConfiguration(
            ClassLoader applicationClassLoader,
            MicronautGrailsAutoConfiguration configuration
        ) {
            this.applicationClassLoader = applicationClassLoader;
            this.configuration = configuration;
        }

        @Override
        @Nonnull
        public List<String> getEnvironments() {
            List<String> environments = new ArrayList<>();
            environments.add(ENVIRONMENT);
            environments.add(configuration.getCompatibilityMode().getEnvironment());
            if (getConfiguredEnvironment() != null) {
                environments.addAll(Arrays.asList(getConfiguredEnvironment().getActiveProfiles()));
            }
            return environments;
        }

        @Override
        public Optional<Boolean> getDeduceEnvironments() {
            return Optional.of(false);
        }

        @Override @Nonnull
        public ClassLoader getClassLoader() {
            return applicationClassLoader;
        }

        public MicronautGrailsAutoConfiguration getConfiguration() {
            return configuration;
        }
    }

    private static class MicronautGrailsAppContext extends DefaultApplicationContext {

        public MicronautGrailsAppContext(MicronautGrailsAppContextConfiguration micronautConfiguration) {
            super(micronautConfiguration);
        }

        @Override
        @Nonnull
        protected DefaultEnvironment createEnvironment(@Nonnull ApplicationContextConfiguration c) {
            DefaultEnvironment environment = super.createEnvironment(c);
            ((MicronautGrailsAppContextConfiguration)c).getConfiguration().configureEnvironment(environment);
            return environment;
        }
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(MicronautGrailsApp.class);

    // copy pasted

    /**
     * Static helper that can be used to run a {@link GrailsApp} from the
     * specified sources using default settings and user supplied arguments.
     *
     * @param sources the sources to load
     * @param args    the application arguments (usually passed from a Java main method)
     * @return the running {@link org.springframework.context.ApplicationContext}
     */
    @SuppressWarnings("rawtypes")
    public static ConfigurableApplicationContext run(Class[] sources, String... args) {
        return new MicronautGrailsApp(sources).run(args);
    }

    /**
     * Static helper that can be used to run a {@link MicronautGrailsApp} from the
     * specified source using default settings and user supplied arguments.
     *
     * @param source the source to load
     * @param args   the application arguments (usually passed from a Java main method)
     * @return the running {@link org.springframework.context.ApplicationContext}
     */
    public static ConfigurableApplicationContext run(Class<?> source, String[] args) {
        return new MicronautGrailsApp(source).run(args);
    }


    public MicronautGrailsApp(Class... sources) {
        super(sources);
    }


    /**
     * Strategy method used to create the {@link org.springframework.context.ApplicationContext}. By default this
     * method will respect any explicitly set application context or application context
     * class before falling back to a suitable default.
     *
     * @return the application context (not yet refreshed)
     * @see #setApplicationContextClass(Class)
     */
    protected ConfigurableApplicationContext createSpringApplicationContext() {
        Class<?> contextClass;
        try {
            switch (this.getWebApplicationType()) {
                case SERVLET:
                    contextClass = Class.forName(DEFAULT_SERVLET_WEB_CONTEXT_CLASS);
                    break;
                case REACTIVE:
                    contextClass = Class.forName(DEFAULT_REACTIVE_WEB_CONTEXT_CLASS);
                    break;
                default:
                    contextClass = Class.forName(DEFAULT_CONTEXT_CLASS);
            }
        } catch (ClassNotFoundException ex) {
            throw new IllegalStateException("Unable create a default ApplicationContext, please specify an ApplicationContextClass", ex);
        }
        return (ConfigurableApplicationContext) BeanUtils.instantiateClass(contextClass);
    }

    @Override
    protected Object printRunStatus(ConfigurableApplicationContext applicationContext) {
        System.out.println(
            "The application is running in Micronaut Grails compatibility mode with the the following environments active: "
                + Arrays.toString(getConfiguredEnvironment().getActiveProfiles())
        );
        return null;
    }

    @Override
    protected void configureEnvironment(ConfigurableEnvironment environment, String[] args) {
        super.configureEnvironment(environment, args);
        environment.addActiveProfile(ENVIRONMENT);

        MicronautGrailsAutoConfiguration app = getApplication();
        environment.addActiveProfile(app.getCompatibilityMode().getEnvironment());
    }

    @Override
    protected ConfigurableApplicationContext createApplicationContext() {
        setAllowBeanDefinitionOverriding(true);

        ConfigurableApplicationContext applicationContext = createSpringApplicationContext();

        long now = System.currentTimeMillis();

        MicronautGrailsAutoConfiguration app = getApplication();
        ClassLoader applicationClassLoader = GrailsApp.class.getClassLoader();
        MicronautGrailsAppContextConfiguration micronautConfiguration = new MicronautGrailsAppContextConfiguration(applicationClassLoader, app);

        List<Class<?>> beanExcludes = new ArrayList<>();
        beanExcludes.add(ConversionService.class);
        beanExcludes.add(org.springframework.core.env.Environment.class);
        beanExcludes.add(PropertyResolver.class);
        beanExcludes.add(ConfigurableEnvironment.class);
        ClassUtils.forName("com.fasterxml.jackson.databind.ObjectMapper", getClassLoader()).ifPresent(beanExcludes::add);
        ApplicationContext micronautContext = new MicronautGrailsAppContext(micronautConfiguration);

        micronautContext.getEnvironment().addPropertySource("grails-config", Collections.singletonMap(MicronautBeanFactoryConfiguration.PREFIX + ".bean-excludes", beanExcludes));
        micronautContext.registerSingleton(MicronautGrailsAutoConfiguration.class, app);
        micronautContext.start();

        ConfigurableApplicationContext parentContext = micronautContext.getBean(ConfigurableApplicationContext.class);
        applicationContext.setParent(parentContext);
        applicationContext.addApplicationListener(new MicronautShutdownListener(micronautContext));

        LOGGER.info("Started Micronaut Parent Application Context in " + (System.currentTimeMillis() - now) + " ms");

        if (isEnableBeanCreationProfiler()) {
            BeanCreationProfilingPostProcessor processor = new BeanCreationProfilingPostProcessor();
            applicationContext.getBeanFactory().addBeanPostProcessor(processor);
            applicationContext.addApplicationListener(processor);
        }
        return applicationContext;
    }

    @Nonnull
    private MicronautGrailsAutoConfiguration getApplication() {
        try {
            return getAllSources()
                .stream()
                .filter(s -> s instanceof Class<?> && MicronautGrailsAutoConfiguration.class.isAssignableFrom((Class<?>)s))
                .findFirst()
                .map(s -> BeanUtils.instantiateClass((Class<?>) s, MicronautGrailsAutoConfiguration.class))
                .orElseGet(MicronautGrailsAutoConfiguration::new);
        } catch (BeanInstantiationException e) {
            LOGGER.error("Failed to instantiate class" + getMainApplicationClass(), e);
            return new MicronautGrailsAutoConfiguration();
        }
    }

}
