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
package com.agorapulse.micronaut.grails.jpa.generator

import groovy.transform.CompileStatic
import org.grails.datastore.gorm.validation.constraints.eval.ConstraintsEvaluator
import org.grails.datastore.mapping.core.Datastore
import org.grails.datastore.mapping.model.PersistentEntity

/**
 * Experimental generator of JPA entities based on GORM entities.
 */
@CompileStatic
class MicronautJpaGenerator extends MicronautDataGenerator {

    MicronautJpaGenerator(Datastore datastore, ConstraintsEvaluator constraintsEvaluator) {
        super(datastore, constraintsEvaluator)
    }

    @Override
    @SuppressWarnings('LineLength')
    protected String generateRepository(PersistentEntity entity, String packageSuffix) {
        String datasourceDefinition = ''

        if (entity.mapping.mappedForm.datasources) {
            String datasource = entity.mapping.mappedForm.datasources.first()
            if (datasource != 'DEFAULT') {
                datasourceDefinition = "('$datasource')"
            }
        }

        return """
        package $entity.javaClass.package.name$packageSuffix

        import io.micronaut.data.annotation.Repository
        import io.micronaut.data.repository.CrudRepository

        @Repository$datasourceDefinition
        interface ${entity.javaClass.simpleName}Repository extends CrudRepository<${entity.javaClass.simpleName}, ${entity.identity.type.simpleName}> {

        }
        """.stripIndent().trim()
    }
}
