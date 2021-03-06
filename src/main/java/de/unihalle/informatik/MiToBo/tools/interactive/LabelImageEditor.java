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

package de.unihalle.informatik.MiToBo.tools.interactive;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.Point2D;
import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.TreeSet;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;

import de.unihalle.informatik.Alida.annotations.ALDAOperator;
import de.unihalle.informatik.Alida.annotations.Parameter;
import de.unihalle.informatik.Alida.annotations.Parameter.ParameterModificationMode;
import de.unihalle.informatik.Alida.annotations.ALDAOperator.Level;
import de.unihalle.informatik.Alida.datatypes.ALDDirectoryString;
import de.unihalle.informatik.Alida.exceptions.ALDException;
import de.unihalle.informatik.Alida.exceptions.ALDOperatorException;
import de.unihalle.informatik.Alida.operator.ALDOperator;
import de.unihalle.informatik.Alida.operator.events.ALDOperatorExecutionProgressEvent;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBRegion2D;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage.MTBImageType;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImageShort;
import de.unihalle.informatik.MiToBo.core.operator.MTBOperator;
import de.unihalle.informatik.MiToBo.io.dirs.DirectoryTree;
import de.unihalle.informatik.MiToBo.io.images.ImageReaderMTB;
import de.unihalle.informatik.MiToBo.io.images.ImageWriterMTB;
import de.unihalle.informatik.MiToBo.morphology.BasicMorphology;
import de.unihalle.informatik.MiToBo.morphology.BasicMorphology.opMode;
import de.unihalle.informatik.MiToBo.segmentation.regions.labeling.LabelComponentsSequential;
import ij.IJ;
import ij.ImagePlus;
import ij.gui.ImageCanvas;
import ij.process.ImageProcessor;

/**
 * A small interactive tool for editing label images by mouse-clicks.
 * <p>
 * The editor supports removing regions by one-click operations. Images can 
 * be relabeled and open contour branches be removed. Also holes within 
 * regions, i.e., background sections and small regions completely surrounded
 * by another region, can be eliminated.<br>
 * Short gaps in boundaries can be closed by drawing free-hand lines 
 * by mouse.<br> 
 * To enhance the display of an image the contrast of region labels can be 
 * improved by shifting dark labels into a higher interval of gray-scale 
 * values increasing the difference (contrast) to the black background. 
 * 
 * @author Birgit Moeller
 */
@ALDAOperator(genericExecutionMode=ALDAOperator.ExecutionMode.ALL, 
	level=Level.APPLICATION, allowBatchMode = false)
