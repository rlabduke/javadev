/*
 * Copyright 2007-2008 Volker Fritzsch
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License. 
 */
package motej.demos.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowStateListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import motej.IrCameraMode;
import motej.IrCameraSensitivity;
import motej.Mote;
import motej.MoteFinder;
import motej.event.CoreButtonEvent;
import motej.event.CoreButtonListener;
import motej.event.IrCameraEvent;
import motej.event.IrCameraListener;
import motej.request.ReportModeRequest;

/**
 * 
 * <p>
 * @author <a href="mailto:vfritzsch@users.sourceforge.net">Volker Fritzsch</a>
 */
@SuppressWarnings("serial")
public class MoteGui {
	
	private int irx1 = 10, iry1 = 100, size1;
	private int irx2, iry2, size2;
	private int irx3, iry3, size3;
	private int irx4, iry4, size4;
	
//	protected class AccelerometerComponent implements GLEventListener {
//		
//		private GLU glu;
//		private GLUT glut;
//		
//		public void init(GLAutoDrawable drawable) {
//			GL gl = drawable.getGL();
//			glu = new GLU();
//			glut = new GLUT();
//			
//			gl.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
//			gl.glShadeModel(GL.GL_FLAT);
//		}
//		
//		public void display(GLAutoDrawable drawable) {
//			GL gl = drawable.getGL();
//
//			gl.glClear(GL.GL_COLOR_BUFFER_BIT);
//		    gl.glColor3f(1.0f, 1.0f, 1.0f);
//		    gl.glLoadIdentity(); /* clear the matrix */
//		    
//		    /* viewing transformation */
//		    glu.gluLookAt(-2.5, 5.0, 8.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0);
//		    gl.glScalef(1.0f, 1.0f, 5.0f); /* modeling transformation */
//		    gl.glRotated(accx, 1d, 0d, 0d);
//		    gl.glRotated(accy, 0d, 1d, 0d);
//		    gl.glRotated(accz, 0d, 0d, 1d);
//		    glut.glutWireCube(1.0f);
//		    gl.glFlush();
//		}
//
//		public void displayChanged(GLAutoDrawable arg0, boolean arg1,
//				boolean arg2) {
//		}
//
//		public void reshape(GLAutoDrawable drawable, int x, int y, int w,
//				int h) {
//			GL gl = drawable.getGL();
//
//			gl.glMatrixMode(GL.GL_PROJECTION); /* prepare for and then */
//		    gl.glLoadIdentity(); /* define the projection */
//		    gl.glFrustum(-1.0, 1.0, -1.0, 1.0, 1.5, 20.0); /* transformation */
//		    gl.glMatrixMode(GL.GL_MODELVIEW); /* back to modelview matrix */
//		    gl.glViewport(0, 0, w, h); /* define the viewport */
//		}
//		
//	}
	
	protected class IRCompentent extends JPanel {
		
		public IRCompentent() {
			setMinimumSize(new Dimension(1024,768));
		}
		
		@Override
		protected void paintComponent(Graphics g) {
			super.paintComponent(g);
			
			Graphics2D g2 = (Graphics2D) g;
			Color origColor = g2.getColor();
			g2.setColor(Color.BLACK);

			AffineTransform trafo = AffineTransform.getScaleInstance(1024 / getWidth(), 768 / getHeight());
			Point2D dest = trafo.transform(new Point2D.Double(irx1, iry1), null);
			System.out.println("width: " + getWidth());
			System.out.println("height: " + getHeight());
			System.out.println("trafox " + irx1 + " -> " + dest.getX());
			System.out.println("trafoy " + iry1 + " -> " + dest.getY());
			g2.drawOval((int)dest.getX(), (int)dest.getY(), (size1 + 1) * 5, (size1 + 1) * 5);
			trafo.transform(new Point2D.Double(irx2, iry2), dest);
			g2.drawOval((int)dest.getX(), (int)dest.getY(), (size2 + 1) * 5, (size1 + 1) * 5);
			trafo.transform(new Point2D.Double(irx3, iry3), dest);
			g2.drawOval((int)dest.getX(), (int)dest.getY(), (size3 + 1) * 5, (size1 + 1) * 5);
			trafo.transform(new Point2D.Double(irx4, iry4), dest);
			g2.drawOval((int)dest.getX(), (int)dest.getY(), (size4 + 1) * 5, (size1 + 1) * 5);
			g2.setColor(origColor);
		}
		
	}
	
