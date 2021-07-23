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

import grails.gorm.validation.ConstrainedProperty
import groovy.transform.CompileStatic
import io.micronaut.core.naming.NameUtils
import org.grails.datastore.mapping.config.Property
import org.grails.datastore.mapping.model.PersistentProperty
import org.grails.orm.hibernate.cfg.JoinTable
import org.grails.orm.hibernate.cfg.PropertyConfig

import javax.persistence.EnumType
import javax.persistence.FetchType

/**
 * This class unifies access to PersistentProperty, ConstrainedProperty and Property objects which shares some common interactions.
 */
@CompileStatic
@SuppressWarnings('MethodCount')
class UnifiedProperty {

    private final PersistentProperty persistentProperty
    private final ConstrainedProperty constrainedProperty
    private final Property mappingProperty
    private final Object defaultValue

    UnifiedProperty(PersistentProperty persistentProperty, ConstrainedProperty constrainedProperty, Property mappingProperty, Object defaultValue) {
        this.defaultValue = defaultValue
        this.persistentProperty = persistentProperty
        this.constrainedProperty = constrainedProperty
        this.mappingProperty = mappingProperty
    }

    PersistentProperty getPersistentProperty() {
        return persistentProperty
    }

    ConstrainedProperty getConstrainedProperty() {
        return constrainedProperty
    }

    Property getMappingProperty() {
        return mappingProperty
    }

    Object getDefaultValue() {
        return defaultValue
    }

    Comparable getMax() {
        return mappingProperty.max ?: constrainedProperty.max
    }

    Comparable getMin() {
        return mappingProperty.min ?: constrainedProperty.min
    }

    List getInList() {
        return mappingProperty.inList ?: constrainedProperty.inList
    }

    Range getRange() {
        return constrainedProperty.range
    }

    Integer getScale() {
        return mappingProperty.scale >= 0 ? mappingProperty.scale : constrainedProperty.scale
    }

    Range getSize() {
        return constrainedProperty.size
    }

    boolean isBlank() {
        return constrainedProperty.blank
    }

    boolean isEmail() {
        return constrainedProperty.email
    }

    boolean isCreditCard() {
        return constrainedProperty.creditCard
    }

    String getMatches() {
        return constrainedProperty.matches
    }

    Object getNotEqual() {
        return constrainedProperty.notEqual
    }

    @SuppressWarnings('LineLength')
    Integer getMaxSize() {
        return (mappingProperty.maxSize ?: constrainedProperty.maxSize ?: (persistentProperty.type == String && !sqlType?.contains('text') ? 255 : null)) as Integer
    }

    Integer getMinSize() {
        return (mappingProperty.minSize ?: constrainedProperty.minSize) as Integer
    }

    boolean isNullable() {
        return persistentProperty.nullable || mappingProperty.nullable || constrainedProperty.nullable
    }

    boolean isUrl() {
        return constrainedProperty.url
    }

    FetchType getFetchType() {
        return mappingProperty.fetchStrategy
    }

    String getCascadeType() {
        return mappingProperty.cascade
    }

    @SuppressWarnings('ImplicitClosureParameter')
    List<String> getUniqueColumnNames() {
        if (mappingProperty.uniquenessGroup) {
            return ([persistentProperty.name] + mappingProperty.uniquenessGroup).collect { toColumnName(it) }
        }

        if (mappingProperty.unique) {
            return Collections.singletonList(columnName)
        }

        return Collections.emptyList()
    }

    @SuppressWarnings(['ImplicitClosureParameter', 'Instanceof'])
    Object getIndex() {
        if (mappingProperty instanceof PropertyConfig && mappingProperty.columns) {
            return mappingProperty.columns.first().index
        }

        return mappingProperty.index as Boolean
    }

    @SuppressWarnings('Instanceof')
    String getColumnName() {
        if (mappingProperty instanceof PropertyConfig) {
            return mappingProperty.columns.first().name ?: toColumnName(persistentProperty.name)
        }
        return toColumnName(persistentProperty.name)
    }

    EnumType getEnumType() {
        if (!Enum.isAssignableFrom(persistentProperty.type)) {
            return null
        }

        return mappingProperty.enumTypeObject ?: EnumType.STRING
    }

    @SuppressWarnings('Instanceof')
    String getSqlType() {
        if (mappingProperty instanceof PropertyConfig) {
            return mappingProperty.sqlType ?: mappingProperty.type
        }
        return null
    }

    @SuppressWarnings('Instanceof')
    String getSqlColumnName() {
        if (mappingProperty instanceof PropertyConfig && mappingProperty.columns) {
            return mappingProperty.columns.first().name
        }
        return null
    }

    @SuppressWarnings('Instanceof')
    JoinTable getJoinTable() {
        if (mappingProperty instanceof PropertyConfig && mappingProperty.joinTable) {
            return mappingProperty.joinTable
        }
        return null
    }

    @SuppressWarnings('Instanceof')
    String getSort() {
        if (mappingProperty instanceof PropertyConfig && mappingProperty.sort) {
            return mappingProperty.sort
        }
        return null
    }

    @SuppressWarnings('Instanceof')
    String getOrder() {
        if (mappingProperty instanceof PropertyConfig && mappingProperty.order) {
            return mappingProperty.order
        }
        return null
    }

    private static String toColumnName(String name) {
        return NameUtils.underscoreSeparate(name).toLowerCase()
    }

}
