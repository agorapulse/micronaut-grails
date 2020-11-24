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
import org.springframework.boot.Banner;
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

    private static final Logger LOGGER = LoggerFactory.getLogger(MicronautGrailsApp.class);

    private final Consumer<Environment> configureMicronautEnvironment;

    // copy pasted

    /**
     * Static helper that can be used to run a {@link GrailsApp} from the
     * specified source using default settings.
     * @param source the source to load
     * @param args the application arguments (usually passed from a Java main method)
     * @return the running {@link org.springframework.context.ApplicationContext}
     */
    public static ConfigurableApplicationContext run(Class<?> source, String... args) {
        return run(source, args, ConsumerWithDelegate.create(Closure.IDENTITY));
    }

    /**
     * Static helper that can be used to run a {@link GrailsApp} from the
     * specified sources using default settings and user supplied arguments.
     * @param sources the sources to load
     * @param args the application arguments (usually passed from a Java main method)
     * @return the running {@link org.springframework.context.ApplicationContext}
     */
    public static ConfigurableApplicationContext run(Class[] sources, String... args) {
        return run(sources, args, ConsumerWithDelegate.create(Closure.IDENTITY));
    }

    /**
     * Static helper that can be used to run a {@link GrailsApp} from the
     * specified source using default settings.
     *
     * @param source the source to load
     * @param args   the application arguments (usually passed from a Java main method)
     * @return the running {@link org.springframework.context.ApplicationContext}
     */
    public static ConfigurableApplicationContext run(
        Class<?> source,
        String[] args,
        @DelegatesTo(value = Environment.class, strategy = Closure.DELEGATE_FIRST)
        @ClosureParams(value = SimpleType.class, options = "io.micronaut.context.env.Environment")
            Closure<Environment> configureMicronautEnvironment
    ) {
        return run(new Class[]{source}, args, configureMicronautEnvironment);
    }

    /**
     * Static helper that can be used to run a {@link GrailsApp} from the
     * specified source using default settings.
     *
     * @param source the source to load
     * @param args   the application arguments (usually passed from a Java main method)
     * @return the running {@link org.springframework.context.ApplicationContext}
     */
    public static ConfigurableApplicationContext run(
        Class<?> source,
        String[] args,
        Consumer<Environment> configureMicronautEnvironment
    ) {
        return run(new Class[]{source}, args, configureMicronautEnvironment);
    }


    /**
     * Static helper that can be used to run a {@link GrailsApp} from the
     * specified sources using default settings and user supplied arguments.
     *
     * @param sources the sources to load
     * @param args    the application arguments (usually passed from a Java main method)
     * @return the running {@link org.springframework.context.ApplicationContext}
     */
    public static ConfigurableApplicationContext run(
        Class<?>[] sources,
        String[] args,
        @DelegatesTo(value = Environment.class, strategy = Closure.DELEGATE_FIRST)
        @ClosureParams(value = SimpleType.class, options = "io.micronaut.context.env.Environment")
            Closure<Environment> configureMicronautEnvironment
    ) {
        return run(sources, args, ConsumerWithDelegate.create(configureMicronautEnvironment));
    }

    /**
     * Static helper that can be used to run a {@link GrailsApp} from the
     * specified sources using default settings and user supplied arguments.
     *
     * @param sources the sources to load
     * @param args    the application arguments (usually passed from a Java main method)
     * @return the running {@link org.springframework.context.ApplicationContext}
     */
    public static ConfigurableApplicationContext run(
        Class<?>[] sources,
        String[] args,
        Consumer<Environment> configureMicronautEnvironment
    ) {
        MicronautGrailsApp grailsApp = new MicronautGrailsApp(configureMicronautEnvironment, sources);
        grailsApp.setBannerMode(Banner.Mode.OFF);
        return grailsApp.run(args);
    }

    private MicronautGrailsApp(Consumer<Environment> configureMicronautEnvironment, Class<?>... sources) {
        super(sources);
        this.configureMicronautEnvironment = configureMicronautEnvironment;
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
    protected ConfigurableApplicationContext createApplicationContext() {
        setAllowBeanDefinitionOverriding(true);
        ConfigurableApplicationContext applicationContext = createSpringApplicationContext();
        long now = System.currentTimeMillis();

        ClassLoader applicationClassLoader = GrailsApp.class.getClassLoader();
        ApplicationContextConfiguration micronautConfiguration = new ApplicationContextConfiguration() {
            @Override @Nonnull
            public List<String> getEnvironments() {
                if (getConfiguredEnvironment() != null) {
                    return Arrays.asList(getConfiguredEnvironment().getActiveProfiles());
                } else {
                    return Collections.emptyList();
                }
            }

            @Override
            public Optional<Boolean> getDeduceEnvironments() {
                return Optional.of(false);
            }

            @Override @Nonnull
            public ClassLoader getClassLoader() {
                return applicationClassLoader;
            }
        };

        List<Class<?>> beanExcludes = new ArrayList<>();
        beanExcludes.add(ConversionService.class);
        beanExcludes.add(org.springframework.core.env.Environment.class);
        beanExcludes.add(PropertyResolver.class);
        beanExcludes.add(ConfigurableEnvironment.class);
        ClassUtils.forName("com.fasterxml.jackson.databind.ObjectMapper", getClassLoader()).ifPresent(beanExcludes::add);
        ApplicationContext micronautContext = new DefaultApplicationContext(micronautConfiguration) {

            @Override @Nonnull
            protected DefaultEnvironment createEnvironment(@Nonnull ApplicationContextConfiguration configuration) {
                DefaultEnvironment environment = super.createEnvironment(configuration);
                configureMicronautEnvironment.accept(environment);
                return environment;
            }

        };

        micronautContext.getEnvironment()
            .addPropertySource("grails-config", Collections.singletonMap(MicronautBeanFactoryConfiguration.PREFIX + ".bean-excludes", beanExcludes));

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



}
