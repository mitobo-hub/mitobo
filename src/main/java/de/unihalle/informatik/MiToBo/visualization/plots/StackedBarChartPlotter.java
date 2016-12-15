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
import de.unihalle.informatik.MiToBo.color.tools.DistinctColorListGenerator;
import de.unihalle.informatik.MiToBo.core.operator.MTBOperator;

import java.awt.Color;
import java.awt.Font;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Set;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.GroupedStackedBarRenderer;
import org.jfree.data.KeyToGroupMap;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.category.DefaultCategoryDataset;

/**
 * Operator that generates stacked bar plots using JFreeChart.
 * <p>
 * For details on the library take a look here:
 * <a href="http://www.jfree.org/jfreechart/">
 * 	http://www.jfree.org/jfreechart/
 * </a>
 * <p>
 * The operator takes input data and generates a stacked bar plot from
 * the data. It provides various parameters to adjust the plot's appearance,
 * like title of the plot, axes labels, or the font size of the tick labels on
 * the axes.
 * <p>
 * As input data the operator takes a hash map which should contain a key for
 * each item to be displayed, i.e., for each column to appear in the plot,
 * and a list of row indizes or 'indicators' with corresponding values which
 * should be plotted for each item. Note that the row indizes for different
 * items should be identical.
 * <p>
 * For an example how the data should look like, consider the following:
 * assume that we are given three different experimental settings e1, e2 and
 * e3, and for each setting we acquire four measurements (or 'indicators' in
 * this context), denoted m1, m2, m3 and m4. To visualize the data in a
 * stacked bar plot we have to pass a hash map to the operator containing
 * three key-value pairs, one for each experimental setting:<br>
 * [e1, hash_1] , [e2, hash_2], [e3, hash_3]<br>
 * In each hash key-value pairs are expected specifying the values for the
 * different indicators in the experiment, e.g., hash_1 should contain the
 * following elements:<br>
 * [m1, 'value of m1 in e1'], ..., [m4, 'value of m4 in e1']
 * <p>
 * For details on stacked bar plots in JFreeChart, e.g., refer to<br>
 * <a href=
 * "http://www.jfree.org/jfreechart/api/javadoc/org/jfree/chart/renderer/category/GroupedStackedBarRenderer.html">
 * http://www.jfree.org/jfreechart/api/javadoc/org/jfree/chart/renderer/category/GroupedStackedBarRenderer.html
 * </a>
 *
 * @author moeller
 */
@ALDAOperator(genericExecutionMode=ALDAOperator.ExecutionMode.NONE,
	level=Level.STANDARD, allowBatchMode=false)
public class StackedBarChartPlotter extends MTBOperator {

	/**
	 * Data to display.
	 */
	@Parameter( label= "Data", required = true,
		dataIOOrder = -1, direction = Direction.IN,
		description = "Data.", mode = ExpertMode.STANDARD)
	protected HashMap< String, HashMap< String, Double>> data;

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
	 * Color map for category colors in plot.
	 */
	@Parameter( label= "Category colors", required = false,
		dataIOOrder = 5, direction = Direction.IN, mode = ExpertMode.STANDARD,
		description = "List of colors for categories.")
	Color[] categoryColors = null;

	/**
	 * Resulting stacked bar chart.
	 */
	@Parameter( label= "Chart Plot",
		dataIOOrder = 1, direction = Direction.OUT,
		description = "Resulting chart plot.", mode = ExpertMode.STANDARD)
	protected JFreeChart stackedBarChart;

	/**
	 * Default constructor.
	 * @throws ALDOperatorException
	 */
	public StackedBarChartPlotter() throws ALDOperatorException {
	  super();
  }

	/**
	 * Set the data to plot.
	 * @param d		Data to plot.
	 */
	public void setData(HashMap< String, HashMap< String, Double>> d) {
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
	 * Specify colors for different categories.
	 * @param colors	List of colors to use for categories.
	 */
	public void setCategoryColors(Color[] colors) {
		this.categoryColors = colors;
	}

	/**
	 * Get reference to resulting chart.
	 * @return	Generated chart plot.
	 */
	public JFreeChart getChart() {
		return this.stackedBarChart;
	}

  @Override
	protected void operate() throws ALDOperatorException,
      ALDProcessingDAGException {
  	final CategoryDataset dataset = createDataset();
  	this.stackedBarChart = createChart(dataset);
  	this.stackedBarChart.setTitle(this.chartTitle);
  }


  /**
   * Create the dataset from input data.
   * @return Resulting dataset.
   */
  private CategoryDataset createDataset() {

  	// initialize result dataset object
  	DefaultCategoryDataset dataset = new DefaultCategoryDataset();

  	// iterate over all items in the given input data
  	Set<String> items = this.data.keySet();
  	// add the data of each item to the dataset
  	LinkedList<String> sortedItems = this.sortStringSet(items);
  	for (String item: sortedItems) {
  		HashMap<String, Double> itemValues = this.data.get(item);
  		// iterate over all row IDs for the given item, i.e., column ID
  		LinkedList<String> sortedItemVals =
  			this.sortStringSet(itemValues.keySet());
  		for (String rowID: sortedItemVals) {
  			dataset.addValue(itemValues.get(rowID), rowID, item);
  		}
  	}
  	return dataset;
  }

  /**
   * Creates the chart.
   *
   * @param dataset  The dataset for the chart.
   * @return Resulting chart.
   * @throws ALDOperatorException
   * @throws ALDProcessingDAGException
   */
  private JFreeChart createChart(final CategoryDataset dataset)
  		throws ALDOperatorException, ALDProcessingDAGException {

  	final JFreeChart chart = ChartFactory.createStackedBarChart(
  		this.chartTitle, 							// chart title
  		this.xLabel,                 	// domain axis label
  		this.yLabel,		             	// range axis label
  		dataset,                     	// data
  		PlotOrientation.VERTICAL,    	// the plot orientation
  		true,                        	// legend
  		true,                        	// tooltips
  		false                       	// urls
		);

  	GroupedStackedBarRenderer renderer = new GroupedStackedBarRenderer();
  	KeyToGroupMap map = new KeyToGroupMap();

  	int keyNum = 0;
  	int categoryNum = 0;
  	for (String colID: this.data.keySet()) {
  		++keyNum;
  		HashMap<String, Double> rowIDs = this.data.get(colID);
  		categoryNum = rowIDs.keySet().size();
  		for (String rowID: rowIDs.keySet()) {
  			map.mapKeyToGroup(rowID, colID);
  		}
  	}
  	renderer.setSeriesToGroupMap(map);
  	renderer.setItemMargin(0.0);

  	// configure colors of different categories
  	if (this.categoryColors == null) {
  		DistinctColorListGenerator cGen = new DistinctColorListGenerator();
  		cGen.setColorNumber(categoryNum);
  		cGen.runOp();
  		this.categoryColors = cGen.getColorList();
  	}
  	for (int i=0; i<keyNum*categoryNum; ++i) {
  		renderer.setSeriesPaint(i, this.categoryColors[i%categoryNum]);
  	}

  	CategoryPlot plot = (CategoryPlot) chart.getPlot();
  	// configure layout of x-axis
  	Font fontTicks = new Font("Dialog", Font.PLAIN, this.tickLabelSize);
  	plot.getDomainAxis().setCategoryMargin(0.05);
  	plot.getDomainAxis().setCategoryLabelPositions(
  		CategoryLabelPositions.UP_45);
  	plot.getDomainAxis().setTickLabelFont(fontTicks);
  	plot.setRenderer(renderer);
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
