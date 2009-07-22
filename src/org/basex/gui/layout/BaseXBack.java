package org.basex.gui.layout;

import java.awt.Color;
import java.awt.Graphics;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import org.basex.gui.GUIConstants;
import org.basex.gui.GUIConstants.Fill;

/**
 * Panel background, extending the {@link JPanel}.
 *
 * @author Workgroup DBIS, University of Konstanz 2005-09, ISC License
 * @author Christian Gruen
 */
public class BaseXBack extends JPanel {
  /** Design of color gradient. */
  private Fill mode;

  /**
   * Default Constructor.
   */
  public BaseXBack() {
    this(Fill.PLAIN);
  }

  /**
   * Default Constructor.
   * @param m visualization mode
   */
  public BaseXBack(final Fill m) {
    setMode(m);
  }

  /**
   * Default Constructor.
   * @param m visualization mode
   */
  public final void setMode(final Fill m) {
    mode = m;
    final boolean o = mode != Fill.NONE;
    if(isOpaque() != o) setOpaque(o);
  }

  @Override
  public void paintComponent(final Graphics g) {
    if(mode == Fill.UP || mode == Fill.DOWN) {
      Color c1 = GUIConstants.color1;
      Color c2 = GUIConstants.color2;
      if(mode == Fill.UP) { final Color t = c1; c1 = c2; c2 = t; }
      BaseXLayout.fill(g, c1, c2, 0, 0, getWidth(), getHeight());
    } else {
      super.paintComponent(g);
    }
  }

  /**
   * Set an empty border.
   * @param t top distance
   * @param l left distance
   * @param b bottom distance
   * @param r right distance
   */
  public final void setBorder(final int t, final int l, final int b,
      final int r) {
    setBorder(new EmptyBorder(t, l, b, r));
  }
}
