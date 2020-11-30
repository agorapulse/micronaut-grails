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
package micronaut.grails.example

import com.agorapulse.micronaut.grails.domain.Manager
import grails.boot.GrailsApp
import grails.boot.config.GrailsAutoConfiguration
import groovy.transform.stc.ClosureParams
import groovy.transform.stc.SimpleType
import groovy.util.logging.Slf4j
import io.micronaut.context.ApplicationContextConfiguration
import io.micronaut.context.DefaultApplicationContext
import io.micronaut.context.env.DefaultEnvironment
import io.micronaut.context.env.Environment
import io.micronaut.core.reflect.ClassUtils
import io.micronaut.spring.context.factory.MicronautBeanFactoryConfiguration
import org.grails.core.util.BeanCreationProfilingPostProcessor
import org.springframework.beans.BeanUtils
import org.springframework.boot.Banner
import org.springframework.boot.WebApplicationType
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.core.convert.ConversionService
import org.springframework.core.env.ConfigurableEnvironment
import org.springframework.core.env.PropertyResolver
import org.springframework.core.io.ResourceLoader

import javax.annotation.Nonnull

class DefaultApplication extends GrailsAutoConfiguration {

    static ConfigurableApplicationContext context

    static void main(String[] args) {
        context = CustomGrailsApp.run(DefaultApplication, args) { env ->
            env.addPackage(Manager.package)
        }
    }

    @Slf4j
    static class CustomGrailsApp extends GrailsApp {

        private final Closure<Environment> configureMicronautEnvironment

        // copy pasted

        /**
         * Static helper that can be used to run a {@link GrailsApp} from the
         * specified source using default settings.
         * @param source the source to load
         * @param args the application arguments (usually passed from a Java main method)
         * @return the running {@link org.springframework.context.ApplicationContext}
         */
        static ConfigurableApplicationContext run(
            Class<?> source,
            String[] args,
            @ClosureParams(value = SimpleType.class, options = 'io.micronaut.context.env.Environment')
                Closure<Environment> configureMicronautEnvironment
        ) {
            return run([source] as Class[], args, configureMicronautEnvironment)
        }


        /**
         * Static helper that can be used to run a {@link GrailsApp} from the
         * specified sources using default settings and user supplied arguments.
         * @param sources the sources to load
         * @param args the application arguments (usually passed from a Java main method)
         * @return the running {@link org.springframework.context.ApplicationContext}
         */
        static ConfigurableApplicationContext run(
            Class<?>[] sources,
            String[] args,
            @ClosureParams(value = SimpleType.class, options = 'io.micronaut.context.env.Environment')
                Closure<Environment> configureMicronautEnvironment
        ) {
            CustomGrailsApp grailsApp = new CustomGrailsApp(configureMicronautEnvironment, sources)
            grailsApp.bannerMode = Banner.Mode.OFF
            return grailsApp.run(args)
        }

        CustomGrailsApp(@ClosureParams(value = SimpleType.class, options = 'io.micronaut.context.env.Environment') Closure<Environment> configureMicronautEnvironment, Class<?>... sources) {
            super(sources)
            this.configureMicronautEnvironment = configureMicronautEnvironment
        }

        CustomGrailsApp(ResourceLoader resourceLoader, @ClosureParams(value = SimpleType.class, options = 'io.micronaut.context.env.Environment') Closure<Environment> configureMicronautEnvironment, Class<?>... sources) {
            super(resourceLoader, sources)
            this.configureMicronautEnvironment = configureMicronautEnvironment
        }


        /**
         * Strategy method used to create the {@link org.springframework.context.ApplicationContext}. By default this
         * method will respect any explicitly set application context or application context
         * class before falling back to a suitable default.
         * @return the application context (not yet refreshed)
         * @see #setApplicationContextClass(Class)
         */
        protected ConfigurableApplicationContext createSpringApplicationContext() {
            Class<?> contextClass = null
            try {
                switch (this.webApplicationType) {
                    case WebApplicationType.SERVLET:
                        contextClass = Class.forName(DEFAULT_SERVLET_WEB_CONTEXT_CLASS)
                        break
                    case WebApplicationType.REACTIVE:
                        contextClass = Class.forName(DEFAULT_REACTIVE_WEB_CONTEXT_CLASS)
                        break
                    default:
                        contextClass = Class.forName(DEFAULT_CONTEXT_CLASS)
                }
            }
            catch (ClassNotFoundException ex) {
                throw new IllegalStateException(
                    "Unable create a default ApplicationContext, " + "please specify an ApplicationContextClass",
                    ex)
            }
            return (ConfigurableApplicationContext) BeanUtils.instantiateClass(contextClass)
        }

        @Override
        protected ConfigurableApplicationContext createApplicationContext() {
            setAllowBeanDefinitionOverriding(true)
            ConfigurableApplicationContext applicationContext = createSpringApplicationContext()
            def now = System.currentTimeMillis()

            ClassLoader applicationClassLoader = GrailsApp.classLoader
            ApplicationContextConfiguration micronautConfiguration = new ApplicationContextConfiguration() {
                @Override
                List<String> getEnvironments() {
                    if (configuredEnvironment != null) {
                        return configuredEnvironment.getActiveProfiles().toList()
                    } else {
                        return Collections.emptyList()
                    }
                }

                @Override
                Optional<Boolean> getDeduceEnvironments() {
                    return Optional.of(false)
                }

                @Override
                ClassLoader getClassLoader() {
                    return applicationClassLoader
                }
            }

            List beanExcludes = []
            beanExcludes.add(ConversionService.class)
            beanExcludes.add(org.springframework.core.env.Environment.class)
            beanExcludes.add(PropertyResolver.class)
            beanExcludes.add(ConfigurableEnvironment.class)
            def objectMapper = ClassUtils.forName("com.fasterxml.jackson.databind.ObjectMapper", classLoader).orElse(null)
            if (objectMapper != null) {
                beanExcludes.add(objectMapper)
            }
            def micronautContext = new DefaultApplicationContext(micronautConfiguration) {

                @Override
                protected DefaultEnvironment createEnvironment(@Nonnull ApplicationContextConfiguration configuration) {
                    return configureMicronautEnvironment.call(super.createEnvironment(configuration)) as DefaultEnvironment
                }
            }

            micronautContext
                .environment
                .addPropertySource("grails-config", [(MicronautBeanFactoryConfiguration.PREFIX + ".bean-excludes"): (Object) beanExcludes])
            micronautContext.start()

            ConfigurableApplicationContext parentContext = micronautContext.getBean(ConfigurableApplicationContext)
            applicationContext.setParent(
                parentContext
            )
            applicationContext.addApplicationListener(new MicronautShutdownListener(micronautContext))
            log.info("Started Micronaut Parent Application Context in ${System.currentTimeMillis() - now}ms")


            if (enableBeanCreationProfiler) {
                def processor = new BeanCreationProfilingPostProcessor()
                applicationContext.getBeanFactory().addBeanPostProcessor(processor)
                applicationContext.addApplicationListener(processor)
            }
            return applicationContext
        }
    }

}
