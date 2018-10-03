/*
 * This file is part of MiToBo, the Microscope Image Analysis Toolbox.
 *
 * Copyright (C) 2010 - 2014
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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.Point2D;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.TreeSet;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import de.unihalle.informatik.Alida.annotations.ALDAOperator;
import de.unihalle.informatik.Alida.annotations.Parameter;
import de.unihalle.informatik.Alida.annotations.Parameter.ParameterModificationMode;
import de.unihalle.informatik.Alida.annotations.ALDAOperator.Level;
import de.unihalle.informatik.Alida.datatypes.ALDDirectoryString;
import de.unihalle.informatik.Alida.exceptions.ALDOperatorException;
import de.unihalle.informatik.Alida.operator.ALDOperator;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage.MTBImageType;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImageByte;
import de.unihalle.informatik.MiToBo.core.operator.MTBOperator;
import de.unihalle.informatik.MiToBo.io.dirs.DirectoryTree;
import de.unihalle.informatik.MiToBo.io.images.ImageReaderMTB;
import de.unihalle.informatik.MiToBo.io.images.ImageWriterMTB;
import de.unihalle.informatik.MiToBo.segmentation.regions.labeling.LabelComponentsSequential;
import ij.IJ;
import ij.ImagePlus;
import ij.gui.ImageCanvas;
import ij.process.ImageProcessor;

/**
 * A small interactive tool for deleting regions from a label image 
 * by mouse-clicks.
 * 
 * @author moeller
 */
@ALDAOperator(genericExecutionMode=ALDAOperator.ExecutionMode.SWING, 
	level=Level.APPLICATION, allowBatchMode = false)
