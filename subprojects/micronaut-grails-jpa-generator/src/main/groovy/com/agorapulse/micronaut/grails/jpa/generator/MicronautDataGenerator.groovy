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
import org.grails.datastore.gorm.validation.constraints.eval.ConstraintsEvaluator
import org.grails.datastore.mapping.config.Property
import org.grails.datastore.mapping.core.Datastore
import org.grails.datastore.mapping.model.PersistentEntity
import org.grails.datastore.mapping.model.PersistentProperty
import org.grails.datastore.mapping.model.types.ManyToOne
import org.grails.datastore.mapping.model.types.OneToMany
import org.grails.datastore.mapping.model.types.OneToOne
import org.grails.datastore.mapping.model.types.Simple

import javax.persistence.FetchType
import javax.persistence.PostLoad
import javax.persistence.PostPersist
import javax.persistence.PostRemove
import javax.persistence.PostUpdate
import javax.persistence.PrePersist
import javax.persistence.PreRemove
import javax.persistence.PreUpdate

/**
 * Experimental generator of JPA entities based on GORM entities.
 */
@CompileStatic
@SuppressWarnings(['MethodSize', 'DuplicateStringLiteral'])
abstract class MicronautDataGenerator {

    private static final Map<String, Class> GORM_HOOKS_TO_ANNOTATIONS = [
            beforeInsert: PrePersist,
            beforeUpdate: PreUpdate,
            beforeDelete: PreRemove,
            afterInsert : PostPersist,
            afterUpdate : PostUpdate,
            afterDelete : PostRemove,
            onLoad      : PostLoad,
    ].asImmutable() as Map<String, Class>

    protected final Datastore datastore
    protected final ConstraintsEvaluator constraintsEvaluator

    protected MicronautDataGenerator(Datastore datastore, ConstraintsEvaluator constraintsEvaluator) {
        this.constraintsEvaluator = constraintsEvaluator
        this.datastore = datastore
    }

    @SuppressWarnings('NestedForLoop')
    int generate(File root, String packageSuffix = '.model') {
        Collection<PersistentEntity> entities = datastore.mappingContext.persistentEntities
        Collection<Class> entityClasses = entities*.javaClass
        entities.each { PersistentEntity entity ->
            File packageDirectory = new File(root, (entity.javaClass.package.name + packageSuffix).replace('.', File.separator))
            packageDirectory.mkdirs()

            File entityFile = new File(packageDirectory, "${entity.javaClass.simpleName}.groovy")
            entityFile.text = generateEntity(entity, entityClasses, packageSuffix)

            File repositoryFile = new File(packageDirectory, "${entity.javaClass.simpleName}Repository.groovy")
            repositoryFile.text = generateRepository(entity, packageSuffix)
        }

        List<Class> requiredEnums = []
        for (PersistentEntity entity in datastore.mappingContext.persistentEntities) {
            for (PersistentProperty property in entity.persistentProperties) {
                if (property.type.enum) {
                    requiredEnums.add(property.type)
                }
            }
        }

        requiredEnums.each { Class enumType ->
            copyEnum(root, enumType)
        }

        return entities.size()
    }

