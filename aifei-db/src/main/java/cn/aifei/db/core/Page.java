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

package cn.aifei.db.core;

import java.io.Serializable;
import java.util.List;

/**
 * Page 封装分页查询结果。
 * setter 方法主要用于反序列化，仅还原对应字段，不联动更新其他字段。
 */
public class Page<T> implements Serializable {

    private List<T> rows;       // 语义对齐 totalRows

    private int pageNum;
    private int pageSize;

    private long totalRows;
    private long totalPages;    // 使用 long 类型，总页数可以大过 Integer.MAX_VALUE

    public Page() {
    }

    /**
     * Constructor.
     * @param rows the rows of paginate result
     * @param pageNum the page number
     * @param pageSize the page size
     * @param totalRows the total rows of paginate
     * @param totalPages the total pages of paginate
     */
    public Page(List<T> rows, int pageNum, int pageSize, long totalRows, long totalPages) {
        this.rows = rows;
        this.pageNum = pageNum;
        this.pageSize = pageSize;
        this.totalRows = totalRows;
        this.totalPages = totalPages;
    }

    /**
     * Constructor.
     * @param rows the rows of paginate result
     * @param pageNum the page number
     * @param pageSize the page size
     * @param totalRows the total rows of paginate
     */
    public Page(List<T> rows, int pageNum, int pageSize, long totalRows) {
        // 取模 % 和除法 / 是整数运算，性能优于浮点运算的 Math.ceil
        this(rows, pageNum, pageSize, totalRows, totalRows % pageSize != 0 ? totalRows / pageSize + 1 : totalRows / pageSize);
    }

    /**
     * Return rows of this page.
     */
    public List<T> getRows() {
        return rows;
    }

    public void setRows(List<T> rows) {
        this.rows = rows;
    }

    /**
     * Return page number.
     */
    public int getPageNum() {
        return pageNum;
    }

    public void setPageNum(int pageNum) {
        this.pageNum = pageNum;
    }

    /**
     * Return page size.
     */
    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    /**
     * Return total rows.
     */
    public long getTotalRows() {
        return totalRows;
    }

    public void setTotalRows(long totalRows) {
        this.totalRows = totalRows;
    }

    /**
     * Return total pages.
     */
    public long getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(long totalPages) {
        this.totalPages = totalPages;
    }

    public boolean firstPage() {
        return pageNum == 1;
    }

    public boolean lastPage() {
        return pageNum >= totalPages;
    }

    @Override
    public String toString() {
        return "{ pageNum = " + pageNum + ", pageSize = " + pageSize + ", totalRows = " + totalRows + ", totalPages = " + totalPages + " }";
    }
}
