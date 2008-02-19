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

import java.io.IOException;

import javax.bluetooth.L2CAPConnection;
import javax.microedition.io.Connector;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import motej.request.ReportModeRequest;

/**
 * 
 * <p>
 * @author <a href="mailto:vfritzsch@users.sourceforge.net">Volker Fritzsch</a>
 */
class IncomingThread extends Thread {
	
	private static final long THREAD_SLEEP = 1l;

	private Log log = LogFactory.getLog(IncomingThread.class);

	private Mote source;

	private volatile boolean active;

	private L2CAPConnection incoming;

	protected IncomingThread(Mote source, String btaddress)
			throws IOException, InterruptedException {
		super("IncomingWiimoteThread:" + btaddress);
		this.source = source;
		incoming = (L2CAPConnection) Connector.open("btl2cap://" + btaddress
				+ ":13;authenticate=false;encrypt=false;master=false",
				Connector.READ);

		Thread.sleep(THREAD_SLEEP);
		active = true;
	}

	public void disconnect() {
		active = false;
	}

	protected void parseAccelerometerData(byte[] bytes) {
		int x = bytes[4] & 0xff;
		int y = bytes[5] & 0xff;
		int z = bytes[6] & 0xff;
		source.fireAccelerometerEvent(x, y, z);
	}

	protected void parseBasicIrCameraData(byte[] bytes, int offset) {
		int x0 = bytes[offset] & 0xff ^ (bytes[offset + 2] & 0x30) << 4;
		int y0 = bytes[offset + 1] & 0xff ^ (bytes[offset + 2] & 0xc0) << 2;
		if (x0 < 1023 && y0 < 1023) {
			source.fireIrCameraEvent(IrCameraMode.BASIC, 0, x0, y0, 0);
		}

		int x1 = bytes[offset + 3] & 0xff ^ (bytes[offset + 2] & 0x03) << 8;
		int y1 = bytes[offset + 4] & 0xff ^ (bytes[offset + 2] & 0x0c) << 6;
		if (x1 < 1023 && y1 < 1023) {
			source.fireIrCameraEvent(IrCameraMode.BASIC, 1, x1, y1, 0);
		}

		int x2 = bytes[offset + 5] & 0xff ^ (bytes[offset + 7] & 0x30) << 4;
		int y2 = bytes[offset + 6] & 0xff ^ (bytes[offset + 7] & 0xc0) << 2;
		if (x2 < 1023 && y2 < 1023) {
			source.fireIrCameraEvent(IrCameraMode.BASIC, 2, x2, y2, 0);
		}

		int x3 = bytes[offset + 8] & 0xff ^ (bytes[offset + 7] & 0x03) << 8;
		int y3 = bytes[offset + 9] & 0xff ^ (bytes[offset + 7] & 0x0c) << 6;
		if (x3 < 1023 && y3 < 1023) {
			source.fireIrCameraEvent(IrCameraMode.BASIC, 3, x3, y3, 0);
		}
	}

	protected void parseCoreButtonData(byte[] bytes) {
		int modifiers = bytes[2] & 0xff ^ (bytes[3] & 0xff) << 8;
		source.fireCoreButtonEvent(modifiers);
	}

	protected void parseExtendedIrCameraData(byte[] bytes, int offset) {
		int x0 = bytes[7] & 0xff ^ (bytes[9] & 0x30) << 4;
		int y0 = bytes[8] & 0xff ^ (bytes[9] & 0xc0) << 2;
		int size0 = bytes[9] & 0x0f;
		if (x0 < 1023 && y0 < 1023) {
			source.fireIrCameraEvent(IrCameraMode.EXTENDED, 0, x0, y0, size0);
		}

		int x1 = bytes[10] & 0xff ^ (bytes[12] & 0x30) << 4;
		int y1 = bytes[11] & 0xff ^ (bytes[12] & 0xc0) << 2;
		int size1 = bytes[12] & 0x0f;
		if (x1 < 1023 && y1 < 1023) {
			source.fireIrCameraEvent(IrCameraMode.EXTENDED, 1, x1, y1, size1);
		}

		int x2 = bytes[13] & 0xff ^ (bytes[15] & 0x30) << 4;
		int y2 = bytes[14] & 0xff ^ (bytes[15] & 0xc0) << 2;
		int size2 = bytes[15] & 0x0f;
		if (x2 < 1023 && y2 < 1023) {
			source.fireIrCameraEvent(IrCameraMode.EXTENDED, 2, x2, y2, size2);
		}

		int x3 = bytes[16] & 0xff ^ (bytes[18] & 0x30) << 4;
		int y3 = bytes[17] & 0xff ^ (bytes[18] & 0xc0) << 2;
		int size3 = bytes[18] & 0x0f;
		if (x3 < 1023 && y3 < 1023) {
			source.fireIrCameraEvent(IrCameraMode.EXTENDED, 3, x3, y3, size3);
		}
	}

