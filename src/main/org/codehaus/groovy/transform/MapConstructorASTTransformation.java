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
package org.codehaus.groovy.transform;

import groovy.transform.MapConstructor;
import org.codehaus.groovy.ast.ASTNode;
import org.codehaus.groovy.ast.AnnotatedNode;
import org.codehaus.groovy.ast.AnnotationNode;
import org.codehaus.groovy.ast.ClassHelper;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.ConstructorNode;
import org.codehaus.groovy.ast.Parameter;
import org.codehaus.groovy.ast.PropertyNode;
import org.codehaus.groovy.ast.expr.ArgumentListExpression;
import org.codehaus.groovy.ast.expr.BooleanExpression;
import org.codehaus.groovy.ast.expr.ConstantExpression;
import org.codehaus.groovy.ast.expr.ConstructorCallExpression;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.ast.expr.MethodCallExpression;
import org.codehaus.groovy.ast.expr.PropertyExpression;
import org.codehaus.groovy.ast.expr.StaticMethodCallExpression;
import org.codehaus.groovy.ast.expr.VariableExpression;
import org.codehaus.groovy.ast.stmt.BlockStatement;
import org.codehaus.groovy.ast.stmt.EmptyStatement;
import org.codehaus.groovy.ast.stmt.ExpressionStatement;
import org.codehaus.groovy.ast.stmt.IfStatement;
import org.codehaus.groovy.ast.stmt.Statement;
import org.codehaus.groovy.ast.stmt.ThrowStatement;
import org.codehaus.groovy.control.CompilePhase;
import org.codehaus.groovy.control.SourceUnit;

import java.util.Collections;
import java.util.Map;

import static org.codehaus.groovy.transform.AbstractASTTransformUtil.assignStatement;
import static org.codehaus.groovy.transform.AbstractASTTransformUtil.equalsNullExpr;

/**
 * Handles generation of code for the {@code @MapConstructor} annotation.
 *
 * @author Paul King
 */
@GroovyASTTransformation(phase = CompilePhase.CANONICALIZATION)
public class MapConstructorASTTransformation extends AbstractASTTransformation {

    static final Class MY_CLASS = MapConstructor.class;
    static final ClassNode MY_TYPE = ClassHelper.make(MY_CLASS);
    static final String MY_TYPE_NAME = "@" + MY_TYPE.getNameWithoutPackage();
    private static final ClassNode MAP_TYPE = ClassHelper.makeWithoutCaching(Map.class, false);
    private static final ClassNode COLLECTIONS_TYPE = ClassHelper.makeWithoutCaching(Collections.class);
    private static final ClassNode CHECK_METHOD_TYPE = ClassHelper.make(ImmutableASTTransformation.class);

    public static void addMapConstructors(ClassNode cNode, boolean hasNoArg, String message) {
        Parameter[] parameters = new Parameter[1];
        parameters[0] = new Parameter(MAP_TYPE, "__namedArgs");
        BlockStatement code = new BlockStatement();
        VariableExpression namedArgs = new VariableExpression("__namedArgs");
        code.addStatement(new IfStatement(equalsNullExpr(namedArgs),
                illegalArgumentBlock(message),
                processArgsBlock(cNode, namedArgs)));
        ConstructorNode init = new ConstructorNode(ACC_PUBLIC, parameters, ClassNode.EMPTY_ARRAY, code);
        cNode.addConstructor(init);
        // add a no-arg constructor too
        if (!hasNoArg) {
            code = new BlockStatement();
            code.addStatement(new ExpressionStatement(new ConstructorCallExpression(ClassNode.THIS, new StaticMethodCallExpression(COLLECTIONS_TYPE, "emptyMap", MethodCallExpression.NO_ARGUMENTS))));
            init = new ConstructorNode(ACC_PUBLIC, Parameter.EMPTY_ARRAY, ClassNode.EMPTY_ARRAY, code);
            cNode.addConstructor(init);
        }
    }

    private static BlockStatement illegalArgumentBlock(String message) {
        BlockStatement block = new BlockStatement();
        block.addStatement(new ThrowStatement(new ConstructorCallExpression(ClassHelper.make(IllegalArgumentException.class),
                new ArgumentListExpression(new ConstantExpression(message)))));
        return block;
    }

    private static BlockStatement processArgsBlock(ClassNode cNode, VariableExpression namedArgs) {
        BlockStatement block = new BlockStatement();
        for (PropertyNode pNode : cNode.getProperties()) {
            if (pNode.isStatic()) continue;

            // if namedArgs.containsKey(propertyName) setProperty(propertyName, namedArgs.get(propertyName));
            BooleanExpression ifTest = new BooleanExpression(new MethodCallExpression(namedArgs, "containsKey", new ConstantExpression(pNode.getName())));
            Expression pExpr = new VariableExpression(pNode);
            Statement thenBlock = assignStatement(pExpr, new PropertyExpression(namedArgs, pNode.getName()));
            IfStatement ifStatement = new IfStatement(ifTest, thenBlock, EmptyStatement.INSTANCE);
            block.addStatement(ifStatement);
        }
        Expression checkArgs = new ArgumentListExpression(new VariableExpression("this"), namedArgs);
        block.addStatement(new ExpressionStatement(new StaticMethodCallExpression(CHECK_METHOD_TYPE, "checkPropNames", checkArgs)));
        return block;
    }

    public void visit(ASTNode[] nodes, SourceUnit source) {
        init(nodes, source);
        AnnotatedNode parent = (AnnotatedNode) nodes[1];
        AnnotationNode anno = (AnnotationNode) nodes[0];
        if (!MY_TYPE.equals(anno.getClassNode())) return;

        if (parent instanceof ClassNode) {
            ClassNode cNode = (ClassNode) parent;
            checkNotInterface(cNode, MY_TYPE_NAME);
            boolean force = memberHasValue(anno, "force", true);
            if (cNode.getDeclaredConstructors().size() == 0 || force) {
                boolean hasNoArg = hasAnnotation(cNode, CanonicalASTTransformation.MY_TYPE) || hasAnnotation(cNode, TupleConstructorASTTransformation.MY_TYPE);
                addMapConstructors(cNode, hasNoArg, "The class " + cNode.getName() + " was incorrectly initialized via the map constructor with null.");
            }
        }
    }
}
