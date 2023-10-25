package bibliothek.gui.dock.station.stack.menu;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.GridLayout;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.AbstractButton;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.UIManager;

/**
 * 
 * scrollable JPopup Menu
 * 
 */

public class XJPopupMenu extends JPopupMenu implements ActionListener {

	private static final long	serialVersionUID	= 1;
	private JPanel				panelMenus			= new JPanel();
	private JScrollPane			jScrollPane			= new JScrollPane();
	
	// popup menu can take maximally 1/3 of the screen, otherwise vertical scroll bar appears
	public static final int MAX_WIDTH = (int) Toolkit.getDefaultToolkit().getScreenSize().getWidth()/3;
	
	public XJPopupMenu() {
		super();
		this.setLayout(new BorderLayout());
		panelMenus.setLayout(new GridLayout(0, 1));
		panelMenus.setBackground(UIManager.getColor("MenuItem.background"));

		jScrollPane.setViewportView(panelMenus);
		jScrollPane.setBorder(null);

		super.add(jScrollPane, BorderLayout.CENTER);
	}

	/**
	 * x, y - position offsets
	 */
	@Override
	public void show(Component invoker, int x, int y) {
		panelMenus.validate();
		GraphicsDevice gd = MouseInfo.getPointerInfo().getDevice();
		int maxScreenHeight = gd.getDisplayMode().getHeight();
		int contentHeight = 0;
		int contentWidth = 0;
		
		/*
		 * Component c - item in the Popup Menu List
		 * c.getMaximumSize() - automatically computed size of one item in the Popup Menu
		 */
		for (Component c : panelMenus.getComponents()) {
			Dimension cMaxSize = c.getMaximumSize();

			contentHeight += (int) cMaxSize.getHeight();
			
			contentWidth = Math.max((int) cMaxSize.getWidth(), contentWidth);
		}

		boolean overflowsVertical = contentHeight > maxScreenHeight;
		boolean overflowsHorizontal = contentWidth > MAX_WIDTH;
		
		JScrollBar verticalScrollBar = jScrollPane.getVerticalScrollBar();
		int scrollbarVertical = overflowsVertical ? verticalScrollBar.getPreferredSize().width : 0;
		int scrollbarHorizontal = overflowsHorizontal ? jScrollPane.getHorizontalScrollBar().getPreferredSize().width : 0;
		verticalScrollBar.setUnitIncrement(30);
		
		Dimension paneSize = new Dimension(contentWidth + scrollbarVertical + 20, contentHeight + scrollbarHorizontal);
		int windowsTaskbarHeight = Toolkit.getDefaultToolkit().getScreenInsets(gd.getDefaultConfiguration()).bottom;
		
		Dimension popupSize = new Dimension(
			Math.min(MAX_WIDTH, paneSize.width) + 20,
			Math.min(maxScreenHeight - windowsTaskbarHeight, paneSize.height - (overflowsVertical ? 20 : 0))
		);
		
		// necessary for displaying the scroll bars
		this.setPopupSize(popupSize);

		this.setInvoker(invoker);
		Point invokerOrigin = invoker.getLocationOnScreen();
	    
		int mouseY = MouseInfo.getPointerInfo().getLocation().y;
	    // Adjust mouseY to be relative to the current screen
	    GraphicsDevice currentScreen = getScreenDeviceAt(MouseInfo.getPointerInfo().getLocation());
	    if (currentScreen != null) {
	        Rectangle screenBounds = currentScreen.getDefaultConfiguration().getBounds();
	        mouseY -= screenBounds.y;
	    }
		
		// lowestY - the lowest point where the popup can be displayed without being hidden by task bar
		int lowestY = maxScreenHeight - popupSize.height - windowsTaskbarHeight;

		Rectangle screenBounds = currentScreen.getDefaultConfiguration().getBounds();
		int popupX = (int) invokerOrigin.getX() + x;
		int popupY = screenBounds.y + Math.min(mouseY, lowestY);
		this.setLocation(popupX, popupY);
		this.setVisible(true);
	}
	
	public static GraphicsDevice getScreenDeviceAt(Point point) {
	    GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
	    GraphicsDevice[] screens = ge.getScreenDevices();
	    
	    for (GraphicsDevice screen : screens) {
	        Rectangle bounds = screen.getDefaultConfiguration().getBounds();
	        if (bounds.contains(point)) {
	            return screen;
	        }
	    }
	    
	    return null;
	}

	public void hidemenu() {
		if (this.isVisible()) {
			this.setVisible(false);
		}
	}

	public void add(AbstractButton menuItem) {
		if (menuItem == null) {
			return;
		}
		panelMenus.add(menuItem);
		menuItem.removeActionListener(this);
		menuItem.addActionListener(this);

	}

	public void actionPerformed(ActionEvent e) {
		this.hidemenu();
	}
	
	@Override
	public Component[] getComponents() {
		return panelMenus.getComponents();
	}
	
}