	protected void parseExtensionData(byte[] bytes, int offset, int length) {
		log.error("extension data handling not implemented yet.");
	}

	protected void parseFullIrCameraData(byte[] bytes, int offset) {
		throw new RuntimeException("full camera mode handling not implemented");
	}

	protected void parseInterleavedAccelerometerData(byte[] bytes, int offset) {
		throw new RuntimeException("interleaved accelerometer data handling not implemented");
	}

	protected void parseInterleavedCoreButtonData(byte[] bytes) {
		throw new RuntimeException("interleaved core button data handling not implemented");
	}

	protected void parseMemoryData(byte[] bytes) {
		int size = (bytes[4] >> 4) & 0x0f;
		int error = bytes[4] & 0x0f;
		byte[] address = new byte[] { bytes[5], bytes[6] };
		byte[] payload = new byte[size];

		System.arraycopy(bytes, 7, payload, 0, size);

		source.fireReadDataEvent(address, payload, error);
	}

	protected void parseStatusInformation(byte[] bytes) {
		boolean[] leds = new boolean[] { (bytes[4] & 0x10) == 0x10,
				(bytes[4] & 0x20) == 0x20, (bytes[4] & 0x40) == 0x40,
				(bytes[4] & 0x80) == 0x80 };
		boolean extensionControllerConnected = (bytes[4] & 0x02) == 0x02;
		boolean speakerEnabled = (bytes[4] & 0x04) == 0x04;
		boolean continuousReportingEnabled = (bytes[4] & 0x08) == 0x08;
		byte batteryLevel = bytes[7];
		
		StatusInformationReport info = new StatusInformationReport(leds, speakerEnabled, continuousReportingEnabled, extensionControllerConnected, batteryLevel);
		source.fireStatusInformationChangedEvent(info);
	}

	public void run() {
		while (active) {
			try {
				byte[] buf = new byte[23];
				incoming.receive(buf);

//				 System.out.println("received: ");
//				 for (int i = 0; i < buf.length; i++) {
//				 System.out.print((int) buf[i] + " ");
//				 }
//				 System.out.println();
//								
//				 System.out.println("as bitset: ");
//				 for (int i = 0; i < buf.length; i++) {
//				 for (int j = 7; j >= 0; j--) {
//				 System.out.print((buf[i] & (1 << j)) == (1 << j) ? "1" :
//				 "0");
//				 }
//				 System.out.print(" ");
//				 }
//				 System.out.println();

				switch (buf[1]) {
				case ReportModeRequest.DATA_REPORT_0x20:
					parseStatusInformation(buf);
					break;

				case ReportModeRequest.DATA_REPORT_0x21:
					parseCoreButtonData(buf);
					parseMemoryData(buf);
					break;

				case ReportModeRequest.DATA_REPORT_0x30:
					parseCoreButtonData(buf);
					break;

				case ReportModeRequest.DATA_REPORT_0x31:
					parseCoreButtonData(buf);
					parseAccelerometerData(buf);
					break;

				case ReportModeRequest.DATA_REPORT_0x32:
					parseCoreButtonData(buf);
					parseExtensionData(buf, 3, 8);
					break;

				case ReportModeRequest.DATA_REPORT_0x33:
					parseCoreButtonData(buf);
					parseAccelerometerData(buf);
					parseExtendedIrCameraData(buf, 7);
					break;

				case ReportModeRequest.DATA_REPORT_0x34:
					parseCoreButtonData(buf);
					parseExtensionData(buf, 3, 19);
					break;

				case ReportModeRequest.DATA_REPORT_0x35:
					parseCoreButtonData(buf);
					parseAccelerometerData(buf);
					parseExtensionData(buf, 7, 16);
					break;

				case ReportModeRequest.DATA_REPORT_0x36:
					parseCoreButtonData(buf);
					parseBasicIrCameraData(buf, 4);
					parseExtensionData(buf, 14, 9);
					break;

				case ReportModeRequest.DATA_REPORT_0x37:
					parseCoreButtonData(buf);
					parseAccelerometerData(buf);
					parseBasicIrCameraData(buf, 7);
					parseExtensionData(buf, 17, 6);
					break;

				case ReportModeRequest.DATA_REPORT_0x3d:
					parseExtensionData(buf, 2, 21);
					break;

				case ReportModeRequest.DATA_REPORT_0x3e:
					log.error("interleaved reporting not implemented yet.");
					break;

				case ReportModeRequest.DATA_REPORT_0x3f:
					log.error("interleaved reporting not implemented yet.");
					break;

				default:
					log.info("unknown or not yet implemented data report: " + buf[1]);
				}

				Thread.sleep(THREAD_SLEEP);
			} catch (InterruptedException ex) {
				log.error("incoming wiimote thread interrupted.", ex);
			} catch (IOException ex) {
				log.error("connection closed?", ex);
				active = false;
			}
		}
		try {
			incoming.close();
		} catch (IOException ex) {
			log.error(ex);
		}
	}
}
