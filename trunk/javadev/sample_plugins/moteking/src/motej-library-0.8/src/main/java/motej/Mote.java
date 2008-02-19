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
package motej;

import javax.bluetooth.RemoteDevice;
import javax.swing.event.EventListenerList;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import motej.event.AccelerometerEvent;
import motej.event.AccelerometerListener;
import motej.event.CoreButtonEvent;
import motej.event.CoreButtonListener;
import motej.event.DataEvent;
import motej.event.DataListener;
import motej.event.IrCameraEvent;
import motej.event.IrCameraListener;
import motej.event.StatusInformationListener;
import motej.request.CalibrationDataRequest;
import motej.request.PlayerLedRequest;
import motej.request.RawByteRequest;
import motej.request.ReportModeRequest;
import motej.request.RumbleRequest;
import motej.request.StatusInformationRequest;
import motej.request.WriteRegisterRequest;

/**
 * 
 * <p>
 * @author <a href="mailto:vfritzsch@users.sourceforge.net">Volker Fritzsch</a>
 */
public class Mote {
	
	private Log log = LogFactory.getLog(Mote.class);

	private OutgoingThread outgoing;

	private IncomingThread incoming;

	private StatusInformationReport statusInformationReport;

	private CalibrationDataReport calibrationDataReport;

	private EventListenerList listenerList = new EventListenerList();

	private String bluetoothAddress;

	public Mote(RemoteDevice device) {
		try {
			bluetoothAddress = device.getBluetoothAddress();
			
			outgoing = new OutgoingThread(device.getBluetoothAddress());
			incoming = new IncomingThread(this, device.getBluetoothAddress());
			
			incoming.start();
			outgoing.start();
			
			outgoing.sendRequest(new StatusInformationRequest());
			outgoing.sendRequest(new CalibrationDataRequest());
		} catch (Exception ex) {
			throw new RuntimeException(ex.fillInStackTrace());
		}
	}

	public void addAccelerometerListener(AccelerometerListener listener) {
		listenerList.add(AccelerometerListener.class, listener);
	}

	public void addCoreButtonListener(CoreButtonListener listener) {
		listenerList.add(CoreButtonListener.class, listener);
	}

	public void addDataListener(DataListener listener) {
		listenerList.add(DataListener.class, listener);
	}

	public void addIrCameraListener(IrCameraListener listener) {
		listenerList.add(IrCameraListener.class, listener);
	}

	public void addStatusInformationListener(StatusInformationListener listener) {
		listenerList.add(StatusInformationListener.class, listener);
	}

	public void disableIrCamera() {
		// 1. Disable IR Camera
		outgoing.sendRequest(new RawByteRequest(new byte[] { 82, 19, 0 }));
		
		// 2. Disable IR Camera 2
		outgoing.sendRequest(new RawByteRequest(new byte[] { 82, 26, 0 }));
	}

	public void disconnect() {
		log.info("Disconnecting Mote " + bluetoothAddress);
		if (outgoing != null) {
			outgoing.disconnect();
			try {
				outgoing.join(5000l);
			} catch (InterruptedException e) {
				log.error(e);
			}
		}
		if (incoming != null) {
			incoming.disconnect();
			try {
				incoming.join(5000l);
			} catch (InterruptedException e) {
				log.error(e);
			}
		}
	}

	public void enableIrCamera() {
		enableIrCamera(IrCameraMode.BASIC, IrCameraSensitivity.MARCAN);
	}

