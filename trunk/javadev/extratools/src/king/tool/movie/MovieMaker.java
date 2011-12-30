// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package king.tool.movie;
import king.*;
import king.core.*;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.io.*;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.*;
//import java.util.regex.*;
import javax.imageio.*; // J2SE 1.4+
import javax.swing.*;
import javax.swing.event.*;
import driftwood.gui.*;
import driftwood.util.ReflectiveRunnable;

import java.util.List;
import javax.swing.Timer;
//}}}
/**
* <code>MovieMaker</code> has not yet been documented.
*
* <p>Copyright (C) 2007 by Ian W. Davis. All rights reserved.
* <br>Begun on Mon Jan 15 10:01:22 EST 2007
*/
public class MovieMaker implements ListSelectionListener
{
//{{{ Constants
//}}}

//{{{ CLASS: MovieRenderer
//##############################################################################
    /**
    * Rendering of frames has to be done on the event thread -- no way around it.
    * Otherwise KinCanvas notices the changing views and tries to redraw at the
    * same time a background thread is trying to draw to the image file.
    * This implementation uses a Timer to exectute one scene at a time on
    * the event thread, with a small break in between.
    * The break allows the progress dialog to repaint, etc.
    */
    class MovieRenderer implements ActionListener
    {
        private Scene[] myScenes;
        private int whichScene;
        private ProgressDialog progDlg;
        private Timer timer;
        
        public MovieRenderer()
        {
            this.myScenes   = scenes.toArray(new Scene[scenes.size()]);
            this.whichScene = 0;
            this.progDlg    = null;
            this.timer      = new Timer(250, this);
            timer.setRepeats(false);
            timer.start();
        }
        
        /** Always runs on the event dispatch thread! */
        public void actionPerformed(ActionEvent ev)
        {
            // startup
            if(progDlg == null)
            {
                progDlg = new ProgressDialog(kMain.getTopWindow(), "Rendering movie...", true);
                progDlg.show(); // non-blocking on this thread
                timer.start();
                return;
            }
            // abort if canceled
            if(progDlg.isCanceled())
            {
                progDlg.dispose();
                return;
            }
            
            if(whichScene > 0)
                myScenes[whichScene-1].gotoEndState();
            try { myScenes[whichScene].renderFrames(MovieMaker.this); }
            catch(IOException ex) { ex.printStackTrace(); }
            progDlg.update(frameCounter, expectedFrames);
            
            whichScene++;
            
            // shutdown ...
            if(whichScene == myScenes.length)
            {
                progDlg.dispose();
            }
            // or queue up the next scene
            else timer.start();
        }
    }
//}}}

//{{{ Variable definitions
//##############################################################################
    KingMain        kMain;
    int             frameCounter = 0, expectedFrames = 0;
    String          movieFilePrefix = null;
    BufferedImage   lastImage = null;
    
