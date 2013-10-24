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
package org.openhab.binding.isyinst.internal.client;

import org.apache.commons.lang.StringUtils;
import org.openhab.binding.isyinst.IsyTypeMapper;
import org.openhab.binding.isyinst.internal.IsyCoreTypeMapper;
import org.openhab.core.events.EventPublisher;
import org.openhab.core.types.State;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nanoxml.XMLElement;
import com.udi.isy.jsdk.insteon.ISYInsteonClient;
import com.universaldevices.client.NoDeviceException;
import com.universaldevices.common.Constants;
import com.universaldevices.common.properties.UDProperty;
import com.universaldevices.device.model.IModelChangeListener;
import com.universaldevices.device.model.UDControl;
import com.universaldevices.device.model.UDFolder;
import com.universaldevices.device.model.UDGroup;
import com.universaldevices.device.model.UDNode;
import com.universaldevices.security.upnp.UPnPSecurity;
import com.universaldevices.upnp.UDProxyDevice;

/**
 * 
 * This class implements a very simple ISY client which prints out events as
 * they occur in ISY
 * 
 * @author UD Architect
 * 
 */
public class OpenhabISYInsteonClient extends ISYInsteonClient {

	private static final Logger logger = LoggerFactory
			.getLogger(OpenhabISYInsteonClient.class);

	private EventPublisher eventPublisher;

	private IsyTypeMapper typeMapper = new IsyCoreTypeMapper();

	/**
	 * Constructor Registers this class as IModelChangeListener
	 * 
	 * @param eventPublisher
	 * 
	 * @see IModelChangeListener
	 * 
	 */
	public OpenhabISYInsteonClient(EventPublisher eventPublisher) {
		super();
		this.eventPublisher = eventPublisher;
	}

	/**
	 * This method is called when a new ISY is announced or discovered on the
	 * network. For this sample, we simply authenticate ourselves
	 */
	public void onNewDeviceAnnounced(UDProxyDevice device) {
		logger.info("NEW DEVICE: " + device.getFriendlyName());
	}

	/**
	 * This method is invoked when ISY goes into Linking mode
	 */
	public void onDiscoveringNodes() {
		logger.info("I am in Linking Mode ...");
	}

	/**
	 * This method is invoked when ISY is no longer in Linking mode
	 */
	public void onNodeDiscoveryStopped() {
		logger.info("I am no longer in Linking mode ...");

	}

	/**
	 * This method is invoked when a group/scene is removed
	 */
	public void onGroupRemoved(String groupAddress) {
		logger.info("Scene: " + groupAddress
				+ " was removed by someone or something!");

	}

	/**
	 * This method is invoked when a group/scene is renamed
	 */
	public void onGroupRenamed(UDGroup group) {
		logger.info("Scene: " + group.address + " was renamed to " + group.name);

	}

	/**
	 * This method is invoked everytime there's a change in the state of a
	 * control for a node (Insteon Device)
	 */
	public void onModelChanged(UDControl control, Object value, UDNode node) {
		if (control == null || value == null || node == null)
			return;
		String itemName = normalizeItemName(node.name);
		
		String controlName = control.label == null ? control.name
				: control.label;
		logger.info("Someone or something changed " + controlName + " to " + value
				+ " at " + itemName);

		// Someone or something changed Status to 142 at Dessous comptoir

		State state = getState(control, value, node);
		
//		23:14:38.344 INFO  o.o.b.i.i.c.OpenhabISYInsteonClient[:111] - Someone or something changed On Level to 140 at SdB Prinicipale
//		23:14:38.345 INFO  o.o.b.i.i.c.OpenhabISYInsteonClient[:118] - postUpdate to  SdB Prinicipale,  state: 140
//		java.lang.IllegalArgumentException: invalid topic: openhab/update/SdB Prinicipale		
		logger.info("postUpdate to  " + itemName + ",  state: " + state);
		eventPublisher.postUpdate(itemName, state);
		
	}

	private String normalizeItemName(String name) {
		return StringUtils.replace(StringUtils.replace(name, " ", "_"), "-", "_");
	}

	/**
	 * Transforms the ISY bus data of a given datapoint into an openHAB type (command or state)
	 * 
	 * @return the openHAB command or state that corresponds to the data
	 */
	private State getState(UDControl control, Object value, UDNode node) {

			State state = typeMapper.toState(control, value, node);
			return state;
	}	
	
	/**
	 * This method is invoked when the network is renamed. Network is the top
	 * most node in the tree in our applet
	 */
	public void onNetworkRenamed(String newName) {
		logger.info("Ah, the network was renamed to " + newName);
	}

