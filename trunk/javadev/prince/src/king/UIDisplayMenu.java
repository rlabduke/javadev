// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package king;
import king.core.*;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.*;
//import java.util.regex.*;
import javax.swing.*;
import javax.swing.event.*;
import driftwood.gui.*;

import java.util.List;
//}}}
/**
* <code>UIDisplayMenu</code> encapsulates all the functions
* listed in KiNG's "Display" menu.
*
* <p>Copyright (C) 2003-2007 by Ian W. Davis. All rights reserved.
* <br>Begun on Mon May 26 12:30:25 EDT 2003
*/
public class UIDisplayMenu //extends ... implements ...
{
//{{{ Constants
//}}}

//{{{ INNER CLASS: DispQualityList
//##################################################################################################
    class DispQualityList extends MenuList
    {
        Map map;
        
        public DispQualityList(Map stringToIntegerMap, Collection strings, String defaultItem)
        {
            super(strings, defaultItem);
            map = stringToIntegerMap;
        }
        
        protected void itemSelected(Object item)
        {
            if(item == null) return;
            KinCanvas canvas = kMain.getCanvas();
            canvas.setQuality( ((Integer)map.get(item)).intValue() );
            canvas.repaint();
        }
    }
//}}}

//{{{ INNER CLASS: AspectAction
//##################################################################################################
    class AspectAction extends AbstractAction
    {
        Aspect aspect;
        
        public AspectAction(int i, Aspect a)
        {
            super(i+" "+a.getName());
            this.aspect = a;
        }
        
        public void actionPerformed(ActionEvent ev)
        {
            kMain.getCanvas().setCurrentAspect(aspect);
        }
    }
//}}}

//{{{ Variable definitions
//##################################################################################################
    KingMain    kMain;
    JMenu       menu        = null;
    JMenu       aspectMenu  = null;
    
    JCheckBoxMenuItem cbPersp = null, cbBigMarkers = null, cbBigLabels = null, cbThickness = null,
        cbThin = null, cbIntensity = null, cbBackground = null, cbMonochrome = null, cbColorByList = null,
        cbStereo = null, cbCrosseye = null;
    JCheckBoxMenuItem autoRockMenuItem = null;

