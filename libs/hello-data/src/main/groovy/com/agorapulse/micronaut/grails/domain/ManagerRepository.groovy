package com.agorapulse.micronaut.grails.domain

import io.micronaut.data.annotation.Repository
import io.micronaut.data.repository.CrudRepository

@Repository
interface ManagerRepository extends CrudRepository<Manager, Long> {

}