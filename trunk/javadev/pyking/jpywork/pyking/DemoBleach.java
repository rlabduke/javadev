package pyking;

import org.python.core.*;

public class DemoBleach extends king.Plugin implements org.python.core.PyProxy, org.python.core.ClassDictInit {
    static String[] jpy$mainProperties = new String[] {"python.modules.builtin", "exceptions:org.python.core.exceptions"};
    static String[] jpy$proxyProperties = new String[] {"python.modules.builtin", "exceptions:org.python.core.exceptions", "python.options.showJavaExceptions", "true"};
    static String[] jpy$packages = new String[] {"java.io", "Reader,PipedWriter,ObjectInput,File,EOFException,ObjectInputValidation,PipedOutputStream,ObjectOutput,SequenceInputStream,PipedInputStream,ObjectStreamConstants,SerializablePermission,NotSerializableException,BufferedWriter,WriteAbortedException,UTFDataFormatException,FileFilter,BufferedInputStream,FileDescriptor,FileWriter,InputStreamReader,StringBufferInputStream,InvalidClassException,PrintStream,StreamCorruptedException,DataInput,BufferedOutputStream,UnsupportedEncodingException,FilePermission,IOException,PushbackInputStream,PipedReader,InterruptedIOException,ByteArrayInputStream,FileInputStream,CharArrayReader,PrintWriter,ByteArrayOutputStream,ObjectOutputStream,FilterWriter,Externalizable,StringWriter,StreamTokenizer,FileNotFoundException,CharArrayWriter,InputStream,NotActiveException,LineNumberReader,FilterInputStream,StringReader,RandomAccessFile,OutputStream,FilterOutputStream,OutputStreamWriter,SyncFailedException,Serializable,PushbackReader,DataInputStream,ObjectInputStream,ObjectStreamClass,CharConversionException,FileReader,BufferedReader,FilterReader,ObjectStreamField,DataOutputStream,InvalidObjectException,FileOutputStream,DataOutput,FilenameFilter,LineNumberInputStream,ObjectStreamException,Writer,OptionalDataException", "king.core", "BallPoint,KinWriter,KinfileTokenizer,TrianglePoint,KList,Aspect,MarkerPoint,KSubgroup,KinfileParser,KingView,VectorPoint,KPaint,KGroup,Kinemage,KPoint,TransformSignalSubscriber,RecursivePointIterator,LabelPoint,TransformSignal,KPalette,MasterGroup,AHE,KinemageSignal,Engine,KinemageSignalSubscriber,AGE,AnimationGroup,DotPoint", "javax.swing.text", null, "javax.swing.tree", null, "king", "MacDropTarget,PointEditor,KinStable,XknWriter,KinfileLoader,Vrml97Writer,UIDisplayMenu,MainWindow,UIMenus,ReflectiveRunnable,KingMain,BasicTool,KingPrefs,PrefsEditor,EDMapPlotter,ToolBoxMW,GroupEditor,PdfExport,Plugin,PointFinder,HTMLHelp,UIText,ContentPane,KinTree,ColorPicker,EDMapPlugin,FileDropHandler,KinLoadListener,ToolBox,EDMapWindow,KinCanvas,GridBagPanel,ImageExport,KinfileIO,ViewEditor,ToolServices,Kinglet", "javax.swing.colorchooser", null, "king.tool", null, "javax.swing.undo", null, "org.python.util", "InteractiveInterpreter,PyServlet,InteractiveConsole,PythonInterpreter,jython,ReadlineConsole,PythonObjectInputStream", "javax.swing.event", null, "javax.swing.border", null, "javax.swing.filechooser", null, "javax.swing.table", null, "java.awt.event", null, "javax.swing.plaf", null, "javax.swing", "InternalFrameFocusTraversalPolicy,KeyStroke,Spring,AbstractSpinnerModel,SizeSequence,ButtonGroup,JCheckBox,ComboBoxModel,JButton,PopupFactory,JColorChooser,TransferHandler,JScrollPane,JRadioButton,JCheckBoxMenuItem,SizeRequirements,ActionMap,AbstractAction,JSlider,JToggleButton,JToolTip,JSplitPane,JDialog,FocusManager,JMenuItem,JPasswordField,DefaultListCellRenderer,JToolBar,DefaultDesktopManager,JComboBox,RootPaneContainer,ComboBoxEditor,GrayFilter,JLayeredPane,InputMap,DefaultButtonModel,BorderFactory,BoundedRangeModel,SpinnerModel,Renderer,JTabbedPane,SortingFocusTraversalPolicy,ListModel,SwingConstants,JList,JInternalFrame,MenuSelectionManager,OverlayLayout,Action,ViewportLayout,JPopupMenu,ComponentInputMap,JMenuBar,Icon,JFileChooser,SpinnerDateModel,CellRendererPane,JTextField,Timer,JViewport,DefaultComboBoxModel,ScrollPaneLayout,AbstractCellEditor,SpinnerListModel,CellEditor,InputVerifier,SwingUtilities,ImageIcon,UnsupportedLookAndFeelException,LookAndFeel,ProgressMonitor,JRootPane,ProgressMonitorInputStream,DefaultFocusManager,JEditorPane,BoxLayout,JTree,UIManager,DefaultListModel,ToolTipManager,LayoutFocusTraversalPolicy,JTextPane,JWindow,SpringLayout,ListCellRenderer,Scrollable,SingleSelectionModel,MenuElement,JDesktopPane,JScrollBar,JPanel,JTextArea,JFormattedTextField,JTable,ButtonModel,JSpinner,Box,DebugGraphics,JApplet,UIDefaults,RepaintManager,DefaultBoundedRangeModel,DefaultCellEditor,AbstractButton,AbstractListModel,JMenu,ScrollPaneConstants,SpinnerNumberModel,JProgressBar,MutableComboBoxModel,DesktopManager,JRadioButtonMenuItem,JOptionPane,JComponent,JSeparator,WindowConstants,JFrame,DefaultListSelectionModel,DefaultSingleSelectionModel,Popup,ListSelectionModel,JLabel", "driftwood.gui", "AttentiveComboBox,AttentiveTextField,LogViewer,AlignBox,ReflectiveAction,FatJList,AngleDial,SuffixFileFilter,TextCutCopyPasteMenu,SwapBox,ExpSlider,FoldingBox,TablePane,IndentBox,MenuList,MagnifiedTheme,TablePane2"};
    