    // Timers, etc for auto-xxx functions
    javax.swing.Timer autoRockTimer = null;
    float   rockStepSize    = 0;
    int     rockStepCount   = 0;
    int     rockMaxSteps    = 0;
    boolean rockRight       = true;
//}}}

//{{{ Constructor(s)
//##################################################################################################
    /**
    * Constructor
    */
    public UIDisplayMenu(KingMain kmain)
    {
        kMain = kmain;
        
        int steptime = kMain.prefs.getInt("autoRockCycleTime") / kMain.prefs.getInt("autoRockCycleSteps");
        autoRockTimer = new javax.swing.Timer(steptime, new ReflectiveAction(null, null, this, "onDispAutoRockStep"));
        autoRockTimer.setRepeats(true);
        autoRockTimer.setCoalesce(false);
        rockMaxSteps    = kMain.prefs.getInt("autoRockCycleSteps") / 2;
        rockStepCount   = rockMaxSteps / 2;
        rockStepSize    = (float)Math.toRadians(2.0 * kMain.prefs.getDouble("autoRockDegrees") / (double)rockMaxSteps);
    }
//}}}

//{{{ getMenu
//##################################################################################################
    public JMenu getMenu()
    {
        if(menu != null) return menu;
        
        menu = new JMenu("Display");
        menu.setMnemonic(KeyEvent.VK_D);
        
        aspectMenu = new JMenu("Aspects");
        aspectMenu.setMnemonic(KeyEvent.VK_A);
        menu.add(aspectMenu);
        rebuildAspectsMenu();
        menu.addSeparator();
        
        cbPersp = new JCheckBoxMenuItem(new ReflectiveAction("Use perspective", null, this, "onDispPersp"));
        cbPersp.setMnemonic(KeyEvent.VK_U);
        cbPersp.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_P, 0)); // 0 => no modifiers
        menu.add(cbPersp);
        cbStereo = new JCheckBoxMenuItem(new ReflectiveAction("Use stereo", null, this, "onDispStereo"));
        cbStereo.setMnemonic(KeyEvent.VK_S);
        cbStereo.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, 0)); // 0 => no modifiers
        menu.add(cbStereo);
        cbCrosseye = new JCheckBoxMenuItem(new ReflectiveAction("Crosseye stereo", null, this, "onDispCrosseye"));
        cbCrosseye.setMnemonic(KeyEvent.VK_C);
        cbCrosseye.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, 0)); // 0 => no modifiers
        menu.add(cbCrosseye);
        menu.addSeparator();
        
        cbBigMarkers = new JCheckBoxMenuItem(new ReflectiveAction("Big markers", null, this, "onDispBigMarkers"));
        cbBigMarkers.setMnemonic(KeyEvent.VK_B);
        cbBigMarkers.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q, 0));
        menu.add(cbBigMarkers);
        cbBigLabels = new JCheckBoxMenuItem(new ReflectiveAction("Big labels", null, this, "onDispBigLabels"));
        cbBigLabels.setMnemonic(KeyEvent.VK_G);
        cbBigLabels.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_W, 0));
        menu.add(cbBigLabels);
        cbThickness = new JCheckBoxMenuItem(new ReflectiveAction("Cue by thickness", null, this, "onDispThickness"));
        cbThickness.setMnemonic(KeyEvent.VK_T);
        cbThickness.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_T, KeyEvent.SHIFT_MASK));
        menu.add(cbThickness);
        cbThin = new JCheckBoxMenuItem(new ReflectiveAction("Thin lines", null, this, "onDispThin"));
        cbThin.setMnemonic(KeyEvent.VK_N);
        cbThin.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_T, 0)); // 0 => no modifiers
        menu.add(cbThin);
        
        Map map = new HashMap();
        map.put("Standard", new Integer(KinCanvas.QUALITY_GOOD));
        map.put("Better", new Integer(KinCanvas.QUALITY_BETTER));
        map.put("Best", new Integer(KinCanvas.QUALITY_BEST));
        map.put("OpenGL", new Integer(KinCanvas.QUALITY_JOGL));
        Collection list = Arrays.asList(new String[] {"Standard", "Better", "Best", "OpenGL"});
        String defQual = "Standard";
        if(kMain.getCanvas().renderQuality == KinCanvas.QUALITY_BETTER)
            defQual = "Better"; // for OS X
        else if(kMain.getCanvas().renderQuality == KinCanvas.QUALITY_JOGL)
            defQual = "OpenGL"; // for king_prefs:joglByDefault = true
        JMenuItem submenu = new DispQualityList(map, list, defQual).getMenu();
        submenu.setText("Rendering quality");
        menu.add(submenu);
        menu.addSeparator();
        
        JMenuItem item = new JMenuItem(new ReflectiveAction("Set contrast...", null, this, "onDispContrast"));
        item.setMnemonic(KeyEvent.VK_O);
        //item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Y, 0)); // 0 => no modifiers
        menu.add(item);
        cbIntensity = new JCheckBoxMenuItem(new ReflectiveAction("Cue by intensity", null, this, "onDispIntensity"));
        cbIntensity.setMnemonic(KeyEvent.VK_I);
        cbIntensity.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Y, 0)); // 0 => no modifiers
        menu.add(cbIntensity);
        cbBackground = new JCheckBoxMenuItem(new ReflectiveAction("White background", null, this, "onDispBackground"));
        cbBackground.setMnemonic(KeyEvent.VK_W);
        cbBackground.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_G, 0)); // 0 => no modifiers
        menu.add(cbBackground);
        cbMonochrome = new JCheckBoxMenuItem(new ReflectiveAction("Monochrome", null, this, "onDispMonochrome"));
        cbMonochrome.setMnemonic(KeyEvent.VK_M);
        menu.add(cbMonochrome);
        cbColorByList = new JCheckBoxMenuItem(new ReflectiveAction("Color by list", null, this, "onDispColorByList"));
        cbColorByList.setMnemonic(KeyEvent.VK_L);
        cbColorByList.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_L, 0)); // 0 => no modifiers
        menu.add(cbColorByList);
        menu.addSeparator();
        
        autoRockMenuItem = new JCheckBoxMenuItem(new ReflectiveAction("Auto-rock", null, this, "onDispAutoRockStartStop"));
        autoRockMenuItem.setMnemonic(KeyEvent.VK_R);
        autoRockMenuItem.setSelected(autoRockTimer.isRunning());
        autoRockMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_R, 0)); // 0 => no modifiers
        menu.add(autoRockMenuItem);
        
        syncCheckboxes();
        
        return menu;
    }
