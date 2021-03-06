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

package zpisync.android.browser;

import java.io.File;
import java.sql.Date;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.teleal.cling.android.AndroidUpnpService;
import org.teleal.cling.model.meta.Device;
import org.teleal.cling.model.meta.LocalDevice;
import org.teleal.cling.model.meta.RemoteDevice;
import org.teleal.cling.registry.DefaultRegistryListener;
import org.teleal.cling.registry.Registry;
import org.teleal.cling.transport.SwitchableRouter;

import zpisync.android.handlers.ConfigHandler;
import zpisync.android.handlers.RunHandler;
import zpisync.android.services.AndroidSyncService;
import zpisync.shared.FileInfo;
import android.app.ListActivity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Toast;

/**
 * @author Christian Bauer
 */
public class BrowseActivity extends ListActivity {

    // private static final Logger log = Logger.getLogger(BrowseActivity.class.getName());

    private ArrayAdapter<DeviceDisplay> listAdapter;

    private BrowseRegistryListener registryListener = new BrowseRegistryListener();

    private AndroidUpnpService upnpService;

    private ServiceConnection serviceConnection = new ServiceConnection() {

        public void onServiceConnected(ComponentName className, IBinder service) {
            upnpService = (AndroidUpnpService) service;

            // Refresh the list with all known devices
            listAdapter.clear();
            for (Device<?, ?, ?> device : upnpService.getRegistry().getDevices()) {
                registryListener.deviceAdded(device);
            }

            // Getting ready for future device advertisements
            upnpService.getRegistry().addListener(registryListener);

            // Search asynchronously for all devices
            upnpService.getControlPoint().search();
        }

        public void onServiceDisconnected(ComponentName className) {
            upnpService = null;
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        listAdapter = new ArrayAdapter<DeviceDisplay>(this, android.R.layout.simple_list_item_1);
        setListAdapter(listAdapter);
        getListView().setOnItemClickListener(new AdapterView.OnItemClickListener() {

			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
					long arg3) {
				String text = listAdapter.getItem(arg2).getDevice().toString();
				showToast(text ,true);
		        
			}
        	
		});
        getApplicationContext().bindService(
                new Intent(this, BrowserUpnpService.class),
                serviceConnection,
                Context.BIND_AUTO_CREATE
        );
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (upnpService != null) {
            upnpService.getRegistry().removeListener(registryListener);
        }
        getApplicationContext().unbindService(serviceConnection);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, 0, 0, R.string.search_lan).setIcon(android.R.drawable.ic_menu_search);
        menu.add(0, 1, 0, R.string.switch_router).setIcon(android.R.drawable.ic_menu_revert);
        menu.add(0, 2, 0, R.string.toggle_debug_logging).setIcon(android.R.drawable.ic_menu_info_details);
        menu.add(0, 3, 0, R.string.reset_device_ID).setIcon(android.R.drawable.ic_menu_info_details);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case 0:
                searchNetwork();
                break;
            case 1:
                if (upnpService != null) {
                    SwitchableRouter router = (SwitchableRouter) upnpService.get().getRouter();
                    if (router.isEnabled()) {
                        Toast.makeText(this, R.string.disabling_router, Toast.LENGTH_SHORT).show();
                        router.disable();
                    } else {
                        Toast.makeText(this, R.string.enabling_router, Toast.LENGTH_SHORT).show();
                        router.enable();
                    }
                }
                break;
            case 2:

