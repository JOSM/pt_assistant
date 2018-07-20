// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.pt_assistant;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.gui.preferences.DefaultTabPreferenceSetting;
import org.openstreetmap.josm.gui.preferences.PreferenceTabbedPane;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.I18n;

public class PTAssistantPluginPreferences extends DefaultTabPreferenceSetting {
	private JCheckBox downloadIncompleteMembers;
	private JCheckBox stopArea;
	private JCheckBox transferDetails;
	private JCheckBox optionsForMendAction;
	private JCheckBox substitutePlatformRelation;
	private JCheckBox stopPositionNodeTag;
	private JCheckBox platformWayDetailsTag;
	private JCheckBox compareNameWithFirstStop;
	private JCheckBox compareNameWithLastStop;
	private JCheckBox checkStartEndIsStopPosition;
	private JCheckBox modeOfTransportToStop;
	private JCheckBox splitWay1;
	private JCheckBox splitWay2;

	public PTAssistantPluginPreferences() {
		super("bus", tr("PTAssistantPlugin settings"),
				tr("Here you can change some preferences of PTAssistant functions"));
	}

	@Override
	public void addGui(PreferenceTabbedPane gui) {
		JPanel mainPanel = new JPanel();
		mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
		mainPanel.setBorder(BorderFactory.createEmptyBorder(5, 20, 5, 5));
		initialiseKeys();

		downloadIncompleteMembers = new JCheckBox(I18n.tr("Download incomplete route relation members"));
		stopArea = new JCheckBox(I18n.tr("Include stop_area tests"));
		transferDetails = new JCheckBox(I18n.tr("Remove public_transport=platform from platform ways"));
		optionsForMendAction = new JCheckBox(I18n.tr("Use 1/2/3/4 instead of A/B/C/D for selecting which way to follow"));
		substitutePlatformRelation = new JCheckBox(I18n.tr("Replace platform way in the route relations by node representing the stop"));
		stopPositionNodeTag = new JCheckBox(I18n.tr("Transfer details from stop_position nodes to node representing the stop"));
		platformWayDetailsTag = new JCheckBox(I18n.tr("Transfer details from platform way to node representing the stop (leave tactile_paving and wheelchair on the platform way)"));
		compareNameWithFirstStop = new JCheckBox(I18n.tr("Don’t compare name of first stop with from tag in route relation"));
		compareNameWithLastStop = new JCheckBox(I18n.tr("Don’t compare name of last stop with to tag in route relation"));
		checkStartEndIsStopPosition = new JCheckBox(I18n.tr("Don’t check whether route relation starts and ends on stop_position node"));
		modeOfTransportToStop = new JCheckBox(I18n.tr("Don’t add public_transport=platform and mode of transport to node representing the stop (suffice with highway=bus_stop or railway=tram_stop)"));
		splitWay1 = new JCheckBox(I18n.tr("Add public_transport=stop_position + mode of transport on these nodes"));
		splitWay2 = new JCheckBox(I18n.tr("Always split ways, even if they are not at the end of the itinerary"));

		markCheckBoxes();

		mainPanel.add(new JLabel("<html><b>Validator :</b></html>"),GBC.eol().fill(GBC.HORIZONTAL));
		mainPanel.add(downloadIncompleteMembers);
		mainPanel.add(stopArea);
		mainPanel.add(compareNameWithFirstStop);
		mainPanel.add(compareNameWithLastStop);
		mainPanel.add(checkStartEndIsStopPosition);
		mainPanel.add(new JLabel("<html><br></html>"),GBC.eol().fill(GBC.HORIZONTAL));

		mainPanel.add(new JLabel("<html><b>Move details to nodes next to highway/railway :</b></html>"),GBC.eol().fill(GBC.HORIZONTAL));
		mainPanel.add(substitutePlatformRelation);
		mainPanel.add(transferDetails);
		mainPanel.add(platformWayDetailsTag);
		mainPanel.add(stopPositionNodeTag);
		mainPanel.add(modeOfTransportToStop);
		mainPanel.add(new JLabel("<html><br></html>"),GBC.eol().fill(GBC.HORIZONTAL));

		mainPanel.add(new JLabel("<html><b>Routing Assistant :</b></html>"),GBC.eol().fill(GBC.HORIZONTAL));
		mainPanel.add(optionsForMendAction);
		mainPanel.add(new JLabel("<html><br></html>"),GBC.eol().fill(GBC.HORIZONTAL));

		mainPanel.add(new JLabel("<html><b>Split way at start/end of route relations mapping mode :</b></html>"),GBC.eol().fill(GBC.HORIZONTAL));
		mainPanel.add(splitWay1);
		mainPanel.add(splitWay2);

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
		Main.pref.putBoolean("pt_assistant.keep-options-numerical-in-mend-action", this.optionsForMendAction.isSelected());
		Main.pref.putBoolean("pt_assistant.substitute-platformway-relation", this.substitutePlatformRelation.isSelected());
		Main.pref.putBoolean("pt_assistant.transfer-stopposition-tag", this.stopPositionNodeTag.isSelected());
		Main.pref.putBoolean("pt_assistant.transfer-platformway-tag", this.platformWayDetailsTag.isSelected());
		Main.pref.putBoolean("pt_assistant.compare-name-from-tag", this.compareNameWithFirstStop.isSelected());
		Main.pref.putBoolean("pt_assistant.compare-name-to-tag", this.compareNameWithLastStop.isSelected());
		Main.pref.putBoolean("pt_assistant.check-route-relation-start-end", this.checkStartEndIsStopPosition.isSelected());
		Main.pref.putBoolean("pt_assistant.add-mode-of-transport-to-stop", this.modeOfTransportToStop.isSelected());
		Main.pref.putBoolean("pt_assistant.split-way-1", this.splitWay1.isSelected());
		Main.pref.putBoolean("pt_assistant.split-way-2", this.splitWay2.isSelected());

		return false;
	}

