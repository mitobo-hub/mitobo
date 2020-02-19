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

/* 
 * Most recent change(s):
 * 
 * $Rev$
 * $Date$
 * $Author$
 * 
 */

package de.unihalle.informatik.MiToBo.visualization.drawing;

import de.unihalle.informatik.Alida.exceptions.ALDOperatorException;
import de.unihalle.informatik.Alida.exceptions.ALDOperatorException.OperatorExceptionType;
import de.unihalle.informatik.Alida.annotations.ALDAOperator;
import de.unihalle.informatik.Alida.annotations.ALDAOperator.ExecutionMode;
import de.unihalle.informatik.Alida.annotations.Parameter;
import de.unihalle.informatik.Alida.annotations.Parameter.Direction;
import de.unihalle.informatik.Alida.annotations.Parameter.ExpertMode;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBImageHistogram;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage;
import de.unihalle.informatik.MiToBo.core.operator.MTBOperator;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.LineBorder;

/**
 * A class to visualize 2D polygons. 
 * <p>
 * Background is always set to 0, polygons are drawn in red.
 *  
 * @author posch
 */
@SuppressWarnings("deprecation")
@ALDAOperator(genericExecutionMode=ExecutionMode.ALL)
public class MTBPlotHistogram extends MTBOperator {
	
	@Parameter( label= "Image", required = false, 
			direction=Direction.IN, dataIOOrder=1, mode=ExpertMode.STANDARD,
			description = "Image")
	private MTBImage image = null;

	@Parameter( label= "Number of Bins", required = false, 
			direction=Direction.IN, dataIOOrder=1, mode=ExpertMode.STANDARD,
			description = "Number of Bins")
	private Integer numberOfBins = 256;

	@Parameter( label= "Histogram", required = false, 
			direction=Direction.IN, dataIOOrder=3, mode=ExpertMode.STANDARD,
			description = "Input polygons.")
	private MTBImageHistogram histogram = null;

	@Parameter( label= "Mask Image", required = false, 
			direction=Direction.IN, dataIOOrder=1, mode=ExpertMode.STANDARD,
			description = "Maks Imake to indicate invalid pixels (pixels with maskImage <> 0.")
	private MTBImage maskImage = null;


	/**
	 * Default constructor.
	 */
	public MTBPlotHistogram() throws ALDOperatorException {
		// nothing happens here
	}
	
	@Override
	public void validateCustom() throws ALDOperatorException {
		if ( image == null && histogram == null)
			throw new ALDOperatorException(OperatorExceptionType.VALIDATION_FAILED,
					"MTBPlotHistogram: either image of histogram must be supplied.");

		if ( maskImage != null && image != null)
			if ( image.getSizeX() != maskImage.getSizeX() ||
			image.getSizeY() != maskImage.getSizeY() ||
			image.getSizeZ() != maskImage.getSizeZ() ||
			image.getSizeT() != maskImage.getSizeT() ||
			image.getSizeC() != maskImage.getSizeC()  )
				throw new ALDOperatorException(OperatorExceptionType.VALIDATION_FAILED,
						"MTBPlotHistogram: image and mask image must have same size.");
	}
	
	@Override
	protected void operate() { 
		if ( this.histogram == null ) {
			if ( maskImage == null)
				histogram = new MTBImageHistogram(image, numberOfBins);
			else
				histogram = new MTBImageHistogram(image, maskImage, numberOfBins);
		}
		
		HistogramFrame frame = new HistogramFrame( histogram.getData());
		frame.setSize(500, 300);
		frame.setDefaultCloseOperation(3);
		frame.setTitle("PlotHistogram");
		frame.setLocationRelativeTo(null);
		frame.setVisible(true);

	}

	public Dimension getPreferredSize() 	{
		return new Dimension(300, 300);
	}
	
	/**
	 * @return the histogram
	 */
	public MTBImageHistogram getHistogram() {
		return histogram;
	}
	
	
	public class HistogramFrame extends JFrame {
		private Histogram display = new Histogram();

		public HistogramFrame(double[] count) {
			display.showHistogram(count);
			
			JPanel p = new JPanel();
			p.setLayout(new BorderLayout());

			setLayout(new BorderLayout());
			add(p, "South");
			add(this.display, "Center");

		}
	}
	
	/**
	 * @param histogram the histogram to set
	 */
	public void setHistogram(MTBImageHistogram histogram) {
		this.histogram = histogram;
	}	
	
	private class Histogram extends JPanel
	{
		private final int MARGIN = 20;
		private final int LABELHEIGHT = 30;
		private final int LABELWIDTH = 30;
		private final double SCALEBARWIDTH = 0.5;
		
		private double[] count;

		public void showHistogram(double[] count)
		{
			this.count = count;
			repaint();
		}

		protected void paintComponent(Graphics g)
		{
			if (this.count == null) return;

			super.paintComponent(g);

			int width = getWidth();
			int height = getHeight();
			int plotwidth = width - 2 * MARGIN - LABELWIDTH;
			int plotHeight = height - 2 * MARGIN - LABELHEIGHT;
			double interval = ((double)plotwidth / this.count.length);
			int individualWidth = (int)(interval* SCALEBARWIDTH);

			double maxCount = 0;
			for (int i = 0; i < this.count.length; i++) {
				if (maxCount < this.count[i]) {
					maxCount = this.count[i];
				}
			}
			
			double xStart = (MARGIN + LABELWIDTH + 0.5*(interval - individualWidth));
			
			// draw labels
			FontMetrics fm = getFontMetrics(getFont());
			
			String str = String.format( "%.2f", histogram.getBinMidpoint(0));
			int strWidth = fm.stringWidth(str);
			g.drawString( str,(int)xStart-strWidth/2, height-MARGIN);

			str = String.format( "%.2f", histogram.getBinMidpoint(histogram.getData().length-1));
			strWidth = fm.stringWidth(str);
			g.drawString( str, (int)(xStart+interval*count.length-1-strWidth/2), height-MARGIN);
			
			str = String.format( "%d", 0);
			strWidth = fm.stringWidth(str);
			int strHeight = fm.getHeight();
			g.drawString( str, (int)(MARGIN/2.0), (int)(MARGIN + plotHeight - strHeight/2.0));
			
			str = String.format( "%d", (int)maxCount);
			strWidth = fm.stringWidth(str);
			g.drawString( str, (int)(MARGIN/2.0), (int)(MARGIN + strHeight/2.0));
			
			// ... and now the histogram
			g.drawLine(MARGIN+LABELWIDTH, MARGIN + plotHeight, MARGIN+LABELWIDTH+plotwidth, MARGIN + plotHeight);
			g.drawLine(MARGIN+LABELWIDTH, MARGIN + plotHeight, MARGIN+LABELWIDTH, MARGIN );
			for (int i = 0; i < this.count.length; i++)
			{
				int x = (int) (xStart + i*interval);
				int barHeight = (int)((float)this.count[i] / maxCount * plotHeight);
				g.drawRect(x, MARGIN + (plotHeight-barHeight), individualWidth, barHeight);
			}
		}
	}
}
