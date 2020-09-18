package org.openstreetmap.josm.plugins.pt_assistant.actions;

import java.awt.event.ActionEvent;
import java.util.*;
import java.util.stream.Collectors;

import javax.swing.*;

import org.openstreetmap.josm.command.AddCommand;
import org.openstreetmap.josm.command.ChangeCommand;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.command.SequenceCommand;
import org.openstreetmap.josm.data.UndoRedoHandler;
import org.openstreetmap.josm.data.osm.*;
import org.openstreetmap.josm.gui.dialogs.relation.IRelationEditor;
import org.openstreetmap.josm.gui.dialogs.relation.MemberTableModel;
import org.openstreetmap.josm.gui.dialogs.relation.RelationEditor;
import org.openstreetmap.josm.gui.dialogs.relation.actions.AbstractRelationEditorAction;
import org.openstreetmap.josm.gui.dialogs.relation.actions.IRelationEditorActionAccess;
import org.openstreetmap.josm.gui.dialogs.relation.actions.IRelationEditorUpdateOn;
import org.openstreetmap.josm.plugins.pt_assistant.data.PTSegmentToExtract;
import org.openstreetmap.josm.plugins.pt_assistant.utils.RouteUtils;
import org.openstreetmap.josm.tools.*;

import static java.util.Collections.*;
import static org.openstreetmap.josm.gui.MainApplication.*;
import static org.openstreetmap.josm.plugins.pt_assistant.data.PTStop.isPTStop;

/*
Extracts selected members to a new route relation
and substitutes them with this new route relation
in the original relation

 * @author Florian, Polyglot
 */
public class ExtractRelationMembersToNewRelationAction extends AbstractRelationEditorAction {
    public ExtractRelationMembersToNewRelationAction(IRelationEditorActionAccess editorAccess) {
        super(editorAccess, IRelationEditorUpdateOn.MEMBER_TABLE_SELECTION,
            IRelationEditorUpdateOn.MEMBER_TABLE_CHANGE,
            IRelationEditorUpdateOn.TAG_CHANGE);
        new ImageProvider("dialogs/relation", "extract_relation").getResource().attachImageIcon(this);
    }

