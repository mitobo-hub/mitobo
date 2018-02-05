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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
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
import de.unihalle.informatik.MiToBo.core.operator.MTBOperator;
import de.unihalle.informatik.MiToBo.io.dirs.DirectoryTree;
import de.unihalle.informatik.MiToBo.io.images.ImageReaderMTB;
import de.unihalle.informatik.MiToBo.io.images.ImageWriterMTB;
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
		implements MouseListener, ActionListener {

	/**
	 * Directory to process.
	 */
	@Parameter(label = "Input Directory", required = true, 
		direction = Parameter.Direction.IN, description = "Input directory.",
		dataIOOrder = 0, callback = "callbackInputDir",
		paramModificationMode = ParameterModificationMode.MODIFIES_VALUES_ONLY)
	private ALDDirectoryString inputDir = null;

	/**
	 * Directory to process.
	 */
	@Parameter(label = "File Filter", required = true, 
		direction = Parameter.Direction.IN, 
		description = "Regular expression to filter input files.",
		dataIOOrder = 1)
	private String inputRegExp = ".tif";

	/**
	 * Optional output directory.
	 * <p>
	 * If left unset results are saved to input directory.
	 */
	@Parameter(label = "Output Directory", required = false, 
		direction = Parameter.Direction.IN, description = "Output directory.",
		dataIOOrder = 0)
	private ALDDirectoryString outputDir = null;

	/**
	 * Main window frame.
	 */
	private JFrame mainFrame;
	
	/**
	 * Image canvas for displaying image and interacting.
	 */
	private ImageCanvas ic;
	
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
				ALDOperator.ConstructioMode.NO_HISTORY);

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
		Pattern p = Pattern.compile(this.inputRegExp);
		for (String f: files) {

			if (this.verbose.booleanValue())
				System.out.println("Processing image " + f + "...");
			
			// if file name does not match filter, skip it
			Matcher m = p.matcher(f);
			if (!m.find())
				continue;
				
			try {
				
				// read input image
				reader.setFileName(f);
				reader.runOp();
				MTBImage image = reader.getResultMTBImage();
				
				// init frame with first image
				if (this.activePlus == null) {

					this.mainFrame = new JFrame();
					this.mainFrame.setLayout(new BorderLayout());
					JPanel buttons = new JPanel();
					JButton next = new JButton("Next");
					next.addActionListener(this);
					next.setActionCommand("next");
					buttons.add(next);
					JButton quit = new JButton("Quit");
					quit.addActionListener(this);
					quit.setActionCommand("quit");
					buttons.add(quit);
					this.mainFrame.add(buttons,BorderLayout.NORTH);
					JPanel canvas = new JPanel();
					
					this.activePlus = image.getImagePlus();
					this.activePlus.show();
					this.ic = this.activePlus.getCanvas();
					this.activePlus.hide();
					this.ic.addMouseListener(this);
					
					canvas.add(this.ic);
					this.mainFrame.add(canvas, BorderLayout.CENTER);
					this.mainFrame.setSize(1200, 1200);
					this.mainFrame.setVisible(true);
				}
				
				this.mainFrame.setTitle(image.getTitle());
				this.activeProcessor = image.getImagePlus().getProcessor();
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
		
		// ignore clicks to the background
		if (label == 0)
			return;
		
		// set all pixels with identical label to background
		for (int y=0; y<this.activeProcessor.getHeight();++y) {
			for (int x=0; x<this.activeProcessor.getWidth();++x) {
				if (this.activeProcessor.getPixel(x, y) == label)
					this.activeProcessor.putPixel(x, y, 0);
			}
		}
		this.activePlus.setProcessor(this.activeProcessor);
		this.activePlus.updateAndDraw();
		this.ic.setImageUpdated();
		this.ic.repaint();
	}

	@Override
	public void mousePressed(MouseEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		// TODO Auto-generated method stub
		
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
	public void actionPerformed(ActionEvent e) {
		
		String c = e.getActionCommand();
		
		// save the current processing result and proceed
		if (c.equals("next")) {
			this.saveFile();
			this.stillActive = false; 
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
	
	/**
	 * Saves the current processing result.
	 */
	private void saveFile() {
		MTBImage newLabelImage = MTBImage.createMTBImage(this.activePlus);
		String withoutPath = 
				this.currentFile.substring(this.currentFile.lastIndexOf("/")+1);
		String withoutEnding = 
				withoutPath.substring(0, withoutPath.lastIndexOf("."));
		System.out.println(withoutEnding);
		String nf = this.internalOutputDir + "/" + withoutEnding + "-edited.tif";
		System.out.println(nf);
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
