package org.openstreetmap.josm.plugins.pt_assistant.actions.routinghelper;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import javax.swing.JOptionPane;

import com.drew.lang.annotations.NotNull;
import org.openstreetmap.josm.data.osm.AbstractPrimitive;
import org.openstreetmap.josm.data.osm.DataSelectionListener;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.MapFrame;
import org.openstreetmap.josm.gui.Notification;
import org.openstreetmap.josm.gui.dialogs.relation.DownloadRelationMemberTask;
import org.openstreetmap.josm.plugins.pt_assistant.utils.DialogUtils;
import org.openstreetmap.josm.plugins.pt_assistant.utils.RouteUtils;
import org.openstreetmap.josm.plugins.pt_assistant.utils.WayUtils;
import org.openstreetmap.josm.tools.I18n;

public class RoutingHelperAction implements DataSelectionListener {
    private static final Set<ITransportMode> TRANSPORT_MODES = new HashSet<>(Arrays.asList(new BicycleTransportMode(), new BusTransportMode()));

    @NotNull
    private Optional<ITransportMode> activeTransportMode = Optional.empty();

    private final RoutingHelperPanel routingHelperPanel = new RoutingHelperPanel(this);

    @NotNull
    private Optional<Relation> currentRelation = Optional.empty();

    @NotNull
    private Optional<RelationMember> currentMember = Optional.empty();

    @Override
    public void selectionChanged(SelectionChangeEvent event) {
        final MapFrame mapframe = MainApplication.getMap();
        if (mapframe != null) {
            final Optional<Relation> singleRelationSelection = Optional.of(event.getSelection())
                .filter(selection -> selection.size() == 1)
                .map(selection -> selection.iterator().next())
                .map(selectedPrimitive -> selectedPrimitive instanceof Relation ? (Relation) selectedPrimitive : null)
                .filter(RouteUtils::isRoute);
            this.currentRelation = singleRelationSelection;
            this.activeTransportMode = currentRelation.flatMap(relation -> TRANSPORT_MODES.stream().filter(it -> it.canBeUsedForRelation(relation)).findFirst());
            if (singleRelationSelection.isPresent()) {
                routingHelperPanel.onRelationChange(singleRelationSelection.get(), activeTransportMode);
                if (mapframe.getTopPanel(RoutingHelperPanel.class) == null) {
                    mapframe.addTopPanel(routingHelperPanel);
                }
            } else {
                mapframe.removeTopPanel(RoutingHelperPanel.class);
            }
        }
    }

    public void goToFirstWay() {
        final Optional<Relation> currentRelation = this.currentRelation;
        final long missingMembersCount = currentRelation
            .map(it ->
                it.getMembers().stream()
                    .filter(member -> member.getMember().isIncomplete())
                    .count()
            )
            .orElse(0L);
        if (missingMembersCount > 0) {
            if (
                DialogUtils.showYesNoQuestion(
                    routingHelperPanel,
                    I18n.tr("Relation is incomplete"),
                    I18n.trn(
                        "The relations has {0} missing member. Would you like to download the missing member now?",
                        "The relations has {0} missing members. Would you like to download the missing members now?",
                        missingMembersCount,
                        missingMembersCount
                    )
                )
            ) {
                final Future<?> f = MainApplication.worker.submit(new DownloadRelationMemberTask(
                    currentRelation.get(),
                    currentRelation.get().getMembers().stream()
                        .map(RelationMember::getMember)
                        .filter(AbstractPrimitive::isIncomplete)
                        .collect(Collectors.toSet()),
                    MainApplication.getLayerManager().getActiveDataLayer()
                ));
                new Thread(() -> {
                    try {
                        f.get();

                        // try again, now the missingMembersCount should be 0, so we should go to the else-branch this time
                        goToFirstWay();
                    } catch (CancellationException | InterruptedException | ExecutionException e) {
                        JOptionPane.showMessageDialog(
                            routingHelperPanel,
                            I18n.tr("The download of missing members has failed!"),
                            I18n.tr("Download failed"),
                            JOptionPane.ERROR_MESSAGE
                        );
                    }

                }).start();
            }
        } else {
            final List<RelationMember> wayMembers = currentRelation.map(relation -> relation.getMembers().stream().filter(RouteUtils::isRouteWayMember).collect(Collectors.toList())).orElse(Collections.emptyList());
            this.currentMember = wayMembers.stream().findFirst();
            if (wayMembers.isEmpty()) {
                JOptionPane.showMessageDialog(routingHelperPanel, "No way found to traverse", "Could not find a way to traverse", JOptionPane.ERROR_MESSAGE);
            } else {
                routingHelperPanel.onCurrentWayChange(
                    currentRelation.get(),
                    wayMembers.get(0),
                    RoutingHelperPanel.ConnectionType.END,
                    wayMembers.size() == 1
                        ? RoutingHelperPanel.ConnectionType.END
                        : (
                            WayUtils.isTouchingOtherWay(wayMembers.get(0).getWay(), wayMembers.get(1).getWay())
                                ? RoutingHelperPanel.ConnectionType.CONNECTED
                                : RoutingHelperPanel.ConnectionType.NOT_CONNECTED
                        ),
                    0,
                    wayMembers.size()
                );
            }
        }
    }

    public void goToPreviousGap() {
        JOptionPane.showMessageDialog(routingHelperPanel, "Not implemented yet", "Not implemented", JOptionPane.ERROR_MESSAGE);
    }

    public void goToPreviousWay() {
        goNWaysForward(-1);
    }

    public void goToNextWay() {
        goNWaysForward(1);
    }

    private void goNWaysForward(final int n) {
        currentRelation.ifPresent(relation ->
            currentMember.ifPresent(member -> {
                final List<RelationMember> wayMembers = relation.getMembers().stream().filter(RouteUtils::isRouteWayMember).collect(Collectors.toList());
                final int targetIndex = wayMembers.indexOf(member) + n;
                if (targetIndex < 0 || targetIndex >= wayMembers.size() - 1) {
                    new Notification(I18n.tr("You reached the end of the route")).setIcon(JOptionPane.INFORMATION_MESSAGE).setDuration(Notification.TIME_SHORT).show();
                } else {
                    currentMember = Optional.of(wayMembers.get(targetIndex));
                    routingHelperPanel.onCurrentWayChange(
                        relation,
                        wayMembers.get(targetIndex),
                        targetIndex <= 0 ? RoutingHelperPanel.ConnectionType.END : (
                            WayUtils.isTouchingOtherWay(wayMembers.get(targetIndex).getWay(), wayMembers.get(targetIndex - 1).getWay())
                                ? RoutingHelperPanel.ConnectionType.CONNECTED
                                : RoutingHelperPanel.ConnectionType.NOT_CONNECTED
                        ),
                        targetIndex >= wayMembers.size() - 1 ? RoutingHelperPanel.ConnectionType.END : (
                            WayUtils.isTouchingOtherWay(wayMembers.get(targetIndex).getWay(), wayMembers.get(targetIndex + 1).getWay())
                                ? RoutingHelperPanel.ConnectionType.CONNECTED
                                : RoutingHelperPanel.ConnectionType.NOT_CONNECTED
                        ),
                        targetIndex,
                        wayMembers.size()
                    );
                }
            })
        );
    }

    public void goToNextGap() {
        JOptionPane.showMessageDialog(routingHelperPanel, "Not implemented yet", "Not implemented", JOptionPane.ERROR_MESSAGE);
    }

}
