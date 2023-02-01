package data.bibliothek.gui.dock.common.icons;

import java.awt.Color;
import java.awt.Image;
import java.io.InputStream;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

import javax.swing.Icon;
import javax.swing.UIManager;

import jiconfont.DefaultIconCode;
import jiconfont.IconCode;
import jiconfont.IconFont;
import jiconfont.swing.IconFontSwing;

/**
 * Library to retrieve icons from custom (icon) fonts.
 */
public class DockFontIconLibrary {

	private static final int											DEFAULT_FONT_SIZE				= 10;
	private static final String											DOCK_ICON_FONT_PATH				= "icon-set.ttf";
	private static final String											DOCK_ICON_FONT_NAME				= "DockIconFont";
	
	private static final int DEFAULT_BIG_FONT_SIZE = 12;

	// maps an icon identifier to its definition for icon creation
	private static final Map<DockFontIconIdentifier, FontIconDefinition>	ICON_IDENTIFIER_TO_ICON_IN_FONT	= new EnumMap<>(DockFontIconIdentifier.class);
	// saves create ImageIcons from a font based on a hashVvalue calculated by using its identifier, font size and color (see calculateHash() for hash creation)
	private static final Map<Integer, Icon>								iconCache						= new HashMap<>();

	static {
		// add mappings from icon identifier to font icon definitions including the name of the font, the icon is contained within and its unicode for creation.
		addFontIconDefinition(DockFontIconIdentifier.CLOSE, DOCK_ICON_FONT_NAME, '\uE800', DEFAULT_BIG_FONT_SIZE);
		addFontIconDefinition(DockFontIconIdentifier.MINIMIZE_WINDOW, DOCK_ICON_FONT_NAME, '\uF2D1', DEFAULT_BIG_FONT_SIZE);
		addFontIconDefinition(DockFontIconIdentifier.MAXIMIZE_WINDOW, DOCK_ICON_FONT_NAME, '\uF2D0', DEFAULT_BIG_FONT_SIZE);
		addFontIconDefinition(DockFontIconIdentifier.RESTORE_WINDOW, DOCK_ICON_FONT_NAME, '\uF2D2', DEFAULT_BIG_FONT_SIZE);
		// register custom font
		registerFont(DOCK_ICON_FONT_PATH, DOCK_ICON_FONT_NAME);
	}
	
	private static void addFontIconDefinition(DockFontIconIdentifier iconIdentifier, String customFontName, char unicode, int defaultFontSize, Color defaultColor) {
		ICON_IDENTIFIER_TO_ICON_IN_FONT.put(iconIdentifier, new FontIconDefinition(customFontName, unicode, defaultFontSize, defaultColor));
	}

	private static void addFontIconDefinition(DockFontIconIdentifier iconIdentifier,
			String customFontName,
			char unicode,
			int defaultFontSize) {
		addFontIconDefinition(iconIdentifier, customFontName, unicode, defaultFontSize, null);
	}

	private DockFontIconLibrary() {/* static class */}

	// register a font by name within the jIconFont library to be able to create icons based on it
	private static void registerFont(String filePath, String fontName) {
		IconFontSwing.register(new IconFont() {

			@Override
			public String getFontFamily() {
				return fontName;
			}

			@Override
			public InputStream getFontInputStream() {
				return this.getClass().getResourceAsStream(filePath);
			}
		});
	}

	
	/**
	 * Returns an FontIcon for the given IconIdentifier. This function should be used, if an Icon should be resizable/changeable in color, otherwise getIcon() can be used directly.
	 * 
	 * @param iconIdentifier The IconIdentifier of the FontIcon to return.
	 * @param cacheIcons     If true, the generated ImageIcons for the fontIcon are cached within this Library by color and fontSize. Should be set to true, if the fontSize and color of the icon is changed often and previous values are re-used to
	 *                           enhance performance.
	 * @return A FontIcon for the given IconIdentifier, with its default fontSize and color. Null is returned, if no icon could be found for the identifier.
	 */
	public static DockFontIcon getScalableIcon(DockFontIconIdentifier iconIdentifier, boolean cacheIcons) {
		if (!ICON_IDENTIFIER_TO_ICON_IN_FONT.containsKey(iconIdentifier)) {
			return null;
		} else {
			FontIconDefinition iconInFont = ICON_IDENTIFIER_TO_ICON_IN_FONT.get(iconIdentifier);
			Color lookAndFeelColor = UIManager.getColor("icon.color");
			Color color = iconInFont.defaultColor == null ? lookAndFeelColor : iconInFont.defaultColor;
			return new DockFontIcon(iconIdentifier, (long) iconInFont.defaultFontSize, color, cacheIcons);
		}
	}
	
