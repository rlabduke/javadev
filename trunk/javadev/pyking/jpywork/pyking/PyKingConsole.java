package pyking;

import org.python.core.*;

public class PyKingConsole extends king.Plugin implements org.python.core.PyProxy, org.python.core.ClassDictInit {
    static String[] jpy$mainProperties = new String[] {"python.modules.builtin", "exceptions:org.python.core.exceptions"};
    static String[] jpy$proxyProperties = new String[] {"python.modules.builtin", "exceptions:org.python.core.exceptions", "python.options.showJavaExceptions", "true"};
    static String[] jpy$packages = new String[] {"king.core", "BallPoint,KinWriter,KinfileTokenizer,TrianglePoint,KList,Aspect,MarkerPoint,KSubgroup,KinfileParser,KingView,VectorPoint,KPaint,KGroup,Kinemage,KPoint,TransformSignalSubscriber,RecursivePointIterator,LabelPoint,TransformSignal,KPalette,MasterGroup,AHE,KinemageSignal,Engine,KinemageSignalSubscriber,AGE,AnimationGroup,DotPoint", "java.io", "Reader,PipedWriter,ObjectInput,File,EOFException,ObjectInputValidation,PipedOutputStream,ObjectOutput,SequenceInputStream,PipedInputStream,ObjectStreamConstants,SerializablePermission,NotSerializableException,BufferedWriter,WriteAbortedException,UTFDataFormatException,FileFilter,BufferedInputStream,FileDescriptor,FileWriter,InputStreamReader,StringBufferInputStream,InvalidClassException,PrintStream,StreamCorruptedException,DataInput,BufferedOutputStream,UnsupportedEncodingException,FilePermission,IOException,PushbackInputStream,PipedReader,InterruptedIOException,ByteArrayInputStream,FileInputStream,CharArrayReader,PrintWriter,ByteArrayOutputStream,ObjectOutputStream,FilterWriter,Externalizable,StringWriter,StreamTokenizer,FileNotFoundException,CharArrayWriter,InputStream,NotActiveException,LineNumberReader,FilterInputStream,StringReader,RandomAccessFile,OutputStream,FilterOutputStream,OutputStreamWriter,SyncFailedException,Serializable,PushbackReader,DataInputStream,ObjectInputStream,ObjectStreamClass,CharConversionException,FileReader,BufferedReader,FilterReader,ObjectStreamField,DataOutputStream,InvalidObjectException,FileOutputStream,DataOutput,FilenameFilter,LineNumberInputStream,ObjectStreamException,Writer,OptionalDataException", "javax.swing.text", null, "javax.swing.tree", null, "king", "MacDropTarget,PointEditor,KinStable,XknWriter,KinfileLoader,Vrml97Writer,UIDisplayMenu,MainWindow,UIMenus,ReflectiveRunnable,KingMain,BasicTool,KingPrefs,PrefsEditor,EDMapPlotter,ToolBoxMW,GroupEditor,PdfExport,Plugin,PointFinder,HTMLHelp,UIText,ContentPane,KinTree,ColorPicker,EDMapPlugin,FileDropHandler,KinLoadListener,ToolBox,EDMapWindow,KinCanvas,GridBagPanel,ImageExport,KinfileIO,ViewEditor,ToolServices,Kinglet", "javax.swing.colorchooser", null, "king.tool", null, "javax.swing.undo", null, "org.python.util", "InteractiveInterpreter,PyServlet,InteractiveConsole,PythonInterpreter,jython,ReadlineConsole,PythonObjectInputStream", "javax.swing.event", null, "javax.swing.border", null, "javax.swing.filechooser", null, "javax.swing.table", null, "java.awt.event", null, "javax.swing", "InternalFrameFocusTraversalPolicy,KeyStroke,Spring,AbstractSpinnerModel,SizeSequence,ButtonGroup,JCheckBox,ComboBoxModel,JButton,PopupFactory,JColorChooser,TransferHandler,JScrollPane,JRadioButton,JCheckBoxMenuItem,SizeRequirements,ActionMap,AbstractAction,JSlider,JToggleButton,JToolTip,JSplitPane,JDialog,FocusManager,JMenuItem,JPasswordField,DefaultListCellRenderer,JToolBar,DefaultDesktopManager,JComboBox,RootPaneContainer,ComboBoxEditor,GrayFilter,JLayeredPane,InputMap,DefaultButtonModel,BorderFactory,BoundedRangeModel,SpinnerModel,Renderer,JTabbedPane,SortingFocusTraversalPolicy,ListModel,SwingConstants,JList,JInternalFrame,MenuSelectionManager,OverlayLayout,Action,ViewportLayout,JPopupMenu,ComponentInputMap,JMenuBar,Icon,JFileChooser,SpinnerDateModel,CellRendererPane,JTextField,Timer,JViewport,DefaultComboBoxModel,ScrollPaneLayout,AbstractCellEditor,SpinnerListModel,CellEditor,InputVerifier,SwingUtilities,ImageIcon,UnsupportedLookAndFeelException,LookAndFeel,ProgressMonitor,JRootPane,ProgressMonitorInputStream,DefaultFocusManager,JEditorPane,BoxLayout,JTree,UIManager,DefaultListModel,ToolTipManager,LayoutFocusTraversalPolicy,JTextPane,JWindow,SpringLayout,ListCellRenderer,Scrollable,SingleSelectionModel,MenuElement,JDesktopPane,JScrollBar,JPanel,JTextArea,JFormattedTextField,JTable,ButtonModel,JSpinner,Box,DebugGraphics,JApplet,UIDefaults,RepaintManager,DefaultBoundedRangeModel,DefaultCellEditor,AbstractButton,AbstractListModel,JMenu,ScrollPaneConstants,SpinnerNumberModel,JProgressBar,MutableComboBoxModel,DesktopManager,JRadioButtonMenuItem,JOptionPane,JComponent,JSeparator,WindowConstants,JFrame,DefaultListSelectionModel,DefaultSingleSelectionModel,Popup,ListSelectionModel,JLabel", "javax.swing.plaf", null, "driftwood.gui", "AttentiveComboBox,AttentiveTextField,LogViewer,AlignBox,ReflectiveAction,FatJList,AngleDial,SuffixFileFilter,TextCutCopyPasteMenu,SwapBox,ExpSlider,FoldingBox,TablePane,IndentBox,MenuList,MagnifiedTheme,TablePane2"};
    
