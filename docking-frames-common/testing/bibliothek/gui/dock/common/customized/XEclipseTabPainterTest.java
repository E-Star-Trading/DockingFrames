package bibliothek.gui.dock.common.customized;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;

import javax.swing.UIManager;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import bibliothek.gui.Dockable;
import bibliothek.gui.dock.DefaultDockable;
import bibliothek.gui.dock.station.stack.tab.TabComponentLayoutManager;
import bibliothek.gui.dock.station.stack.tab.layouting.TabPlacement;

/**
 * Covers the three hooks that let a client change tab metrics without reimplementing the painter:
 * {@link XEclipseTabPainter.Factory}, {@link XEclipseTabPainter#labelInsetsFor(TabPlacement)} /
 * {@link XEclipseTabPainter#buttonInsetsFor(TabPlacement)} and
 * {@link XEclipseTabPainter#labelFont()}.
 *
 * <p>The insets tests matter because {@link XEclipseTabPainter#update()} runs again on every
 * selection, focus, colour, orientation and enablement change; before the hooks existed it re-imposed
 * literals and discarded whatever a client had set.</p>
 *
 * <p>No test here asserts a pixel height from a font: the available fonts differ per machine. The
 * heights asserted are differences and sums over the label's own preferred size.</p>
 */
public class XEclipseTabPainterTest{
    /** the label insets the painter has used for horizontal tabs since 1.1.3p4 */
    private static final Insets HORIZONTAL_LABEL_INSETS = new Insets( 3, 5, 3, 2 );

    /** the label insets the painter has used for vertical tabs since 1.1.3p4 */
    private static final Insets VERTICAL_LABEL_INSETS = new Insets( 5, 3, 2, 3 );

    private XEclipseTabPane pane;
    private Font previousDefaultFont;

    @Before
    public void setUp(){
        pane = new XEclipseTabPane( new XEclipseTheme(), null );

        // update() reads "defaultFont" from the look and feel; the stock Metal defaults have no such key
        previousDefaultFont = UIManager.getLookAndFeelDefaults().getFont( "defaultFont" );
        if( previousDefaultFont == null ){
            UIManager.getLookAndFeelDefaults().put( "defaultFont", UIManager.getFont( "Label.font" ) );
        }
    }

    @After
    public void tearDown(){
        UIManager.getLookAndFeelDefaults().put( "defaultFont", previousDefaultFont );
    }

    @Test
    public void theDefaultInsetsAreTheOnesThePainterHasAlwaysUsed(){
        XEclipseTabPainter tab = tab( XEclipseTabPainter.FACTORY );

        assertEquals( HORIZONTAL_LABEL_INSETS, tab.getLabelInsets() );
        assertEquals( new Insets( 1, 0, 1, 5 ), tab.getButtonInsets() );

        tab.setOrientation( TabPlacement.LEFT_OF_DOCKABLE );

        assertEquals( VERTICAL_LABEL_INSETS, tab.getLabelInsets() );
        assertEquals( new Insets( 0, 1, 5, 1 ), tab.getButtonInsets() );
    }

    @Test
    public void overriddenInsetsSurviveTheUpdatesThatRunOnEveryStateChange(){
        Insets label = new Insets( 1, 5, 1, 2 );
        Insets button = new Insets( 0, 0, 0, 5 );
        XEclipseTabPainter tab = tab( insetFactory( label, button ) );

        assertEquals( label, tab.getLabelInsets() );
        assertEquals( button, tab.getButtonInsets() );

        tab.setEnabled( false );
        tab.setEnabled( true );
        tab.updateFocus();
        tab.getPreferredSize();

        assertEquals( "update() re-imposed its own insets", label, tab.getLabelInsets() );
        assertEquals( "update() re-imposed its own insets", button, tab.getButtonInsets() );
    }

    @Test
    public void anOverrideKeepsTheOrientationSplit(){
        Insets horizontal = new Insets( 1, 5, 1, 2 );
        XEclipseTabPainter tab = tab( insetFactory( horizontal, new Insets( 0, 0, 0, 5 ) ) );

        tab.setOrientation( TabPlacement.RIGHT_OF_DOCKABLE );

        assertEquals( "a vertical tab was padded on the horizontal axis", new Insets( 5, 1, 2, 1 ), tab.getLabelInsets() );
    }

