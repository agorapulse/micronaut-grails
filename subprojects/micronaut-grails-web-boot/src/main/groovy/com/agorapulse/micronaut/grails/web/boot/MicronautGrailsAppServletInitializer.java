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
package com.agorapulse.micronaut.grails.web.boot;

import com.agorapulse.micronaut.grails.MicronautGrailsAppBuilder;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.builder.ParentContextApplicationContextInitializer;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.context.AnnotationConfigServletWebServerApplicationContext;
import org.springframework.boot.web.servlet.support.ErrorPageFilter;
import org.springframework.boot.web.servlet.support.ServletContextApplicationContextInitializer;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.Assert;
import org.springframework.web.context.WebApplicationContext;

import javax.servlet.ServletContext;

/**
 * Ensure a {@link com.agorapulse.micronaut.grails.MicronautGrailsApp} in constructed during servlet initialization.
 *
 * Inspired by {@code GrailsAppServletInitializer}
 *
 */
public abstract class MicronautGrailsAppServletInitializer extends SpringBootServletInitializer {

    /*
     * TODO: Replace with createSpringApplicationBuilder() override when upgrading to Spring Boot 1.3 M4 and delete this method
     */

    protected WebApplicationContext createRootApplicationContext(ServletContext servletContext) {
        SpringApplicationBuilder builder = new MicronautGrailsAppBuilder();
        builder.main(getClass());
        ApplicationContext parent = getExistingRootWebApplicationContext(servletContext);
        if (parent != null) {
            this.logger.info("Root context already created (using as parent).");
            servletContext.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, null);
            builder.initializers(new ParentContextApplicationContextInitializer(parent));
        }
        builder.initializers(new ServletContextApplicationContextInitializer(servletContext));
        builder.contextClass(AnnotationConfigServletWebServerApplicationContext.class);
        builder = configure(builder);
        SpringApplication application = builder.build();
        if (application.getAllSources().isEmpty() && AnnotationUtils.findAnnotation(getClass(), Configuration.class) != null) {
            application.getSources().add(getClass().getName());
        }
        Assert.state(
            application.getAllSources().size() > 0,
            "No SpringApplication sources have been defined. Either override the configure method or add an @Configuration annotation"
        );
        // Ensure error pages are registered
        application.getSources().add(ErrorPageFilter.class.getName());
        return run(application);
    }

    private ApplicationContext getExistingRootWebApplicationContext(ServletContext servletContext) {
        Object context = servletContext.getAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE);
        if (context instanceof ApplicationContext) {
            return (ApplicationContext) context;
        }
        return null;
    }
}
