package bibliothek.gui.dock.common.customized;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import bibliothek.extension.gui.dock.theme.eclipse.EclipseTabStateInfo;
import bibliothek.gui.Dockable;
import bibliothek.gui.dock.DefaultDockable;
import bibliothek.gui.dock.action.DockAction;
import bibliothek.gui.dock.action.DockActionSource;
import bibliothek.gui.dock.action.DefaultDockActionSource;
import bibliothek.gui.dock.common.CControl;
import bibliothek.gui.dock.common.action.predefined.CCloseAction;

/**
 * Where the close action is shown.
 *
 * <p>The fork used to answer this one by itself - <code>if( action instanceof CCloseAction.Action ) return
 * showForTab;</code> - which put a close button on every tab and kept it off the side whatever the
 * {@link bibliothek.extension.gui.dock.theme.eclipse.EclipseThemeConnector} said. No client could move it,
 * because the connector was never asked. Now it is asked, like every other action, and
 * {@link XDefaultEclipseThemeConnector} answers SIDE for an action carrying no
 * {@link bibliothek.extension.gui.dock.theme.eclipse.EclipseTabDockAction} annotation - which
 * {@code CCloseAction.Action} does not.</p>
 */
public class XEclipseDockActionSourceTest{

    private java.awt.Color previousIconColor;

    /**
     * The close action is given an icon before anything decides where it goes, and the icon library reads its
     * colour out of the look and feel. Without one it throws, and this test would fail for a reason that has
     * nothing to do with what it asks.
     */
    @Before
    public void giveTheIconLibraryAColour(){
        previousIconColor = javax.swing.UIManager.getColor( "icon.color" );
        if( previousIconColor == null ){
            javax.swing.UIManager.put( "icon.color", java.awt.Color.WHITE );
        }
    }

    @After
    public void restoreTheIconColour(){
        javax.swing.UIManager.put( "icon.color", previousIconColor );
    }

    @Test
    public void theCloseActionIsOfferedToTheSideAndNotToTheTab(){
        DockAction close = closeAction();

        assertFalse( "the close action is still hard-wired onto the tab", includedIn( close, true ) );
        assertTrue( "the close action never reaches the side", includedIn( close, false ) );
    }

    /** The counterpart: an action the connector sends to the tab still goes there. */
    @Test
    public void anActionTheConnectorSendsToTheTabStillGoesThere(){
        DockAction tabbed = new TabbedAction();

        assertTrue( includedIn( tabbed, true ) );
        assertFalse( includedIn( tabbed, false ) );
    }

    private static boolean includedIn( DockAction action, boolean showForTab ){
        DefaultDockActionSource source = new DefaultDockActionSource();
        source.add( action );
        Probe probe = new Probe( new XEclipseTheme(), source, new Tab( new DefaultDockable( "Sheet" ) ), showForTab );
        return probe.includes( action );
    }

    private static DockAction closeAction(){
        CControl control = new CControl();
        return new CCloseAction( control ).intern();
    }

    /** An action that says it belongs on the tab, the way CCloseAction.Action does not. */
    @bibliothek.extension.gui.dock.theme.eclipse.EclipseTabDockAction
    private static class TabbedAction extends bibliothek.gui.dock.action.actions.SimpleButtonAction{
        // the annotation is the whole point
    }

    /** Opens up the protected decision this test is about. */
    private static class Probe extends XEclipseDockActionSource{
        Probe( XEclipseTheme theme, DockActionSource source, EclipseTabStateInfo tab, boolean showForTab ){
            super( theme, source, tab, showForTab );
        }

        boolean includes( DockAction action ){
            return include( action );
        }
    }

    /** A tab that is neither selected nor focused, and whose dockable has no controller. */
    private static class Tab implements EclipseTabStateInfo{
        private final Dockable dockable;

        Tab( Dockable dockable ){
            this.dockable = dockable;
        }

        public Dockable getDockable(){
            return dockable;
        }

        public boolean isSelected(){
            return false;
        }

        public boolean isFocused(){
            return false;
        }
    }
}
