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

package de.unihalle.informatik.MiToBo.segmentation.activecontours.datatypes;

import de.unihalle.informatik.Alida.annotations.ALDDataIOProvider;
import de.unihalle.informatik.Alida.dataio.provider.ALDDataIOSwingInitialGUIValueDefaultHandler;
import de.unihalle.informatik.Alida.dataio.provider.swing.components.ALDParametrizedClassConfigWindow;
import de.unihalle.informatik.Alida.dataio.provider.swing.components.ALDSwingComponent;
import de.unihalle.informatik.Alida.dataio.provider.swing.events.ALDSwingValueChangeEvent;
import de.unihalle.informatik.Alida.dataio.provider.swing.events.ALDSwingValueChangeListener;
import de.unihalle.informatik.Alida.dataio.provider.swing.events.ALDSwingValueChangeReporter;
import de.unihalle.informatik.Alida.exceptions.ALDDataIOException;
import de.unihalle.informatik.Alida.exceptions.ALDDataIOProviderException;
import de.unihalle.informatik.Alida.exceptions.ALDDataIOProviderException.ALDDataIOProviderExceptionType;
import de.unihalle.informatik.Alida.helpers.ALDClassInfo;
import de.unihalle.informatik.Alida.operator.ALDParameterDescriptor;
import de.unihalle.informatik.MiToBo.segmentation.levelset.core.MTBSet_LevelEnergyDerivable;
import de.unihalle.informatik.MiToBo.segmentation.levelset.core.energies.derivable.MTBLevelsetEnergyDerivable;
import de.unihalle.informatik.MiToBo.segmentation.snakes.datatypes.MTBSet_SnakeEnergyComputable;
import de.unihalle.informatik.MiToBo.segmentation.snakes.datatypes.MTBSet_SnakeEnergyDerivable;
import de.unihalle.informatik.MiToBo.segmentation.snakes.energies.MTBSnakeEnergyComputable;
import de.unihalle.informatik.MiToBo.segmentation.snakes.energies.MTBSnakeEnergyDerivable;

import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableModel;

import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.reflect.Field;
import java.util.*;

/**
 * Class for loading/saving sets of PDE energies in GUI contexts.
 * 
 * @author moeller
 */
