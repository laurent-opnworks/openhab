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
import java.util.List;
import java.util.StringTokenizer;

import org.apache.commons.lang.StringUtils;
import org.openhab.binding.isyinst.IsyInstBindingProvider;
import org.openhab.core.binding.BindingConfig;
import org.openhab.core.items.Item;
import org.openhab.core.types.Type;
import org.openhab.model.item.binding.AbstractGenericBindingProvider;
import org.openhab.model.item.binding.BindingConfigParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is responsible for parsing the binding configuration.
 * 
 * @author opnworks.com
 * @since 1.3.0-SNAPSHOT
 */
public class IsyInstGenericBindingProvider extends AbstractGenericBindingProvider implements IsyInstBindingProvider {

	private static final Logger logger = LoggerFactory
			.getLogger(IsyInstGenericBindingProvider.class);
	
	/**
	 * {@inheritDoc}
	 */
	public String getBindingType() {
		return "isyinst";
	}

	/**
	 * @{inheritDoc}
	 */
	@Override
	public void validateItemType(Item item, String bindingConfig) throws BindingConfigParseException {
		//if (!(item instanceof SwitchItem || item instanceof DimmerItem)) {
		//	throw new BindingConfigParseException("item '" + item.getName()
		//			+ "' is of type '" + item.getClass().getSimpleName()
		//			+ "', only Switch- and DimmerItems are allowed - please check your *.items configuration");
		//}
		logger.info("validateItemType(). item: " + item + ", bindingConfig: " + bindingConfig); 
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void processBindingConfiguration(String context, Item item, String bindingConfig) throws BindingConfigParseException {
		super.processBindingConfiguration(context, item, bindingConfig);
		
		// processBindingConfiguration(). context: demo.items, item: Chambre_Plafond (Type=DimmerItem, State=Uninitialized), bindingConfig: Chambre-Plafond
		logger.info("processBindingConfiguration(). context: " + context + ", item: " + item + ", bindingConfig: " + bindingConfig); 
		super.processBindingConfiguration(context, item, bindingConfig);
		
		//parse bindingconfig here ...
		if (bindingConfig != null && bindingConfig.startsWith("groups:")) {
			bindGroups(context, item, StringUtils.substringAfter(bindingConfig, ":"));
		}
		else {
			IsyInstBindingConfig config = new IsyInstBindingConfig();
			config.nodeName = bindingConfig;
			addBindingConfig(item, config);
			logger.info("Binding configuration saved for item '{}'. BindingConfig: {}", item.toString(), bindingConfig);
		}
	}
	
	
	private void bindGroups(String context, Item item, String groups) {
		StringTokenizer tk = new StringTokenizer(groups, ",");
		IsyInstGroupBindingConfig config = new IsyInstGroupBindingConfig();
		while (tk.hasMoreTokens()) {
			String groupName = tk.nextToken();
			config.nodeNames.add(groupName);
		}
		addBindingConfig(item, config);
		logger.info("Group binding configuration saved for item '{}'. Groups: {}", item.toString(), groups);
	}


	class IsyInstBindingConfig implements BindingConfig {
		// put member fields here which holds the parsed values
		public String nodeName;
//		public UDControl control;
//		public UDNode node;
		
//		public Datapoint mainDataPoint = null;
//		public Datapoint readableDataPoint = null;
//		public Map<String, UDNode> allNodes = new HashMap<String, UDNode>();
	}
	
	class IsyInstGroupBindingConfig implements BindingConfig {
		// put member fields here which holds the parsed values
		public List<String> nodeNames = new ArrayList<String>();
	}


	@Override
	public Iterable<String> getNodeNames(String itemName,
			Class<? extends Type> typeClass) {

		synchronized(bindingConfigs) {
			List<String> nodeNames = new ArrayList<String>();
	
			BindingConfig config = bindingConfigs.get(itemName);

			if (config instanceof IsyInstBindingConfig) {
				nodeNames.add(((IsyInstBindingConfig) config).nodeName);
			}
			else if (config instanceof IsyInstGroupBindingConfig) {
				nodeNames.addAll(((IsyInstGroupBindingConfig) config).nodeNames);
			}
			return nodeNames;
		}
	}
	
	
}