    @Override
    public void actionPerformed(ActionEvent actionEvent) {
        final MemberTableModel memberTableModel = editorAccess.getMemberTableModel();
        IRelationEditor editor = editorAccess.getEditor();
        final Relation originalRelation = editor.getRelation();
        Relation editedRelation = new Relation(originalRelation);
        // save the current state, otherwise accidents happen
        memberTableModel.applyToRelation(editedRelation);
        editorAccess.getTagModel().applyToPrimitive(editedRelation);
        UndoRedoHandler.getInstance().add(new ChangeCommand(originalRelation, editedRelation));

        final Collection<RelationMember> selectedMembers = memberTableModel.getSelectedMembers();

        List<Command> commands = new ArrayList<>();

        String title = I18n.tr("Extract part from relation?");
        String question = I18n.tr(
            "Do you want to create a new relation with the {0} selected members?",
            selectedMembers.size()
        );
        List<Relation> parentRelations = new ArrayList<>(Utils.filteredCollection(originalRelation.getReferrers(), Relation.class));
        parentRelations.removeIf(parentRelation -> !Objects.equals(parentRelation.get("type"), "superroute"));
        final int numberOfParentRelations = parentRelations.size();
        final JCheckBox cbReplaceInSuperrouteRelations = new JCheckBox(I18n.tr(
            "This route is a member of {0} superroute {1}, add the extracted relation there instead of in this relation?",
            numberOfParentRelations,
            I18n.trn("relation", "relations", numberOfParentRelations)));
        final JCheckBox cbConvertToSuperroute = new JCheckBox(I18n.tr("Convert this relation to superroute"));
        final JCheckBox cbProposed = new JCheckBox(I18n.tr("subrelation is proposed (wenslijn)"));
        final JCheckBox cbDeviation = new JCheckBox(I18n.tr("subrelation is deviation (omleiding)"));
        final JCheckBox cbFindAllSegmentsAutomatically = new JCheckBox(I18n.tr("Process complete route relation for segments and extract into multiple sub route relations"));
        final JTextField tfNameTag = new JTextField(getEditor().getRelation().get("name"));
        ArrayList<Object> paramsList = new ArrayList<>();
        paramsList.add(question);
        paramsList.add(tfNameTag);
        // Only show cbReplaceInSuperrouteRelations when it's relevant
        if (numberOfParentRelations > 0) {
            // check the check box
            cbReplaceInSuperrouteRelations.setSelected(true);
            paramsList.add(cbReplaceInSuperrouteRelations);
        }
        if (originalRelation.hasTag("cycle_network", "cycle_highway")) {
            paramsList.add(cbDeviation);
            paramsList.add(cbProposed);
        }
        paramsList.add(cbConvertToSuperroute);
        if (RouteUtils.isVersionTwoPTRoute(originalRelation)) {
            cbConvertToSuperroute.setSelected(true);
            cbFindAllSegmentsAutomatically.setSelected(true);
            paramsList.add(cbFindAllSegmentsAutomatically);
        }
        paramsList.add(cbConvertToSuperroute);
        Object[] params = paramsList.toArray(new Object[0]); // paramsList.size()]);
        if (
            JOptionPane.showConfirmDialog(
                getMemberTable(),
                params,
                title,
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.QUESTION_MESSAGE
            ) == JOptionPane.OK_OPTION
        ) {
            if (!cbFindAllSegmentsAutomatically.isSelected()) {
                final Relation clonedRelation = new Relation(originalRelation);
                boolean substituteWaysWithRelation = !cbReplaceInSuperrouteRelations.isSelected();
                List<Integer> selectedIndices = Arrays.stream(memberTableModel.getSelectedIndices())
                    .boxed().collect(Collectors.toList());
                final Relation extractedRelation = extractMembersForIndicesAndSubstitute(
                    clonedRelation, selectedIndices, substituteWaysWithRelation);
                if (extractedRelation != null) {
                    if (extractedRelation.getId() <= 0) {
                        if (cbConvertToSuperroute.isSelected()) {
                            clonedRelation.put("type", "superroute");
                        }
                        extractedRelation.put("name", tfNameTag.getText());
                        if (cbProposed.isSelected()) {
                            extractedRelation.put("state", "proposed");
                            extractedRelation.put("name", tfNameTag.getText() + " (wenslijn)");
                        }
                        if (cbDeviation.isSelected()) {
                            extractedRelation.put("name", tfNameTag.getText() + " (omleiding)");
                        }
                        DataSet activeDataSet = getLayerManager().getActiveDataSet();
                        commands.add(new AddCommand(activeDataSet, extractedRelation));
                    }
                    commands.add(new ChangeCommand(originalRelation, clonedRelation));
                    addExtractedRelationToParentSuperrouteRelations(originalRelation, commands, parentRelations, selectedIndices, extractedRelation);
                    UndoRedoHandler.getInstance().add(new SequenceCommand(I18n.tr("Extract ways to relation"), commands));
                    RelationEditor extraEditor = RelationEditor.getEditor(getEditor().getLayer(), extractedRelation, emptyList());
                    extraEditor.setVisible(true);
                    extraEditor.setAlwaysOnTop(true);

                    try {
                        editor.reloadDataFromRelation();
                        // todo this often gives index out of bounds exception
                        // I can't figure out why.
                        // catching it here causes the exception to occur when
                        // the user presses the update button, so not really a solution
                        // The update button appears if I remove 1 or 2 members before extracting
                        // so when the relation editor is 'dirty'.
                        // using memberTableModel.applyToRelation(originalRelation);
                        // at the beginning doesn't seem to actually save the relation
                    } catch (Exception e) {
                        Logging.error(e);
                    }
                }
            } else {
                splitInSegments(originalRelation);
                editor.reloadDataFromRelation();
            }
        }
    }

