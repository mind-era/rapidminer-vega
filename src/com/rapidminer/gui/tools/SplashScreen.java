/*
 *  RapidMiner
 *
 *  Copyright (C) 2001-2010 by Rapid-I and the contributors
 *
 *  Complete list of developers available at our web site:
 *
 *       http://rapid-i.com
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License
 *  along with this program.  If not, see http://www.gnu.org/licenses/.
 */
package com.rapidminer.gui.tools;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Paint;
import java.awt.image.BufferedImage;
import java.awt.image.RescaleOp;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Properties;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import com.rapidminer.tools.LogService;
import com.rapidminer.tools.Tools;
import com.rapidminer.tools.container.Pair;
import com.rapidminer.tools.plugin.Plugin;

/**
 * The splash screen is displayed during start up of RapidMiner. It displays the logo and the some start information.
 * The product logo should have a size of approximately 270 times 70 pixels.
 * 
 * @author Ingo Mierswa
 */
public class SplashScreen extends JPanel {

	private static final int EXTENSION_GAP = 400;
	private static final float EXTENSION_FADE_TIME = 1000;
	private static final int MAX_NUMBER_EXTENSION_ICONS = 9;

	private static final long serialVersionUID = -1525644776910410809L;

	private static final Paint MAIN_PAINT = Color.BLACK;

	public static Image backgroundImage = null;

	private static final int MARGIN = 10;

	private static final String PROPERTY_FILE = "splash_infos.properties";

	static {
		try {
			if (backgroundImage == null) {
				URL url = Tools.getResource("splashscreen_community.png");
				if (url != null)
					backgroundImage = ImageIO.read(url);
			}
		} catch (IOException e) {
			LogService.getGlobal().logWarning("Cannot load images for splash screen. Using empty splash screen...");
		}
	}

	private transient Image productLogo;

	private Properties properties;

	private JFrame splashScreenFrame = new JFrame();

	private String message = "Starting...";

	private boolean infosVisible;

	private ArrayList<Pair<BufferedImage, Long>> extensionIcons = new ArrayList<Pair<BufferedImage, Long>>();
	private long lastExtensionAdd = 0;

	public SplashScreen(String productVersion, Image productLogo) {
		this(productLogo, createDefaultProperties(productVersion));
	}

	public SplashScreen(String productVersion, Image productLogo, URL propertyFile) {
		this(productLogo, createProperties(productVersion, propertyFile));
	}

	public SplashScreen(Image productLogo, Properties properties) {
		super();

		this.properties = properties;

		this.productLogo = productLogo;

		splashScreenFrame = new JFrame(properties.getProperty("name"));
		splashScreenFrame.getContentPane().add(this);
		SwingTools.setFrameIcon(splashScreenFrame);

		splashScreenFrame.setUndecorated(true);
		if (backgroundImage != null)
			splashScreenFrame.setSize(backgroundImage.getWidth(this), backgroundImage.getHeight(this));
		else
			splashScreenFrame.setSize(450, 350);
		splashScreenFrame.setLocationRelativeTo(null);
	}

	private static Properties createDefaultProperties(String productVersion) {
		return createProperties(productVersion, Tools.getResource(PROPERTY_FILE));
	}

	private static Properties createProperties(String productVersion, URL propertyFile) {
		Properties properties = new Properties();
		if (propertyFile != null) {
			try {
				InputStream in = propertyFile.openStream();
				properties.load(in);
				in.close();
			} catch (Exception e) {
				LogService.getGlobal().logError("Cannot read splash screen infos: " + e.getMessage());
			}
		}
		properties.setProperty("version", productVersion);
		return properties;
	}

	public void showSplashScreen() {
		splashScreenFrame.setVisible(true);
	}

	public JFrame getSplashScreenFrame() {
		return splashScreenFrame;
	}

	public void dispose() {
		splashScreenFrame.dispose();
		splashScreenFrame = null;
	}

