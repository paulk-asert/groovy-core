/*
 * Copyright 2003-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package groovy.transform;

import org.codehaus.groovy.transform.GroovyASTTransformationClass;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Class annotation used to assist in the creation of map constructors in classes.
 * <p>
 * It allows you to write classes in this shortened form:
 * <pre>
 * {@code @MapConstructor} class Customer {
 *     String first, last
 *     int age
 * }
 * def c = new Customer(first:'Tom', last:'Jones', age:21)
 * </pre>
 * The {@code @MapConstructor} annotation instructs the compiler to execute an
 * AST transformation which adds the necessary constructor method(s) to your class.
 * <p>
 * Groovy normally simulates a map constructor for any class with a no-arg constructor plus
 * property setting using normal JavaBean-style conventions but this annotation creates an
 * explicit Map constructor. This is convenient when access to the object via Java is required
 * or when Groovy's simulated map constructor support doesn't provide the required behavior.
 *
 * @author Paul King
 * @since 2.2.0
 */
@java.lang.annotation.Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
@GroovyASTTransformationClass("org.codehaus.groovy.transform.MapConstructorASTTransformation")
public @interface MapConstructor {
    /**
     * By default, this annotation becomes a no-op if you provide your own constructor.
     * By setting {@code force=true} then the map constructor(s) will be added regardless of
     * whether existing constructors exist. It is up to you to avoid creating duplicate constructors.
     */
    boolean force() default false;
}