public class LabelImageEditor extends MTBOperator 
		implements MouseListener, MouseMotionListener, ActionListener {

	/**
	 * Class identifier.
	 */
	private static final String classID = "[LabelImageEditor]";

	/**
	 * Maximal border width default value.
	 */
	private static final int defaultMaxBorderWidth = 7;

	/**
	 * Default width of freehand border lines.
	 */
	private static final int defaultDrawLineWidth = 3;

	/**
	 * Image directory to process.
	 */
	@Parameter(label = "Input Directory", required = true, 
		direction = Parameter.Direction.IN, description = "Input directory.",
		dataIOOrder = 0, callback = "callbackInputDir",
		paramModificationMode = ParameterModificationMode.MODIFIES_VALUES_ONLY)
	private ALDDirectoryString inputDir = null;

	/**
	 * Optional file filter.
	 */
	@Parameter(label = "File Filter", required = false, 
		direction = Parameter.Direction.IN, 
		description = "Regular expression to filter input files.",
		dataIOOrder = 0)
	private String inputRegExp = null;

	/**
	 * Optional output directory.
	 * <p>
	 * If left unset results are saved to input directory.
	 */
	@Parameter(label = "Output Directory", required = false, 
		direction = Parameter.Direction.IN, description = "Output directory.",
		dataIOOrder = 1)
	private ALDDirectoryString outputDir = null;

	/**
	 * Optional output file suffix.
	 */
	@Parameter(label = "Output file ending", required = false, 
		direction = Parameter.Direction.IN, description = "Output file ending.",
		dataIOOrder = 2)
	private String outFileEnding = "-edited.tif";

	/**
	 * Main window frame.
	 */
	private JFrame mainFrame;
	
	/**
	 * Image canvas for displaying image and interacting.
	 */
	private ImageCanvas ic;
	
	/**
	 * Original image currently under processing.
	 */
	private MTBImage originalImage;

	/**
	 * Image currently under processing.
	 */
	private MTBImageShort activeImage;
	
	/**
	 * Reference to currently active image.
	 */
	private ImagePlus activePlus = null;
	
	/**
	 * Reference to processor of currently active image 
	 * {@link LabelImageEditor#activePlus}.
	 */
	private ImageProcessor activeProcessor;
	
	/**
	 * Reference to last processor before last action. 
	 * {@link LabelImageEditor#activePlus}.
	 */
	private ImageProcessor lastProcessor;

	/**
	 * Flag to indicate if process is still active.
	 */
	private boolean stillActive = true;
	
	/**
	 * Full path name of currently active image file.
	 */
	private String currentFile;
	
	/**
	 * Flag to indicate if operations are finished.
	 */
	private boolean finished = false;
	
	/**
	 * Internal variable of output dir (to avoid modifying parameter).
	 */
	private String internalOutputDir;

	/**
	 * Flag to ensure that callback only works once the operator has been called.
	 */
	private boolean calledFirstTime = true;
	
	/**
	 * In memory mode labels can be transferred from one region to another.
	 */
	private boolean memoryMode = false;
	
	/**
	 * Stores the last clicked label for transfer in memory mode.
	 */
	private int lastLabel;
	
	/**
	 * Local flag to indicate if we are currently tracing a path 
	 * for a new piece of boundary.
	 */
	private boolean scanPath = false;
	
	/**
	 * Local helper variable in drawing new boundaries, 
	 * stores last point of current path.
	 */
	private Point2D.Double lastPathPoint;
	
	/**
	 * Window for setting some options and configuration parameters.
	 */
	private JFrame optionsFrame;
	
	/**
	 * Text field with current maximal border width.
	 */
	private JTextField optionsBorderMaxWidth;
	
	/**
	 * Text field with current width of lines when drawing boundaries.
	 */
	private JTextField optionsDrawLineWidth;
	
	/**
	 * Button to select black pen color in drawing.
	 */
	private JRadioButton buttonBlack; 

	/**
	 * Button to select white pen color in drawing.
	 */
	private JRadioButton buttonWhite; 

	/**
	 * Default constructor.
	 * @throws ALDOperatorException Thrown if construction fails.
	 */
	public LabelImageEditor() throws ALDOperatorException {
		// nothing to do here
	}
	
	/* (non-Javadoc)
	 * @see de.unihalle.informatik.Alida.operator.ALDOperator#operate()
	 */
	@Override
	protected void operate() 
			throws ALDOperatorException {

		this.fireOperatorExecutionProgressEvent(
				new ALDOperatorExecutionProgressEvent(this, classID 
					+ "... editor is ready to go!"));

		// (re-)init operator
		this.activePlus = null;
		this.activeProcessor = null;
		this.stillActive = true;
		this.finished = false;
		
		// deactivate history mode
		ALDOperator.setConstructionMode(
				ALDOperator.HistoryConstructionMode.NO_HISTORY);

		// read input directory
		DirectoryTree tree = 
			new DirectoryTree(this.inputDir.getDirectoryName(), true);
		
		// check output directory
		if (this.outputDir == null)
			this.internalOutputDir = this.inputDir.getDirectoryName();
		else
			this.internalOutputDir = this.outputDir.getDirectoryName();
			
		
		// init file reader and iterate over all files
		ImageReaderMTB reader = new ImageReaderMTB();
		Vector<String> files = tree.getFileList();
		Pattern p = null;
		if (this.inputRegExp != null && !this.inputRegExp.isEmpty())
			p = Pattern.compile(this.inputRegExp);
		for (String f: files) {

			// skip Alida history files
			if (f.endsWith(".ald"))
				continue;

			if (this.verbose.booleanValue())
				System.out.println("Processing image " + f + "...");
			this.fireOperatorExecutionProgressEvent(
					new ALDOperatorExecutionProgressEvent(this, classID 
						+ " processing image " + f + "..."));
			
			// if file name does not match filter, skip it
			if (p != null) {
				Matcher m = p.matcher(f);
				if (!m.find())
					continue;
			}
				
			try {
				
				// read input image
				reader.setFileName(f);
				reader.runOp();
				this.originalImage = reader.getResultMTBImage();
				this.activeImage = (MTBImageShort)this.originalImage.convertType(
								MTBImageType.MTB_SHORT, true);
				
				// init frame with first image
				if (this.activePlus == null) {
					this.initMainFrame();
				}
				
				// init the options window
				this.initOptionsFrame();
				
				this.mainFrame.setTitle(this.activeImage.getTitle());
				this.activeProcessor = this.activeImage.getImagePlus().getProcessor();
				this.currentFile = f;
				
				this.activePlus.setProcessor(this.activeProcessor);
				this.ic.setImageUpdated();
				this.ic.repaint();

				this.stillActive = true;
				while(this.stillActive) {
					Thread.sleep(100);
				}
				if (this.finished) {
					return;
				}
			} catch (Exception e) {
				JOptionPane.showMessageDialog(this.mainFrame,
					"Problems processing image " + f + "... Skipping!\n\nDetails: \n" 
						+ e.getMessage(), "ImageLabelEditor Warning",
				    	JOptionPane.ERROR_MESSAGE);
			}
		}
		if (this.mainFrame != null) {
			this.mainFrame.setVisible(false);
			this.mainFrame.dispose();
		}
		this.finished = true;
	}

	/* (non-Javadoc)
	 * @see java.awt.event.MouseListener#mouseClicked(java.awt.event.MouseEvent)
	 */
	@Override
	public void mouseClicked(MouseEvent e) {
		
		// get mouse position in image 
		int mx = this.ic.offScreenX(e.getX());
		int my = this.ic.offScreenY(e.getY());
		
		// get label at click position
		int label = this.activeProcessor.getPixel(mx, my);
		
		int onmask = InputEvent.META_DOWN_MASK;
    if ((e.getModifiersEx() & onmask) == onmask) {
  		this.fireOperatorExecutionProgressEvent(
  				new ALDOperatorExecutionProgressEvent(this, classID 
  					+ " -> registered mouse-click, meta key is down!"));
    }
		
    boolean fillInRegion = false;
    if (label == 0 && IJ.shiftKeyDown() && IJ.controlKeyDown()) {
  		this.fireOperatorExecutionProgressEvent(
  				new ALDOperatorExecutionProgressEvent(this, classID 
  					+ " -> registered mouse-click, shift and control keys down, " 
  						+ "preparing to fill region..."));
			fillInRegion = true;
		}
    
		// ignore clicks to the background except in memory mode
		// => filling in background regions is allowed
		if ( label == 0 && !( this.memoryMode || fillInRegion ))
			return;
		
		if (IJ.shiftKeyDown() && !IJ.controlKeyDown()) {
  		this.fireOperatorExecutionProgressEvent(
  				new ALDOperatorExecutionProgressEvent(this, classID 
  					+ " -> registered mouse-click, shift key down, control key up, " 
  						+ "preparing to join regions..."));
			this.memoryMode = true;
			this.lastLabel = label;
			return;
		}

		boolean newLabel = false;
		if (IJ.controlKeyDown() && !IJ.shiftKeyDown()) {
  		this.fireOperatorExecutionProgressEvent(
  				new ALDOperatorExecutionProgressEvent(this, classID 
  					+ " -> registered mouse-click, control key down, shift key up, " 
  						+ "preparing to assign new label..."));
			newLabel = true;
		}
		
		// set all pixels with identical label to background
		this.lastProcessor = this.activeProcessor.duplicate();
		if (this.memoryMode) {
			for (int y=0; y<this.activeProcessor.getHeight();++y) {
				for (int x=0; x<this.activeProcessor.getWidth();++x) {
					if (this.activeProcessor.getPixel(x, y) == label) {
						this.activeProcessor.putPixel(x, y, this.lastLabel);
					}
				}
			}
			this.memoryMode = false;
		}
		else if (newLabel) {
			int clabel;
			int maxLabel = 0;
			TreeSet<Integer> labels = new TreeSet<>();
			for (int y=0; y<this.activeProcessor.getHeight();++y) {
				for (int x=0; x<this.activeProcessor.getWidth();++x) {
					clabel = this.activeProcessor.getPixel(x, y);
					labels.add(Integer.valueOf(clabel));
					if (clabel > maxLabel)
						maxLabel = clabel;
				}
			}
			for (int nl = maxLabel; nl > 0; --nl) {
				if (labels.contains(Integer.valueOf(nl)))
					continue;
				for (int y=0; y<this.activeProcessor.getHeight();++y) {
					for (int x=0; x<this.activeProcessor.getWidth();++x) {
						if (this.activeProcessor.getPixel(x, y) == label) {
							this.activeProcessor.putPixel(x, y, nl);
						}
					}
				}				
			}
			newLabel = false;
		}
		// fill-in the clicked (background) region
		else if (fillInRegion) {

			// determine new label to assign
			int clabel;
			int maxLabel = 0;
			TreeSet<Integer> labels = new TreeSet<>();
			for (int y=0; y<this.activeProcessor.getHeight();++y) {
				for (int x=0; x<this.activeProcessor.getWidth();++x) {
					clabel = this.activeProcessor.getPixel(x, y);
					labels.add(Integer.valueOf(clabel));
					if (clabel > maxLabel)
						maxLabel = clabel;
				}
			}
			// search for next free label to assign
			for (int nl = maxLabel; nl > 0; --nl) {
				if (labels.contains(Integer.valueOf(nl)))
					continue;
				labelNeighbors(this.activeProcessor, mx, my, nl);
			}
		}
		// remove the clicked region
		else {
			for (int y=0; y<this.activeProcessor.getHeight();++y) {
				for (int x=0; x<this.activeProcessor.getWidth();++x) {
					if (this.activeProcessor.getPixel(x, y) == label) {
						this.activeProcessor.putPixel(x, y, 0);
					}
				}
			}
		}
		this.activePlus.setProcessor(this.activeProcessor);
		this.activePlus.updateAndDraw();
		this.ic.setImageUpdated();
		this.ic.repaint();
	}

	/**
	 * Recursively label neighbors until approaching next region. 
	 * 
	 * @param ip		Image processor to modify.
	 * @param px		Position in x where to start.
	 * @param py		Position in y where to start.
	 * @param label	Label to assign to new region.
	 */
	private static void labelNeighbors(ImageProcessor ip, 
			int px, int py, int label) {
		
		int borderWidth = 3;
		
		// if pixel is already labeled, it is not of interest for us
		if (ip.getPixel(px, py) != 0)
			return;
		
		int nx = -1, ny = -1;
		// check all neighbors
		for (int dy=-borderWidth; dy<=borderWidth; ++dy) {
			for (int dx=-borderWidth; dx<=borderWidth; ++dx) {
				nx = px+dx;
				ny = py+dy;
				if (nx<0 || nx>=ip.getWidth() || ny<0 || ny>=ip.getHeight()) 
					continue;
				// if we have foreign neighbors, cancel further processing
				if (ip.getPixel(nx, ny) != 0 && ip.getPixel(nx, ny) != label) {
					return;
				}
			}
		}
		// there are no foreign neighbors => label pixel
		ip.putPixel(px, py, label);
		for (int dy=-borderWidth; dy<=borderWidth; ++dy) {
			for (int dx=-borderWidth; dx<=borderWidth; ++dx) {
				// skip pixel itself
				if (dx==0 && dy==0)
					continue;
				nx = px+dx;
				ny = py+dy;
				if (nx<0 || nx>=ip.getWidth() || ny<0 || ny>=ip.getHeight()) 
					continue;
				// continue recursively
				labelNeighbors(ip, nx, ny, label);
			}
		}
	}
	
	@Override
	public void mousePressed(MouseEvent e) {
		
		// back-up for undo function
		this.lastProcessor = this.activeProcessor.duplicate();

		// get mouse position in image 
		int mx = this.ic.offScreenX(e.getX());
		int my = this.ic.offScreenY(e.getY());

		this.scanPath = true;
		this.lastPathPoint = new Point2D.Double(mx, my);
	
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		this.scanPath = false;
	}

	@Override
	public void mouseEntered(MouseEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mouseExited(MouseEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mouseDragged(MouseEvent e) {
		if (this.scanPath) {
			
			this.activeProcessor.setColor(Color.BLACK);

			// get mouse position in image 
			int mx = this.ic.offScreenX(e.getX());
			int my = this.ic.offScreenY(e.getY());
			
			// get line width from options window
			int lineWidth = defaultDrawLineWidth;
			try {
				int w = Integer.parseInt(this.optionsDrawLineWidth.getText());
				lineWidth = w;
			}
			catch (Exception exp) {
				// if there is any problem, just ignore...
			}
			this.activeProcessor.setLineWidth(lineWidth);
			// set pen color for drawing
			if (this.buttonBlack.isSelected())
				this.activeProcessor.setColor(Color.BLACK);
			else
				this.activeProcessor.setColor(Color.WHITE);
			this.activeProcessor.drawLine(
					(int)this.lastPathPoint.x, (int)this.lastPathPoint.y, mx, my);
			this.lastPathPoint.x = mx;
			this.lastPathPoint.y = my;
			
			this.activePlus.setProcessor(this.activeProcessor);
			this.activePlus.updateAndDraw();
			this.ic.setImageUpdated();
			this.ic.repaint();

		}
	}

	@Override
	public void mouseMoved(MouseEvent e) {
		// TODO Auto-generated method stub
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		
		String c = e.getActionCommand();
		
		// save the current processing result and proceed
		if (c.equals("next")) {
			this.saveFile();
			this.stillActive = false; 
		}
		// proceed without doing any changes to the current image
		else if (c.equals("skip")) {
			this.stillActive = false; 
		}
		// optimize contrast
		else if (c.equals("contrast")) {
			
			this.lastProcessor = this.activeProcessor.duplicate();

			this.optimizeContrast(this.activeProcessor);

			this.activePlus.setProcessor(this.activeProcessor);
			this.activePlus.updateAndDraw();
			this.ic.setImageUpdated();
			this.ic.repaint();
		}
		// delete obsolete boundaries
		else if (c.equals("boundaries")) {
			
			this.lastProcessor = this.activeProcessor.duplicate();
			
			int nLabel;
			TreeSet<Integer> labels = new TreeSet<>();

			// run over the image and search for background pixels having two or 
			// more components with the same label in their neighborhood
			int neighborhoodSize = (int)(defaultMaxBorderWidth/2.0);
			try {
				int maxWidth = Integer.parseInt(this.optionsBorderMaxWidth.getText());
				neighborhoodSize = (int)(maxWidth/2.0);
			}
			catch (Exception exp) {
				// if there is any problem, just ignore...
			}
			int neighborhoodPixelNum = (2*neighborhoodSize + 1) * (2*neighborhoodSize + 1);
			for (int y=0; y<this.activeProcessor.getHeight();++y) {
				for (int x=0; x<this.activeProcessor.getWidth();++x) {
					int label = this.activeProcessor.getPixel(x, y);
					if (label != 0)
						continue;

					labels.clear();
					for (int dy = -neighborhoodSize; dy <= neighborhoodSize; ++dy) {
						for (int dx = -neighborhoodSize; dx <= neighborhoodSize; ++dx) {
							if (dx == 0 && dy == 0)
								continue;
							if (   x+dx >= 0 && x+dx < this.activeImage.getSizeX()
									&& y+dy >= 0 && y+dy < this.activeImage.getSizeY()) {
								nLabel = this.activeProcessor.getPixel(x+dx,  y+dy);
								if (nLabel == 0)
									continue;
								labels.add(Integer.valueOf(nLabel));
							}						
						}
					}
					if (labels.size() == 1) {
						int count = 0;
						label = labels.first().intValue();
						for (int dy = -neighborhoodSize; dy <= neighborhoodSize; ++dy) {
							for (int dx = -neighborhoodSize; dx <= neighborhoodSize; ++dx) {
								if (dx == 0 && dy == 0)
									continue;
								if (   x+dx >= 0 && x+dx < this.activeImage.getSizeX()
										&& y+dy >= 0 && y+dy < this.activeImage.getSizeY()) {
									if (this.activeProcessor.getPixel(x+dx,  y+dy) == label)
										++count;
								}
							}
						}
						if (count > neighborhoodPixelNum/2) 
							this.activeProcessor.putPixel(x, y, label);
					}
				}
			}
			this.activePlus.setProcessor(this.activeProcessor);
			this.activePlus.updateAndDraw();
			this.ic.setImageUpdated();
			this.ic.repaint();
		}
		// relabel image
		else if (c.equals("relabel")) {
			
			MTBImageShort binImage = 
					(MTBImageShort)this.activeImage.duplicate().convertType(
							MTBImageType.MTB_SHORT, true);
			binImage.fillBlack();
			for (int y=0; y<this.activeProcessor.getHeight();++y) {
				for (int x=0; x<this.activeProcessor.getWidth();++x) {
					if (this.activeProcessor.getPixel(x, y) > 0)
						binImage.putValueInt(x, y, 255);
				}
			}
			try {
				LabelComponentsSequential lop = 
						new LabelComponentsSequential(binImage, false);
				lop.runOp();
				
				// back-up old image version before doing modifications
				this.lastProcessor = this.activeProcessor.duplicate();

				for (int y=0; y<this.activeProcessor.getHeight();++y) {
					for (int x=0; x<this.activeProcessor.getWidth();++x) {
						this.activeProcessor.putPixel(x, y, 
							lop.getLabelImage().getValueInt(x, y));
					}
				}
				
				this.optimizeContrast(this.activeProcessor);
				
				this.activePlus.setProcessor(this.activeProcessor);
				this.activePlus.updateAndDraw();
				this.ic.setImageUpdated();
				this.ic.repaint();
			} catch (Exception e1) {
				// just ignore
				e1.printStackTrace();
				return;
			}
		}
		// fill holes in regions
		else if (c.equals("holes")) {
			try {
				// back-up old image version before doing modifications
				this.lastProcessor = this.activeProcessor.duplicate();

				int height = this.activeProcessor.getHeight();
				int width = this.activeProcessor.getWidth();
				
				// background image with boundaries and holes in white
				MTBImageShort backImage = 
					(MTBImageShort)this.activeImage.duplicate().convertType(
							MTBImageType.MTB_SHORT, true);
				backImage.fillBlack();
				for (int y=0; y<height;++y) {
					for (int x=0; x<width;++x) {
						if (this.activeProcessor.getPixel(x, y) == 0)
							backImage.putValueInt(x, y, 255);
					}
				}

				// eliminate one-pixel wide boundaries -> holes remaining
				BasicMorphology bm = new BasicMorphology();
				bm.setInImg(backImage);
				bm.setMask(BasicMorphology.maskShape.CIRCLE, 1);
				bm.setMode(opMode.ERODE);
				bm.runOp();
				backImage = (MTBImageShort)bm.getResultImage();
				bm.setInImg(backImage);
				bm.setMask(BasicMorphology.maskShape.CIRCLE, 1);
				bm.setMode(opMode.DILATE);
				bm.runOp();
				backImage = (MTBImageShort)bm.getResultImage();
				
				// label hole regions
				LabelComponentsSequential lop = 
						new LabelComponentsSequential(backImage, false);
				lop.runOp();
				
				// check the labels in the neighborhood of each region
				// => if only one, than region is obviously a hole
				int x, y, val;
				TreeSet<Integer> neighborLabels = new TreeSet<Integer>();
				for (MTBRegion2D reg: lop.getResultingRegions()) {
					neighborLabels.clear();
					for (Point2D.Double p: reg.getPoints()) {
						for (int dy = -2; dy <= 2; ++dy) {
							for (int dx = -2; dx <= 2; ++dx) {
								x = (int)(p.x+dx);
								y = (int)(p.y+dy);
								if (x >= 0 && x < width && y>= 0 && y < height) {
									if (this.activeProcessor.getPixelValue(x, y) > 0)
										neighborLabels.add(Integer.valueOf(
											(int)this.activeProcessor.getPixelValue(x, y)));
								}
							}							
						}
					}
					if (neighborLabels.size() == 1) {
						// label of surrounding region
						val = neighborLabels.first().intValue();
						// fill hole region
						for (Point2D.Double p: reg.getPoints()) {
							this.activeProcessor.putPixel((int)p.x, (int)p.y, val);
						}					
					}
				}				
				this.activePlus.setProcessor(this.activeProcessor);
				this.activePlus.updateAndDraw();
				this.ic.setImageUpdated();
				this.ic.repaint();
			} catch (Exception e1) {
				// just ignore
				e1.printStackTrace();
				return;
			}
		}
		// undo last operation
		else if (c.equals("undo")) {
			this.activeProcessor = this.lastProcessor;
			this.activePlus.setProcessor(this.lastProcessor);
			this.activePlus.updateAndDraw();
			this.ic.setImageUpdated();
			this.ic.repaint();
		}
		// save the current processing result and close the frame
		else if (c.equals("quit")) {
			this.saveFile();
			this.stillActive = false;
			this.optionsFrame.setVisible(false);
			this.optionsFrame.dispose();
			this.mainFrame.setVisible(false);
			this.mainFrame.dispose();
			this.finished = true;
		}
		// open window with options
		else if (c.equals("options")) {
			this.optionsFrame.setVisible(true);
		}
		// open window with options
		else if (c.equals("optionsOk")) {
			this.optionsFrame.setVisible(false);
		}
	}
	
	/**
	 * Initialize the main window on opening the first image.
	 */
	private void initMainFrame() {
		
		// main window
		this.mainFrame = new JFrame();
		this.mainFrame.setLayout(new BorderLayout());
		
		// buttons
		JPanel buttons = new JPanel();
		JButton next = new JButton("Next");
		next.addActionListener(this);
		next.setToolTipText("Save changes and proceed with next image.");
		next.setActionCommand("next");
		next.setMnemonic(KeyEvent.VK_N);
		buttons.add(next);
		JButton skip = new JButton("Skip");
		skip.addActionListener(this);
		skip.setToolTipText("Ignore all changes and skip image.");
		skip.setActionCommand("skip");
		skip.setMnemonic(KeyEvent.VK_S);
		buttons.add(skip);
		JButton contrast = new JButton("Contrast");
		contrast.addActionListener(this);
		contrast.setToolTipText("Improve visibility by shifting labels " 
			+ "to brighter values.");
		contrast.setActionCommand("contrast");
		contrast.setMnemonic(KeyEvent.VK_C);
		buttons.add(contrast);
		JButton relabel = new JButton("Relabel");
		relabel.addActionListener(this);
		relabel.setToolTipText("Relabel the image, unify labels.");
		relabel.setActionCommand("relabel");
		relabel.setMnemonic(KeyEvent.VK_R);
		buttons.add(relabel);
		JButton boundaries = new JButton("Fix Borders");
		boundaries.addActionListener(this);
		boundaries.setToolTipText("Remove open boundary branches and " 
			+ "obsolete pieces of boundaries inside regions.");
		boundaries.setActionCommand("boundaries");
		boundaries.setMnemonic(KeyEvent.VK_B);
		buttons.add(boundaries);
		JButton holes = new JButton("Fill Holes");
		holes.addActionListener(this);
		holes.setToolTipText("Fill holes inside regions.");
		holes.setActionCommand("holes");
		holes.setMnemonic(KeyEvent.VK_H);
		buttons.add(holes);
		JButton undo = new JButton("Undo");
		undo.addActionListener(this);
		undo.setToolTipText("Undo last action.");
		undo.setActionCommand("undo");
		undo.setMnemonic(KeyEvent.VK_U);
		buttons.add(undo);
		JButton options = new JButton("Options");
		options.addActionListener(this);
		options.setToolTipText("Configuration options.");
		options.setActionCommand("options");
		options.setMnemonic(KeyEvent.VK_O);
		buttons.add(options);
		JButton quit = new JButton("Quit");
		quit.addActionListener(this);
		quit.setToolTipText("Save current image and quit the editor.");
		quit.setActionCommand("quit");
		quit.setMnemonic(KeyEvent.VK_Q);
		buttons.add(quit);
		this.mainFrame.add(buttons,BorderLayout.NORTH);
		JPanel canvas = new JPanel();
		
		this.activePlus = this.activeImage.getImagePlus();
		this.activePlus.show();
		this.ic = this.activePlus.getCanvas();
		this.activePlus.hide();
		this.ic.addMouseListener(this);
		this.ic.addMouseMotionListener(this);
		
		canvas.add(this.ic);
		this.mainFrame.add(canvas, BorderLayout.CENTER);
		this.mainFrame.setSize(1200, 1200);
		this.mainFrame.setVisible(true);
		
	}
	
	/**
	 * Initialize the options window.
	 */
	private void initOptionsFrame() {
		
		// main window
		this.optionsFrame = new JFrame();
		this.optionsFrame.setTitle("LabelImageEditor - Options");
		this.optionsFrame.setLayout(new BorderLayout());
		
		JPanel paramPanel = new JPanel();
		paramPanel.setLayout(new GridLayout(3,2));

		// border max width for configuring neighborhood size in fix borders
		JPanel labelPanel = new JPanel();
		JLabel label = new JLabel("Maximal border width:");
		label.setToolTipText("Maximal width of boundaries, used in fixing boundaries.");
		labelPanel.add(label);
		paramPanel.add(labelPanel);
		this.optionsBorderMaxWidth = new JTextField(String.valueOf(defaultMaxBorderWidth));
		paramPanel.add(this.optionsBorderMaxWidth);
		// width of line when drawing new borders
		labelPanel = new JPanel();
		label = new JLabel("Drawing width:");
		label.setToolTipText("Line width for drawing new freehand boundaries.");
		labelPanel.add(label);
		paramPanel.add(labelPanel);
		this.optionsDrawLineWidth = new JTextField(String.valueOf(defaultDrawLineWidth));
		paramPanel.add(this.optionsDrawLineWidth);
		// line color
		labelPanel = new JPanel();
		label = new JLabel("Pen color:");
		label.setToolTipText("Color for drawing (black for boundaries, white for cell outlines).");
		labelPanel.add(label);
		paramPanel.add(labelPanel);
		JPanel optionsDrawColor = new JPanel();
		ButtonGroup colors = new ButtonGroup();
		this.buttonBlack = new JRadioButton("black");
		this.buttonBlack.setSelected(true);
		colors.add(this.buttonBlack);
		optionsDrawColor.add(this.buttonBlack);
		this.buttonWhite = new JRadioButton("white");
		colors.add(this.buttonWhite);
		optionsDrawColor.add(this.buttonWhite);
		paramPanel.add(optionsDrawColor);
		
		this.optionsFrame.add(paramPanel, BorderLayout.CENTER);
		
		JPanel buttons = new JPanel();
		JButton optionsOk = new JButton("Ok");
		optionsOk.addActionListener(this);
		optionsOk.setToolTipText("Close the options window.");
		optionsOk.setActionCommand("optionsOk");
		buttons.add(optionsOk);
		this.optionsFrame.add(buttons, BorderLayout.SOUTH);
		
		this.optionsFrame.setSize(350, 150);
	}
	
	/**
	 * Optimize visibility by shifting labels into 
	 * upper part of gray-scale range.
	 * 
	 * @param ip	Image processor to process.
	 */
	private void optimizeContrast(ImageProcessor ip) {

		// count labels
		TreeSet<Integer> labels = new TreeSet<>();
		for (int y=0; y<ip.getHeight();++y) {
			for (int x=0; x<ip.getWidth();++x) {
				labels.add(Integer.valueOf(ip.getPixel(x, y)));
			}
		}
		LinkedList<Integer> sortedLabels = new LinkedList<>();
		for (Integer i: labels) {
			sortedLabels.add(i);
		}
		Collections.sort(sortedLabels);
		// remove the background
		sortedLabels.removeFirst();
		Collections.reverse(sortedLabels);
		
		HashMap<Integer, Integer> labelMap = new HashMap<>();
		
		// check range of available intensity values
		int max = (int)this.activeImage.getTypeMax();
		int min = (int)(max * 1.0 / 3.0);			
		int step = (max - min) / labels.size();

		int n = 0, newLabel;
		for (Integer i: sortedLabels) {
			newLabel = max - n * step;
			labelMap.put(i, Integer.valueOf(newLabel));
			++n;
		}
		
		for (int y=0; y<ip.getHeight();++y) {
			for (int x=0; x<ip.getWidth();++x) {
				int label = ip.getPixel(x, y);
				if (label == 0)
					continue;
				newLabel = labelMap.get(Integer.valueOf(label)).intValue();
				ip.putPixel(x, y, newLabel);
			}
		}
	}
	
	/**
	 * Saves the current processing result.
	 */
	private void saveFile() {
		MTBImage newLabelImage = this.originalImage.duplicate();
		newLabelImage.fillBlack();
		for (int y=0; y<newLabelImage.getSizeY(); ++y) {
			for (int x=0; x<newLabelImage.getSizeX(); ++x) {
				newLabelImage.putValueInt(x, y, this.activePlus.getProcessor().getPixel(x, y));
			}
		}

		// relabel image to ensure consecutive labels
		try {
			LabelComponentsSequential lop = 
				new LabelComponentsSequential(newLabelImage, false);
			lop.runOp();

			// Hack: make sure to preserve calibration information
			newLabelImage = this.originalImage.duplicate();
			for (int y=0; y<newLabelImage.getSizeY(); ++y) {
				for (int x=0; x<newLabelImage.getSizeX(); ++x) {
					newLabelImage.putValueInt(x, y, lop.getLabelImage().getValueInt(x, y));
				}
			}

		} catch (ALDException aoe) {
			System.out.println("-> relabeling failed, skipping step...");
			aoe.printStackTrace();
		}

		String withoutPath = 
				this.currentFile.substring(this.currentFile.lastIndexOf(File.separator)+1);
		String withoutEnding = 
				withoutPath.substring(0, withoutPath.lastIndexOf("."));
		String nf = 
			this.internalOutputDir + File.separator + withoutEnding	+ this.outFileEnding;
		ImageWriterMTB iw;
		try {
			if (this.verbose.booleanValue())
				System.out.println("Writing image to " + nf + "...");
			iw = new ImageWriterMTB(newLabelImage, nf);
			iw.runOp();
			if (this.verbose.booleanValue())
				System.out.println("Done!");
		} catch (Exception e) {
			JOptionPane.showMessageDialog(this.mainFrame,
				"Problems saving edit result for " + this.currentFile 
					+ "... skipping!\n\nDetails: \n"	+ e.getMessage(), 
						"ImageLabelEditor Warning",	JOptionPane.ERROR_MESSAGE);
		}
		
	}
	
	/**
	 * Callback function called in case of changes of parameter {@link #inputDir}.
	 * <p>
	 * If the parameter {@link #outputDir} is null on changes of 
	 * {@link #inputDir} it will get the same value. 
	 */
	@SuppressWarnings("unused")
	private void callbackInputDir() 
	{
		// the first call takes place during initialization, but this call should
		// not result in changes of the value for the output directors, thus,
		// just ignore it
		if (this.calledFirstTime) {
			this.calledFirstTime = false;
			return;
		}
    try {
  		if (   this.outputDir == null 
  				|| this.outputDir.getDirectoryName().isEmpty()) {
  			this.setParameter("outputDir", this.inputDir);
    	}
    } catch (ALDOperatorException e) {
    	// TODO Auto-generated catch block
    	e.printStackTrace();
    }
	}
}
