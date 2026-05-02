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

import cn.aifei.db.core.AifeiDao;
import cn.aifei.db.core.AifeiRow;
import cn.aifei.db.core.Page;
import cn.aifei.db.core.SqlPara;
import java.util.List;

/**
 * NullHooks
 */
public class NullHooks {

    public static final InsertHook INSERT_HOOK = new InsertHook() {
        @Override
        public Object beforeRowInsert(AifeiDao<?, ?> dao, AifeiRow<?> row) {
            return null;
        }

        @Override
        public void afterRowInsert(AifeiDao<?, ?> dao, AifeiRow<?> row, Object fromBeforeRowInsert) {
        }
    };

    public static final DeleteHook DELETE_HOOK = new DeleteHook() {
        @Override
        public Object beforeSqlDelete(AifeiDao<?, ?> dao) {
            return null;
        }

        @Override
        public void afterSqlDelete(AifeiDao<?, ?> dao, int ret, Object fromBeforeSqlDelete) {
        }

        @Override
        public Object beforeRowDelete(AifeiDao<?, ?> dao, AifeiRow<?> row) {
            return null;
        }

        @Override
        public void afterRowDelete(AifeiDao<?, ?> dao, AifeiRow<?> row, Object fromBeforeRowDelete) {
        }
    };

    public static final UpdateHook UPDATE_HOOK = new UpdateHook() {
        @Override
        public Object beforeSqlUpdate(AifeiDao<?, ?> dao) {
            return null;
        }

        @Override
        public void afterSqlUpdate(AifeiDao<?, ?> dao, int ret, Object fromBeforeSqlUpdate) {
        }

        @Override
        public Object beforeRowUpdate(AifeiDao<?, ?> dao, AifeiRow<?> row) {
            return null;
        }

        @Override
        public void afterRowUpdate(AifeiDao<?, ?> dao, AifeiRow<?> row, Object fromBeforeRowUpdate) {
        }
    };

    public static final FindHook FIND_HOOK = new FindHook() {
        @Override
        public Object beforeFind(AifeiDao<?, ?> dao) {
            return null;
        }

        @Override
        public <T extends AifeiRow<T>> void afterFind(AifeiDao<?, T> dao, List<T> rowList, Object fromBeforeFind) {
        }
    };

    public static final QueryHook QUERY_HOOK = new QueryHook() {
        @Override
        public Object beforeQuery(AifeiDao<?, ?> dao) {
            return null;
        }

        @Override
        public void afterQuery(AifeiDao<?, ?> dao, List<?> ret, Object fromBeforeQuery) {
        }
    };

    public static final PaginateHook PAGINATE_HOOK = new PaginateHook() {
        @Override
        public Object beforeQueryTotalRows(AifeiDao<?, ?> dao, Boolean hasGroupBy, SqlPara queryTotalRows) {
            return null;
        }

        @Override
        public void afterQueryTotalRows(AifeiDao<?, ?> dao, long ret, Object fromBeforeQueryTotalRows) {
        }

        @Override
        public Object beforePaginate(AifeiDao<?, ?> dao, int pageNum, int pageSize, long totalRows, SqlPara paginate) {
            return null;
        }

        @Override
        public <T extends AifeiRow<T>> void afterPaginate(AifeiDao<?, T> dao, Page<T> page, Object fromBeforePaginate) {
        }
    };
}


