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
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Paint;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.RenderingHints;

import javax.swing.UIManager;
import javax.swing.border.Border;

import bibliothek.extension.gui.dock.theme.eclipse.stack.tab.Arch;
import bibliothek.extension.gui.dock.theme.eclipse.stack.tab.ArchGradientPainter;
import bibliothek.extension.gui.dock.theme.eclipse.stack.tab.BorderedComponent;
import bibliothek.extension.gui.dock.theme.eclipse.stack.tab.DefaultInvisibleTab;
import bibliothek.extension.gui.dock.theme.eclipse.stack.tab.InvisibleTab;
import bibliothek.extension.gui.dock.theme.eclipse.stack.tab.InvisibleTabPane;
import bibliothek.extension.gui.dock.theme.eclipse.stack.tab.TabPanePainter;
import bibliothek.gui.DockController;
import bibliothek.gui.Dockable;
import bibliothek.gui.dock.util.Transparency;
import bibliothek.gui.dock.util.color.ColorCodes;


@ColorCodes({
    "stack.tab.border", 
    "stack.tab.border.selected", 
    "stack.tab.border.selected.focused", 
    "stack.tab.border.selected.focuslost",
    "stack.tab.border.disabled",
    
    "stack.tab.top", 
    "stack.tab.top.selected", 
    "stack.tab.top.selected.focused",
    "stack.tab.top.selected.focuslost",
    "stack.tab.top.disabled", 
    
    "stack.tab.text", 
    "stack.tab.text.selected", 
    "stack.tab.text.selected.focused", 
    "stack.tab.text.selected.focuslost",
    "stack.tab.text.disabled", 
    
    "stack.border" })
public class XEclipseTabPainter extends XBaseTabComponent {
    private final int[] TOP_LEFT_CORNER_X = { 0, 1, 1, 2, 3, 4, 5, 6 };
    private final int[] TOP_LEFT_CORNER_Y = { 6, 5, 4, 3, 2, 1, 1, 0 };
    
    private static final int CORNER_RADIUS = 25;
    
    private Arch arch;
    private boolean wasPreviousSelected = false;
    
    /**
     * This factory creates instances of {@link ArchGradientPainter}.
     */
    public static final XTabPainter FACTORY = new XTabPainter(){
        public XTabComponent createTabComponent( XEclipseTabPane pane, Dockable dockable ) {
            return new XEclipseTabPainter( pane, dockable );
        }
        
        public TabPanePainter createDecorationPainter( XEclipseTabPane pane ){
            return new XLinePainter( pane );
        }
        
        public InvisibleTab createInvisibleTab( InvisibleTabPane pane, Dockable dockable ){
            return new DefaultInvisibleTab( pane, dockable );
        }

        public Border getFullBorder( BorderedComponent owner, DockController controller, Dockable dockable ){
          return UIManager.getBorder("Dock.title.border");
        }
    };

    /** number of pixels at the left side that are empty and under the selected predecessor of this tab */
    private final int TAB_OVERLAP = 24;

    /**
     * Creates a new painter.
     * @param pane the owner of this painter
     * @param dockable the dockable which this painter represents
     */
    public XEclipseTabPainter( XEclipseTabPane pane, Dockable dockable ){
        super( pane, dockable );

        setOpaque( false );

        update();
        updateFont();
        updateBorder();
    }

    @Override
    public void updateBorder(){
//      border is not wanted
    }

    public Insets getOverlap( XTabComponent other ) {
        if( other instanceof ArchGradientPainter ){
            ArchGradientPainter painter = (ArchGradientPainter)other;
            if( painter.isSelected() ){
                if( getOrientation().isHorizontal() )
                    return new Insets( 0, 10 + TAB_OVERLAP, 0, 0 );
                else
                    return new Insets( 10 + TAB_OVERLAP, 0, 0, 0 );
            }
        }
        
        return new Insets( 0, 0, 0, 0 );
    }
    
    @Override
    public Dimension getPreferredSize(){
        boolean previousSelected = isPreviousTabSelected();
        if( wasPreviousSelected != previousSelected ){
            update();
        }
        
        return super.getPreferredSize();
    }
    
    @Override
    public Dimension getMinimumSize(){
        boolean previousSelected = isPreviousTabSelected();
        if( wasPreviousSelected != previousSelected ){
            update();
        }
        
        return super.getMinimumSize();
    }
    
    @Override
    public void updateFocus(){
        update();
        updateBorder();
        updateFont();
    }
    
    @Override
    protected void updateOrientation(){
        update();
    }
    
