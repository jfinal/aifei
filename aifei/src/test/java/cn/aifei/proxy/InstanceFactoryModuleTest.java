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

package cn.aifei.proxy;

import cn.aifei.util.ComputeCache;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;
import static org.junit.Assume.*;

public class InstanceFactoryModuleTest {

    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void respectsModuleExportsWithAndWithoutReadEdges() throws Exception {
        assumeFalse("JDK 8 has no JPMS", "1.8".equals(System.getProperty("java.specification.version")));
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        assumeNotNull(compiler);
        Path sources = temporaryFolder.newFolder("sources").toPath();
        Path modules = temporaryFolder.newFolder("modules").toPath();
        for (String resource : Arrays.asList("review.factory/module-info.java",
                "review.factory/cn/aifei/proxy/ModuleProbe.java", "review.services/module-info.java",
                "review.services/exported/Service.java", "review.services/exported/ThrowingService.java",
                "review.services/qualified/Service.java", "review.services/closed/Service.java")) {
            copyResource("/modules/" + resource, sources.resolve(resource));
        }
        Path factoryModule = modules.resolve("review.factory");
        for (Class<?> type : Arrays.asList(InstanceFactory.class, ComputeCache.class)) {
            String resource = type.getName().replace('.', '/') + ".class";
            copyResource("/" + resource, factoryModule.resolve(resource));
        }

        ByteArrayOutputStream diagnostics = new ByteArrayOutputStream();
        int result = compiler.run(null, diagnostics, diagnostics,
                "--module-source-path", sources.toString(), "--patch-module", "review.factory=" + factoryModule,
                "-d", modules.toString(), "-m", "review.factory,review.services");
        assertEquals(diagnostics.toString("UTF-8"), 0, result);

        for (boolean readable : new boolean[] {false, true}) {
            runModuleProbe(modules, readable);
        }
    }

    private void runModuleProbe(Path modules, boolean readable) throws Exception {
        String executable = System.getProperty("os.name").startsWith("Windows") ? "java.exe" : "java";
        String java = new File(System.getProperty("java.home"), "bin/" + executable).getAbsolutePath();
        List<String> command = new ArrayList<>(Arrays.asList(java, "--module-path", modules.toString(),
                "--add-modules", "review.services"));
        if (readable) {
            command.addAll(Arrays.asList("--add-reads", "review.factory=review.services"));
        }
        command.addAll(Arrays.asList("-m", "review.factory/cn.aifei.proxy.ModuleProbe", String.valueOf(readable)));
        Path outputFile = temporaryFolder.newFile("module-" + readable + ".log").toPath();
        Process process = new ProcessBuilder(command).redirectErrorStream(true)
                .redirectOutput(outputFile.toFile()).start();
        try {
            assertTrue("Module probe timed out", process.waitFor(30, TimeUnit.SECONDS));
            String output = new String(Files.readAllBytes(outputFile), StandardCharsets.UTF_8);
            assertEquals(output, 0, process.exitValue());
            assertTrue(output, output.contains("MODULE_OK reads=" + readable));
        } finally {
            process.destroyForcibly();
        }
    }

    private void copyResource(String name, Path destination) throws Exception {
        Files.createDirectories(destination.getParent());
        try (InputStream input = InstanceFactoryModuleTest.class.getResourceAsStream(name)) {
            assertNotNull("Missing fixture: " + name, input);
            Files.copy(input, destination);
        }
    }
}
