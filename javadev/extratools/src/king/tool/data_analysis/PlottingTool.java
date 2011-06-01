// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package king.tool.data_analysis;
import king.*;
import king.core.*;
import king.points.*;
import king.io.*;
import king.tool.util.KinUtil;

import java.awt.event.*;
import java.util.*;
import java.io.*;
import java.awt.*;
import java.net.*;
import javax.swing.*;
import driftwood.gui.*;
import driftwood.util.*;
import driftwood.r3.*;
//}}}

public class PlottingTool extends BasicTool {
    
//{{{ Variable definitions
//##################################################################################################
    ArrayList allPoints; //list of original values from selected file.
    TreeMap binnedPoints; //points split by bin (color) value.
    HashMap plottedPoints; //points split by value.
    JFileChooser filechooser;
    //JComboBox color1;

    TablePane pane;
    JButton plotButton, /*exportButton,*/ parallelButton/*, filterButton, resetButton*/;
    JTextField numBinsField;
    JTextField xMultField, yMultField, zMultField;
    //JTextField xFiltField, yFiltField, zFiltField;
    //JTextField xFiltRange, yFiltRange, zFiltRange;
    JCheckBox /*clickColorBox, */wrapBox, pointIdBox;
    //JComboBox[] comboBoxes;
    //JRadioButton[] xButtons, yButtons, zButtons;
    //ButtonGroup xGroup, yGroup, zGroup;
    FatJList labelList, xList, yList, zList, colorList;
//}}}

//{{{ Constructor(s)
//##################################################################################################
    /**
    * Constructor
    */
    public PlottingTool(ToolBox tb)
    {
        super(tb);
    }
//}}}

