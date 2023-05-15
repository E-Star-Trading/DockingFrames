package bibliothek.gui.dock.station.stack.menu;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.AbstractButton;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
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
		int maxScreenHeight = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds().height;
		int contentHeight = 0;
		int contentWidth = 0;
		
		/*
		 * Component c - item in the Popup Menu List
		 * c.getMaximumSize() - automatically computed size of one item in the Popup Menu
		 */
		for (Component c : panelMenus.getComponents()) {
			Dimension cMaxSize = c.getMaximumSize();

			contentHeight += (int) cMaxSize.getHeight();

			int cWidth = (int) cMaxSize.getWidth();

			if (cWidth > contentWidth) {
				contentWidth = cWidth;
			}
		}

		boolean overflowsVertical = contentHeight > maxScreenHeight;
		boolean overflowsHorizontal = contentWidth > MAX_WIDTH;
		
		int scrollbarVertical = overflowsVertical ? jScrollPane.getVerticalScrollBar().getPreferredSize().width : 0;
		int scrollbarHorizontal = overflowsHorizontal ? jScrollPane.getHorizontalScrollBar().getPreferredSize().width : 0;

		Dimension paneSize = new Dimension(contentWidth + scrollbarVertical + 20, contentHeight + scrollbarHorizontal);

		Dimension popupSize = new Dimension(
			Math.min(MAX_WIDTH, paneSize.width) + 20,
			Math.min(maxScreenHeight, paneSize.height - (overflowsVertical ? 20 : 0))
		);
		
		// necessary for displaying the scroll bars
		this.setPopupSize(popupSize);

		this.setInvoker(invoker);

		Point invokerOrigin = invoker.getLocationOnScreen();
		
		/*
		 * adjusting the Popup Menu according to the Widows Task Bar height
		 * the Popup Menu on the main screen ends just above the Windows Task Bar
		 * other screens will use the same adjustment
		 */
		int originY = (int) invokerOrigin.getY() + y;
		int screenHeight = (int) Toolkit.getDefaultToolkit().getScreenSize().getHeight();
		int windowsTaskbarHeight = screenHeight - GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds().height;
		
		// lowestY - the lowest point where the popup can be displayed without being hidden by task bar
		int lowestY = screenHeight - popupSize.height - windowsTaskbarHeight;

		this.setLocation((int) invokerOrigin.getX() + x, Math.min(originY, lowestY));

		this.setVisible(true);
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