    public static class _PyInner extends PyFunctionTable implements PyRunnable {
        private static PyObject s$0;
        private static PyObject i$1;
        private static PyObject i$2;
        private static PyObject i$3;
        private static PyObject i$4;
        private static PyObject s$5;
        private static PyObject s$6;
        private static PyObject s$7;
        private static PyObject s$8;
        private static PyObject i$9;
        private static PyObject s$10;
        private static PyObject s$11;
        private static PyObject s$12;
        private static PyObject s$13;
        private static PyObject s$14;
        private static PyObject s$15;
        private static PyObject s$16;
        private static PyObject s$17;
        private static PyObject s$18;
        private static PyObject s$19;
        private static PyFunctionTable funcTable;
        private static PyCode c$0___init__;
        private static PyCode c$1_buildGUI;
        private static PyCode c$2_initInterpreter;
        private static PyCode c$3_getToolsMenuItem;
        private static PyCode c$4_toString;
        private static PyCode c$5_showDialog;
        private static PyCode c$6_executeScript;
        private static PyCode c$7_scriptChosen;
        private static PyCode c$8_PyKingConsole;
        private static PyCode c$9___init__;
        private static PyCode c$10_actionPerformed;
        private static PyCode c$11_ActionCallback;
        private static PyCode c$12_main;
        private static void initConstants() {
            s$0 = Py.newString("KiNG plugin offering an interactive Python console.\012    ");
            i$1 = Py.newInteger(15);
            i$2 = Py.newInteger(60);
            i$3 = Py.newInteger(2);
            i$4 = Py.newInteger(0);
            s$5 = Py.newString("\012# Enter commands in this window, then press Execute.\012# The following variables have been pre-defined for you:\012#   kMain       the king.KingMain instance representing the application\012#   kCanvas     the king.KinCanvas that is the current display surface\012#   toolbox     the king.ToolBox that this plugin belongs to\012#   services    the king.ToolServices object for showing markers, etc\012\012from king import *\012from king.core import *\012\012");
            s$6 = Py.newString("Sample scriptlets...");
            s$7 = Py.newString("Bleach");
            s$8 = Py.newString("Execute");
            i$9 = Py.newInteger(1);
            s$10 = Py.newString("Python console");
            s$11 = Py.newString("toolbox");
            s$12 = Py.newString("kMain");
            s$13 = Py.newString("kCanvas");
            s$14 = Py.newString("services");
            s$15 = Py.newString("\012");
            s$16 = Py.newString("*** An error occurred in interpretting your script.\012");
            s$17 = Py.newString("");
            s$18 = Py.newString("\012# \"Bleaches\" the (point) color out of all visible points\012kin = kMain.getKinemage()\012if kin != None:\012    rpi = RecursivePointIterator(kin)\012    while rpi.hasNext():\012        point = rpi.next()\012        if(point.isTotallyOn()):\012            point.setColor(None)\012    kCanvas.repaint()\012            ");
            s$19 = Py.newString("/Users/ian/javadev/pyking/PyKingConsole.py");
            funcTable = new _PyInner();
            c$0___init__ = Py.newCode(2, new String[] {"self", "toolbox"}, "/Users/ian/javadev/pyking/PyKingConsole.py", "__init__", false, false, funcTable, 0, null, null, 0, 1);
            c$1_buildGUI = Py.newCode(1, new String[] {"self", "content", "btnRun"}, "/Users/ian/javadev/pyking/PyKingConsole.py", "buildGUI", false, false, funcTable, 1, null, null, 0, 1);
            c$2_initInterpreter = Py.newCode(1, new String[] {"self"}, "/Users/ian/javadev/pyking/PyKingConsole.py", "initInterpreter", false, false, funcTable, 2, null, null, 0, 1);
            c$3_getToolsMenuItem = Py.newCode(1, new String[] {"self"}, "/Users/ian/javadev/pyking/PyKingConsole.py", "getToolsMenuItem", false, false, funcTable, 3, null, null, 0, 1);
            c$4_toString = Py.newCode(1, new String[] {"self"}, "/Users/ian/javadev/pyking/PyKingConsole.py", "toString", false, false, funcTable, 4, null, null, 0, 1);
            c$5_showDialog = Py.newCode(2, new String[] {"self", "event"}, "/Users/ian/javadev/pyking/PyKingConsole.py", "showDialog", false, false, funcTable, 5, null, null, 0, 1);
            c$6_executeScript = Py.newCode(2, new String[] {"self", "event", "script", "out", "err"}, "/Users/ian/javadev/pyking/PyKingConsole.py", "executeScript", false, false, funcTable, 6, null, null, 0, 1);
            c$7_scriptChosen = Py.newCode(2, new String[] {"self", "event", "chosen"}, "/Users/ian/javadev/pyking/PyKingConsole.py", "scriptChosen", false, false, funcTable, 7, null, null, 0, 1);
            c$8_PyKingConsole = Py.newCode(0, new String[] {}, "/Users/ian/javadev/pyking/PyKingConsole.py", "PyKingConsole", false, false, funcTable, 8, null, null, 0, 0);
            c$9___init__ = Py.newCode(2, new String[] {"self", "callback"}, "/Users/ian/javadev/pyking/PyKingConsole.py", "__init__", false, false, funcTable, 9, null, null, 0, 1);
            c$10_actionPerformed = Py.newCode(2, new String[] {"self", "event"}, "/Users/ian/javadev/pyking/PyKingConsole.py", "actionPerformed", false, false, funcTable, 10, null, null, 0, 1);
            c$11_ActionCallback = Py.newCode(0, new String[] {}, "/Users/ian/javadev/pyking/PyKingConsole.py", "ActionCallback", false, false, funcTable, 11, null, null, 0, 0);
            c$12_main = Py.newCode(0, new String[] {}, "/Users/ian/javadev/pyking/PyKingConsole.py", "main", false, false, funcTable, 12, null, null, 0, 0);
        }
        
        
        public PyCode getMain() {
            if (c$12_main == null) _PyInner.initConstants();
            return c$12_main;
        }
        
