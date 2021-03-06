package org.basex.query.func.fn;

import org.basex.core.locks.*;
import org.basex.query.*;
import org.basex.query.expr.*;
import org.basex.query.func.*;
import org.basex.query.util.*;
import org.basex.query.util.list.*;
import org.basex.query.value.type.*;
import org.basex.query.var.*;

/**
 * Context-based function.
 *
 * @author BaseX Team 2005-19, BSD License
 * @author Christian Gruen
 */
public abstract class ContextFn extends StandardFunc {
  /**
   * Argument that provides the context.
   * @return context argument.
   */
  int contextArg() {
    return 0;
  }

  /**
   * Indicates if the function access the current context.
   * @return result of check
   */
  final boolean contextAccess() {
    return exprs.length == contextArg();
  }

  @Override
  public final boolean has(final Flag... flags) {
    return Flag.CTX.in(flags) && contextAccess() || super.has(flags);
  }

  @Override
  public final boolean accept(final ASTVisitor visitor) {
    return (!contextAccess() || visitor.lock(Locking.CONTEXT, false)) && super.accept(visitor);
  }

  /**
   * Optimizes a context-based function call.
   * @param cc compilation context
   */
  protected void optContext(final CompileContext cc) {
    final boolean context = contextAccess();
    final Expr expr = context ? cc.qc.focus.value : exprs[contextArg()];
    if(expr != null) {
      final SeqType st = expr.seqType();
      if(st.oneOrMore() && !st.mayBeArray()) exprType.assign(Occ.ONE);
    }
  }

  @Override
  public final Expr inline(final Var var, final Expr ex, final CompileContext cc)
      throws QueryException {
    return inline(var, ex, cc, v -> contextAccess() ? cc.function(definition.function, info,
        new ExprList(exprs.length + 1).add(exprs).add(ex).finish()) : null);
  }
}