    @SuppressWarnings([
        'AbcMetric',
        'UnnecessaryObjectReferences',
        'ImplicitClosureParameter',
        'Instanceof',
        'LineLength',
        'MethodSize',
        'UnnecessaryCollectCall',
        'CyclomaticComplexity',
    ])
    String generateEntity(PersistentEntity entity, Collection<Class> persistentEntitiesTypes, String packageSuffix) {
        List<UnifiedProperty> unifiedProperties = collectUnifiedProperties(entity)
        Set<String> imports = new TreeSet<>(unifiedProperties.collect {
            if (it.persistentProperty instanceof OneToMany) {
                return (it.persistentProperty as OneToMany).associatedEntity.javaClass
            }
            return it.persistentProperty.type
        }.findAll {
            it && !it.primitive && it.package && it.package != entity.javaClass.package && it.package.name != 'java.util' && it.package.name != 'java.lang'
        }.collect {
            if (it in persistentEntitiesTypes) {
                return it.package.name + packageSuffix + '.' + it.simpleName
            }
            return it.name
        })

        StringWriter bodyStringWriter = new StringWriter()
        PrintWriter body = new PrintWriter(bodyStringWriter)

        imports.add('javax.persistence.Entity')
        imports.add(CompileStatic.name)

        if (entity.javaClass.name != entity.name) {
            body.println('@CompileStatic')
            body.println("""@Entity(name="$entity.name")""")
        } else {
            body.println('@Entity')
            body.println('@CompileStatic')
        }

        List<UnifiedProperty> uniqueProperties = unifiedProperties.findAll { !it.index && it.uniqueColumnNames }
        List<UnifiedProperty> indexedProperties = unifiedProperties.findAll { it.index }

        if (uniqueProperties || indexedProperties) {
            imports.add('javax.persistence.Table')
            body.println('@Table(')
            if (indexedProperties) {
                imports.add('javax.persistence.Index')
                body.println('    indexes = [')
                body.println(indexedProperties.collect { UnifiedProperty indexed ->
                    StringWriter indexAnnotation = new StringWriter()
                    indexAnnotation.print('        @Index(')

                    if (indexed.index instanceof String) {
                        indexAnnotation.print("name = '$indexed.index', ")
                    }
                    if (indexed.uniqueColumnNames.size() == 1) {
                        indexAnnotation.print('unique = true, ')
                    }
                    indexAnnotation.print("columnList = '${indexed.columnName}')")
                    return indexAnnotation.toString()
                }.join(',\n'))
                if (uniqueProperties) {
                    body.println('    ],')
                } else {
                    body.println('    ]')
                }
            }
            if (uniqueProperties) {
                imports.add('javax.persistence.UniqueConstraint')
                body.println('    uniqueConstraints = [')
                body.println(uniqueProperties.collect { it -> "        @UniqueConstraint(columnNames = [${it.uniqueColumnNames.collect { column -> "'$column'" }.join(', ')}])" }.join(',\n'))
                body.println('    ]')
            }
            body.println(')')
        }

        body.println("class $entity.javaClass.simpleName {")
        body.println()

        if (entity.identity) {
            imports.add('javax.persistence.Id')
            imports.add('javax.persistence.GeneratedValue')
            imports.add('javax.persistence.GenerationType')

            body.println('    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)')
            body.println("    $entity.identity.type.simpleName $entity.identity.name")
            body.println()
        }

        if (entity.versioned) {
            imports.add('javax.persistence.Version')
            body.println('    @Version')
            body.println("    $entity.version.type.simpleName $entity.version.name")
            body.println()
        }

        for (UnifiedProperty property in unifiedProperties) {
            printProperty(property, body, imports)
        }

        GORM_HOOKS_TO_ANNOTATIONS.each { String methodName, Class annotation ->
            if (entity.javaClass.declaredMethods.any { it.name == methodName }) {
                imports.add(annotation.name)
                body.println("    @$annotation.simpleName")
                body.println("    void $methodName() {")
                body.println('        // TODO: reimplement')
                body.println('    }')
                body.println()
            }
        }

        body.println('}')

        StringWriter finalWriter = new StringWriter()
        PrintWriter finalPrintWriter = new PrintWriter(finalWriter)
        finalPrintWriter.println "package $entity.javaClass.package.name$packageSuffix"
        finalPrintWriter.println()

        imports.each { String imported ->
            finalPrintWriter.println("import $imported")
        }

        finalPrintWriter.println()
        finalPrintWriter.println(bodyStringWriter.toString())

        return finalWriter.toString()
    }

    @SuppressWarnings('LineLength')
    private static void printProperty(
            UnifiedProperty unified,
            PrintWriter writer,
            Set<String> imports
    ) {
        switch (unified.persistentProperty) {
            case Simple:
                printSimpleProperty(writer, unified, imports)
                break
            case OneToOne:
                printOneToOne(writer, unified, imports)
                break
            case OneToMany:
                printOneToMany(unified, writer, imports)
                break
            case ManyToOne:
                printManyToOne(writer, unified, imports)
                break
            default:
                writer.println("    // TODO: property type ${unified.persistentProperty.getClass()}. not yet handled")
                writer.print("    $unified.persistentProperty.type.simpleName $unified.persistentProperty.name")
                writer.println()
                writer.println()
        }
    }

    private static void printManyToOne(PrintWriter writer, UnifiedProperty unified, Set<String> imports) {
        imports.add('javax.persistence.ManyToOne')
        writer.print('    @ManyToOne')
        if (unified.mappingProperty.lazy || unified.mappingProperty.fetchStrategy && unified.mappingProperty.fetchStrategy != FetchType.EAGER) {
            imports.add('javax.persistence.FetchType')
            writer.print('(fetch = FetchType.LAZY)')
        }
        writer.println()
        writer.println("    $unified.persistentProperty.type.simpleName $unified.persistentProperty.name")
        writer.println()
    }

