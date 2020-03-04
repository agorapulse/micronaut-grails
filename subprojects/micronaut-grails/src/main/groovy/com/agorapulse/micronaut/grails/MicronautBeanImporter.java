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
import io.micronaut.core.naming.NameUtils;
import io.micronaut.inject.qualifiers.Qualifiers;

import java.lang.annotation.Annotation;
import java.util.*;

/**
 * Defineds which Micronaut beans should be added into a Grails' Spring application context. The processor will then
 * find all of the Micronaut beans of the specified types and add them as beans to the Spring application context.
 * <p>
 * <p>
 * It can also manage beans to
 *
 * @since 1.2.3.1
 */
public class MicronautBeanImporter {

    public static MicronautBeanImporter create() {
        return new MicronautBeanImporter();
    }

    private final Map<String, TypeAndQualifier<?>> micronautBeanQualifiers = new LinkedHashMap<>();
    private final List<PropertyTranslatingCustomizer> customizers = new ArrayList<>();

    protected MicronautBeanImporter() {}

    public MicronautBeanImporter addByType(Class<?> type) {
        return addByType(NameUtils.decapitalize(type.getSimpleName()), type);
    }

    public MicronautBeanImporter addByType(String grailsBeanName, Class<?> type, Class<?>... types) {
        if (types.length == 0) {
            micronautBeanQualifiers.put(grailsBeanName, new TypeAndQualifier<>(type, null));
        } else {
            micronautBeanQualifiers.put(grailsBeanName, new TypeAndQualifier<>(type, Qualifiers.byType(types)));
        }
        return this;
    }

    public MicronautBeanImporter addByStereotype(String grailsBeanName, Class<?> beanType, Class<? extends Annotation> type) {
        return addByQualifiers(grailsBeanName, beanType, Qualifiers.byStereotype(type));
    }

    /**
     * @deprecated use {@link #addByStereotype(String, Class, Class)} instead
     */
    public MicronautBeanImporter addByStereotype(String grailsBeanName, Class<? extends Annotation> type) {
        return addByQualifiers(grailsBeanName, Qualifiers.byStereotype(type));
    }

    /**
     * @deprecated use {@link #addByName(String, Class)} instead
     */
    public MicronautBeanImporter addByName(String name) {
        return addByName(name, name);
    }

    public MicronautBeanImporter addByName(String name, Class<?> beanType) {
        return addByName(name, beanType, name);
    }

    /**
     * @deprecated use {@link #addByName(String, Class, String)} instead
     */
    public MicronautBeanImporter addByName(String grailsBeanName, String micronautName) {
        micronautBeanQualifiers.put(grailsBeanName, new TypeAndQualifier<>(null, Qualifiers.byName(micronautName)));
        return this;
    }

    public MicronautBeanImporter addByName(String grailsBeanName, Class<?> beanType, String micronautName) {
        micronautBeanQualifiers.put(grailsBeanName, new TypeAndQualifier<>(beanType, Qualifiers.byName(micronautName)));
        return this;
    }

    @SafeVarargs
    public final <T> MicronautBeanImporter addByQualifiers(String grailsBeanName, Class<T> beanType, Qualifier<T>... qualifiers) {
        micronautBeanQualifiers.put(grailsBeanName, new TypeAndQualifier<>(beanType, Qualifiers.byQualifiers(qualifiers)));
        return this;
    }

    /**
     * @deprecated use {@link #addByQualifiers(String, Class, Qualifier[])} instead
     */
    @SafeVarargs
    public final <T> MicronautBeanImporter addByQualifiers(String grailsBeanName, Qualifier<T>... qualifiers) {
        return addByQualifiers(grailsBeanName, (Class<T>) null, Qualifiers.byQualifiers(qualifiers));
    }

    public MicronautBeanImporter customize(PropertyTranslatingCustomizer customizer) {
        this.customizers.add(customizer);
        return this;
    }

    public MicronautBeanImporter customize(PropertyTranslatingCustomizer.Builder customizer) {
        return customize(customizer.build());
    }

    public Map<String, TypeAndQualifier<?>> getMicronautBeanQualifiers() {
        return Collections.unmodifiableMap(micronautBeanQualifiers);
    }

    public List<PropertyTranslatingCustomizer> getCustomizers() {
        return Collections.unmodifiableList(customizers);
    }

    /**
     * @return new GrailsMicronautBeanProcessor for current builder
     * @deprecated please declare {@link MicronautBeanImporter} bean to avoid multiple Micronaut application context
     * inside single application.
     */
    public GrailsMicronautBeanProcessor build() {
        try {
            throw new IllegalStateException();
        } catch (IllegalStateException th) {
            GrailsMicronautBeanProcessor.LOGGER.error("Old style of importing Micronaut beans used. This will lead to having multiple Micronaut application context in the application");
        }
        return new GrailsMicronautBeanProcessor(getMicronautBeanQualifiers(), getCustomizers());
    }
}

