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
package com.agorapulse.micronaut.grails.example

import com.agorapulse.micronaut.grails.domain.ManagerService
import groovy.transform.CompileStatic
import io.micronaut.context.ApplicationContext
import io.micronaut.context.annotation.Value

import javax.inject.Singleton

@Singleton
@CompileStatic
class InjectedUsingBridge {

    private final ManagerService managerService
    private final ApplicationContext micronautContext
    private final String valueWithMicronautPrefix
    private final String valueWithoutPrefix
    private final String ignoredvalue

    InjectedUsingBridge(
        ManagerService managerService,
        ApplicationContext micronautContext,
        @Value('${micronaut.foo.bar:}') String valueWithMicronautPrefix,
        @Value('${bar.foo:}') String valueWithoutPrefix,
        @Value('${ex.foo.bar:}') String ignoredvalue

    ) {
        this.managerService = managerService
        this.micronautContext = micronautContext
        this.valueWithMicronautPrefix = valueWithMicronautPrefix
        this.valueWithoutPrefix = valueWithoutPrefix
        this.ignoredvalue = ignoredvalue
    }

    Number getManagerCount() {
        return managerService?.count()
    }

    ApplicationContext getMicronautContext() {
        return micronautContext
    }

    String getValueWithMicronautPrefix() {
        return valueWithMicronautPrefix
    }

    String getValueWithoutPrefix() {
        return valueWithoutPrefix
    }

    String getIgnoredValue() {
        return ignoredvalue
    }

    @Override
    public String toString() {
        return "InjectedUsingBridge";
    }

}