	private MoteGui.IRCompentent ircomp;

	private Mote mote;
	
//	private int accx, accy, accz;

	private boolean[] leds = new boolean[] { false, false, false, false };

	protected Action findAction = new AbstractAction("Find Motes") {

		public void actionPerformed(ActionEvent arg0) {
			MoteFinder finder = MoteFinder.getMoteFinder();
//			List<Wiimote> motes = finder.findNewDevices(30000l);
			mote = finder.findMote();
//			System.out.println("found " + motes.size() + " motes.");
//			if (motes.size() > 0) {
//				mote = motes.get(0);
			if (mote != null) {
				
				while (mote.getStatusInformationReport() == null) {
					System.out.println("waiting for status information report");
					try {
						Thread.sleep(10l);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				System.out.println(mote.getStatusInformationReport());
				
				while (mote.getCalibrationDataReport() == null) {
					System.out.println("waiting for calibration data report");
					try {
						Thread.sleep(10l);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				System.out.println(mote.getCalibrationDataReport());
				
//				mote.addAccelerometerListener(new AccelerometerListener() {
//				
//					public void accelerometerChanged(AccelerometerEvent evt) {
//						accx = evt.getX();
//						accy = evt.getY();
//						accz = evt.getZ();
////						glpanel.repaint();
//					}
//				
//				});
				
				mote.addIrCameraListener(new IrCameraListener() {
					
					public void irImageChanged(IrCameraEvent evt) {
						switch (evt.getSlot()) {
						case 0:
							irx1 = evt.getX();
							iry1 = evt.getY();
							size1 = evt.getSize();
							break;
						case 1:
							irx2 = evt.getX();
							iry2 = evt.getY();
							size2 = evt.getSize();
							break;
						case 2:
							irx3 = evt.getX();
							iry3 = evt.getY();
							size3 = evt.getSize();
							break;
						case 3:
							irx4 = evt.getX();
							iry4 = evt.getY();
							size4 = evt.getSize();
							break;
						default:
							System.out.println("unknown camera slot");
						}
						ircomp.repaint();
					}
				
				});
				
				mote.addCoreButtonListener(new CoreButtonListener() {

					public void buttonPressed(final CoreButtonEvent evt) {
						SwingUtilities.invokeLater(new Runnable() {

							public void run() {
								if (evt.isButtonAPressed()) {
									feedbackButtonA.setBackground(Color.BLUE);
								} else {
									feedbackButtonA.setBackground(origColor);
								}

								if (evt.isButtonBPressed())
									feedbackButtonB.setBackground(Color.BLUE);
								else
									feedbackButtonB.setBackground(origColor);
								
								if (evt.isButtonHomePressed())
									feedbackButtonHome.setBackground(Color.BLUE);
								else
									feedbackButtonHome.setBackground(origColor);
								
								if (evt.isButtonMinusPressed())
									feedbackButtonMinus.setBackground(Color.BLUE);
								else
									feedbackButtonMinus.setBackground(origColor);
								
								if (evt.isButtonOnePressed())
									feedbackButtonOne.setBackground(Color.BLUE);
								else
									feedbackButtonOne.setBackground(origColor);
								
								if (evt.isButtonPlusPressed())
									feedbackButtonPlus.setBackground(Color.BLUE);
								else
									feedbackButtonPlus.setBackground(origColor);
								
								if (evt.isButtonTwoPressed())
									feedbackButtonTwo.setBackground(Color.BLUE);
								else
									feedbackButtonTwo.setBackground(origColor);
								
								if (evt.isDPadDownPressed())
									feedbackButtonDPadDown.setBackground(Color.BLUE);
								else
									feedbackButtonDPadDown.setBackground(origColor);
								
								if (evt.isDPadLeftPressed())
									feedbackButtonDPadLeft.setBackground(Color.BLUE);
								else
									feedbackButtonDPadLeft.setBackground(origColor);
								
								if (evt.isDPadRightPressed())
									feedbackButtonDPadRight.setBackground(Color.BLUE);
								else
									feedbackButtonDPadRight.setBackground(origColor);
								
								if (evt.isDPadUpPressed())
									feedbackButtonDPadUp.setBackground(Color.BLUE);
								else
									feedbackButtonDPadUp.setBackground(origColor);
							}

						});
					}

				});
			}
		}

	};

	protected Action cameraExtendedAction = new AbstractAction("camera (extended)") {

		public void actionPerformed(ActionEvent arg0) {
			mote.enableIrCamera(IrCameraMode.EXTENDED, IrCameraSensitivity.MARCAN);
		}

	};
	
	protected Action report0x30Action = new AbstractAction("Report: 0x30") {
		
		public void actionPerformed(ActionEvent arg0) {
			mote.setReportMode(ReportModeRequest.DATA_REPORT_0x30);
		};
		
	};
	
	protected Action report0x31Action = new AbstractAction("Report: 0x31") {
		
		public void actionPerformed(ActionEvent arg0) {
			mote.setReportMode(ReportModeRequest.DATA_REPORT_0x31);
		};
		
	};
	
	protected Action report0x33Action = new AbstractAction("Report: 0x33") {
		
		public void actionPerformed(ActionEvent arg0) {
			mote.setReportMode(ReportModeRequest.DATA_REPORT_0x33);
		};
		
	};
	
	protected Action report0x36Action = new AbstractAction("Report: 0x36") {
		
		public void actionPerformed(ActionEvent arg0) {
			mote.setReportMode(ReportModeRequest.DATA_REPORT_0x36);
		};
		
	};

	protected Action cameraBasicAction = new AbstractAction("camera (basic)") {
	
		public void actionPerformed(ActionEvent arg0) {
			mote.enableIrCamera(IrCameraMode.BASIC, IrCameraSensitivity.MARCAN);
		}
	
	};
	
	protected Action setLed1Action = new AbstractAction("LED 1") {

		public void actionPerformed(ActionEvent arg0) {
			leds[0] = leds[0] ? false : true;
			mote.setPlayerLeds(leds);
		}

	};

	protected Action setLed2Action = new AbstractAction("LED 2") {

		public void actionPerformed(ActionEvent arg0) {
			leds[1] = leds[1] ? false : true;
			mote.setPlayerLeds(leds);
		}

	};

	protected Action setLed3Action = new AbstractAction("LED 3") {

		public void actionPerformed(ActionEvent arg0) {
			leds[2] = leds[2] ? false : true;
			mote.setPlayerLeds(leds);
		}

	};

	protected Action setLed4Action = new AbstractAction("LED 4") {

		public void actionPerformed(ActionEvent arg0) {
			leds[3] = leds[3] ? false : true;
			mote.setPlayerLeds(leds);
		}

	};

	protected Action rumbleAction = new AbstractAction("Rumble") {

		public void actionPerformed(ActionEvent arg0) {
			mote.rumble(1000l);
		}

	};

	private JButton feedbackButtonA;

	private JButton feedbackButtonB;

	private JButton feedbackButtonHome;

	private JButton feedbackButtonPlus;

	private JButton feedbackButtonMinus;

	private JButton feedbackButtonDPadLeft;

	private JButton feedbackButtonDPadRight;

	private JButton feedbackButtonDPadUp;

	private JButton feedbackButtonDPadDown;

	private JButton feedbackButtonOne;

	private JButton feedbackButtonTwo;

	private Color origColor;

//	private GLJPanel glpanel;
	
	public MoteGui() {
		final JFrame frame = new JFrame("MoteGui");
		frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		frame.getContentPane().setLayout(new BorderLayout());
		
		ircomp = new MoteGui.IRCompentent();
		ircomp.setBorder(BorderFactory.createLineBorder(Color.BLACK));
		ircomp.setPreferredSize(new Dimension(1024, 768));
		
//		glpanel = new GLJPanel();
//		glpanel.addGLEventListener(new MoteGui.AccelerometerComponent());
//		glpanel.setPreferredSize(new Dimension(600,600));

		JPanel centerPanel = new JPanel(new FlowLayout());
		centerPanel.add(ircomp);
//		centerPanel.add(glpanel);
		
		frame.getContentPane().add(centerPanel, BorderLayout.CENTER);
		
		JPanel topPanel = new JPanel(new FlowLayout());
		frame.getContentPane().add(topPanel, BorderLayout.NORTH);
		
		JPanel bottomPanel = new JPanel(new FlowLayout());
		frame.getContentPane().add(bottomPanel, BorderLayout.SOUTH);

		JButton findButton = new JButton("Find motes");
		findButton.setAction(findAction);
		topPanel.add(findButton);

		JButton cachedButton = new JButton(cameraExtendedAction);
		topPanel.add(cachedButton);

		JButton led1Button = new JButton("LED 1");
		led1Button.setAction(setLed1Action);
		topPanel.add(led1Button);

		JButton led2Button = new JButton("LED 2");
		led2Button.setAction(setLed2Action);
		topPanel.add(led2Button);

		JButton led3Button = new JButton("LED 3");
		led3Button.setAction(setLed3Action);
		topPanel.add(led3Button);

		JButton led4Button = new JButton("LED 4");
		led4Button.setAction(setLed4Action);
		topPanel.add(led4Button);

		JButton rumbleButton = new JButton(rumbleAction);
		topPanel.add(rumbleButton);
		
		JButton cameraButton = new JButton(cameraBasicAction);
		topPanel.add(cameraButton);
		
		JButton report0x30 = new JButton(report0x30Action);
		report0x30.setToolTipText("core buttons");
		topPanel.add(report0x30);

		JButton report0x31 = new JButton(report0x31Action);
		report0x31.setToolTipText("core buttons and accelerometer");
		topPanel.add(report0x31);
		
		JButton report0x33 = new JButton(report0x33Action);
		report0x33.setToolTipText("core buttons, accelerometer and IR (extended)");
		topPanel.add(report0x33);
		
		JButton report0x36 = new JButton(report0x36Action);
		report0x36.setToolTipText("core buttons, ir (basic) and extension");
		topPanel.add(report0x36);

		feedbackButtonA = new JButton("A");
		feedbackButtonA.setEnabled(false);
		bottomPanel.add(feedbackButtonA);

		feedbackButtonB = new JButton("B");
		feedbackButtonB.setEnabled(false);
		bottomPanel.add(feedbackButtonB);

		feedbackButtonOne = new JButton("One");
		feedbackButtonOne.setEnabled(false);
		bottomPanel.add(feedbackButtonOne);

		feedbackButtonTwo = new JButton("Two");
		feedbackButtonTwo.setEnabled(false);
		bottomPanel.add(feedbackButtonTwo);

		feedbackButtonHome = new JButton("Home");
		feedbackButtonHome.setEnabled(false);
		bottomPanel.add(feedbackButtonHome);

		feedbackButtonDPadLeft = new JButton("DPadLeft");
		feedbackButtonDPadLeft.setEnabled(false);
		bottomPanel.add(feedbackButtonDPadLeft);

		feedbackButtonDPadRight = new JButton("DPadRight");
		feedbackButtonDPadRight.setEnabled(false);
		bottomPanel.add(feedbackButtonDPadRight);

		feedbackButtonDPadUp = new JButton("DPadUp");
		feedbackButtonDPadUp.setEnabled(false);
		bottomPanel.add(feedbackButtonDPadUp);

		feedbackButtonDPadDown = new JButton("DPadDown");
		feedbackButtonDPadDown.setEnabled(false);
		bottomPanel.add(feedbackButtonDPadDown);

		feedbackButtonPlus = new JButton("Plus");
		feedbackButtonPlus.setEnabled(false);
		bottomPanel.add(feedbackButtonPlus);

		feedbackButtonMinus = new JButton("Minus");
		feedbackButtonMinus.setEnabled(false);
		bottomPanel.add(feedbackButtonMinus);
		
		frame.addWindowStateListener(new WindowStateListener() {
		
			public void windowStateChanged(WindowEvent arg0) {
				if (arg0.getNewState() == WindowEvent.WINDOW_CLOSED) {
					if (mote != null) {
						mote.disconnect();
					}
					frame.dispose();
				}
			}
		
		});
		
		frame.setVisible(true);
		frame.pack();

		origColor = feedbackButtonA.getBackground();
	}

	public static void main(String[] args) {
		SwingUtilities.invokeLater(new Runnable() {

			public void run() {
				new MoteGui();
			}

		});
	}
}
