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

package cn.aifei.log.log4j;

import cn.aifei.log.Log;
import cn.aifei.log.LogFactory;
import cn.aifei.log.LogKit;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class Log4jLogTest {

    private LogFactory originalFactory;

    @Before
    public void setUp() {
        originalFactory = LogKit.get().getLogFactory();
        LogKit.get().setLogFactory(new Log4jLogFactory());
    }

    @After
    public void tearDown() {
        LogKit.get().setLogFactory(originalFactory);
    }

    @Test
    public void logGetUsesCallerClass() {
        assertEquals(getClass().getName(), Log.get().getName());
    }

    @Test
    public void constructorUsesCallerClass() {
        assertEquals(getClass().getName(), new Log4jLog().getName());
    }

    @Test
    public void factoryUsesCallerClass() {
        assertEquals(getClass().getName(), new Log4jLogFactory().getLog().getName());
    }

    @Test
    public void reflectiveConstructorUsesCallerClass() throws Exception {
        Log log = Log4jLog.class.getDeclaredConstructor().newInstance();
        assertEquals(getClass().getName(), log.getName());
    }

    @Test
    public void reflectiveFactoryUsesCallerClass() throws Exception {
        Log log = (Log) Log4jLogFactory.class.getMethod("getLog").invoke(new Log4jLogFactory());
        assertEquals(getClass().getName(), log.getName());
    }

    @Test
    public void logGetSkipsCustomFactory() {
        LogKit.get().setLogFactory(new Log4jLogFactory() {
            @Override
            public Log getLog() {
                return createLog();
            }

            private Log createLog() {
                return new Log4jLog();
            }
        });
        assertEquals(getClass().getName(), Log.get().getName());
    }

    @Test
    public void nestedCallerKeepsCanonicalName() {
        assertEquals(NestedCaller.class.getCanonicalName(), NestedCaller.LOG.getName());
    }

    private static class NestedCaller {
        private static final Log LOG = Log.get();
    }
}
