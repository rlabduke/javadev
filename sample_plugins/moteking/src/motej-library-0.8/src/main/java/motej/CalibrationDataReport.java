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

/**
 * Calibration data for the onboard accelerometer (as stored in the Wiimote's
 * memory, starting at address 0x16 and repeated at 0x20).
 * <p>
 * 
 * @author <a href="mailto:vfritzsch@users.sourceforge.net">Volker Fritzsch</a>
 */
public class CalibrationDataReport {

	private byte zeroX;

	private byte zeroY;

	private byte zeroZ;

	private byte gravityX;

	private byte gravityY;

	private byte gravityZ;

	public CalibrationDataReport(byte zeroX, byte zeroY, byte zeroZ,
			byte gravityX, byte gravityY, byte gravityZ) {
		this.zeroX = zeroX;
		this.zeroY = zeroY;
		this.zeroZ = zeroZ;
		this.gravityX = gravityX;
		this.gravityY = gravityY;
		this.gravityZ = gravityZ;
	}

	/**
	 * Calibrated force of gravity for the accelerometers X axis.
	 * 
	 * @return the force of gravity X axis
	 */
	public byte getGravityX() {
		return gravityX;
	}

	/**
	 * Calibrated force of gravity for the accelerometers Y axis.
	 * 
	 * @return the force of gravity Y axis
	 */
	public byte getGravityY() {
		return gravityY;
	}

	/**
	 * Calibrated force of gravity for the accelerometers Z axis.
	 * 
	 * @return the force of gravity Z axis
	 */
	public byte getGravityZ() {
		return gravityZ;
	}

	/**
	 * Calibrated zero offsets for the accelerometers X axis.
	 * 
	 * @return zero offset X axis
	 */
	public byte getZeroX() {
		return zeroX;
	}

	/**
	 * Calibrated zero offsets for the accelerometers Y axis.
	 * 
	 * @return zero offset Y axis
	 */
	public byte getZeroY() {
		return zeroY;
	}

	/**
	 * Calibrated zero offsets for the accelerometers Z axis.
	 * 
	 * @return zero offset Z axis
	 */
	public byte getZeroZ() {
		return zeroZ;
	}

	@Override
	public String toString() {
		return "CalibrationDataReport[zeroPointAxisX: " + zeroX
				+ ", zeroPointAxisY: " + zeroY + ", zeroPointAxisZ: " + zeroZ
				+ ", plusOneGPointAxisX: " + gravityX
				+ ", plusOneGPointAxisY: " + gravityY
				+ ", plusOneGPointAxisZ: " + gravityZ + "]";
	}
}