    public static class _PyInner extends PyFunctionTable implements PyRunnable {
        private static PyObject s$0;
        private static PyObject s$1;
        private static PyObject s$2;
        private static PyFunctionTable funcTable;
        private static PyCode c$0___init__;
        private static PyCode c$1_getToolsMenuItem;
        private static PyCode c$2_toString;
        private static PyCode c$3_activate;
        private static PyCode c$4_DemoBleach;
        private static PyCode c$5_main;
        private static void initConstants() {
            s$0 = Py.newString("Demonstration plugin that 'bleaches' the point color out of all visible points.\012    ");
            s$1 = Py.newString("Demo: bleach points");
            s$2 = Py.newString("/Users/ian/javadev/pyking/DemoBleach.py");
            funcTable = new _PyInner();
            c$0___init__ = Py.newCode(2, new String[] {"self", "toolbox"}, "/Users/ian/javadev/pyking/DemoBleach.py", "__init__", false, false, funcTable, 0, null, null, 0, 1);
            c$1_getToolsMenuItem = Py.newCode(1, new String[] {"self"}, "/Users/ian/javadev/pyking/DemoBleach.py", "getToolsMenuItem", false, false, funcTable, 1, null, null, 0, 1);
            c$2_toString = Py.newCode(1, new String[] {"self"}, "/Users/ian/javadev/pyking/DemoBleach.py", "toString", false, false, funcTable, 2, null, null, 0, 1);
            c$3_activate = Py.newCode(2, new String[] {"self", "event", "point", "kin", "rpi"}, "/Users/ian/javadev/pyking/DemoBleach.py", "activate", false, false, funcTable, 3, null, null, 0, 1);
            c$4_DemoBleach = Py.newCode(0, new String[] {}, "/Users/ian/javadev/pyking/DemoBleach.py", "DemoBleach", false, false, funcTable, 4, null, null, 0, 0);
            c$5_main = Py.newCode(0, new String[] {}, "/Users/ian/javadev/pyking/DemoBleach.py", "main", false, false, funcTable, 5, null, null, 0, 0);
        }
        
        
        public PyCode getMain() {
            if (c$5_main == null) _PyInner.initConstants();
            return c$5_main;
        }
        
        public PyObject call_function(int index, PyFrame frame) {
            switch (index){
                case 0:
                return _PyInner.__init__$1(frame);
                case 1:
                return _PyInner.getToolsMenuItem$2(frame);
                case 2:
                return _PyInner.toString$3(frame);
                case 3:
                return _PyInner.activate$4(frame);
                case 4:
                return _PyInner.DemoBleach$5(frame);
                case 5:
                return _PyInner.main$6(frame);
                default:
                return null;
            }
        }
        
        private static PyObject __init__$1(PyFrame frame) {
            frame.getlocal(0).__setattr__("parent", frame.getlocal(1));
            frame.getlocal(0).__setattr__("kMain", frame.getlocal(1).__getattr__("kMain"));
            frame.getlocal(0).__setattr__("kCanvas", frame.getlocal(1).__getattr__("kCanvas"));
            frame.getlocal(0).__setattr__("services", frame.getlocal(1).__getattr__("services"));
            return Py.None;
        }
        
        private static PyObject getToolsMenuItem$2(PyFrame frame) {
            return frame.getglobal("JMenuItem").__call__(new PyObject[] {frame.getlocal(0).invoke("toString"), frame.getlocal(0).__getattr__("activate")}, new String[] {"actionPerformed"});
        }
        
