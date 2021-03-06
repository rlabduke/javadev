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
import driftwood.r3.*; // (ARK Spring2010)

import java.util.List;
import javax.swing.Timer;
//}}}
/**
* <code>UIDisplayMenu</code> encapsulates all the functions
* listed in KiNG's "Display" menu.
*
* <p>Copyright (C) 2003-2007 by Ian W. Davis. All rights reserved.
* <br>Begun on Mon May 26 12:30:25 EDT 2003
*/
public class UIDisplayMenu implements KMessage.Subscriber, ChangeListener // added ChangeListener (ARK Spring2010)
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

//{{{ INNER CLASS: ResizeCanvasDialog
//##################################################################################################
    class ResizeCanvasDialog extends JDialog
    {
        
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
    JCheckBoxMenuItem autoRockMenuItem = null, autoAnimMenuItem = null;

    // Timers, etc for auto-xxx functions
    Timer   autoRockTimer   = null;
    float   rockStepSize    = 0;
    int     rockStepCount   = 0;
    int     rockMaxSteps    = 0;
    boolean rockRight       = true;
    
    Timer   autoAnimTimer   = null;
    
    // For Ribbon Sides menu (ARK Spring2010)
    JSlider hASlider, sASlider, vASlider, hBSlider, sBSlider, vBSlider; 
    JCheckBox isOnAlpha, isOnBeta;
    JButton restoreDefaults;
//}}}

//{{{ Constructor(s)
//##################################################################################################
    /**
    * Constructor
    */
    public UIDisplayMenu(KingMain kmain)
    {
        kMain = kmain;
        
        int steptime    = kMain.prefs.getInt("autoRockCycleTime") / kMain.prefs.getInt("autoRockCycleSteps");
        autoRockTimer   = new Timer(steptime, new ReflectiveAction(null, null, this, "onDispAutoRockStep"));
        //autoRockTimer.setRepeats(true);
        //autoRockTimer.setCoalesce(false);
        rockMaxSteps    = kMain.prefs.getInt("autoRockCycleSteps") / 2;
        rockStepCount   = rockMaxSteps / 2;
        rockStepSize    = (float)Math.toRadians(2.0 * kMain.prefs.getDouble("autoRockDegrees") / (double)rockMaxSteps);
        
        autoAnimTimer   = new Timer(kMain.prefs.getInt("autoAnimateDelay"), new ReflectiveAction(null, null, this, "onDispAutoAnimStep"));
        
        kMain.subscribe(this);
    }
//}}}

//{{{ deliverMessage
//##################################################################################################
    static final long SYNC_CHECKBOXES = KMessage.DISPLAY_OPTIONS | KMessage.KIN_SWITCHED;
    public void deliverMessage(KMessage msg)
    {
        if(msg.getSource() != this && msg.testProg(SYNC_CHECKBOXES))
        {
            syncCheckboxes();
        }
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
        
        JMenuItem itemCanvasSize = new JMenuItem(new ReflectiveAction("Resize canvas...", null, this, "onResizeCanvas"));
        itemCanvasSize.setMnemonic(KeyEvent.VK_I);
        menu.add(itemCanvasSize);
        JMenuItem itemContrast = new JMenuItem(new ReflectiveAction("Set contrast...", null, this, "onDispContrast"));
        itemContrast.setMnemonic(KeyEvent.VK_O);
        //itemContrast.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Y, 0)); // 0 => no modifiers
        menu.add(itemContrast);
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
        JMenuItem itemRibbonSides = new JMenuItem(new ReflectiveAction("Ribbon sides...", null, this, "onDispRibbonSides"));
        itemRibbonSides.setMnemonic(KeyEvent.VK_S);
        menu.add(itemRibbonSides);		// (ARK Spring2010)
        menu.addSeparator();
        
        autoRockMenuItem = new JCheckBoxMenuItem(new ReflectiveAction("Auto-rock", null, this, "onDispAutoRockStartStop"));
        autoRockMenuItem.setMnemonic(KeyEvent.VK_R);
        autoRockMenuItem.setSelected(autoRockTimer.isRunning());
        autoRockMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_R, 0)); // 0 => no modifiers
        menu.add(autoRockMenuItem);
        autoAnimMenuItem = new JCheckBoxMenuItem(new ReflectiveAction("Auto-animate", null, this, "onDispAutoAnimStartStop"));
        //autoAnimMenuItem.setMnemonic(KeyEvent.VK_R);
        autoAnimMenuItem.setSelected(autoAnimTimer.isRunning());
        //autoAnimMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_R, 0)); // 0 => no modifiers
        menu.add(autoAnimMenuItem);
        
        syncCheckboxes();
        
        return menu;
    }
//}}}

