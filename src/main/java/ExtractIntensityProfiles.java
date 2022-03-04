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



import java.awt.Color;
import java.awt.geom.Point2D;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.Set;
import java.util.Vector;

import de.unihalle.informatik.Alida.exceptions.ALDOperatorException;
import de.unihalle.informatik.Alida.exceptions.ALDProcessingDAGException;
import de.unihalle.informatik.Alida.exceptions.ALDOperatorException.OperatorExceptionType;
import de.unihalle.informatik.Alida.helpers.ALDFilePathManipulator;
import de.unihalle.informatik.Alida.operator.events.ALDOperatorExecutionProgressEvent;
import de.unihalle.informatik.Alida.annotations.ALDAOperator;
import de.unihalle.informatik.Alida.annotations.ALDAOperator.Level;
import de.unihalle.informatik.Alida.annotations.Parameter;
import de.unihalle.informatik.Alida.datatypes.ALDFileString;
import de.unihalle.informatik.MiToBo.color.conversion.HSVToRGBArrayConverter;
import de.unihalle.informatik.MiToBo.color.conversion.HSVToRGBPixelConverter;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBContour2D;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBContour2DSet;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBVectorField2D;
import de.unihalle.informatik.MiToBo.core.datatypes.images.*;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage.MTBImageType;
import de.unihalle.informatik.MiToBo.core.imageJ.RoiManagerAdapter;
import de.unihalle.informatik.MiToBo.core.operator.*;
import de.unihalle.informatik.MiToBo.fields.FieldOperations2D;
import de.unihalle.informatik.MiToBo.fields.FieldOperations2D.FieldOperation;
import de.unihalle.informatik.MiToBo.fields.GradientFieldCalculator2D;
import de.unihalle.informatik.MiToBo.fields.GradientFieldCalculator2D.GradientMode;
import de.unihalle.informatik.MiToBo.gui.MTBTableModel;
import de.unihalle.informatik.MiToBo.io.dirs.DirectoryTree;
import de.unihalle.informatik.MiToBo.io.images.ImageReaderMTB;
import de.unihalle.informatik.MiToBo.io.images.ImageWriterMTB;
import de.unihalle.informatik.MiToBo.math.arrays.filter.GaussFilterDouble1D;
import de.unihalle.informatik.MiToBo.morphology.DistanceTransform;
import de.unihalle.informatik.MiToBo.morphology.ImgDilate;
import ij.ImagePlus;
import ij.gui.PolygonRoi;
import ij.process.ImageProcessor;
import loci.common.services.DependencyException;
import loci.common.services.ServiceException;
import loci.formats.FormatException;
import de.unihalle.informatik.MiToBo.morphology.DistanceTransform.DistanceMetric;
import de.unihalle.informatik.MiToBo.morphology.DistanceTransform.ForegroundColor;

/**
 * This class implements morphological dilation on 2D binary/grayscale images.
 * <p>
 * If the given image only contains two pixel values it is interpreted as 
 * binary image. In the resulting image the background pixels will be set 
 * to the smaller value, while the foreground pixels will be set to the 
 * larger ones.
 * <p> 
 * The structuring element is a square matrix of size 'masksize' x 'masksize', 
 * with reference pixel in the center of the matrix.
 *
 * Attention: if masksize is even, errors may result due 
 *            to lack of operator symmetry
 *
 * @author moeller
 */
@ALDAOperator(genericExecutionMode=ALDAOperator.ExecutionMode.ALL,
		level=Level.APPLICATION)
public class ExtractIntensityProfiles extends MTBOperator {

	@Parameter( label= "Input Image", required = true, dataIOOrder = 0,
			direction = Parameter.Direction.IN, description = "Input image")
	private transient MTBImageByte inImg = null;

	@Parameter( label= "Width", required = true, dataIOOrder = 0,
			direction = Parameter.Direction.IN, description = "Width")
	private transient int width = 11;

	/**
	 * Default constructor.
	 *  @throws ALDOperatorException
	 */
	public ExtractIntensityProfiles() throws ALDOperatorException {
		// nothing to do here
	}

	/** Get value of inImg.
	  * Explanation: Input image.
	  * @return value of inImg
	  */
	public MTBImage getInputImage(){
		return this.inImg;
	}