public class LabelImageEditor extends MTBOperator 
		implements MouseListener, MouseMotionListener, ActionListener {

	/**
	 * Directory to process.
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
	 * Main window frame.
	 */
	private JFrame mainFrame;
	
	/**
	 * Image canvas for displaying image and interacting.
	 */
	private ImageCanvas ic;
	
	private MTBImageByte activeImage;
	
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
	
	private boolean memoryMode = false;
	private int lastLabel;
	
	private boolean scanPath = false;
	private Point2D.Double lastPathPoint;
	
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

			if (this.verbose.booleanValue())
				System.out.println("Processing image " + f + "...");
			
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
				this.activeImage = 
						(MTBImageByte)reader.getResultMTBImage().convertType(
								MTBImageType.MTB_BYTE, true);
				
				// init frame with first image
				if (this.activePlus == null) {

					this.mainFrame = new JFrame();
					this.mainFrame.setLayout(new BorderLayout());
					JPanel buttons = new JPanel();
					JButton next = new JButton("Next");
					next.addActionListener(this);
					next.setActionCommand("next");
					buttons.add(next);
					JButton contrast = new JButton("Optimize Contrast");
					contrast.addActionListener(this);
					contrast.setActionCommand("contrast");
					buttons.add(contrast);
					JButton boundaries = new JButton("Fix Boundaries");
					boundaries.addActionListener(this);
					boundaries.setActionCommand("boundaries");
					buttons.add(boundaries);
					JButton relabel = new JButton("Relabel");
					relabel.addActionListener(this);
					relabel.setActionCommand("relabel");
					buttons.add(relabel);
					JButton undo = new JButton("Undo");
					undo.addActionListener(this);
					undo.setActionCommand("undo");
					buttons.add(undo);
					JButton quit = new JButton("Quit");
					quit.addActionListener(this);
					quit.setActionCommand("quit");
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
					System.out.println("Finished!");
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
		
		// ignore clicks to the background except in memory mode
		// => filling in background regions is allowed
		if (label == 0 && !memoryMode)
			return;
		
		if (IJ.shiftKeyDown()) {
			this.memoryMode = true;
			this.lastLabel = label;
			return;
		}

		boolean newLabel = false;
		if (IJ.controlKeyDown()) {
			newLabel = true;
		}
		
		// set all pixels with identical label to background
		this.lastProcessor = this.activeProcessor.duplicate();
		if (this.memoryMode) {
			for (int y=0; y<this.activeProcessor.getHeight();++y) {
				for (int x=0; x<this.activeProcessor.getWidth();++x) {
					if (this.activeProcessor.getPixel(x, y) == label) {
						this.activeProcessor.putPixel(x, y, lastLabel);
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
					labels.add(new Integer(clabel));
					if (clabel > maxLabel)
						maxLabel = clabel;
				}
			}
			for (int nl = maxLabel; nl > 0; --nl) {
				if (labels.contains(new Integer(nl)))
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
		// optimize contrast
		if (c.equals("contrast")) {
			
			this.lastProcessor = this.activeProcessor.duplicate();

			this.optimizeContrast(this.activeProcessor);

			this.activePlus.setProcessor(this.activeProcessor);
			this.activePlus.updateAndDraw();
			this.ic.setImageUpdated();
			this.ic.repaint();
		}
		// delete obsolete boundaries
		if (c.equals("boundaries")) {
			
			this.lastProcessor = this.activeProcessor.duplicate();
			
			int nLabel;
			TreeSet<Integer> labels = new TreeSet<>();

			// run over the image and search for background pixels having two or 
			// more components with the same label in their neighborhood
			for (int y=0; y<this.activeProcessor.getHeight();++y) {
				for (int x=0; x<this.activeProcessor.getWidth();++x) {
					int label = this.activeProcessor.getPixel(x, y);
					if (label != 0)
						continue;

					labels.clear();
					for (int dy = -1; dy <= 1; ++dy) {
						for (int dx = -1; dx <= 1; ++dx) {
							if (dx == 0 && dy == 0)
								continue;
							if (   x+dx >= 0 && x+dx < this.activeImage.getSizeX()
									&& y+dy >= 0 && y+dy < this.activeImage.getSizeY()) {
								nLabel = this.activeProcessor.getPixel(x+dx,  y+dy);
								if (nLabel == 0)
									continue;
								labels.add(new Integer(nLabel));
							}						
						}
					}
					if (labels.size() == 1) {
						int count = 0;
						label = labels.first().intValue();
						for (int dy = -1; dy <= 1; ++dy) {
							for (int dx = -1; dx <= 1; ++dx) {
								if (dx == 0 && dy == 0)
									continue;
								if (   x+dx >= 0 && x+dx < this.activeImage.getSizeX()
										&& y+dy >= 0 && y+dy < this.activeImage.getSizeY()) {
									if (this.activeProcessor.getPixel(x+dx,  y+dy) == label)
										++count;
								}
							}
						}
						if (count > 4) 
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
			
			MTBImageByte binImage = 
					(MTBImageByte)this.activeImage.duplicate().convertType(
							MTBImageType.MTB_BYTE, true);
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
			this.mainFrame.setVisible(false);
			this.mainFrame.dispose();
			this.finished = true;
		}
	}
	
	private void optimizeContrast(ImageProcessor ip) {

		// count labels
		TreeSet<Integer> labels = new TreeSet<>();
		for (int y=0; y<ip.getHeight();++y) {
			for (int x=0; x<ip.getWidth();++x) {
				labels.add(new Integer(ip.getPixel(x, y)));
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
		int min = (int)(max * 2.0 / 3.0);			
		int step = (max - min) / labels.size();

		int n = 0, newLabel;
		for (Integer i: sortedLabels) {
			newLabel = max - n * step;
			labelMap.put(i, new Integer(newLabel));
			++n;
		}
		
		for (int y=0; y<ip.getHeight();++y) {
			for (int x=0; x<ip.getWidth();++x) {
				int label = ip.getPixel(x, y);
				if (label == 0)
					continue;
				newLabel = labelMap.get(new Integer(label));
				ip.putPixel(x, y, newLabel);
			}
		}
	}
	
	/**
	 * Saves the current processing result.
	 */
	private void saveFile() {
		MTBImage newLabelImage = MTBImage.createMTBImage(this.activePlus);
		String withoutPath = 
				this.currentFile.substring(this.currentFile.lastIndexOf("/")+1);
		String withoutEnding = 
				withoutPath.substring(0, withoutPath.lastIndexOf("."));
		String nf = this.internalOutputDir + "/" + withoutEnding + "-edited.tif";
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
