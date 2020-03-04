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
import io.micronaut.context.Qualifier;
import io.micronaut.core.naming.NameUtils;
import io.micronaut.inject.BeanDefinition;
import io.micronaut.inject.qualifiers.Qualifiers;

import java.lang.annotation.Annotation;
import java.util.*;
import java.util.function.Function;

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

    public static class UntypedBeanSupplier implements Function<ApplicationContext, Optional<BeanDefinition<?>>> {

        private final Qualifier<?> qualifier;

        public UntypedBeanSupplier(Qualifier<?> qualifier) {
            this.qualifier = qualifier;
        }

        @Override
        public Optional<BeanDefinition<?>> apply(ApplicationContext applicationContext) {
            Collection<BeanDefinition<?>> beanDefinitions = applicationContext.getBeanDefinitions((Qualifier<Object>) qualifier);

            if (beanDefinitions.size() > 1) {
                throw new IllegalArgumentException("There is too many candidates for " + qualifier + "! Candidates: " + beanDefinitions);
            }

            return beanDefinitions.stream().findFirst();
        }

        @Override
        public String toString() {
            return "supplier using qualifier " + qualifier;
        }

    }

    public static class TypedBeanSupplier<T> implements Function<ApplicationContext, Optional<BeanDefinition<T>>> {

        private final Class<T> type;
        private final Qualifier<T> qualifier;

        public TypedBeanSupplier(Class<T> type, Qualifier<T> qualifier) {
            this.type = type;
            this.qualifier = qualifier;
        }

        @Override
        public Optional<BeanDefinition<T>> apply(ApplicationContext applicationContext) {
            return applicationContext.findBeanDefinition(type, qualifier);
        }

        @Override
        public String toString() {
            return "supplier of " + type + " using qualifier " + qualifier;
        }

    }

    public static class BeanByTypeSupplier<T> implements Function<ApplicationContext, Optional<BeanDefinition<T>>> {

        private final Class<T> type;

        public BeanByTypeSupplier(Class<T> type) {
            this.type = type;
        }

        @Override
        public Optional<BeanDefinition<T>> apply(ApplicationContext applicationContext) {
            return applicationContext.findBeanDefinition(type);
        }

        @Override
        public String toString() {
            return "supplier of " + type;
        }

    }

    public static MicronautBeanImporter create() {
        return new MicronautBeanImporter();
    }

    private final Map<String, Function<ApplicationContext, Optional<BeanDefinition<?>>>> suppliers = new LinkedHashMap<>();
    private final List<PropertyTranslatingCustomizer> customizers = new ArrayList<>();

    protected MicronautBeanImporter() {}

    public MicronautBeanImporter add(String name, Function<ApplicationContext, Optional<BeanDefinition<?>>> supplier) {
        suppliers.put(name, supplier);
        return this;
    }

    public MicronautBeanImporter addByType(Class<?> type) {
        return add(NameUtils.decapitalize(type.getSimpleName()), new BeanByTypeSupplier(type));
    }

    public MicronautBeanImporter addByType(String grailsBeanName, Class<?> first, Class<?>... types) {
        if (types.length == 0) {
            return add(NameUtils.decapitalize(grailsBeanName), new BeanByTypeSupplier(first));
        } else {
            return add(NameUtils.decapitalize(grailsBeanName), new TypedBeanSupplier(first, Qualifiers.byType(types)));
        }
    }

    public MicronautBeanImporter addByStereotype(String grailsBeanName, Class<? extends Annotation> type) {
        return addByQualifiers(grailsBeanName, Qualifiers.byStereotype(type));
    }

    public MicronautBeanImporter addByStereotype(String grailsBeanName, Class<?> beanType, Class<? extends Annotation> type) {
        return addByQualifiers(grailsBeanName, beanType, Qualifiers.byStereotype(type));
    }

    public MicronautBeanImporter addByName(String name) {
        return addByName(name, name);
    }

    public MicronautBeanImporter addByName(String grailsBeanName, String micronautName) {
        return add(grailsBeanName, new UntypedBeanSupplier(Qualifiers.byName(micronautName)));
    }

    @SafeVarargs
    public final <T> MicronautBeanImporter addByQualifiers(String grailsBeanName, Class<T> type, Qualifier<T>... qualifiers) {
        Qualifier<T> qualifier = Qualifiers.byQualifiers(qualifiers);
        return add(grailsBeanName, new TypedBeanSupplier(type, qualifier));
    }

    /**
     * @deprecated use {@link #addByQualifiers(String, Class, Qualifier[])}
     */
    @SafeVarargs
    public final <T> MicronautBeanImporter addByQualifiers(String grailsBeanName, Qualifier<T>... qualifiers) {
        Qualifier<T> qualifier = Qualifiers.byQualifiers(qualifiers);
        return add(grailsBeanName, new UntypedBeanSupplier(qualifier));
    }

    public MicronautBeanImporter customize(PropertyTranslatingCustomizer customizer) {
        this.customizers.add(customizer);
        return this;
    }

    public MicronautBeanImporter customize(PropertyTranslatingCustomizer.Builder customizer) {
        return customize(customizer.build());
    }

    public Map<String, Function<ApplicationContext, Optional<BeanDefinition<?>>>> getSuppliers() {
        return Collections.unmodifiableMap(suppliers);
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
        return new GrailsMicronautBeanProcessor(getSuppliers(), getCustomizers());
    }
}

