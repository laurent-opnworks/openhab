/**
 * openHAB, the open Home Automation Bus.
 * Copyright (C) 2010-2013, openHAB.org <admin@openhab.org>
 *
 * See the contributors.txt file in the distribution for a
 * full listing of individual contributors.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation; either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 * Additional permission under GNU GPL version 3 section 7
 *
 * If you modify this Program, or any covered work, by linking or
 * combining it with Eclipse (or a modified version of that library),
 * containing parts covered by the terms of the Eclipse Public License
 * (EPL), the licensors of this Program grant you additional permission
 * to convey the resulting work.
 */
package org.openhab.binding.isyinst;

import org.openhab.core.types.State;
import org.openhab.core.types.Type;

import com.universaldevices.device.model.UDControl;
import com.universaldevices.device.model.UDNode;

public interface IsyTypeMapper {

	/**
	 * maps an openHAB command/state to a string value which correspond to its node in ISY
	 *  
	 * @param type a command or state
	 * @param dpt the corresponding node type
	 * @return node value as a string
	 */
	public String toNodeValue(Type type, String nodeType);

	/**
	 * maps a datapoint value to an openHAB command or state
	 * 
	 * @param datapoint the source node 
	 * @param data the node value 
	 * @return a command or state of openHAB
	 */
	public Type toType(UDNode node, String data);

	public State toState(UDControl control, Object value, UDNode node);
}
