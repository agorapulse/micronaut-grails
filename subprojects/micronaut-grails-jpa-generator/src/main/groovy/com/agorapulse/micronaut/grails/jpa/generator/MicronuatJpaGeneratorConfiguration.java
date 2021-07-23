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
package com.agorapulse.micronaut.grails.jpa.generator;

import org.grails.datastore.gorm.validation.constraints.eval.ConstraintsEvaluator;
import org.grails.datastore.mapping.core.Datastore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.inject.Named;

@Configuration
public class MicronuatJpaGeneratorConfiguration {

    @Bean
    public static MicronautJpaGenerator micronautJpaGenerator(
            @Named("hibernateDatastore") Datastore datastore,
            ConstraintsEvaluator constraintsEvaluator
    ) {
        return new MicronautJpaGenerator(datastore, constraintsEvaluator);
    }

}