	public void enableIrCamera(IrCameraMode mode, IrCameraSensitivity sensitivity) {
		// 1. Enable IR Camera (Send 0x04 to Output Report 0x13)
		outgoing.sendRequest(new RawByteRequest(new byte[] { 82, 19, 4 }));

		// 2. Enable IR Camera 2 (Send 0x04 to Output Report 0x1a)
		outgoing.sendRequest(new RawByteRequest(new byte[] { 82, 26, 4 }));

		// 3. Write 0x08 to register 0xb00030
		outgoing.sendRequest(new WriteRegisterRequest(new byte[] { (byte) 0xb0,
				0x00, 0x30 }, new byte[] { 0x08 }));

		// 4. Write Sensitivity Block 1 to registers at 0xb00000
		outgoing.sendRequest(new WriteRegisterRequest(new byte[] { (byte) 0xb0,
				0x00, 0x00 }, sensitivity.block1()));

		// 5. Write Sensitivity Block 2 to registers at 0xb0001a
		outgoing.sendRequest(new WriteRegisterRequest(new byte[] { (byte) 0xb0,
				0x00, 0x1a }, sensitivity.block2()));

		// 6. Write Mode Number to register 0xb00033
		outgoing.sendRequest(new WriteRegisterRequest(new byte[] { (byte) 0xb0,
				0x00, 0x33 }, new byte[] { mode.modeAsByte() }));
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof Mote))
			return false;

		return hashCode() == obj.hashCode();
	}

	protected void fireAccelerometerEvent(int x, int y, int z) {
		AccelerometerListener[] listeners = listenerList.getListeners(AccelerometerListener.class);
		AccelerometerEvent evt = new AccelerometerEvent(this, x, y, z);
		for (AccelerometerListener l : listeners) {
			l.accelerometerChanged(evt);
		}
	}

	protected void fireCoreButtonEvent(int modifiers) {
		CoreButtonListener[] listeners = listenerList.getListeners(CoreButtonListener.class);
		CoreButtonEvent evt = new CoreButtonEvent(this, modifiers);
		for (CoreButtonListener l : listeners) {
			l.buttonPressed(evt);
		}
	}

	protected void fireIrCameraEvent(IrCameraMode mode, int camera, int x, int y, int size) {
		IrCameraListener[] listeners = listenerList.getListeners(IrCameraListener.class);
		IrCameraEvent evt = new IrCameraEvent(this, mode, camera, x, y, size);
		for (IrCameraListener l : listeners) {
			l.irImageChanged(evt);
		}
	}

	protected void fireReadDataEvent(byte[] address, byte[] payload, int error) {
		if (calibrationDataReport == null && error == 0 && address[0] == 0x00 && address[1] == 0x20) {
			// calibration data (most probably), if thats the first time, it's probably for us - so we'll consume this event.
			CalibrationDataReport report = new CalibrationDataReport(payload[0], payload[1], payload[2],
					payload[4], payload[5], payload[6]);
			calibrationDataReport = report;
		} else {
			DataListener[] listeners = listenerList.getListeners(DataListener.class);
			DataEvent evt = new DataEvent(address, payload, error);
			for (DataListener l : listeners) {
				l.dataRead(evt);
			}
		}
	}

	protected void fireStatusInformationChangedEvent(StatusInformationReport report) {
		this.statusInformationReport = report;
		StatusInformationListener[] listeners = listenerList.getListeners(StatusInformationListener.class);
		for (StatusInformationListener l : listeners) {
			l.statusInformationReceived(report);
		}
	}

	public String getBluetoothAddress() {
		return bluetoothAddress;
	}

	public CalibrationDataReport getCalibrationDataReport() {
		return calibrationDataReport;
	}

	public StatusInformationReport getStatusInformationReport() {
		return statusInformationReport;
	}

	@Override
	public int hashCode() {
		return bluetoothAddress.hashCode();
	}

	public void removeAccelerometerListener(AccelerometerListener listener) {
		listenerList.remove(AccelerometerListener.class, listener);
	}

	public void removeCoreButtonListener(CoreButtonListener listener) {
		listenerList.remove(CoreButtonListener.class, listener);
	}

	public void removeDataListener(DataListener listener) {
		listenerList.remove(DataListener.class, listener);
	}

	public void removeIrCameraListener(IrCameraListener listener) {
		listenerList.remove(IrCameraListener.class, listener);
	}

	public void removeStatusInformationListener(StatusInformationListener listener) {
		listenerList.remove(StatusInformationListener.class, listener);
	}

	public void requestStatusInformation() {
		outgoing.sendRequest(new StatusInformationRequest());
	}

	public void rumble(long millis) {
		outgoing.sendRequest(new RumbleRequest(millis));
	}

	public void setPlayerLeds(boolean[] leds) {
		outgoing.sendRequest(new PlayerLedRequest(leds));
	}

	public void setReportMode(byte mode) {
		outgoing.sendRequest(new ReportModeRequest(mode));
	}

	public void setReportMode(byte mode, boolean continuous) {
		outgoing.sendRequest(new ReportModeRequest(mode, continuous));
	}
}