        public PyObject call_function(int index, PyFrame frame) {
            switch (index){
                case 0:
                return _PyInner.__init__$1(frame);
                case 1:
                return _PyInner.buildGUI$2(frame);
                case 2:
                return _PyInner.initInterpreter$3(frame);
                case 3:
                return _PyInner.getToolsMenuItem$4(frame);
                case 4:
                return _PyInner.toString$5(frame);
                case 5:
                return _PyInner.showDialog$6(frame);
                case 6:
                return _PyInner.executeScript$7(frame);
                case 7:
                return _PyInner.scriptChosen$8(frame);
                case 8:
                return _PyInner.PyKingConsole$9(frame);
                case 9:
                return _PyInner.__init__$10(frame);
                case 10:
                return _PyInner.actionPerformed$11(frame);
                case 11:
                return _PyInner.ActionCallback$12(frame);
                case 12:
                return _PyInner.main$13(frame);
                default:
                return null;
            }
        }
        
        private static PyObject __init__$1(PyFrame frame) {
            frame.getlocal(0).__setattr__("parent", frame.getlocal(1));
            frame.getlocal(0).__setattr__("kMain", frame.getlocal(1).__getattr__("kMain"));
            frame.getlocal(0).__setattr__("kCanvas", frame.getlocal(1).__getattr__("kCanvas"));
            frame.getlocal(0).__setattr__("services", frame.getlocal(1).__getattr__("services"));
            frame.getlocal(0).invoke("buildGUI");
            frame.getlocal(0).invoke("initInterpreter");
            return Py.None;
        }
        
