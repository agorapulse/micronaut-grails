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
import groovy.lang.Closure;
import groovy.lang.DelegatesTo;
import groovy.transform.stc.ClosureParams;
import groovy.transform.stc.SimpleType;
import io.micronaut.context.ApplicationContext;
import io.micronaut.context.ApplicationContextConfiguration;
import io.micronaut.context.DefaultApplicationContext;
import io.micronaut.context.env.DefaultEnvironment;
import io.micronaut.context.env.Environment;
import io.micronaut.core.convert.ConversionService;
import io.micronaut.core.reflect.ClassUtils;
import io.micronaut.core.value.PropertyResolver;
import io.micronaut.spring.context.factory.MicronautBeanFactoryConfiguration;
import org.grails.core.util.BeanCreationProfilingPostProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import space.jasan.support.groovy.closure.ConsumerWithDelegate;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

public class MicronautGrailsApp extends GrailsApp {

    public static final String ENVIRONMENT_STRICT = "micronaut-grails-strict";
    public static final String ENVIRONMENT_BRIDGE = "micronaut-grails-bridge";
    public static final String ENVIRONMENT_LEGACY = "micronaut-grails-legacy";
    public static final String ENVIRONMENT = "micronaut-grails";

    public enum Compatibility {
        /**
         * Runs the application in the legacy mode.
         *
         * <ul>
         *     <li>Two Micronaut contexts are created - one by Grails App and another one by Micronaut Grails for beans imported using {@link MicronautBeanImporter}</li>
         *     <li>Micronaut beans declared using {@link MicronautBeanImporter} can be injected by name without <code>@Inject</code> annotation</li>
         *     <li>Property prefixes are stripped by beans imported using {@link MicronautBeanImporter} according to existing {@link PropertyTranslatingCustomizer} beans</li>
         *     <li>Properties are ignored by beans imported using {@link MicronautBeanImporter} according to existing {@link PropertyTranslatingCustomizer} beans</li>
         * </ul>
         */
        @Deprecated
        LEGACY(ENVIRONMENT_LEGACY),

        /**
         * Runs the application in the bridge mode.
         *
         * <ul>
         *     <li>Only one Micronaut context is created - the one by Grails App</li>
         *     <li>Micronaut beans declared using {@link MicronautBeanImporter} can be injected by name without <code>@Inject</code> annotation</li>
         *     <li>Property prefixes are <em>no longer stripped</em> by beans imported using {@link MicronautBeanImporter} according to existing {@link PropertyTranslatingCustomizer} beans</li>
         *     <li>Properties are <em>no longer ignored</em> by beans imported using {@link MicronautBeanImporter} according to existing {@link PropertyTranslatingCustomizer} beans</li>
         * </ul>
         */
        BRIDGE(ENVIRONMENT_BRIDGE),

        /**
         * Runs the application in the bridge mode.
         *
         * <ul>
         *     <li>Only one Micronaut context is created - the one by Grails App</li>
         *     <li>All Micronaut beans must use <code>@Inject</code> annotation to be injected</li>
         *     <li>Property prefixes are <em>no longer stripped</em> by beans imported using {@link MicronautBeanImporter} according to existing {@link PropertyTranslatingCustomizer} beans</li>
         *     <li>Properties are <em>no longer ignored</em> by beans imported using {@link MicronautBeanImporter} according to existing {@link PropertyTranslatingCustomizer} beans</li>
         * </ul>
         */
        STRICT(ENVIRONMENT_STRICT);

        private final String environment;

        Compatibility(String environment) {
            this.environment = environment;
        }

        public String getEnvironment() {
            return environment;
        }

    }

    public static class Configuration {
        private final List<Class<?>> sources = new ArrayList<>();

        private String[] args;
        private Compatibility compatibility = Compatibility.LEGACY;
        private Consumer<Environment> environment = e -> { };

        public Configuration source(Class<?> source) {
            this.sources.add(source);
            return this;
        }

        public Configuration sources(List<Class<?>> sources) {
            this.sources.addAll(sources);
            return this;
        }

        public Configuration sources(Class<?>... sources) {
            this.sources.addAll(Arrays.asList(sources));
            return this;
        }

        public Configuration arguments(String... args) {
            this.args = args;
            return this;
        }

        public Configuration compatibility(Compatibility compatibility) {
            this.compatibility = compatibility;
            return this;
        }

        public Configuration environment(Consumer<Environment> micronaut) {
            this.environment = micronaut;
            return this;
        }

       public Configuration environment(
            @DelegatesTo(value = Environment.class, strategy = Closure.DELEGATE_FIRST)
            @ClosureParams(value = SimpleType.class, options = "io.micronaut.context.env.Environment")
            Closure<Environment> micronaut
        ) {
            this.environment = this.environment.andThen(ConsumerWithDelegate.create(micronaut));
            return this;
        }

        protected Environment applyToEnvironment(Environment env) {
            this.environment.accept(env);
            return env;
        }