    public void splitInSegments(Relation originalRelation) {
        final Relation clonedRelation = new Relation(originalRelation);
        final long clonedRelationId = clonedRelation.getId();
        final List<RelationMember> members = clonedRelation.getMembers();
        List<PTSegmentToExtract> segments = new ArrayList<>();
        RelationMember previousWayMember = null;
        RelationMember currentWayMember = null;
        RelationMember nextWayMember = null;
        boolean startNewSegment = false;
        PTSegmentToExtract segment = null;
        for (int i = members.size()-1; i>=0; i--) {
            RelationMember rm = members.get(i);
            if (RouteUtils.isPTWay(rm)) {
                previousWayMember = rm;
                if (currentWayMember == null) {
                    currentWayMember = previousWayMember;
                    continue;
                } else if (nextWayMember == null) {
                    nextWayMember = currentWayMember;
                    currentWayMember = previousWayMember;
                    segment = new PTSegmentToExtract(clonedRelation);
                    continue;
                }
                // now previousWayMember, currentWayMember and nextWayMember all have values
                // how to decide whether currentWayMember is in the same segment as nextWayMember?
                // it is if for all the parent route relations of currentWayMember
                // that go in the same direction, the nextWayMember is the same as the nextWayMember of this route
                // to exclude route that go in the opposite direction it's necessary to compare
                // previousWayMember with their nextWayMember and nextWayMember with their previousWayMember
                final Way previousWay = previousWayMember.getWay();
                final Way currentWay = currentWayMember.getWay();
                final Way nextWay = nextWayMember.getWay();
                List<Relation> parentRouteRelations = new ArrayList<>(Utils.filteredCollection(currentWay.getReferrers(), Relation.class));
                if (segment != null) {
                    segment.addPTWay(nextWayMember, i+2,
                                     clonedRelation.get("ref"), clonedRelation.get("colour"));
                }
                if (startNewSegment) {
                    segment = createNewSegment(clonedRelation, segments, segment);
                    startNewSegment = false;
                }
                String previousWayName = previousWay.get("name");
                String currentWayName = currentWay.get("name");
                String nextWayName = nextWay.get("name");
                final long previousWayId = previousWay.getId();
                for (Relation parentRoute : parentRouteRelations) {
                    if (parentRoute.getId() != clonedRelationId && RouteUtils.isVersionTwoPTRoute(parentRoute)) {
                        List<Pair<Way, Way>> prevAndNext = findPreviousAndNextWayInRoute(parentRoute.getMembers(), currentWay);
                        Way previousWayInParentRoute = null;
                        Way nextWayInParentRoute = null;
                        for (int k = 0; k < prevAndNext.size(); k++) {
                            Pair<Way, Way> wayPair = prevAndNext.get(k);
                            previousWayInParentRoute = wayPair.a;
                            nextWayInParentRoute = wayPair.b;
                            /*
                             We are not interested in route relations that describe vehicles
                             traveling in the opposite direction
                            */
                            long previousWayInParentRouteId = 0;
                            if (previousWayInParentRoute != null) {
                                String previousWayInParentName = previousWayInParentRoute.get("name");
                                previousWayInParentRouteId = previousWayInParentRoute.getId();
                            }
                            if (nextWayInParentRoute != null) {
                                String nextWayInParentName = nextWayInParentRoute.get("name");
                            }
//                                if (currentWayName == "006 Burchtstraat") {  }
                            if (isItineraryInSameDirection(nextWay, nextWayInParentRoute, previousWayId, previousWayInParentRouteId)) {
                               if (!startNewSegment &&
                                    previousWayInParentRouteId != 0 && previousWayId != previousWayInParentRouteId) {
                                    // if one of the parent relations has a different previous way
                                    // it's time to start a new segment
                                    startNewSegment = true;
                                }
                                // If the next way after the previous way's parent
                                // routes isn't the same as currentWay
                                // a split is also needed
                                if (!startNewSegment) {
                                    startNewSegment = isNextWayOfPreviousWayDifferentFromCurrentWayInAtLeastOneOfTheParentsOfPreviousWay(previousWay, currentWay);
                                }
                            }
                            if (startNewSegment == true) { break; }
                        }
                        segment.addLineIdentifier(parentRoute.get("ref"));
                        segment.addColour(parentRoute.get("colour"));
                    }
                }
            } else {
                if (isPTStop(rm)) {
                    // This only works if the presumption of stops being added
                    // before way members in the route relation is true
                    assert currentWayMember != null;
                    if (RouteUtils.isPTWay(currentWayMember)) {
                        assert nextWayMember != null;
                        if (RouteUtils.isPTWay(nextWayMember)) {
                            createNewSegment(clonedRelation, segments, segment);
                            break;
                        }
                    }
                }
            }
            nextWayMember = currentWayMember;
            currentWayMember = previousWayMember;
        }
        UndoRedoHandler.getInstance().add(new ChangeCommand(originalRelation, clonedRelation));
    }

