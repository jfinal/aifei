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
 * Page 封装分页查询结果
 */
public class Page<T> implements Serializable {

    private int pageNum;
    private int pageSize;

    private long totalRows;
    private int totalPages;

    private List<T> rows;   // 语义对齐 totalRows

    public Page() {
    }

    /**
     * Constructor.
     * @param pageNum the page number
     * @param pageSize the page size
     * @param totalRows the total rows of paginate
     * @param totalPages the total pages of paginate
     * @param rows the rows of paginate result
     */
    public Page(int pageNum, int pageSize, long totalRows, int totalPages, List<T> rows) {
        this.pageNum = pageNum;
        this.pageSize = pageSize;
        this.totalRows = totalRows;
        this.totalPages = totalPages;
        this.rows = rows;
    }

    /**
     * Constructor.
     * @param pageNum the page number
     * @param pageSize the page size
     * @param totalRows the total rows of paginate
     * @param rows the rows of paginate result
     */
    public Page(int pageNum, int pageSize, long totalRows, List<T> rows) {
        // 取模 % 和除法 / 是整数运算，性能优于浮点运算的 Math.ceil
        this(pageNum, pageSize, totalRows, (int) (totalRows % pageSize != 0 ? totalRows / pageSize + 1 : totalRows / pageSize), rows);
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
    public int getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }

    public boolean isFirstPage() {
        return pageNum == 1;
    }

    public boolean isLastPage() {
        return pageNum >= totalPages;
    }

    public boolean hasPreviousPage() {
        return pageNum > 1;
    }

    public boolean hasNextPage() {
        return pageNum < totalPages;
    }

    @Override
    public String toString() {
        return "{ pageNum = " + pageNum + ", pageSize = " + pageSize + ", totalRows = " + totalRows + ", totalPages = " + totalPages + " }";
    }
}
