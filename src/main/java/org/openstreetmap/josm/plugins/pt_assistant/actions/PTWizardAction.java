// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.pt_assistant.actions;

import static org.openstreetmap.josm.gui.help.HelpUtil.ht;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.SpinnerNumberModel;

import org.openstreetmap.josm.actions.JosmAction;
import org.openstreetmap.josm.data.Preferences;
import org.openstreetmap.josm.data.preferences.sources.SourceEntry;
import org.openstreetmap.josm.data.preferences.sources.SourceType;
import org.openstreetmap.josm.gui.ExtendedDialog;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.MainMenu;
import org.openstreetmap.josm.gui.Notification;
import org.openstreetmap.josm.gui.dialogs.properties.TagEditHelper;
import org.openstreetmap.josm.gui.download.UserQueryList.SelectorItem;
import org.openstreetmap.josm.gui.io.CustomConfigurator;
import org.openstreetmap.josm.gui.mappaint.MapPaintStyles;
import org.openstreetmap.josm.gui.mappaint.StyleSource;
import org.openstreetmap.josm.io.CachedFile;
import org.openstreetmap.josm.plugins.pt_assistant.utils.PTProperties;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.Logging;

/**
 * A wizard that helps users in working with key features of PT Assistant
 *
 * @author Biswesh
 */
public final class PTWizardAction extends JosmAction {
    private static final SpinnerNumberModel QUESTION_1_SPINNER_MODEL = new SpinnerNumberModel();

    public boolean noDialogBox;
    private int closeCheck = 0;
    /**
     * Constructs a new {@code PTWizardAction}
     */
    public PTWizardAction() {
        super(
            tr("PT Wizard"),
            "clock",
            tr("Set up PT Assistant for more convenient use"),
            null,
            false
        );

        putValue("help", ht("/Action/PTWizard"));
        putValue("toolbar", "help/PTWizard");
        MainApplication.getToolbar().register(this);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (PTProperties.WIZARD_PAGES.get().isEmpty()) {
            readPreferencesFromXML();
            addDefaultValues();
        }

        if (this.noDialogBox) {
            this.noDialogBox = false;
        } else {
            JPanel panel = new JPanel(new GridBagLayout());
            panel.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));

            PTWizardDialog wizardDialog = new PTWizardDialog();
            wizardDialog.setPreferredSize(new Dimension(250, 300));
            wizardDialog.setButtonIcons("ok", "cancel");
            JScrollPane scrollPanel = new JScrollPane(panel);
            wizardDialog.setContent(scrollPanel, false);
            nextAct(0, panel);