    List<Scene>     scenes;
    JList           sceneJList;
    JFileChooser    fileChooser;
//}}}

//{{{ Constructor(s)
//##############################################################################
    public MovieMaker(KingMain kMain)
    {
        super();
        this.kMain = kMain;
        this.scenes = new ArrayList<Scene>();
        buildGUI();
    }
//}}}

//{{{ buildGUI
//##############################################################################
    private void buildGUI()
    {
        fileChooser = new JFileChooser();
        String currdir = System.getProperty("user.dir");
        if(currdir != null) fileChooser.setCurrentDirectory(new File(currdir));
        
        sceneJList = new FatJList(0, 10);
        sceneJList.setListData(scenes.toArray());
        sceneJList.addListSelectionListener(this);
        
        JButton renderBtn = new JButton(new ReflectiveAction("Render movie ...", null, this, "onStartRenderMovie"));
        JButton btn500x500 = new JButton(new ReflectiveAction("500 x 500", null, this, "onSize500x500"));
        JButton btn600x400 = new JButton(new ReflectiveAction("600 x 400", null, this, "onSize600x400"));
        
        JButton snapshotBtn = new JButton(new ReflectiveAction("+ Snapshot", null, this, "onAddSnapshotScene"));
            snapshotBtn.setHorizontalAlignment(JButton.LEFT);
        JButton rockBtn = new JButton(new ReflectiveAction("+ Rock", null, this, "onAddRockScene"));
            rockBtn.setHorizontalAlignment(JButton.LEFT);
        JButton spinBtn = new JButton(new ReflectiveAction("+ Spin", null, this, "onAddSpinScene"));
            spinBtn.setHorizontalAlignment(JButton.LEFT);
        JButton flyBtn = new JButton(new ReflectiveAction("+ Fly", null, this, "onAddFlyingScene"));
            flyBtn.setHorizontalAlignment(JButton.LEFT);
        JButton animateBtn = new JButton(new ReflectiveAction("+ Animate", null, this, "onAddAnimateScene"));
            animateBtn.setHorizontalAlignment(JButton.LEFT);
        JButton deleteBtn = new JButton(new ReflectiveAction("- Delete", null, this, "onDeleteScene"));
            deleteBtn.setHorizontalAlignment(JButton.LEFT);
        JButton configBtn = new JButton(new ReflectiveAction("Configure ...", null, this, "onConfigureScene"));
            configBtn.setHorizontalAlignment(JButton.LEFT);
        
        TablePane2 cp = new TablePane2();
        cp.hfill(true).vfill(true).addCell(new JScrollPane(sceneJList));
        cp.startSubtable().hfill(true).memorize();
            cp.addCell(snapshotBtn).newRow();
            cp.addCell(rockBtn).newRow();
            cp.addCell(spinBtn).newRow();
            cp.addCell(flyBtn).newRow();
            cp.addCell(animateBtn).newRow();
            cp.addCell(deleteBtn).newRow();
            cp.addCell(configBtn).newRow();
        cp.endSubtable();
        cp.newRow();
        cp.startSubtable(2,1);
            cp.addCell(btn500x500);
            cp.addCell(btn600x400);
            cp.addCell(renderBtn);
        cp.endSubtable();
        
        JDialog dialog = new JDialog(kMain.getTopWindow(), "Movie Maker", false);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.setContentPane(cp);
        dialog.pack();
        dialog.show();
        // non-modal: execution continues...
    }
//}}}

//{{{ onStartRenderMovie
//##############################################################################
    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onStartRenderMovie(ActionEvent ev)
    {
        int result = fileChooser.showSaveDialog(kMain.getTopWindow());
        if(result != JFileChooser.APPROVE_OPTION) return;

        try
        {
            File out = fileChooser.getSelectedFile();
            this.movieFilePrefix = out.getCanonicalPath();
            
            this.frameCounter   = 0;
            this.expectedFrames = 0;
            for(Scene scene : this.scenes)
                expectedFrames += scene.getDuration();
            
            //// This freezes the GUI but is the only thing that seems to work.
            //for(Scene scene: this.scenes)
            //{
            //    scene.renderFrames(this);
            //    scene.gotoEndState();
            //}
            
            new MovieRenderer();
        }
        catch(IOException ex) { ex.printStackTrace(); }
        
        //JOptionPane.showMessageDialog(kMain.getTopWindow(),
        //    "Movie is finished rendering.", "Done!", JOptionPane.INFORMATION_MESSAGE);
    }
//}}}

//{{{ onSize500x500, onSize600x400
//##############################################################################
    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onSize500x500(ActionEvent ev)
    {
        kMain.getCanvas().setPreferredSize(new Dimension(500, 500));
        kMain.getTopWindow().pack();
    }

    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onSize600x400(ActionEvent ev)
    {
        kMain.getCanvas().setPreferredSize(new Dimension(600, 400));
        kMain.getTopWindow().pack();
    }
//}}}

//{{{ ListSelectionListener.valueChanged
//##############################################################################
    /**
    * Every time an item is selected in the list, set the kinemage to look
    * like it would when that scene finished.
    * That way, an inserted scene can continue smoothly from where
    * the other one left off.
    */
    public void valueChanged(ListSelectionEvent ev)
    {
        int i = sceneJList.getSelectedIndex();
        if(i != -1)
            scenes.get(i).gotoEndState();
    }
//}}}

//{{{ onAddSnapshot/Rock/Spin/FlyingScene, addScene
//##############################################################################
    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onAddSnapshotScene(ActionEvent ev)
    { addScene(new SnapshotScene(kMain, 15)); }
    
    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onAddRockScene(ActionEvent ev)
    {
        // 30 degree rock over 6 seconds is nice (90 frames @ 15 FPS)
        // 30 degree rock needs half as many frames as a 360 degree spin to mesh
        // 60 degree rock needs same number of frames as a 360 degree spin to mesh
        addScene(new RockBackAndForthScene(kMain, 90, 30));
    }
    
    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onAddSpinScene(ActionEvent ev)
    { addScene(new SpinAroundScene(kMain, 180, 360)); }
    
    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onAddFlyingScene(ActionEvent ev)
    { addScene(new FlyingCameraScene(kMain, 60)); }
    
    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onAddAnimateScene(ActionEvent ev)
    { addScene(new AnimateScene(kMain, 1)); }
    
