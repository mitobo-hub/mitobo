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

package de.unihalle.informatik.MiToBo.io.importer.rsml;

import de.unihalle.informatik.Alida.exceptions.ALDOperatorException;

import java.io.File;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.Set;
import java.util.Vector;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;

import org.w3c.dom.Element;

import de.unihalle.informatik.Alida.annotations.ALDAOperator;
import de.unihalle.informatik.Alida.annotations.ALDAOperator.ExecutionMode;
import de.unihalle.informatik.Alida.annotations.Parameter;
import de.unihalle.informatik.Alida.annotations.Parameter.Direction;
import de.unihalle.informatik.Alida.annotations.Parameter.ExpertMode;
import de.unihalle.informatik.Alida.datatypes.ALDDirectoryString;
import de.unihalle.informatik.MiToBo.apps.minirhizotron.datatypes.MTBRootTree;
import de.unihalle.informatik.MiToBo.apps.minirhizotron.datatypes.MTBRootTreeNodeData;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBTreeNode;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage.MTBImageType;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImageRGB;
import de.unihalle.informatik.MiToBo.core.operator.MTBOperator;
import de.unihalle.informatik.MiToBo.io.dirs.DirectoryTree;
import de.unihalle.informatik.MiToBo.io.images.ImageReaderMTB;
import de.unihalle.informatik.MiToBo.xsd.rsml.PointType;
import de.unihalle.informatik.MiToBo.xsd.rsml.RootType;
import de.unihalle.informatik.MiToBo.xsd.rsml.Rsml;
import de.unihalle.informatik.MiToBo.xsd.rsml.RootType.Geometry;
import de.unihalle.informatik.MiToBo.xsd.rsml.RootType.Functions.Function;
import de.unihalle.informatik.MiToBo.xsd.rsml.Rsml.Scene;

/**
 * Reads the contents of the RSML project files in the given directory.
 * <p>
 * The importer assumes that all files belong to a single project or time-series.
 *   
 * @author Birgit Moeller
 * @author Stefan Posch
 */
@ALDAOperator(genericExecutionMode=ExecutionMode.ALL)
public class MTBRSMLProjectImporter extends MTBOperator {
	
	private static final String FUNCTION_NAME_DIAMETER = "diameter";
	private static final String FUNCTION_NAME_STATUSLABEL = "statusLabel";
	private static final String PROPERTY_NAME_PARENTNODE = "parent-node";

	/*
	 * Some status predefines.
	 */
	
	/**
	 * Undefined status.
	 */
	public static final int STATUS_UNDEFINED = -1;
	
	/**
	 * Segments of a connector treeline.
	 */
	public static final int STATUS_CONNECTOR = -3;

	/**
	 * to represent (virtual) segments to connect 
	 * branches (polylines) of a root to form one (connected) treeline
	 * <br>
	 * used to represent genuine rizoTrak virtual segments
	 * 
	 */
	public static final int STATUS_VIRTUAL = -2;
	
	/**
	 * to represent (virtual) segments created on import from RSML to connect all
	 * branches (polylines) of a root to form one (connected) treeline
	 * 	 * <br>
	 * used to represent virtual segments create for/from RSML
	 * 
	 */
	public static final int STATUS_VIRTUAL_RSML = -4;


	/**
	 * Default status label for segments.
	 */
	private static byte default_statuslabel = 0;

	/**
	 * Maximal distance allowed for parent nodes to deviate from precise location.
	 */
	private static final Double EPSILON = 0.01;

	/**
	 * Operator identifier.
	 */
	private static String opIdentifier = "[MTBRSMLFileReader]";
	
	/**
	 * Flag indicating if identifiers in a time-series are unified.
	 */
	private boolean allUnified = false;
	
	/**
	 * Directory of RSML files.
	 */
	@Parameter( label= "RSML Directory", required = true,
		direction=Direction.IN, dataIOOrder=0, mode=ExpertMode.STANDARD,
		description = "RSML input directory." )
	protected ALDDirectoryString rsmlDir = null;

	/**
	 * Resulting info object containing all information parsed from input files.
	 */
	@Parameter( label= "Result RSML Info Object",
		direction=Direction.OUT, dataIOOrder=0,	description = "RSML info object." )
	protected MTBRSMLProjectInfo rsmlProjectInfo;
	
	/**
	 * Default constructor.
	 * @throws ALDOperatorException	Thrown in case of failure.
	 */
	public MTBRSMLProjectImporter() throws ALDOperatorException {
		// nothing happens here
	}
	
