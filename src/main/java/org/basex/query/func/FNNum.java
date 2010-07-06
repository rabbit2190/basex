package org.basex.query.func;

import java.math.BigDecimal;
import org.basex.query.QueryContext;
import org.basex.query.QueryException;
import org.basex.query.expr.Expr;
import org.basex.query.item.Dbl;
import org.basex.query.item.Dec;
import org.basex.query.item.Flt;
import org.basex.query.item.Item;
import org.basex.query.item.Itr;
import org.basex.query.item.Seq;
import org.basex.query.item.Type;
import org.basex.query.util.Err;

/**
 * Numeric functions.
 *
 * @author Workgroup DBIS, University of Konstanz 2005-10, ISC License
 * @author Christian Gruen
 */
final class FNNum extends Fun {
  @Override
  public Item atomic(final QueryContext ctx) throws QueryException {
    final Item it = expr[0].atomic(ctx);
    if(it == null) return null;

    if(!it.u() && !it.n()) Err.num(info(), it);
    final double d = it.dbl();
    switch(func) {
      case ABS:    return abs(it);
      case CEIL:   return num(it, d, Math.ceil(d));
      case FLOOR:  return num(it, d, Math.floor(d));
      case RND:    return rnd(it, d, ctx, false);
      case RNDHLF: return rnd(it, d, ctx, true);
      default:     return super.atomic(ctx);
    }
  }

  @Override
  public Expr c(final QueryContext ctx) throws QueryException {
    if(expr[0].e()) return expr[0];
    for(final Expr a : expr) if(!a.i()) return this;
    final Item it = atomic(ctx);
    return it == null ? Seq.EMPTY : it;
  }

  /**
   * Returns the absolute item.
   * @param it input item
   * @return absolute item
   * @throws QueryException query exception
   */
  private Item abs(final Item it) throws QueryException {
    final double d = it.dbl();
    final boolean s = d > 0d || 1 / d > 0;

    switch(it.type) {
      case DBL: return s ? it : Dbl.get(Math.abs(it.dbl()));
      case FLT: return s ? it : Flt.get(Math.abs(it.flt()));
      case DEC: return s ? it : Dec.get(it.dec().abs());
      case ITR: return s ? it : Itr.get(Math.abs(it.itr()));
      default:  return Itr.get(Math.abs(it.itr()));
    }
  }

  /**
   * Returns a rounded item.
   * @param it input item
   * @param d input double value
   * @param h2e half-to-even flag
   * @param ctx query context
   * @return absolute item
   * @throws QueryException query exception
   */
  private Item rnd(final Item it, final double d, final QueryContext ctx,
      final boolean h2e) throws QueryException {

    final int pp = expr.length == 1 ? 0 : (int) checkItr(expr[1], ctx);
    if(it.type == Type.DEC && pp >= 0) {
      final BigDecimal bd = it.dec();
      final int m = h2e ? BigDecimal.ROUND_HALF_EVEN : bd.signum() > 0 ?
          BigDecimal.ROUND_HALF_UP : BigDecimal.ROUND_HALF_DOWN;
      return Dec.get(bd.setScale(pp, m));
    }

    // calculate precision factor
    double p = 1;
    for(long i = pp; i > 0; i--) p *= 10;
    for(long i = pp; i < 0; i++) p /= 10;

    double c = d;
    if(h2e) {
      c = p == 1 && (c % 2 == .5 || c % 2 == -1.5) ? c - .5 :
        Math.floor(c * p + .5) / p;
    } else if(d == d && d != 0 && d >= Long.MIN_VALUE && d < Long.MAX_VALUE) {
      final double dp = d * p;
      c = (dp >= -.5d && dp < 0 ? -0d : Math.round(dp)) / p;
    }
    return num(it, d, c);
  }

  /**
   * Returns a numeric result with the correct data type.
   * @param it input item
   * @param n input double value
   * @param d calculated double value
   * @return numeric item
   */
  private Item num(final Item it, final double n, final double d) {
    final Item i = it.u() ? Dbl.get(n) : it;
    if(n == d) return i;

    switch(it.type) {
      case DEC: return Dec.get(d);
      case DBL: return Dbl.get(d);
      case FLT: return Flt.get((float) d);
      default:  return Itr.get((long) d);
    }
  }
}
