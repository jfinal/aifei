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

package cn.aifei.router;

import cn.aifei.argument.Argument;
import cn.aifei.core.Input;
import cn.aifei.core.Output;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * ActionGroup
 */
public class ActionGroup {

    Action[] actionArray;
    final List<Action> actionList = new ArrayList<>();

    /**
     * 添加 action 到 Action Group
     */
    public void add(Action action) {
        if (!actionList.isEmpty()) {
            checkAmbiguousMapping(action);
        }

        actionList.add(action);
        // 按参与路由匹配的参数数量降序排列，尽可能匹配更多参数，提高匹配精度
        actionList.sort((a, b) -> b.getMatchParaCount() - a.getMatchParaCount());
        actionArray = actionList.toArray(new Action[0]);
    }

    /**
     * 获取 Action 数组
     */
    public Action[] getActionArray() {
        return actionArray;
    }

    // 检测歧义映射
    private void checkAmbiguousMapping(Action action) {
        int methodParaCount = action.getMethodParaCount();
        int pathParaCount = action.getPathParaCount();
        Set<String> paraNameSet = getParaNameSet(action.getArguments());

        for (Action act : actionList) {
            // 若命名参数数量、命名参数名集合、路径参数数量三者都相等时，则判定为歧义映射
            if (methodParaCount == act.getMethodParaCount() && pathParaCount == act.getPathParaCount()) {
                Set<String> set = getParaNameSet(act.getArguments());
                if (paraNameSet.equals(set)) {
                    String msg = "Ambiguous mapping: " + action.getBriefInfo() + ";\nConflicts with: " + act.getBriefInfo();
                    throw new IllegalStateException(msg);
                }
            }
        }
    }

    /*
     * 获取参与路由匹配的 "非路径参数" 的参数名集合。
     * 注意："路径参数" 在路由匹配中参与位置的匹配，而不参与名称的匹配，故需排除其名称
     */
    private Set<String> getParaNameSet(Argument<?, ?, ?>[] arguments) {
        return Arrays.stream(arguments)
                .filter(a -> a.isMatch() && !a.isPathPara())
                .map(Argument::getName)
                .collect(Collectors.toSet());
    }
}