	/**
	 * Specify input directory.
	 * @param dir	Input directory where to find the RSML files.
	 */
	public void setInputDirectory(ALDDirectoryString dir) {
		this.rsmlDir = dir;
	}
	
	/**
	 * Get resulting info object.
	 * @return	Info object with all RSML data.
	 */
	public MTBRSMLProjectInfo getRSMLProjectInfo() {
		return this.rsmlProjectInfo;
	}
	
	@Override
	protected void operate() throws ALDOperatorException {
		
		// init the result object
		this.rsmlProjectInfo = new MTBRSMLProjectInfo();
		
		// parse input directory (non-recursively) for files.
		DirectoryTree dt = new DirectoryTree( this.rsmlDir.getDirectoryName(), false );
		this.rsmlProjectInfo.baseDir = this.rsmlDir.getDirectoryName();
		
		// parse all RSML files
		Vector<Rsml> rsmls = new Vector<Rsml>();
		Vector<File> rsmlFiles = new Vector<File>();
		for ( String fileName : dt.getFileList() ) {
			File rsmlFile = new File( fileName );
			try {
				JAXBContext context = JAXBContext.newInstance( Rsml.class );
				Unmarshaller um = context.createUnmarshaller();
				rsmls.add( (Rsml) um.unmarshal( rsmlFile ));
			} catch ( Exception e ) {
				e.printStackTrace();
				System.err.println(opIdentifier +	
					" cannot read RSML from " + rsmlFile.getName() + "... skipping!");
				continue;
			}
			rsmlFiles.add( rsmlFile );
		}

		// sort files according to time-sequence order
		HashMap<Integer, Rsml> rsmlMap = new HashMap<Integer, Rsml>();
		HashMap<Integer, File> fileMap = new HashMap<Integer, File>();
		int id=0;
		for ( Rsml rsml : rsmls ) {
			rsmlMap.put( rsml.getMetadata().getTimeSequence().getIndex().intValue(), rsml );
			fileMap.put( rsml.getMetadata().getTimeSequence().getIndex().intValue(), rsmlFiles.get(id) );
			++id;
		}
		Set<Integer> keyset = rsmlMap.keySet();
		ArrayList<Integer> sortedKeys = new ArrayList<Integer>();
		for ( Integer k : keyset )
			sortedKeys.add(k);
		Collections.sort( sortedKeys );
		for ( Integer k : sortedKeys ) {
			this.rsmlProjectInfo.rsmlFiles.add( fileMap.get(k) );
			this.rsmlProjectInfo.rsmls.add( rsmlMap.get(k) );
		}
		
		// get status labels from RSML files
		for( int i = 0; i < this.rsmlProjectInfo.rsmls.size(); i++ ) {
			HashMap<Integer, String> statusMap = 
				this.getStatusLabelMappingFromScene( this.rsmlProjectInfo.rsmls.get(i) );
			if( 		null != statusMap 
					&& 	statusMap.size() > 0 ) {
				for( int j : statusMap.keySet() ) {
					MTBRSMLProjectInfo.MTBRSMLStatusLabel rsl = new MTBRSMLProjectInfo.MTBRSMLStatusLabel(
						statusMap.get(j),	statusMap.get(j).substring(0, 1), j,
							MTBRSMLProjectInfo.MTBRSMLStatusLabel.DEFAULT_STATUS_COLOR );
					this.rsmlProjectInfo.statusLabels.add(rsl);
				}
			}
			else {
				System.out.println( opIdentifier + " info: no label mapping was found in " 
						+ this.rsmlProjectInfo.rsmlFiles.get(i).getName() + ".\n" );
			}
		}

		// check for "unified" flag in all files
		this.allUnified = true;
		for(Rsml rsml: this.rsmlProjectInfo.rsmls) {
			if(		 null != rsml.getMetadata() 
					&& null != rsml.getMetadata().getTimeSequence()) {
				
				if( !rsml.getMetadata().getTimeSequence().isUnified() ) {
					this.allUnified = false;
					break;
				}
			}
			else {
				this.allUnified = false;
				break;
			}
		}
		if( ! this.allUnified ) {
			System.out.println(opIdentifier + " info: at least one unified flag has not been set " 
				+ "or does not exist in the selected RSML files.\n"
					+ "Consequently connectors will not be created on import.");
		}

		// loop over the RSML files and parse plant and root data
		for ( int i = 0 ; i < this.rsmlProjectInfo.rsmls.size() ; i++ ) {
			MTBRSMLFileInfo info = this.parseRsmlFile( this.rsmlProjectInfo.rsmls.get(i) );
			this.rsmlProjectInfo.rsmlInfos.add( info );
		}
		
//		try {
//			ImageReaderMTB reader = new ImageReaderMTB("/home/moeller/work/data/Ecotron-EInsect-2018-AnnotationKevin-Projects/T24/CTS/EInsect_T024_CTS_13.06.18_000000_4_HCM.tif");
//			reader.runOp();
//			MTBImageRGB im = (MTBImageRGB)reader.getResultMTBImage().convertType(MTBImageType.MTB_RGB, true);
//			Set<Integer> keys = this.rsmlProjectInfo.rsmlInfos.get(3).rootSystems.keySet();
//			for (Integer k: keys) {
//				HashMap<String, MTBRootTree> trees = this.rsmlProjectInfo.rsmlInfos.get(3).rootSystems.get(k); 
//				for (MTBRootTree tr: trees.values())
//					tr.drawToImage(im);
//			}
//			im.show();
//		} catch (Exception e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
	}
	
