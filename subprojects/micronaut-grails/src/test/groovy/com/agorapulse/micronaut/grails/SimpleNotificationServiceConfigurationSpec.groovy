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
package com.agorapulse.micronaut.grails

import com.agorapulse.micronaut.amazon.awssdk.sns.SimpleNotificationService
import com.agorapulse.micronaut.amazon.awssdk.sns.SimpleNotificationServiceConfiguration
import com.agorapulse.micronaut.amazon.awssdk.sqs.SimpleQueueService
import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import io.micronaut.inject.qualifiers.Qualifiers
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.TestPropertySource
import spock.lang.Specification

/**
 * Tests for micronaut Spring bean processor.
 */
@CompileDynamic
@ContextConfiguration(classes = [GrailsSimpleNotificationServiceConfig, MicronautGrailsConfiguration])
@TestPropertySource('classpath:com/agorapulse/micronaut/grails/SimpleNotificationServiceConfigurationSpec.properties')
class SimpleNotificationServiceConfigurationSpec extends Specification {

    private static final String ARN = 'arn::dummy'

    @Autowired
    ApplicationContext ctx

    void 'test sns configuration'() {
        expect:
            ctx.containsBean('simpleNotificationService')
        when:
            SimpleNotificationServiceConfiguration configuration = ctx.getBean(SimpleNotificationServiceConfiguration)
        then:
            configuration
            configuration.android
            configuration.android.arn == ARN
    }

}

@CompileStatic
@Configuration
class GrailsSimpleNotificationServiceConfig {

    @Bean
    MicronautBeanImporter myImporter() {
        return MicronautBeanImporter.create()
            .customize(PropertyTranslatingCustomizer
                .builder()
                .replacePrefix('aws.sns', 'grails.plugin.awssdk.sns')
            )
            .addByType(SimpleNotificationService)
            .addByType(SimpleNotificationServiceConfiguration)
            .addByQualifiers('notificationsQueueService', SimpleQueueService, Qualifiers.byName('notifications'))
            .addByQualifiers('syncQueueService', SimpleQueueService, Qualifiers.byName('notification-manager-device-sync'))
            .createMapForPropertiesStarting('aws.sqs.queues')
    }

}