    @Override
    protected void updateSelected(){
        update();
        updateBorder();
        updateFont();
    }
    
    @Override
    protected void updateColors(){
        update();   
    }
    
    @Override
    protected void updateEnabled(){
        updateBorder();
        update();
    }
    
    /**
     * Updates the layout information of this painter.
     */
    protected void update(){
        wasPreviousSelected = isPreviousTabSelected();
        
        Insets labelInsets = null;
        Insets buttonInsets = null;
        
        switch( getOrientation() ){
            case TOP_OF_DOCKABLE:
            case BOTTOM_OF_DOCKABLE:
                labelInsets = new Insets( 3, 5, 3, 2 );
                buttonInsets = new Insets( 1, 0, 1, 5 );
                break;
            case LEFT_OF_DOCKABLE:
            case RIGHT_OF_DOCKABLE:
                labelInsets = new Insets( 5, 3, 2, 3 );
                buttonInsets = new Insets( 0, 1, 5, 1 );
                break;
        }
        
        getLabel().setForeground( getTextColor() );
        Font font = UIManager.getLookAndFeelDefaults().getFont("defaultFont");
		getLabel().setFont(font.deriveFont(Font.BOLD));
        setLabelInsets( labelInsets );
        setButtonInsets( buttonInsets );
        
        revalidate();
        repaint();
    }
    
    protected Arch arch( int width, int height ){
        if( arch == null || arch.getWidth() != width || arch.getHeight() != height )
            arch = new Arch( width, height );
        return arch;
    }

    private Color getTextColor(){
		// @liso: change tab foreground
		return UIManager.getColor("Dock.foreground");
    }
    
    @Override
    public void paintBackground( Graphics g ){
        Graphics2D g2d = (Graphics2D) g;

        Color color;
		// @liso: change tab color

//        if( !isEnabled() ){
//            color = colorStackTabTopDisabled.value();
//        }else 
        if( isFocused() || isSelected() ){
        	color = UIManager.getColor("Dock.selectedBackground");
        }
        else{
        	color = UIManager.getColor("Dock.background");
        }
        
		paintBackground(g2d, color);
    }
    
    @Override
    public void paintForeground( Graphics g ){
		// actually sets the background of unselected tabs....
        // draw separator lines
		// if (!isSelected()){
		// int w = getWidth();
		// int h = getHeight();
		// Graphics2D g2d = (Graphics2D) g;
		// g2d.setRenderingHint( RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON );
		// g2d.setColor(colorStackTabTopDisabled.value());
		// if( getOrientation().isHorizontal() ) {
		// g2d.fillRoundRect(0, 0, w - 3, h, CORNER_RADIUS, CORNER_RADIUS);
		// }
		// else {
		// g2d.fillRoundRect(0, 0, w, h - 3, CORNER_RADIUS, CORNER_RADIUS);
		// }
		// g2d.setRenderingHint( RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_DEFAULT );
		// }
    }

    @Override
    public boolean contains( int x, int y ) {
        if( !super.contains( x, y ) )
            return false;

        if( containsButton( x, y )){
            return true;
        }
        
        if( isSelected() ){
            int w = getWidth();
            int h = getHeight();
            
            Polygon left = leftSide( 0, 0, w, h );
            if( left.contains( x, y ))
                return true;
            
            Polygon right = rightSide( 0, 0, w, h );
            if( right.contains( x, y ))
                return true;
            
            Rectangle leftBox = left.getBounds();
            Rectangle rightBox = right.getBounds();
            
            if( getOrientation().isHorizontal() ){
                if( leftBox.x + leftBox.width > x )
                    return false;
            
                if( rightBox.x < x )
                    return false;
            }
            else{
                if( leftBox.y + leftBox.height > y )
                    return false;
                
                if( rightBox.y < y )
                    return false;
            }
            
            return true;
        }
        else
            return true;
    }
    
