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

import com.agorapulse.micronaut.grails.MicronautGrailsApp
import com.agorapulse.micronaut.grails.domain.Manager
import grails.boot.config.GrailsAutoConfiguration

import groovy.transform.CompileStatic
import org.springframework.context.ConfigurableApplicationContext

@CompileStatic
class Application extends GrailsAutoConfiguration {

    static ConfigurableApplicationContext context

    static void main(String[] args) {
        context = MicronautGrailsApp.run(Application, args) {
            addPackage Manager.package.name
        }
    }

}
