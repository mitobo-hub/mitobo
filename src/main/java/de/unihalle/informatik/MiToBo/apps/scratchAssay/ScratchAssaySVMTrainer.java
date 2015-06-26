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

package de.unihalle.informatik.MiToBo.apps.scratchAssay;

import ij.IJ;

import java.io.File;
import java.io.IOException;
import java.util.Vector;

import libsvm.svm;
import libsvm.svm_model;
import libsvm.svm_node;
import libsvm.svm_parameter;
import libsvm.svm_problem;
import loci.common.services.DependencyException;
import loci.common.services.ServiceException;
import loci.formats.FormatException;
import de.unihalle.informatik.Alida.dataio.provider.swing.components.ALDTableWindow;
import de.unihalle.informatik.Alida.datatypes.ALDDirectoryString;
import de.unihalle.informatik.Alida.exceptions.ALDOperatorException;
import de.unihalle.informatik.Alida.exceptions.ALDProcessingDAGException;
import de.unihalle.informatik.Alida.exceptions.ALDWorkflowException;
import de.unihalle.informatik.Alida.annotations.ALDAOperator;
import de.unihalle.informatik.Alida.annotations.Parameter;
import de.unihalle.informatik.Alida.annotations.ALDAOperator.Level;
import de.unihalle.informatik.Alida.annotations.Parameter.ExpertMode;
import de.unihalle.informatik.Alida.annotations.Parameter.ParameterModificationMode;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage;
import de.unihalle.informatik.MiToBo.core.operator.MTBOperator;
import de.unihalle.informatik.MiToBo.gui.MTBTableModel;
import de.unihalle.informatik.MiToBo.io.images.ImageReaderMTB;


/**
 * class for training and cross validating a support vector machine for classification of scratch assay<br/> 
 * images into images containing a scratch and those that don't
 *   
 * @author glass
 *
 */
@ALDAOperator(genericExecutionMode=ALDAOperator.ExecutionMode.SWING, level=Level.STANDARD, allowBatchMode = false)
public class ScratchAssaySVMTrainer extends MTBOperator
{
	@Parameter(label = "directory containing positive samples", required = true, direction = Parameter.Direction.IN, supplemental = false, description = "directory containing positive samples", mode = ExpertMode.STANDARD, dataIOOrder = 0)
	private ALDDirectoryString posDir = new ALDDirectoryString(IJ.getDirectory("current"));
	
	@Parameter(label = "directory containing negative samples", required = true, direction = Parameter.Direction.IN, supplemental = false, description = "directory containing negative samples", mode = ExpertMode.STANDARD, dataIOOrder = 1)
	private ALDDirectoryString negDir = new ALDDirectoryString(IJ.getDirectory("current"));

	@Parameter(label = "scratch orientation", required = true, direction = Parameter.Direction.IN, supplemental = false, description = "horizontally or vertically oriented scratch", mode = ExpertMode.STANDARD, dataIOOrder = 2)
	private ScratchOrientation orientation = ScratchOrientation.HORIZONTALLY;
	
	@Parameter(label = "\u03C3", required = true, direction = Parameter.Direction.IN, supplemental = false, description = "standard deviation of gauss filter", mode = ExpertMode.STANDARD, dataIOOrder = 3)
	private Integer sigma = 2;
	
	@Parameter(label = "entropy filter size", required = true, direction = Parameter.Direction.IN, supplemental = false, description = "size of entropy filter mask", mode = ExpertMode.STANDARD, dataIOOrder = 4)
	private Integer entropyFilterSize = 25;
	
	@Parameter(label = "regularization parameter (C)", required = false, direction = Parameter.Direction.IN, supplemental = false, description = "regularization parameter", mode = ExpertMode.STANDARD, dataIOOrder = 5)
	private Integer C = 100;
	