                Logger logger = Logger.getLogger("org.teleal.cling");
                if (logger.getLevel().equals(Level.FINEST)) {
                    Toast.makeText(this, R.string.disabling_debug_logging, Toast.LENGTH_SHORT).show();
                    logger.setLevel(Level.INFO);
                } else {
                    Toast.makeText(this, R.string.enabling_debug_logging, Toast.LENGTH_SHORT).show();
                    logger.setLevel(Level.FINEST);
                }
                break;
            case 3:
            	try{
            		File file = new File(ConfigHandler.IDFILE);
            		if(file.delete()){
            			System.out.println(file.getName() + " is deleted!");
            			RunHandler.prepareDevice();
            			finish();
            		}else{
            			System.out.println("Delete operation is failed.");
            		}
            	}catch(Exception e){
            		e.printStackTrace();
            	}
            	break;
        }
        return false;
    }

    protected void searchNetwork() {
        if (upnpService == null) return;
        Toast.makeText(this, R.string.searching_lan, Toast.LENGTH_SHORT).show();
        upnpService.getRegistry().removeAllRemoteDevices();
        upnpService.getControlPoint().search();
    }

    protected class BrowseRegistryListener extends DefaultRegistryListener {

        /* Discovery performance optimization for very slow Android devices! */

        @Override
        public void remoteDeviceDiscoveryStarted(Registry registry, RemoteDevice device) {
            deviceAdded(device);
        }

        @Override
        public void remoteDeviceDiscoveryFailed(Registry registry, final RemoteDevice device, final Exception ex) {
            showToast(
                    "Discovery failed of '" + device.getDisplayString() + "': " +
                            (ex != null ? ex.toString() : "Couldn't retrieve device/service descriptors"),
                    true
            );
            deviceRemoved(device);
        }
        /* End of optimization, you can remove the whole block if your Android handset is fast (>= 600 Mhz) */

        @Override
        public void remoteDeviceAdded(Registry registry, RemoteDevice device) {
            deviceAdded(device);
        }

        @Override
        public void remoteDeviceRemoved(Registry registry, RemoteDevice device) {
            deviceRemoved(device);
        }

        @Override
        public void localDeviceAdded(Registry registry, LocalDevice device) {
            deviceAdded(device);
        }

        @Override
        public void localDeviceRemoved(Registry registry, LocalDevice device) {
            deviceRemoved(device);
        }

        public void deviceAdded(final Device<?, ?, ?> device) {
            runOnUiThread(new Runnable() {
                public void run() {
                    DeviceDisplay d = new DeviceDisplay(device);

                    int position = listAdapter.getPosition(d);
                    if (position >= 0) {
                        // Device already in the list, re-set new value at same position
                        listAdapter.remove(d);
                        listAdapter.insert(d, position);
                    } else {
                        listAdapter.add(d);
                    }

                    // Sort it?
                    // listAdapter.sort(DISPLAY_COMPARATOR);
                    // listAdapter.notifyDataSetChanged();
                }
            });
        }

        public void deviceRemoved(final Device<?, ?, ?> device) {
            runOnUiThread(new Runnable() {
                public void run() {
                    listAdapter.remove(new DeviceDisplay(device));
                }
            });
        }
    }

    protected void showToast(final String msg, final boolean longLength) {
        runOnUiThread(new Runnable() {
            public void run() {
                Toast.makeText(
                        BrowseActivity.this,
                        msg,
                        longLength ? Toast.LENGTH_LONG : Toast.LENGTH_SHORT
                ).show();
            }
        });
    }

    protected class DeviceDisplay {
    	String testCase = "test";
        Device<?, ?, ?> device;

        public DeviceDisplay(Device<?, ?, ?> device) {
            this.device = device;
        }

        public Device<?, ?, ?> getDevice() {
            return device;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            DeviceDisplay that = (DeviceDisplay) o;
            return device.equals(that.device);
        }

        @Override
        public int hashCode() {
            return device.hashCode();
        }

        @Override
        public String toString() {
            String name = 
                    device.getDetails() != null && device.getDetails().getFriendlyName() != null
                            ? device.getDetails().getFriendlyName()
                            : device.getDisplayString();
            // Display a little star while the device is being loaded (see performance optimization earlier)
            return (device.isFullyHydrated() ? name : name + " *");
        }
    }

    static final Comparator<DeviceDisplay> DISPLAY_COMPARATOR =
            new Comparator<DeviceDisplay>() {
                public int compare(DeviceDisplay a, DeviceDisplay b) {
                    return a.toString().compareTo(b.toString());
                }
            };
}
