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
package com.agorapulse.micronaut.grails.domain

// tag::body[]
import com.agorapulse.micronaut.grails.jpa.generator.MicronautJpaGenerator
import com.agorapulse.testing.fixt.Fixt
import io.micronaut.context.ApplicationContext
import org.grails.datastore.gorm.validation.constraints.eval.DefaultConstraintEvaluator
import org.grails.orm.hibernate.HibernateDatastore
import spock.lang.AutoCleanup
import spock.lang.Specification

/**
 * Example specification generating JPA entities from GORM entities.
 */
class MicronautGeneratorSpec extends Specification {

    Fixt fixt = Fixt.create(MicronautGeneratorSpec)

    @AutoCleanup ApplicationContext context = ApplicationContext.run()

    MicronautJpaGenerator generator = new MicronautJpaGenerator(
        context.getBean(HibernateDatastore),
        new DefaultConstraintEvaluator()
    )

    void 'generate domains'() {
        given:
            File root = initRootDirectory()
        when:
            generator.generate(root)
        then:
            noExceptionThrown()

        when:
            File entityFile = new File(root, 'com/agorapulse/micronaut/grails/domain/Manager.groovy')
            File repositoryFile = new File(root, 'com/agorapulse/micronaut/grails/domain/ManagerRepository.groovy')
        then:
            entityFile.exists()
            entityFile.text.trim() == fixt.readText('Manager.groovy.txt').trim()

            repositoryFile.exists()
            repositoryFile.text.trim() == fixt.readText('ManagerRepository.groovy.txt').trim()
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