	@Parameter(label = "kernel type", required = false, direction = Parameter.Direction.IN, supplemental = false, description = "type of svm kernel", mode = ExpertMode.STANDARD, dataIOOrder = 6,
			callback = "showDegreeTextbox", paramModificationMode = ParameterModificationMode.MODIFIES_INTERFACE)
	private KERNEL_TYPE type = KERNEL_TYPE.LINEAR;
	
	@Parameter(label = "polynomial degree", required = false, direction = Parameter.Direction.IN, supplemental = false, description = "degree for polynomial svm kernel", mode = ExpertMode.STANDARD, dataIOOrder = 7)
	private Integer degree = 2;
	
	@Parameter(label = "validation method", required = false, direction = Parameter.Direction.IN, supplemental = false, description = "validation method for x-validation", mode = ExpertMode.STANDARD, dataIOOrder = 8,
			callback = "showKTextbox", paramModificationMode = ParameterModificationMode.MODIFIES_INTERFACE)
	private ValidationMethod method = ValidationMethod.NONE;
	
	@Parameter(label = "k (for k-fold x-validation)", required = false, direction = Parameter.Direction.IN, supplemental = false, description = "k for k-fold cross validation", mode = ExpertMode.STANDARD, dataIOOrder = 9)
	private Integer k = 2;
	
	@Parameter(label = "maximum iterations", required = false, direction = Parameter.Direction.IN, supplemental = false, description = "maximum number of iterations of level set segmentation", mode = ExpertMode.ADVANCED, dataIOOrder = 10)
	private Integer maxIter = 2000;

	
	public enum ValidationMethod	// available cross validation methods
	{
		NONE,
		K_FOLD,
		LEAVE_ONE_OUT
	}
	
	public enum KERNEL_TYPE
	{
		LINEAR, 
		RADIAL, 
		POLYNOMIAL
	}
	
	public enum ScratchOrientation
	{
		HORIZONTALLY,
		VERTICALLY
	}
	
	
	// training parameters	
	private svm_parameter param;
	private svm_problem prob;		
	private svm_model model;
	private String error_msg;
	private Vector<Double> vy = new Vector<Double>();
	private Vector<svm_node[]> vx = new Vector<svm_node[]>();
	
	// cross validation variables
	private int fp = 0;	// number of false positives
	private int fn = 0;	// number of false negatives
	private int tp = 0;	// number of true positives
	private int tn = 0;	// number of true negatives
	
	StringBuffer falsePositiveFileNames = new StringBuffer("");
	StringBuffer falseNegativeFileNames = new StringBuffer("");
	StringBuffer falseFileNames = new StringBuffer("");
	
	private Boolean isHorizontal = true;
	
	String outFile;
	
	
	public ScratchAssaySVMTrainer() throws ALDOperatorException
	{
		
	}
	
	
	/**
	 * 
	 * @param posDir	directory containing positive samples
	 * @param negDir	directory containing negative samples
	 * @param sigma		standard deviation of gauss filter
	 * @param entropyFilterSize	size of entropy filter mask
	 * @param isHorizontal	is scratch horizontally oriented (assumed to be vertically oriented else)
	 * @param maxIter	maximum number of iterations for level set segmentation
	 * @throws ALDOperatorException
	 */
	public ScratchAssaySVMTrainer(String posDir, String negDir, int sigma, int entropyFilterSize, boolean isHorizontal, int maxIter) throws ALDOperatorException
	{
		this.posDir = new ALDDirectoryString(posDir);
		this.negDir = new ALDDirectoryString(negDir);
		this.sigma = sigma;
		this.entropyFilterSize = entropyFilterSize;
		this.isHorizontal = isHorizontal;
		this.maxIter = maxIter;
	}
		

