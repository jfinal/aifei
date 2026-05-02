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

package cn.aifei.enjoy.stat.ast;

import java.io.IOException;
import cn.aifei.enjoy.Env;
import cn.aifei.enjoy.TemplateException;
import cn.aifei.enjoy.expr.ast.ExprList;
import cn.aifei.enjoy.io.Writer;
import cn.aifei.enjoy.stat.Location;
import cn.aifei.enjoy.stat.Scope;

/**
 * Stat
 */
public abstract class Stat {

    protected Location location;

    public Stat setLocation(Location location) {
        this.location = location;
        return this;
    }

    public Location getLocation() {
        return location;
    }

    public void setExprList(ExprList exprList) {
    }

    public void setStat(Stat stat) {
    }

    public abstract void exec(Env env, Scope scope, Writer writer);

    public boolean hasEnd() {
        return false;
    }

    protected void write(Writer writer, String str) {
        try {
            writer.write(str, 0, str.length());
        } catch (IOException e) {
            throw new TemplateException(e.getMessage(), location, e);
        }
    }
}


