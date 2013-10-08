/*
 * Copyright to the original author or authors.
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
package org.rioproject.impl.opstring

import org.codehaus.groovy.ast.CodeVisitorSupport
import org.codehaus.groovy.ast.expr.*
import org.codehaus.groovy.ast.stmt.ExpressionStatement
import org.codehaus.groovy.ast.stmt.Statement
import org.rioproject.resolver.Artifact
import org.rioproject.resolver.ResolverHelper

/**
 * A {@link CodeVisitorSupport} that handles declarations of {@code uses} or {@code using} to augment the classpath
 * of the {@link org.rioproject.opstring.OperationalString} being parsed.
 *
 * @author Dennis Reedy
 */
class UsesVisitor extends CodeVisitorSupport {
    private final GroovyClassLoader classLoader
    private final List<Statement> statements

    UsesVisitor(final GroovyClassLoader classLoader, final List<Statement> statements) {
        this.classLoader = classLoader
        this.statements = statements
    }

    @Override
    public void visitMethodCallExpression(final MethodCallExpression call) {
        if(call.getMethodAsString()=="using" || call.getMethodAsString()=="uses") {
            if(call.getArguments() instanceof ArgumentListExpression) {
                ArgumentListExpression args = (ArgumentListExpression)call.getArguments()
                for(Expression argExp : args.getExpressions()) {
                    String value = findValue(argExp.text, statements)
                    resolve(value, classLoader)
                }
            }
        }
        super.visitMethodCallExpression(call);
    }

    private String findValue(final String reference, final List<Statement> statements) {
        if(Artifact.isArtifact(reference)) {
            return reference;
        }
        String value = null;
        String[] items = reference.split(",");
        if(items.length==1) {
            try {
                new URL(items[0]);
                value = items[0];
            } catch (MalformedURLException e) {

                String[] parts = reference.split("\\.");
                for(Statement statement : statements) {
                    Expression expression = ((ExpressionStatement) statement).getExpression();
                    if(expression instanceof BinaryExpression) {
                        BinaryExpression expr = (BinaryExpression)expression;
                        if(expr.getLeftExpression().getText().equals(parts[0])) {
                            if(expr.getRightExpression() instanceof MapExpression) {
                                MapExpression mExpr = (MapExpression)expr.getRightExpression();
                                for(MapEntryExpression entry : mExpr.getMapEntryExpressions()) {
                                    if(entry.getKeyExpression().getText().equals(parts[1])) {
                                        value = entry.getValueExpression().getText();
                                        break;
                                    }
                                }
                            } else {
                                value = expr.getRightExpression().getText();
                                break;
                            }
                        }
                    }
                }
            }
        }
        return value;
    }

    void resolve(final String value, final GroovyClassLoader classLoader) {
        String[] classPath = ResolverHelper.getResolver().getClassPathFor(value);
        for(String item : classPath) {
            URL url = new File(item).toURI().toURL();
            if(!hasURL(classLoader.getURLs(), url))
                classLoader.addURL(url);
        }
    }

    boolean hasURL(final URL[] urls, final URL url) {
        boolean found = false;
        for(URL u : urls) {
            if(u.equals(url)) {
                found = true;
                break;
            }
        }
        return found;
    }
}
