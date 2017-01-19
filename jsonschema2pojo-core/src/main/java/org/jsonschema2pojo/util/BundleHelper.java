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

import com.sun.codemodel.*;

import java.util.HashMap;

public class BundleHelper {
    private static HashMap<String, String> mapper = new HashMap<String, String>();

    static {
        mapper.put("String", "String");
        mapper.put("Double", "Double");
        mapper.put("Integer", "Int");
        mapper.put("Boolean", "Boolean");
    }

    private JExpression makeImpression(final String expression) {
        return new JExpressionImpl() {
            public void generate(JFormatter f) {
                f.p(expression);
            }
        };
    }

    public void addCreateFromBundle(JDefinedClass jclass) {
        JMethod createFromBundle = jclass.method(JMod.PUBLIC | JMod.STATIC, jclass, "fromBundle");
        JVar in = createFromBundle.param(android.os.Bundle.class, "bundle");

        JInvocation invoc = JExpr._new(jclass);

        for (JFieldVar f : jclass.fields().values()) {
            if ((f.mods().getValue() & JMod.STATIC) == JMod.STATIC) {
                continue;
            }

            String key = f.type().name();

            if (mapper.containsKey(key)) {
                invoc.arg(JExpr.invoke(in, "get" + mapper.get(key)).arg(f.name()));
            } else {
                final JExpression expr = makeImpression(key);

                JInvocation ivGetBundle = JExpr.invoke(in, "getBundle").arg(f.name());
                JInvocation ivFromBundle = JExpr.invoke(expr, "fromBundle");

                invoc.arg(ivFromBundle.arg(ivGetBundle));
            }
        }

        createFromBundle.body()._return(invoc);
    }

    public void addWriteToBundle(JDefinedClass jclass) {
        JMethod method = jclass.method(JMod.PUBLIC, android.os.Bundle.class, "toBundle");

        JClass bundleType = jclass.owner().ref(android.os.Bundle.class);
        JVar instance = method.body().decl(bundleType, "result", JExpr._new(bundleType));

        for (JFieldVar f : jclass.fields().values()) {
            if ((f.mods().getValue() & JMod.STATIC) == JMod.STATIC) {
                continue;
            }

            final JExpression expr = makeImpression("\"" + f.name() + "\"");

            String key = f.type().name();

            if (mapper.containsKey(key)) {
                method.body().invoke(instance, "put" + mapper.get(key)).arg(expr).arg(f);
            } else {
                method.body().invoke(instance, "putBundle").arg(expr).arg(f.invoke("toBundle"));
            }
        }

        method.body()._return(instance);
    }
}
