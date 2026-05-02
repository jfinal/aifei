/*
 * Copyright 2011-2035 詹波 (aifei.cn)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cn.aifei.db.generator;

import cn.aifei.db.dialect.Dialect;
import cn.aifei.enjoy.Engine;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * ModelSetGenerator 覆盖生成 model set
 */
public class ModelSetGenerator {

    String modelSetPackage;
    String modelSetPath;
    String modelSetName;

    Engine engine = new Engine().setToClassPathSourceFactory();
    String template = "/cn/aifei/db/generator/_model_set_template.af";

    /**
     * 配置生成 model set 的 enjoy 模板文件
     */
    public ModelSetGenerator setModelSetTemplate(String modelSetTemplate) {
        this.template = modelSetTemplate;
        return this;
    }

    /**
     * 获取 engine 进行个性化配置
     */
    public Engine getEngine() {
        return engine;
    }

    public ModelSetGenerator init(String modelSetPackage, String modelSetPath, String modelSetName) {
        Objects.requireNonNull(modelSetPackage, "modelSetPackage can not be null.");
        Objects.requireNonNull(modelSetPath, "modelSetPath can not be null.");
        Objects.requireNonNull(modelSetName, "modelSetName can not be null.");
        this.modelSetPackage = modelSetPackage;
        this.modelSetPath = modelSetPath;
        this.modelSetName = modelSetName;

        engine.setStaticFieldExpression(true).setStaticMethodExpression(true);
        return this;
    }

    public void generate(Dialect dialect, List<TableInfo> tableInfoList, String modelPackage) throws IOException {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("modelSetPackage", modelSetPackage);
        data.put("modelSetName", modelSetName);
        data.put("tableInfoList", tableInfoList);
        data.put("modelPackage", modelPackage);
        String content = engine.getTemplate(template).renderToString(data);
        writeToFile(modelSetName, content);
    }

    /**
     * model set 覆盖写入
     */
    private void writeToFile(String modelSetName, String content) throws IOException {
        String target = modelSetPath + File.separator + modelSetName + ".java";
        Files.createDirectories(Paths.get(modelSetPath));           // 目录不存在则创建
        try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(target), StandardCharsets.UTF_8)) {
            writer.write(content);
        }
    }
}



