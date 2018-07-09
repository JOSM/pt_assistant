// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.pt_assistant;

import static org.openstreetmap.josm.tools.I18n.tr;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JPanel;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.gui.preferences.DefaultTabPreferenceSetting;
import org.openstreetmap.josm.gui.preferences.PreferenceTabbedPane;
import org.openstreetmap.josm.tools.I18n;

public class PTAssistantPluginPreferences extends DefaultTabPreferenceSetting {
	private JCheckBox downloadIncompleteMembers;
	private JCheckBox stopArea;
	private JCheckBox transferDetails;

	public PTAssistantPluginPreferences() {
		super("bus", tr("PTAssistantPlugin settings"),
				tr("Here you can change some preferences of PTAssistant functions"));
	}

	@Override
	public void addGui(PreferenceTabbedPane gui) {
		JPanel mainPanel = new JPanel();
		mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
		mainPanel.setBorder(BorderFactory.createEmptyBorder(5, 20, 5, 5));

		downloadIncompleteMembers = new JCheckBox(I18n.tr("Download incomplete route relation members"));
		stopArea = new JCheckBox(I18n.tr("Include stop_area tests"));
		transferDetails = new JCheckBox(
				I18n.tr("Add public_transport=platform to the platform node in transfer of details action"));

		markCheckBoxes();

		mainPanel.add(downloadIncompleteMembers);
		mainPanel.add(stopArea);
		mainPanel.add(transferDetails);

		createPreferenceTabWithScrollPane(gui, mainPanel);
	}

	/**
	 * Action to be performed when the OK button is pressed
	 */
	@Override
	public boolean ok() {
		Main.pref.putBoolean("pt_assistant.download-incomplete", this.downloadIncompleteMembers.isSelected());
		Main.pref.putBoolean("pt_assistant.stop-area-tests", this.stopArea.isSelected());
		Main.pref.putBoolean("pt_assistant.transfer-details-action", this.transferDetails.isSelected());
		return false;
	}

	private void markCheckBoxes() {
		if (Main.pref.getBoolean("pt_assistant.download-incomplete"))
			downloadIncompleteMembers.setSelected(true);

		if (Main.pref.getBoolean("pt_assistant.stop-area-tests"))
			stopArea.setSelected(true);

		if (Main.pref.getBoolean("pt_assistant.transfer-details-action"))
			transferDetails.setSelected(true);
	}
}
