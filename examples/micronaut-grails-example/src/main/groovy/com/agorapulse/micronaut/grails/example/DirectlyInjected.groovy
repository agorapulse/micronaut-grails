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

import javax.inject.Singleton

@Singleton
@CompileStatic
class DirectlyInjected {

    private final ManagerService managerService
    private final ApplicationContext micronautContext

    DirectlyInjected(ManagerService managerService, ApplicationContext micronautContext) {
        this.managerService = managerService
        this.micronautContext = micronautContext
    }

    @Override
    String toString() {
        return "DirectlyInjected"
    }

    ApplicationContext getMicronautContext() {
        return micronautContext
    }

}
