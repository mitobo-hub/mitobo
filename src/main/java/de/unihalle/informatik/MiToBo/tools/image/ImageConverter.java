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

package de.unihalle.informatik.MiToBo.tools.image;

import de.unihalle.informatik.Alida.admin.annotations.ALDMetaInfo;
import de.unihalle.informatik.Alida.admin.annotations.ALDMetaInfo.ExportPolicy;
import de.unihalle.informatik.Alida.annotations.ALDAOperator;
import de.unihalle.informatik.Alida.annotations.Parameter;
import de.unihalle.informatik.Alida.annotations.Parameter.ExpertMode;
import de.unihalle.informatik.Alida.exceptions.ALDOperatorException;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImageRGB;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImageWindow;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage.MTBImageType;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImageWindow.BoundaryPadding;
import de.unihalle.informatik.MiToBo.core.operator.MTBOperator;

/**
 * Plugin to convert images that are represented by MTBImage.
 * This plugin can be used as operator, because it provides more functionality than the MTBImage.convertType(..)-method,
 * namely splitting of RGB color channels to true channels and merging of true channels to RGB color channels. See setChannelsAreRGBFlag(..)-method
 * 
 * @author Oliver Gress
 *
 */
@ALDMetaInfo(export=ExportPolicy.ALLOWED)
@ALDAOperator(genericExecutionMode=ALDAOperator.ExecutionMode.ALL)
public class ImageConverter extends MTBOperator {

	@Parameter( label= "InputImage", required = true,
			direction = Parameter.Direction.IN, 
			 mode=ExpertMode.STANDARD, dataIOOrder=1,
      description = "Input image")
	private transient MTBImage inputImg = null;
	
	@Parameter( label= "ResultingImage", required = true,
			direction = Parameter.Direction.OUT, 
			 mode=ExpertMode.STANDARD, dataIOOrder=1,
			description = "Resulting image")
	private transient MTBImage resultImg = null;
	
	@Parameter( label= "OutputType", required = true,
			direction = Parameter.Direction.IN, 
			mode=ExpertMode.STANDARD, dataIOOrder=2,
			description = "output image type")
	private MTBImageType outputType = null;
	
	@Parameter( label= "ScaleValues", required = true,
			direction = Parameter.Direction.IN, 
			 mode=ExpertMode.STANDARD, dataIOOrder=3, 	
			description = "Scale image values to the range of values of the output type if necessary")
	private boolean scaleValues = true;
	
	@Parameter( label= "ChannelsAreRGB", required = false,
			direction = Parameter.Direction.IN, 
			 mode=ExpertMode.STANDARD, dataIOOrder=4,
			description = "Flag for interpretation of channels as RGB color channels (and vice versa) if converting from or to RGB")
	private Boolean channelsAreRGB = null;
	
	/**
	 * Constructor. Use set-functions to specify parameters
	 */
	public ImageConverter() throws ALDOperatorException {
		super();
	}
	
	/**
	 * Constructor
	 * @param _inputImg input image that has to be converted
	 * @param _outputType output image type
	 * @param _scaleValues set true to scale values to the range of output type values if necessary
	 * @param _channelsAreRGB if true, RGB color channels are separated to true channels when converting from RGB to gray and channels are merged into RGB color channels
	 *                       when converting from gray to RGB. See setChannelsAreRGBFlag(..)-method. May be null for gray-to-gray conversion.
	 */
	public ImageConverter(MTBImage _inputImg, MTBImageType _outputType, 
			boolean _scaleValues, Boolean _channelsAreRGB) 
		throws ALDOperatorException {
		super();
		
		this.inputImg = _inputImg;
		this.outputType = _outputType;
		this.scaleValues = _scaleValues;
		this.channelsAreRGB = _channelsAreRGB;
	}
	
	@Override
	public void validateCustom() throws ALDOperatorException {
		if ((this.inputImg.getType() == MTBImageType.MTB_RGB 
				^ this.outputType == MTBImageType.MTB_RGB)
				&& this.channelsAreRGB == null) {
			throw new ALDOperatorException(
					ALDOperatorException.OperatorExceptionType.VALIDATION_FAILED, 
					"ImageConverter.validateCustom(): "
					+ " If input type is non-RGB and output type is RGB, the flag for channel interpretation must be set.");
		}
	}
	
