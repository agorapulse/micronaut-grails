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
import grails.compiler.GrailsCompileStatic
import grails.converters.JSON

import javax.inject.Inject
import javax.inject.Named

@GrailsCompileStatic
class TestController {

    // micronaut beans requires @Inject but can have any name
    @Inject DirectlyInjected someDirectlyInjected
    @Inject @Named("test") InjectedWithQualifier someInjectedWithQualifier


    // beans using MicronautImporter can be injected without @Inject but requires a matching name
    InjectedUsingBridge injectedUsingBridge
    InjectedUsingBridgeWithDifferentName otherInjected

    def health() {
        render "OK"
    }

    def index() {
        render([
            someDirectlyInjected     : someDirectlyInjected?.toString(),
            someInjectedWithQualifier: someInjectedWithQualifier?.toString(),
            injectedUsingBridge      : injectedUsingBridge?.toString(),
            otherInjected            : otherInjected?.toString()
        ] as JSON)
    }

    def contexts() {
        render([
            someDirectlyInjected: someDirectlyInjected?.micronautContext?.class?.name,
            injectedUsingBridge : injectedUsingBridge?.micronautContext?.class?.name,
        ] as JSON)
    }

    def values() {
        render([
            someDirectlyInjected: [
                valueWithMicronautPrefix: someDirectlyInjected?.valueWithMicronautPrefix,
                valueWithoutPrefix      : someDirectlyInjected?.valueWithoutPrefix,
                ignoredValue            : someDirectlyInjected?.ignoredValue,
            ],
            injectedUsingBridge : [
                valueWithMicronautPrefix: injectedUsingBridge?.valueWithMicronautPrefix,
                valueWithoutPrefix      : injectedUsingBridge?.valueWithoutPrefix,
                ignoredValue            : injectedUsingBridge?.ignoredValue,
            ],
        ] as JSON)
    }

    def managers() {
        render([
            someDirectlyInjected: someDirectlyInjected?.managerCount,
            injectedUsingBridge : injectedUsingBridge?.managerCount,
        ] as JSON)
    }

    def profiles() {
        render([
            someDirectlyInjected: someDirectlyInjected?.micronautContext?.environment?.activeNames,
            injectedUsingBridge: injectedUsingBridge?.micronautContext?.environment?.activeNames
        ] as JSON)
    }

}
