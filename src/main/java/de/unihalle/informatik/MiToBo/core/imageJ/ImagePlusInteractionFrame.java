/*
 * This file is part of MiToBo, the Microscope Image Analysis Toolbox.
 *
 * Copyright (C) 2010 - 2015
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

package de.unihalle.informatik.MiToBo.core.imageJ;

import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.Point2D;
import java.util.LinkedList;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import de.unihalle.informatik.MiToBo.core.datatypes.images.*;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage.MTBImageType;
import ij.gui.ImageCanvas;

/**
 * Adapter for GUI interactions with ImageJ's {@link ij.ImagePlus}.
 * <p>
 * The adapter displays a copy of the given image to the user and 
 * allows for selecting pixels. As soon as the frame is no longer
 * visible, collected data can be requested via the get methods.
 *
 * @author Birgit Moeller
 */
public class ImagePlusInteractionFrame extends JFrame 
	implements ActionListener, MouseListener {
		
		/**
		 * Associated MiToBo image.
		 */
		MTBImageRGB img;
		
		/**
		 * Canvas used by ImageJ to display the image.
		 */
		ImageCanvas canvas;
		
		/**
		 * List of pixels the user selected.
		 */
		LinkedList<Point2D.Double> clickedPoints = 
				new LinkedList<Point2D.Double>();
		
		/**
		 * Default constructor.
		 * @param image	Image to interact with.
		 */
		public ImagePlusInteractionFrame(MTBImage image) {
			this.setLayout(new GridLayout(2, 1));
			this.add(new JLabel("<html>&nbsp;&nbsp;&nbsp;&nbsp;"
					+ "Please select a set of pixels,<br>"
					+ "&nbsp;&nbsp;&nbsp;&nbsp;" 
					+ "click 'Ok' if you are done.</html>"));
			JPanel buttonPanel = new JPanel();
			JButton b = new JButton("Ok");
			b.addActionListener(this);
			b.setActionCommand("Ok");
			buttonPanel.add(b);
			this.add(buttonPanel);
			this.setSize(250, 150);
			this.setVisible(true);
			if (image.getType() == MTBImageType.MTB_RGB)
				this.img = (MTBImageRGB)image.duplicate();
			else
				this.img = (MTBImageRGB)image.convertType(
						MTBImageType.MTB_RGB, true);
			this.img.setTitle("Please click here...");
			this.img.show();
			this.canvas = this.img.getImagePlus().getCanvas();
			this.canvas.addMouseListener(this);
		}

		/**
		 * Get a reference to the list of clicked points.
		 * @return List of clicked points.
		 */
		public LinkedList<Point2D.Double> getClickedPoints() {
			return this.clickedPoints;
		}
		
		@Override
		public void actionPerformed(ActionEvent e) {
			
			String com = e.getActionCommand();
			if (com == "Ok") {
				this.setVisible(false);
				this.img.close();
			}
			
		}

		@Override
		public void mouseClicked(MouseEvent e) {
			int x = e.getX();
			int y = e.getY();
			this.clickedPoints.add(new Point2D.Double(x, y));
			// draw point in red to image (2 = cross)
			this.img.drawPoint2D(x, y, 0, 0xFF0000, 2);
			this.img.updateAndRepaintWindow();
		}

		@Override
		public void mousePressed(MouseEvent e) {
			// not yet handled		
		}

		@Override
		public void mouseReleased(MouseEvent e) {
			// not yet handled		
		}

		@Override
		public void mouseEntered(MouseEvent e) {
			// not yet handled		
		}

		@Override
		public void mouseExited(MouseEvent e) {
			// not yet handled		
		}
		
	}