    @Test
    public void anOverriddenLabelFontReplacesTheLookAndFeelFont(){
        final Font wanted = new Font( Font.MONOSPACED, Font.ITALIC, 23 );
        XEclipseTabPainter tab = tab( new XEclipseTabPainter.Factory(){
            @Override
            public XTabComponent createTabComponent( XEclipseTabPane owner, Dockable dockable ){
                return new XEclipseTabPainter( owner, dockable ){
                    @Override
                    protected Font labelFont(){
                        return wanted;
                    }
                };
            }
        } );

        assertEquals( wanted, layout( tab ).getLabel().getFont() );
    }

    @Test
    public void theFactoryCreatesWhateverItsSubclassBuilds(){
        XEclipseTabPainter tab = tab( insetFactory( new Insets( 1, 1, 1, 1 ), new Insets( 1, 1, 1, 1 ) ) );

        assertTrue( "a subclass of the factory could not supply its own component", tab instanceof Marked );
        assertSame( XLinePainter.class, new XEclipseTabPainter.Factory().createDecorationPainter( pane ).getClass() );
    }

    @Test
    public void theTabIsAsHighAsItsLabelPlusTheLabelInsets(){
        // the contract a client tuning the insets relies on: nothing else contributes to the height
        Insets label = new Insets( 1, 5, 1, 2 );
        XEclipseTabPainter tab = tab( insetFactory( label, new Insets( 0, 0, 0, 5 ) ) );
        tab.setText( "Quoting Sheet" );

        TabComponentLayoutManager layout = layout( tab );
        assertEquals( 0, layout.getFreeSpaceToOpenSide() );
        assertEquals( 0, layout.getFreeSpaceToParallelBorder() );
        assertEquals( 0, layout.getFreeSpaceToSideBorder() );

        Dimension labelSize = layout.getLabel().getPreferredSize();
        assertTrue( "the label reported no height, so the sum below would prove nothing", labelSize.height > 0 );
        assertEquals( labelSize.height + label.top + label.bottom, tab.getPreferredSize().height );
    }

    private XEclipseTabPainter tab( XTabPainter factory ){
        XTabComponent component = factory.createTabComponent( pane, new DefaultDockable( "Quoting Sheet" ) );
        assertNotNull( component );
        return (XEclipseTabPainter)component;
    }

    private TabComponentLayoutManager layout( XEclipseTabPainter tab ){
        return (TabComponentLayoutManager)tab.getLayout();
    }

    /**
     * A factory whose tabs answer <code>label</code> and <code>button</code> for horizontal placements
     * and their transposition for vertical ones.
     */
    private XEclipseTabPainter.Factory insetFactory( final Insets label, final Insets button ){
        return new XEclipseTabPainter.Factory(){
            @Override
            public XTabComponent createTabComponent( XEclipseTabPane owner, Dockable dockable ){
                return new MarkedPainter( owner, dockable, label, button );
            }
        };
    }

    /** lets {@link #theFactoryCreatesWhateverItsSubclassBuilds()} recognise a client-built component */
    private interface Marked{
        // marker only
    }

    private static class MarkedPainter extends XEclipseTabPainter implements Marked{
        private final Insets label;
        private final Insets button;

        MarkedPainter( XEclipseTabPane pane, Dockable dockable, Insets label, Insets button ){
            super( pane, dockable );
            this.label = label;
            this.button = button;
            update();
        }

        @Override
        protected Insets labelInsetsFor( TabPlacement placement ){
            // the superclass constructor calls update() before our fields are assigned
            return label == null ? super.labelInsetsFor( placement ) : oriented( label, placement );
        }

        @Override
        protected Insets buttonInsetsFor( TabPlacement placement ){
            return button == null ? super.buttonInsetsFor( placement ) : oriented( button, placement );
        }

        private Insets oriented( Insets insets, TabPlacement placement ){
            switch( placement ){
                case LEFT_OF_DOCKABLE:
                case RIGHT_OF_DOCKABLE:
                    return new Insets( insets.left, insets.top, insets.right, insets.bottom );
                default:
                    return new Insets( insets.top, insets.left, insets.bottom, insets.right );
            }
        }
    }
}
