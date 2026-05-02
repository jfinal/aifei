package cn.aifei.enjoy.stat.ast;

import cn.aifei.enjoy.Env;
import cn.aifei.enjoy.expr.ast.Expr;
import cn.aifei.enjoy.expr.ast.ExprList;
import cn.aifei.enjoy.expr.ast.Logic;
import cn.aifei.enjoy.io.Writer;
import cn.aifei.enjoy.stat.Location;
import cn.aifei.enjoy.stat.ParseException;
import cn.aifei.enjoy.stat.Scope;

/**
 * #returnIf(expr) 指令，当 expr 为 true 时返回，等价于：
 *     #if (expr)
 *         #return
 *     #end
 */
public class ReturnIf extends Stat {

    final Expr expr;

    public ReturnIf(ExprList exprList, Location location) {
        if (exprList.length() == 0) {
            throw new ParseException("The parameter of #returnIf directive can not be blank", location);
        }
        this.expr = exprList.getActualExpr();
    }

    @Override
    public void exec(Env env, Scope scope, Writer writer) {
        if (Logic.isTrue(expr.eval(scope))) {
            scope.getCtrl().setReturn();
        }
    }
}