    public PTSegmentToExtract createNewSegment(Relation clonedRelation, List<PTSegmentToExtract> segments, PTSegmentToExtract segment) {
        segments.add(segment);
        ArrayList<String> tagsToKeep = new ArrayList<>();
        for (String s : Arrays.asList("type", "route", "public_transport:version")) {
            tagsToKeep.add(s);
        }
        Relation extractedRelation = segment.extractToRelation(tagsToKeep, true);

        segment = new PTSegmentToExtract(clonedRelation);
        return segment;
    }

    public boolean isNextWayOfPreviousWayDifferentFromCurrentWayInAtLeastOneOfTheParentsOfPreviousWay(Way previousWay, Way currentWay) {
        List<Relation> parentRoutesOfPreviousWay = new ArrayList<>(Utils.filteredCollection(previousWay.getReferrers(), Relation.class));
        for (Relation parentRouteOfPreviousWay : parentRoutesOfPreviousWay) {
            if (RouteUtils.isVersionTwoPTRoute(parentRouteOfPreviousWay)) {
                List<Pair<Way, Way>> prevAndNextParent = findPreviousAndNextWayInRoute(parentRouteOfPreviousWay.getMembers(), previousWay);
                Way nextWayInParentRouteOfPreviousWay = null;
                if (prevAndNextParent.size() > 0) {
                    // todo what if the way occurs more than once in the parent
                    // route of the previous way?
                    nextWayInParentRouteOfPreviousWay = prevAndNextParent.get(0).b;
                    if (nextWayInParentRouteOfPreviousWay != null) {
                        String nextWayInParentRouteOfPreviousWayName = nextWayInParentRouteOfPreviousWay.get("name");
                    }
                }
                if (nextWayInParentRouteOfPreviousWay != null
                    && nextWayInParentRouteOfPreviousWay.getId() != currentWay.getId()) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean isItineraryInSameDirection(Way nextWayMember, Way nextWayInParentRoute, long previousWayId, long previousWayInParentRouteId) {
        return !(nextWayInParentRoute != null && previousWayId == nextWayInParentRoute.getId() ||
            previousWayInParentRouteId != 0 && nextWayMember.getId() == previousWayInParentRouteId);
    }

    /**
     * for all occurences of wayToLocate this method returns the way before it and the way after it
     * @param members          The members list of the relation
     * @param wayToLocate      The way to locate in the list
     * @return a list of way pairs
     */
    private List<Pair<Way,Way>> findPreviousAndNextWayInRoute(List<RelationMember> members, Way wayToLocate) {
        final long wayToLocateId = wayToLocate.getId();
        Way previousWay;
        Way nextWay = null;
        boolean foundWay = false;
        List<Pair<Way,Way>> wayPairs = new ArrayList<>();
        for (int j = members.size() - 1; j>=0 ; j--) {
            RelationMember rm = members.get(j);
            if (rm.isWay() && RouteUtils.isPTWay(rm)) {
                previousWay = rm.getWay();
                if (foundWay) {
                    wayPairs.add(0, new Pair<>(previousWay,nextWay));
                    nextWay = null;
                    foundWay = false;
                    continue;
                }
                if (previousWay.getId() == wayToLocateId) {
                    foundWay = true;
                } else {
                    nextWay = previousWay;
                }
            }
        }
        return wayPairs;
    }
    /** This method modifies clonedRelation in place if substituteWaysWithRelation is true
     *  and if ways are extracted into a new relation
     *  It takes a list of (preferably) consecutive indices
     *  If there is already a relation with the same members in the same order
     *  it returns a pointer to that relation
     *  otherwise it returns a new relation with a negative id,
     *  which still needs to be added using addCommand()*/

    public Relation extractMembersForIndicesAndSubstitute(Relation clonedRelation,
                                                          List<Integer> selectedIndices,
                                                          boolean substituteWaysWithRelation) {
        Relation extractedRelation = new Relation();
        extractedRelation.setKeys(clonedRelation.getKeys());
        extractedRelation.put("type", "route");

        int index = 0;
        boolean atLeast1MemberAddedToExtractedRelation = false;
        for (int i = selectedIndices.size() - 1; i >= 0; i--) {
            atLeast1MemberAddedToExtractedRelation = true;
            index = selectedIndices.get(i);
            RelationMember relationMember = clonedRelation.removeMember(index);
            extractedRelation.addMember(0, relationMember);
        }

        if (atLeast1MemberAddedToExtractedRelation) {
            if (substituteWaysWithRelation) {
                // replace removed members with the extracted relation
                index = PTSegmentToExtract.limitIntegerTo(index, clonedRelation.getMembersCount());
                clonedRelation.addMember(index, new RelationMember("", extractedRelation));
                }
            } else {
                return null;
            }
        return extractedRelation;
    }

    private void addExtractedRelationToParentSuperrouteRelations(Relation originalRelation, List<Command> commands, List<Relation> parentRelations, List<Integer> selectedIndices, Relation extractedRelation) {
        for (Relation superroute : parentRelations) {
            int index = 0;
            for (RelationMember member : superroute.getMembers()) {
                if (member.getMember().getId() == originalRelation.getId()) {
                    Relation clonedParentRelation = new Relation(superroute);
                    if (selectedIndices.get(0) != 0) {
                        // if the user selected a block that didn't start with
                        // the first member, add the extracted relation after
                        // the position where this relation was in the parent
                        index++;
                    }
                    index = PTSegmentToExtract.limitIntegerTo(index, superroute.getMembersCount());
                    clonedParentRelation.addMember(index, new RelationMember("", extractedRelation));
                    commands.add(new ChangeCommand(superroute, clonedParentRelation));
                    break;
                }
                index++;
            }
        }
    }

    @Override
    public boolean isExpertOnly() {
        return true;
    }

    @Override
    protected void updateEnabledState() {
        final boolean newEnabledState = !editorAccess.getSelectionTableModel().getSelection().isEmpty()
            && Optional.ofNullable(editorAccess.getTagModel().get("type")).filter(it -> it.getValue().matches("route|superroute")).isPresent();
            // && !editorAccess.getEditor().isDirtyRelation(); // This seems to cause a problem

        if (newEnabledState) {
            putValue(SHORT_DESCRIPTION, I18n.tr("Extract selected part of the route into new relation"));
        } else {
            putValue(SHORT_DESCRIPTION, I18n.tr("Extract into new relation (needs type=route/superroute tag and at least one selected relation member)"));
        }
        setEnabled(newEnabledState);
    }
}
