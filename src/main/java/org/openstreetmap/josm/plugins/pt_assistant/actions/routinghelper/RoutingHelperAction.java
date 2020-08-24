package org.openstreetmap.josm.plugins.pt_assistant.actions.routinghelper;

import java.awt.event.ActionEvent;
import java.util.Collections;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import javax.swing.JOptionPane;

import com.drew.lang.annotations.NotNull;
import com.drew.lang.annotations.Nullable;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.MapFrame;
import org.openstreetmap.josm.gui.dialogs.relation.actions.AbstractRelationEditorAction;
import org.openstreetmap.josm.gui.dialogs.relation.actions.IRelationEditorActionAccess;
import org.openstreetmap.josm.gui.dialogs.relation.actions.IRelationEditorUpdateOn;
import org.openstreetmap.josm.plugins.pt_assistant.utils.BoundsUtils;
import org.openstreetmap.josm.tools.I18n;
import org.openstreetmap.josm.tools.ImageProvider;

public class RoutingHelperAction extends AbstractRelationEditorAction {
    private static final Set<ITransportMode> TRANSPORT_MODES = Collections.singleton(new BusTransportMode());

    private Optional<ITransportMode> activeTransportMode;

    private final RoutingHelperPanel routingHelperPanel = new RoutingHelperPanel(this);

    public RoutingHelperAction(IRelationEditorActionAccess editorAccess) {
        super(editorAccess, IRelationEditorUpdateOn.TAG_CHANGE);
        new ImageProvider("dialogs/relation", "routing_assistance.svg").getResource().attachImageIcon(this, true);
        putValue(SHORT_DESCRIPTION, I18n.tr("Routing helper"));
    }

    @Override
    protected void updateEnabledState() {
        final Relation currentRelation = getEditor().getRelation();
        final Optional<ITransportMode> newActiveTransportMode = TRANSPORT_MODES.stream()
            .filter(mode -> mode.canBeUsedForRelation(currentRelation))
            .findFirst();
        this.activeTransportMode = newActiveTransportMode;
        setEnabled(newActiveTransportMode.isPresent() && MainApplication.getMap().getTopPanel(RoutingHelperPanel.class) == null);
    }

    @Override
    public void actionPerformed(@NotNull final ActionEvent actionEvent) {
        final MapFrame mapFrame = MainApplication.getMap();

        if (mapFrame.getTopPanel(RoutingHelperPanel.class) == null) {
            mapFrame.addTopPanel(routingHelperPanel);
            updateEnabledState();
        }

        final Way currentWay = editorAccess.getEditor().getRelation().getMembers().stream().map(it -> it.isWay() ? it.getWay() : null).filter(Objects::nonNull).findFirst().orElse(null);
        if (currentWay != null) {
            MainApplication.getMap().mapView.zoomTo(BoundsUtils.fromBBox(currentWay.getBBox()));
        }
        routingHelperPanel.onCurrentWayChange(currentWay);
    }

    public void goToPreviousGap() {
        JOptionPane.showMessageDialog(routingHelperPanel, "Not implemented yet", "Not implemented", JOptionPane.ERROR_MESSAGE);
    }

    public void goToPreviousWay() {
        JOptionPane.showMessageDialog(routingHelperPanel, "Not implemented yet", "Not implemented", JOptionPane.ERROR_MESSAGE);
    }

    public void goToNextWay() {
        JOptionPane.showMessageDialog(routingHelperPanel, "Not implemented yet", "Not implemented", JOptionPane.ERROR_MESSAGE);
    }

    public void goToNextGap() {
        JOptionPane.showMessageDialog(routingHelperPanel, "Not implemented yet", "Not implemented", JOptionPane.ERROR_MESSAGE);
    }
}
