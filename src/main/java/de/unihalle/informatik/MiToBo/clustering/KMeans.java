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

package de.unihalle.informatik.MiToBo.clustering;

import java.util.ArrayList;

import weka.clusterers.*;
import weka.core.Instances;
import weka.core.Attribute;
import weka.core.Instance;
import weka.core.DenseInstance;
import weka.filters.unsupervised.attribute.Remove;
import de.unihalle.informatik.Alida.annotations.ALDAOperator;
import de.unihalle.informatik.Alida.annotations.ALDAOperator.Level;
import de.unihalle.informatik.Alida.annotations.Parameter;
import de.unihalle.informatik.Alida.annotations.Parameter.ExpertMode;
import de.unihalle.informatik.Alida.exceptions.ALDOperatorException;
import de.unihalle.informatik.Alida.exceptions.ALDOperatorException.OperatorExceptionType;
import de.unihalle.informatik.MiToBo.core.operator.MTBOperator;
import de.unihalle.informatik.MiToBo.gui.MTBTableModel;

/**
 * Clustering of (feature) vectors with k-means based on Weka library.
 * 
 * @author Birgit Moeller
 * @author Elisabeth Piltz
 */
@ALDAOperator(genericExecutionMode=ALDAOperator.ExecutionMode.NONE,
		level=Level.STANDARD)
public class KMeans extends MTBOperator {

	/**
	 * Data to be clustered.
	 * <p>
	 * Each line of the table represents one vector, headers encode attributes.
	 */
	@Parameter( label= "Data set", required = true, dataIOOrder = 0, 
			direction = Parameter.Direction.IN, mode = ExpertMode.STANDARD, 
			description = "Data set to be clustered.")
	private MTBTableModel data = null;

	/**
	 * Number of clusters to be used.
	 */
	@Parameter( label= "Number of clusters", required = true, 
			dataIOOrder = 1, direction = Parameter.Direction.IN, 
			mode = ExpertMode.STANDARD, description = "Number of clusters.")
	private int clusterNum = 3;

	/**
	 * Exclude list with indices of attributes to ignore.
	 * <p>
	 * Note that the attribute indices start with 1.
	 */
	@Parameter( label= "Exclude list", required = true, 
			dataIOOrder = 2, direction = Parameter.Direction.IN, 
			mode = ExpertMode.ADVANCED, description = "Indices to exclude.")
	private int[] excludeList = null;

	/**
	 * List of assigned cluster indices, starting with 1.
	 */
	@Parameter( label= "Cluster labels", dataIOOrder = 0, 
			direction = Parameter.Direction.OUT, description = "Labels.")
	private transient MTBTableModel clusterLabels = null;

	/**
	 * Default constructor.
	 *  @throws ALDOperatorException
	 */
	public KMeans() throws ALDOperatorException {
		// nothing to do here
	}		

	/**
	 * This method does the actual work.
	 * @throws ALDOperatorException 
	 */
	@Override
	protected void operate() throws ALDOperatorException {
		
		// prepare Weka data set
		Instances dataSet = this.prepareWekaData();
		
		// run k-means
		try {
			this.doClustering(dataSet);
		} catch (Exception ex) {
			ex.printStackTrace();
			throw new ALDOperatorException(OperatorExceptionType.OPERATE_FAILED,
					"[KMeans] Something went wrong in Weka k-means clustering...");
		}
	}

	/**
	 * Converts the given data set into Weka instances.
	 * @return 	Weka data set.
	 */
	private Instances prepareWekaData() {
		
		// extract attributes from input data
		int cols = this.data.getColumnCount();
		ArrayList<Attribute> attribs = new ArrayList<Attribute>(cols);
		for (int i=0; i<cols; ++i) {
			attribs.add(new Attribute(this.data.getColumnName(i)));
		}

		// create instance data set
		Instances dataSet = new Instances("dataSet", attribs, 1);
		Instance item = null;
		for(int i=0; i < this.data.getRowCount(); ++i) {
			item = new DenseInstance(cols);
			for (int a=0; a<cols; ++a) {
				item.setValue(attribs.get(a), 
						((Double)(this.data.getValueAt(i, a))).doubleValue());
			}
			dataSet.add(item);	
		}
		return dataSet;
	}
		
	/**
	 * Does the actual clustering.
	 * @param dataSet		Data set to be clustered.
	 * @throws Exception
	 */
	private void doClustering(Instances dataSet) throws Exception {
		
		SimpleKMeans clusterer = new SimpleKMeans();
		clusterer.setNumClusters(this.clusterNum);
		clusterer.setSeed(0);
		FilteredClusterer fClusterer = new FilteredClusterer();
		fClusterer.setClusterer(clusterer);

		// exclude attributes
		Remove filter = new Remove();
		filter.setInputFormat(dataSet);
		if (this.excludeList != null) {
			StringBuffer excludeString = new StringBuffer();
			for (int i=0; i<this.excludeList.length; ++i) { 
				excludeString.append(Integer.toString(this.excludeList[i]));
				if (i < this.excludeList.length - 1)
					excludeString.append(",");
			}
			filter.setAttributeIndices(excludeString.toString());
		}
		fClusterer.setFilter(filter);
		
		// run k-means
		if (this.verbose.booleanValue())
			System.out.print("[KMeans] Running algorithm...");
		fClusterer.buildClusterer(dataSet);
		if (this.verbose.booleanValue())
			System.out.println("done!");

		// prepare return data
		this.clusterLabels = new MTBTableModel(
				this.data.getRowCount(), this.data.getColumnCount() + 1);
		for (int i=0;i<this.data.getColumnCount();++i)
			this.clusterLabels.setColumnName(i, this.data.getColumnName(i));
		this.clusterLabels.setColumnName(this.data.getColumnCount(), "Label");
		int clusterID;
		for (int i = 0; i < dataSet.numInstances(); i++) {
			for (int j = 0; j < this.data.getColumnCount(); ++j)
				this.clusterLabels.setValueAt(this.data.getValueAt(i, j),i,j);
			clusterID = fClusterer.clusterInstance(dataSet.instance(i));
			// Attention, cluster IDs start with 0, but should be 1!
			this.clusterLabels.setValueAt(
				new Integer(clusterID+1), i, this.data.getColumnCount());
		}			
	}
	
	/**
	 * Specify input data.
	 * @param inputData		Data to be clustered.
	 */
	public void setInputData(MTBTableModel inputData) {
		this.data = inputData;
	}
	
	/**
	 * Specify number of clusters.
	 * @param num		Number of clusters to be applied.
	 */
	public void setClusterNum(int num) {
		this.clusterNum = num;
	}
	
	/**
	 * Specify attributes to be excluded.
	 * @param ids		List of IDs to be ignored.
	 */
	public void setExcludeList(int[] ids) {
		this.excludeList = ids;
	}
	
	/**
	 * Get assigned labels.
	 * @return	Table with cluster labels.
	 */
	public MTBTableModel getDataLabels() {
		return this.clusterLabels;
	}
}