	@Override
	protected void operate() {
		
		if (this.outputType == this.inputImg.getType())
			this.resultImg = this.inputImg.duplicate();
		else {
			
			if (!(this.inputImg.getType() == MTBImageType.MTB_RGB ^ this.outputType == MTBImageType.MTB_RGB)
				|| this.channelsAreRGB.booleanValue() == false)
				
				this.resultImg = this.inputImg.convertType(this.outputType, this.scaleValues);
			else {
				
				if (this.inputImg.getType() != MTBImageType.MTB_RGB && this.outputType == MTBImageType.MTB_RGB) {
					
					this.resultImg = MTBImage.createMTBImage(this.inputImg.getSizeX(), this.inputImg.getSizeY(), this.inputImg.getSizeZ(), 
							this.inputImg.getSizeT(), (this.inputImg.getSizeC()-1)/3 + 1, this.outputType);
					
					this.resultImg.setCalibration(this.inputImg.getCalibration().copy());
					this.resultImg.setXML(this.inputImg.getXML());
					
					MTBImage byteImg = null;
					
					if (this.inputImg.getType() != MTBImageType.MTB_BYTE)
						byteImg = this.inputImg.convertType(MTBImageType.MTB_BYTE, this.scaleValues);
					else
						byteImg = this.inputImg;
					
					MTBImageWindow imgWin = new MTBImageWindow(this.inputImg.getSizeX(), this.inputImg.getSizeY(), this.inputImg.getSizeZ(),
							this.inputImg.getSizeT(), 1, byteImg, BoundaryPadding.PADDING_ZERO);
					
					for (int c = 0; c < this.inputImg.getSizeC(); c++) {
						if (c % 3 == 0) {
							((MTBImageRGB)this.resultImg).getChannelR().setImagePart(imgWin, 0, 0, 0, 0, c/3);
						}
						
						if (c % 3 == 1) {
							((MTBImageRGB)this.resultImg).getChannelG().setImagePart(imgWin, 0, 0, 0, 0, c/3);
						}
						
						if (c % 3 == 2) {
							((MTBImageRGB)this.resultImg).getChannelB().setImagePart(imgWin, 0, 0, 0, 0, c/3);
						}
						
						imgWin.incrPositionC();
					}
				}
				else {
					
					this.resultImg = MTBImage.createMTBImage(this.inputImg.getSizeX(), this.inputImg.getSizeY(), this.inputImg.getSizeZ(), 
							this.inputImg.getSizeT(), this.inputImg.getSizeC()*3, this.outputType);
					
					this.resultImg.setCalibration(this.inputImg.getCalibration().copy());
					this.resultImg.setXML(this.inputImg.getXML());

					// copy the single color channels one after the other to save some memory
					MTBImage colorchannelImg = null;
					MTBImageWindow imgWin = null;
					
					// copy red values to corresponding channels
					if (this.outputType == MTBImageType.MTB_BYTE)
						colorchannelImg = ((MTBImageRGB)this.inputImg).getChannelR();
					else
						colorchannelImg = ((MTBImageRGB)this.inputImg).getChannelR().convertType(this.outputType, this.scaleValues);
					
					imgWin = new MTBImageWindow(colorchannelImg.getSizeX(), colorchannelImg.getSizeY(), colorchannelImg.getSizeZ(),
										colorchannelImg.getSizeT(), 1, colorchannelImg, BoundaryPadding.PADDING_ZERO);
					
					for (int c = 0; c < this.inputImg.getSizeC(); c++) {
						
						this.resultImg.setImagePart(imgWin, 0, 0, 0, 0, c*3);
						imgWin.incrPositionC();
					}

					// copy green values to corresponding channels
					if (this.outputType == MTBImageType.MTB_BYTE)
						colorchannelImg = ((MTBImageRGB)this.inputImg).getChannelG();
					else
						colorchannelImg = ((MTBImageRGB)this.inputImg).getChannelG().convertType(this.outputType, this.scaleValues);
					
					imgWin = new MTBImageWindow(colorchannelImg.getSizeX(), colorchannelImg.getSizeY(), colorchannelImg.getSizeZ(),
							colorchannelImg.getSizeT(), 1, colorchannelImg, BoundaryPadding.PADDING_ZERO);
					
					for (int c = 0; c < this.inputImg.getSizeC(); c++) {
						
						this.resultImg.setImagePart(imgWin, 0, 0, 0, 0, c*3 + 1);
						imgWin.incrPositionC();
					}
					
					// copy blue values to corresponding channels
					if (this.outputType == MTBImageType.MTB_BYTE)
						colorchannelImg = ((MTBImageRGB)this.inputImg).getChannelB();
					else
						colorchannelImg = ((MTBImageRGB)this.inputImg).getChannelB().convertType(this.outputType, this.scaleValues);
					
					imgWin = new MTBImageWindow(colorchannelImg.getSizeX(), colorchannelImg.getSizeY(), colorchannelImg.getSizeZ(),
							colorchannelImg.getSizeT(), 1, colorchannelImg, BoundaryPadding.PADDING_ZERO);
					
					for (int c = 0; c < this.inputImg.getSizeC(); c++) {
						
						this.resultImg.setImagePart(imgWin, 0, 0, 0, 0, c*3 + 2);
						imgWin.incrPositionC();
					}
				}
				
				this.resultImg.setTitle(MTBImage.getTitleRunning(this.inputImg.getTitle()));
			}
		}
	}
	