	@Override
	public void operate() throws ALDOperatorException, ALDProcessingDAGException
	{
		switch(orientation)
		{
			case HORIZONTALLY:
				isHorizontal = true;
				break;
			case VERTICALLY:
				isHorizontal = false;
				break;
		}
		
		// initialize svm parameter
		param = new svm_parameter();
		
		param.C = C;
		
		switch(type)
		{
			case LINEAR:
				param.kernel_type = svm_parameter.LINEAR;
				break;
			case RADIAL:
				param.kernel_type = svm_parameter.RBF;
				break;
			case POLYNOMIAL:
				param.kernel_type = svm_parameter.POLY;
				break;
			default:
				param.kernel_type = svm_parameter.LINEAR;
				break;
		}
		
		param.degree = degree;
		
		param.svm_type = svm_parameter.C_SVC;
		param.gamma = 1d/3d;	// 1 / number of features
		param.coef0 = 0;
		param.nu = 0.5;
		param.cache_size = 100;
		
		param.eps = 1e-3;
		param.p = 0.1;
		param.shrinking = 1;
		param.probability = 0;
		param.nr_weight = 0;
		param.weight_label = new int[0];
		param.weight = new double[0];
		
		switch(method)
		{
			case NONE:	// just train the svm
			{
				File[] posSamples = new File(posDir.getDirectoryName()).listFiles();
				extract(posSamples, 1);
				
				File[] negSamples = new File(negDir.getDirectoryName()).listFiles();
				extract(negSamples, -1);
			
				train();
				
				saveSVM();
			}
			break;
			case K_FOLD:	// stratified k-fold cross validation
			{
				stratifiedCrossValidation();
				
				MTBTableModel resultsTable = makeTable();
				ALDTableWindow tw = new ALDTableWindow(resultsTable);
				tw.setTitle("results");
				tw.openWindow();
			}
			break;
			case LEAVE_ONE_OUT:	// leave-one-out cross validation
			{
				leaveOneOutCrossValidation();
				
				MTBTableModel resultsTable = makeTable();
				ALDTableWindow tw = new ALDTableWindow(resultsTable);
				tw.setTitle("results");
				tw.openWindow();
			}
			break;
		}
		
		
	}
	
	
	/**
	 * segment the given images and extract their features for classification
	 * 
	 * @param samples 	array of filenames containing images
	 * @param label		label of the input images
	 */
	private void extract(File[] samples, double label)
	{
		if(verbose)
		{
			System.out.println("extracting features");
		}
		
		int n = samples.length;
		
		for(int i = 0; i < n; i++)
		{
			IJ.showProgress(0);
			
			File currFile = samples[i];
			
			if(currFile.isFile())
			{
				String path = currFile.getAbsolutePath();
				
					
				try
				{
					MTBImage currImg = readImage(path);

					if(currImg != null)
					{
						ScratchAssaySegmenter segmenter = new ScratchAssaySegmenter(currImg, sigma, entropyFilterSize, 
																					isHorizontal, false, maxIter);
						segmenter.runOp();

						double[] features = segmenter.getScratchFeatures();

						if(verbose)
						{
							System.out.println(path + ": "+"ed: " + features[0] + ", bc: " + features[1] + ", ks: " + features[2]);
							System.out.println();
						}

						addFeatures(label, features);
					}
					
				}
				catch(NullPointerException e)
				{
					e.printStackTrace();
				}
				catch(ALDOperatorException e)
				{
					e.printStackTrace();
				} 
				catch(ALDProcessingDAGException e)
				{
					e.printStackTrace();
				}
				
				
				//currImg.close();
			}
		
			IJ.showProgress(i, n);
		}
	}
	
	
	/**
	 * segment the given images and extract their features for classification
	 * 
	 * @param samples	vector of filenames containing images
	 * @param label		label of the input images
	 */
	private void extract(Vector<File> samples, double label)
	{
		if(verbose)
		{
			System.out.println("extracting features");
		}
		
		for(int i = 0; i < samples.size(); i++)
		{
			File currFile = samples.elementAt(i);
			
			if(currFile.isFile())
			{
				String path = currFile.getAbsolutePath();
				
				try
				{
					MTBImage currImg = readImage(path);
					
					if(currImg != null)
					{
						ScratchAssaySegmenter segmenter = new ScratchAssaySegmenter(currImg, sigma, entropyFilterSize, 
																					isHorizontal, false, maxIter);
						
						segmenter.runOp();

						double[] features = segmenter.getScratchFeatures();

						if(verbose)
						{
							System.out.println(path + ": "+"ed: " + features[0] + ", bc: " + features[1] + ", ks: " + features[2]);
							System.out.println();
						}
						
						addFeatures(label, features);
					}
					
				}
				catch(NullPointerException e)
				{
					e.printStackTrace();
				}
				catch(ALDOperatorException e)
				{
					e.printStackTrace();
				} 
				catch(ALDProcessingDAGException e)
				{
					e.printStackTrace();
				}
				
				//currImg.close();
			}
		
		}
	}
	
	
	/**
	 * the actual svm training
	 */
	private void train()
	{	
		prob = new svm_problem();
		prob.l = vy.size();
		prob.x = new svm_node[prob.l][];
		
		for(int i = 0; i < prob.l; i++)
		{
			prob.x[i] = vx.elementAt(i);
		}
		
		prob.y = new double[prob.l];
		
		for(int i = 0;i < prob.l; i++)
		{
			prob.y[i] = vy.elementAt(i);
		}
		
		error_msg = svm.svm_check_parameter(prob, param);

		if(error_msg != null)
		{
			System.err.print("Error: " + error_msg + "\n");
			//System.exit(1);
		}

		model = svm.svm_train(prob, param);	// the actual training
	}

	
	/**
	 * save resulting svm model file
	 */
	private void saveSVM()
	{
		outFile = posDir.getDirectoryName() + "/svm_k" + param.kernel_type + "_C" + param.C +".txt";
		
		try
		{
			svm.svm_save_model(outFile, model);
			
			System.out.println("model file saved to " + outFile);
		} 
		catch(IOException e)
		{
			e.printStackTrace();
		}
	}
	
