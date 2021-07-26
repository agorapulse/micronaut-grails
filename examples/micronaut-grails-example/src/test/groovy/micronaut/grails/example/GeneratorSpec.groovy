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
import com.agorapulse.testing.fixt.Fixt
import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Specification

@MicronautGrailsIntegration
class GeneratorSpec extends Specification {

    Fixt fixt = Fixt.create(GeneratorSpec)

    @Autowired MicronautJpaGenerator generator

    void 'generate domains'() {
        given:
            File root = initRootDirectory()
        when:
            int generated = generator.generate(root)
        then:
            noExceptionThrown()

            generated == 1

        when:
            File entityFile = new File(root, 'micronaut/grails/example/User.groovy')
            File repositoryFile = new File(root, 'micronaut/grails/example/UserRepository.groovy')
        then:
            entityFile.exists()
            entityFile.text.trim() == fixt.readText('User.groovy.txt').trim()

            repositoryFile.exists()
            repositoryFile.text.trim() == fixt.readText('UserRepository.groovy.txt').trim()
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
// end::body[]