	public static DockFontIcon getScalableIcon(DockFontIconIdentifier iconIdentifier) {
	  return getScalableIcon(iconIdentifier, true);
	}

	/**
	 * Returns an FontIcon for the given IconIdentifier. This function should be used, if an Icon should be resizable/changeable in color, otherwise getIcon() can be used directly.
	 * 
	 * @param iconIdentifier The IconIdentifier of the FontIcon to return.
	 * @param fontSize       The fontSize of the icon.
	 * @param color          The color of the icon.
	 * @param cacheIcons     If true, the generated ImageIcons for the fontIcon are cached within this Library by color and fontSize. Should be set to true, if the fontSize and color of the icon is changed often and previous values are re-used to
	 *                           enhance performance.
	 * @return A FontIcon for the given IconIdentifier, with the given parameters. Null is returned, if no icon could be found for the identifier.
	 */
	public static DockFontIcon getScalableIcon(DockFontIconIdentifier iconIdentifier, float fontSize, Color color, boolean cacheIcons) {
		if (!ICON_IDENTIFIER_TO_ICON_IN_FONT.containsKey(iconIdentifier)) {
			return null;
		} else {
			return new DockFontIcon(iconIdentifier, fontSize, color, cacheIcons);
		}
	}

	/**
	 * Returns an ImageIcon for the given iconIdentifier, generated by using a registered custom font, or retrieved from the cache.
	 * For generation of the icon, its unicode and the font name of the registered font, the icon is contained within, are used.
	 * The font name and unicode are hard coded within the static block of this class.
	 * 
	 * @param iconIdentifier Used to retrieve the registered font and unicode to create the icon.
	 * @param fontSize       The fontSize of the icon to create.
	 * @param color          The color of the icon to create.
	 * @param cacheIcon      If true, the returned icon is cached within this class by identifier, fontSize and color for fast retrieval.
	 * @return The generated or cached icon for the parameters. Null is returned, if no icon for the identifier is registered within this class.
	 */
	public static Icon getIcon(DockFontIconIdentifier iconIdentifier, float fontSize, Color color, boolean cacheIcon) {
		if (!ICON_IDENTIFIER_TO_ICON_IN_FONT.containsKey(iconIdentifier)) {
			return null;
		} else {
			FontIconDefinition iconDefinition = ICON_IDENTIFIER_TO_ICON_IN_FONT.get(iconIdentifier);
			IconCode iconCode = new DefaultIconCode(iconDefinition.fontName, iconDefinition.uniCode);
			if (cacheIcon) {
				Integer hashValue = calculateHash(iconIdentifier, fontSize, color);
				return iconCache.computeIfAbsent(hashValue, hash -> IconFontSwing.buildIcon(iconCode, fontSize, color));
			} else {
				return IconFontSwing.buildIcon(iconCode, fontSize, color);
			}

		}
	}

	// helper function to cash icons for an iconIdentifier by fontSize and color.
	private static Integer calculateHash(DockFontIconIdentifier iconIdentifier, float fontSize, Color color) {
		int salt = 13;
		return salt + iconIdentifier.name().hashCode() + Float.floatToIntBits(fontSize) + color.hashCode();
	}

	/**
	 * Helper class that represents the definition of a fontIcon.
	 * 
	 * Contains the fontName of the font the icon is contained within, its unicode and a default color and font size for creation.
	 */
	private static class FontIconDefinition {

		private int		defaultFontSize;
		private Color	defaultColor;
		private String	fontName;
		private char	uniCode;

		private FontIconDefinition(String fontName, char uniCode, int defaultFontSize, Color defaultColor) {
			this.fontName = fontName;
			this.uniCode = uniCode;
			this.defaultFontSize = defaultFontSize;
			this.defaultColor = defaultColor;
		}

	}

	/**
	 * Returns an image based on the given icon specifications.
	 */
	public static Image getImage(DockFontIconIdentifier iconIdentifier, float size, Color color) {
		FontIconDefinition iconDefinition = ICON_IDENTIFIER_TO_ICON_IN_FONT.get(iconIdentifier);
		IconCode iconCode = new DefaultIconCode(iconDefinition.fontName, iconDefinition.uniCode);
		return IconFontSwing.buildImage(iconCode, size, color);
	}

}