	/**
	 * This method does the actual work. 
	 * @throws ALDOperatorException 
	 * @throws ALDProcessingDAGException 
	 */
	@Override
	protected void operate() throws ALDOperatorException, ALDProcessingDAGException {
		
		String fileRoot = 
				ALDFilePathManipulator.removeExtension(this.inImg.getTitle());
		System.out.println(fileRoot);
		
		String enhImageNmae = fileRoot + "-enhanced.tif";
		MTBImageByte enhImage = null;
		ImageReaderMTB imreader;
		try {
			imreader = new ImageReaderMTB(enhImageNmae);
			imreader.runOp();
			enhImage = (MTBImageByte)imreader.getResultMTBImage().convertType(
					MTBImageType.MTB_BYTE, true);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		DirectoryTree rootDirTree = new DirectoryTree(".", false);
		Vector<String> files = rootDirTree.getFileList();
		
		Vector<String> contourFiles = new Vector<>();
		for (String f: files) {
			if (f.contains(fileRoot) && f.contains("contourData")) {
				contourFiles.add(f);
			}
		}
		
		MTBImageByte workImg = (MTBImageByte)this.inImg.duplicate();
		MTBImageByte dilImg = (MTBImageByte)this.inImg.duplicate();
		
		int x, y;
		int cPointsNum = 0;
		Vector<Double> curvatures = new Vector<Double>();
		try {
			for (String f: contourFiles) {
				
				workImg.fillBlack();
				
				Vector<Point2D.Double> cPoints = new Vector<Point2D.Double>();
				BufferedReader reader = new BufferedReader(new FileReader(f));
				String line = reader.readLine();
				while (line != null) {
					String[] parts = line.split(" ");
					x = Double.valueOf(parts[0]).intValue();
					y = Double.valueOf(parts[1]).intValue();
					curvatures.add(Double.valueOf(parts[2]));
					workImg.putValueInt(x, y, 255);
					cPoints.add(new Point2D.Double(x,y));
					++cPointsNum;
					line = reader.readLine();
				}

				// write curvature image
//				double curvMin = Double.MAX_VALUE;
//				double curvMax = Double.MIN_VALUE;
//				for (Double d: curvatures) {
//					if (d.doubleValue() < curvMin) 
//						curvMin = d.doubleValue();
//					else if (d.doubleValue() > curvMax) 
//						curvMax = d.doubleValue();
//				}
//				double rangePos = curvMax;
//				double rangeNeg = Math.abs(curvMin);
				double rangePos = 30;
				double rangeNeg = 30;
				Color minColor = Color.BLUE;
				Color meanColor = Color.YELLOW;
				Color maxColor = Color.RED;
				int minR = minColor.getRed();
				int minG = minColor.getGreen();
				int minB = minColor.getBlue();

				int meanR = meanColor.getRed();
				int meanG = meanColor.getGreen();
				int meanB = meanColor.getBlue();				
				
				int maxR = maxColor.getRed();
				int maxG = maxColor.getGreen();
				int maxB = maxColor.getBlue();
				MTBImageRGB curvImage = (MTBImageRGB)MTBImage.createMTBImage(
						1024, 1024, 1, 1, 1, MTBImageType.MTB_RGB);
				curvImage.fillWhite();
				MTBImageDouble curvImageDouble = (MTBImageDouble)MTBImage.createMTBImage(
						1024, 1024, 1, 1, 1, MTBImageType.MTB_DOUBLE);
				int i = 0;
				int newR, newG, newB;
				double ratio;
				for (Point2D.Double p: cPoints) {
					double curv = curvatures.elementAt(i).doubleValue();
					if (curv >= 0) {
						ratio = curv / rangePos;
						newR = meanR + (int)(ratio*(maxR - meanR) + 0.5);
						newG = meanG + (int)(ratio*(maxG - meanG) + 0.5);
						newB = meanB + (int)(ratio*(maxB - meanB) + 0.5);
					}
					else {
						ratio = Math.abs(curv) / rangeNeg;
						newR = minR + (int)(ratio*(meanR - minR) + 0.5);
						newG = minG + (int)(ratio*(meanG - minG) + 0.5);
						newB = minB + (int)(ratio*(meanB - minB) + 0.5);						
					}
					curvImage.putValueR((int)p.x, (int)p.y, newR);
					curvImage.putValueG((int)p.x, (int)p.y, newG);
					curvImage.putValueB((int)p.x, (int)p.y, newB);
					curvImageDouble.putValueDouble((int)p.x, (int)p.y, curv);
					++i;
				}

				MTBImageRGB imgLegend = (MTBImageRGB)MTBImage.createMTBImage(
						300, 1000, 1, 1, 1, MTBImageType.MTB_RGB);

				double v;
				int row;
				for (y=0; y<500; ++y) {
					v = -30 + y * 0.002 * rangeNeg;
					ratio = (v + 30) / rangeNeg;
					newR = minR + (int)(ratio*(meanR - minR) + 0.5);
					newG = minG + (int)(ratio*(meanG - minG) + 0.5);
					newB = minB + (int)(ratio*(meanB - minB) + 0.5);

					row = 999-y;
					for (x=0; x < 200; ++x) {
						imgLegend.putValueR(x, row, newR);
						imgLegend.putValueG(x, row, newG);
						imgLegend.putValueB(x, row, newB);
					}
				}
				for (y=500; y<1000; ++y) {
					v = 0 + y * 0.002 * rangePos;
					ratio = (v - 30) / rangePos;
					newR = meanR + (int)(ratio*(maxR - meanR) + 0.5);
					newG = meanG + (int)(ratio*(maxG - meanG) + 0.5);
					newB = meanB + (int)(ratio*(maxB - meanB) + 0.5);

					row = 999-y;
					for (x=0; x < 200; ++x) {
						imgLegend.putValueR(x, row, newR);
						imgLegend.putValueG(x, row, newG);
						imgLegend.putValueB(x, row, newB);
					}
				}
				ImageProcessor ip= imgLegend.getImagePlus().getProcessor();
				ip.setColor(Color.white);
				ip.moveTo(210, 995);
				ip.drawString(Double.toString(-30));
				ip.moveTo(210, 500);
				ip.drawString(Double.toString(0));
				ip.moveTo(210, 20);
				ip.drawString(Double.toString(30));
				imgLegend = (MTBImageRGB)MTBImage.createMTBImage(new ImagePlus("Legend image", ip));

				// save file
				ImageWriterMTB imWrite;
				try {
					imWrite = new ImageWriterMTB();
					imWrite.setFileName("colorLegend.tif");
					imWrite.setInputMTBImage(imgLegend);
					imWrite.runOp();
				} catch (Exception e) {
					e.printStackTrace();
				}
				
				MTBContour2D c = new MTBContour2D(cPoints);
				PolygonRoi proi = c.convertToImageJRoi()[0];
				
				DistanceTransform dt = new DistanceTransform(workImg, 
						DistanceMetric.CITYBLOCK, ForegroundColor.FG_WHITE);
				dt.setPrecursorInfosEnabled(true);
				dt.runOp();
				Point2D.Double[][] nearPixelMap = dt.getClosestObjectPixelMap();
				
				dilImg = (MTBImageByte)workImg.duplicate();
				ImgDilate dil= new ImgDilate(dilImg, 41);
				dil.runOp();
				MTBImageByte dilatedImg = (MTBImageByte)dil.getResultImage();
				
				double maxIntensity = 0;
				
				double[][] intensitiesIn = new double[this.inImg.getSizeY()][this.inImg.getSizeX()];
				int[][] countsIn = new int[this.inImg.getSizeY()][this.inImg.getSizeX()];
				for (int py=0; py<this.inImg.getSizeY(); ++py) {
					for (int px=0; px<this.inImg.getSizeX(); ++px) {
						if (dilatedImg.getValueInt(px, py) == 255 && workImg.getValueInt(px, py) != 255) {
//							int nx = (int)nearPixelMap[py][px].x;
//							int ny = (int)nearPixelMap[py][px].y;
//							if (proi.contains(px, py)) {
//								intensitiesIn[ny][nx] += this.inImg.getValueInt(px, py);
//								countsIn[ny][nx] += 1;
////								workImg.putValueInt(px, py, 160);
//							}
							if (proi.contains(px, py)) {
								Point2D.Double p = new Point2D.Double(px,py);
								for (Point2D.Double n: cPoints) {
									if (n.distance(p) < 8) {
										if (this.inImg.getValueInt(px, py) > maxIntensity)
											maxIntensity = this.inImg.getValueInt(px, py);
										
										intensitiesIn[(int)n.y][(int)n.x] += this.inImg.getValueInt(px, py);
										countsIn[(int)n.y][(int)n.x] += 1;										
//										curvImage.putValueR(px, py, this.inImg.getValueInt(px, py));
//										curvImage.putValueG(px, py, this.inImg.getValueInt(px, py));
//										curvImage.putValueB(px, py, this.inImg.getValueInt(px, py));
										curvImage.putValueR(px, py, 255 - enhImage.getValueInt(px, py));
										curvImage.putValueG(px, py, 255 - enhImage.getValueInt(px, py));
										curvImage.putValueB(px, py, 255 - enhImage.getValueInt(px, py));
									}
								}
							}
							// broaden contour
							else {
								Point2D.Double p = new Point2D.Double(px,py);
								int nx = (int)nearPixelMap[py][px].x;
								int ny = (int)nearPixelMap[py][px].y;
								Point2D.Double n = new Point2D.Double(nx,ny);
								if (p.distance(n) < 3) {
									curvImage.putValueR(px, py, curvImage.getValueR(nx, ny));
									curvImage.putValueG(px, py, curvImage.getValueG(nx, ny));
									curvImage.putValueB(px, py, curvImage.getValueB(nx, ny));
								}
							}
						}
					}					
				}
				
				System.out.println("Max intensity: " + maxIntensity);
				
				String outname = f.replaceAll("contourData", "contourData-image");
				outname = outname.replaceAll(".txt", ".tif");
				ImageWriterMTB iw = new ImageWriterMTB(curvImage, outname);
				iw.runOp();
				
				outname = f.replaceAll("contourData", "contourData-imageGray");
				outname = outname.replaceAll(".txt", ".tif");
				iw = new ImageWriterMTB(curvImageDouble, outname);
				iw.runOp();
			

				// reformat values to arrays
				double[] inValues = new double[cPoints.size()];
				double[] inCounts = new double[cPoints.size()];
				int cc = 0;
				for (Point2D.Double p: cPoints) {
					inValues[cc] = intensitiesIn[(int)p.y][(int)p.x];
					inCounts[cc] = countsIn[(int)p.y][(int)p.x];
					++cc;
				}
				
//				GaussFilterDouble1D gaussFilter = new GaussFilterDouble1D();
//				double[] gaussKernel = GaussFilterDouble1D.getGaussKernel(11.0);
//				gaussFilter.setKernel(gaussKernel);
//
//				gaussFilter.setInputArray(inValues);
//				gaussFilter.runOp(HidingMode.HIDE_CHILDREN);
//				inValues = gaussFilter.getResultArray();
//				
//				gaussFilter.setInputArray(inCounts);
//				gaussFilter.runOp(HidingMode.HIDE_CHILDREN);
//				inCounts = gaussFilter.getResultArray();

				// save curvature values
				try {
					String outfile = f.replaceAll("contourData", "contourData-extended");
					BufferedWriter writer = new BufferedWriter(new FileWriter(outfile));

					double in;
					String inStr;
					int id = 0;

					double posCurvSum = 0;
					int posCurvCount = 0;
					double negCurvSum = 0;
					int negCurvCount = 0;
					
					for (Point2D.Double p: cPoints) {
						in = 0; 
						inStr = "NaN";
						if (inCounts[id] > 0) {
							in = inValues[id] / maxIntensity / inCounts[id];
							inStr = Double.toString(in);
							
							if (curvatures.elementAt(id).doubleValue() >= 0) {
								posCurvSum += inValues[id];
								posCurvCount += inCounts[id];
							}
							else {
								negCurvSum += inValues[id];
								negCurvCount += inCounts[id];								
							}
								
						}					
						writer.write(p.x + " " + p.y + " " + curvatures.elementAt(id) + " "
								+ inStr + "\n");
						++id;
					}
					
					writer.close();
					
					System.out.println("Pos. Curvature Intensity Sum = " + posCurvSum/maxIntensity);
					System.out.println("Pos. Curvature Intensity Count = " + posCurvCount);
					System.out.println("Pos. Curvature Intensity Avg = " + posCurvSum/maxIntensity/posCurvCount);
					System.out.println("Neg. Curvature Intensity Sum = " + negCurvSum/maxIntensity);
					System.out.println("Neg. Curvature Intensity Count = " + negCurvCount);
					System.out.println("Neg. Curvature Intensity Avg = " + negCurvSum/maxIntensity/negCurvCount);
					System.out.println();

				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
