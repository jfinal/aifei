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

import cn.aifei.db.core.AifeiDao;
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
 * DaoGenerator 生成 dao。如果 dao 已经存在不进行覆盖，因为 dao 中用户可按需添加代码，例如查询或者缓存这类功能
 */
public class DaoGenerator {

    String daoPackage;
    String daoPath;

    Class<? extends AifeiDao> baseDao = BaseDao.class;

    Engine engine = new Engine().setToClassPathSourceFactory();
    String template = "/cn/aifei/db/generator/_dao_template.af";

    /**
     * 配置 BaseDao，用于定制 BaseDao 实现。默认值 cn.aifei.db.generator.BaseDao.class
     */
    public DaoGenerator setBaseDao(Class<? extends AifeiDao> baseDao) {
        Objects.requireNonNull(baseDao, "baseDao can not be null.");
        this.baseDao = baseDao;
        return this;
    }

    /**
     * 配置生成 dao 的 enjoy 模板文件
     */
    public DaoGenerator setDaoTemplate(String daoTemplate) {
        this.template = daoTemplate;
        return this;
    }

    /**
     * 获取 engine 进行个性化配置
     */
    public Engine getEngine() {
        return engine;
    }

    public DaoGenerator init(String daoPackage, String daoPath) {
        Objects.requireNonNull(daoPackage, "daoPackage can not be null.");
        Objects.requireNonNull(daoPath, "daoPath can not be null.");
        this.daoPackage = daoPackage;
        this.daoPath = daoPath;

        engine.setStaticFieldExpression(true).setStaticMethodExpression(true);
        return this;
    }

    public void generate(Dialect dialect, List<TableInfo> tableInfoList, String modelPackage) throws IOException {
        for (TableInfo tableInfo : tableInfoList) {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("tableInfo", tableInfo);
            data.put("daoPackage", daoPackage);
            data.put("modelPackage", modelPackage);
            data.put("baseDaoFullName", baseDao.getName());
            data.put("baseDaoName", baseDao.getSimpleName());
            String content = engine.getTemplate(template).renderToString(data);
            writeToFile(tableInfo, content);
        }
    }

    /**
     * dao 存在时则跳过
     */
    private void writeToFile(TableInfo tableInfo, String content) throws IOException {
        String target = daoPath + File.separator + tableInfo.daoName + ".java";
        Path targetFile = Paths.get(target);
        if (Files.notExists(targetFile)) {                  // 文件存在则跳过
            Files.createDirectories(Paths.get(daoPath));    // 目录不存在则创建
            try (BufferedWriter writer = Files.newBufferedWriter(targetFile, StandardCharsets.UTF_8)) {
                writer.write(content);
            }
        }
    }
}