        private static PyObject toString$3(PyFrame frame) {
            return s$1;
        }
        
        private static PyObject activate$4(PyFrame frame) {
            frame.setlocal(3, frame.getlocal(0).__getattr__("kMain").invoke("getKinemage"));
            if (frame.getlocal(3)._eq(frame.getglobal("None")).__nonzero__()) {
                return Py.None;
            }
            frame.setlocal(4, frame.getglobal("RecursivePointIterator").__call__(frame.getlocal(3)));
            while (frame.getlocal(4).invoke("hasNext").__nonzero__()) {
                frame.setlocal(2, frame.getlocal(4).invoke("next"));
                if (frame.getlocal(2).invoke("isTotallyOn").__nonzero__()) {
                    frame.getlocal(2).invoke("setColor", frame.getglobal("None"));
                }
            }
            frame.getlocal(0).__getattr__("kCanvas").invoke("repaint");
            return Py.None;
        }
        
        private static PyObject DemoBleach$5(PyFrame frame) {
            /* Demonstration plugin that 'bleaches' the point color out of all visible points.
                 */
            frame.setlocal("__init__", new PyFunction(frame.f_globals, new PyObject[] {}, c$0___init__));
            frame.setlocal("getToolsMenuItem", new PyFunction(frame.f_globals, new PyObject[] {}, c$1_getToolsMenuItem));
            frame.setlocal("toString", new PyFunction(frame.f_globals, new PyObject[] {}, c$2_toString));
            frame.setlocal("activate", new PyFunction(frame.f_globals, new PyObject[] {}, c$3_activate));
            return frame.getf_locals();
        }
        
        private static PyObject main$6(PyFrame frame) {
            frame.setglobal("__file__", s$2);
            
            frame.setlocal("java", org.python.core.imp.importOne("java", frame));
            frame.setlocal("javax", org.python.core.imp.importOne("javax", frame));
            frame.setlocal("driftwood", org.python.core.imp.importOne("driftwood", frame));
            frame.setlocal("king", org.python.core.imp.importOne("king", frame));
            org.python.core.imp.importAll("king", frame);
            org.python.core.imp.importAll("king.core", frame);
            org.python.core.imp.importAll("javax.swing", frame);
            frame.setlocal("DemoBleach", Py.makeClass("DemoBleach", new PyObject[] {frame.getname("Plugin")}, c$4_DemoBleach, null, DemoBleach.class));
            return Py.None;
        }
        
    }
    public static void moduleDictInit(PyObject dict) {
        dict.__setitem__("__name__", new PyString("DemoBleach"));
        Py.runCode(new _PyInner().getMain(), dict, dict);
    }
    
    public static void main(String[] args) throws java.lang.Exception {
        String[] newargs = new String[args.length+1];
        newargs[0] = "DemoBleach";
        System.arraycopy(args, 0, newargs, 1, args.length);
        Py.runMain(pyking.DemoBleach._PyInner.class, newargs, DemoBleach.jpy$packages, DemoBleach.jpy$mainProperties, "pyking", new String[] {"DemoBleach", "PyKingConsole"});
    }
    
    public java.lang.Object clone() throws java.lang.CloneNotSupportedException {
        return super.clone();
    }
    
    public void finalize() throws java.lang.Throwable {
        super.finalize();
    }
    
    public javax.swing.JMenuItem getToolsMenuItem() {
        PyObject inst = Py.jgetattr(this, "getToolsMenuItem");
        return (javax.swing.JMenuItem)Py.tojava(inst._jcall(new Object[] {}), javax.swing.JMenuItem.class);
    }
    
    public java.lang.String super__toString() {
        return super.toString();
    }
    
    public java.lang.String toString() {
        PyObject inst = Py.jfindattr(this, "toString");
        if (inst != null) return (java.lang.String)Py.tojava(inst._jcall(new Object[] {}), java.lang.String.class);
        else return super.toString();
    }
    
    public DemoBleach(king.ToolBox arg0) {
        super(arg0);
        __initProxy__(new Object[] {arg0});
    }
    
    private PyInstance __proxy;
    public void _setPyInstance(PyInstance inst) {
        __proxy = inst;
    }
    
    public PyInstance _getPyInstance() {
        return __proxy;
    }
    
    private PySystemState __sysstate;
    public void _setPySystemState(PySystemState inst) {
        __sysstate = inst;
    }
    
    public PySystemState _getPySystemState() {
        return __sysstate;
    }
    
    public void __initProxy__(Object[] args) {
        Py.initProxy(this, "pyking.DemoBleach", "DemoBleach", args, DemoBleach.jpy$packages, DemoBleach.jpy$proxyProperties, "pyking", new String[] {"DemoBleach", "PyKingConsole"});
    }
    
    static public void classDictInit(PyObject dict) {
        dict.__setitem__("__supernames__", Py.java2py(new String[] {"super__toString", "finalize", "clone"}));
    }
    
}