  //{{{ buildGUI
  //##############################################################################
  private void buildGUI() {
	
    dialog = new JDialog(kMain.getTopWindow(), "Data Plotter", false);
    if (allPoints == null) return;
    Iterator iter = allPoints.iterator();
    String[] values = (String[]) iter.next();
    int numColumns = values.length;
    
    pane = new TablePane();
    pane.newRow();
    
    JLabel infoLabel = new JLabel("Data Plotter has detected " + numColumns + " columns of data;  Row 1 shown below.");
    pane.add(infoLabel, numColumns, 1);
    pane.newRow();
    
    String[] axLabels = new String[numColumns];
    //axLabels[0] = "Axis 0";
    for(int i = 0; i < numColumns; i++) {
	    //pane.newRow();
	    axLabels[i] = "Axis " + i;
	    //pane.add(new JLabel(values[i]));
	  }
	  //pane.add(new JLabel(values[0]));
	  if (numColumns > 20) {
	    numColumns = 20;
	  }
	  
	  //ArrayList labelList = new ArrayList();
	  //for (String lab : values) {
	  //  JLabel newLab = new JLabel(lab);
	  //  labelList.add(newLab);
	  //}
	  
	  labelList = new FatJList(0, 10);
	  labelList.setListData(values);
	  labelList.setVisibleRowCount(numColumns + 3);
	  // I didn't know you could redefine functions when you make a new instance of something,
	  // but apparently the following lines works to make the list unselectable!
	  labelList.setSelectionModel(new DefaultListSelectionModel() {
	      public void addSelectionInterval(int index0, int index1) {}
	      public void setSelectionInterval(int index0, int index1) {}
	  });
	  
	  xList = new FatJList(0, 10);
	  xList.setListData(axLabels);
	  xList.setVisibleRowCount(numColumns + 3);
	  
	  yList = new FatJList(0, 10);
	  yList.setListData(axLabels);
	  yList.setVisibleRowCount(numColumns + 3);
	  
	  zList = new FatJList(0, 10);
	  zList.setListData(axLabels);
	  zList.setVisibleRowCount(numColumns + 3);
	  
	  colorList = new FatJList(0, 10);
	  colorList.setListData(axLabels);
	  colorList.setVisibleRowCount(numColumns + 3);
	  
	  plotButton = new JButton(new ReflectiveAction("Plot!", null, this, "onPlot"));
	  
	  //color1 = new JComboBox(KPalette.getStandardMap().values().toArray());
	  //color1.setSelectedItem(KPalette.blue);
	  //pane.add(color1, 2, 1);
	  
	  //clickColorBox = new JCheckBox("Color on click");
	  //clickColorBox.setSelected(false);
	  
	  //exportButton = new JButton(new ReflectiveAction("Export!", null, this, "onExport"));
	  parallelButton = new JButton(new ReflectiveAction("Parallel!", null, this, "onPlotParallel"));
	  numBinsField = new JTextField("10", 4);
	  wrapBox = new JCheckBox("Wrap to 0-360");
	  pointIdBox = new JCheckBox("Add second row to pID");
	  
	  pane.newRow();
	  pane.add(new JLabel(" Row 1"));
	  pane.add(new JLabel(" X axis"));
	  pane.add(new JLabel(" Y axis"));
	  pane.add(new JLabel(" Z axis"));
	  pane.add(new JLabel(" bins"));
	  pane.newRow();
	  pane.vfill(true).hfill(true).add(new JScrollPane(labelList), 1, 8);
	  pane.add(new JScrollPane(xList), 1, 8);
	  pane.add(new JScrollPane(yList), 1, 8);
	  pane.add(new JScrollPane(zList), 1, 8);
	  pane.add(new JScrollPane(colorList), 1, 8);
	  
	  pane.vfill(false).hfill(true).addCell(plotButton);
	  pane.newRow();
	  pane.addCell(parallelButton);
	  pane.newRow();
	  //pane.addCell(exportButton);
	  pane.add(new JLabel(" "));
	  pane.newRow();
	  pane.add(new JLabel("# of Bins:"));
	  pane.newRow();
	  pane.add(numBinsField);
	  pane.newRow();
	  pane.add(new JLabel(" "));
	  pane.newRow();
	  pane.add(wrapBox);
	  //pane.add(color1, 2, 1);
	  pane.newRow();
	  pane.add(pointIdBox);
	  //pane.add(clickColorBox, 2, 1);
	  pane.newRow();
	  pane.newRow();
	  JLabel multLabel = new JLabel("multiplier:");
	  xMultField = new JTextField("1", 4);
	  //JLabel yLabel = new JLabel("y mult=");
	  yMultField = new JTextField("1", 4);
	  //JLabel zLabel = new JLabel("z mult=");
	  zMultField = new JTextField("1", 4);
	  
	  
	  //JLabel xLab2 = new JLabel("keep x=");
	  //xFiltField = new JTextField("0", 4);
	  //xFiltRange = new JTextField("-1", 4);
	  //JLabel yLab2 = new JLabel("keep y=");
	  //yFiltField = new JTextField("0", 4);
	  //yFiltRange = new JTextField("-1", 4);
	  //JLabel zLab2 = new JLabel("keep z=");
	  //zFiltField = new JTextField("0", 4);
	  //zFiltRange = new JTextField("-1", 4);
	  
	  
	  //filterButton = new JButton(new ReflectiveAction("Filter!", null, this, "onFilter"));
	  //resetButton = new JButton(new ReflectiveAction("ResetFilt", null, this, "onReset"));
		
	  pane.add(multLabel);
	  pane.add(xMultField);
	  
	  pane.add(yMultField);
	  pane.add(zMultField);
	  //pane.add(xFiltRange);
	  //pane.skip();
	  
	  
	  //pane.newRow();
	  //pane.add(yLabel);
	  //pane.add(yMultField);
	  //
	  //pane.add(yLab2);
	  //pane.add(yFiltField);
	  //pane.add(yFiltRange);
	  
	  //pane.add(filterButton);
	  //pane.newRow();
	  //pane.add(zLabel);
	  //pane.add(zMultField);
	  //
	  //pane.add(zLab2);
	  //pane.add(zFiltField);
	  //pane.add(zFiltRange);
	  //
	  //pane.add(resetButton);
	  //pane.hfill(true);
	  
	  dialog.addWindowListener(this);
	  dialog.setContentPane(pane);
	}
	//}}}

//{{{ start
//##################################################################################################
    public void start()
    {
	allPoints = new ArrayList();
	binnedPoints = new TreeMap();
	plottedPoints = new HashMap();
	openFile();

	buildGUI();    
	show();
	    
    }
//}}}

//{{{ makeFileChooser
//##################################################################################################
    void makeFileChooser()
    {
	
        // Make accessory for file chooser
        TablePane acc = new TablePane();

        // Make actual file chooser -- will throw an exception if we're running as an Applet
        filechooser = new JFileChooser();
        String currdir = System.getProperty("user.dir");
        if(currdir != null) filechooser.setCurrentDirectory(new File(currdir));
        
        filechooser.setAccessory(acc);

    }
//}}}

//{{{ openFile
//##################################################################################################
    public void openFile()
    {
        // Create file chooser on demand
        if(filechooser == null) makeFileChooser();
        
        if(JFileChooser.APPROVE_OPTION == filechooser.showOpenDialog(kMain.getTopWindow()))
	{
	    try {
		File f = filechooser.getSelectedFile();
		if(f != null && f.exists()) {
		    //dialog.setTitle(f.getName());
		    BufferedReader reader = new BufferedReader(new FileReader(f));
		    String delimChoice = askDelimiter(f.getName());
		    if (delimChoice != null) {
			scanFile(reader, delimChoice);
		    }
		    reader.close();
			

		    kCanvas.repaint(); // otherwise we get partial-redraw artifacts
		}
	    } 
	    
	    catch(IOException ex) { // includes MalformedURLException 
		JOptionPane.showMessageDialog(kMain.getTopWindow(),
					      "An I/O error occurred while loading the file:\n"+ex.getMessage(),
					      "Sorry!", JOptionPane.ERROR_MESSAGE);
		//ex.printStackTrace(SoftLog.err);
	    } catch(IllegalArgumentException ex) {
		JOptionPane.showMessageDialog(kMain.getTopWindow(),
					      "Wrong file format was chosen, or file is corrupt:\n"+ex.getMessage(),
					      "Sorry!", JOptionPane.ERROR_MESSAGE);
		//ex.printStackTrace(SoftLog.err);
	    }
	}
    }
    
//}}}

//{{{ askFormats
    private String askDelimiter(String f) {
	Object[] choices = {"Comma (,)", "Semi-colon (;)", "Colon (:)", "Space", "Tab"};
	String choice = (String) JOptionPane.showInputDialog(kMain.getTopWindow(), 
		  "Please indicate the delimiter to use to parse the data.", 
							     "Choose", JOptionPane.PLAIN_MESSAGE, 
							     null, choices, "Comma (,)");
	if (choice.equals("Comma (,)")) {
	    return ",";
	} else if (choice.equals("Semi-colon (;)")) {
	    return ";";
	} else if (choice.equals("Colon (:)")) {
	    return ":";
	} else if (choice.equals("Tab")) {
	    return "\t";
	} else {
	    return " ";
	}
    }
    
//}}}

