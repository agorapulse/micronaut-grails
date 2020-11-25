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

import com.agorapulse.gru.Gru
import com.agorapulse.gru.http.Http
import com.agorapulse.micronaut.grails.domain.ManagerService
import com.agorapulse.micronaut.grails.example.DirectlyInjected
import com.agorapulse.micronaut.grails.example.InjectedUsingBridge
import org.junit.Rule
import org.springframework.context.ConfigurableApplicationContext
import spock.lang.Shared
import spock.lang.Specification

abstract class AbstractApplicationSpec extends Specification {

    @Shared ConfigurableApplicationContext context

    @Rule Gru gru = Gru.equip(Http.steal(this)).prepare('http://localhost:9999')

    abstract Class<?> getApplicationClass()

    void setupSpec() {
        Throwable th = null
        Thread.start {
            try {
                applicationClass.main()
            } catch (Throwable e) {
                th = e
            }
        }

        for (i in 0..<1000) {
            if (applicationClass.context != null) {
                break;
            }
            if (th != null) {
                throw th
            }
            Thread.sleep(100)
        }
        assert applicationClass.context, "application context is set"

        context = applicationClass.context
    }

    void cleanupSpec() {
        if (applicationClass.context?.isActive()) {
            try {
                applicationClass.context.stop()
            } catch (IllegalStateException ise) {
                if (!ise.message.contains('has been closed already')) {
                    throw ise
                }
            }
        }
    }

    void 'application context is set'() {
        expect:
            context
    }

    void 'package is applied'() {
        expect:
            context.getBean(DirectlyInjected).micronautContext.environment.packages.contains(ManagerService.package.name)
            context.getBean(InjectedUsingBridge).micronautContext.environment.packages.contains(ManagerService.package.name)
    }

    void 'fetch services'() {
        expect:
            gru.test {
                get '/test/index'
                expect {
                    json 'services.json'
                }
            }
    }

    void 'only one context is created'() {
        expect:
            gru.test {
                get '/test/contexts'
                expect {
                    json 'contexts.json'
                }
            }
    }

    void 'check property translation and exclusion works'() {
        expect:
            gru.test {
                get '/test/values'
                expect {
                    json 'values.json'
                }
            }
    }

}
