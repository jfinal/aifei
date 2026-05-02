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
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * BaseModelGenerator 覆盖生成 base model
 */
public class BaseModelGenerator {

    String baseModelPackage;
    String baseModelPath;

    boolean generateShortSetter = true;
    boolean chainableSetter = false;

    Engine engine = new Engine().setToClassPathSourceFactory();
    String template = "/cn/aifei/db/generator/_base_model_template.af";

    /**
     * 配置是否生成 "short setter" 方法提升开发体验，默认值为 true。
     * <p>
     * 例如：user.name("james").age(28)
     */
    public BaseModelGenerator setGenerateShortSetter(boolean enable) {
        this.generateShortSetter = enable;
        return this;
    }

    /**
     * 配置 setter 方法是否支持链式调用，默认值为 false。
     * 注意：json 框架有可能无法识别链式 setter 方法，从而无法将 json 转换为 model
     * <p>
     * 例如：user.setName("james").setAge(28)
     */
    public BaseModelGenerator setChainableSetter(boolean enable) {
        this.chainableSetter = enable;
        return this;
    }

    /**
     * 配置生成 base model 的 enjoy 模板文件
     */
    public BaseModelGenerator setBaseModelTemplate(String baseModelTemplate) {
        this.template = baseModelTemplate;
        return this;
    }

    /**
     * 获取 engine 进行个性化配置
     */
    public Engine getEngine() {
        return engine;
    }

    // Generator 调用该方法初始化生成器
    public BaseModelGenerator init(String baseModelPackage, String baseModelPath) {
        Objects.requireNonNull(baseModelPackage, "baseModelPackage can not be null.");
        Objects.requireNonNull(baseModelPath, "baseModelPath can not be null.");
        this.baseModelPackage = baseModelPackage;
        this.baseModelPath = baseModelPath;

        // 初始化 engine
        engine.setStaticFieldExpression(true)
                .setStaticMethodExpression(true)
                .addSharedMethod(BaseModelGeneratorUtil.class);
        return this;
    }

    public void generate(Dialect dialect, List<TableInfo> tableInfoList, String modelPackage, String daoPackage) throws IOException {
        for (TableInfo tableInfo : tableInfoList) {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("tableInfo", tableInfo);
            data.put("baseModelPackage", baseModelPackage);
            data.put("generateShortSetter", generateShortSetter);
            data.put("chainableSetter", chainableSetter);
            data.put("modelPackage", modelPackage);
            data.put("daoPackage", daoPackage);
            String content = engine.getTemplate(this.template).renderToString(data);
            writeToFile(tableInfo, content);
        }
    }

    /**
     * base model 覆盖写入
     */
    private void writeToFile(TableInfo tableInfo, String content) throws IOException {
        String target = baseModelPath + File.separator + tableInfo.baseModelName + ".java";
        Files.createDirectories(Paths.get(baseModelPath));          // 目录不存在则创建
        try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(target), StandardCharsets.UTF_8)) {
            writer.write(content);
        }
    }
}


