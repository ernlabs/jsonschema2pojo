/**
 * Copyright © 2010-2014 Nokia
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

package org.jsonschema2pojo.example;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import org.jsonschema2pojo.DefaultGenerationConfig;
import org.jsonschema2pojo.GenerationConfig;
import org.jsonschema2pojo.Jackson2Annotator;
import org.jsonschema2pojo.SchemaGenerator;
import org.jsonschema2pojo.SchemaMapper;
import org.jsonschema2pojo.SchemaStore;
import org.jsonschema2pojo.rules.RuleFactory;

import com.sun.codemodel.JCodeModel;

import static java.lang.System.exit;

public class GenerateModels {

    public static void main(String[] args) throws IOException {

        // BEGIN EXAMPLE

        if(args.length < 4) {
            System.out.println("usage: GenerateModels jsonFile pathFile packagePrefix className");
            exit(-99);
        }

        JCodeModel codeModel = new JCodeModel();

        URL source = new URL(args[0]);

        GenerationConfig config = new DefaultGenerationConfig() {
            @Override
            public boolean isGenerateBuilders() { // set config option by overriding method
                return true;
            }

            @Override
            public boolean isConstructorsRequiredPropertiesOnly() {
                return true;
            }

            @Override
            public boolean isIncludeAccessors() {
                return true;
            }

            @Override
            public boolean isIncludeAdditionalProperties() { return false; }

            @Override
            public boolean isIncludeConstructors() { return true; }

            @Override
            public boolean isParcelable() { return false; }

            @Override
            public boolean isWriteableMap() { // set config option by overriding method
                return true;
            }

            @Override
            public boolean isIncludeHashcodeAndEquals() {
                return true;
            }

            @Override
            public boolean isIncludeToString() {
                return false;
            }

            @Override
            public boolean isIncludeJsr303Annotations() {
                return true;
            }

            @Override
            public boolean isIncludeJsr305Annotations() {
                return false;
            }

        };

        SchemaMapper mapper = new SchemaMapper(new RuleFactory(config, new Jackson2Annotator(config), new SchemaStore()), new SchemaGenerator());
        mapper.generate(codeModel, args[3], args[2], source);

        String pathName = args[1];
        codeModel.build(new File(pathName));

//        pathName = "/Users/r0m00kk/Documents/dev/testxform/src";
//        codeModel.build(new File(pathName));

        // END EXAMPLE

    }
}