	/**
	 * 
	 * @param path
	 * @return image read from given path, if it is a valid image path
	 */
	private MTBImage readImage(String path)
	{		
		MTBImage img = null;
		
		try
		{
			ImageReaderMTB reader = new ImageReaderMTB(path);
			reader.runOp();
			
			img = reader.getResultMTBImage();
		} 
		catch(ALDOperatorException e1)
		{
			e1.printStackTrace();
		} 
		catch(FormatException e1)
		{
			e1.printStackTrace();
		} 
		catch(IOException e1)
		{
			e1.printStackTrace();
		} 
		catch(DependencyException e1)
		{
			e1.printStackTrace();
		} 
		catch(ServiceException e1)
		{
			e1.printStackTrace();
		} 
		catch(ALDProcessingDAGException e)
		{
			e.printStackTrace();
		}
		
		return img;
	}
	
	
	/**
	 * add the given patterns (feature arrays) together with their corresponding label to the 
	 * existing patterns used to train the SVM
	 *  
	 * @param label	label for the givven features
	 * @param features	array of feature values
	 */
	private void addFeatures(double label, double[] features)
	{
		int m = features.length;
		
		svm_node[] x = new svm_node[m];
		
		for(int j = 0; j < m; j++)
		{
			x[j] = new svm_node();
			x[j].index = j+1;
			x[j].value = features[j];
		}
		
		vy.add(label);
		vx.add(x);
	}
	
	
	/**
	 * 
	 * @param param
	 */
	public void setSVMParameters(svm_parameter param)
	{
		this.param.kernel_type = param.kernel_type;
		this.param.C = param.C;
		this.degree = param.degree;
	}
	
	
	
