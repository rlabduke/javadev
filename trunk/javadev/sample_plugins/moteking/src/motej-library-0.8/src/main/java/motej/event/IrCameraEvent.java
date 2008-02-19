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
package motej.event;

import motej.IrCameraMode;
import motej.Mote;

/**
 * 
 * <p>
 * @author <a href="mailto:vfritzsch@users.sourceforge.net">Volker Fritzsch</a>
 */
public class IrCameraEvent {

	private int x;
	
	private int y;
	
	private int size;
	
	private Mote source;
	
	private IrCameraMode mode;
	
	private int slot;
	
	public IrCameraEvent(Mote source, IrCameraMode mode, int slot, int x, int y, int size) {
		this.source = source;
		this.slot = slot;
		this.mode = mode;
		this.x = x;
		this.y = y;
		this.size = size;
	}

	public int getSlot() {
		return slot;
	}

	public int getX() {
		return x;
	}

	public int getY() {
		return y;
	}

	public int getSize() {
		return size;
	}

	public Mote getSource() {
		return source;
	}

	public IrCameraMode getMode() {
		return mode;
	}
	
}