    /**
     * Paints the background of a selected tab.
     * @param g the graphics context to use
     * @param color the color at the top
     */
    private void paintBackground( Graphics g, Color color){
        int x = 0;
        int y = 0;
        int w = getWidth();
        int h = getHeight();
        Graphics2D g2d = (Graphics2D) g;
        
        g2d.setRenderingHint( RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON );
        
        Paint old = g2d.getPaint();
        g2d.setPaint( color );
        
        if( getTransparency() != Transparency.TRANSPARENT ){
            g.fillRoundRect(x, y, w - 1, h - 1, CORNER_RADIUS, CORNER_RADIUS);
        }
        
        g2d.setPaint( old );
        
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_DEFAULT);
    }
    
    /**
     * Mirrors <code>coordinates</code>, an element that has the value
     * <code>min + x</code> afterwards has the value <code>max - x</code>.
     * @param coordinates the array to mirror
     */
    private void mirror( int[] coordinates ){
        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;
        
        for( int c : coordinates ){
            min = Math.min( min, c );
            max = Math.max( max, c );
        }
        
        for( int i = 0, n = coordinates.length; i<n; i++ ){
            coordinates[i] = max - (coordinates[i] - min);
        }
    }
    
    private void transformFromTopToOrientation( Polygon polygon ){
        switch( getOrientation() ){
            case BOTTOM_OF_DOCKABLE:
                mirror( polygon.ypoints );
                break;
            case RIGHT_OF_DOCKABLE:
                mirror( polygon.ypoints );
                // fall through
            case LEFT_OF_DOCKABLE:
                int[] temp = polygon.xpoints;
                polygon.xpoints = polygon.ypoints;
                polygon.ypoints = temp;
        }
    }
    
    /**
     * Creates a polygon to paint the left or top side of a tab.
     * @param x the x coordinate of the area in which to paint
     * @param y the y coordinate of the area in which to paint
     * @param w the with of the paintable area
     * @param h the height of the paintable area
     * @return the new polygon
     */
    private Polygon leftSide( int x, int y, int w, int h ){
        if( getOrientation().isVertical() ){
            int t = x;
            x = y;
            y = t;
            
            t = w;
            w = h;
            h = t;
        }
        
        Polygon polygon = leftSideTop( x, y, w, h );
        transformFromTopToOrientation( polygon );
        return polygon;
    }
    
    private Polygon leftSideTop( int x, int y, int w, int h ){
        int[] xPoints = new int[ TOP_LEFT_CORNER_X.length+2 ];
        int[] yPoints = new int[ TOP_LEFT_CORNER_Y.length+2 ];
        
        System.arraycopy( TOP_LEFT_CORNER_X, 0, xPoints, 1, TOP_LEFT_CORNER_X.length );
        System.arraycopy( TOP_LEFT_CORNER_Y, 0, yPoints, 1, TOP_LEFT_CORNER_Y.length );
        
        int max = 0;
        
        for( int i = 1, n = xPoints.length-1; i<n; i++ ){
            max = Math.max( max, xPoints[i] );
            xPoints[i] += x;
            yPoints[i] += y;
        }
        
        xPoints[0] = x;
        yPoints[0] = y+h-1;
        
        int index = xPoints.length-1;
        
        xPoints[index] = x+max;
        yPoints[index] = y+h-1;
        
        return new Polygon( xPoints, yPoints, xPoints.length );
    }
    
    
    /**
     * Creates a polygon to paint the right or bottom side of a tab.
     * @param x the x coordinate of the area in which to paint
     * @param y the y coordinate of the area in which to paint
     * @param w the with of the paintable area
     * @param h the height of the paintable area
     * @return the new polygon
     */
    private Polygon rightSide( int x, int y, int w, int h ){
        int labelMin = 6;
        
        if( getIcon() != null ){
            if( getOrientation().isHorizontal() ){
                labelMin += getIcon().getIconWidth() + getLabel().getIconOffset();
            }
            else{
                labelMin += getIcon().getIconHeight() + getLabel().getIconOffset();
            }
        }
        
        if( getOrientation().isVertical() ){
            int t = x;
            x = y;
            y = t;
            
            t = w;
            w = h;
            h = t;
        }
        
        Polygon polygon = rightSideTop( x, y, w, h, labelMin );
        transformFromTopToOrientation( polygon );
        return polygon;
    }
    
    private Polygon rightSideTop( int x, int y, int w, int h, int labelMin ){
        Arch arch = arch( Math.max( 1, Math.min( w-labelMin, h*34/22 ) ), h );
        
        int[] xPoints = new int[ arch.getWidth()+1 ];
        int[] yPoints = new int[ arch.getWidth()+1 ];
        
        for( int i = 0, n = arch.getWidth(); i<n; i++ ){
            xPoints[i] = x+w-n+i;
            yPoints[i] = arch.getValue( i ) + y;
        }
        
        int index = xPoints.length-1;
        
        xPoints[ index ] = xPoints[ 0 ];
        yPoints[ index ] = yPoints[ index-1 ];
        
        return new Polygon( xPoints, yPoints, xPoints.length );
    }
    
}
