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

import com.agorapulse.micronaut.grails.jpa.generator.MicronautJpaGenerator
import com.agorapulse.micronaut.grails.test.MicronautGrailsIntegration
import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Specification

@MicronautGrailsIntegration
class IntegrationSpec extends Specification {

    @Autowired MicronautJpaGenerator generator

    void 'application started'() {
        when:
            new URL("http://localhost:$serverPort").text
        then:
            noExceptionThrown()
    }

    void 'generate domains'() {
        given:
            File root = initRootDirectory()
        when:
            int generated = generator.generate(root)
        then:
            noExceptionThrown()

            generated == 1

            new File(root, 'micronaut/grails/example/User.groovy').exists()
            new File(root, 'micronaut/grails/example/UserRepository.groovy').exists()
    }

    private static File initRootDirectory() {
        File root = new File(System.getProperty('java.io.tmpdir'), 'micronaut-data-model')

        if (root.exists()) {
            root.deleteDir()
        }

        root.mkdirs()

        return root
    }

}
