package org.openstreetmap.josm.plugins.pt_assistant.gui.stoparea;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.swing.AbstractAction;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import org.openstreetmap.josm.actions.JosmAction;
import org.openstreetmap.josm.data.osm.BBox;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.dialogs.relation.MemberTableModel;
import org.openstreetmap.josm.gui.dialogs.relation.RelationEditor;
import org.openstreetmap.josm.gui.dialogs.relation.actions.IRelationEditorActionAccess;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.mappaint.Cascade;
import org.openstreetmap.josm.plugins.customizepublictransportstop.OSMTags;
import org.openstreetmap.josm.plugins.pt_assistant.data.DerivedDataSet;
import org.openstreetmap.josm.plugins.pt_assistant.gui.linear.RelationAccess;
import org.openstreetmap.josm.plugins.pt_assistant.gui.linear.RelationEditorAccessUtils;
import org.openstreetmap.josm.plugins.pt_assistant.gui.utils.AbstractVicinityPanel;
import org.openstreetmap.josm.plugins.pt_assistant.gui.utils.UnBoldLabel;
import org.openstreetmap.josm.plugins.pt_assistant.gui.utils.ZoomSaver;
import org.openstreetmap.josm.plugins.pt_assistant.utils.DialogUtils;
import org.openstreetmap.josm.plugins.pt_assistant.utils.DownloadUtils;
import org.openstreetmap.josm.plugins.pt_assistant.utils.StopUtils;

/**
 * Allows to show the vicintiy of a stop area end edit it in a visualized view
 * <p>
 * Supported features:
 * - add / remove platform
 * - add / remove stop_position
 */
public class StopVicinityPanel extends AbstractVicinityPanel {
    public static final String CSS_CLASS_PLATFORM = "platform";
    public static final String CSS_CLASS_EMPTYMEMBER = "emptymember";
    public static final String CSS_CLASS_STOP_POSITION = "stop_position";

    public StopVicinityPanel(IRelationEditorActionAccess editorAccess, ZoomSaver zoomSaver) {
        super(createDataSetWithNewRelation(editorAccess.getEditor().getLayer(),
            editorAccess.getEditor().getRelation(), editorAccess), editorAccess, zoomSaver);

        if (RelationAccess.of(editorAccess)
                .getMembers()
                .stream()
                .anyMatch(it -> it.getMember().isIncomplete())) {
            UnBoldLabel warnPanel = new UnBoldLabel(MessageFormat.format(
                "<html><p>{0}</p><p>{1}</p></html>",
                tr("This relation contains incomplete (not downloaded) members!"),
                tr("Some features may not be visible on this map.")));
            warnPanel.setForeground(new Color(0xAA0000));
            warnPanel.setBorder(new CompoundBorder(
                new LineBorder(warnPanel.getForeground(), 2),
                new EmptyBorder(5, 10, 5, 10)
            ));
            warnPanel.setBackground(new Color(0xFFBABA));
            warnPanel.setOpaque(true);
            //warnPanel.setSize(warnPanel.getMinimumSize());
            //warnPanel.setLocation(10, 40);
            add(warnPanel, BorderLayout.NORTH);
        }

        UnBoldLabel legend = new UnBoldLabel(
            tr("blue: area relation member")
            + " "
            + tr("red: invalid relation member")
            + " "
            + (StopUtils.findParentStopGroup(editorAccess.getEditor().getRelation()) != null
                ?  tr("green: objects belonging to the same area group") : "")
        );
        legend.setBorder(new EmptyBorder(5, 5, 5, 5));
        add(legend, BorderLayout.SOUTH);
    }

