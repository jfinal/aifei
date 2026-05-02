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
import cn.aifei.db.dialect.MysqlDialect;
import cn.aifei.db.dialect.OracleDialect;
import static cn.aifei.enjoy.util.StrUtil.*;
import javax.sql.DataSource;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * 生成器。
 *
 * <pre>
 * 生成细则：
 *  1: 生成会强制覆盖 base model、model set，请勿修改这两类文件，数据库有变化时重新生成一次即可
 *  2: 生成不会覆盖已存在的 model、dao 文件。model 可添加状态有关功能，dao 可按需添加查询与缓存等功能
 *  3: model set 文件默认会生成在 basePath 目录下
 *  4: 可以通过继承 BaseModelGenerator、ModelGenerator、ModelSetGenerator、DaoGenerator
 *     来创建自定义生成器，然后通过 Generator 的 setter 方法指定自定义生成器来生成
 *  5: 同理可通过继承 MetaReader 定制 meta 信息读取
 *
 * 例子：
 *  // 基础包名
 *  String basePackage = "cn.aifei.vip.common.db";
 *
 *  // 基础生成路径
 *  String basePath = System.getProperty("user.dir") + "/src/main/java/" + basePackage.replace('.', '/');
 *
 *  // 调用生成器
 *  new Generator(new MysqlDialect(), dataSource, basePackage, basePath).generate();
 * </pre>
 */
public class Generator {

    // 方言、数据源
    Dialect dialect;
    DataSource dataSource;

    // 基础包名与路径
    String basePackage;
    String basePath;

    // MetaReader 与四个生成器
    MetaReader metaReader = new MetaReader();
    ModelGenerator modelGenerator = new ModelGenerator();
    BaseModelGenerator baseModelGenerator = new BaseModelGenerator();
    DaoGenerator daoGenerator = new DaoGenerator();
    ModelSetGenerator modelSetGenerator = new ModelSetGenerator();

    // model、base model、dao 类名函数
    Function<String, String> modelNameFun = table -> firstCharToUpperCase(toCamelCase(table, dialect instanceof OracleDialect));
    Function<String, String> baseModelNameFun = modelName -> "Base" + modelName;
    Function<String, String> daoNameFun = modelName -> modelName + "Dao";
    Supplier<String> modelSetNameFun = () -> "ModelSet";

    // 基于 basePackage 得到 model、base model、dao、model set 包名
    Function<String, String> modelPackageFun = basePackage -> basePackage + ".model";
    Function<String, String> baseModelPackageFun = basePackage -> basePackage + ".base";
    Function<String, String> daoPackageFun = basePackage -> basePackage + ".dao";
    Function<String, String> modelSetPackageFun = basePackage -> basePackage;

    // 基于 basePath 得到 model、base model、dao、model set 路径
    Function<String, String> modelPathFun = basePath -> basePath + "/model";
    Function<String, String> baseModelPathFun = basePath -> basePath + "/base";
    Function<String, String> daoPathFun = basePath -> basePath + "/dao";
    Function<String, String> modelSetPathFun = basePath -> basePath;

    /**
     * 构造方法，默认使用 MysqlDialect
     */
    public Generator(DataSource dataSource, String basePackage, String basePath) {
        this(new MysqlDialect(), dataSource, basePackage, basePath);
    }

    public Generator(Dialect dialect, DataSource dataSource, String basePackage, String basePath) {
        Objects.requireNonNull(dialect, "dialect can not be null.");
        Objects.requireNonNull(dataSource, "dataSource can not be null.");
        if (isBlank(basePackage)) {
            throw new IllegalArgumentException("basePackage can not be blank.");
        }
        if (basePackage.contains("/") || basePackage.contains("\\")) {
            throw new IllegalArgumentException("basePackage error : " + basePackage);
        }
        if (isBlank(basePath)) {
            throw new IllegalArgumentException("basePath can not be blank.");
        }

        if (basePath.endsWith("/") || basePath.endsWith("\\")) {
            basePath = basePath.substring(0, basePath.length() - 1);
        }
        this.dialect = dialect;
        this.dataSource = dataSource;
        this.basePackage = basePackage;
        this.basePath = basePath;
    }

    /**
     * 配置 MetaReader
     */
    public Generator configMetaReader(Consumer<MetaReader> config) {
        config.accept(this.metaReader);
        return this;
    }

    /**
     * 配置 ModelGenerator
     */
    public Generator configModelGenerator(Consumer<ModelGenerator> config) {
        config.accept(this.modelGenerator);
        return this;
    }

    /**
     * 配置 BaseModelGenerator
     */
    public Generator configBaseModelGenerator(Consumer<BaseModelGenerator> config) {
        config.accept(this.baseModelGenerator);
        return this;
    }

    /**
     * 配置 DaoGenerator
     */
    public Generator configDaoGenerator(Consumer<DaoGenerator> config) {
        config.accept(this.daoGenerator);
        return this;
    }

    /**
     * 配置 ModelSetGenerator
     */
    public Generator configModelSetGenerator(Consumer<ModelSetGenerator> config) {
        config.accept(this.modelSetGenerator);
        return this;
    }

    /**
     * 注入自定义 MetaReader
     */
    public Generator setMetaReader(MetaReader metaReader) {
        this.metaReader = metaReader;
        return this;
    }

