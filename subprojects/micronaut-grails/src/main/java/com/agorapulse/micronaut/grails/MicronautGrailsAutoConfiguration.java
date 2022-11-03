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

import grails.boot.config.GrailsAutoConfiguration;
import io.micronaut.context.env.Environment;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class MicronautGrailsAutoConfiguration extends GrailsAutoConfiguration {

    @Override
    public Collection<Package> packages() {
        List<Package> packages = new ArrayList<>(super.packages());
        packages.addAll(getPackages());
        return packages;
    }

    @Override
    public Collection<String> packageNames() {
        List<String> packageNames = new ArrayList<>(super.packageNames());
        packageNames.addAll(getPackageNames());
        return packageNames;
    }

    public void configureEnvironment(Environment environment) {
        // for some reasons overriding the packages() and packageNames() does not work here
        // (maybe some AST transformation in the subclass?)
        List<Package> packages = new ArrayList<>(packages());
        packages.addAll(getPackages());
        List<String> packageNames = packages.stream().map(Package::getName).collect(Collectors.toList());
        packageNames.addAll(packageNames());
        packageNames.addAll(getPackageNames());

        packageNames.forEach(environment::addPackage);

        doWithMicronautEnvironment(environment);
    }

    public CompatibilityMode getCompatibilityMode() {
        return CompatibilityMode.STRICT;
    }

    public Collection<Package> getPackages() {
        return Collections.emptyList();
    }

    public Collection<String> getPackageNames() {
        return Collections.emptyList();
    }

    protected void doWithMicronautEnvironment(Environment environment) {
        // to be overridden by the application class
    }

}