  //{{{ scanFile
  //##################################################################################################
  /**
  * Does most of the work reading and analyzing the data files.
  **/
  private void scanFile(BufferedReader reader, String delimiter) {
    String line;
    try {
      while((line = reader.readLine())!=null){
        line = line.trim();
        if (!line.startsWith("#")) {
          String[] strings = Strings.explode(line, delimiter.charAt(0), false, true);
          allPoints.add(strings);
        }
      }
    }
    catch (IOException ex) {
      JOptionPane.showMessageDialog(kMain.getTopWindow(),
      "An I/O error occurred while loading the file:\n"+ex.getMessage(),
      "Sorry!", JOptionPane.ERROR_MESSAGE);
      //ex.printStackTrace(SoftLog.err);
    }
  }
  //}}}
    
  //{{{ onPlot
  public void onPlot(ActionEvent ev) {
    int x = -1, y = -1, z = -1, color = -1;
    x = xList.getSelectedIndex();
    y = yList.getSelectedIndex();
    z = zList.getSelectedIndex();
    color = colorList.getSelectedIndex();
    if (plotButton.getText().equals("Plot!")) {
	    createPoints(x, y, z, color);
    } else {
	    replotPoints(x, y, z, color);
    }
  }
  //}}}
    
  //{{{ createPoints
  public void createPoints(int x, int y, int z, int color) {
    plotButton.setText("Replot!");
    binnedPoints.clear();
    plottedPoints.clear();
    
    // figure out dimension names
    String[] firstVal = (String[]) allPoints.get(0);
    String[] secVal = (String[]) allPoints.get(1);
    ArrayList dimNames = new ArrayList();
    for (int i = 1; i < firstVal.length; i++) {
	    dimNames.add(firstVal[i]);
    }
    
    // count number of numeric dimensions
    int numInd = 0;
    for (int i = 0; i < secVal.length; i++) {
	    if (KinUtil.isNumeric(secVal[i])) numInd++;
    }
    
    // initialize dimension min-max array
    Integer[] dimMinMax = new Integer[numInd * 2];
    for (int i = 0; i < dimMinMax.length; i = i+2) {
	    dimMinMax[i] = new Integer(1000000);
    }
    for (int i = 1; i < dimMinMax.length; i = i+2) {
	    dimMinMax[i] = new Integer(-1000000);
    }
    
    // create bins for the points
    double minColor = 100000;
    double maxColor = -100000;
    ArrayList colors = new ArrayList();
    if (color != -1) {
	    Iterator iter = allPoints.iterator();
	    while (iter.hasNext()) {
        String[] value = (String[]) iter.next();
        //if (color != -1) {
          //System.out.println(value[color]);
          if (value.length == firstVal.length) {
            if (KinUtil.isNumeric(value[color])) {
              double dColor = Double.parseDouble(value[color]);
              if (minColor > dColor) {
                minColor = dColor;
              }
              if (maxColor < dColor) {
                maxColor = dColor;
              }
              colors.add(new Double(value[color]));
            }
          }
	    }
    }
    //double perDiv = (maxColor-minColor)/10;
    //createBins(numInd, minColor, maxColor, perDiv, color);
    createBins(numInd, colors, color);
    
    Iterator iter = allPoints.iterator();
    BallPoint point;
    //double minColor = 100000;
    //double maxColor = -100000;
    
    while (iter.hasNext()) {
	    String[] value = (String[]) iter.next();
	    float[] floats = new float[numInd];
	    int floatInd = 0;
	    // one pass to see if number of numeric values matches number of dimensions
	    for (int i = 0; i < value.length; i++) {
        if (KinUtil.isNumeric(value[i])) {
          floatInd++;
        }
	    }
	    // if they don't match, then this line is not made into a point
	    if (floatInd == numInd) {
        floatInd = 0;
        for (int i = 0; i < value.length; i++) {
          if (KinUtil.isNumeric(value[i])) {
            floats[floatInd] = Float.parseFloat(value[i]);
            if (wrapBox.isSelected()) floats[floatInd] = wrapValue(floats[floatInd]);
            updateMinMax(dimMinMax, floatInd, floats[floatInd]);
            floatInd++;
          }
        }
		    
        String pName;
        if (pointIdBox.isSelected()) {
          pName = value[0] + " " + value[1];
        } else {
          pName = value[0];
        }
        point = new BallPoint(pName);
        plottedPoints.put(value, point);
        point.setRadius((float)0.1);
        point.setAllCoords(floats);
        if (x == -1) point.setX(0);
        if (y == -1) point.setY(0);
        if (z == -1) point.setZ(0);
        point.useCoordsXYZ(x-1, y-1, z-1); // since the first value is a string identifier, indices are off by 1
        rescalePoint(point);
        if (color != -1) {
          Iterator keys = binnedPoints.keySet().iterator();
          Double binValue = null;
          while (keys.hasNext()) {
            Double key = (Double) keys.next();
            if (key.compareTo(new Double(value[color])) <= 0) {
              binValue = key;
            }
          }
          KList list = (KList) binnedPoints.get(binValue);
          list.add(point);
          point.setParent(list);
        } else {
          KList list = (KList) binnedPoints.get(new Double("0"));
          list.add(point);
          point.setParent(list);
        }
	    }
    }
    //System.out.println(minColor + " " + maxColor);
    plot(dimNames, dimMinMax);
  }
  //}}}
  