@ALDDataIOProvider(priority=10)
public class MTBSet_ActiveContourEnergyDataIOSwing 
	extends ALDDataIOSwingInitialGUIValueDefaultHandler {

	/**
	 * Interface method to announce class for which IO is provided for.
	 * 
	 * @return  Collection of classes provided.
	 */
	@Override
	public Collection<Class<?>> providedClasses() {
		LinkedList<Class<?>> classes = new LinkedList<Class<?>>();
	  classes.add(MTBSet_SnakeEnergyDerivable.class);
	  classes.add(MTBSet_SnakeEnergyComputable.class);
	  classes.add(MTBSet_LevelEnergyDerivable.class);
		return classes;
	}
	
	/** 
	 * GUI element for reading sets of snake energies.
	 */
	/* (non-Javadoc)
	 * @see de.unihalle.informatik.Alida.helpers.ALDDataIOSwing#createGUIElement(java.lang.Class, java.lang.Object)
	 */
	@Override
	public ALDSwingComponent createGUIElement (Field field, Class<?> cl, 
			Object obj, ALDParameterDescriptor descr)
		throws ALDDataIOProviderException {
		return
			new MTBSet_ActiveContourEnergyConfigButton(field, cl, obj, descr);
	}

	@Override
  public void setValue(Field field, Class<?> cl, 
  		ALDSwingComponent guiElement, Object value) 
  	throws ALDDataIOProviderException {
		if (value == null)
			return;
		if (!(guiElement instanceof MTBSet_ActiveContourEnergyConfigButton))
			throw new ALDDataIOProviderException(
      	ALDDataIOProviderExceptionType.UNSPECIFIED_ERROR, 
      	"MTBSet_ActiveContourEnergy-IO: setValue() got invalid GUI element!");
		if (   (!(value instanceof MTBSet_SnakeEnergyDerivable))
				&& (!(value instanceof MTBSet_SnakeEnergyComputable))
				&& (!(value instanceof MTBSet_LevelEnergyDerivable)))
			throw new ALDDataIOProviderException(
      	ALDDataIOProviderExceptionType.UNSPECIFIED_ERROR, 
      	"MTBSet_ActiveContourEnergy-IO: setValue() got wrong object type!");
		try {
			((MTBSet_ActiveContourEnergyConfigButton)guiElement).setValue(
					field, cl, value);
		} catch (ALDDataIOException exp) {
			throw new ALDDataIOProviderException(
      		ALDDataIOProviderExceptionType.UNSPECIFIED_ERROR, 
      		"MTBSet_ActiveContourEnergy-IO: setValue() an error occurred...\n" +
      		exp.getCommentString());
		}
  }

	@Override
	public Object readData(Field field, Class<?> cl, 
			ALDSwingComponent guiElement) 
		throws ALDDataIOProviderException {
		if (!(guiElement instanceof MTBSet_ActiveContourEnergyConfigButton))
			return null;
		try {
			Object obj =
				((MTBSet_ActiveContourEnergyConfigButton)guiElement).readData(field, cl);
			return obj;
    } catch (ALDDataIOException e) {
    	// if exception came from provider, just throw it to parent
    	if (e instanceof ALDDataIOProviderException)
    		throw (ALDDataIOProviderException)e;
    	// otherwise embed it into our own exception
    	// (... necessary due to method signature)
			throw new ALDDataIOProviderException(
      	ALDDataIOProviderExceptionType.UNSPECIFIED_ERROR, 
      	"MTBSet_ActiveContourEnergy-IO: Data IO Manager returned an error: \n"+
      	e.getCommentString());
    }
	}

	@Override
	public JComponent writeData(Object obj, ALDParameterDescriptor descr) {
		return	new MTBSet_ActiveContourEnergyDisplayButton(obj, descr);
	}

	/**
	 * GUI element for configuring collections.
	 * <p>
	 * This button has a collection configuration window attached to it 
	 * where specific data is stored and accessable.
	 * 
	 * @author moeller
	 */
	private class MTBSet_ActiveContourEnergyConfigButton 
		extends ALDSwingComponent	implements ALDSwingValueChangeListener {

		/**
		 * Button to display configuration window.
		 */
		private JButton confButton;
		
		/**
		 * Collection configuration window.
		 */
		private MTBSet_ActiveContourEnergyConfigWindow confWin;

		/**
		 * Constructor.
		 * 
		 * @param field	Field of collection.
		 * @param cl		Class of collection.
		 * @param obj		Default object.
		 * @param d			(Operator) parameter descriptor.
		 */
		public MTBSet_ActiveContourEnergyConfigButton(
				Field field, Class<?> cl, Object obj, ALDParameterDescriptor d) 
			throws ALDDataIOProviderException {
			this.confWin = 
				new MTBSet_ActiveContourEnergyConfigWindow(field, cl, obj, d);
			this.confWin.addValueChangeEventListener(this);
			this.confButton = new JButton("Configure Energies...");
			this.confButton.setActionCommand("configButtonPressed");
			this.confButton.addActionListener(this.confWin);
		}
		
		@Override
    public JButton getJComponent() {
			return this.confButton;
		}
		
		/**
		 * Updates GUI component with given value.
		 * 
		 * @param field	Field of collection.
		 * @param cl	Class of collection.
		 * @param obj	Default object.
		 * @return Current data.
		 * @throws ALDDataIOException 
		 */
		public void setValue(Field field, Class<?> cl, Object obj) 
			throws ALDDataIOException {
			this.confWin.setValue(field, cl,obj);
		}
		
		/**
		 * Gets the data from the configuration window.
		 * 
		 * @param field	Field of collection.
		 * @param cl	Class of collection.
		 * @param obj	Default object.
		 * @return Current data.
		 * @throws ALDDataIOException 
		 */
		public Object readData(Field field, Class<?> cl) 
				throws ALDDataIOException {
			return this.confWin.readData(field, cl);
		}

		@Override
    public void handleValueChangeEvent(ALDSwingValueChangeEvent event) {
			this.fireALDSwingValueChangeEvent(event);
		}

		@Override
    public void disableComponent() {
			if (this.confWin != null)
				this.confWin.disableComponent();
    }

		@Override
    public void enableComponent() {
			if (this.confWin != null)
				this.confWin.enableComponent();
    }

		@Override
    public void dispose() {
			if (this.confWin != null)
				this.confWin.dispose();
    }
	}	
	
	/**
	 * Collection configuration window.
	 * 
	 * @author moeller
	 */
	private class MTBSet_ActiveContourEnergyConfigWindow 
			extends ALDSwingValueChangeReporter	
			implements ActionListener, TableModelListener,
				ALDSwingValueChangeListener {

		/**
		 * Main frame.
		 */
		private JFrame window;

		/**
		 * Fixed width of window.
		 */
		private final int frameWidth = 400;
		
		/**
		 * Fixed height of window.
		 */
		private final int frameHeight = 450;

		/**
		 * Main panel of main frame.
		 */
		private JPanel mainPanel = null;

		/**
		 * List of available energies.
		 */
		@SuppressWarnings("rawtypes")
		private Collection<Class> availableClasses = null;
		
		/**
		 * Mapping of short names to class objects.
		 */
		@SuppressWarnings("rawtypes")
		private HashMap<String, Class> classNameMapping = null;
		
		/**
		 * List of currently selected energies.
		 */
		private LinkedList<String> selectedEnergies =	
				new LinkedList<String>();
		
		/**
		 * Mapping of short names to configuration windows.
		 */
		private HashMap<String, ALDParametrizedClassConfigWindow> 
			energyConfWins = null;

		/**
		 * Add button for energies.
		 */
		private JButton addEnergyButton;
		/**
		 * Remove button for energies.
		 */
		private JButton removeEnergyButton;
		/**
		 * Config button for energies.
		 */
		private JButton confEnergyButton;
		/**
		 * Close button.
		 */
		private JButton closeButton;
		/**
		 * Combobox for selecting energies.
		 */
		private JList energySelection;
		/**
		 * List of selected energies.
		 */
		JTable energyTab = null;
		/**
		 * Model for the energy table.
		 */
		MTBSet_SnkEnergyPDEGUITableModel energyTabModel;
		/**
		 * Individual energy weights.
		 */
		Vector<Double> energyWeights = new Vector<Double>();

		/**
		 * (Operator) parameter descriptor of associated parameter.
		 */
		private ALDParameterDescriptor paramDescr;
		
		/**
		 * Default constructor.
		 * 
		 * @param field		Field.
		 * @param cl			Class.
		 * @param o	bj		Preset object.
		 * @param d				(Operator) parameter descriptor.
		 */
		public MTBSet_ActiveContourEnergyConfigWindow(
				Field field, Class<?> cl, Object obj, ALDParameterDescriptor d) 
			throws ALDDataIOProviderException {
			this.window = new JFrame();
			this.window.setTitle("MTB Snake Derivable Energies");
			this.window.setSize(this.frameWidth,this.frameHeight);
			this.buildMainPanel(cl);
			this.window.add(this.mainPanel);
			this.paramDescr = d;
			// if a default value is given, set the value in window 
			if (obj != null) {
	      try {
	        this.setValue(field, cl, obj);
        } catch (ALDDataIOException e) {
        	throw new ALDDataIOProviderException(
        		ALDDataIOProviderExceptionType.SET_VALUE_FAILED,
        		"[MTBSet_ActiveContourEnergyDataIOSwing] " 
        			+ "setting default value failed!");
        }
			}
		}
		
		/**
		 * Disables graphical elements to prohibit value changes.
		 */
		public void disableComponent() {
			if (this.addEnergyButton != null)
				this.addEnergyButton.setEnabled(false);
			if (this.removeEnergyButton != null)
				this.removeEnergyButton.setEnabled(false);
			if (this.confEnergyButton != null)
				this.confEnergyButton.setEnabled(false);
			if (this.energyTab != null)
				this.energyTab.setEnabled(false);
			Set<String> keys = this.energyConfWins.keySet();
			for (String key: keys)
				this.energyConfWins.get(key).disableComponent();
		}
		
		/**
		 * Enables graphical elements to allow for value changes.
		 */
		public void enableComponent() {
			if (this.addEnergyButton != null)
				this.addEnergyButton.setEnabled(true);
			if (this.removeEnergyButton != null)
				this.removeEnergyButton.setEnabled(true);
			if (this.confEnergyButton != null)
				this.confEnergyButton.setEnabled(true);
			if (this.energyTab != null)
				this.energyTab.setEnabled(true);
			Set<String> keys = this.energyConfWins.keySet();
			for (String key: keys)
				this.energyConfWins.get(key).enableComponent();
		}

		/**
		 * Releases all graphical components associcated with this provider.
		 */
		public void dispose() {
			Set<String> keys = this.energyConfWins.keySet();
			for (String key: keys) {
				if (this.energyConfWins.get(key) != null)
					this.energyConfWins.get(key).dispose();
			}
			this.window.dispose();
		}

		/**
		 * Extracts current collection data.
		 * 
		 * @param field	Field of collection elements.
		 * @param cl	Class of collection elements.
		 * @return	Current collection.
		 * @throws ALDDataIOException 
		 */
		@SuppressWarnings("unused")
    public void setValue(Field field, Class<?> cl, Object value) 
			throws ALDDataIOException {
			if (!(value instanceof MTBSet_ActiveContourEnergy))
				throw new ALDDataIOProviderException(
					ALDDataIOProviderExceptionType.SET_VALUE_FAILED,
					"[MTBSet_ActiveContourEnergyDataIOSwing] setValue() got wrong type!");
			MTBSet_ActiveContourEnergy enerSet = 
					(MTBSet_ActiveContourEnergy)value;
			// new table model
			this.selectedEnergies.clear();
			this.energyTabModel= new MTBSet_SnkEnergyPDEGUITableModel(0, 2);
			this.energyTabModel.setColumnIdentifiers(new Object[]{"Energy","Weight"});
			for (int i=0; i<enerSet.getGenericEnergyList().size();++i) {
				Object e = enerSet.getEnergy(i);
				// update energy object itself and its configuration window
				this.energyConfWins.get(e.getClass().getSimpleName()).setValue(e);
				// update list of selected energies
				Double weight = enerSet.getWeight(i);
				String energy = e.getClass().getSimpleName();
				if (this.selectedEnergies.contains(energy))
					return;
				// add new energy to selection
				this.selectedEnergies.add(energy);
				Object [] newRow = new Object[]{energy,weight.toString()};
				this.energyWeights.add(weight);
				this.energyTabModel.addRow(newRow);
			}
			this.energyTab.setModel(this.energyTabModel);
			// only now add listener to avoid catching events too early...
			this.energyTabModel.addTableModelListener(this);
		}
		
		/**
		 * Extracts current collection data.
		 * 
		 * @param field	Field of collection elements.
		 * @param cl	Class of collection elements.
		 * @return	Current collection.
		 * @throws ALDDataIOException 
		 */
		@SuppressWarnings("null")
    public MTBSet_ActiveContourEnergy readData(Field field, Class<?> cl) 
			throws ALDDataIOException {
			// get number of entries in table
			int rows = this.energyTab.getRowCount();
			if (rows == 0)
				return null;
			
			MTBSet_ActiveContourEnergy energySet = null;
			String energyType = "";
			if (cl.equals(MTBSet_SnakeEnergyDerivable.class)) {
				energySet = new MTBSet_SnakeEnergyDerivable();
				energyType = "snakes_derivable";
			}
			else if (cl.equals(MTBSet_SnakeEnergyComputable.class)) {
				energySet = new MTBSet_SnakeEnergyComputable();
				energyType = "snakes_computable";
			}
			else if (cl.equals(MTBSet_LevelEnergyDerivable.class)) {
				energySet = new MTBSet_LevelEnergyDerivable();
				energyType = "levelsets";
			}
			for (int r = 0; r < rows; ++r) {
				String ename = (String)this.energyTabModel.getValueAt(r, 0);
				Class<?> c = this.classNameMapping.get(ename);
				MTBActiveContourEnergyDerivable energy = 
        	(MTBActiveContourEnergyDerivable)this.energyConfWins.get(ename).
        																										readData(field,c);
				if (energyType.equals("snakes_derivable"))
					((MTBSet_SnakeEnergyDerivable)energySet).addEnergy(
						(MTBSnakeEnergyDerivable)energy, Double.valueOf(
							(String)(this.energyTabModel.getValueAt(r, 1))).doubleValue());
				else if (energyType.equals("snakes_computable"))
					((MTBSet_SnakeEnergyComputable)energySet).addEnergy(
						(MTBSnakeEnergyComputable)energy, Double.valueOf(
							(String)(this.energyTabModel.getValueAt(r, 1))).doubleValue());
				else if (energyType.equals("levelsets"))
					((MTBSet_LevelEnergyDerivable)energySet).addEnergy(
						(MTBLevelsetEnergyDerivable)energy,	Double.valueOf(
							(String)(this.energyTabModel.getValueAt(r, 1))).doubleValue());
			}
			return energySet;
		}
		
		/**
		 * Displays configuration window.
		 * <p>
		 * If a default collection is given, the collection is displayed.
		 */
		private void openWindow() {
			this.window.setVisible(true);
		}

		/**
		 * Build the main panel for configuring the list of energies.
		 */
		@SuppressWarnings("rawtypes")
		private void buildMainPanel(Class<?> targetClass) {

			this.mainPanel = new JPanel();
			BoxLayout pgr = new BoxLayout(this.mainPanel, BoxLayout.Y_AXIS);
			this.mainPanel.setLayout(pgr);

			// temporary local variables
			JLabel tmpLab = null;
			JPanel tmpPanel = null;
			FlowLayout fl = null;

			fl = new FlowLayout();
			fl.setAlignment(FlowLayout.LEFT);
			tmpPanel = new JPanel();
			tmpPanel.setLayout(fl);
			tmpLab = new JLabel("Available energies: ");
			tmpPanel.add(tmpLab);
			this.mainPanel.add(tmpPanel);

			// list of available energies
			fl = new FlowLayout();
			fl.setAlignment(FlowLayout.LEFT);
			tmpPanel = new JPanel();
			tmpPanel.setLayout(fl);

			// check which energy set to handle
			this.availableClasses= null;
			if (targetClass.equals(MTBSet_SnakeEnergyDerivable.class))
				this.availableClasses= 
					ALDClassInfo.lookupExtendingClasses(MTBSnakeEnergyDerivable.class);
			else if (targetClass.equals(MTBSet_SnakeEnergyComputable.class))
				this.availableClasses= 
					ALDClassInfo.lookupExtendingClasses(MTBSnakeEnergyComputable.class);
			else if (targetClass.equals(MTBSet_LevelEnergyDerivable.class))
				this.availableClasses= 
					ALDClassInfo.lookupExtendingClasses(MTBLevelsetEnergyDerivable.class);
			
			this.classNameMapping = new HashMap<String, Class>();
			this.energyConfWins = 
				new HashMap<String, ALDParametrizedClassConfigWindow>();
			Vector<String> energyList = new Vector<String>();
			for (Class c : this.availableClasses) {
				String cname = c.getSimpleName();
				this.classNameMapping.put(cname, c);
				ALDParametrizedClassConfigWindow tmpWin;
        try {
	        tmpWin = new ALDParametrizedClassConfigWindow(c.newInstance(), null);
					tmpWin.addValueChangeEventListener(this);
					this.energyConfWins.put(cname, tmpWin); 
					energyList.add(cname);
        } catch (InstantiationException e) {
        	System.err.println("[MTBSet_ActiveContourEnergyDataIOSwing] " 
        											+ "Could not instantiate object of type " + c);
	        e.printStackTrace();
        } catch (IllegalAccessException e) {
        	System.err.println("[MTBSet_ActiveContourEnergyDataIOSwing] " 
        																							+ "Illegal access...");
	        e.printStackTrace();
        }
			}
			// sort list of available energies lexicographically
			Collections.sort(energyList);
			
			this.energySelection = new JList(energyList);
			this.energySelection.ensureIndexIsVisible(1);
			this.energySelection.setSelectionMode(
																				ListSelectionModel.SINGLE_SELECTION);
			this.energySelection.setEnabled(true);
			JScrollPane scrollPane = new JScrollPane();
			scrollPane.setViewportView(this.energySelection);
			scrollPane.setAutoscrolls(true);
			this.mainPanel.add(scrollPane);
			
			tmpPanel = new JPanel();
			this.addEnergyButton = new JButton("   Add selected energy...   ");
			this.addEnergyButton.setActionCommand("addEnergy");
			this.addEnergyButton.addActionListener(this);
			tmpPanel.add(this.addEnergyButton);
			this.mainPanel.add(tmpPanel);

			fl = new FlowLayout();
			fl.setAlignment(FlowLayout.LEFT);
			tmpPanel = new JPanel();
			tmpPanel.setLayout(fl);
			tmpLab = new JLabel("Selected energies: ");
			tmpPanel.add(tmpLab);
			this.mainPanel.add(tmpPanel);

			// selected energies table
			fl = new FlowLayout();
			fl.setAlignment(FlowLayout.LEFT);
			tmpPanel = new JPanel();
			tmpPanel.setLayout(fl);
			this.energyTabModel= new MTBSet_SnkEnergyPDEGUITableModel(0, 2);
			this.energyTabModel.addTableModelListener(this);
			this.energyTabModel.setColumnIdentifiers(new Object[]{"Energy","Weight"});
			this.energyTab = new JTable(this.energyTabModel);
			scrollPane= new JScrollPane();
			scrollPane.setViewportView(this.energyTab);
			scrollPane.setAutoscrolls(true);
			this.mainPanel.add(scrollPane);

			// add remove and configure buttons
			GridLayout gl = new GridLayout(1,2);
			tmpPanel = new JPanel();
			tmpPanel.setLayout(gl);
			this.confEnergyButton = new JButton(" Configure  ");
			this.confEnergyButton.setActionCommand("configEnergy");
			this.confEnergyButton.addActionListener(this);
			this.removeEnergyButton = new JButton("     Remove       ");
			this.removeEnergyButton.setActionCommand("removeEnergy");
			this.removeEnergyButton.addActionListener(this);
			tmpPanel.add(this.removeEnergyButton);
			tmpPanel.add(this.confEnergyButton);
			this.mainPanel.add(tmpPanel);

			// close button
			gl = new GridLayout(1,2);
			tmpPanel = new JPanel();
			tmpPanel.setLayout(gl);
			this.closeButton = new JButton(" Close  ");
			this.closeButton.setActionCommand("closeWin");
			this.closeButton.addActionListener(this);
			tmpPanel.add(new JLabel(""));
			tmpPanel.add(this.closeButton);
			this.mainPanel.add(tmpPanel);
		}

		@Override
    public void actionPerformed(ActionEvent e) {
			String cmd = e.getActionCommand(); 
			if (cmd.equals("configButtonPressed")) {
				this.openWindow();
			}
			// handle configuration actions
			if (e.getActionCommand() == "addEnergy") {
				// get ID from GUI
				String energy = (String)this.energySelection.getSelectedValue();
				if (this.selectedEnergies.contains(energy))
					return;
				// add new energy to selection
				this.selectedEnergies.add(energy);
				Object [] newRow = new Object[]{energy,"1.0"};
				this.energyWeights.add(new Double(1.0));
				this.energyTabModel.addRow(newRow);
			}
			if (e.getActionCommand() == "removeEnergy") {
				// get selected row from table
				if (this.energyTab.getSelectedRow() != -1) {
					int entry = this.energyTab.getSelectedRow();
					this.selectedEnergies.remove(entry);
					this.energyWeights.remove(entry);
					this.energyTabModel.removeRow(entry);
				}
			}
			if (e.getActionCommand() == "configEnergy") {
				// get selected row from table
				if (this.energyTab.getSelectedRow() != -1) {
					int entry = this.energyTab.getSelectedRow();
					String energy = (String)this.energyTabModel.getValueAt(entry, 0);
					// open the corresponding window
					try {
	          this.energyConfWins.get(energy).setVisible(true);
          } catch (ALDDataIOProviderException e1) {
	          // TODO Auto-generated catch block
	          e1.printStackTrace();
          }
				}
			}
			else if (cmd.equals("closeWin")) {
				this.window.setVisible(false);
			}
	  }		
		
		@Override
    public void tableChanged(TableModelEvent e) {
			// ignore all events except updates
			if (   e.getType() == TableModelEvent.INSERT 
					|| e.getType() == TableModelEvent.DELETE
					|| e.getType() == TableModelEvent.UPDATE) {
				this.fireALDSwingValueChangeEvent(
					new ALDSwingValueChangeEvent(this, this.paramDescr));
			}
    }

		@Override
    public void handleValueChangeEvent(ALDSwingValueChangeEvent event) {
			this.fireALDSwingValueChangeEvent(event);
		}

		/**
		 * Internal PDE snake energy GUI table model.
		 * 
		 * @author moeller
		 */
		private class MTBSet_SnkEnergyPDEGUITableModel extends DefaultTableModel
		implements TableModelListener {

			/**
			 * Default serial number.
			 */
			private static final long serialVersionUID = 1L;

			/**
			 * Default constructor.
			 * 
			 * @param row	Initial row count.
			 * @param col	Initial col count.
			 */
			public MTBSet_SnkEnergyPDEGUITableModel(int row, int col) {
				super(row,col);
				this.addTableModelListener(this);
			}

			@Override
			public boolean isCellEditable(int row, int column) {
				if (column == 0)
					return false;
				return super.isCellEditable(row, column);
			}

			@Override
			public void tableChanged(TableModelEvent e) {
				int row = e.getFirstRow();
				if (e.getType() == TableModelEvent.UPDATE && row != -1) {
					if (MTBSet_ActiveContourEnergyConfigWindow.
								this.energyTabModel.getRowCount() > 0) {
						Double d = Double.valueOf(
								(String)MTBSet_ActiveContourEnergyConfigWindow.
								this.energyTabModel.getValueAt(row, 1));
						MTBSet_ActiveContourEnergyConfigWindow.
								this.energyWeights.setElementAt(d, row);
					}
				}
				if (MTBSet_ActiveContourEnergyConfigWindow.this.energyTab != null)	
					MTBSet_ActiveContourEnergyConfigWindow.this.energyTab.tableChanged(e);
			}
		}
	}

	/**
	 * GUI element for showing energy collections.
	 * <p>
	 * This button has a window attached to it where energy sets are displayed.
	 * 
	 * @author moeller
	 */
	private class MTBSet_ActiveContourEnergyDisplayButton extends JButton {

		/**
		 * Collection display window.
		 */
		private MTBSet_ActiveContourEnergyDisplayWindow displayWin;

		/**
		 * Constructor.
		 * @param obj			Object to display.
		 * @param descr		Associated parameter descriptor.
		 */
		public MTBSet_ActiveContourEnergyDisplayButton(Object obj, 
				ALDParameterDescriptor descr) {
			super("Show Energies...");
			this.displayWin = new MTBSet_ActiveContourEnergyDisplayWindow(obj,descr);
			this.setActionCommand("displayButtonPressed");
			this.addActionListener(this.displayWin);
		}
		
		/**
		 * Updates GUI component with given value.
		 * 
		 * @param field	Field of collection.
		 * @param cl	Class of collection.
		 * @param obj	Default object.
		 * @throws ALDDataIOException 
		 */
		@SuppressWarnings("unused")
    public void setValue(Field field, Class<?> cl, Object obj) 
			throws ALDDataIOException {
			this.displayWin.setValue(obj);
		}		
	}	
	
	/**
	 * Energy collection display window.
	 * 
	 * @author moeller
	 */
	private class MTBSet_ActiveContourEnergyDisplayWindow 
			implements ActionListener {

		/**
		 * Main frame.
		 */
		private JFrame window;

		/**
		 * Fixed width of window.
		 */
		private final int frameWidth = 400;
		
		/**
		 * Fixed height of window.
		 */
		private final int frameHeight = 250;

		/**
		 * Main panel of main frame.
		 */
		private JPanel mainPanel = null;

		/**
		 * List of currently selected energies.
		 */
		private LinkedList<String> selectedEnergies =	new LinkedList<String>();
		
		/**
		 * Mapping of short names to configuration windows.
		 */
		private HashMap<String, ALDParametrizedClassConfigWindow> 
			energyConfWins = null;

		/**
		 * Config button for energies.
		 */
		private JButton displayEnergyButton;
		/**
		 * Close button.
		 */
		private JButton closeButton;
		/**
		 * List of selected energies.
		 */
		JTable energyTab = null;
		/**
		 * Model for the energy table.
		 */
		MTBSet_SnkEnergyPDEGUITableModel energyTabModel;
		/**
		 * Individual energy weights.
		 */
		Vector<Double> energyWeights = new Vector<Double>();

		/**
		 * Default constructor.
		 * @param o				Object to display.
		 * @param descr		Related parameter descriptor.
		 */
		public MTBSet_ActiveContourEnergyDisplayWindow(
				Object o, @SuppressWarnings("unused") ALDParameterDescriptor descr) {
			this.window = new JFrame();
			this.window.setTitle("MTB Snake Derivable Energies");
			this.window.setSize(this.frameWidth,this.frameHeight);
			this.buildMainPanel();
			this.window.add(this.mainPanel);
			try {
	      this.setValue(o);
      } catch (ALDDataIOException e) {
	      e.printStackTrace();
      }
		}

		/**
		 * Set new value.
		 * @param value		New value to set.
		 * @throws ALDDataIOException 
		 */
		@SuppressWarnings("unused")
    public void setValue(Object value) throws ALDDataIOException {
			MTBSet_ActiveContourEnergy enerSet = 
					(MTBSet_ActiveContourEnergy)value;
			// new table model
			this.selectedEnergies.clear();
			this.energyTabModel= new MTBSet_SnkEnergyPDEGUITableModel(0, 2);
			this.energyTabModel.setColumnIdentifiers(new Object[]{"Energy","Weight"});
			this.energyConfWins = 
					new HashMap<String, ALDParametrizedClassConfigWindow>();
			ALDParametrizedClassConfigWindow tmpWin;
			for (int i=0; i<enerSet.getGenericEnergyList().size();++i) {
				Object e = enerSet.getEnergy(i);
				tmpWin = new ALDParametrizedClassConfigWindow(e, null);
				this.energyConfWins.put(e.getClass().getSimpleName(), tmpWin); 
				// update list of selected energies
				Double weight = enerSet.getWeight(i);
				String energy = e.getClass().getSimpleName();
				if (this.selectedEnergies.contains(energy))
					return;
				// add new energy to selection
				this.selectedEnergies.add(energy);
				Object [] newRow = new Object[]{energy,weight.toString()};
				this.energyWeights.add(weight);
				this.energyTabModel.addRow(newRow);
			}	
			this.energyTab.setModel(this.energyTabModel);
			// make table non-editable
			for (int c = 0; c < this.energyTab.getColumnCount(); ++c) {
				Class<?> col_class = this.energyTab.getColumnClass(c);
				this.energyTab.setDefaultEditor(col_class, null);
			}
			this.energyTabModel.fireTableDataChanged();
		}
		
		/**
		 * Displays configuration window.
		 * <p>
		 * If a default collection is given, the collection is displayed.
		 */
		private void openWindow() {
			this.window.setVisible(true);
		}

		/**
		 * Build the main panel for configuring the list of energies.
		 */
		private void buildMainPanel() {

			this.mainPanel = new JPanel();
			BoxLayout pgr = new BoxLayout(this.mainPanel, BoxLayout.Y_AXIS);
			this.mainPanel.setLayout(pgr);

			// temporary local variables
			JLabel tmpLab = null;
			JPanel tmpPanel = null;
			FlowLayout fl = null;

			// list of available energies
			fl = new FlowLayout();
			fl.setAlignment(FlowLayout.LEFT);
			tmpPanel = new JPanel();
			tmpPanel.setLayout(fl);

			fl = new FlowLayout();
			fl.setAlignment(FlowLayout.LEFT);
			tmpPanel = new JPanel();
			tmpPanel.setLayout(fl);
			tmpLab = new JLabel("Selected energies: ");
			tmpPanel.add(tmpLab);
			this.mainPanel.add(tmpPanel);

			// selected energies table
			fl = new FlowLayout();
			fl.setAlignment(FlowLayout.LEFT);
			tmpPanel = new JPanel();
			tmpPanel.setLayout(fl);
			this.energyTabModel= new MTBSet_SnkEnergyPDEGUITableModel(0, 2);
			this.energyTabModel.setColumnIdentifiers(new Object[]{"Energy","Weight"});
			this.energyTab = new JTable(this.energyTabModel);
			// make table non-editable
			for (int c = 0; c < this.energyTab.getColumnCount(); ++c) {
				Class<?> col_class = this.energyTab.getColumnClass(c);
				this.energyTab.setDefaultEditor(col_class, null);
			}
			JScrollPane scrollPane= new JScrollPane();
			scrollPane.setViewportView(this.energyTab);
			scrollPane.setAutoscrolls(true);
			this.mainPanel.add(scrollPane);

			// add parameter and close buttons
			GridLayout gl = new GridLayout(1,2);
			tmpPanel = new JPanel();
			tmpPanel.setLayout(gl);
			this.displayEnergyButton = new JButton(" Show parameters... ");
			this.displayEnergyButton.setActionCommand("displayEnergy");
			this.displayEnergyButton.addActionListener(this);
			this.closeButton = new JButton(" Close  ");
			this.closeButton.setActionCommand("closeWin");
			this.closeButton.addActionListener(this);
			tmpPanel.add(this.displayEnergyButton);
			tmpPanel.add(this.closeButton);
			this.mainPanel.add(tmpPanel);
		}

		@Override
    public void actionPerformed(ActionEvent e) {
			String cmd = e.getActionCommand(); 
			if (cmd.equals("displayButtonPressed")) {
				this.openWindow();
			}
			if (e.getActionCommand() == "displayEnergy") {
				// get selected row from table
				if (this.energyTab.getSelectedRow() != -1) {
					int entry = this.energyTab.getSelectedRow();
					String energy = (String)this.energyTabModel.getValueAt(entry, 0);
					// open the corresponding window
					try {
	          this.energyConfWins.get(energy).setVisible(true);
          } catch (ALDDataIOProviderException e1) {
          	JOptionPane.showMessageDialog(this.window, 
          		"Energy configuration cannot not be displayed, not found!");
          	return;
          }
				}
			}
			else if (cmd.equals("closeWin")) {
				Set<String> keys = this.energyConfWins.keySet();
				for (String key: keys) {
					if (this.energyConfWins.get(key) != null)
	          try {
	            this.energyConfWins.get(key).setVisible(false);
            } catch (ALDDataIOProviderException e1) {
            	// nothing can be done here...
            }
				}
				this.window.setVisible(false);
			}
	  }		

		/**
		 * Internal GUI table model.
		 * 
		 * @author moeller
		 */
		private class MTBSet_SnkEnergyPDEGUITableModel extends DefaultTableModel
		implements TableModelListener {

			/**
			 * Default serial number.
			 */
			private static final long serialVersionUID = 1L;

			/**
			 * Default constructor.
			 * 
			 * @param row	Initial row count.
			 * @param col	Initial col count.
			 */
			public MTBSet_SnkEnergyPDEGUITableModel(int row, int col) {
				super(row,col);
				this.addTableModelListener(this);
			}

			@Override
			public boolean isCellEditable(int row, int column) {
				if (column == 0)
					return false;
				return super.isCellEditable(row, column);
			}

			@Override
			public void tableChanged(TableModelEvent e) {
				int row = e.getFirstRow();
				if (e.getType() == TableModelEvent.UPDATE && row != -1) {
					if (MTBSet_ActiveContourEnergyDisplayWindow.
								this.energyTabModel.getRowCount() > 0) {
						Double d = Double.valueOf(
								(String)MTBSet_ActiveContourEnergyDisplayWindow.
								this.energyTabModel.getValueAt(row, 1));
						MTBSet_ActiveContourEnergyDisplayWindow.
								this.energyWeights.setElementAt(d, row);
					}
				}
				if (MTBSet_ActiveContourEnergyDisplayWindow.this.energyTab != null)	
					MTBSet_ActiveContourEnergyDisplayWindow.this.energyTab.tableChanged(e);
			}
		}
	}
}