    @SuppressWarnings(['AbcMetric', 'LineLength'])
    private static void printOneToMany(UnifiedProperty unified, PrintWriter writer, Set<String> imports) {
        OneToMany oneToMany = unified.persistentProperty as OneToMany
        List<CharSequence> parts = []

        if (oneToMany.referencedPropertyName) {
            parts.add("mappedBy = '$oneToMany.referencedPropertyName'")
        }

        if (unified.mappingProperty.lazy == Boolean.FALSE || unified.mappingProperty.fetchStrategy && unified.mappingProperty.fetchStrategy != FetchType.LAZY) {
            imports.add('javax.persistence.FetchType')
            parts.add('fetch = FetchType.EAGER')
        }

        if (unified.mappingProperty.cascade == 'all-delete-orphan') {
            imports.add('javax.persistence.CascadeType')
            parts.add('cascade = CascadeType.ALL, orphanRemoval = true')
        } else if (unified.mappingProperty.cascade) {
            imports.add('javax.persistence.CascadeType')
            parts.add("cascade = CascadeType.${unified.mappingProperty.cascade.toUpperCase()}")
        } else if (unified.mappingProperty.cascades) {
            imports.add('javax.persistence.CascadeType')
            parts.add("cascade = CascadeType.${unified.mappingProperty.cascades.first()}")
        }

        imports.add('javax.persistence.OneToMany')

        if (parts) {
            writer.println("    @OneToMany(${parts.join(', ')})")
        } else {
            writer.println('    @OneToMany')
        }

        if (unified.joinTable && unified.joinTable.key && unified.joinTable.column) {
            imports.add('javax.persistence.JoinTable')
            imports.add('javax.persistence.JoinColumn')
            writer.println("    @JoinTable(joinColumns = @JoinColumn(name = '$unified.joinTable.key.name'), inverseJoinColumns = @JoinColumn(name = '$unified.joinTable.column.name'))")
        }

        if (unified.sort) {
            imports.add('javax.persistence.OrderBy')
            if (unified.order) {
                writer.println("    @OrderBy('${unified.sort} ${unified.order.toUpperCase()}')")
            } else {
                writer.println("    @OrderBy('${unified.sort}')")
            }
        }

        writer.println("    ${unified.sort ? 'List' : 'Set'}<$oneToMany.associatedEntity.javaClass.simpleName> $unified.persistentProperty.name")
        writer.println()
    }

    private static void printOneToOne(PrintWriter writer, UnifiedProperty unified, Set<String> imports) {
        imports.add('javax.persistence.OneToOne')
        writer.print('    @OneToOne')
        if (unified.mappingProperty.lazy || unified.mappingProperty.fetchStrategy && unified.mappingProperty.fetchStrategy != FetchType.EAGER) {
            imports.add('javax.persistence.FetchType')
            writer.print('(fetch = FetchType.LAZY)')
        }
        writer.println()
        writer.println("    $unified.persistentProperty.type.simpleName $unified.persistentProperty.name")
        writer.println()
    }