	/**
	 * Parse the status label mapping from given RSML file.
	 * @param rsml 	RSML file.
	 * @return Map of the status label mappings or null if none is defined in the RSML file.
	 */
	private HashMap<Integer, String> getStatusLabelMappingFromScene(Rsml rsml) {
		
		if(		 null == rsml.getScene() 
				|| null == rsml.getScene().getProperties() 
				|| null == rsml.getScene().getProperties().getAny()) 
			return null;
		
		HashMap<Integer, String> map = new HashMap<Integer, String>();
		
		for( Element e: rsml.getScene().getProperties().getAny() ) {

			if(     e.getAttributes().getLength() < 2 
					|| !e.getNodeName().equals("statusLabelMapping") ) 
				continue;
			
			int b = Integer.parseInt(e.getAttribute("int"));
			String s = e.getAttribute("value");
			
			map.put(b, s);
		}
		return map;
	}
	
	/** 
	 * Parse the given RSML file.
	 * @param rsml	RSML data structure read from file.
	 * @return Info object with data from file.
	 */
	private MTBRSMLFileInfo parseRsmlFile(Rsml rsml) { 
		
		MTBRSMLFileInfo info = new MTBRSMLFileInfo();
		
		// extract some general meta data from file
		if( null != rsml.getMetadata() ) {
			info.timeSeriesID = rsml.getMetadata().getTimeSequence().getIndex().intValue();
			
			if ( null != rsml.getMetadata().getImage() ) {
				info.imageSHA256 = rsml.getMetadata().getImage().getSha256();
				info.imageName = rsml.getMetadata().getImage().getName();
			}
		}
		
		try {

			// extract some general scene information
			info.plantCount = rsml.getScene().getPlant().size();

			// import the root
			int plantID = 0;
			for ( Scene.Plant plant : rsml.getScene().getPlant() ) {
				
				info.rootCountPerPlant.put(plantID, plant.getRoot().size());

				HashMap<String, MTBRootTree> roots = new HashMap<String, MTBRootTree>();
				for ( RootType root : plant.getRoot() ) {
					
					// remember connector ID, identifiers of roots linked to connectors start with C-...
					if ( this.allUnified && root.getId().startsWith("C-") )
						this.rsmlProjectInfo.connectorIDs.add(root.getId());
					
					// convert root
					MTBRootTree rt = this.createTreeForRoot(root);
					roots.put(root.getId(), rt);
				}
				info.rootSystems.put(plantID, roots);
				++plantID;
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		return info;
	}

	/** 
	 * Create a root tree from the given root object.
	 * @param root	Root object to convert to {@link MTBRootTree}.
	 * @return Resulting root tree.
	 */
	private MTBRootTree createTreeForRoot(RootType root) {
		MTBRootTree rootTree = new MTBRootTree();
		this.fillTreeFromRoot( root, rootTree, null );
		return rootTree;
	}
	
	/** 
	 * Recursively parses all polylines belonging to the given root.
	 * @param root 									Root object to be parsed.
	 * @param tree									Target root tree.
	 * @param parentTreelineNodes 	Map of root tree nodes indexed by IDs from RSML.
	 */
	private void fillTreeFromRoot( RootType root, MTBRootTree tree, 
			HashMap<Integer, MTBTreeNode> parentTreelineNodes) {
		
		Geometry geometry = root.getGeometry();
		Function diameters = this.getFunctionByName( root, FUNCTION_NAME_DIAMETER );
		Function statuslabels = this.getFunctionByName( root, FUNCTION_NAME_STATUSLABEL );
				
		Iterator<PointType> pointItr = geometry.getPolyline().getPoint().iterator();
		if ( !pointItr.hasNext() ) {
			// no nodes at all
			return;
		}
		
		// index of the next node to insert into the root tree
		int pointIndex = 0;
		
		HashMap<Integer,MTBTreeNode> treelineNodes = 
				new HashMap<Integer, MTBTreeNode>( geometry.getPolyline().getPoint().size() );
		
		// reference to node last added
		MTBTreeNode previousnode;
		
		// get first point
		PointType firstPoint = pointItr.next();
		double firstPointX = firstPoint.getX().doubleValue();
		double firstPointY = firstPoint.getY().doubleValue();

		/*
		 * create first node and connect to parent node within the treeline (if existing)
		 */

		// toplevel root/polyline
		if (parentTreelineNodes == null) {
			
			MTBRootTreeNodeData nd = new MTBRootTreeNodeData(
					firstPoint.getX().doubleValue(), firstPoint.getY().doubleValue() );
			nd.setRadius( this.getRadiusFromRsml( diameters, pointIndex ) );
			nd.setStatus( (byte)0 );
			tree.getRoot().setData(nd);
			previousnode = tree.getRoot();
			treelineNodes.put( pointIndex, previousnode );
			
		} 
		// non-toplevel root/polyline 
		else {

			// find the parent node in the tree from which the current root/polyline branches 
			int parentNodeIndex = this.getParentNodeIndex( root );
			
			MTBTreeNode parentNode = null;
			if ( parentNodeIndex != (-1) ) {
				parentNode = parentTreelineNodes.get( parentNodeIndex-1 );
			}
			
			boolean foundParentNode;
			double dist, minDist;
			MTBRootTreeNodeData rnd;

			// if no parent node is specified or parent node as read from RSML is broken due to invalid index
			if ( parentNode == null ) {
				foundParentNode = false;
				// nearest node of the parent root/polyline
				minDist = Double.MAX_VALUE;
				for ( MTBTreeNode node : parentTreelineNodes.values() ) {
					rnd = (MTBRootTreeNodeData)node.getData();
					dist = (firstPointX - rnd.getXPos()) * (firstPointX - rnd.getXPos())
							+ (firstPointY - rnd.getYPos()) * (firstPointY - rnd.getYPos());
					if ( dist < minDist) {
						parentNode = node;
						minDist = dist;
					}
				}
			} 
			else {
				foundParentNode = true;
				rnd = (MTBRootTreeNodeData)parentNode.getData();
				minDist = (firstPointX - rnd.getXPos()) * (firstPointX - rnd.getXPos())
						+ (firstPointY - rnd.getYPos()) * (firstPointY - rnd.getYPos());
			}
			
			// create the first treeline node linking it into the treeline
			byte statuslabel = this.getStatuslabelFromRsml( statuslabels, pointIndex );
 
			if (	 this.isStatuslabelFromRsmlDefined( statuslabels, pointIndex ) 
					&& statuslabel == STATUS_UNDEFINED 
					&& Math.sqrt(minDist) < EPSILON ) {
				
				// skip the first node of this root/polyline if there are more nodes
				if ( pointItr.hasNext() ) {
					++pointIndex;
					firstPoint = pointItr.next();
					statuslabel = this.getStatuslabelFromRsml( statuslabels, pointIndex );	
				}
			} 
			else {
				if ( !foundParentNode ) {
					statuslabel = STATUS_VIRTUAL_RSML;
				} 
				else {
					statuslabel = STATUS_VIRTUAL;
				}
			}
			
			// finally add the node
			MTBRootTreeNodeData nd = new MTBRootTreeNodeData(
					firstPoint.getX().doubleValue(), firstPoint.getY().doubleValue());
			nd.setRadius(this.getRadiusFromRsml( diameters, pointIndex ));
			nd.setStatus(statuslabel);
			MTBTreeNode childNode = new MTBTreeNode(nd); 
			parentNode.addChild(childNode);
			treelineNodes.put( pointIndex, childNode);
			previousnode = childNode;
		}
		// increase point index
		++pointIndex;
		
		// loop over the remaining points
		MTBTreeNode node;
		while ( pointItr.hasNext() ) {
			PointType point = pointItr.next();			
			MTBRootTreeNodeData nd = new MTBRootTreeNodeData(
				point.getX().doubleValue(), point.getY().doubleValue());
			nd.setRadius(this.getRadiusFromRsml( diameters, pointIndex ));
			nd.setStatus(this.getStatuslabelFromRsml( statuslabels, pointIndex ));
			node = new MTBTreeNode(nd); 
			previousnode.addChild(node);
			treelineNodes.put( pointIndex, node );
			previousnode = node;
			++pointIndex;
		}
		
		// recursively add the child roots
		for ( RootType childRoot : root.getRoot() ) {
			this.fillTreeFromRoot( childRoot, tree, treelineNodes );
		}
	}

	/**
	 * Parse the parent node or parent node attribute from the root.
	 * @param root	Root to analyze.
	 * @return Parent node index (starting with 1 by RSML convetions) or <code>-1</code> if not found.
	 */
	private int getParentNodeIndex(RootType root) {
		int parentNodeIndex = -1;
		if ( root.getProperties() != null && root.getProperties().getAny() != null ) {
			for ( Element e : root.getProperties().getAny()) {
				if (   e.getNodeName().equals( PROPERTY_NAME_PARENTNODE) 
						|| e.getNodeName().equals( "parent-node") ) {
					parentNodeIndex =  Integer.valueOf( e.getAttribute( "value"));
				} 
			}
		}
		return parentNodeIndex;
	}

	/**
	 * Get status label for index <code>pointIndex</code> from the function <code>statuslabels</code>.
	 * @param statuslabels	RSML function.
	 * @param pointIndex		Index of point for which to get the status label.
	 * @return Status label or <code>default_statuslabel</code> if function is null or index out of range.
	 */
	private byte getStatuslabelFromRsml(Function statuslabels, int pointIndex) {
		if (	 statuslabels != null 
				&& statuslabels.getAny().size() > pointIndex 
				&& pointIndex >= 0 ) {
			return getSampleValue( statuslabels.getAny().get( pointIndex) ).byteValue();
		} 
		return default_statuslabel;
	}
	
	private boolean isStatuslabelFromRsmlDefined( Function statuslabels, int pointIndex) {
		if (	 statuslabels != null 
				&& statuslabels.getAny().size() > pointIndex 
				&& pointIndex >= 0 ) {
			return true;
		}
		return false;
	}

	/**
	 * Get RSML function by name.
	 * @param root	Root object.
	 * @param name	Function name.
	 * 
	 * @return First function with name <code>name</code>, null if none exists.
	 */
	private Function getFunctionByName(RootType root, String name) {
		
		if (	 root.getFunctions() == null 
				|| root.getFunctions().getFunction() == null ) {
			return null;
		}
		
		ListIterator<Function> itr = root.getFunctions().getFunction().listIterator();
		
		while ( itr.hasNext() ) {
			Function fct = itr.next();
			if ( fct.getName().equals(name) ) {
				return fct;
			}
		}
		return null;
	}

	/** 
	 * Get the radius for index <code>pointIndex</code> from the function <code>diameters</code>.
	 * @param diameters		Function.
	 * @param pointIndex	Index of point for which to get the diameter.
	 * @return	Radius or null if <code>diameters</code> is null or index out of range.
	 */
	private float getRadiusFromRsml(Function diameters, int pointIndex) {
		if (	 diameters != null 
				&& diameters.getAny().size() > pointIndex 
				&& pointIndex >= 0 ) {
			return 0.5f * getSampleValue( diameters.getAny().get( pointIndex) ).floatValue();
		} 
		return 0.0f;
	}
	
	/** 
	 * Get the value of a sample element in a function.
	 * 
	 * @param element	Element to process.
	 * @return Value, otherwise null if neither value attribute nor the text content is defined.
	 */ 
	private BigDecimal getSampleValue(Element element) {
		if ( element == null ) {
			return null;
		} 
		if (	 	element.getAttribute( "value" ) != null 
				&& !element.getAttribute( "value" ).isEmpty() ) {
			return BigDecimal.valueOf( Double.valueOf( element.getAttribute( "value" ) ) );
		} 
		if ( element.getTextContent() != null ) {
			return BigDecimal.valueOf( Double.valueOf( element.getTextContent() ) );
		} 
		return null;
	}

}
