/*
 * Bibliothek - DockingFrames
 * Library built on Java/Swing, allows the user to "drag and drop"
 * panels containing any Swing-Component the developer likes to add.
 * 
 * Copyright (C) 2007 Benjamin Sigg
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 * 
 * Benjamin Sigg
 * benjamin_sigg@gmx.ch
 * CH - Switzerland
 */
package bibliothek.gui.dock.common.customized;

import java.awt.Color;
import java.awt.Graphics;

import javax.swing.UIManager;

import bibliothek.extension.gui.dock.theme.eclipse.stack.tab.TabPanePainter;
import bibliothek.gui.DockController;
import bibliothek.gui.dock.util.color.AbstractDockColor;
import bibliothek.gui.dock.util.color.ColorCodes;
import bibliothek.gui.dock.util.color.ColorManager;
import bibliothek.gui.dock.util.color.DockColor;

/**
 * Paints the background of the tab by just painting a single line.
 * @author Benjamin Sigg
 */
@ColorCodes( "stack.border" )
public class XLinePainter implements TabPanePainter {
    private AbstractDockColor color = new AbstractDockColor( "stack.border", DockColor.KIND_DOCK_COLOR, Color.BLACK ){
        @Override
        protected void changed( Color oldColor, Color newColor ) {
            pane.repaint();
        }        
    };

    private XEclipseTabPane pane;

    /**
     * Creates a new painter.
     * @param pane the component for which this painter will work
     */
    public XLinePainter( XEclipseTabPane pane ){
        this.pane = pane;
    }

    public void setController( DockController controller ) {
        ColorManager colors = controller == null ? null : controller.getColors();
        color.setManager( colors );
    }
    
    public void paintBackground( Graphics g ){
		// @liso: set background of docking panel
		g.setColor(UIManager.getColor("Panel.group.background"));
		 int w = pane.getComponent().getWidth();
		 int h = pane.getComponent().getHeight();
		g.fillRect(0, 0, w, h);
    }

    public void paintForeground( Graphics g ){
      // ignore
    }
}