    private static DerivedDataSet createDataSetWithNewRelation(OsmDataLayer layer, Relation stopRelation,
                                                               IRelationEditorActionAccess editorAccess) {
        long editedRelationId = stopRelation == null ? 0 : stopRelation.getId();
        BBox bBox = new BBox();
        RelationEditorAccessUtils.streamMembers(editorAccess)
            // Extra space: Something around 200.500m depending on where we are on the map.
            .forEach(p -> bBox.addPrimitive(p.getMember(), 0.005));

        return new DerivedDataSet(layer.getDataSet()) {
            @Override
            protected boolean isIncluded(OsmPrimitive primitive) {
                return primitive.getType() != OsmPrimitiveType.RELATION
                    // Normal primitives: all in bbox
                    ? bBox.intersects(primitive.getBBox())
                    // Relations: all except the one we edit
                    // todo: restrict this, e.g. only PT relations + multipolygons in bbox
                    : primitive.getId() != editedRelationId;
            }

            @Override
            protected void addAdditionalGeometry(AdditionalGeometryAccess addTo) {
                // Now apply the relation editor changes
                // Simulate org.openstreetmap.josm.gui.dialogs.relation.actions.SavingAction.applyChanges
                Relation relation = new Relation();
                editorAccess.getTagModel().applyToPrimitive(relation);
                // This is a hack to tag our currently active relation.
                // There is no id selector in MapCSS, so we need a way to uniquely identify our relation
                relation.put("activePtRelation", "1");

                if (stopRelation != null) {
                    addTo.addAsCopy(stopRelation, relation);

                    // Now we search for all sibling relations.
                    // Due to https://josm.openstreetmap.de/ticket/6129#comment:24 we cannot do it in MapCSS
                    stopRelation.getReferrers()
                        .stream()
                        .filter(r -> r instanceof Relation && StopUtils.isStopAreaGroup((Relation) r))
                        .flatMap(parent -> ((Relation) parent).getMembers().stream())
                        .map(RelationMember::getMember)
                        .filter(sibling -> sibling != stopRelation)
                        .forEach(sibling -> {
                            Relation copy = new Relation((Relation) sibling);
                            copy.put("siblingOfActive", "1");
                            // This will add the copy with the fake tag.
                            addOrGetDerived(copy);
                        });

                } else {
                    addTo.add(relation);
                }

                RelationEditorAccessUtils.getRelationMembers(editorAccess)
                    .forEach(m -> relation.addMember(addOrGetDerivedMember(m)));
            }
        };
    }


    @Override
    protected JComponent generateActionButtons() {
        JPanel panel = new JPanel();
        panel.setOpaque(false);

        JButton downloadButton = new JButton(new JosmAction(
            tr("Download vicinity"),
            "download",
            tr("Download data around the current station area."),
            null,
            false
        ) {
            @Override
            public void actionPerformed(ActionEvent e) {
                DownloadUtils.downloadUsingOverpass(
                    "org/openstreetmap/josm/plugins/pt_assistant/gui/linear/downloadStopAreaVicinity.query.txt",
                    line -> line
                        .replace("##NODEIDS##", DownloadUtils.collectMemberIds(RelationAccess.of(editorAccess), OsmPrimitiveType.NODE))
                        .replace("##WAYIDS##", DownloadUtils.collectMemberIds(RelationAccess.of(editorAccess), OsmPrimitiveType.WAY))
                        .replace("##RELATIONIDS##", DownloadUtils.collectMemberIds(RelationAccess.of(editorAccess), OsmPrimitiveType.RELATION)));
            }
        });
        panel.add(downloadButton);

        Relation group = StopUtils.findParentStopGroup(editorAccess.getEditor().getRelation());
        if (group != null) {
            panel.add(new JButton(new JosmAction(tr("Open area group"), null, null, null, false) {
                @Override
                public void actionPerformed(ActionEvent e) {
                    DialogUtils.showRelationEditor(RelationEditor.getEditor(
                        editorAccess.getEditor().getLayer(),
                        group,
                        group.getMembers()
                            .stream()
                            .filter(m -> m.getMember().getPrimitiveId().equals(editorAccess.getEditor().getRelation()))
                            .collect(Collectors.toList())
                    ));
                }
            }));
        }

        panel.add(generateZoomToButton(tr("Zoom to"), tr("Zoom to the current station area.")));

        return panel;
    }

    @Override
    protected void doInitialZoom() {
        zoomToEditorRelation();
    }

    @Override
    protected List<String> getStylePath() {
        return Arrays.asList(
            "org/openstreetmap/josm/plugins/pt_assistant/gui/stoparea/ptbackground.mapcss",
            "org/openstreetmap/josm/plugins/pt_assistant/gui/stoparea/stopareavicinity.mapcss");
    }

    @Override
    protected OsmPrimitive getPrimitiveAt(Point point) {
        return getOsmPrimitiveAt(point, it -> !getAvailableActions(it).isEmpty());
    }

    @Override
    protected void doAction(Point point, OsmPrimitive originalPrimitive) {
        List<EStopVicinityAction> actions = getAvailableActions(originalPrimitive);
        if (!actions.isEmpty()) {
            showActionsMenu(point, originalPrimitive, actions);
        }
    }

