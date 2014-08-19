package org.basex.query.func.fn;

import org.basex.query.*;
import org.basex.query.value.item.*;
import org.basex.util.*;

/**
 * Function implementation.
 *
 * @author BaseX Team 2005-14, BSD License
 * @author Christian Gruen
 */
public final class FnRound extends Num {
  @Override
  public Item item(final QueryContext qc, final InputInfo ii) throws QueryException {
    return round(qc, false);
  }

  @Override
  public boolean has(final Flag flag) {
    return flag == Flag.X30 && exprs.length == 2 || super.has(flag);
  }
}