//{{{ syncCheckboxes
//##################################################################################################
    /**
    * Adjusts the selection state of checkboxes in the Display menu
    * to match parameters in the engine and the kinemage.
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

//{{{ onDispAutoRock{StartStop, Step}
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
    }
//}}}

//{{{ onDispAutoAnim{StartStop, Step}
//##################################################################################################
    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onDispAutoAnimStartStop(ActionEvent ev)
    {
        //Kinemage k = kMain.getKinemage();
        //if(k == null) return;
        if(autoAnimMenuItem.isSelected())
        {
            String time = JOptionPane.showInputDialog(kMain.getTopWindow(),
                "Rate of animation? (milliseconds)", Integer.toString(autoAnimTimer.getDelay()));
            try { autoAnimTimer.setDelay(Integer.parseInt(time)); }
            catch(NumberFormatException ex) {}
            autoAnimTimer.start();
        }
        else
        {
            autoAnimTimer.stop();
        }
    }

    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onDispAutoAnimStep(ActionEvent ev)
    {
        Kinemage k = kMain.getKinemage();
        if(k == null) return;
        k.animate(1);
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

//{{{ onDispRibbonSides
//##################################################################################################
    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onDispRibbonSides(ActionEvent ev) // (ARK Spring2010)
    {
    	Engine engine = kMain.getCanvas().getEngine();
        Kinemage kin = kMain.getKinemage(); /////???
	if(kin!=null && engine!=null){
        	// Get initial values in case user cancels, there's gotta be an easier way to do this...
        	boolean ribbonSidesAlpha0 = kin.atSidedcoloringAlpha;
        	boolean ribbonSidesBeta0 = kin.atSidedcoloringBeta;
        	// use triple class for its constructors and .like() method, could probably instead use Integer[]...
        	Triple backHSValpha0 = new Triple().like(engine.curBackHSValpha);
        	Triple backHSVbeta0 = new Triple().like(engine.curBackHSVbeta);
        	
        	isOnAlpha = new JCheckBox("Color by side (alpha)");
        	isOnAlpha.setSelected(kin.atSidedcoloringAlpha); 
        	isOnAlpha.addChangeListener(this);
        	isOnBeta = new JCheckBox("Color by side (beta)");
        	isOnBeta.setSelected(kin.atSidedcoloringBeta); 
        	isOnBeta.addChangeListener(this);
		hASlider = new JSlider(0, 360, (int)engine.curBackHSValpha.getX());
		hASlider.addChangeListener(this);
		sASlider = new JSlider(0, 100, (int)engine.curBackHSValpha.getY());
		sASlider.addChangeListener(this);
		vASlider = new JSlider(0, 100, (int)engine.curBackHSValpha.getZ());
		vASlider.addChangeListener(this);
		hBSlider = new JSlider(0, 360, (int)engine.curBackHSVbeta.getX());
		hBSlider.addChangeListener(this);
		sBSlider = new JSlider(0, 100, (int)engine.curBackHSVbeta.getY());
		sBSlider.addChangeListener(this);
		vBSlider = new JSlider(0, 100, (int)engine.curBackHSVbeta.getZ());
		vBSlider.addChangeListener(this);
		restoreDefaults = new JButton("Restore defaults");
		restoreDefaults.addChangeListener(this);
		
		Object[] msg = {isOnAlpha,"Alpha hue offset: ", hASlider,"Alpha saturation offset: ", sASlider, "Alpha brightness offset: ", vASlider,
				new JSeparator(), 
				isOnBeta, "Beta hue offset: ",hBSlider, "Beta saturation offset: ", sBSlider, "Beta brightness offset: ", vBSlider,
				restoreDefaults};
			
        	JOptionPane op = new JOptionPane(msg, JOptionPane.PLAIN_MESSAGE, JOptionPane.OK_CANCEL_OPTION);
		JDialog dialog = op.createDialog(kMain.getTopWindow(), "Set ribbon backside color offsets");
		dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
		dialog.setVisible(true);
        	
		if(op.getValue()!=null && ((Integer)op.getValue()).intValue()==JOptionPane.CANCEL_OPTION ){
        	    // restore initial values
		    kin.atSidedcoloringAlpha = ribbonSidesAlpha0;
        	    kin.atSidedcoloringBeta = ribbonSidesBeta0;
        	    engine.curBackHSValpha.setXYZ(backHSValpha0.getX(),backHSValpha0.getY(),backHSValpha0.getZ());
        	    engine.curBackHSVbeta.setXYZ(backHSVbeta0.getX(),backHSVbeta0.getY(),backHSVbeta0.getZ());
        	}
		else  
		    kMain.publish(new KMessage(this, KMessage.DISPLAY_OPTIONS));   
	}
    }
//}}}

//{{{ onResizeCanvas
//##################################################################################################
    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onResizeCanvas(ActionEvent ev)
    {
        JDialog dialog = new JDialog();
        Object[] sizes = {"1024 x 768", "675 x 675", "500 x 500", "Other..."};
        String s = (String) JOptionPane.showInputDialog(
            dialog, "Resize canvas to:", "Resize canvas", JOptionPane.PLAIN_MESSAGE, null, 
            sizes, "1024 x 768");
        
        if (s != null && s.length() > 0)
        {
            // Apparently packing the ContentPane twice is sometimes necessary.
            // For example, when the monitor size prevents the first pack from 
            // reaching the full preferred size for the KinCanvas graphics area, 
            // the second pack for some reason extends the entire MainWindow 
            // past the monitor's viewing area so the graphics area reaches its
            // full preferred size. (DAK 100118)
            if     (s.equals("1024 x 768"))  resizeCanvas(1024, 768);
            else if(s.equals("675 x 675" ))  resizeCanvas(675 , 675);
            else if(s.equals("500 x 500" ))  resizeCanvas(500 , 500);
            else if(s.equals("Other..."  ))
            {
                String t = (String)JOptionPane.showInputDialog(new JDialog(), 
                    "Enter desired size (format: \"W x H\" or \"WxH\"):",
                    "Resize canvas to custom size", JOptionPane.PLAIN_MESSAGE,
                    null, null, null);
                if ((t != null) && (t.length() > 0))
                {
                    String u = t.replaceAll("\\s+", ""); // trim whitespace
                    try
                    {
                        int i = u.indexOf("x");
                        int w = Integer.parseInt(u.substring(0, i));
                        int h = Integer.parseInt(u.substring(i+1));
                        resizeCanvas(w, h);
                    }
                    catch(StringIndexOutOfBoundsException ex)
                    { throwResizeCanvasFormatException(); }
                    catch(NullPointerException ex)
                    { throwResizeCanvasFormatException(); }
                    catch(NumberFormatException ex)
                    { throwResizeCanvasFormatException(); }
                }
            }
        }
    }
//}}}

//{{{ resizeCanvas
//##################################################################################################
    public void resizeCanvas(int w, int h)
    {
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment(); 
        GraphicsDevice[] gs = ge.getScreenDevices();
        for (int i = 0; i < gs.length; i++)
        {
            GraphicsDevice gd = gs[i];
            GraphicsConfiguration gc = gd.getDefaultConfiguration();
            Rectangle r = gc.getBounds();
            if(r.contains(kMain.getTopWindow().getLocation()))
            {
                // KiNG window is on this screen (as opposed to e.g. 
                // the other monitor in a dual display setup)
                DisplayMode dm = gd.getDisplayMode();
                int screenWidth = dm.getWidth();
                int screenHeight = dm.getHeight();
                if(w > screenWidth || h > screenHeight)
                {
                    JOptionPane.showMessageDialog(new JDialog(), 
                        "Requested canvas size ("+w+" x "+h+") is too big for\nthe KiNG "+
                        "window's current screen ("+screenWidth+" x "+screenHeight+")!",
                        "That's not gonna fit...", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                else // proceed with resize
                {
                    kMain.getContentPane().resetSplits();
                    kMain.getCanvas().setPreferredSize(new Dimension(w, h));
                    kMain.getTopWindow().pack();
                    kMain.getTopWindow().pack();
                    kMain.publish(new KMessage(this, KMessage.DISPLAY_OPTIONS));
                }
            }
        }
    }

    void throwResizeCanvasFormatException()
    {
        JOptionPane.showMessageDialog(
            new JDialog(), "Required format: \"W x H\" or \"WxH\"!", 
            "Wrong width-by-height format!", JOptionPane.ERROR_MESSAGE);
    }
//}}}

//{{{ stateChanged
//##################################################################################################
    public void stateChanged(ChangeEvent ev) // (ARK Spring2010)
    {
        Object src = ev.getSource();
        Engine engine = kMain.getCanvas().getEngine();
	Kinemage kin = kMain.getKinemage(); /////???

        if(src == isOnAlpha) kin.atSidedcoloringAlpha = isOnAlpha.isSelected();
        else if(src == isOnBeta) kin.atSidedcoloringBeta = isOnBeta.isSelected();
        else if(src == hASlider) engine.curBackHSValpha.setX(hASlider.getValue());
        else if(src == sASlider) engine.curBackHSValpha.setY(sASlider.getValue());
        else if(src == vASlider) engine.curBackHSValpha.setZ(vASlider.getValue());
        else if(src == hBSlider) engine.curBackHSVbeta.setX(hBSlider.getValue());
        else if(src == sBSlider) engine.curBackHSVbeta.setY(sBSlider.getValue());
        else if(src == vBSlider) engine.curBackHSVbeta.setZ(vBSlider.getValue());
        else if(src == restoreDefaults){
        	engine.curBackHSValpha.setXYZ(engine.defBackHSValpha.getX(),engine.defBackHSValpha.getY(),engine.defBackHSValpha.getZ());
        	engine.curBackHSVbeta.setXYZ(engine.defBackHSVbeta.getX(),engine.defBackHSVbeta.getY(),engine.defBackHSVbeta.getZ());
        	hASlider.setValue((int)engine.curBackHSValpha.getX());
        	sASlider.setValue((int)engine.curBackHSValpha.getY());
        	vASlider.setValue((int)engine.curBackHSValpha.getZ());
        	hBSlider.setValue((int)engine.curBackHSVbeta.getX());
        	sBSlider.setValue((int)engine.curBackHSVbeta.getY());
        	vBSlider.setValue((int)engine.curBackHSVbeta.getZ());
        }
        else System.err.println("Unknown event source: "+src);
        
        kMain.getCanvas().repaint();
    }
//}}}

//{{{ empty_code_segment
//##################################################################################################
//}}}
}//class