  //{{{ plot
  public void plot(Collection dimNames, Integer[] dimMinMax) {
    KGroup group = new KGroup("Data Points");
    group.setAnimate(true);
    group.addMaster("Data Points");
    KGroup subgroup = new KGroup("points");
    subgroup.setHasButton(false);
    group.add(subgroup);
    Collection lists = binnedPoints.values();
    Iterator iter = lists.iterator();
    while (iter.hasNext()) {
      KList list = (KList) iter.next();
      //list.setParent(subgroup);
      subgroup.add(list);
      
    }
    Kinemage kin = kMain.getKinemage();
    if (kin == null) {
      kin = new Kinemage(KinfileParser.DEFAULT_KINEMAGE_NAME+"1");
    }
    if (kin.dimensionNames.size() != 0) {
      JOptionPane.showMessageDialog(pane, "This kinemage already had high dimensions!  They have been replaced.", "Error", JOptionPane.ERROR_MESSAGE);
    }
    kin.dimensionNames = new ArrayList<String>(dimNames);
    kin.dimensionMinMax = new ArrayList<Number>(Arrays.asList(dimMinMax));
    kin.getMasterByName("Data Points");
    kin.add(group);
    if (kMain.getKinemage() == null) {
      kMain.getStable().append(Arrays.asList(new Kinemage[] {kin}));
    }
    //kin.initAll();
    //kin.fireKinChanged(Kinemage.CHANGE_EVERYTHING);
    //kMain.publish(new KMessage(kMain, KMessage.KIN_LOADED));
  }
  //}}}
    