    private void addScene(Scene s)
    {
        if(s.configure())
        {
            int i = sceneJList.getSelectedIndex();
            if(i == -1)
                scenes.add(s);
            else
                scenes.add(i+1, s);
            sceneJList.setListData(scenes.toArray());
        }
    }
//}}}

//{{{ onDeleteScene, onConfigureScene
//##############################################################################
    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onDeleteScene(ActionEvent ev)
    {
        int i = sceneJList.getSelectedIndex();
        if(i != -1)
        {
            scenes.remove(i);
            sceneJList.setListData(scenes.toArray());
        }
    }

    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onConfigureScene(ActionEvent ev)
    {
        int i = sceneJList.getSelectedIndex();
        if(i != -1)
        {
            Scene s = scenes.get(i);
            if(s.configure())
                s.gotoEndState();
        }
    }
//}}}


//{{{ writeFrame, getFileForFrame
//##############################################################################
    public void writeFrame() throws IOException
    { writeFrame(1); }
    
    public void writeFrame(int numCopies) throws IOException
    { writeFrame(getKinImage(), numCopies); }
    
    public void writeFrame(BufferedImage img, int numCopies) throws IOException
    {
        this.lastImage = img;
        File file = getFileForFrame(++frameCounter);
        ImageIO.write(lastImage, "png", file);
        
        for(int i = 1; i < numCopies; i++)
        {
            File file2 = getFileForFrame(++frameCounter);
            InputStream src = new BufferedInputStream(new FileInputStream(file));
            OutputStream dst = new BufferedOutputStream(new FileOutputStream(file2));
            byte[] buffer = new byte[2048];
            int len;
            while((len = src.read(buffer)) != -1) dst.write(buffer, 0, len);
            dst.close();
            src.close();
        }
    }
    
    File getFileForFrame(int frameNo)
    {
        String filenum = ""+(frameNo);
        while(filenum.length() < 4) filenum = "0"+filenum;
        File file = new File(movieFilePrefix+"_"+filenum+".png");
        return file;
    }
//}}}

//{{{ getKinImage
//##############################################################################
    public BufferedImage getKinImage()
    {
        // This has to be done on the main Swing thread, or else
        // KinCanvas may try to redraw itself at the same time,
        // which leads to races and exceptions.
        // With JOGL enabled, it actually causes undiagnosable seg faults.

        final int resol = 1;
        KinCanvas kCanvas = kMain.getCanvas();
        Dimension dim = kCanvas.getCanvasSize();
        dim.width *= resol;
        dim.height *= resol;
        BufferedImage img = new BufferedImage(dim.width, dim.height,
            BufferedImage.TYPE_INT_ARGB); // needed so we can get transparency in output
        Graphics2D g2 = img.createGraphics();
        g2.scale(resol, resol);
        kCanvas.paintCanvas(g2, dim, KinCanvas.QUALITY_BEST);
        g2.dispose();
        
        return img;
    }
//}}}

//{{{ blendImages
//##############################################################################
    /**
    * Blends two images using the specified alpha values.
    */
    static public BufferedImage blendImages(BufferedImage i1, double a1, BufferedImage i2, double a2)
    {
        int width = i1.getWidth(), height = i1.getHeight();
        if(width != i2.getWidth() || height != i2.getHeight())
            throw new IllegalArgumentException("Image size mismatch");
        
        BufferedImage out = new BufferedImage(width, height,
            BufferedImage.TYPE_INT_ARGB); // needed so we can get transparency in output
        
        ImageObserver dummyObserver = new ImageObserver() {
            public boolean imageUpdate(Image img, int infoflags, int x, int y, int width, int height)
            { return false; } // == image fully loaded
        };
        
        Graphics2D g2 = out.createGraphics();
        g2.setComposite(AlphaComposite.getInstance(
            AlphaComposite.SRC, (float) a1));
        g2.drawImage(i1, 0, 0, dummyObserver);
        g2.setComposite(AlphaComposite.getInstance(
            AlphaComposite.SRC_OVER, (float) a2));
        g2.drawImage(i2, 0, 0, dummyObserver);
        g2.dispose();
        
        return out;
    }
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}
}//class

