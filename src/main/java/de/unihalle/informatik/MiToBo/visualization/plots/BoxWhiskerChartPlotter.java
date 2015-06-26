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

package de.unihalle.informatik.MiToBo.visualization.plots;

import de.unihalle.informatik.Alida.annotations.ALDAOperator;
import de.unihalle.informatik.Alida.annotations.ALDAOperator.Level;
import de.unihalle.informatik.Alida.annotations.Parameter;
import de.unihalle.informatik.Alida.annotations.Parameter.Direction;
import de.unihalle.informatik.Alida.annotations.Parameter.ExpertMode;
import de.unihalle.informatik.Alida.exceptions.ALDOperatorException;
import de.unihalle.informatik.Alida.exceptions.ALDProcessingDAGException;
import de.unihalle.informatik.MiToBo.core.operator.MTBOperator;

import java.awt.Font;
import java.util.HashMap;

import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.labels.BoxAndWhiskerToolTipGenerator;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.renderer.category.BoxAndWhiskerRenderer;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.statistics.BoxAndWhiskerCategoryDataset;
import org.jfree.data.statistics.DefaultBoxAndWhiskerCategoryDataset;

import java.util.Collections;
import java.util.LinkedList;
import java.util.Set;

/**
 * Operator that generates box-whisker plots using JFreeChart.
 * <p>
 * For details on the library take a look here:
 * <a href="http://www.jfree.org/jfreechart/">
 * 	http://www.jfree.org/jfreechart/
 * </a>
 * <p>
 * The operator takes input data and generates a box-whisker plot of the data,
 * optionally grouping the boxes into different categories. In addition, the
 * operator provides various parameters to adjust the plot's appearance, 
 * like title of the plot, axes labels, or the font size of the tick labels on 
 * the axes. 
 * <p>
 * As input data the operator takes a hash map which should contain a key for 
 * each category to be displayed, i.e., each group of boxes to appear in the
 * plot, and as values the corresponding data. The data is also expected to be
 * arranged in a hash map where the key string indicates the name of the 
 * variable or indicator, and the values are given in terms of a list of 
 * doubles. 
 * <p>
 * For an example how the data should look like, consider the following: 
 * assume that we are given two groups of cells, i.e., two categories g1 and g2, 
 * and each group contains 30 elements. For each element two measurements (or 
 * 'indicators' in this context) are given, denoted m1 and m2. We would like
 * to visualize for each group the distribution of the two measurements, i.e.,
 * get a box plot with two categories and two boxes in each category. 
 * Consequently, we have to pass a hash map to the operator containing
 * two key-value pairs, one for each category or group, respectively:<br>
 * [g1, hash_1] , [g2, hash_2]<br>
 * In each group hash, key-value pairs are expected where the key refers to the 
 * indicator and the value, given in terms of a list, contains the 
 * measurements of the indicator for the elements of that group. 
 * In this example each hash, thus, contains two key-value pairs where the keys 
 * are given by m1 and m2, and each value is given by a list of 30 double 
 * entries: <br>
 * [m1, [v1, ..., v30] ] , [m2, [v1, ..., v30] ]
 * <p>
 * For more details on box-whisker plots in JFreeChart, e.g., refer to<br>
 * <a href= 
 * "http://www.jfree.org/jfreechart/api/javadoc/org/jfree/chart/renderer/category/BoxAndWhiskerRenderer.html">
 * http://www.jfree.org/jfreechart/api/javadoc/org/jfree/chart/renderer/category/BoxAndWhiskerRenderer.html
 * </a>
 * 
 * @author moeller
 */
@ALDAOperator(genericExecutionMode=ALDAOperator.ExecutionMode.NONE,
	level=Level.STANDARD, allowBatchMode=false)
public class BoxWhiskerChartPlotter extends MTBOperator {
	
	/**
	 * Data to display.
	 */
	@Parameter( label= "Data", required = true, 
		dataIOOrder = -1, direction = Direction.IN, 
		description = "Data.", mode = ExpertMode.STANDARD)
	protected HashMap< String, HashMap<String, LinkedList<Double>> > data;
	
	/**
	 * Title of the chart.
	 */
	@Parameter( label= "Chart title", required = false, 
		dataIOOrder = 1, direction = Direction.IN, 
		description = "Chart title.", mode = ExpertMode.STANDARD)
	protected String chartTitle = "";
	
	/**
	 * Label of the x-axis.
	 */
	@Parameter( label= "X-axis label", required = false, 
		dataIOOrder = 2, direction = Direction.IN, 
		description = "Label of x-axis.", mode = ExpertMode.STANDARD)
	protected String xLabel = "";
	
	/**
	 * Label of the y-axis.
	 */
	@Parameter( label= "Y-axis label", required = false, 
		dataIOOrder = 3, direction = Direction.IN, 
		description = "Label of y-axis.", mode = ExpertMode.STANDARD)
	protected String yLabel = "";

