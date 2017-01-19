/**
 * Copyright © 2010-2013 Nokia
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jsonschema2pojo.util;

import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableMap;
import com.fasterxml.jackson.databind.JsonNode;
import com.sun.codemodel.*;
import org.jsonschema2pojo.Schema;
import org.jsonschema2pojo.rules.RuleFactory;

import java.util.HashMap;

import static org.apache.commons.lang3.StringUtils.capitalize;

public class WritableMapHelper {
    private static HashMap<String, String> baseMapper = new HashMap<String, String>();

    static {
        baseMapper.put("String", "String");
        baseMapper.put("Double", "Double");
        baseMapper.put("Integer", "Int");
        baseMapper.put("Boolean", "Boolean");
    }

    private static HashMap<String, String> getMapper = new HashMap<String, String>(baseMapper);

    static {
        getMapper.put("com.facebook.react.bridge.WritableMap", "Map");
    }

    private static HashMap<String, String> setMapper = new HashMap<String, String>(baseMapper);

    static {
        setMapper.put("com.facebook.react.bridge.ReadableMap", "Map");
    }

    private JExpression makeImpression(final String expression) {
        return new JExpressionImpl() {
            public void generate(JFormatter f) {
                f.p(expression);
            }
        };
    }

    public void addCreateFromMap(JDefinedClass jclass) {
        JClass exceptionClass = jclass.owner().ref("RequiredFieldException");

        JMethod createFromMap = jclass.method(JMod.PUBLIC | JMod.STATIC, jclass, "fromReadableMap")._throws(exceptionClass);
        JVar in = createFromMap.param(com.facebook.react.bridge.ReadableMap.class, "map");

        JInvocation invoc = JExpr._new(jclass);

        for (JFieldVar f : jclass.fields().values()) {
            if ((f.mods().getValue() & JMod.STATIC) == JMod.STATIC) {
                continue;
            }

            final JExpression inst = makeImpression("Util");

            String key = f.type().name();

            if (baseMapper.containsKey(key)) {
                invoc.arg(JExpr.invoke(inst, "get" + baseMapper.get(key)).arg(in).arg(f.name()));
            } else {
                final JExpression expr = makeImpression(key);

                JInvocation ivGetMap = JExpr.invoke(inst, "getMap").arg(in).arg(f.name());
                JInvocation ivFromMap = JExpr.invoke(expr, "fromReadableMap");

                invoc.arg(ivFromMap.arg(ivGetMap));
            }
        }

        createFromMap.body()._return(invoc);
    }

    public void addWriteToMap(JDefinedClass jclass) {
        JMethod method = jclass.method(JMod.PUBLIC, com.facebook.react.bridge.WritableMap.class, "toWritableMap");

        JClass mapType = jclass.owner().ref(com.facebook.react.bridge.WritableNativeMap.class);
        JVar instance = method.body().decl(mapType, "result", JExpr._new(mapType));

        for (JFieldVar f : jclass.fields().values()) {
            if ((f.mods().getValue() & JMod.STATIC) == JMod.STATIC) {
                continue;
            }

            final JExpression expr = makeImpression("\"" + f.name() + "\"");
            final JExpression inst = makeImpression("Util");

            String key = f.type().name();

            if (baseMapper.containsKey(key)) {
                method.body().invoke(inst, "put" + baseMapper.get(key)).arg(instance).arg(expr).arg(f);
            } else {
                method.body().invoke(inst, "putMap").arg(instance).arg(expr).arg(f.invoke("toWritableMap"));
            }
        }

        method.body()._return(instance);
    }

    public void createException(RuleFactory ruleFactory, JsonNode node, JPackage _package, Schema schema) {
        JDefinedClass jclass = null;
        try {
            jclass = _package._class(getClassName(ruleFactory, "RequiredFieldException", node, _package))._extends(Exception.class);

            schema.setJavaTypeIfEmpty(jclass);

            JMethod methodBody = jclass.constructor(JMod.PUBLIC);
            JVar var = methodBody.param(String.class, "message");
            methodBody.body().invoke("super").arg(var);

        } catch (JClassAlreadyExistsException e) {
            e.printStackTrace();
        }
    }

    public void createUtil(RuleFactory ruleFactory, JsonNode node, JPackage _package, Schema schema) {
        JDefinedClass jclass = null;
        try {
            jclass = _package._class(getClassName(ruleFactory, "Util", node, _package));

        } catch (JClassAlreadyExistsException e) {
            e.printStackTrace();
        }

        schema.setJavaTypeIfEmpty(jclass);

        JClass stringClass = jclass.owner().ref(String.class);

        for (String k : getMapper.keySet()) {
            String s = getMapper.get(k);
            JClass mapType = jclass.owner().ref(k);

            JMethod method = jclass.method(JMod.PUBLIC | JMod.STATIC, jclass.owner().VOID, "put" + s);
            method.param(WritableMap.class, "map");
            JVar keyVar = method.param(stringClass, "key");
            JVar valueVar = method.param(mapType, "value");

            JBlock block = method.body();

            JConditional condition = block._if(makeImpression(valueVar.name() + " != null"));
            condition._then().invoke(makeImpression("map"), "put" + s).arg(keyVar).arg(valueVar);
            condition._else().invoke(makeImpression("map"), "putNull").arg(keyVar);
        }

        for (String k : setMapper.keySet()) {
            String s = setMapper.get(k);
            JClass mapType = jclass.owner().ref(k);

            JMethod method = jclass.method(JMod.PUBLIC | JMod.STATIC, mapType, "get" + s);
            method.param(ReadableMap.class, "map");
            JVar keyVar = method.param(stringClass, "key");

            JBlock block = method.body();

            JConditional condition = block._if(makeImpression("map.isNull(key)"));
            condition._then()._return(makeImpression("null"));
            condition._else()._return(JExpr.invoke(makeImpression("map"), "get" + s).arg(keyVar));
        }

        JClass exceptionClass = jclass.owner().ref("com.walmartlabs.electrode.mobile.RequiredFieldException");

        JMethod method = jclass.method(JMod.PUBLIC | JMod.STATIC, jclass.owner().VOID, "assertValid")._throws(exceptionClass);
        JVar a = method.param(Object.class, "value");
        JBlock block = method.body();

        JConditional condition = block._if(makeImpression(a.name() + " == null"));
        condition._then()._throw(JExpr._new(exceptionClass).arg("Assert Required Value Failed"));

    }

    private String getClassName(RuleFactory ruleFactory, String nodeName, JsonNode node, JPackage _package) {
        String prefix = ruleFactory.getGenerationConfig().getClassNamePrefix();
        String suffix = ruleFactory.getGenerationConfig().getClassNameSuffix();
        String fieldName = ruleFactory.getNameHelper().getFieldName(nodeName, node);
        String capitalizedFieldName = capitalize(fieldName);
        String fullFieldName = createFullFieldName(capitalizedFieldName, prefix, suffix);

        String className = ruleFactory.getNameHelper().replaceIllegalCharacters(fullFieldName);
        String normalizedName = ruleFactory.getNameHelper().normalizeName(className);
        return makeUnique(normalizedName, _package);
    }

    private String createFullFieldName(String nodeName, String prefix, String suffix) {
        String returnString = nodeName;
        if (prefix != null) {
            returnString = prefix + returnString;
        }

        if (suffix != null) {
            returnString = returnString + suffix;
        }

        return returnString;
    }

    private String makeUnique(String className, JPackage _package) {
        try {
            JDefinedClass _class = _package._class(className);
            _package.remove(_class);
            return className;
        } catch (JClassAlreadyExistsException e) {
            return makeUnique(className + "_", _package);
        }
    }
}
