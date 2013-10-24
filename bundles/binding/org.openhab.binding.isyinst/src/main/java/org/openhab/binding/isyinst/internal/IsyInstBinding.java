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
package org.openhab.binding.isyinst.internal;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.openhab.binding.isyinst.IsyInstBindingProvider;
import org.openhab.binding.isyinst.internal.client.OpenhabISYErrorHandler;
import org.openhab.binding.isyinst.internal.client.OpenhabISYInsteonClient;
import org.openhab.core.binding.AbstractActiveBinding;
import org.openhab.core.library.types.IncreaseDecreaseType;
import org.openhab.core.types.Command;
import org.openhab.core.types.State;
import org.openhab.core.types.Type;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.udi.insteon.client.InsteonConstants;
import com.universaldevices.client.NoDeviceException;
import com.universaldevices.device.model.UDGroup;
import com.universaldevices.device.model.UDNode;
import com.universaldevices.resources.errormessages.Errors;

/**
 * Implement this class if you are going create an actively polling service like
 * querying a Website/Device.
 * 
 * @author opnworks.com
 * @since 1.3.0-SNAPSHOT
 */
public class IsyInstBinding extends
		AbstractActiveBinding<IsyInstBindingProvider> implements ManagedService {

	private static final Logger logger = LoggerFactory
			.getLogger(IsyInstBinding.class);

	/**
	 * the refresh interval which is used to poll values from the IsyInst server
	 * (optional, defaults to 60000ms)
	 */
	private long refreshInterval = 2000L; //60000;

	private OpenhabISYInsteonClient myISY;
	
	IsyCoreTypeMapper typeMapper = new IsyCoreTypeMapper();

	/**
	 * used to store events that we have sent ourselves; we need to remember them for not reacting to them
	 */
	private List<String> ignoreEventList = new ArrayList<String>();
	
	public IsyInstBinding() {
	}

	public void activate() {

		myISY = new OpenhabISYInsteonClient(eventPublisher);
		Errors.addErrorListener(new OpenhabISYErrorHandler());
		// TODO
		// new MyCommandHandler().start();
	}

	public void deactivate() {
		// deallocate resources here that are no longer needed and
		// should be reset when activating this binding again
		logger.info("UNSUBSCRIBING FROM EVENTS");
		getISY().getDevice().unsubscribeFromEvents();
		logger.info("UNSUBSCRIBE FROM EVENTS... DONE");
	}

	/**
	 * @{inheritDoc
	 */
	@Override
	protected long getRefreshInterval() {
		return refreshInterval;
	}

	/**
	 * @{inheritDoc
	 */
	@Override
	protected String getName() {
		return "IsyInst Refresh Service";
	}

	/**
	 * @{inheritDoc
	 */
	@Override
	protected void execute() {
		// the frequently executed code (polling) goes here ...
		logger.debug("execute() method is called!");

		try {
			// if (args.length == 0)
			// app.getISY().start();
			// else if (args.length == 2)
			// app.getISY().start(args[0], args[1]);
			// else
			// {
			// System.err.println("usage: MyISYInsteonClientApp ([uuid][url])|(no args for UPnP)");
			// System.exit(1);
			// }

			getISY().changeNodeState("DFON", null, "B 31 66 1");
			Thread.sleep(100);
			getISY().queryNode("B 31 66 1");
			Thread.sleep(100);
			getISY().changeNodeState("DFOF", null, "B 31 66 1");
			logger.info("app.getISY().changeNodeState(\"DFOF\", null, \"B 31 66 1\")");
			Thread.sleep(100);
			/*
			 * getISY().changeNodeState("X10", "1", "A2"); Thread.sleep(5000);
			 * getISY().changeNodeState("X10", "5", "B"); Thread.sleep(5000);
			 */

		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
	}

	public synchronized OpenhabISYInsteonClient getISY() {
		return myISY;
	}

	/**
	 * @{inheritDoc
	 */
	@Override
	protected void internalReceiveCommand(String itemName, Command command) {
		// the code being executed when a command was sent on the openHAB
		// event bus goes here. This method is only called if one of the
		// BindingProviders provide a binding for the given 'itemName'.
		logger.debug("Received command (item='{}', command='{}')", itemName, command.toString());
		if (!isEcho(itemName, command)) {
			processInsteonCommand(itemName, command);
		}	
	}

	private boolean isEcho(String itemName, Type type) {
		String ignoreEventListKey = itemName + type.toString();
		if (ignoreEventList.contains(ignoreEventListKey)) {
			ignoreEventList.remove(ignoreEventListKey);
			logger.trace("We received this event (item='{}', state='{}') from ISY, so we don't send it back again -> ignore!", itemName, type.toString());
			return true;
		}
		else {
			return false;
		}
	}	
	
	/**
	 * Processes an Insteon command
	 * @param cmd - the command to be processed
	 * @param tk - the StringTokenizer
	 */
	protected void processInsteonCommand(String itemName, Command command){
		
		Set<UDNode> nodes  = getNodes(itemName, command.getClass());
		
//		StringTokenizer tk;
//		String address=tk.nextToken();
		if (nodes.isEmpty()) {
			return;
		}
		UDNode node = null;
		String cmd = null;
		
		if (nodes.size() > 1) {
			// We have a group of nodes
			for (UDNode aNode : nodes) {
				if (aNode.name.equals(command.toString())) { 
					node = aNode;
					break;
				}
			}
			if (node == null) {
				logger.warn("Node named '{}' not found in group '{}'", command, nodes);
				return;
			}
		}
		else {
			node = nodes.iterator().next();
			cmd = typeMapper.toNodeValue(command, node.type);
		}
		
		if (node instanceof UDGroup) {
			if (cmd == null) {
				cmd = InsteonConstants.DEVICE_ON;
			}
			logger.debug("Changing state of node '{}' to '{}'", node, cmd);
			if (!getISY().changeGroupState(cmd , null, node.address)) {
				logger.warn("Command '{}' unsuccessful for group '{}'", node, cmd);
			};
		}
		else {
			logger.debug("Changing state of node '{}' to '{}'", node, cmd);
			if (!getISY().changeNodeState(cmd, null, node.address)) {
				logger.warn("Command '{}' unsuccessful for node '{}'", node, cmd);
			};
		}
	}	
	
	private Set<UDNode> getNodes(String itemName, final Class<? extends Type> typeClass) {
		Set<UDNode> nodes = new HashSet<UDNode>();
		for (IsyInstBindingProvider provider : providers) {
			for (String nodeName : provider.getNodeNames(itemName, typeClass)) {
				try {
//					{E 3B FC 1=SaM-Plafond, E 33 67 1=Living-Plafond, E 36 17 1=Cuisine-Susp, E 27 3E 6=Entree-D, E 27 3E 5=Entree-C, E 27 3E 4=Entree-B, E 27 3E 3=Entree-A, A 1C D 6=Telecommande F, E 27 3E 1=Entree-Dim, A 1C D 5=Telecommande E, A 1C D 4=Telecommande D, E 3D 94 1=SdB-Prinicipale, A 1C D 3=Telecommande C, E 3E AA 1=Chambre-Plafond, A 1C D 2=Telecommande B, A 1C D 1=Telecommande A, E 80 B8 1=Dessous-comptoir}
					for (UDNode node : getISY().getNodes().values()) {
						if (node.name.equals(nodeName)) {
							nodes.add(node);	
						}
					}
					for (UDNode node : getISY().getGroups().values()) {
						if (node.name.equals(nodeName)) {
							nodes.add(node);	
						}
					}
				} catch (NoDeviceException e) {
					logger.warn("Error getting node {}", nodeName, e);
				}
			}
		}
		return nodes;
	}

	/**
	 * @{inheritDoc
	 */
	@Override
	protected void internalReceiveUpdate(String itemName, State newState) {
		// the code being executed when a state was sent on the openHAB
		// event bus goes here. This method is only called if one of the
		// BindingProviders provide a binding for the given 'itemName'.
		logger.debug("internalReceiveCommand() is called!");
	}

	/**
	 * @{inheritDoc
	 */
	@Override
	public void updated(Dictionary<String, ?> config)
			throws ConfigurationException {
		if (config != null) {

			// to override the default refresh interval one has to add a
			// parameter to openhab.cfg like
			// <bindingName>:refresh=<intervalInMs>
			String refreshIntervalString = (String) config.get("refresh");
			if (StringUtils.isNotBlank(refreshIntervalString)) {
				refreshInterval = Long.parseLong(refreshIntervalString);
			}

			// read further config parameters here ...

			setProperlyConfigured(true);
		}
	}

}