	/**
	 * stratified k-fold cross validation, i.e. training and test tests contain 
	 * approximately the same proportions of positive and negative samples in 
	 * every iteration 
	 */
	private void stratifiedCrossValidation()
	{
		System.out.println("--- starting " + k + "-fold cross validation ---");
		File[] tempPosSamples = new File(posDir.getDirectoryName()).listFiles();
		File[] tempNegSamples = new File(negDir.getDirectoryName()).listFiles();
		
		Vector<File> posSamples = new Vector<File>();
		Vector<File> negSamples = new Vector<File>();
		
		// filter files
		for(int i = 0; i < tempPosSamples.length; i++)
		{
			File currFile = tempPosSamples[i];
			
			if(currFile.isFile())
			{
				posSamples.add(tempPosSamples[i]);
			}
		}
		
		for(int i = 0; i < tempNegSamples.length; i++)
		{
			File currFile = tempNegSamples[i];
			
			if(currFile.isFile())
			{
				negSamples.add(tempNegSamples[i]);
			}
		}
		
		int n = posSamples.size();
		int m = negSamples.size();

		if(k > 1 && k <= n && k <= m)
		{
			// starting k-fold cross validation
			
			int nn = n/k;
			int mm = m/k;
			
			for(int i = 0; i < k; i++)
			{
				IJ.showProgress(0);
				
				System.out.println("run no. " + (i+1));
				
				vy = new Vector<Double>();
				vx = new Vector<svm_node[]>();
				
				// split files in training and test data
				Vector<File> posTrain = new Vector<File>();
				Vector<File> posTest = new Vector<File>();
				Vector<File> negTrain = new Vector<File>();
				Vector<File> negTest = new Vector<File>();
				
				for(int j = 0; j < n; j++)
				{
					if(j >= (nn * i) && j < (nn * (i+1)))
					{
						posTest.add(posSamples.elementAt(j));
					}
					else
					{
						posTrain.add(posSamples.elementAt(j));
					}
				}
				
				for(int j = 0; j < m; j++)
				{
					if(j >= (mm * i) && j < (mm * (i+1)))
					{
						negTest.add(negSamples.elementAt(j));
					}
					else
					{
						negTrain.add(negSamples.elementAt(j));
					}
				}
				
				if(verbose)
				{
					System.out.println("test samples:");
					for(int l = 0; l < posTest.size(); l++)
					{
						System.out.print(posTest.elementAt(l) + " | ");
					}
					System.out.println();
					for(int l = 0; l < negTest.size(); l++)
					{
						System.out.print(negTest.elementAt(l) + " | ");
					}
					System.out.println();
				}
				
				// exractFeatures of subsets
				System.out.println("extracting features ...");
				extract(negTrain, -1);
				extract(posTrain, 1);
				
				System.out.println("training phase ...");
				
				train();
				
				System.out.println("test phase ...");
				int[] posResults = test(posTest, 1);
				
				falseNegativeFileNames.append(falseFileNames);
				
				int[] negResults = test(negTest, -1);
				
				falsePositiveFileNames.append(falseFileNames);
				
				tp += posResults[0];
				fn += posResults[1];
				tn += negResults[0];
				fp += negResults[1];
				
				if(verbose)
				{
					System.out.println("true positives: " + posResults[0]);
					System.out.println("true negatives: " + negResults[0]);
					System.out.println("false positives: " + negResults[1]);
					System.out.println("false negatives: " + posResults[1]);
				}
				
				
				IJ.showProgress(i,k);
			}
			
			if(verbose)
			{
				System.out.println();
				System.out.println("overall results:");
				System.out.println("true positives: " + tp);
				System.out.println("true negatives: " + tn);
				System.out.println("false positives: " + fp);
				System.out.println("false negatives: " + fn);
				System.out.println();
				System.out.println("false positives: " + falsePositiveFileNames);
				System.out.println("false negatives: " + falseNegativeFileNames);
			}
			
		}
		else
		{
			System.err.println("at least one training/testing subset is empty, aborting cross validation");
			IJ.showMessage("at least one training/testing subset is empty, aborting cross validation");
		}
		
	}
	
	
	/**
	 * leave-one-out cross validation, i.e. iteratively one sample is taken as test <br/>
	 * sample whereas all others are used for training
	 *
	 */
	private void leaveOneOutCrossValidation()
	{
		System.out.println("starting leave-one-out cross validation");
		
		File[] tempPosSamples = new File(posDir.getDirectoryName()).listFiles();
		File[] tempNegSamples = new File(negDir.getDirectoryName()).listFiles();
		
		Vector<File> posSamples = new Vector<File>();
		Vector<File> negSamples = new Vector<File>();
		
		// filter files
		for(int i = 0; i < tempPosSamples.length; i++)
		{
			File currFile = tempPosSamples[i];
			
			if(currFile.isFile())
			{
				posSamples.add(tempPosSamples[i]);
			}
		}
		
		for(int i = 0; i < tempNegSamples.length; i++)
		{
			File currFile = tempNegSamples[i];
			
			if(currFile.isFile())
			{
				negSamples.add(tempNegSamples[i]);
			}
		}
		
		int n = posSamples.size();
		int m = negSamples.size();
		int iter = 0;
		
		IJ.showProgress(0);
		
		// test positive samples
		for(int i = 0; i < n; i++)
		{
			iter++;
			System.out.println("run no. " + iter + " of " + (n+m));
			
			vy = new Vector<Double>();
			vx = new Vector<svm_node[]>();
			
			// split files in training and test data
			Vector<File> posTrain = new Vector<File>();
			Vector<File> posTest = new Vector<File>();
			Vector<File> negTrain = new Vector<File>();
			
			for(int j = 0; j < n; j++)
			{
				if(j == i)
				{
					posTest.addElement(posSamples.elementAt(j));
					
					if(verbose)
					{
						System.out.println("current test image: " + posSamples.elementAt(j));
					}
				}
				else
				{
					posTrain.addElement(posSamples.elementAt(j));
				}
			}
			
			negTrain = negSamples;
			
			// extract features of subsets
			System.out.println("extracting features ...");
			extract(negTrain, -1);
			extract(posTrain, 1);
			
			System.out.println("training phase ...");
			
			train();
			
			System.out.println("test phase ...");
			int[] posResults = test(posTest, 1);
			
			tp += posResults[0];
			fn += posResults[1];
			
			falseNegativeFileNames.append(falseFileNames);
			
			if(verbose)
			{
				System.out.println("true positives: " + posResults[0]);
				
				System.out.println("false negatives: " + posResults[1]);
			}
			
			
			IJ.showProgress(iter,(n + m));
		}
		
		
		// test negative samples
		for(int i = 0; i < m; i++)
		{
			iter++;
			System.out.println("run no. " + iter + " of " + (n+m));
			
			vy = new Vector<Double>();
			vx = new Vector<svm_node[]>();
			
			// split pattern in training and test data
			Vector<File> posTrain = new Vector<File>();
			Vector<File> negTrain = new Vector<File>();
			Vector<File> negTest = new Vector<File>();
			
			for(int j = 0; j < m; j++)
			{
				if(j == i)
				{
					negTest.addElement(negSamples.elementAt(j));
					
					System.out.println("current test image: " + negSamples.elementAt(j));
				}
				else
				{
					negTrain.addElement(negSamples.elementAt(j));
				}
			}
			
			posTrain = posSamples;
			
			// exractFeatures of subsets
			extract(negTrain, -1);
			extract(posTrain, 1);
			
			System.out.println("training phase ...");
			
			train();
			
			System.out.println("test phase ...");
			
			int[] negResults = test(negTest, -1);
			
			tn += negResults[0];
			fp += negResults[1];
			
			falsePositiveFileNames.append(falseFileNames);
			
			if(verbose)
			{
				System.out.println("true negatives: " + negResults[0]);
				System.out.println("false positives: " + negResults[1]);
			}
			
			IJ.showProgress(iter,(n + m));
		}
		
		if(verbose)
		{
			System.out.println();
			System.out.println("overall results:");
			System.out.println("true positives: " + tp);
			System.out.println("true negatives: " + tn);
			System.out.println("false positives: " + fp);
			System.out.println("false negatives: " + fn);
			System.out.println();
			System.out.println("false positives: " + falsePositiveFileNames);
			System.out.println("false negatives: " + falseNegativeFileNames);
		}	
		
	}
	