    /**
     * 注入自定义 ModelGenerator
     */
    public Generator setModelGenerator(ModelGenerator modelGenerator) {
        this.modelGenerator = modelGenerator;
        return this;
    }

    /**
     * 注入自定义 BaseModelGenerator
     */
    public Generator setBaseModelGenerator(BaseModelGenerator baseModelGenerator) {
        this.baseModelGenerator = baseModelGenerator;
        return this;
    }

    /**
     * 注入自定义 DaoGenerator
     */
    public Generator setDaoGenerator(DaoGenerator daoGenerator) {
        this.daoGenerator = daoGenerator;
        return this;
    }

    /**
     * 注入自定义 ModelSetGenerator
     */
    public Generator setModelSetGenerator(ModelSetGenerator modelSetGenerator) {
        this.modelSetGenerator = modelSetGenerator;
        return this;
    }

    /**
     * 配置 model name 生成函数
     */
    public Generator setModelNameFun(Function<String, String> modelNameFun) {
        this.modelNameFun = modelNameFun;
        return this;
    }

    /**
     * 配置 base model name 生成函数
     */
    public Generator setBaseModelNameFun(Function<String, String> baseModelNameFun) {
        this.baseModelNameFun = baseModelNameFun;
        return this;
    }

    /**
     * 配置 dao name 生成函数
     */
    public Generator setDaoNameFun(Function<String, String> daoNameFun) {
        this.daoNameFun = daoNameFun;
        return this;
    }

    /**
     * 配置 model set name 生成函数
     */
    public Generator setModelSetNameFun(Supplier<String> modelSetNameFun) {
        this.modelSetNameFun = modelSetNameFun;
        return this;
    }

    /**
     * 配置 model package 生成函数
     */
    public void setModelPackageFun(Function<String, String> modelPackageFun) {
        this.modelPackageFun = modelPackageFun;
    }

    /**
     * 配置 model path 生成函数
     */
    public void setModelPathFun(Function<String, String> modelPathFun) {
        this.modelPathFun = modelPathFun;
    }

    /**
     * 配置 base model package 生成函数
     */
    public Generator setBaseModelPackageFun(Function<String, String> baseModelPackageFun) {
        this.baseModelPackageFun = baseModelPackageFun;
        return this;
    }

    /**
     * 配置 base model path 生成函数
     */
    public Generator setBaseModelPathFun(Function<String, String> baseModelPathFun) {
        this.baseModelPathFun = baseModelPathFun;
        return this;
    }

    /**
     * 配置 dao package 生成函数
     */
    public Generator setDaoPackageFun(Function<String, String> daoPackageFun) {
        this.daoPackageFun = daoPackageFun;
        return this;
    }

    /**
     * 配置 dao path 生成函数
     */
    public Generator setDaoPathFun(Function<String, String> daoPathFun) {
        this.daoPathFun = daoPathFun;
        return this;
    }

    /**
     * 配置 model set package 生成函数
     */
    public Generator setModelSetPackageFun(Function<String, String> modelSetPackageFun) {
        this.modelSetPackageFun = modelSetPackageFun;
        return this;
    }

    /**
     * 配置 model set path 生成函数
     */
    public Generator setModelSetPathFun(Function<String, String> modelSetPathFun) {
        this.modelSetPathFun = modelSetPathFun;
        return this;
    }

    /**
     * 生成
     */
    public void generate() {
        System.out.println("Aifei-db 开始生成... \nbasePackage: " + basePackage + "\n" + "basePath:    " + basePath);

        // 读取 table info
        List<TableInfo> tableInfoList = metaReader.read(dialect, dataSource);

        // 创建 modelName、baseModelName、daoName、modelSetName
        for (TableInfo tableInfo : tableInfoList) {
            tableInfo.modelName = modelNameFun.apply(tableInfo.name);
            tableInfo.baseModelName = baseModelNameFun.apply(tableInfo.modelName);
            tableInfo.daoName = daoNameFun.apply(tableInfo.modelName);
        }
        String modelSetName = modelSetNameFun.get();

        // 创建 model、base model、dao、model set 的 package
        String modelPackage = modelPackageFun.apply(basePackage);
        String baseModelPackage = baseModelPackageFun.apply(basePackage);
        String daoPackage = daoPackageFun.apply(basePackage);
        String modelSetPackage = modelSetPackageFun.apply(basePackage);

        // 创建 model、base model、dao、model set 的 path
        String modelPath = modelPathFun.apply(basePath);
        String baseModelPath = baseModelPathFun.apply(basePath);
        String daoPath = daoPathFun.apply(basePath);
        String modelSetPath = modelSetPathFun.apply(basePath);

        try {
            // 生成 base model、model、dao、model set 的类文件
            baseModelGenerator.init(baseModelPackage, baseModelPath).generate(dialect, tableInfoList, modelPackage, daoPackage);
            modelGenerator.init(modelPackage, modelPath).generate(dialect, tableInfoList, baseModelPackage);
            daoGenerator.init(daoPackage, daoPath).generate(dialect, tableInfoList, modelPackage);
            modelSetGenerator.init(modelSetPackage, modelSetPath, modelSetName).generate(dialect, tableInfoList, modelPackage);

            System.out.println("Aifei-db 生成完毕");

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}