	private void initialiseKeys () {
		// check if the preference contains the key or not, if not open up a dialog box
		Set<String> keySet = Main.pref.getKeySet();
		if (!keySet.contains("pt_assistant.substitute-platformway-relation"))
			Main.pref.putBoolean("pt_assistant.substitute-platformway-relation", true);

		if (!keySet.contains("pt_assistant.transfer-stopposition-tag"))
			Main.pref.putBoolean("pt_assistant.transfer-stopposition-tag", true);

		if (!keySet.contains("pt_assistant.transfer-platformway-tag"))
			Main.pref.putBoolean("pt_assistant.transfer-platformway-tag", true);

		if (!keySet.contains("pt_assistant.split-way-1"))
			Main.pref.putBoolean("pt_assistant.split-way-1", true);
	}

	private void markCheckBoxes() {
		if (Main.pref.getBoolean("pt_assistant.download-incomplete"))
			downloadIncompleteMembers.setSelected(true);

		if (Main.pref.getBoolean("pt_assistant.stop-area-tests"))
			stopArea.setSelected(true);

		if (Main.pref.getBoolean("pt_assistant.transfer-details-action"))
			transferDetails.setSelected(true);

		if (Main.pref.getBoolean("pt_assistant.keep-options-numerical-in-mend-action"))
			optionsForMendAction.setSelected(true);

		if (Main.pref.getBoolean("pt_assistant.substitute-platformway-relation"))
			substitutePlatformRelation.setSelected(true);

		if (Main.pref.getBoolean("pt_assistant.transfer-stopposition-tag"))
			stopPositionNodeTag.setSelected(true);

		if (Main.pref.getBoolean("pt_assistant.transfer-platformway-tag"))
			platformWayDetailsTag.setSelected(true);

		if (Main.pref.getBoolean("pt_assistant.compare-name-from-tag"))
			compareNameWithFirstStop.setSelected(true);

		if (Main.pref.getBoolean("pt_assistant.compare-name-to-tag"))
			compareNameWithLastStop.setSelected(true);

		if (Main.pref.getBoolean("pt_assistant.check-route-relation-start-end"))
			checkStartEndIsStopPosition.setSelected(true);

		if (Main.pref.getBoolean("pt_assistant.add-mode-of-transport-to-stop"))
			modeOfTransportToStop.setSelected(true);

		if (Main.pref.getBoolean("pt_assistant.split-way-1"))
			splitWay1.setSelected(true);

		if (Main.pref.getBoolean("pt_assistant.split-way-2"))
			splitWay2.setSelected(true);
	}
}