  //{{{ wrapValue
  public float wrapValue(float value) {
    if (value >= 0) return value;
    else return value + 360;
  }
  //}}}
  
  //{{{ createBins
  public void createBins(int numInd, ArrayList colors, int color) {
    if (color != -1) {
	    if (KinUtil.isNumeric(numBinsField.getText())) {
        int numBins = Integer.parseInt(numBinsField.getText());
        Collections.sort(colors);
        int size = colors.size();
        double sizePerBin = size/numBins;
        for (int i = 0; i < numBins; i++) {
          Double bin = (Double)colors.get((int)Math.floor(sizePerBin * i));
          KList list = new KList(KList.BALL, bin.toString());
          list.setNoHighlight(true);
          list.addMaster(bin.toString());
          list.setDimension(numInd);
          binnedPoints.put(bin, list);
        }
	    } else {
        JOptionPane.showMessageDialog(pane, "Please put a number in the 'number of bins' field.", "Error", JOptionPane.ERROR_MESSAGE);
	    }
    } else {
	    KList list = new KList(KList.BALL, "multi-dim points");
      list.setNoHighlight(true);
	    list.setDimension(numInd);
	    binnedPoints.put(new Double("0"), list);
    }
  }
  //}}}
  
  //{{{ updateMinMax
  public void updateMinMax(Integer[] dimMinMax, int index, float value) {
    int min = dimMinMax[index*2].intValue();
    int max = dimMinMax[index*2 + 1].intValue();
    if (min > value) dimMinMax[index*2] = new Integer((int)Math.floor((double)value));
    if (max < value) dimMinMax[index*2+1] = new Integer((int)Math.ceil((double)value));
  }
  //}}}
  
  //{{{ replotPoints
  public void replotPoints(int x, int y, int z, int color) {
    // x, y, z, and color are array indexes.
    Iterator iter = allPoints.iterator();
    while (iter.hasNext()) {
	    String[] value = (String[]) iter.next();
	    KPoint point = (KPoint) plottedPoints.get(value);
	    if (x == -1) point.setX(0);
	    if (y == -1) point.setY(0);
	    if (z == -1) point.setZ(0);
	    point.useCoordsXYZ(x-1, y-1, z-1); // since the first value is a string identifier, indices are off by 1
	    if (KinUtil.isNumeric(xMultField.getText())) {
        point.setX(point.getX() * Double.parseDouble(xMultField.getText()));
	    }
	    if (KinUtil.isNumeric(yMultField.getText())) {
        point.setY(point.getY() * Double.parseDouble(yMultField.getText()));
	    }
	    if (KinUtil.isNumeric(zMultField.getText())) {
        point.setZ(point.getZ() * Double.parseDouble(zMultField.getText()));
	    }
    }
    kCanvas.repaint();
  }
  //}}}
  
