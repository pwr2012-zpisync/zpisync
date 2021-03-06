/*
 * Copyright (C) 2010 Teleal GmbH, Switzerland
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package zpisync.shared.services;

import java.beans.PropertyChangeSupport;

import org.teleal.cling.binding.annotations.UpnpAction;
import org.teleal.cling.binding.annotations.UpnpInputArgument;
import org.teleal.cling.binding.annotations.UpnpOutputArgument;
import org.teleal.cling.binding.annotations.UpnpService;
import org.teleal.cling.binding.annotations.UpnpServiceId;
import org.teleal.cling.binding.annotations.UpnpServiceType;
import org.teleal.cling.binding.annotations.UpnpStateVariable;

@UpnpService(serviceId = @UpnpServiceId("ZpiSync"), serviceType = @UpnpServiceType(value = "ZpiSync", version = 1))
public class UpnpZpiSync {
	private final PropertyChangeSupport propertyChangeSupport;

	public UpnpZpiSync() {
		this.propertyChangeSupport = new PropertyChangeSupport(this);
	}

	public PropertyChangeSupport getPropertyChangeSupport() {
		return propertyChangeSupport;
	}

	@UpnpStateVariable(defaultValue = "0", sendEvents = false)
	private boolean target = false;

	@UpnpStateVariable(defaultValue = "0")
	private boolean status = false;

	@UpnpStateVariable(defaultValue = "")
	private String endpointUrl = "http://example.com/";

	@UpnpAction
	public void setTarget(@UpnpInputArgument(name = "NewTargetValue") boolean newTargetValue) {

		boolean targetOldValue = target;
		target = newTargetValue;
		boolean statusOldValue = status;
		status = newTargetValue;

		// These have no effect on the UPnP monitoring but it's JavaBean
		// compliant
		getPropertyChangeSupport().firePropertyChange("target", targetOldValue, target);
		getPropertyChangeSupport().firePropertyChange("status", statusOldValue, status);

		// This will send a UPnP event, it's the name of a state variable that
		// sends events
		getPropertyChangeSupport().firePropertyChange("Status", statusOldValue, status);
	}

	@UpnpAction(out = @UpnpOutputArgument(name = "RetTargetValue"))
	public boolean getTarget() {
		return target;
	}

	@UpnpAction(out = @UpnpOutputArgument(name = "ResultStatus"))
	public boolean getStatus() {
		return status;
	}

	// XXX
	private static String url = null;

	public static void setUrl(String synchronizationEndpoint) {
		url = synchronizationEndpoint;
	}

	@UpnpAction(out = @UpnpOutputArgument(name = "EndpointUrl"))
	public String getEndpointUrl() {
		//return endpointUrl;
		return url;
	}

}