	/**
	 * Size of axes' tick labels.
	 */
	@Parameter( label= "Tick label size", required = false, 
		dataIOOrder = 4, direction = Direction.IN, mode = ExpertMode.STANDARD,
		description = "Size of tick labels on both axes.")
	protected int tickLabelSize = 8;

	/**
	 * Width of whiskers.
	 */
//	@Parameter( label= "Width of whiskers", required = false, 
//		dataIOOrder = 5, direction = Direction.IN, mode = ExpertMode.STANDARD,
//		description = "Width of whisker bars.")
//	protected int whiskerWidth = 2;

	/**
	 * Resulting box-whisker chart.
	 */
	@Parameter( label= "Box-whisker chart plot",  
		dataIOOrder = 1, direction = Direction.OUT, 
		description = "Resulting plot.", mode = ExpertMode.STANDARD)
	protected JFreeChart boxWhiskerChart;
	
	/**
	 * Default constructor.
	 * @throws ALDOperatorException
	 */
	public BoxWhiskerChartPlotter() throws ALDOperatorException {
	  super();
  }
	
	/**
	 * Set the data to plot.
	 * @param d		Data to plot.
	 */
	public void setData(HashMap< String,HashMap<String, LinkedList<Double>>> d) {
		this.data = d;
	}
	
	/**
	 * Set title of chart plot.
	 * @param title		Title string.
	 */
	public void setTitle(String title) {
		this.chartTitle = title;
	}
	
	/**
	 * Set label of x-axis.
	 * @param xlabel	Label string.
	 */
	public void setXAxisLabel(String xlabel) {
		this.xLabel = xlabel;
	}

	/**
	 * Set label of y-axis.
	 * @param ylabel	Label string.
	 */
	public void setYAxisLabel(String ylabel) {
		this.yLabel = ylabel;
	}

	/**
	 * Set size of tick labels on both axes.
	 * @param size	Size of tick labels.
	 */
	public void setTickLabelSize(int size) {
		this.tickLabelSize = size;
	}

	/**
	 * Set width of whisker bars.
	 * @param width		Width of whiskers.
	 */
//	public void setWhiskerWidth(int width) {
//		this.whiskerWidth = width;
//	}
	
	/**
	 * Get reference to resulting chart.
	 * @return	Generated chart plot.
	 */
	public JFreeChart getChart() {
		return this.boxWhiskerChart;
	}
	
  @Override
	@SuppressWarnings("unused")
  protected void operate() throws ALDOperatorException,
      ALDProcessingDAGException {
  	final BoxAndWhiskerCategoryDataset dataset = createDataset();
  	this.boxWhiskerChart = this.createChart(dataset);
  	this.boxWhiskerChart.setTitle(this.chartTitle);
  }
  
  /**
   * Create the dataset from the given input data.
   * @return Resulting dataset.
   */
  private BoxAndWhiskerCategoryDataset createDataset() {

  	// initialize result dataset object
    final DefaultBoxAndWhiskerCategoryDataset dataset 
    	= new DefaultBoxAndWhiskerCategoryDataset();

  	// iterate over all categories in the given input data 
  	Set<String> category = this.data.keySet();
  	// add the data of each item to the dataset
  	for (String c: category) {
  		HashMap<String,LinkedList<Double>> itemValues = this.data.get(c);
  		LinkedList<String> sortedItemVals = 
  			this.sortStringSet(itemValues.keySet());
  		for (String i: sortedItemVals) {
        dataset.add(itemValues.get(i), i, c);
  		}
  	}
    return dataset;
  }
  
  /**
   * Creates the chart.
   * 
   * @param dataset  The dataset for the chart.
   * @return Resulting chart.
   */
  private JFreeChart createChart(final CategoryDataset dataset) {

  	final CategoryAxis xAxis = new CategoryAxis(this.xLabel);
  	final NumberAxis yAxis = new NumberAxis(this.yLabel);
  	yAxis.setAutoRangeIncludesZero(false);
  	final BoxAndWhiskerRenderer renderer = new BoxAndWhiskerRenderer();
  	renderer.setFillBox(true);
  	renderer.setBaseToolTipGenerator(new BoxAndWhiskerToolTipGenerator());
  	final CategoryPlot plot = new CategoryPlot(dataset, xAxis, yAxis, renderer);
  	final JFreeChart chart = new JFreeChart(this.chartTitle,
 			new Font("SansSerif", Font.BOLD, 14), plot,	true);
    return chart;
  }
  
  /**
   * Helper method to sort set of strings alphabetically in ascending order.
   * @param input	Set of strings to sort.
   * @return	Sorted list.
   */
  private LinkedList<String> sortStringSet(Set<String> input) {
  	LinkedList<String> sortedItems= new LinkedList<String>();
  	for (String e: input)
  		sortedItems.add(e);
  	Collections.sort(sortedItems);
  	return sortedItems;
  }
}
