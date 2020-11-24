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

import com.agorapulse.micronaut.grails.example.DirectlyInjected
import com.agorapulse.micronaut.grails.example.InjectedUsingBridge
import com.agorapulse.micronaut.grails.example.InjectedUsingBridgeWithDifferentName
import com.agorapulse.micronaut.grails.example.InjectedWithQualifier
import grails.converters.JSON
import groovy.transform.CompileStatic

import javax.inject.Inject
import javax.inject.Named

@CompileStatic
class ServicesController {

    // micronaut beans requires @Inject but can have any name
    @Inject DirectlyInjected someDirectlyInjected
    @Inject @Named("test") InjectedWithQualifier someInjectedWithQualifier

    // beans using MicronautImporter can be injected without @Inject but requires a matching name
    InjectedUsingBridge injectedUsingBridge
    InjectedUsingBridgeWithDifferentName otherInjected

    def index() {
        render([
            someDirectlyInjected: someDirectlyInjected?.toString(),
            someInjectedWithQualifier: someInjectedWithQualifier.toString(),
            injectedUsingBridge: injectedUsingBridge.toString(),
            otherInjected: otherInjected.toString()
        ] as JSON)
    }

    def identical() {
        render([
            identical: someDirectlyInjected.micronautContext == injectedUsingBridge.micronautContext
        ] as JSON)
    }

}