	/**
	 * 
	 * @param samples	Vector of sample files
	 * @param label		ground truth labels for the input images
	 * @return numbers of right and wrong classified images 
	 */
	private int[] test(Vector<File> samples, int label)
	{
		int right = 0;
		int wrong = 0;
		falseFileNames = new StringBuffer("");
		
		for(int i = 0; i < samples.size(); i++)
		{
			File currFile = samples.elementAt(i);
			
			if(currFile.isFile())
			{
				String path = currFile.getAbsolutePath();
				
				try
				{
					MTBImage currImg = readImage(path);
					
					if(currImg != null)
					{
						ScratchAssaySegmenter segmenter = new ScratchAssaySegmenter(currImg, sigma, entropyFilterSize, 
																					isHorizontal, false, maxIter);

						segmenter.runOp();

						double[] features = segmenter.getScratchFeatures();

						String fileName = currFile.getName();

						double v = classify(features);

						System.out.println(fileName + ": " + v);
						//System.out.println("bc: " + features[0] + ", ed: " + features[1] + ", ks: " + features[2]);
						//System.out.println();

						if(v == label)
						{
							right++;
						}
						else
						{
							wrong++;
							falseFileNames.append(fileName + "\n");
						}
					}
					
				}
				catch(NullPointerException e)
				{
					e.printStackTrace();
				}
				catch(ALDOperatorException e)
				{
					e.printStackTrace();
				} 
				catch(ALDProcessingDAGException e)
				{
					e.printStackTrace();
				}
				
			}
		
		}
		
		return new int[]{right, wrong};
	}
	
	
	/**
	 * 
	 * @param features
	 * @return	predicted class label
	 */
	private double classify(double[] features)
	{

		svm_node[] x = new svm_node[3];
		x[0] = new svm_node();
		x[0].index = 1;
		x[0].value = features[0];
		
		x[1] = new svm_node();
		x[1].index = 2;
		x[1].value = features[1];
		
		x[2] = new svm_node();
		x[2].index = 3;
		x[2].value = features[2];
		
		double v = svm.svm_predict(model,x);
		
		return v;
	}
	
	
	/**
	 * 
	 * @return table containing the cross validation results
	 */
	private MTBTableModel makeTable()
	{	
		// initialize table
		Vector<String> header = new Vector<String>();
		header.add("true positives");
		header.add("true negatives");
		header.add("false positives");
		header.add("false negatives");
		
		MTBTableModel table = new MTBTableModel(1, header.size(), header);
		
		table.setValueAt(tp, 0, 0);
		table.setValueAt(tn, 0, 1);
		table.setValueAt(fp, 0, 2);
		table.setValueAt(fn, 0, 3);
		
		return table;
	}
	
	
	// ------------------------------ callback functions ------------------------------
	
