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

public enum CompatibilityMode {
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
    LEGACY(MicronautGrailsApp.ENVIRONMENT_LEGACY),

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
    BRIDGE(MicronautGrailsApp.ENVIRONMENT_BRIDGE),

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
    STRICT(MicronautGrailsApp.ENVIRONMENT_STRICT);

    private final String environment;

    CompatibilityMode(String environment) {
        this.environment = environment;
    }

    public String getEnvironment() {
        return environment;
    }

}
