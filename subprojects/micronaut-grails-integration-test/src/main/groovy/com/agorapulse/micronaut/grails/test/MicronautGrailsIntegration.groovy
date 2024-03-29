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
package com.agorapulse.micronaut.grails.test

import org.codehaus.groovy.transform.GroovyASTTransformationClass

import java.lang.annotation.ElementType
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy
import java.lang.annotation.Target

/**
 * Transformation to apply to integration tests
 *
 * @author Graeme Rocher
 * @since Grails 2.3
 *
 * @author Vladimir Orany
 * @since Micronaut Grails 3.0.4
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@GroovyASTTransformationClass('com.agorapulse.micronaut.grails.test.MicronautGrailsIntegrationTestMixinTransformation')
@interface MicronautGrailsIntegration {

    /**
     * Specify the Application class which should be used for
     * this functional test.  If unspecified the test runtime
     * environment will attempt to locate a class in the project
     * which extends grails.boot.config.GrailsAutoConfiguration
     * which can be problematic in multi project builds where
     * multiple Application classes may exist.
     */
    Class applicationClass() default { }

}
