import org          # to access org.python.* fully-qualified names
import java         # to access java.* fully-qualified names
import javax        # to access javax.* fully-qualified names
import driftwood    # to access driftwood.* fully-qualified names
import king         # to access king.* fully-qualified names
from org.python.util import *
from king import *
from king.core import *
from driftwood.gui import *
from java.io import *
from javax.swing import *

class PyKingConsole(Plugin):
    """KiNG plugin offering an interactive Python console.
    """
    def __init__(self, toolbox):
        #Plugin.__init__(self, toolbox) -- apparently not needed
        # Jython can't access protected variables in the superclass,
        # so we have to do this stupid hack.
        self.parent = toolbox
        self.kMain = toolbox.kMain
        self.kCanvas = toolbox.kCanvas
        self.services = toolbox.services
        self.buildGUI()
        self.initInterpreter()
    def buildGUI(self):
        self.outputText = JTextArea(15, 60)
        self.outputText.setTabSize(2)
        self.outputText.setEditable(0)
        self.inputText = JTextArea("""
# Enter commands in this window, then press Execute.
# The following variables have been pre-defined for you:
#   kMain       the king.KingMain instance representing the application
#   kCanvas     the king.KinCanvas that is the current display surface
#   toolbox     the king.ToolBox that this plugin belongs to
#   services    the king.ToolServices object for showing markers, etc

from king import *
from king.core import *

""", 15, 60)
        self.inputText.setTabSize(2)
        self.sampleScripts = JComboBox(["Sample scriptlets...", "Bleach"])
        # A little work-around because JComboBox has a public method actionPerformed()
        self.sampleScripts.addActionListener(ActionCallback(self.scriptChosen))
        btnRun = JButton("Execute", actionPerformed=self.executeScript)
        content = TablePane2()
        content.hfill(1).vfill(1).addCell(JScrollPane(self.outputText),2,1).newRow()
        content.hfill(1).vfill(1).addCell(JScrollPane(self.inputText),2,1).newRow()
        content.addCell(btnRun)
        content.right().addCell(self.sampleScripts)
        self.dialogBox = JFrame("Python console")
        self.dialogBox.setContentPane(content)
    def initInterpreter(self):
        self.interp = PythonInterpreter()
        self.interpOut = CharArrayWriter()
        self.interp.setOut(self.interpOut)
        self.interpErr = CharArrayWriter()
        self.interp.setErr(self.interpErr)
        self.interp.set("toolbox", self.parent)
        self.interp.set("kMain", self.kMain)
        self.interp.set("kCanvas", self.kCanvas)
        self.interp.set("services", self.services)
    def getToolsMenuItem(self):
        return JMenuItem(self.toString(), actionPerformed=self.showDialog)
    def toString(self):
        return "Python console"
    def showDialog(self, event):
        self.dialogBox.pack()
        self.dialogBox.setVisible(1)
    def executeScript(self, event):
        script = self.inputText.getText()
        try:
            self.interp.exec(script)
        except:
            err = self.interpErr.toString()
            self.interpErr.reset()
            if(len(err) > 0):
                self.outputText.append(err)
                self.outputText.append("\n")
            else:
                self.outputText.append("*** An error occurred in interpretting your script.\n")
        else:
            self.outputText.append(script)
            self.outputText.append("\n")
            self.inputText.setText("")
            out = self.interpOut.toString()
            self.interpOut.reset()
            if(len(out) > 0):
                self.outputText.append(out)
                self.outputText.append("\n")
    def scriptChosen(self, event):
        chosen = self.sampleScripts.getSelectedItem()
        if(chosen == "Bleach"):
            self.inputText.append("""
# "Bleaches" the (point) color out of all visible points
kin = kMain.getKinemage()
if kin != None:
    iter = KIterator.visiblePoints(kin)
    while iter.hasNext():
        point = iter.next()
        point.setColor(None)
""")
        #elif(chosen == ...)
        
# We need this for JComboBox because it has a public method actionPerformed()
class ActionCallback(java.awt.event.ActionListener):
    def __init__(self, callback):
        self.callback = callback
    def actionPerformed(self, event):
        self.callback(event)