  //{{{ onPlotParallel
  public void onPlotParallel(ActionEvent ev) {
    Kinemage kin = kMain.getKinemage();
    String key = ParaParams.class.getName()+".instance";
    ParaParams params = (ParaParams) kin.metadata.get(key);
    if (params == null) {
      params = new ParaParams(kMain, kin);
      kin.metadata.put(key, params);
    }
    params.swap();
    //KGroup group = new KGroup("parallel");
    //kin.add(group);
    //KGroup subgroup = new KGroup("partest");
    //subgroup.setHasButton(false);
    //group.add(subgroup);
    //Iterator iter = allPoints.iterator();
    //while (iter.hasNext()) {
	  //  String[] value = (String[]) iter.next();
	  //  if (value.length > 0) {
    //    int[] order = {2, 5, 3, 6, 4, 7, 2};
    //    KList list = makeList(value, order);
    //    subgroup.add(list);
    //    //list.setParent(subgroup);
    //    list.setWidth(1);
    //    list.setAlpha(128);
    //    list.setHasButton(false);
	  //  }
    //}
    //kMain.notifyChange(KingMain.EM_EDIT_GROSS | KingMain.EM_ON_OFF);
    
  }
  //}}}

//{{{ makeList
        public KList makeList(String[] value, int[] order) {
	KList list = new KList(KList.VECTOR);
	list.setName(value[0]);
	VectorPoint prevPoint = null;
	for (int i = 0; i < order.length; i++) {
	    String coord = value[order[i]];
	    VectorPoint point = new VectorPoint(coord, prevPoint);
	    point.setXYZ(i*100, Double.parseDouble(coord), Double.parseDouble(value[8])*5);
	    list.add(point);
	    prevPoint = point;
	    list.setColor(KPaint.createLightweightHSV("blue", 240, 100 - (Float.parseFloat(value[8]))/2,100,240,100 - (Float.parseFloat(value[8]))/2,100));
	}
	
	return list;
    }
//}}}	    

  //{{{ rescalePoint
  /**
  * Handles rescaling the plotted points.  Used to scale the tranformed coordinates, but due
  * to setDrawXYZ functions disappearing in King 2.0, this function will have to actually 
  * rescale the values of the coordinates.
  **/
  public void rescalePoint(KPoint point) {
    //Collection points = plottedPoints.values();
    //Iterator iter = points.iterator();
    //while (iter.hasNext()) {
      //    AbstractPoint point = (AbstractPoint) iter.next();
	    if (KinUtil.isNumeric(xMultField.getText()) && KinUtil.isNumeric(yMultField.getText()) && KinUtil.isNumeric(zMultField.getText())) {
        point.setX(point.getX() * Double.parseDouble(xMultField.getText()));
        point.setY(point.getY() * Double.parseDouble(yMultField.getText()));
        point.setZ(point.getZ() * Double.parseDouble(zMultField.getText()));
	    }
	}
  //}}}
    
//{{{ xx_click() functions
//##################################################################################################
  //  /** Override this function for (left-button) clicks */
  //  public void click(int x, int y, KPoint p, MouseEvent ev)
  //  {
  //      super.click(x, y, p, ev);    
	//if (p != null) {
	//    if (clickColorBox.isSelected()) {
	//	p.setColor((KPaint)color1.getSelectedItem());
	//    }
	//}
  //  }
//}}}

//{{{ onExport
  //  public void onExport(ActionEvent ev) {
	////addAllDataPoints();
	//JFileChooser saveChooser = new JFileChooser();
	//String currdir = System.getProperty("user.dir");
	//if(currdir != null) {
	//    saveChooser.setCurrentDirectory(new File(currdir));
	//}
	//if (saveChooser.APPROVE_OPTION == saveChooser.showSaveDialog(kMain.getTopWindow())) {
	//    File f = saveChooser.getSelectedFile();
	//    if( !f.exists() ||
  //              JOptionPane.showConfirmDialog(kMain.getTopWindow(),
  //                  "This file exists -- do you want to overwrite it?",
  //                  "Overwrite file?", JOptionPane.YES_NO_OPTION)
  //              == JOptionPane.YES_OPTION )
  //          {
  //              saveDataFile(f);
  //          }
	//}
  //
  //  }
//}}}

//{{{ onFilter
    
  //  public void onFilter(ActionEvent ev) {
	//double x, xrange, y, yrange, z, zrange;
	//if ((KinUtil.isNumeric(xFiltField.getText()))&&(KinUtil.isNumeric(xFiltRange.getText()))&& 
	//    (KinUtil.isNumeric(yFiltField.getText()))&&(KinUtil.isNumeric(yFiltRange.getText()))&& 
	//    (KinUtil.isNumeric(zFiltField.getText()))&&(KinUtil.isNumeric(zFiltRange.getText()))) {
	//	
	//    x = Double.parseDouble(xFiltField.getText());
	//    xrange = Double.parseDouble(xFiltRange.getText());
	//    y = Double.parseDouble(yFiltField.getText());
	//    yrange = Double.parseDouble(yFiltRange.getText());
	//    z = Double.parseDouble(zFiltField.getText());
	//    zrange = Double.parseDouble(zFiltRange.getText());
	//    filterCoord(x, xrange, y, yrange, z, zrange);
	//} else {
	//    JOptionPane.showMessageDialog(pane, "You have to put numbers in the text boxes!", "Error",
	//				  JOptionPane.ERROR_MESSAGE);
	//    
	//}
  //
  //  }

///}}}

//{{{ filterCoord

