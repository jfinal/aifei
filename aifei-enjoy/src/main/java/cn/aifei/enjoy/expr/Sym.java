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

package cn.aifei.enjoy.expr;

/**
 * Sym
 */
public enum Sym {

    ASSIGN("="),

    DOT("."), RANGE(".."), COLON(":"), STATIC("::"), COMMA(","), SEMICOLON(";"),
    LPAREN("("), RPAREN(")"), LBRACK("["), RBRACK("]"), LBRACE("{"), RBRACE("}"),

    ADD("+"), SUB("-"), INC("++"), DEC("--"),
    MUL("*"), DIV("/"), MOD("%"),

    EQUAL("=="), NOTEQUAL("!="), LT("<"), LE("<="), GT(">"), GE(">="),

    NOT("!"), AND("&&"), OR("||"),

    QUESTION("?"),
    NULL_SAFE("??"),

    OPTIONAL_CHAIN("?."),

    ID("ID"),

    STR("STR"), TRUE("TRUE"), FALSE("FALSE"), NULL("NULL"),
    INT("INT"), LONG("LONG"), FLOAT("FLOAT"), DOUBLE("DOUBLE"),

    EOF("EOF");

    private final String value;

    private Sym(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

    public String toString() {
        return value;
    }
}