	/**
	 * Get input image
	 */
	public MTBImage getInputImg() {
		return this.inputImg;
	}

	/**
	 * Set input image 
	 */
	public void setInputImg(MTBImage _inputImg1) {
		this.inputImg = _inputImg1;
	}

	/**
	 * Get result image
	 */
	public MTBImage getResultImg() {
		return this.resultImg;
	}

	/**
	 * Set result image
	 */
	protected void setResultImg(MTBImage resultImg1) {
		this.resultImg = resultImg1;
	}

	/**
	 * Get output image type
	 */
	public MTBImageType getOutputType() {
		return this.outputType;
	}
	
	/**
	 * Set output image type
	 */
	public void setOutputType(MTBImageType outputType1) {
		this.outputType = outputType1;
	}

	/**
	 * Get flag if values are scaled to match the range of output type values if necessary
	 */
	public boolean isScaleValues() {
		return this.scaleValues;
	}
	
	/**
	 * Set flag if values are scaled to match the range of output type values if necessary
	 */
	public void setScaleValues(boolean scaleValues1) {
		this.scaleValues = scaleValues1;
	}

	/**
	 * Get flag if RGB color channels are interpreted as real image channels. (see setChannelsAreRGBFlag(..))
	 */
	public Boolean getChannelsAreRGBFlag() {
		return this.channelsAreRGB;
	}

