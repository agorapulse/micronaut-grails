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

// tag::body[]
import com.agorapulse.micronaut.grails.jpa.generator.MicronautJpaGenerator
import com.agorapulse.micronaut.grails.test.MicronautGrailsIntegration
import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Specification

@MicronautGrailsIntegration
class IntegrationSpec extends Specification {

    @Autowired MicronautJpaGenerator generator

    void 'application started'() {
        expect:
            new URL("http://localhost:$serverPort/test/health").text == 'OK'
    }

}
// end::body[]
