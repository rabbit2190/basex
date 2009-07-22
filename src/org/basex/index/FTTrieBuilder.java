package org.basex.index;

import static org.basex.data.DataText.*;
import java.io.IOException;
import org.basex.data.Data;
import org.basex.io.DataOutput;
import org.basex.util.Num;

/**
 * This class builds an index for text contents in a compressed trie.
 *
 * @author Workgroup DBIS, University of Konstanz 2005-09, ISC License
 * @author Sebastian Gath
 * @author Christian Gruen
 */
public final class FTTrieBuilder extends FTBuilder {
  /** CTArray for tokens. */
  private final FTArray index = new FTArray(128);
  /** Hash structure for temporarily saving the tokens. */
  private FTHash hash = new FTHash();

  /**
   * Builds the index structure and returns an index instance.
   * @param data data reference
   * @return index instance
   * @throws IOException IO Exception
   */
  public FTIndex build(final Data data) throws IOException {
    index(data);
    return new FTTrie(data);
  }

  @Override
  void index(final byte[] tok) {
    hash.index(tok, id, wp.pos);
  }

  @Override
  void write(final Data data) throws IOException {
    final String db = data.meta.name;
    final DataOutput outb = new DataOutput(db, DATAFTX + 'b');

    hash.init();

    while(hash.more()) {
      final int p = hash.next();
      final byte[] tok = hash.key();
      final int ds = hash.ns[p];
      final long cpre = outb.size();

      // write compressed pre and pos arrays
      final byte[] vpre = hash.pre[p];
      final byte[] vpos = hash.pos[p];
      int lpre = 4;
      int lpos = 4;

      // ftdata is stored here, with pre1, pos1, ..., preu, posu
      final int pres = Num.size(vpre);
      final int poss = Num.size(vpos);
      while(lpre < pres && lpos < poss) {
        for(int z = 0, l = Num.len(vpre, lpre); z < l; z++)
          outb.write(vpre[lpre++]);
        for(int z = 0, l = Num.len(vpos, lpos); z < l; z++)
          outb.write(vpos[lpos++]);
      }
      index.insertSorted(tok, ds, cpre);
    }

    hash = null;

    final byte[][] tokens = index.tokens.list;
    final int[][] next = index.next.list;

    // save each node: l, t1, ..., tl, n1, v1, ..., nu, vu, s, p
    // l = length of the token t1, ..., tl
    // u = number of next nodes n1, ..., nu
    // v1= the first byte of each token n1 points, ...
    // s = size of pre values saved at pointer p
    // [byte, byte[l], byte, int, byte, ..., int, long]
    final DataOutput outN = new DataOutput(db, DATAFTX + 'a');
    // ftdata is stored here, with pre1, ..., preu, pos1, ..., posu
    // each node entries size is stored here
    final DataOutput outS = new DataOutput(db, DATAFTX + 'c');

    // document contains any text nodes -> empty index created;
    // only root node is kept
    int s = 0;
    if(index.count != 1) {
      // index.next[i] : [p, n1, ..., s, d]
      // index.tokens[p], index.next[n1], ..., index.pre[d]

      // first root node
      // write token size as byte
      outN.write((byte) 1);
      // write token
      outN.write((byte) -1);
      // write next pointer
      int j = 1;
      for(; j < next[0].length - 2; j++) {
        outN.writeInt(next[0][j]); // pointer
        // first char of next node
        outN.write(tokens[next[next[0][j]][0]][0]);
      }

      outN.writeInt(next[0][j]); // data size
      outN.write5(-1); // pointer on data - root has no data
      outS.writeInt(s);
      s += 2L + (next[0].length - 3) * 5L + 9L;
      // all other nodes
      final int il = index.next.size;
      for(int i = 1; i < il; i++) {
        // check pointer on data needs 1 or 2 ints
        final int lp = next[i][next[i].length - 1] > -1 ? 0 : -1;
        // write token size as byte
        outN.write((byte) tokens[next[i][0]].length);
        // write token
        outN.write(tokens[next[i][0]]);
        // write next pointer
        j = 1;
        for(; j < next[i].length - 2 + lp; j++) {
          outN.writeInt(next[i][j]); // pointer
          // first char of next node
          outN.write(tokens[next[next[i][j]][0]][0]);
        }
        outN.writeInt(next[i][j]); // data size
        if(next[i][j] == 0 && next[i][j + 1] == 0) {
          // node has no data
          outN.write5(next[i][j + 1]);
        } else {
          // write pointer on data
          if(lp == 0) {
            outN.write5(next[i][j + 1]);
          } else {
            outN.write5(toLong(next[i], next[i].length - 2));
          }
        }
        outS.writeInt(s);
        s += 1L + tokens[next[i][0]].length * 1L + (next[i].length - 3 + lp)
            * 5L + 9L;
      }
    }

    outS.writeInt(s);
    outb.close();
    outN.close();
    outS.close();
  }

  /**
   * Converts long values split with toArray back.
   * @param ar int[] with values
   * @param p pointer where the first value is found
   * @return long l
   */
  private static long toLong(final int[] ar, final int p) {
    long l = (long) ar[p] << 16;
    l += -ar[p + 1] & 0xFFFF;
    return l;
  }
}