//}}}

//{{{ syncCheckboxes
//##################################################################################################
    /**
    * Adjusts the selection state of checkboxes in the Display menu to match parameters in the engine
    */
    void syncCheckboxes()
    {
        if(menu == null) return;
        KinCanvas kCanvas = kMain.getCanvas();
        Engine engine = kCanvas.getEngine();
        
        cbPersp.setSelected(engine.usePerspective);
        if(engine instanceof Engine2D)
        {
            Engine2D e2d = (Engine2D) engine;
            cbStereo.setSelected(e2d.useStereo);
            cbCrosseye.setSelected(e2d.stereoRotation < 0);
        }
        cbBigMarkers.setSelected(engine.bigMarkers);
        cbBigLabels.setSelected(engine.bigLabels);
        cbThickness.setSelected(engine.cueThickness);
        cbThin.setSelected(engine.thinLines);
        cbIntensity.setSelected(engine.cueIntensity);
        cbBackground.setSelected(engine.whiteBackground);
        cbMonochrome.setSelected(engine.monochrome);
        cbColorByList.setSelected(engine.colorByList);
        
        Kinemage kin = kMain.getKinemage();
        if(kin == null) return;
        
        cbPersp.setSelected(kin.atPerspective);
        cbThickness.setSelected(!kin.atOnewidth);
        cbThin.setSelected(kin.atThinline);
        cbBackground.setSelected(kin.atWhitebackground);
        cbColorByList.setSelected(kin.atListcolordominant);
        
    }
//}}}

//{{{ rebuildAspectsMenu
//##################################################################################################
    /**
    * Refills the Display | Aspects menu from the
    * specified iterator of Aspect objects
    */
    public void rebuildAspectsMenu()
    {
        if(aspectMenu == null) return;
        aspectMenu.removeAll();

        JMenuItem item;
        JRadioButtonMenuItem ritem;
        ButtonGroup rgroup = new ButtonGroup();
        
        Kinemage kin = kMain.getKinemage();
        if(kin != null)
        {
            ritem = new JRadioButtonMenuItem(new ReflectiveAction("Aspects off", null, this, "onDisplayAspectsOff"));
            ritem.setMnemonic(KeyEvent.VK_O);
            ritem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SLASH, 0)); // 0 => no modifiers
            rgroup.add(ritem);
            aspectMenu.add(ritem);
            aspectMenu.addSeparator();
            
            int i = 1;
            for(Aspect aspect : kin.getAspects())
            {
                ritem = new JRadioButtonMenuItem(new AspectAction((i++), aspect));
                rgroup.add(ritem);
                aspectMenu.add(ritem);
            }

            /* Next / Previous */
            if(i > 1) aspectMenu.addSeparator();
            item = new JMenuItem(new ReflectiveAction("Next aspect", null, this, "onDisplayAspectsNext"));
            item.setMnemonic(KeyEvent.VK_N);
            item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_CLOSE_BRACKET, 0)); // 0 => no modifiers
            aspectMenu.add(item);
            item = new JMenuItem(new ReflectiveAction("Previous aspect", null, this, "onDisplayAspectsPrevious"));
            item.setMnemonic(KeyEvent.VK_P);
            item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_OPEN_BRACKET, 0));
            aspectMenu.add(item);
            /* Next / Previous */
        }
        else
        {
            item = new JMenuItem("No aspects available");
            item.setEnabled(false);
            aspectMenu.add(item);
        }

        // This step is ESSENTIAL for the menu to appear & keep working!
        //menubar.revalidate();
    }
//}}}

