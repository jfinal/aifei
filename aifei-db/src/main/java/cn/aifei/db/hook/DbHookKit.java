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

package cn.aifei.db.hook;

import java.util.Objects;

/**
 * DbHookKit 钩子管理工具箱
 */
public class DbHookKit {

    // 以下 hook 可配置为 AifeiDb 生效，不同的 AifeiDb(数据源) 对象可配置不同的 hook 实现
    private InsertHook insertHook = NullHooks.INSERT_HOOK;
    private DeleteHook deleteHook = NullHooks.DELETE_HOOK;
    private UpdateHook updateHook = NullHooks.UPDATE_HOOK;
    private FindHook findHook = NullHooks.FIND_HOOK;
    private QueryHook queryHook = NullHooks.QUERY_HOOK;
    private PaginateHook paginateHook = NullHooks.PAGINATE_HOOK;

    public InsertHook getInsertHook() {
        return insertHook;
    }

    public void setInsertHook(InsertHook insertHook) {
        Objects.requireNonNull(insertHook, "insertHook can not be null.");
        this.insertHook = insertHook;
    }

    public DeleteHook getDeleteHook() {
        return deleteHook;
    }

    public void setDeleteHook(DeleteHook deleteHook) {
        Objects.requireNonNull(deleteHook, "deleteHook can not be null.");
        this.deleteHook = deleteHook;
    }

    public UpdateHook getUpdateHook() {
        return updateHook;
    }

    public void setUpdateHook(UpdateHook updateHook) {
        Objects.requireNonNull(updateHook, "updateHook can not be null.");
        this.updateHook = updateHook;
    }

    public FindHook getFindHook() {
        return findHook;
    }

    public void setFindHook(FindHook findHook) {
        Objects.requireNonNull(findHook, "findHook can not be null.");
        this.findHook = findHook;
    }

    public QueryHook getQueryHook() {
        return queryHook;
    }

    public void setQueryHook(QueryHook queryHook) {
        Objects.requireNonNull(queryHook, "queryHook can not be null.");
        this.queryHook = queryHook;
    }

    public PaginateHook getPaginateHook() {
        return paginateHook;
    }

    public void setPaginateHook(PaginateHook paginateHook) {
        Objects.requireNonNull(paginateHook, "paginateHook can not be null.");
        this.paginateHook = paginateHook;
    }
}