        private static PyObject buildGUI$2(PyFrame frame) {
            frame.getlocal(0).__setattr__("outputText", frame.getglobal("JTextArea").__call__(i$1, i$2));
            frame.getlocal(0).__getattr__("outputText").invoke("setTabSize", i$3);
            frame.getlocal(0).__getattr__("outputText").invoke("setEditable", i$4);
            frame.getlocal(0).__setattr__("inputText", frame.getglobal("JTextArea").__call__(s$5, i$1, i$2));
            frame.getlocal(0).__getattr__("inputText").invoke("setTabSize", i$3);
            frame.getlocal(0).__setattr__("sampleScripts", frame.getglobal("JComboBox").__call__(new PyList(new PyObject[] {s$6, s$7})));
            frame.getlocal(0).__getattr__("sampleScripts").invoke("addActionListener", frame.getglobal("ActionCallback").__call__(frame.getlocal(0).__getattr__("scriptChosen")));
            frame.setlocal(2, frame.getglobal("JButton").__call__(new PyObject[] {s$8, frame.getlocal(0).__getattr__("executeScript")}, new String[] {"actionPerformed"}));
            frame.setlocal(1, frame.getglobal("TablePane2").__call__());
            frame.getlocal(1).invoke("hfill", i$9).invoke("vfill", i$9).invoke("addCell", new PyObject[] {frame.getglobal("JScrollPane").__call__(frame.getlocal(0).__getattr__("outputText")), i$3, i$9}).invoke("newRow");
            frame.getlocal(1).invoke("hfill", i$9).invoke("vfill", i$9).invoke("addCell", new PyObject[] {frame.getglobal("JScrollPane").__call__(frame.getlocal(0).__getattr__("inputText")), i$3, i$9}).invoke("newRow");
            frame.getlocal(1).invoke("addCell", frame.getlocal(2));
            frame.getlocal(1).invoke("right").invoke("addCell", frame.getlocal(0).__getattr__("sampleScripts"));
            frame.getlocal(0).__setattr__("dialogBox", frame.getglobal("JFrame").__call__(s$10));
            frame.getlocal(0).__getattr__("dialogBox").invoke("setContentPane", frame.getlocal(1));
            return Py.None;
        }
        
        private static PyObject initInterpreter$3(PyFrame frame) {
            frame.getlocal(0).__setattr__("interp", frame.getglobal("PythonInterpreter").__call__());
            frame.getlocal(0).__setattr__("interpOut", frame.getglobal("CharArrayWriter").__call__());
            frame.getlocal(0).__getattr__("interp").invoke("setOut", frame.getlocal(0).__getattr__("interpOut"));
            frame.getlocal(0).__setattr__("interpErr", frame.getglobal("CharArrayWriter").__call__());
            frame.getlocal(0).__getattr__("interp").invoke("setErr", frame.getlocal(0).__getattr__("interpErr"));
            frame.getlocal(0).__getattr__("interp").invoke("set", s$11, frame.getlocal(0).__getattr__("parent"));
            frame.getlocal(0).__getattr__("interp").invoke("set", s$12, frame.getlocal(0).__getattr__("kMain"));
            frame.getlocal(0).__getattr__("interp").invoke("set", s$13, frame.getlocal(0).__getattr__("kCanvas"));
            frame.getlocal(0).__getattr__("interp").invoke("set", s$14, frame.getlocal(0).__getattr__("services"));
            return Py.None;
        }
        