        public Configuration verify() {
            if (sources.isEmpty()) {
                throw new IllegalStateException("At least one class must be set using the 'source' method!");
            }

            if (args == null) {
                throw new IllegalStateException("Arguments were not set using 'arguments' method!");
            }
            return this;
        }
    }

    private class MicronautGrailsAppContextConfiguration implements ApplicationContextConfiguration {
        private final ClassLoader applicationClassLoader;

        public MicronautGrailsAppContextConfiguration(
            ClassLoader applicationClassLoader
        ) {
            this.applicationClassLoader = applicationClassLoader;
        }

        @Override @Nonnull
        public List<String> getEnvironments() {
            List<String> environments = new ArrayList<>();
            environments.add(ENVIRONMENT);
            environments.add(configuration.compatibility.environment);
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
    }

    private class MicronautGrailsAppContext extends DefaultApplicationContext {

        public MicronautGrailsAppContext(ApplicationContextConfiguration micronautConfiguration) {
            super(micronautConfiguration);
        }

        @Override @Nonnull
        protected DefaultEnvironment createEnvironment(@Nonnull ApplicationContextConfiguration c) {
            DefaultEnvironment environment = super.createEnvironment(c);
            configuration.environment.accept(environment);
            return environment;
        }

    }

    private static final Logger LOGGER = LoggerFactory.getLogger(MicronautGrailsApp.class);

    private final Configuration configuration;

    // copy pasted

    /**
     * Static helper that can be used to run a {@link MicronautGrailsApp} from the
     * specified source using default settings.
     *
     * @param configuration application configuration
     * @return the running {@link org.springframework.context.ApplicationContext}
     */
    public static ConfigurableApplicationContext run(Consumer<Configuration> configuration) {
        Configuration c = initConfiguration(configuration);
        return new MicronautGrailsApp(c).run(c.args);
    }

    /**
     * Static helper that can be used to run a {@link MicronautGrailsApp} from the
     * specified source using default settings.
     *
     * @param configuration application configuration
     * @return the running {@link org.springframework.context.ApplicationContext}
     */
    public static ConfigurableApplicationContext run(
        @DelegatesTo(value = Configuration.class, strategy = Closure.DELEGATE_FIRST)
        @ClosureParams(value = SimpleType.class, options = "com.agorapulse.micronaut.grails.MicronautGrailsApp.Configuration")
        Closure<Configuration> configuration
    ) {
        return run(ConsumerWithDelegate.create(configuration));
    }


    /**
     * Static helper that can be used to run a {@link GrailsApp} from the
     * specified sources using default settings and user supplied arguments.
     * @param sources the sources to load
     * @param args the application arguments (usually passed from a Java main method)
     * @return the running {@link org.springframework.context.ApplicationContext}
     * @deprecated use {@link #run(Consumer)}
     */
    public static ConfigurableApplicationContext run(Class[] sources, String... args) {
        return run(c -> c.sources(sources).arguments(args));
    }

    /**
     * Static helper that can be used to run a {@link MicronautGrailsApp} from the
     * specified source using default settings and user supplied arguments.
     *
     * @param source  the source to load
     * @param args    the application arguments (usually passed from a Java main method)
     * @return the running {@link org.springframework.context.ApplicationContext}
     * @deprecated use {@link #run(Consumer)}
     */
    public static ConfigurableApplicationContext run(Class<?> source, String[] args) {
        return run(c -> c.source(source).arguments(args));
    }


    private MicronautGrailsApp(Configuration configuration) {
        super(configuration.verify().sources.toArray(new Class[0]));
        this.configuration = configuration;
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
    protected ConfigurableApplicationContext createApplicationContext() {
        setAllowBeanDefinitionOverriding(true);
        ConfigurableApplicationContext applicationContext = createSpringApplicationContext();
        long now = System.currentTimeMillis();

        ClassLoader applicationClassLoader = GrailsApp.class.getClassLoader();
        ApplicationContextConfiguration micronautConfiguration = new MicronautGrailsAppContextConfiguration(applicationClassLoader);

        List<Class<?>> beanExcludes = new ArrayList<>();
        beanExcludes.add(ConversionService.class);
        beanExcludes.add(org.springframework.core.env.Environment.class);
        beanExcludes.add(PropertyResolver.class);
        beanExcludes.add(ConfigurableEnvironment.class);
        ClassUtils.forName("com.fasterxml.jackson.databind.ObjectMapper", getClassLoader()).ifPresent(beanExcludes::add);
        ApplicationContext micronautContext = new MicronautGrailsAppContext(micronautConfiguration);

        micronautContext.getEnvironment().addPropertySource("grails-config", Collections.singletonMap(MicronautBeanFactoryConfiguration.PREFIX + ".bean-excludes", beanExcludes));
        micronautContext.registerSingleton(Configuration.class, configuration);

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

    private static Configuration initConfiguration(Consumer<Configuration> configurationConsumer) {
        Configuration configuration = new Configuration();
        configurationConsumer.accept(configuration);
        return configuration;
    }

}
