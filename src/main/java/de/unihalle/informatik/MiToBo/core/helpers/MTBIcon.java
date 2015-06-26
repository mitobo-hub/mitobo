/*
 * This file is part of MiToBo, the Microscope Image Analysis Toolbox.
 *
 * Copyright (C) 2010 - @YEAR@
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Fore more information on MiToBo, visit
 *
 *    http://www.informatik.uni-halle.de/mitobo/
 *
 */

package de.unihalle.informatik.MiToBo.core.helpers;

import java.awt.Graphics;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;

/**
 * Singleton class to provide access to MiToBo icon in graphical environments.
 * 
 * @author moeller
 */
public class MTBIcon {

	/**
	 * Singleton reference.
	 */
	private static MTBIcon instance = null;
	
	/**
	 * The icon by itself.
	 */
	private ImageIcon mitoboIcon;

	/**
	 * Get singleton instance.
	 * @return	Singleton instance.
	 */
	public static MTBIcon getInstance() {
		if (instance == null) {
			instance = new MTBIcon();
		}
		return instance;
	}
	
	/**
	 * Get reference to the icon.
	 * @return	The MiToBo icon.
	 */
	public ImageIcon getIcon() {
		return this.mitoboIcon;
	}
	
	/**
	 * Default constructor.
	 */
	protected MTBIcon() {

		// initialize the icon
		String iconDataName = "/share/logo/MiToBo_logo.png";
		Image img = null;
		BufferedImage bi = null;
		Graphics g = null;
		InputStream is = null;
		try {
			ImageIcon icon;
			File iconDataFile = new File("./" + iconDataName);
			if(iconDataFile.exists()) {
				icon = new ImageIcon("./" + iconDataName);
				img = icon.getImage();
			}
			// try to find it inside a jar archive....
			else {
				is = MTBIcon.class.getResourceAsStream(iconDataName);
				if (is == null) {
					System.err.println("Warning - cannot find icons...");
					img = new BufferedImage(20,20,BufferedImage.TYPE_INT_ARGB);
				}
				else {
					img = ImageIO.read(is);
				}
				bi= new BufferedImage(20,20,BufferedImage.TYPE_INT_ARGB);
				g = bi.createGraphics();
				g.drawImage(img, 0, 0, 20, 20, null);
			}
		} catch (IOException ex) {
			System.err.println("MTBIcon - problems loading icons...!");
			img = new BufferedImage(20,20,BufferedImage.TYPE_INT_ARGB);
			bi= new BufferedImage(20,20,BufferedImage.TYPE_INT_ARGB);
			g = bi.createGraphics();
			g.drawImage(img, 0, 0, 20, 20, null);
		}
		this.mitoboIcon = new ImageIcon(img);
	}
}