        private static PyObject getToolsMenuItem$4(PyFrame frame) {
            return frame.getglobal("JMenuItem").__call__(new PyObject[] {frame.getlocal(0).invoke("toString"), frame.getlocal(0).__getattr__("showDialog")}, new String[] {"actionPerformed"});
        }
        
        private static PyObject toString$5(PyFrame frame) {
            return s$10;
        }
        
        private static PyObject showDialog$6(PyFrame frame) {
            frame.getlocal(0).__getattr__("dialogBox").invoke("pack");
            frame.getlocal(0).__getattr__("dialogBox").invoke("setVisible", i$9);
            return Py.None;
        }
        
        private static PyObject executeScript$7(PyFrame frame) {
            // Temporary Variables
            boolean t$0$boolean;
            PyException t$0$PyException;
            
            // Code
            frame.setlocal(2, frame.getlocal(0).__getattr__("inputText").invoke("getText"));
            t$0$boolean = true;
            try {
                frame.getlocal(0).__getattr__("interp").invoke("exec", frame.getlocal(2));
            }
            catch (Throwable x$0) {
                t$0$boolean = false;
                t$0$PyException = Py.setException(x$0, frame);
                frame.setlocal(4, frame.getlocal(0).__getattr__("interpErr").invoke("toString"));
                frame.getlocal(0).__getattr__("interpErr").invoke("reset");
                if (frame.getglobal("len").__call__(frame.getlocal(4))._gt(i$4).__nonzero__()) {
                    frame.getlocal(0).__getattr__("outputText").invoke("append", frame.getlocal(4));
                    frame.getlocal(0).__getattr__("outputText").invoke("append", s$15);
                }
                else {
                    frame.getlocal(0).__getattr__("outputText").invoke("append", s$16);
                }
            }
            if (t$0$boolean) {
                frame.getlocal(0).__getattr__("outputText").invoke("append", frame.getlocal(2));
                frame.getlocal(0).__getattr__("outputText").invoke("append", s$15);
                frame.getlocal(0).__getattr__("inputText").invoke("setText", s$17);
                frame.setlocal(3, frame.getlocal(0).__getattr__("interpOut").invoke("toString"));
                frame.getlocal(0).__getattr__("interpOut").invoke("reset");
                if (frame.getglobal("len").__call__(frame.getlocal(3))._gt(i$4).__nonzero__()) {
                    frame.getlocal(0).__getattr__("outputText").invoke("append", frame.getlocal(3));
                    frame.getlocal(0).__getattr__("outputText").invoke("append", s$15);
                }
            }
            return Py.None;
        }
        
        private static PyObject scriptChosen$8(PyFrame frame) {
            frame.setlocal(2, frame.getlocal(0).__getattr__("sampleScripts").invoke("getSelectedItem"));
            if (frame.getlocal(2)._eq(s$7).__nonzero__()) {
                frame.getlocal(0).__getattr__("inputText").invoke("append", s$18);
            }
            return Py.None;
        }
        
        private static PyObject PyKingConsole$9(PyFrame frame) {
            /* KiNG plugin offering an interactive Python console.
                 */
            frame.setlocal("__init__", new PyFunction(frame.f_globals, new PyObject[] {}, c$0___init__));
            frame.setlocal("buildGUI", new PyFunction(frame.f_globals, new PyObject[] {}, c$1_buildGUI));
            frame.setlocal("initInterpreter", new PyFunction(frame.f_globals, new PyObject[] {}, c$2_initInterpreter));
            frame.setlocal("getToolsMenuItem", new PyFunction(frame.f_globals, new PyObject[] {}, c$3_getToolsMenuItem));
            frame.setlocal("toString", new PyFunction(frame.f_globals, new PyObject[] {}, c$4_toString));
            frame.setlocal("showDialog", new PyFunction(frame.f_globals, new PyObject[] {}, c$5_showDialog));
            frame.setlocal("executeScript", new PyFunction(frame.f_globals, new PyObject[] {}, c$6_executeScript));
            frame.setlocal("scriptChosen", new PyFunction(frame.f_globals, new PyObject[] {}, c$7_scriptChosen));
            return frame.getf_locals();
        }
        
