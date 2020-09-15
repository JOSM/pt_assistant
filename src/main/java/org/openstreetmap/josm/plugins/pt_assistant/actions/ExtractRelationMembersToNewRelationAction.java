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
import org.openstreetmap.josm.plugins.pt_assistant.utils.RouteUtils;
import org.openstreetmap.josm.tools.*;

import static java.util.Collections.*;
import static org.openstreetmap.josm.gui.MainApplication.*;

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
        Object[] params = paramsList.toArray(new Object[paramsList.size()]);
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
                final Relation clonedRelation = new Relation(originalRelation);
                DataSet activeDataSet = getLayerManager().getActiveDataSet();
                String previousLineIdentifiersSignature = "";
                List<RelationMember> members = clonedRelation.getMembers();
                List<String> streetNamesList = new ArrayList<>();
                List<Integer> selectedIndices = new ArrayList<>();
                Way nextWay = new Way();
                boolean startNewSegment = false;
                boolean firstWayWasReached = false;
                for (int i = members.size()-1; i>=0; i--) {
                    RelationMember rm = members.get(i);
                    selectedIndices.add(0, i);
                    if (RouteUtils.isPTWay(rm)) {
                        final Way rmWay = rm.getWay();
                        List<Relation> parentRouteRelations = new ArrayList<>(Utils.filteredCollection(rmWay.getReferrers(), Relation.class));
                        List<String> lineIdentifiersList = new ArrayList<>();
                        String ref = clonedRelation.get("ref");
                        if (ref != null) {
                            lineIdentifiersList.add(ref);
                        }
                        startNewSegment = false;
                        if (i==0) {
                            // make sure we process the last segment
                            // since we're working backwards, it's actually the first segment
                            startNewSegment = true;
                        } else {
                            // also make sure to reset startNewSegment flag

                        }
                        for (Relation pr : parentRouteRelations) {
                            if (RouteUtils.isVersionTwoPTRoute(pr) && pr.getId() != clonedRelation.getId()) {
                                // todo code is needed to detect way sequences that are in the route relation twice
                                List<Pair<Way, Way>> prevAndNext = findPreviousAndNextWayInRoute(pr.getMembers(), rmWay);
                                Way previousWayInParentRoute = null;
                                Way nextWayInParentRoute = null;
                                for (int j=prevAndNext.size()-1; j>0; j--) {
//                                    if (j<prevAndNext.size()-1) {
//                                        break;
//                                    }
                                    Pair<Way, Way> pan = prevAndNext.get(j);
                                    previousWayInParentRoute = pan.a;
                                    nextWayInParentRoute = pan.b;

                                    /*
                                     We are not interested in route relations that describe vehicles
                                     traveling in the opposite direction
                                    */
                                    Way previousWay = findPreviousWay(members, i);
                                    String currentWayName = rmWay.get("name");
                                    String previousWayName = previousWay.get("name");
                                    String previousWayInParentName = previousWayInParentRoute.get("name");
                                    if (nextWayInParentRoute != null) {
                                        String nextWayInParentName = nextWayInParentRoute.get("name");
                                    }
                                    String nextWayName = nextWay.get("name");
                                    if (currentWayName == "006 Burchtstraat") {
                                        currentWayName = rmWay.get("name");
                                    }
                                    final long previousWayId = previousWay.getId();
                                    if (nextWayInParentRoute != null &&
                                        (previousWayId == nextWayInParentRoute.getId() ||
                                            nextWay.getId() == previousWayInParentRoute.getId())) {
                                        // this parent relation describes an itinerary in the opposite direction
                                        // so we don't need it
                                    } else {
                                        if (!startNewSegment && previousWayId != previousWayInParentRoute.getId()) {
                                            // if one of the parent relations has a different previous way
                                            // it's time to start a new segment
                                            startNewSegment = true;
                                        }
                                        // If the next way after the previous way's in one of the parent
                                        // routes isn't the same as the way we're looking at right now
                                        // a split is also needed
                                        if (!startNewSegment) {
                                            // no need to check for this if a new segment will be created anyway already
                                            List<Relation> parentRoutesOfPreviousWay = new ArrayList<>(Utils.filteredCollection(previousWay.getReferrers(), Relation.class));
                                            for (Relation prOfPreviousWay : parentRoutesOfPreviousWay) {
                                                if (RouteUtils.isVersionTwoPTRoute(prOfPreviousWay)) {
                                                    List<Pair<Way, Way>> prevAndNextParent = findPreviousAndNextWayInRoute(prOfPreviousWay.getMembers(), previousWay);
                                                    Way nextWayInParentRouteOfPreviousWay = null;
                                                    for (int k = prevAndNextParent.size()-1; k>0; k--) {
//                                                        if (k<prevAndNextParent.size()-1) {
//                                                            break;
//                                                        }
                                                        Pair<Way, Way> panParent = prevAndNext.get(k);
                                                        nextWayInParentRouteOfPreviousWay = panParent.b;
                                                        if (nextWayInParentRouteOfPreviousWay != null
                                                            && nextWayInParentRouteOfPreviousWay != rmWay) {
                                                            startNewSegment = true;
                                                            break;
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                        String lineIdentifier = pr.get("ref");
                                        if (lineIdentifier != null && !lineIdentifiersList.contains(lineIdentifier)) {
                                            lineIdentifiersList.add(lineIdentifier);
                                        }
                                    }
                                }
                            }
                        }
                        sort(lineIdentifiersList);
                        String lineIdentifiersSignature = String.join(";", lineIdentifiersList);

                        String streetName = rmWay.get("name");
                        if (streetName == null) {
                            streetName = rmWay.get("ref");
                        }
                        addStreetName(streetNamesList, streetName);
                        if (startNewSegment) {
                            ArrayList<String> allButLastStreetNames = new ArrayList<>();
                            for (int j = 0; j < streetNamesList.size()-1; j++) {
                                String element = streetNamesList.get(j);
                                allButLastStreetNames.add(element);
                            }
                            String streetNamesSignature = String.join(";", allButLastStreetNames);
                            Relation extractedRelation = extractMembersForIndicesAndSubstitute(
                                clonedRelation, selectedIndices, true);
                            if (extractedRelation != null && extractedRelation.getId() <= 0) {
                                updateTags(lineIdentifiersSignature, allButLastStreetNames, streetNamesSignature, extractedRelation);
                                commands.add(new AddCommand(activeDataSet, extractedRelation));
                            }
                            String lastStreetName = "";
                            if (streetNamesList.size()>0) {
                                lastStreetName = streetNamesList.get(0); // streetNamesList.size() - 1);
                            }
                            streetNamesList = new ArrayList<>();
                            if (!"".equals(lastStreetName)) {
                                streetNamesList.add(lastStreetName);
                            }
                            selectedIndices = new ArrayList<>();
                        }
                        nextWay = rmWay;
                        if (firstWayWasReached == true) {
                            break;
                        }
                    } else {
                        firstWayWasReached = true;
                    }
                }
                commands.add(new ChangeCommand(originalRelation, clonedRelation));
                UndoRedoHandler.getInstance().add(new SequenceCommand(I18n.tr("Extract ways to relation"), commands));
                editor.reloadDataFromRelation();
            }
        }
    }

    private Way findPreviousWay(List<RelationMember> members, int i) {
        if (i < 1) {
            return new Way();
        }
        final RelationMember potentialWayMember = members.get(i - 1);
        if (potentialWayMember.isWay()) {
            return potentialWayMember.getWay();
        } else {
            return new Way();
        }
//        Way previousWay;
//        if (i < members.size() - 1 && members.get(i + 1).isWay()) {
//            previousWay = members.get(i + 1).getWay();
//        } else {
//            previousWay = new Way();
//        }
//        return previousWay;
    }

    private void addStreetName(List<String> streetNamesList, String streetName) {
        if (streetName != null && !streetNamesList.contains(streetName)) {
            streetNamesList.add(0, streetName);
        }
    }

    private void updateTags(String previousLineIdentifiersSignature, List<String> streetNamesList, String streetNamesSignature, Relation extractedRelation) {
        if (!streetNamesSignature.isEmpty()) {
            extractedRelation.put("street_names", streetNamesSignature);
        }
        String fromToStreet = "";
        if (streetNamesList.size() > 0) {
            fromToStreet = streetNamesList.get(0).concat(" ");
            if (streetNamesList.size() > 1) {
                fromToStreet = String.format("%s- %s ", fromToStreet, streetNamesList.get(streetNamesList.size() - 1));
            }
        }
        String name = String.format("%s(%s)", fromToStreet, previousLineIdentifiersSignature);
        extractedRelation.put("name", name);
        extractedRelation.put("route_ref", previousLineIdentifiersSignature);
        // todo make a list of the ones we want to keep and remove the others
        extractedRelation.remove("bus");
        extractedRelation.remove("colour");
        extractedRelation.remove("from");
        extractedRelation.remove("network");
        extractedRelation.remove("network:wikidata");
        extractedRelation.remove("operator");
        extractedRelation.remove("operator:wikidata");
        extractedRelation.remove("ref");
        extractedRelation.remove("to");
        extractedRelation.remove("via");
        extractedRelation.remove("wikidata");
    }

    private List<Pair<Way,Way>> findPreviousAndNextWayInRoute(List<RelationMember> members, Way wayToLocate) {
        Way previousWay;
        Way nextWay = null;
        boolean foundWay = false;
        List<Pair<Way,Way>> pairsList = new ArrayList<>();
        for (int j = members.size() - 1; j >= 0; j--) {
            RelationMember rm = members.get(j);
            if (rm.isWay()) {
                previousWay = rm.getWay();
                if (foundWay) {
                    pairsList.add(new Pair(previousWay,nextWay));
                    nextWay = null;
                    foundWay = false;
                    continue;
                }
                if (previousWay.getId() == wayToLocate.getId()) {
                    foundWay = true;
                } else {
                    nextWay = previousWay;
                }
            }
        }
        return pairsList;
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
        List<Pair<Integer, Relation>> parentRouteRelationsCandidates = new ArrayList<>();
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
            // check if a similar sub route relation already exists
//            if (relationMember.getType().equals(OsmPrimitiveType.WAY)) {
//                final Way rmWay = relationMember.getWay();
//                for (Pair<Integer, Relation> candidateForRemoval : parentRouteRelationsCandidates) {
//                    if (candidateForRemoval.a > 1) {
//                        final RelationMember wayBefore = candidateForRemoval.b.getMember(candidateForRemoval.a - 1);
//                        parentRouteRelationsCandidates.remove(candidateForRemoval);
//                        if (wayBefore.isWay() && wayBefore.getWay().equals(rmWay)) {
//                            Pair<Integer, Relation> candidate = Pair.create(candidateForRemoval.a -1, candidateForRemoval.b);
//                            parentRouteRelationsCandidates.add(candidate);
//                        }
//                    }
//                }
//                for (Relation wayParent : Utils.filteredCollection(rmWay.getReferrers(), Relation.class)) {
//                    if (!wayParent.equals(clonedRelation)) {
//                        final int indexInParent = wayParent.getMembersCount() - 1;
//                        final RelationMember wayParentMember = wayParent.getMember(indexInParent);
//                        if (wayParentMember.isWay()) {
//                            Way lastWayOfParent = wayParentMember.getWay();
//                            if (rmWay.equals(lastWayOfParent)) {
//                                Pair<Integer, Relation> candidate = Pair.create(indexInParent, wayParent);
//                                parentRouteRelationsCandidates.add(candidate);
//                                break;
//                            }
//                        }
//                    }
//                }
//            }
        }
        int membersCount = extractedRelation.getMembersCount();
        for (Pair<Integer, Relation> candidate : parentRouteRelationsCandidates) {
            if (membersCount == candidate.b.getMembersCount()) {
                extractedRelation = candidate.b;
            }
        }

        if (atLeast1MemberAddedToExtractedRelation) {
            if (substituteWaysWithRelation) {
                // replace removed members with the extracted relation
                index = limitIntegerTo(index, clonedRelation.getMembersCount());
                clonedRelation.addMember(index, new RelationMember("", extractedRelation));
                }
            } else {
                return null;
            }
        return extractedRelation;
    }

    private int limitIntegerTo(int index, int limit) {
        if (index > limit) {
            index = limit;
        }
        return index;
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
                    index = limitIntegerTo(index, superroute.getMembersCount());
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