	/**
	 * This method is called when a new group/scene has been created
	 */
	public void onNewGroup(UDGroup newGroup) {
		logger.info("Yummy: we now have a new scene with address "
				+ newGroup.address + " and name " + newGroup.name);
	}

	/**
	 * This method is called when a new node (Insteon Device) has been added
	 */
	public void onNewNode(UDNode newNode) {
		logger.info("Yummy: we now have a new Insteon device with address "
				+ newNode.address + " and name " + newNode.name);

	}

	/**
	 * This method is called when an Insteon Device does not correctly
	 * communicate with ISY
	 */
	public void onNodeError(UDNode node) {
		logger.info("What's going on? The Insteon device at address "
				+ node.address + " and name " + node.name
				+ " is no longer responding to my communication attempts!");

	}

	/**
	 * This method is called with a node is enabled or disabled
	 * 
	 * @param node
	 * @param b
	 */
	public void onNodeEnabled(UDNode node, boolean b) {
		logger.info(String.format("Node %s is now %s", node.name, b ? "enabled"
				: "disabled"));
	}

	/**
	 * This method is called when a node (Insteon Device) has been permanently
	 * removed from ISY
	 */
	public void onNodeRemoved(String nodeAddress) {
		logger.info("Whooah ... node with address " + nodeAddress
				+ " was permanently removed from ISY");

	}

	/**
	 * This method is called when a node (Insteon Device) is removed from a
	 * scene
	 */
	public void onNodeRemovedFromGroup(UDNode node, UDGroup group) {
		logger.info("Insteon device with address " + node.address
				+ " and name " + node.name + " is no longer part of the "
				+ group.name + " scene!");

	}

	/**
	 * This method is called when a node's role changes in the given group
	 * (master/slave role)
	 */
	public void onNodeToGroupRoleChanged(UDNode node, UDGroup group,
			char new_role) {
		logger.info("Insteon device with address " + node.address
				+ " now has a new role in group with address " + group.address
				+ " : ");
		if (new_role == Constants.UD_LINK_MODE_MASTER)
			logger.info("Controller/Master");
		else
			logger.info("Responder/Slave");
	}

	/**
	 * This method is invoked when a node (Insteon Device) is renamed
	 */
	public void onNodeRenamed(UDNode node) {
		logger.info("Insteon device with address " + node.address
				+ " was renamed to " + node.name);

	}

	/**
	 * This method is invoked when a node (Insteon Device) has been moved to a
	 * scene as controller/master
	 */
	public void onNodeMovedAsMaster(UDNode node, UDGroup group) {
		logger.info("Insteon device " + node.name + " is now part of the "
				+ group.name + " scene as a master/controller");

	}

	/**
	 * This method is invoked when a node (Insteon Device) has been moved to a
	 * scene as responder/slave
	 */
	public void onNodeMovedAsSlave(UDNode node, UDGroup group) {
		logger.info("Insteon device " + node.name + " is now part of the "
				+ group.name + " scene as a slave/responder");

	}

	/**
	 * This method is invoked with the library does not receive announcements
	 * from ISY and considers it offline
	 */
	public void onDeviceOffLine() {
		logger.info("oo; ISY is offLine. Did you unplug it?");

	}

	/**
	 * This method is invoked when a currently known ISY (UDProxyDevice) is back
	 * on line
	 */
	public void onDeviceOnLine() {
		logger.info("Hooray: ISY is on line ...");
		final UDProxyDevice device = getDevice();
		if (device == null)
			return;
		if (device.isSecurityEnabled()
				|| device.securityLevel > UPnPSecurity.NO_SECURITY) {
			if (device.isAuthenticated && device.isOnline)
				return;
			try {
				logger.info("AUTHENICATING/SUBSCRIBING");
				if (!authenticate("admin", "admin")) {
					logger.info("AUTHENICATION FAILED");
				} else {
					logger.info("AUTHENICATION SUCCEEDED");
				}

			} catch (NoDeviceException e) {
				System.err.println("This should never happen!");
			}
		} else {
			// just subscribe to events
			logger.info("SUBSCRIBING");
			device.subscribeToEvents(true);
			logger.info("SUBSCRIPTION DONE");
		}
	}

	/**
	 * This method is invoked when the state of the system (whether or not busy)
	 * is changed
	 * 
	 * @param busy
	 *            - whether or not ISY is busy
	 */
	public void onSystemStatus(boolean busy) {
		if (busy)
			logger.info("I am busy now; please give me some reprieve and don't ask me for more!");
		else
			logger.info("I am ready and at your service");
	}

	/**
	 * This method is invoked when internet access is disabled on ISY
	 */
	public void onInternetAccessDisabled() {
		logger.info("You can no longer reach me through the internet");

	}

