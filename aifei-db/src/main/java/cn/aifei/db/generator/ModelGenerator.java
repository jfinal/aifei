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
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * ModelGenerator 生成 model。如果 Model 已经存在不进行覆盖，因为 model 中可按需添加代码
 */
public class ModelGenerator {

    String modelPackage;
    String modelPath;

    Engine engine = new Engine().setToClassPathSourceFactory();
    String template = "/cn/aifei/db/generator/_model_template.af";

    /**
     * 配置生成 model 的 enjoy 模板文件
     */
    public ModelGenerator setModelTemplate(String modelTemplate) {
        this.template = modelTemplate;
        return this;
    }

    /**
     * 获取 engine 进行个性化配置
     */
    public Engine getEngine() {
        return engine;
    }

    public ModelGenerator init(String modelPackage, String modelPath) {
        Objects.requireNonNull(modelPackage, "modelPackage can not be null.");
        Objects.requireNonNull(modelPath, "modelPath can not be null.");
        this.modelPackage = modelPackage;
        this.modelPath = modelPath;

        engine.setStaticFieldExpression(true).setStaticMethodExpression(true);
        return this;
    }

    public void generate(Dialect dialect, List<TableInfo> tableInfoList, String baseModelPackage) throws IOException {
        for (TableInfo tableInfo : tableInfoList) {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("tableInfo", tableInfo);
            data.put("modelPackage", modelPackage);
            data.put("baseModelPackage", baseModelPackage);
            String content = engine.getTemplate(template).renderToString(data);
            writeToFile(tableInfo, content);
        }
    }

    /**
     * model 存在时则跳过
     */
    private void writeToFile(TableInfo tableInfo, String content) throws IOException {
        String target = modelPath + File.separator + tableInfo.modelName + ".java";
        Path targetFile = Paths.get(target);
        if (Files.notExists(targetFile)) {                  // 文件存在则跳过
            Files.createDirectories(Paths.get(modelPath));  // 目录不存在则创建
            try (BufferedWriter writer = Files.newBufferedWriter(targetFile, StandardCharsets.UTF_8)) {
                writer.write(content);
            }
        }
    }
}