	@Override
	public void paint(Graphics g) {
		super.paint(g);
		drawMain((Graphics2D) g);
		g.setColor(Color.black);
		g.drawRect(0, 0, getWidth() - 1, getHeight() - 1);

		// draw extensions
		int size = extensionIcons.size();
		if (size > 0) {
			Graphics2D g2d = (Graphics2D) g;
			g2d.translate(170, 140);
			g2d.scale(0.5, 0.5);
			long currentTimeMillis = System.currentTimeMillis();

			int numberToShow = 0;
			for (Pair<BufferedImage, Long> pair : extensionIcons) {
				if (currentTimeMillis > pair.getSecond())
					numberToShow++;
			}

			// now paint other icons
			int shiftX = 51;
			for (int i = 0; i < numberToShow; i++) {
				RescaleOp rop;
				if (numberToShow > i + MAX_NUMBER_EXTENSION_ICONS) {
					// then we have to fade out again
					Pair<BufferedImage, Long> pair = extensionIcons.get(i + MAX_NUMBER_EXTENSION_ICONS);					
					float min = Math.min((currentTimeMillis - pair.getSecond()) / EXTENSION_FADE_TIME, 1f);
					rop = new RescaleOp(new float[] { 1 - min, 1 - min, 1 - min, 1 - min }, new float[4], null);
				} else {
					// fade in
					Pair<BufferedImage, Long> pair = extensionIcons.get(i);
					float min = Math.min((currentTimeMillis - pair.getSecond()) / EXTENSION_FADE_TIME, 1f);
					rop = new RescaleOp(new float[] { min, min, min, min }, new float[4], null);
				}

				g2d.drawImage(extensionIcons.get(i).getFirst(), rop, (i % MAX_NUMBER_EXTENSION_ICONS) * shiftX, 0);
			}

		}

	}

	public void drawMain(Graphics2D g) {
		g.setPaint(MAIN_PAINT);
		g.fillRect(0, 0, getWidth(), getHeight());

		if (backgroundImage != null)
			g.drawImage(backgroundImage, 0, 0, this);

		if (productLogo != null)
			g.drawImage(productLogo, getWidth() / 2 - productLogo.getWidth(this) / 2, 90, this);

		g.setColor(SwingTools.BROWN_FONT_COLOR);
		if (message != null) {
			g.setFont(new java.awt.Font("SansSerif", java.awt.Font.BOLD, 11));
			drawString(g, message, 255);
		}

		if (infosVisible) {
			g.setFont(new java.awt.Font("SansSerif", java.awt.Font.PLAIN, 10));
			drawString(g, properties.getProperty("name") + " " + properties.getProperty("version"), 275);
			drawString(g, properties.getProperty("license"), 290);
			drawString(g, properties.getProperty("warranty"), 305);
			drawString(g, properties.getProperty("copyright"), 320);
			drawString(g, properties.getProperty("more"), 335);
		}
	}

	private void drawString(Graphics2D g, String text, int height) {
		if (text == null)
			return;
		// Rectangle2D stringBounds = g.getFontMetrics().getStringBounds(text, g);
		// float xPos = (float)(getWidth() - MARGIN - stringBounds.getWidth());
		float xPos = MARGIN;
		float yPos = height;
		g.drawString(text, xPos, yPos);
	}

	public void setMessage(String message) {
		this.message = message;
		repaintLater();
	}

	public void setProperty(String key, String value) {
		properties.setProperty(key, value);
		repaintLater();
	}

	public void setInfosVisible(boolean b) {
		this.infosVisible = b;
		repaintLater();
	}

	public void addExtension(Plugin plugin) {
		ImageIcon extensionIcon = plugin.getExtensionIcon();
		if (extensionIcon != null) {
			long currentTimeMillis = System.currentTimeMillis();
			if (currentTimeMillis < lastExtensionAdd + EXTENSION_GAP)
				currentTimeMillis = lastExtensionAdd + EXTENSION_GAP;
			lastExtensionAdd = currentTimeMillis;

			BufferedImage bufferedImage = new BufferedImage(48, 48, BufferedImage.TYPE_INT_ARGB);
			Graphics2D graphics = (Graphics2D) bufferedImage.getGraphics();
			graphics.drawImage(extensionIcon.getImage(), 0, 0, null);

			extensionIcons.add(new Pair<BufferedImage, Long>(bufferedImage, currentTimeMillis));

			if (extensionIcons.size() == 1)
				new Thread() {
					@Override
					public void run() {
						while (splashScreenFrame != null) {
							repaint();
							synchronized (this) {
								try {
									wait(10);
								} catch (InterruptedException e) {
								}
							}
						}
					};
				}.start();
		}
	}

	private void repaintLater() {
		if (SwingUtilities.isEventDispatchThread()) {
			repaint();
		} else {
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					repaint();
				}
			});
		}
	}
}