    @SuppressWarnings(['AbcMetric', 'CyclomaticComplexity', 'MethodSize'])
    private static void printSimpleProperty(
            PrintWriter writer,
            UnifiedProperty unified,
            Set<String> imports
    ) {
        if (unified.nullable) {
            imports.add('javax.annotation.Nullable')
            writer.println('    @Nullable')
        } else if (!unified.persistentProperty.type.primitive) {
            imports.add('javax.validation.constraints.NotNull')
            writer.println('    @NotNull')
        }

        if (unified.range == null && unified.min != null) {
            imports.add('javax.validation.constraints.Min')
            writer.println("    @Min(${unified.min}L)")
        } else if (unified.range == null && unified.min != null) {
            imports.add('javax.validation.constraints.Min')
            writer.println("    @Min(${unified.min}L)")
        }

        if (unified.range == null && unified.max != null) {
            imports.add('javax.validation.constraints.Max')
            writer.println("    @Max(${unified.max}L)")
        } else if (unified.range == null && unified.max != null) {
            imports.add('javax.validation.constraints.Max')
            writer.println("    @Max(${unified.max}L)")
        }

        if (unified.range != null) {
            imports.add('javax.validation.constraints.Max')
            imports.add('javax.validation.constraints.Min')
            writer.println("    @Min(${unified.range.from}L)")
            writer.println("    @Max(${unified.range.to}L)")
        }

        if (unified.size != null) {
            imports.add('javax.validation.constraints.Size')
            writer.println("    @Size(min = $unified.size.from, max = $unified.size.to)")
        }

        if (unified.scale != null) {
            imports.add('javax.validation.constraints.Digits')
            writer.println("    @Digits(fraction = $unified.scale)")
        }

        if (!unified.blank) {
            imports.add('javax.validation.constraints.NotBlank')
            writer.println('    @NotBlank')
        }

        if (unified.persistentProperty.type == String && unified.email) {
            imports.add('javax.validation.constraints.Email')
            writer.println('    @Email')
        }

        if (unified.persistentProperty.type == String && unified.matches != null) {
            imports.add('javax.validation.constraints.Pattern')
            writer.println("    @Pattern(regexp = '$unified.matches')")
        }

        if (unified.inList != null) {
            imports.add('javax.validation.constraints.Pattern')
            writer.println("    @Pattern(regexp = '${unified.inList.join('|')}')")
        }

        if (unified.persistentProperty.type == String && unified.creditCard) {
            writer.println('    // TODO: must be credit card')
        }

        if (unified.notEqual) {
            writer.println("    // TODO: must not be equal to '$unified.notEqual'")
        }

        if (unified.minSize != null && unified.maxSize != null) {
            imports.add('javax.validation.constraints.Size')
            writer.println("    @Size(min = $unified.minSize, max = $unified.maxSize)")
        } else if (unified.maxSize != null) {
            imports.add('javax.validation.constraints.Size')
            writer.println("    @Size(max = $unified.maxSize)")
        } else if (unified.minSize != null) {
            imports.add('javax.validation.constraints.Size')
            writer.println("    @Size(min = $unified.minSize)")
        }

        if (unified.enumType) {
            imports.add('javax.persistence.Enumerated')
            imports.add('javax.persistence.EnumType')
            writer.println("    @Enumerated(EnumType.$unified.enumType)")
        }

        if (unified.sqlType || unified.sqlColumnName) {
            imports.add('javax.persistence.Column')
            writer.print('    @Column(')
            if (unified.sqlType) {
                writer.print("columnDefinition = '${unified.sqlType}'")
                if (unified.sqlColumnName) {
                    writer.print(', ')
                }
            }
            if (unified.sqlColumnName) {
                writer.print("name = '${unified.sqlColumnName}'")
            }

            writer.println(')')
        }

        if (unified.persistentProperty.name == 'dateCreated') {
            imports.add('io.micronaut.data.annotation.DateCreated')
            writer.println('    @DateCreated')
        }

        if (unified.persistentProperty.name == 'lastUpdated') {
            imports.add('io.micronaut.data.annotation.DateUpdated')
            writer.println('    @DateUpdated')
        }

        writer.print("    $unified.persistentProperty.type.simpleName $unified.persistentProperty.name")

        Object defaultValue = unified.defaultValue
        if (defaultValue != null && defaultValue != 0 && defaultValue != Boolean.FALSE) {
            // further - column definition?
            switch (defaultValue) {
                case CharSequence:
                    writer.print " = '${defaultValue}'"
                    break
                case Number:
                case Boolean:
                    writer.print " = ${defaultValue}"
                    break
                case Enum:
                    writer.print " = ${unified.persistentProperty.type.simpleName}.${defaultValue}"
                    break
                case Date:
                    long delta = Math.abs(System.currentTimeMillis() - (defaultValue as Date).time)
                    if (delta < 1000) {
                        writer.print ' = new Date()'
                    } else {
                        writer.print " // TODO: defaults to '${defaultValue}'"
                    }
                    break
                case Currency:
                    writer.print(" = Currency.getInstance('$defaultValue')")
                    break
                case Locale.default:
                    writer.print(' = Locale.default')
                    break
                case Locale.ENGLISH:
                    writer.print(' = Locale.ENGLISH')
                    break
                case Locale:
                    Locale defaultLocale = defaultValue as Locale
                    writer.print(" = new Locale('${defaultLocale.language}', '${defaultLocale.country}')")
                    break
                case TimeZone.default:
                    writer.print(' = TimeZone.default')
                    break
                default:
                    writer.print " // TODO: defaults to '${defaultValue}'"
            }
        }

        writer.println()
        writer.println()
    }

    @SuppressWarnings('LineLength')
    protected abstract String generateRepository(PersistentEntity entity, String packageSuffix)

    private static void copyEnum(File root, Class enumType) {
        if (!enumType.enum) {
            return
        }

        File packageDirectory = new File(root, enumType.package.name.replace('.', File.separator))
        packageDirectory.mkdirs()

        File enumFile = new File(packageDirectory, "${enumType.simpleName}.java")

        List<Object> enumValues = enumType.getMethod('values').invoke(null) as List<Object>

        enumFile.text = """
        package $enumType.package.name;

        public enum $enumType.simpleName {
            // TODO: migrate original enum
            ${enumValues.join(', ')};
        }
        """.stripIndent().trim()
    }

    @SuppressWarnings('ImplicitClosureParameter')
    private List<UnifiedProperty> collectUnifiedProperties(PersistentEntity entity) {
        Object newInstance = entity.newInstance()
        Map<String, ConstrainedProperty> constraintedProperties = constraintsEvaluator.evaluate(entity.javaClass)
        List<UnifiedProperty> unifiedProperties = entity
                .persistentProperties
                .sort(false) { it.name }
                .findAll { entity.identity?.name != it.name && entity.version?.name != it.name }
                .collect { PersistentProperty property ->
                    ConstrainedProperty constrainedProperty = constraintedProperties.get(property.name)
                    Property mappingProperty = property.mapping.mappedForm as Property
                    Object value = property.reader.read(newInstance)

                    return new UnifiedProperty(property, constrainedProperty, mappingProperty, value)
                }
        return unifiedProperties
    }

}