	/**
	 * This method is invoked with internet access is enabled on ISY
	 * 
	 * @param url
	 *            - the external fully qualified url through which ISY can be
	 *            accessed
	 */
	public void onInternetAccessEnabled(String url) {
		logger.info("You can now reach me remotely at: " + url);

	}

	/**
	 * This method is invoked when trigger status changes
	 * 
	 * @param arg1
	 *            - the status
	 * @param arg2
	 *            - extra information
	 */
	public void onTriggerStatus(String arg1, XMLElement arg2) {
		logger.info("Trigger status changed: " + arg1);

	}

	public void onDeviceSpecific(String arg1, String node, XMLElement arg2) {
		logger.info("Device Specific action: ");
		logger.info(arg2.toString());

	}

	public void onProgress(String arg1, XMLElement arg2) {
		logger.info("Progress Report:");
		logger.info(arg2.toString());
	}

	/**
	 * Implement any cleanup Routines necessary here
	 */
	@Override
	public void cleanUp() {
		logger.info("Clean up whatever other static objects you have around");

	}

	@Override
	public void onSystemConfigChanged(String event, XMLElement eventInfo) {
		logger.info("System configuration changed");

	}

	@Override
	public void onFolderRemoved(String folderAddress) {
		logger.info(String.format("Folder removed %s", folderAddress));

	}

	@Override
	public void onFolderRenamed(UDFolder folder) {
		logger.info(String.format("Folder renamed %s, new name %s",
				folder.address, folder.name));

	}

	@Override
	public void onNewFolder(UDFolder folder) {
		logger.info(String.format("New Folder %s, name %s", folder.address,
				folder.name));

	}

	@Override
	public void onNodeHasPendingDeviceWrites(UDNode node, boolean hasPending) {
		logger.info(String.format("Node %s, %s pending device writes",
				node.name, hasPending ? "has" : "does not have"));

	}

	@Override
	public void onNodeIsWritingToDevice(UDNode node, boolean isWriting) {
		logger.info(String.format("Node %s, %s being programmed", node.name,
				isWriting ? "is" : "is not"));

	}

	@Override
	public void onNodeParentChanged(UDNode node, UDNode newParent) {
		logger.info(String.format("Node %s, has new parent %s", node.name,
				newParent.name));

	}

	@Override
	public void onNodePowerInfoChanged(UDNode node) {
		logger.info("Not supported ");

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.universaldevices.device.model.IModelChangeListener#onNodeDeviceIdChanged
	 * (com.universaldevices.upnp.UDProxyDevice,
	 * com.universaldevices.device.model.UDNode)
	 */
	@Override
	public void onNodeDeviceIdChanged(UDProxyDevice device, UDNode node) {
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.universaldevices.device.model.IModelChangeListener#
	 * onNodeDevicePropertiesRefreshed(com.universaldevices.upnp.UDProxyDevice,
	 * com.universaldevices.device.model.UDNode)
	 */
	@Override
	public void onNodeDevicePropertiesRefreshed(UDProxyDevice device,
			UDNode node) {
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.universaldevices.device.model.IModelChangeListener#
	 * onNodeDevicePropertiesRefreshedComplete
	 * (com.universaldevices.upnp.UDProxyDevice)
	 */
	@Override
	public void onNodeDevicePropertiesRefreshedComplete(
			UDProxyDevice proxyDevice) {
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.universaldevices.device.model.IModelChangeListener#
	 * onNodeDevicePropertyChanged(com.universaldevices.upnp.UDProxyDevice,
	 * com.universaldevices.device.model.UDNode,
	 * com.universaldevices.common.properties.UDProperty)
	 */
	@Override
	public void onNodeDevicePropertyChanged(UDProxyDevice device, UDNode node,
			UDProperty<?> property) {
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.universaldevices.device.model.IModelChangeListener#onNodeRevised(
	 * com.universaldevices.upnp.UDProxyDevice,
	 * com.universaldevices.device.model.UDNode)
	 */
	@Override
	public void onNodeRevised(UDProxyDevice device, UDNode node) {
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.universaldevices.device.model.IModelChangeListener#onNodeErrorCleared
	 * (com.universaldevices.upnp.UDProxyDevice,
	 * com.universaldevices.device.model.UDNode)
	 */
	@Override
	public void onNodeErrorCleared(UDProxyDevice arg0, UDNode arg1) {
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.universaldevices.device.model.IModelChangeListener#onLinkerEvent(
	 * com.universaldevices.upnp.UDProxyDevice, java.lang.String,
	 * com.nanoxml.XMLElement)
	 */
	@Override
	public void onLinkerEvent(UDProxyDevice arg0, String arg1, XMLElement arg2) {
		// TODO Auto-generated method stub

	}

}
