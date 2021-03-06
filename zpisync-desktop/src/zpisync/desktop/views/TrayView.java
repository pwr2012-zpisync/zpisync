package zpisync.desktop.views;

import java.awt.AWTException;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.TrayIcon.MessageType;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.logging.Level;
import java.util.logging.Logger;

import zpisync.desktop.Resources;
import zpisync.desktop.ViewBase;
import zpisync.desktop.controllers.AppController;
import zpisync.desktop.models.TrayModel;

public class TrayView extends ViewBase<TrayModel> implements ActionListener {

	private static final Logger log = Logger.getLogger(TrayView.class.getName());

	private AppController app;
	private TrayIcon trayIcon;

	private MenuItem itemSyncNow;
	private MenuItem itemPreferences;
	private MenuItem itemExit;

	/**
	 * Create the frame.
	 */
	public TrayView() {
		this(AppController.NULL);
	}

	public TrayView(AppController app) {
		this.app = app;
		createTrayIcon();
	}

	private void createTrayIcon() {
		if (SystemTray.isSupported()) {
			SystemTray tray = SystemTray.getSystemTray();
			PopupMenu popup = createMenu();

			trayIcon = new TrayIcon(Resources.getAppIcon(), "ZpiSync", popup);
			trayIcon.addActionListener(this);
			trayIcon.setImageAutoSize(true);

			try {
				tray.add(trayIcon);
				return;
			} catch (AWTException e) {
				log.log(Level.SEVERE, e.getMessage(), e);
			}
		}
		log.warning("System tray icon not supported");
	}

	private PopupMenu createMenu() {
		PopupMenu popup = new PopupMenu();
		popup.addActionListener(this);

		popup.add(itemSyncNow = new MenuItem("Sync now"));
		popup.add(itemPreferences = new MenuItem("Preferences..."));
		popup.addSeparator();
		popup.add(itemExit = new MenuItem("Exit"));

		return popup;
	}

	public void displayMessage(String caption, String text, MessageType messageType) {
		if (trayIcon != null)
			trayIcon.displayMessage(caption, text, messageType);
	}

	private boolean isActionSource(ActionEvent e, MenuItem item) {
		if (e.getActionCommand() != null)
			return e.getActionCommand().equals(item.getActionCommand());
		return e.getActionCommand() == item.getActionCommand();
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (isActionSource(e, itemPreferences) || e.getSource() == trayIcon) {
			app.showPreferences();
		} else if (isActionSource(e, itemSyncNow)) {
			app.syncNow();
		} else if (isActionSource(e, itemExit)) {
			app.exit();
		}
	}

	@Override
	public void modelToView(TrayModel model) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void viewToModel(TrayModel model) {
		// TODO Auto-generated method stub
		
	}

}