            int lastCheck = -1;
            try {
                for (int i = 1; i <= 4; i++) {
                    if (lastCheck == closeCheck) {
                        return;
                    } else {
                        lastCheck = closeCheck;
                    }
                    ExtendedDialog dialog = wizardDialog.showDialog();
                    switch (dialog.getValue()) {
                        case 1: nextAct(i, panel); break;
                        default: return; // Do nothing
                    }
                }
            } catch (Exception ex) {
                Logging.error(ex);
            }
        }
    }

    private void readPreferencesFromXML() {
        try {
            final CachedFile cf = getCachedFile();
            if (cf != null) {
                new CustomConfigurator.XMLCommandProcessor(Preferences.main()).openAndReadXML(
                    new ByteArrayInputStream(
                        cf.getContentReader().lines()
                            .filter(line -> !line.contains("{{{") && !line.contains("}}}"))
                            .collect(Collectors.joining("\n"))
                            .getBytes(StandardCharsets.UTF_8)
                    )
                );
            }
        } catch (IOException e) {
            Logging.error(e);
        }
    }

    @SuppressWarnings("resource")
    public CachedFile getCachedFile() {
        try {
            return new CachedFile("https://josm.openstreetmap.de/wiki/Plugin/PT_Assistant/Wizard?format=txt").setHttpAccept("text");
        } catch (Exception e) {
            new Notification(tr("Unable to connect to {0}",
                    "https://josm.openstreetmap.de/wiki/Plugin/PT_Assistant/Wizard")).setIcon(JOptionPane.WARNING_MESSAGE).show();
        }
        return null;
    }

    private void addDefaultValues() {
        TagEditHelper.PROPERTY_RECENT_TAGS_NUMBER.put(PTProperties.WIZARD_1_SUGGESTION.get());
        question2ChangeValues(PTProperties.getWizardSuggestions(2).stream().map(it -> it.get(0)).collect(Collectors.toList()));
        question3ChangeValues(PTProperties.getWizardSuggestions(3).stream().filter(
                it -> "Default".equals(it.get(1))).map(it -> it.get(0)).collect(Collectors.toList()));
        question4ChangeValues(PTProperties.getWizardSuggestions(4).stream().filter(
                it -> "Default".equals(it.get(1))).map(it -> it.get(0)).collect(Collectors.toList()));
    }

    private void addLabel(JPanel panel, int questionNumber, boolean withTitle, boolean withQuestion, boolean withSuggestion) {

        if (withTitle) {
            String title = Config.getPref().get("pt_assistant.wizard."+ questionNumber +".title");
            JTextArea j = new JTextArea(tr(title));
            j.setLineWrap(true);
            j.setWrapStyleWord(true);
            j.setEditable(false);
            j.setOpaque(false);
            j.setFont(new java.awt.Font("Serif", Font.BOLD, 18));
            panel.add(j, GBC.eol().fill(GBC.HORIZONTAL));
        }

        if (withQuestion) {
            String question = Config.getPref().get("pt_assistant.wizard."+ questionNumber +".question");
            JTextArea j = new JTextArea(tr(question));
            j.setLineWrap(true);
            j.setWrapStyleWord(true);
            j.setEditable(false);
            j.setOpaque(false);
            panel.add(new JLabel("<html><br></html>"), GBC.eol().fill(GBC.HORIZONTAL));
            panel.add(j, GBC.eol().fill(GBC.HORIZONTAL));
        }

        if (withSuggestion) {
            String suggestion = Config.getPref().get("pt_assistant.wizard."+ questionNumber +".suggestion");
            JTextArea j = new JTextArea(tr("suggested value : "+ suggestion));
            j.setLineWrap(true);
            j.setWrapStyleWord(true);
            j.setEditable(false);
            j.setOpaque(false);
            panel.add(new JLabel("<html><br></html>"), GBC.eol().fill(GBC.HORIZONTAL));
            panel.add(j, GBC.eol().fill(GBC.HORIZONTAL));
        }
    }

    private void introduction(JPanel panel) {
        addLabel(panel, 0, true, false, false);

        String information = PTProperties.WIZARD_INFORMATION.get();
        JTextArea j = new JTextArea(tr(information));
        j.setLineWrap(true);
        j.setWrapStyleWord(true);
        j.setEditable(false);
        j.setOpaque(false);
        panel.add(new JLabel("<html><br></html>"), GBC.eol().fill(GBC.HORIZONTAL));
        panel.add(j, GBC.eol().fill(GBC.HORIZONTAL));
    }

    private void question1(JPanel panel) {
        addLabel(panel, 1, true, true, false);
        QUESTION_1_SPINNER_MODEL.setValue(TagEditHelper.PROPERTY_RECENT_TAGS_NUMBER.get());
        final JSpinner spinner = new JSpinner(QUESTION_1_SPINNER_MODEL);
        panel.add(new JLabel("<html><br></html>"), GBC.eol().fill(GBC.HORIZONTAL));
        panel.add(spinner, GBC.eol().fill(GBC.HORIZONTAL));
    }

    private void question2(JPanel panel) {

        addLabel(panel, 2, true, true, false);

        Box checkBoxPanel = Box.createVerticalBox();
        checkBoxPanel.setOpaque(true);
        checkBoxPanel.setBackground(Color.white);

        List<String> currentList = Config.getPref().getList("toolbar");

        try {
            for (List<String> suggestionList : PTProperties.getWizardSuggestions(2)) {
                final String content = suggestionList.get(0);
                final String value = suggestionList.get(1);
                final JCheckBox con = new JCheckBox(content);
                if (currentList.contains(value)) {
                    con.setSelected(true);
                }
                checkBoxPanel.add(con);
            }
        } catch (Exception e) {
            Logging.warn(e);
        }

        checkBoxPanel.setBackground(Color.white);
        checkBoxPanel.add(Box.createRigidArea(new Dimension(10, 0)));

        panel.add(new JLabel("<html><br></html>"), GBC.eol().fill(GBC.HORIZONTAL));
        panel.add(checkBoxPanel, GBC.eol().fill(GBC.HORIZONTAL));
    }

    private void question3(JPanel panel) {

        addLabel(panel, 3, true, true, false);

        Box checkBoxPanel = Box.createVerticalBox();
        checkBoxPanel.setOpaque(true);
        checkBoxPanel.setBackground(Color.white);

        List<StyleSource> styleList = MapPaintStyles.getStyles().getStyleSources();
        try {
            for (List<String> suggestionList : PTProperties.getWizardSuggestions(3)) {
                final String paintStyle = suggestionList.get(0);
                JCheckBox con = new JCheckBox(paintStyle);
                for (StyleSource style : styleList) {
                    if (style.title.equals(paintStyle)) {
                        con.setSelected(true);
                        break;
                    }
                }
                checkBoxPanel.add(con);
            }
        } catch (Exception e) {
            Logging.error(e);
        }

        checkBoxPanel.setBackground(Color.white);
        checkBoxPanel.add(Box.createRigidArea(new Dimension(10, 0)));

        panel.add(new JLabel("<html><br></html>"), GBC.eol().fill(GBC.HORIZONTAL));
        panel.add(checkBoxPanel, GBC.eol().fill(GBC.HORIZONTAL));
    }

    private void question4(JPanel panel) {
        addLabel(panel, 4, true, true, false);

        Box checkBoxPanel = Box.createVerticalBox();
        checkBoxPanel.setOpaque(true);
        checkBoxPanel.setBackground(Color.white);

        final Map<String, SelectorItem> items = restorePreferences();

        try {
            for (List<String> suggestionList : PTProperties.getWizardSuggestions(4)) {
                String content = suggestionList.get(0);
                JCheckBox con = new JCheckBox(content);
                if (items.containsKey(content))
                        con.setSelected(true);
                checkBoxPanel.add(con);
            }
        } catch (Exception e) {
             Logging.warn(e);
        }

        checkBoxPanel.setBackground(Color.white);
        checkBoxPanel.add(Box.createRigidArea(new Dimension(10, 0)));

        panel.add(new JLabel("<html><br></html>"), GBC.eol().fill(GBC.HORIZONTAL));
        panel.add(checkBoxPanel, GBC.eol().fill(GBC.HORIZONTAL));
    }

    private void question1Action() {
        TagEditHelper.PROPERTY_RECENT_TAGS_NUMBER.put(QUESTION_1_SPINNER_MODEL.getNumber().intValue());
    }

    /*
     * the following function is common action for questions 2 to 4
     */
    private void question2to4Action(JPanel panel, int questionNumber) {

        List<String> finalValues = new ArrayList<>();
        Component[] components = panel.getComponents();
        for (Component comp : components) {
            if (comp instanceof Box) {
                Box checkBox = (Box) comp;
                Component[] checkBoxComponents = checkBox.getComponents();
                for (Component checkComponent : checkBoxComponents) {
                    if (checkComponent instanceof JCheckBox) {
                        JCheckBox checks = (JCheckBox) checkComponent;
                        if (checks.isSelected()) {
                            finalValues.add(checks.getText());
                        }
                    }
                }

            }
        }

        switch (questionNumber) {
            case 2: question2ChangeValues(finalValues); break;
            case 3: question3ChangeValues(finalValues); break;
            case 4: question4ChangeValues(finalValues); break;
            default : // Do nothing
        }
    }

    private void question2ChangeValues(List<String> finalValues) {
        if (Logging.isDebugEnabled()) {
            Logging.debug(String.join("\n", finalValues));
        }
        if (finalValues.contains("Sort stops in route relation")) {
            MainMenu.add(MainApplication.getMenu().toolsMenu, new SortPTRouteMembersMenuBar());
        }

        List<String> current = Config.getPref().getList("toolbar");

        final List<String> newList = new ArrayList<>(current);

        if (current.size() == 0) {
            newList.addAll(Arrays.asList(defaultToolBar()));
        }

        try {
            for (List<String> suggestionList : PTProperties.getWizardSuggestions(2)) {
                String content = suggestionList.get(0);
                String value = suggestionList.get(1);
                if (finalValues.contains(content) && !newList.contains(value)) {
                    newList.add(value);
                } else if (!finalValues.contains(content)) {
                    newList.remove(value);
                }
            }
        } catch (Exception e) {
             Logging.warn(e);
        }

        List<String> t = new LinkedList<>(newList);
        try {
            Config.getPref().putList("toolbar", t);
            MainApplication.getToolbar().refreshToolbarControl();
        } catch (Exception e) {
            Logging.warn(e);
        }
    }

    private void question3ChangeValues(List<String> finalValues) {
        try {
            for (List<String> suggestionList : PTProperties.getWizardSuggestions(3)) {
                String paintStyle = suggestionList.get(0);
                String url = suggestionList.get(2);

                final List<StyleSource> styleList = MapPaintStyles.getStyles().getStyleSources();

                if (finalValues.contains(paintStyle)) {
                    if (styleList.stream().noneMatch(style -> style.title.equals(paintStyle))) {
                        MapPaintStyles.addStyle(new SourceEntry(SourceType.MAP_PAINT_STYLE, url, null, paintStyle, true));
                    }
                } else {
                    styleList.stream()
                        .filter(style -> style.title.equals(paintStyle))
                        .findFirst()
                        .ifPresent(style -> {
                            MapPaintStyles.removeStyle(new SourceEntry(SourceType.MAP_PAINT_STYLE, url, null, paintStyle, true));
                        });
                }
            }
        } catch (Exception e) {
            Logging.warn(e);
        }
    }

    private void question4ChangeValues(List<String> finalValues) {
        final List<List<String>> suggestionLists = PTProperties.getWizardSuggestions(4);
        List<SelectorItem> itemList = new ArrayList<>();
        List<String> unmarkedKeyList = new ArrayList<>();

        Map<String, SelectorItem> items;
        items = restorePreferences();

        for (List<String> keys : suggestionLists) {
            String Key = keys.get(0);
            if (!finalValues.contains(Key))
                    unmarkedKeyList.add(Key);
        }

        for (String fv : finalValues) {
            if (!items.containsKey(fv)) {
                for (List<String> suggestions : suggestionLists) {
                    String key = suggestions.get(0);
                    if (key == fv) {
                        String Value = "";
                        for (int i = 2; i < suggestions.size(); i++) {
                            Value = Value + suggestions.get(i);
                        }
                        SelectorItem item = new SelectorItem(key, Value);
                        itemList.add(item);
                        break;
                    }
                }
            }

        }

        for (String unmarkedKey : unmarkedKeyList) {
            if (items.containsKey(unmarkedKey)) {
                items.remove(unmarkedKey);
            }
        }

        for (SelectorItem item : itemList) {
            items.put(item.getKey(), item);
        }
        try {
            savePreferences(items);
        } catch (Exception e) {
            Logging.warn(e);
        }
    }

    /**
     * Loads the user saved items from preferences.
     * @return A set of the user saved items.
     */
    private Map<String, SelectorItem> restorePreferences() {
        Collection<Map<String, String>> toRetrieve =
                Config.getPref().getListOfMaps("download.overpass.queries", Collections.emptyList());
        Map<String, SelectorItem> result = new HashMap<>();
        final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss, dd-MM-yyyy");

        for (Map<String, String> entry : toRetrieve) {
            try {
                String key = entry.get("key");
                String query = entry.get("query");
                String lastEditText = entry.get("lastEdit");
                // Compatibility: Some entries may not have a last edit set.
                LocalDateTime lastEdit = lastEditText == null ? LocalDateTime.MIN : LocalDateTime.parse(lastEditText, FORMAT);

                result.put(key, new SelectorItem(key, query, lastEdit));
            } catch (Exception e) {
                Logging.warn(e);
            }
        }

        return result;
    }

    /**
     * Saves all elements from the list to the preferences.
     * @param items selector items to save
     */
    private void savePreferences(Map<String, SelectorItem> items) {
            final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss, dd-MM-yyyy");
        List<Map<String, String>> toSave = new ArrayList<>(items.size());
        for (SelectorItem item : items.values()) {
            Map<String, String> it = new HashMap<>();
            it.put("key", item.getKey());
            it.put("query", item.getQuery());
            it.put("lastEdit", item.getLastEdit().format(FORMAT));

            toSave.add(it);
        }

        Config.getPref().putListOfMaps("download.overpass.queries", toSave);
    }

    private String[] defaultToolBar() {
        String[] deftoolbar = {"open", "save", "download", "upload", "|",
            "undo", "redo", "|", "dialogs/search", "preference", "|", "splitway", "combineway",
            "wayflip", "|", "imagery-offset", "|", "tagginggroup_Highways/Streets",
            "tagginggroup_Highways/Ways", "tagginggroup_Highways/Waypoints",
            "tagginggroup_Highways/Barriers", "|", "tagginggroup_Transport/Car",
            "tagginggroup_Transport/Public Transport", "|", "tagginggroup_Facilities/Tourism",
            "tagginggroup_Facilities/Food+Drinks", "|", "tagginggroup_Man Made/Historic Places", "|",
            "tagginggroup_Man Made/Man Made"};
        return deftoolbar;
    }

    private void nextAct(int pageNumber, JPanel panel) {

        switch (pageNumber) {
            case 2: question1Action(); break;
            case 3: question2to4Action(panel, 2); break;
            case 4: question2to4Action(panel, 3); break;
            case 5: question2to4Action(panel, 4); break;
            default : // Do nothing
        }

        panel.removeAll();

        switch (pageNumber) {
            case 0: introduction(panel); break;
            case 1: question1(panel); break;
            case 2: question2(panel); break;
            case 3: question3(panel); break;
            case 4: question4(panel); break;
            default : // Do nothing
        }
    }

    private class PTWizardDialog extends ExtendedDialog {

        PTWizardDialog() {
            super(MainApplication.getMainFrame(), tr("PT Wizard"), new String[] {tr("Ok"), tr("Cancel") },
                    true);
        }

        @Override
        protected void buttonAction(int buttonIndex, ActionEvent evt) {
            closeCheck++;
            super.buttonAction(buttonIndex, evt);
        }
    }
}
