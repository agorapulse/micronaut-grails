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
// tag::body[]
package micronaut.grails.example

import grails.compiler.GrailsCompileStatic
import micronaut.grails.example.other.Vehicle

@GrailsCompileStatic
class User {

    String userName
    String password
    String email

    static hasMany = [vehicles: Vehicle]

    static constraints = {
        userName blank: false, unique: true
        password size: 5..10, blank: false
        email email: true, blank: true
    }

}
// end::body[]