  //  public void filterCoord(double x, double xrange, double y, double yrange, double z, double zrange) {
	//double lowX = x - xrange;
	//double highX = x + xrange;
	//double lowY = y - yrange;
	//double highY = y + yrange;
	//double lowZ = z - zrange;
	//double highZ = z + zrange;
	//
	//Set keys = plottedPoints.keySet();
	//HashMap newPlottedPoints = new HashMap();
	//Iterator iter = keys.iterator();
	//while (iter.hasNext()) {
	//    String[] key = (String[]) iter.next();
	//    KPoint point = (KPoint) plottedPoints.get(key);
	//    double xCoord = point.getX();
	//    double yCoord = point.getY();
	//    double zCoord = point.getZ();
	//    if (xrange != -1) {
	//	if ((xCoord >= lowX)&&(xCoord <= highX)) {
	//	    newPlottedPoints.put(key, point);
	//	} else {
	//	    point.setColor(KPalette.invisible);
	//	}
	//    }
	//    if (yrange != -1) {
	//	if ((yCoord >= lowY)&&(yCoord <= highY)) {
	//	    newPlottedPoints.put(key, point);
	//	} else {
	//	    point.setColor(KPalette.invisible);
	//	}
	//    }
	//    if (zrange != -1) {
	//	if ((zCoord >= lowZ)&&(zCoord <= highZ)) {
	//	    newPlottedPoints.put(key, point);
	//	} else {
	//	    point.setColor(KPalette.invisible);
	//	}
	//    }
	//}
	////plottedPoints = newPlottedPoints;
	//kCanvas.repaint();
  //
  //  }

///}}}

  //{{{ onReset
  //public void onReset(ActionEvent ev) {
  //  xFiltField.setText("0");
  //  yFiltField.setText("0");
  //  zFiltField.setText("0");
  //  xFiltRange.setText("-1");
  //  yFiltRange.setText("-1");
  //  zFiltRange.setText("-1");
  //}
   //}}}
  
//{{{ saveDataFile

  //  public void saveDataFile(File f) {
	//try {
	//    Writer w = new FileWriter(f);
	//    PrintWriter out = new PrintWriter(new BufferedWriter(w));
	//    //addAllDataPoints();
	//    out.println("@kinemage 0");
	//    //out.println("@group {" + f.getName() + "} dimension=7 wrap=360 select");
	//    //out.println("@balllist {" + f.getName() + "} nohilite");
	//    //Iterator iter = allPoints.iterator();
	//    String[] zeroVal = (String[]) allPoints.get(0);
	//    out.println("@group {" + f.getName() + "} dimension=" + (zeroVal.length - 1) + " wrap=360 select");
	//    out.println("@balllist {" + f.getName() + "} nohilite");
	//    //int length = value.length;
	//    Iterator iter = allPoints.iterator();
	//    while (iter.hasNext()) {
	//	String[] value = (String[]) iter.next();
	//	out.print("{" + value[0] + "} ");
	//	for (int i = 1; i < value.length; i++) {
	//	    out.print(value[i]);
	//	    if (i != value.length - 1) {
	//		out.print(", ");
	//	    }
	//	}
	//	out.println("");
	//    }
	//    out.flush();
	//    w.close();
  //
	//} catch (IOException ex) {
	//    JOptionPane.showMessageDialog(kMain.getTopWindow(),
  //              "An error occurred while saving the file.",
  //              "Sorry!", JOptionPane.ERROR_MESSAGE);
  //      }
  //  }
//}}}

//{{{ getHelpAnchor, toString
//##################################################################################################
    public String getHelpAnchor()
    { return "#plotting-tool"; }
    
    /** Returns the URL of a web page explaining use of this tool */
    public URL getHelpURL()
    {
        URL     url     = getClass().getResource("/extratools/tools-manual.html");
        String  anchor  = getHelpAnchor();
        if(url != null && anchor != null)
        {
            try { url = new URL(url, anchor); }
            catch(MalformedURLException ex) { ex.printStackTrace(SoftLog.err); }
            return url;
        }
        else return null;
    }

    public Container getToolPanel()
    { return pane; }
    
    public String toString()
    { return "Data plotter"; }
//}}}

}