	@SuppressWarnings("unused")
	private void showDegreeTextbox()
	{
		try
		{
			if(type == KERNEL_TYPE.POLYNOMIAL)
			{
				if(!this.hasParameter("degree"))
				{
					this.addParameter("degree");
				}
			}
			else
			{
				if(this.hasParameter("degree"))
				{
					this.removeParameter("degree");
				}
			}
		}
		catch(ALDOperatorException e)
		{
			e.printStackTrace();
		}
	}
	
	
	@SuppressWarnings("unused")
	private void showKTextbox()
	{
		try
		{
			if(method == ValidationMethod.K_FOLD)
			{
				if(!this.hasParameter("k"))
				{
					this.addParameter("k");
				}
			}
			else
			{
				if(this.hasParameter("k"))
				{
					this.removeParameter("k");
				}
			}
		}
		catch(ALDOperatorException e)
		{
			e.printStackTrace();
		}
	}
}

/*BEGIN_MITOBO_ONLINE_HELP
<p><a target="_blank" href="http://www2.informatik.uni-halle.de/agprbio/mitobo//api/de/unihalle/informatik/MiToBo/apps/scratchAssay/ScratchAssaySVMTrainer.html">API</a></p>
<ul><li>
<p>This operator offers the possibilty to train or validate a support vector machine model for classifying scratch assay images into such containing a scratch and such that don't (already closed)</p>
</li><li>
<p>newly created models can be used in the <a href="de.unihalle.informatik.MiToBo.apps.scratchAssay.ScratchAssayAnalyzer.html">Scratch Assay Analyzer</a></p>
</li><li>
<p>In order to use this operator two directories have to be created, one containing images where there is a scratch visible (positive samples) and a second containing images without a scratch (negative samples)</p>
</li><li>
<p>all images should have the same dimensions and the scratches have to be oriented either all horizontally or all vertically  </p>
</li></ul>
<h2>Usage:</h2>
<h3>required parameters:</h3>

<ul>
	<li>
		<p><tt> directory containing positive samples</tt>
		<ul>
			<li>
				<p>directory containing scratch assay images where the scratch has not been closed</p>
			</li>
		</ul>
		</p>
	</li>
	<li>
		<p><tt>directory containing negative samples</tt> 
		<ul>
			<li>
				<p>directory containing scratch assay images where the scratch has already been closed</p>
			</li>
		</ul>
		</p>
	</li>
	<li>
		<p><tt>scratch orientation</tt>
		<ul>
			<li>
				<p>horizontally or </p>
			</li>
			<li>
				<p>vertically</p>
			</li>
		</ul>
		</p>
	</li>
	<li>
		<p><tt>entropy filter size</tt>
		<ul>
			<li>
				<p>size of entropy filter mask</p>
			</li>
			<li>
				<p>increase lets the scratch area decrease</p>
			</li>
		</ul>
		</p>
	</li>
	<li>
		<p><tt>sigma</tt>
	<ul>
		<li>
			<p>standard deviation of gauss filter</p>
		</li>
		<li>
			<p>increase leads to more image smoothing and scratch area tends to decrease</p>
		</li>
	</ul>
		</p>
	</li>
</ul>
<h3>optional parameters:</h3>

<ul>
	<li>
		<p>regularization parameter (C)
	<ul>
		<li>
			<p>determines severity of misclassification of outliers</p>
		</li>
	</ul>
		</p>
	</li>
	<li>
		<p>kernel type
	<ul>
		<li>
			<p>determines type of kernel for support vector machine</p>
		</li>
		<li>
			<p>available options are LINEAR, RADIAL and POLYNOMIAL</p>
		</li>
	</ul>
		</p>
	</li>
	<li>
		<p>degree
	<ul>
		<li>
			<p>degree of polynomial if polynomial kernel is chosen</p>
		</li>
	</ul>
		</p>
	</li>
	<li>
		<p>k
	<ul>
		<li>
			<p>fold number if k-fold cross validation is performed</p>
		</li>
	</ul>
		</p>
	</li>
	<li>
		<p>validation method
	<ul>
		<li>
			<p>which (if any) validation method shoulb be used</p>
		</li>
		<li>
			<p>available options are 
		<ul>
			<li>
				<p>NONE: train a new svm model</p>
			</li>
			<li>
				<p>K&nbsp;FOLD: stratified k-fold cross validation</p>
			</li>
			<li>
				<p>LEAVE&nbsp;ONE&nbsp;OUT: leave-one-out cross validation</p>
			</li>
		</ul>
			</p>
		</li>
	</ul>
	</p>
	</li>
	<li>
		<p><tt>maximum iterations</tt>
	<ul>
		<li>
			<p>maximum number of iterations for level set segmentation</p>
		</li>
	</ul>
		</p>
	</li>
</ul>
<h3>supplemental parameters:</h3>

<ul>
	<li>
		<p><tt>Verbose</tt>
	<ul>
		<li>
			<p>output some additional information</p>
		</li>
	</ul>
		</p>
	</li>
</ul>
END_MITOBO_ONLINE_HELP*/
