// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.pt_assistant;

import static org.openstreetmap.josm.tools.I18n.tr;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.openstreetmap.josm.data.preferences.BooleanProperty;
import org.openstreetmap.josm.gui.preferences.DefaultTabPreferenceSetting;
import org.openstreetmap.josm.gui.preferences.PreferenceTabbedPane;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.I18n;

public class PTAssistantPluginPreferences extends DefaultTabPreferenceSetting {
	private final JCheckBox downloadIncompleteMembers;
	private final JCheckBox stopArea;
	private final JCheckBox transferDetails;
	private final JCheckBox optionsForMendAction;
	private final JCheckBox substitutePlatformRelation;
	private final JCheckBox stopPositionNodeTag;
	private final JCheckBox platformWayDetailsTag;
	private final JCheckBox compareNameWithFirstStop;
	private final JCheckBox compareNameWithLastStop;
	private final JCheckBox checkStartEndIsStopPosition;
	private final JCheckBox modeOfTransportToStop;
	private final JCheckBox splitWay1;
	private final JCheckBox splitWay2;
	public static final BooleanProperty DOWNLOAD_INCOMPLETE = new BooleanProperty("pt_assistant.download-incomplete", false);
	public static final BooleanProperty STOP_AREA_TEST = new BooleanProperty("pt_assistant.stop-area-tests", false);
	public static final BooleanProperty TRANSFER_DETAILS = new BooleanProperty("pt_assistant.transfer-details-action", false);
	public static final BooleanProperty NUMERICAL_OPTIONS = new BooleanProperty("pt_assistant.keep-options-numerical-in-mend-action", false);
	public static final BooleanProperty SUBSTITUTE_PLATFORM_RELATION = new BooleanProperty("pt_assistant.substitute-platformway-relation", true);
	public static final BooleanProperty TRANSFER_STOP_POSITION = new BooleanProperty("pt_assistant.transfer-stopposition-tag", true);
	public static final BooleanProperty TRANSFER_PLATFORMWAY = new BooleanProperty("pt_assistant.transfer-platformway-tag", true);
	public static final BooleanProperty COMPARE_FROM_TAG = new BooleanProperty("pt_assistant.compare-name-from-tag", false);
	public static final BooleanProperty COMPARE_TO_TAG = new BooleanProperty("pt_assistant.compare-name-to-tag", false);
	public static final BooleanProperty CHECK_START_END = new BooleanProperty("pt_assistant.check-route-relation-start-end", false);
	public static final BooleanProperty ADD_MODE_OF_TRANSPORT = new BooleanProperty("pt_assistant.add-mode-of-transport-to-stop", false);
	public static final BooleanProperty SPLITWAY_1 = new BooleanProperty("pt_assistant.split-way-1", true);
	public static final BooleanProperty SPLITWAY_2 = new BooleanProperty("pt_assistant.split-way-2", false);

	public PTAssistantPluginPreferences() {
		super("bus", tr("PTAssistantPlugin settings"),
				tr("Here you can change some preferences of PTAssistant functions"));

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
	}

	@Override
	public void addGui(PreferenceTabbedPane gui) {
		JPanel mainPanel = new JPanel();
		mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
		mainPanel.setBorder(BorderFactory.createEmptyBorder(5, 20, 5, 5));

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
		DOWNLOAD_INCOMPLETE.put(this.downloadIncompleteMembers.isSelected());
		STOP_AREA_TEST.put(this.stopArea.isSelected());
		TRANSFER_DETAILS.put(this.transferDetails.isSelected());
		NUMERICAL_OPTIONS.put(this.optionsForMendAction.isSelected());
		SUBSTITUTE_PLATFORM_RELATION.put(this.substitutePlatformRelation.isSelected());
		TRANSFER_STOP_POSITION.put(this.stopPositionNodeTag.isSelected());
		TRANSFER_PLATFORMWAY.put(this.platformWayDetailsTag.isSelected());
		COMPARE_FROM_TAG.put(this.compareNameWithFirstStop.isSelected());
		COMPARE_TO_TAG.put(this.compareNameWithLastStop.isSelected());
		CHECK_START_END.put(this.checkStartEndIsStopPosition.isSelected());
		ADD_MODE_OF_TRANSPORT.put(this.modeOfTransportToStop.isSelected());
		SPLITWAY_1.put(this.splitWay1.isSelected());
		SPLITWAY_2.put(this.splitWay2.isSelected());

		return false;
	}

	private void markCheckBoxes() {
		downloadIncompleteMembers.setSelected(DOWNLOAD_INCOMPLETE.get());

		stopArea.setSelected(STOP_AREA_TEST.get());

		transferDetails.setSelected(TRANSFER_DETAILS.get());

		optionsForMendAction.setSelected(NUMERICAL_OPTIONS.get());

		substitutePlatformRelation.setSelected(SUBSTITUTE_PLATFORM_RELATION.get());

		stopPositionNodeTag.setSelected(TRANSFER_STOP_POSITION.get());

		platformWayDetailsTag.setSelected(TRANSFER_PLATFORMWAY.get());

		compareNameWithFirstStop.setSelected(COMPARE_FROM_TAG.get());

		compareNameWithLastStop.setSelected(COMPARE_TO_TAG.get());

		checkStartEndIsStopPosition.setSelected(CHECK_START_END.get());

		modeOfTransportToStop.setSelected(ADD_MODE_OF_TRANSPORT.get());

		splitWay1.setSelected(SPLITWAY_1.get());

		splitWay2.setSelected(SPLITWAY_2.get());
	}
}
