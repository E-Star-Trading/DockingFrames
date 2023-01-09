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

import javax.swing.Icon;
import javax.swing.UIManager;

import bibliothek.extension.gui.dock.theme.eclipse.EclipseTabStateInfo;
import bibliothek.extension.gui.dock.theme.eclipse.EclipseThemeConnector;
import bibliothek.gui.Dockable;
import bibliothek.gui.dock.action.DockAction;
import bibliothek.gui.dock.action.DockActionSource;
import bibliothek.gui.dock.action.FilteredDockActionSource;
import bibliothek.gui.dock.common.action.CButton;
import bibliothek.gui.dock.common.action.CMenu;
import bibliothek.gui.dock.common.action.core.CommonSimpleButtonAction;
import bibliothek.gui.dock.common.action.core.CommonSimpleMenuAction;
import bibliothek.gui.dock.common.action.predefined.CCloseAction;
import bibliothek.gui.dock.common.action.predefined.CMaximizeAction;
import bibliothek.gui.dock.common.action.predefined.CMinimizeAction;
import bibliothek.gui.dock.common.action.predefined.CNormalizeAction;
import bibliothek.gui.dock.common.intern.action.CExtendedModeAction;
import data.bibliothek.gui.dock.common.icons.DockFontIcon;
import data.bibliothek.gui.dock.common.icons.DockFontIconIdentifier;
import data.bibliothek.gui.dock.common.icons.DockFontIconLibrary;

/**
 * A list of {@link DockAction DockActions} filtered by the 
 * {@link EclipseThemeConnector}, using {@link EclipseThemeConnector#shouldShowOnTab(DockAction, EclipseTabStateInfo)}.
 * @author Benjamin Sigg
 *
 */
public class XEclipseDockActionSource extends FilteredDockActionSource {
    /** the theme for which this source is used */
    private XEclipseTheme theme;
    /** the tab associated with the {@link Dockable} */
    private EclipseTabStateInfo tab;
    
    /** whether this source is used to show actions on a tab */
    private boolean showForTab;
    
    /**
     * Creates a new source
     * @param theme the theme for which this source is used
     * @param source the source which is filtered
     * @param tab the tab associated with the {@link Dockable}
     * @param showForTab whether this source is associated with the tab (or not)
     */
    public XEclipseDockActionSource( XEclipseTheme theme, DockActionSource source, EclipseTabStateInfo tab, boolean showForTab ){
        super( source );
        this.theme = theme;
        this.tab = tab;
        this.showForTab = showForTab;
    }
    
    @Override
    protected boolean include( DockAction action ){
        EclipseThemeConnector connector = theme.getThemeConnector( tab.getDockable().getController() );
		if (action instanceof CCloseAction.Action) {
			((CCloseAction.Action) action)
					.setIcon(DockFontIconLibrary.getScalableIcon(DockFontIconIdentifier.CLOSE));
        }
		changeIcon(action);
		if (action instanceof CCloseAction.Action) {
			return showForTab;
		}
        if( showForTab ){
            return connector.shouldShowOnTab( action, tab );
        }
        else{
            return connector.shouldShowOnSide( action, tab );
        }
    }

	// @liso change icons in dockable
	private void changeIcon(DockAction action) {
		updateCMenuIconColorToThemeColor(action);
		if (action instanceof CExtendedModeAction.Action) {
			CExtendedModeAction.Action ext = (CExtendedModeAction.Action) action;
			if (ext.getAction() instanceof CMinimizeAction) {
				((CMinimizeAction.Action) action).setIcon(
						DockFontIconLibrary.getScalableIcon(DockFontIconIdentifier.MINIMIZE_WINDOW));
			}
			if (ext.getAction() instanceof CMaximizeAction) {
				((CMaximizeAction.Action) action).setIcon(
						DockFontIconLibrary.getScalableIcon(DockFontIconIdentifier.MAXIMIZE_WINDOW));
			}
			if (ext.getAction() instanceof CNormalizeAction) {
				((CNormalizeAction.Action) action).setIcon(
						DockFontIconLibrary.getScalableIcon(DockFontIconIdentifier.RESTORE_WINDOW));
			}
		}
	}

	private void updateCMenuIconColorToThemeColor(DockAction action) {
		if (action instanceof CommonSimpleMenuAction) {
			if (((CommonSimpleMenuAction) action).getAction() instanceof CMenu) {
				CommonSimpleMenuAction menu = (CommonSimpleMenuAction) action;
				updateFontIconColor(menu.getIcon());
			}
		}
		if (action instanceof CommonSimpleButtonAction) {
			if (((CommonSimpleButtonAction) action).getAction() instanceof CButton) {
				CommonSimpleButtonAction menu = (CommonSimpleButtonAction) action;
				updateFontIconColor(menu.getIcon());
			}
		}
	}

	private void updateFontIconColor(Icon actionIcon) {
		if (actionIcon instanceof DockFontIcon) {
			DockFontIcon icon = (DockFontIcon) actionIcon;
			icon.setColor(UIManager.getColor("icon.color"));
		}
	}

}