    private void showActionsMenu(Point point, OsmPrimitive primitive, List<EStopVicinityAction> actions) {
        ActionsMenu menu = new ActionsMenu(primitive, actions);
        menu.show(mapView, point.x, point.y);
        menu.addPopupMenuListener(new PopupMenuListener() {
            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                dataSetCopy.getClone().setSelected(Collections.singleton(primitive));
            }

            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
                dataSetCopy.getClone().setSelected(Collections.emptySet());
            }

            @Override
            public void popupMenuCanceled(PopupMenuEvent e) {
            }
        });
    }

    protected List<EStopVicinityAction> getAvailableActions(OsmPrimitive primitive) {
        for (RelationMember m : RelationAccess.of(editorAccess).getMembers()) {
            if (m.getMember().equals(primitive)) {
                return getAvailableActionsForMember(primitive, m.getRole());
            }
        }
        return getAvailableActionsForNonmember(primitive);
    }

    private List<EStopVicinityAction> getAvailableActionsForMember(OsmPrimitive primitive, String role) {
        ArrayList<EStopVicinityAction> actions = new ArrayList<>();
        actions.add(EStopVicinityAction.REMOVE_FROM_STOP_AREA);

        Cascade defaultLayer = getCascade(primitive);
        if (defaultLayer.containsKey(CSS_CLASS_PLATFORM) && !OSMTags.PLATFORM_ROLE.equals(role)) {
            actions.add(EStopVicinityAction.SET_ROLE_PLATFORM);
        }
        if (defaultLayer.containsKey(CSS_CLASS_EMPTYMEMBER) && !"".equals(role)) {
            actions.add(EStopVicinityAction.SET_ROLE_EMPTY);
        }
        if (defaultLayer.containsKey(CSS_CLASS_STOP_POSITION) && !OSMTags.STOP_ROLE.equals(role)) {
            actions.add(EStopVicinityAction.SET_ROLE_STOP);
        }
        return actions;
    }

    private List<EStopVicinityAction> getAvailableActionsForNonmember(OsmPrimitive primitive) {
        Relation area = StopUtils.findContainingStopArea(primitive);
        if (area != null && !area.equals(editorAccess.getEditor().getRelation())) {
            // If the item is in a different stop area, we don't allow adding it.
            ArrayList<EStopVicinityAction> actions = new ArrayList<>();
            actions.add(EStopVicinityAction.OPEN_AREA_RELATION);
            if (null == StopUtils.findParentStopGroup(area)) {
                actions.add(EStopVicinityAction.CREATE_STOP_AREA_GROUP);
            }
            return actions;
        } else {
            Cascade cascade = getCascade(primitive);
            if (cascade.containsKey(CSS_CLASS_STOP_POSITION)) {
                return Arrays.asList(EStopVicinityAction.ADD_STOP_TO_STOP_AREA);
            } else if (cascade.containsKey(CSS_CLASS_PLATFORM)) {
                return Arrays.asList(EStopVicinityAction.ADD_PLATFORM_TO_STOP_AREA);
            } else if (cascade.containsKey(CSS_CLASS_EMPTYMEMBER)) {
                return Arrays.asList(EStopVicinityAction.ADD_EMPTY_TO_STOP_AREA);
            } else {
                return Collections.emptyList();
            }
        }
    }

    // Enum to make it fast, e.g for getAvailableActions
    enum EStopVicinityAction {
        // Add with stop role
        ADD_STOP_TO_STOP_AREA {
            @Override
            void addActionButtons(JPopupMenu menu, OsmPrimitive primitive, IRelationEditorActionAccess editorAccess) {
                menu.add(createActionButton(tr("Add this stop to the stop area relation"), () -> {
                    addMember(editorAccess, primitive, OSMTags.STOP_ROLE);
                }));
            }
        },
        // Add with platform role
        ADD_PLATFORM_TO_STOP_AREA {
            @Override
            void addActionButtons(JPopupMenu menu, OsmPrimitive primitive, IRelationEditorActionAccess editorAccess) {
                menu.add(createActionButton(tr("Add this platform to the stop area relation"), () -> {
                    addMember(editorAccess, primitive, OSMTags.PLATFORM_ROLE);
                }));
            }
        },

        ADD_EMPTY_TO_STOP_AREA {
            @Override
            void addActionButtons(JPopupMenu menu, OsmPrimitive primitive, IRelationEditorActionAccess editorAccess) {
                menu.add(createActionButton(tr("Add this element to the stop area relation"), () -> {
                    addMember(editorAccess, primitive, "");
                }));
            }
        },
        // Remove any member
        REMOVE_FROM_STOP_AREA {
            @Override
            void addActionButtons(JPopupMenu menu, OsmPrimitive primitive, IRelationEditorActionAccess editorAccess) {
                menu.add(createActionButton(tr("Remove from the stop area relation"), () -> {
                    MemberTableModel model = editorAccess.getMemberTableModel();
                    for (int i = 0; i < model.getRowCount(); i++) {
                        if (model.getValue(i).getMember().equals(primitive)) {
                            model.remove(i);
                            break;
                        }
                    }
                }));
            }
        },

        OPEN_AREA_RELATION {
            @Override
            void addActionButtons(JPopupMenu menu, OsmPrimitive primitive, IRelationEditorActionAccess editorAccess) {
                Relation area = StopUtils.findContainingStopArea(primitive);
                if (area != null) {
                    JPanel panel = new JPanel();
                    panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));
                    panel.setBorder(new EmptyBorder(5, 5, 5, 5));
                    panel.add(new UnBoldLabel("<html>" + tr("This element belongs to the stop area <i>{0}</i>",
                        UnBoldLabel.safeHtml(area.getName())) + "</html>"));
                    if (Objects.equals(StopUtils.findParentStopGroup(area), StopUtils.findParentStopGroup(editorAccess.getEditor().getRelation()))) {
                        panel.add(new UnBoldLabel(tr("This element belongs to the same stop area group.")));
                    }
                    menu.add(panel);
                    menu.add(createActionButton(tr("Open area relation"), () -> {
                        DialogUtils.showRelationEditor(RelationEditor.getEditor(
                            editorAccess.getEditor().getLayer(),
                            area,
                            area.getMembers()
                                .stream()
                                .filter(it -> it.getMember().equals(primitive))
                                .collect(Collectors.toList())
                        ));
                    }));
                }
            }
        },
        CREATE_STOP_AREA_GROUP {
            @Override
            void addActionButtons(JPopupMenu menu, OsmPrimitive primitive, IRelationEditorActionAccess editorAccess) {
                // Only works if our relation was already saved
                boolean enabled = editorAccess.getEditor().getRelation() != null;

                JMenuItem button = EStopVicinityAction.createActionButton(tr("Create area group with current area")
                    + (enabled ? "" : " (" + tr("requires save") + ")"), () -> {
                    Relation other = StopUtils.findContainingStopArea(primitive);
                    Relation me = editorAccess.getEditor().getRelation();

                    Relation groupRelation = new Relation();
                    groupRelation.setModified(true);

                    groupRelation.put(OSMTags.KEY_RELATION_TYPE, OSMTags.PUBLIC_TRANSPORT_TAG);
                    groupRelation.put(OSMTags.PUBLIC_TRANSPORT_TAG, OSMTags.STOP_AREA_GROUP_TAG_VALUE);

                    groupRelation.addMember(new RelationMember("", me));
                    groupRelation.addMember(new RelationMember("", other));

                    DialogUtils.showRelationEditor(RelationEditor.getEditor(
                        MainApplication.getLayerManager().getEditLayer(),
                        groupRelation,
                        null /* no selected members */
                    ));
                });
                button.setEnabled(enabled);
                menu.add(button);
            }
        },
        SET_ROLE_PLATFORM {
            @Override
            void addActionButtons(JPopupMenu menu, OsmPrimitive primitive, IRelationEditorActionAccess editorAccess) {
                addActionButtonForRole(menu, primitive, editorAccess, OSMTags.PLATFORM_ROLE);
            }
        },
        SET_ROLE_STOP {
            @Override
            void addActionButtons(JPopupMenu menu, OsmPrimitive primitive, IRelationEditorActionAccess editorAccess) {
                addActionButtonForRole(menu, primitive, editorAccess, OSMTags.STOP_ROLE);
            }
        },
        SET_ROLE_EMPTY {
            @Override
            void addActionButtons(JPopupMenu menu, OsmPrimitive primitive, IRelationEditorActionAccess editorAccess) {
                addActionButtonForRole(menu, primitive, editorAccess, "");
            }
        };

        private static void addActionButtonForRole(JPopupMenu menu, OsmPrimitive primitive, IRelationEditorActionAccess editorAccess, String role) {
            menu.add(createActionButton(tr("Set role to ''{0}''", role), () -> {
                for (int i = 0; i < editorAccess.getMemberTableModel().getRowCount(); i++) {
                    if (editorAccess.getMemberTableModel().getValue(i).getMember().equals(primitive)) {
                        editorAccess.getMemberTableModel().updateRole(new int[] {i}, role);
                    }
                }
            }));
        }

        private static void addMember(IRelationEditorActionAccess editorAccess, OsmPrimitive primitive, String role) {
            MemberTableModel table = editorAccess.getMemberTableModel();
            table.addMembersAtEnd(Collections.singletonList(primitive));
            table.updateRole(new int[]{table.getRowCount() - 1}, role);
        }

        abstract void addActionButtons(JPopupMenu menu, OsmPrimitive primitive, IRelationEditorActionAccess editorAccess);

        private static JMenuItem createActionButton(String text, Runnable action) {
            return new JMenuItem(new AbstractAction(text) {
                @Override
                public void actionPerformed(ActionEvent e) {
                    action.run();
                }
            });
        }

    }

    private class ActionsMenu extends JPopupMenu {
        ActionsMenu(OsmPrimitive forPrimitive, List<EStopVicinityAction> actions) {
            add(new TagsOfPrimitive(forPrimitive));
            add(new JSeparator());
            for (EStopVicinityAction action : actions) {
                action.addActionButtons(this, forPrimitive, editorAccess);
            }
            add(EStopVicinityAction.createActionButton(tr("Select in main window"), () -> {
                MainApplication.getLayerManager().getActiveDataSet()
                    .setSelected(forPrimitive);
            }));
        }
    }
}
