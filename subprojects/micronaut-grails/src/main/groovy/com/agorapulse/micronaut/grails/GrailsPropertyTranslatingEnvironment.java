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

import io.micronaut.context.env.DefaultEnvironment;
import io.micronaut.core.convert.ArgumentConversionContext;
import org.springframework.core.env.AbstractEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertySource;

import javax.annotation.Nullable;
import java.util.*;

class GrailsPropertyTranslatingEnvironment extends DefaultEnvironment {

    private final Environment environment;
    private final PropertyTranslatingCustomizer customizer;

    GrailsPropertyTranslatingEnvironment(Environment environment, PropertyTranslatingCustomizer customizer) {
        super(environment.getActiveProfiles());
        this.environment = environment;
        this.customizer = customizer;

        if (environment instanceof AbstractEnvironment) {
            Map<String, Object> multilayer = new LinkedHashMap<>();
            AbstractEnvironment abEnv = (AbstractEnvironment) environment;
            for (PropertySource<?> source : abEnv.getPropertySources()) {
                if (source instanceof MapPropertySource) {
                    MapPropertySource mps = (MapPropertySource) source;
                    mps.getSource().forEach((k, v) -> {
                        String[] parts = k.split("\\.");
                        Map<String, Object> currentLevelMap = multilayer;
                        String prefix = "";
                        for (int i = 0; i < parts.length - 1; i++) {
                            String part = parts[i];
                            Object currentOrNewMap = currentLevelMap.computeIfAbsent(part, key -> new LinkedHashMap<>());
                            multilayer.put(prefix + part, currentOrNewMap);
                            if (currentOrNewMap instanceof Map) {
                                currentLevelMap = (Map<String, Object>) currentOrNewMap;
                            } else {
                                // conflict - cannot convert key to map of maps
                                return;
                            }
                            prefix = prefix.length() == 0 ? part  + "." : prefix + part + ".";
                        }
                        currentLevelMap.put(parts[parts.length - 1], v);
                    });
                }
            }
            abEnv.getPropertySources().addLast(new MapPropertySource("multilayer", multilayer));
        }
    }

    @Override
    public io.micronaut.context.env.Environment start() {
        return this;
    }

    @Override
    public io.micronaut.context.env.Environment stop() {
        return this;
    }

    @Override
    public boolean containsProperty(@Nullable String name) {
        if (environment.containsProperty(name)) {
            return true;
        }

        Set<String> alternativeNames = customizer.getAlternativeNames(name);
        if (alternativeNames.isEmpty()) {
            return false;
        }

        return alternativeNames.stream().anyMatch(environment::containsProperty);
    }

    @Override
    public boolean containsProperties(@Nullable String name) {
        return containsProperty(name);
    }

    @Override
    public <T> Optional<T> getProperty(@Nullable String name, ArgumentConversionContext<T> conversionContext) {
        Class<T> type = conversionContext.getArgument().getType();
        T value = environment.getProperty(name, type);
        if (value != null) {
            return Optional.of(value);
        }

        Set<String> alternativeNames = customizer.getAlternativeNames(name);
        if (alternativeNames.isEmpty()) {
            return Optional.empty();
        }

        for (String alternativeName : alternativeNames) {
            T alternativeValue = environment.getProperty(alternativeName, type);
            if (alternativeValue != null) {
                return Optional.of(alternativeValue);
            }
        }

        return Optional.empty();
    }

}