//{{{ onDisplayAspects{Off, Next, Previous}
//##################################################################################################
    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onDisplayAspectsOff(ActionEvent ev)
    {
        kMain.getCanvas().setCurrentAspect(null);
    }

    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onDisplayAspectsNext(ActionEvent ev)
    {
        Kinemage kin = kMain.getKinemage();
        if(kin == null) return;
        Aspect currAspect = kMain.getCanvas().getCurrentAspect();
        List<Aspect> aspects = kin.getAspects();
        if(aspects.size() == 0)
            kMain.getCanvas().setCurrentAspect(null);
        else if(currAspect == null) // if nothing's selected yet
            kMain.getCanvas().setCurrentAspect(aspects.get(0));
        else // if something's already selected
        {
            int i = aspects.indexOf(currAspect);
            if(i == aspects.size()-1) i = 0; // if current was last, choose first
            else i = i+1;
            kMain.getCanvas().setCurrentAspect(aspects.get(i));
        }
    }

    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onDisplayAspectsPrevious(ActionEvent ev)
    {
        Kinemage kin = kMain.getKinemage();
        if(kin == null) return;
        Aspect currAspect = kMain.getCanvas().getCurrentAspect();
        List<Aspect> aspects = kin.getAspects();
        if(aspects.size() == 0)
            kMain.getCanvas().setCurrentAspect(null);
        else if(currAspect == null) // if nothing's selected yet
            kMain.getCanvas().setCurrentAspect(aspects.get(aspects.size()-1));
        else // if something's already selected
        {
            int i = aspects.indexOf(currAspect);
            if(i == 0) i = aspects.size()-1; // if current was first, choose last
            else i = i-1;
            kMain.getCanvas().setCurrentAspect(aspects.get(i));
        }
    }
//}}}

//{{{ onDisp{Persp, Stereo, Crosseye}
//##################################################################################################
    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onDispPersp(ActionEvent ev)
    {
        Kinemage kin = kMain.getKinemage();
        if(kin != null)
        {
            kin.atPerspective = cbPersp.isSelected();
            // Deliberately don't mark kin as modified
            kMain.publish(new KMessage(this, KMessage.DISPLAY_OPTIONS));
        }
    }

    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onDispStereo(ActionEvent ev)
    {
        if(kMain.getCanvas() != null)
        {
            Engine engine = kMain.getCanvas().getEngine();
            if(engine != null && engine instanceof Engine2D)
                ((Engine2D)engine).useStereo = cbStereo.isSelected();
            kMain.publish(new KMessage(this, KMessage.DISPLAY_OPTIONS));
        }
    }

    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onDispCrosseye(ActionEvent ev)
    {
        if(kMain.getCanvas() != null)
        {
            Engine engine = kMain.getCanvas().getEngine();
            if(engine != null && engine instanceof Engine2D)
            {
                Engine2D e2d = (Engine2D) engine;
                e2d.stereoRotation
                    = (cbCrosseye.isSelected() ^ e2d.stereoRotation < 0) ? -e2d.stereoRotation : e2d.stereoRotation;
            }
            kMain.publish(new KMessage(this, KMessage.DISPLAY_OPTIONS));
        }
    }
//}}}

//{{{ onDisp{BigMarkers, BigLabels, Thickness, Thin}
//##################################################################################################
    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onDispBigMarkers(ActionEvent ev)
    {
        if(kMain.getCanvas() != null)
        {
            Engine engine = kMain.getCanvas().getEngine();
            if(engine != null) engine.bigMarkers = cbBigMarkers.isSelected();
            kMain.publish(new KMessage(this, KMessage.DISPLAY_OPTIONS));
        }
    }
    
    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onDispBigLabels(ActionEvent ev)
    {
        if(kMain.getCanvas() != null)
        {
            Engine engine = kMain.getCanvas().getEngine();
            if(engine != null) engine.bigLabels = cbBigLabels.isSelected();
            kMain.publish(new KMessage(this, KMessage.DISPLAY_OPTIONS));
        }
    }
    
    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onDispThickness(ActionEvent ev)
    {
        Kinemage kin = kMain.getKinemage();
        if(kin != null)
        {
            kin.atOnewidth = ! cbThickness.isSelected();
            // Deliberately don't mark kin as modified
            kMain.publish(new KMessage(this, KMessage.DISPLAY_OPTIONS));
        }
    }

    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onDispThin(ActionEvent ev)
    {
        Kinemage kin = kMain.getKinemage();
        if(kin != null)
        {
            kin.atThinline = cbThin.isSelected();
            // Deliberately don't mark kin as modified
            // Assume we don't want width depth cues with thinline
            if(cbThin.isSelected())
            {
                kin.atOnewidth = true;
                cbThickness.setSelected(false);
            }
            kMain.publish(new KMessage(this, KMessage.DISPLAY_OPTIONS));
        }
    }