        private static PyObject __init__$10(PyFrame frame) {
            frame.getlocal(0).__setattr__("callback", frame.getlocal(1));
            return Py.None;
        }
        
        private static PyObject actionPerformed$11(PyFrame frame) {
            frame.getlocal(0).invoke("callback", frame.getlocal(1));
            return Py.None;
        }
        
        private static PyObject ActionCallback$12(PyFrame frame) {
            frame.setlocal("__init__", new PyFunction(frame.f_globals, new PyObject[] {}, c$9___init__));
            frame.setlocal("actionPerformed", new PyFunction(frame.f_globals, new PyObject[] {}, c$10_actionPerformed));
            return frame.getf_locals();
        }
        
        private static PyObject main$13(PyFrame frame) {
            frame.setglobal("__file__", s$19);
            
            frame.setlocal("org", org.python.core.imp.importOne("org", frame));
            frame.setlocal("java", org.python.core.imp.importOne("java", frame));
            frame.setlocal("javax", org.python.core.imp.importOne("javax", frame));
            frame.setlocal("driftwood", org.python.core.imp.importOne("driftwood", frame));
            frame.setlocal("king", org.python.core.imp.importOne("king", frame));
            org.python.core.imp.importAll("org.python.util", frame);
            org.python.core.imp.importAll("king", frame);
            org.python.core.imp.importAll("king.core", frame);
            org.python.core.imp.importAll("driftwood.gui", frame);
            org.python.core.imp.importAll("java.io", frame);
            org.python.core.imp.importAll("javax.swing", frame);
            frame.setlocal("PyKingConsole", Py.makeClass("PyKingConsole", new PyObject[] {frame.getname("Plugin")}, c$8_PyKingConsole, null, PyKingConsole.class));
            frame.setlocal("ActionCallback", Py.makeClass("ActionCallback", new PyObject[] {frame.getname("java").__getattr__("awt").__getattr__("event").__getattr__("ActionListener")}, c$11_ActionCallback, null, ActionCallback.class));
            return Py.None;
        }
        
    }
    public static class ActionCallback extends java.lang.Object implements java.awt.event.ActionListener, org.python.core.PyProxy, org.python.core.ClassDictInit {
        public void actionPerformed(java.awt.event.ActionEvent arg0) {
            PyObject inst = Py.jgetattr(this, "actionPerformed");
            inst._jcall(new Object[] {arg0});
        }
        
        public ActionCallback() {
            super();
            __initProxy__(new Object[] {});
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
            Py.initProxy(this, "pyking.PyKingConsole", "ActionCallback", args, PyKingConsole.jpy$packages, PyKingConsole.jpy$proxyProperties, "pyking", new String[] {"PyKingConsole"});
        }
        
        static public void classDictInit(PyObject dict) {
            dict.__setitem__("__supernames__", Py.java2py(new String[] {}));
        }
        
    }
    public static void moduleDictInit(PyObject dict) {
        dict.__setitem__("__name__", new PyString("PyKingConsole"));
        Py.runCode(new _PyInner().getMain(), dict, dict);
    }
    
    public static void main(String[] args) throws java.lang.Exception {
        String[] newargs = new String[args.length+1];
        newargs[0] = "PyKingConsole";
        System.arraycopy(args, 0, newargs, 1, args.length);
        Py.runMain(pyking.PyKingConsole._PyInner.class, newargs, PyKingConsole.jpy$packages, PyKingConsole.jpy$mainProperties, "pyking", new String[] {"PyKingConsole"});
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
    
    public PyKingConsole(king.ToolBox arg0) {
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
        Py.initProxy(this, "pyking.PyKingConsole", "PyKingConsole", args, PyKingConsole.jpy$packages, PyKingConsole.jpy$proxyProperties, "pyking", new String[] {"PyKingConsole"});
    }
    
    static public void classDictInit(PyObject dict) {
        dict.__setitem__("__supernames__", Py.java2py(new String[] {"super__toString", "finalize", "clone"}));
    }
    
}