	/**
	 * Set flag if RGB color channels are interpreted as real image channels. If true and conversion
	 * is from gray value to RGB or vice versa, the each color channel is treated as separate channel.
	 * If false RGB values are converted to gray values (gray = (R+G+B)/3). In the other conversion direction, gray values are converted
	 * to gray RGB values (R=G=B = gray) 
	 */
	public void setChannelsAreRGBFlag(Boolean channelsAreRGB1) {
		this.channelsAreRGB = channelsAreRGB1;
	}

//	/**
//	 * Generate dialog for parameter specification. Returns null if dialog was canceled.
//	 */
//	protected boolean parameterDialog() {
//		
//		GenericDialog gd = new GenericDialog("Convert image");
//
//		gd.addMessage("Input image datatype: " + this.inputImg.getType().toString());
//		
//		MTBImageType[] types = MTBImageType.values();
//		String[] stypes = new String[types.length];
//		for (int i = 0; i < types.length; i++) {
//			stypes[i] = types[i].toString();
//		}
//
//		gd.addChoice("Select new image datatype", stypes, stypes[this.inputImg.getType().ordinal()]);
//
//		gd.addCheckbox("Scale pixel values", true);
//		
//		if (this.inputImg.getType() == MTBImageType.MTB_RGB) {
//			gd.addCheckbox("Split RGB channels", true);
//		}
//		else {
//			gd.addCheckbox("Merge channels to RGB", false);
//		}
//
//		Panel hpanel = new Panel();
//		hpanel.setLayout(new BoxLayout(hpanel, BoxLayout.X_AXIS));
//		hpanel.add(new Label("                                                         "));
//		hpanel.add(Box.createHorizontalGlue());
//		Button hbutton = new Button("Help");
//		hbutton.addActionListener(this);
//		hpanel.add(hbutton);
//		gd.addPanel(hpanel);
//		
//		gd.addDialogListener(this);
//		gd.addComponentListener(this);
//
//		gd.validate();
//		gd.showDialog();
//		
//		if (gd.wasOKed()) {
//			
//			this.outputType = types[gd.getNextChoiceIndex()];
//			this.scaleValues = gd.getNextBoolean();
//			
//			if (this.inputImg.getType() == MTBImageType.MTB_RGB ^ this.outputType == MTBImageType.MTB_RGB)
//				this.channelsAreRGB = gd.getNextBoolean();
//			else
//				this.channelsAreRGB = null;
//
//			
//			return true;
//		}
//		else 
//			return false;
//		
//	}
//
//	@Override
//	public void actionPerformed(ActionEvent e) {
//		
//		if (e.getActionCommand().equals("Help")) {
//			
//			GenericDialog gd = new GenericDialog("Help");
//			gd.hideCancelButton();
//			
//			Panel headerpanel = new Panel();
//			headerpanel.setLayout(new BoxLayout(headerpanel, BoxLayout.Y_AXIS));
//			Label header = new Label("Datatype information:");
//			headerpanel.add(header);
//			headerpanel.add(new Label(" "));
//			gd.addPanel(headerpanel);
//			
//			
//			Panel infopanel = new Panel();
//			infopanel.setLayout(new GridLayout(8, 4));
//			
//			infopanel.add(new Label("MiToBo type"));
//			infopanel.add(new Label("Bits per pixel"));
//			infopanel.add(new Label("Description"));
//			infopanel.add(new Label("ImageJ type"));
//
//			infopanel.add(new Label("------------------"));
//			infopanel.add(new Label("------------------"));
//			infopanel.add(new Label("------------------"));
//			infopanel.add(new Label("------------------"));
//			
//			infopanel.add(new Label("MTB_BYTE"));
//			infopanel.add(new Label("8"));
//			infopanel.add(new Label("unsigned byte"));
//			infopanel.add(new Label("unsigned byte"));
//			
//			
//			infopanel.add(new Label("MTB_SHORT"));
//			infopanel.add(new Label("16"));
//			infopanel.add(new Label("unsigned short"));
//			infopanel.add(new Label("unsigned short"));
//			
//			
//			infopanel.add(new Label("MTB_INT"));
//			infopanel.add(new Label("32"));
//			infopanel.add(new Label("signed int"));
//			infopanel.add(new Label("NONE"));
//			
//			
//			infopanel.add(new Label("MTB_FLOAT"));
//			infopanel.add(new Label("32"));
//			infopanel.add(new Label("float"));
//			infopanel.add(new Label("float"));
//			
//			
//			infopanel.add(new Label("MTB_DOUBLE"));
//			infopanel.add(new Label("64"));
//			infopanel.add(new Label("double"));
//			infopanel.add(new Label("NONE"));
//			
//			
//			infopanel.add(new Label("MTB_RGB"));
//			infopanel.add(new Label("8 (per channel)"));
//			infopanel.add(new Label("unsigned byte for each R,G,B"));
//			infopanel.add(new Label("NONE"));
//			
//			
//			gd.addPanel(infopanel);
//			
//			gd.showDialog();
//			
//		}
//		
//	}
//
//	@Override
//	public boolean dialogItemChanged(GenericDialog gd, AWTEvent e) {
//		
//		if (e != null && this.inputImg.getType() != MTBImageType.MTB_RGB) {
//			
//			if (e.paramString().matches(".*item=MTB_RGB.*")) {
//				
//				@SuppressWarnings("unchecked")
//				Vector<Checkbox> cbs = gd.getCheckboxes();
//				
//				cbs.get(1).setVisible(true);
//				cbs.get(1).repaint();
//
//				gd.validate();
//			}
//			else if (e.paramString().matches(".*item=MTB_(RGB){0}.*")) {
//				@SuppressWarnings("unchecked")
//				Vector<Checkbox> cbs = gd.getCheckboxes();
//				
//				cbs.get(1).setVisible(false);
//				cbs.get(1).repaint();
//				
//				this.channelsAreRGB = false;
//			}
//
//		}
//		else if (e != null && this.inputImg.getType() == MTBImageType.MTB_RGB) {
//			
//			if (e.paramString().matches(".*item=MTB_RGB.*")) {
//
//				@SuppressWarnings("unchecked")
//				Vector<Checkbox> cbs = gd.getCheckboxes();
//				
//				cbs.get(1).setVisible(false);
//				cbs.get(1).repaint();
//				
//				this.channelsAreRGB = false;
//			}
//			else if (e.paramString().matches(".*item=MTB_(RGB){0}.*")) {
//
//				@SuppressWarnings("unchecked")
//				Vector<Checkbox> cbs = gd.getCheckboxes();
//				
//				cbs.get(1).setVisible(true);
//				cbs.get(1).repaint();
//				
//				gd.validate();
//			}
//			
//			
//		}
//		
//		return true;
//	}

	
//	@Override
//	public void componentResized(ComponentEvent e) {
//	}

//	@Override
//	public void componentMoved(ComponentEvent e) {
//	}

//	@Override
//	public void componentShown(ComponentEvent e) {
//		if (e != null && e.getComponent() instanceof GenericDialog) {
//			GenericDialog gd = (GenericDialog)e.getComponent();
//			gd.validate();
//			
//			//if (this.inputImg.getType() != MTBImageType.MTB_RGB) {
//				
//				@SuppressWarnings("unchecked")
//				Vector<Checkbox> cbs = gd.getCheckboxes();
//				
//				cbs.get(1).setVisible(false);
//				cbs.get(1).repaint();
//				//gd.repaint();
//			//}
//		}
//	}

//	@Override
//	public void componentHidden(ComponentEvent e) {
//	}


}
