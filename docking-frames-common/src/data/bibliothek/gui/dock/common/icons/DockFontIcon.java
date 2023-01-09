package data.bibliothek.gui.dock.common.icons;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.util.Objects;

import javax.swing.Icon;


/**
 * A FontIcon wraps an ImageIcon created based on Icon within a (custom) font.
 * The icon can be resized and changed in color. When changing one of these parameters, the wrapped ImageIcon is replaced using the {@link DockFontIconLibrary}.
 * 
 */
public class DockFontIcon implements Icon {

	private DockFontIconIdentifier	iconIdentifier;
	private Icon			scaledIcon;
	private float			fontSize;
	private Color			color;
	private boolean			cacheIcons;

	/**
	 * Creates a new FontIcon and its wrapped ImageIcon content by using the {@link DockFontIconLibrary}.
	 * 
	 * @param iconIdentifier The identifier of the icon.
	 * @param fontSize       The initial size of the icon.
	 * @param color          The initial color of the icon.
	 * @param cacheIcons      Boolean that indicates of the created image icons based on this icon should be cached within the {@link DockFontIconLibrary}.
	 */
	public DockFontIcon(DockFontIconIdentifier iconIdentifier, float fontSize, Color color, boolean cacheIcons) {
		this.iconIdentifier = iconIdentifier;
		this.fontSize = fontSize;
		this.color = color;
		this.cacheIcons = cacheIcons;
		this.scaledIcon = DockFontIconLibrary.getIcon(iconIdentifier, fontSize, color, cacheIcons);
	}

	/**
	 * Sets the font size of the icon to the given value.
	 * 
	 * @param fontSize If the given value is different from the fontSize of this Icon, then the wrapped ImageIcon is retrieved with the new size from the {@link DockFontIconLibrary}.
	 */
	public void setFonSize(float fontSize) {
		if (this.fontSize != fontSize) {
			this.fontSize = fontSize;
			this.scaledIcon = DockFontIconLibrary.getIcon(iconIdentifier, fontSize, color, cacheIcons);
		}
	}

	/**
	 * Sets the color of the icon to the given value.
	 * 
	 * @param color If the given value is different from the color of this Icon, then the wrapped ImageIcon is retrieved with the new color from the {@link DockFontIconLibrary}.
	 */
	public void setColor(Color color) {
		if (!Objects.equals(this.color, color)) {
			this.color = color;
			this.scaledIcon = DockFontIconLibrary.getIcon(iconIdentifier, fontSize, color, cacheIcons);
		}
	}

	@Override
	public void paintIcon(Component c, Graphics g, int x, int y) {
		scaledIcon.paintIcon(c, g, x, y);
	}

	@Override
	public int getIconWidth() {
		return scaledIcon.getIconWidth();
	}

	@Override
	public int getIconHeight() {
		return scaledIcon.getIconHeight();
	}

}