//}}}

//{{{ onDisp{Intensity, Background, Monochrome, ColorByList}
//##################################################################################################
    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onDispIntensity(ActionEvent ev)
    {
        if(kMain.getCanvas() != null)
        {
            Engine engine = kMain.getCanvas().getEngine();
            if(engine != null) engine.cueIntensity = cbIntensity.isSelected();
            kMain.publish(new KMessage(this, KMessage.DISPLAY_OPTIONS));                
        }
    }

    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onDispBackground(ActionEvent ev)
    {
        Kinemage kin = kMain.getKinemage();
        if(kin != null)
        {
            kin.atWhitebackground = cbBackground.isSelected();
            // Deliberately don't mark kin as modified
            kMain.publish(new KMessage(this, KMessage.DISPLAY_OPTIONS));
        }
    }

    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onDispMonochrome(ActionEvent ev)
    {
        if(kMain.getCanvas() != null)
        {
            Engine engine = kMain.getCanvas().getEngine();
            if(engine != null)
            {
                boolean sel = cbMonochrome.isSelected();
                engine.monochrome = sel; 
                // assume we want white background with monochrome
                if(sel == true)
                {
                    engine.whiteBackground = true;
                    cbBackground.setSelected(true);
                }
            }
            kMain.publish(new KMessage(this, KMessage.DISPLAY_OPTIONS));                
        }
    }

    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onDispColorByList(ActionEvent ev)
    {
        Kinemage kin = kMain.getKinemage();
        if(kin != null)
        {
            kin.atListcolordominant = cbColorByList.isSelected();
            // Deliberately don't mark kin as modified
            kMain.publish(new KMessage(this, KMessage.DISPLAY_OPTIONS));
        }
    }
//}}}

//{{{ onDisplayAutoRock{StartStop, Step}
//##################################################################################################
    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onDispAutoRockStartStop(ActionEvent ev)
    {
        Kinemage k = kMain.getKinemage();
        if(k == null) return;
        if(autoRockMenuItem.isSelected())
        {
            rockStepCount   = rockMaxSteps / 2;
            rockRight       = true;
            autoRockTimer.start();
        }
        else
        {
            autoRockTimer.stop();
        }
    }

    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onDispAutoRockStep(ActionEvent ev)
    {
        KView v = kMain.getView();
        if(v == null) return;
        
        if(rockStepCount >= rockMaxSteps)
        {
            rockStepCount = 0;
            rockRight = !rockRight;
        }
        
        // Stiff bouncing:
        //if(rockRight) v.rotateY( rockStepSize);
        //else          v.rotateY(-rockStepSize);
        
        // Slows and pauses at either end:
        if(rockRight) v.rotateY((float)(2 *  rockStepSize * Math.sin((Math.PI*rockStepCount)/rockMaxSteps)));
        else          v.rotateY((float)(2 * -rockStepSize * Math.sin((Math.PI*rockStepCount)/rockMaxSteps)));
        
        rockStepCount++;
        
        kMain.publish(new KMessage(this, KMessage.VIEW_MOVED));
    }
//}}}

//{{{ onDispContrast
//##################################################################################################
    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onDispContrast(ActionEvent ev)
    {
        JSlider slider = new JSlider(0, 100, 50);
        
        int response = JOptionPane.showConfirmDialog(kMain.getTopWindow(),
            slider, "Set display contrast",
            JOptionPane.OK_CANCEL_OPTION , JOptionPane.PLAIN_MESSAGE);
        
        if(response == JOptionPane.OK_OPTION)
        {
            int contrast = slider.getValue() - 50;
            KPalette.setContrast(1.0 + contrast/50.0);
            kMain.publish(new KMessage(this, KMessage.DISPLAY_OPTIONS));                
        }
    }
//}}}

//{{{ empty_code_segment
//##################################################################################################
//}}}
}//class

