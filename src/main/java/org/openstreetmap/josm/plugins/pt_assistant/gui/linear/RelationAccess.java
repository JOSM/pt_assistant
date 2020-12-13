package org.openstreetmap.josm.plugins.pt_assistant.gui.linear;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.gui.dialogs.relation.actions.IRelationEditorActionAccess;
import org.openstreetmap.josm.gui.tagging.TagModel;

/**
 * Allows reading from an existing relation or the relation editor.
 */
public interface RelationAccess {

    Map<String, String> getTags();

    default String get(String key) {
        return getTags().get(key);
    }

    List<RelationMember> getMembers();

    static RelationAccess of(Relation relation) {
        return new RelationAccess() {
            @Override
            public Map<String, String> getTags() {
                return relation.getKeys();
            }

            @Override
            public List<RelationMember> getMembers() {
                return relation.getMembers();
            }

            @Override
            public Relation getRelation() {
                return relation;
            }

            @Override
            public long getRelationId() {
                return relation.getId();
            }
        };
    }
    static RelationAccess of(IRelationEditorActionAccess editor) {
        return new RelationAccess() {
            @Override
            public String get(String key) {
                TagModel tagModel = editor.getTagModel().get(key);
                return tagModel == null ? null : tagModel.getValue();
            }

            @Override
            public Map<String, String> getTags() {
                return editor.getTagModel().getTags();
            }

            @Override
            public List<RelationMember> getMembers() {
                return RelationEditorAccessUtils.streamMembers(editor)
                    .collect(Collectors.toList());
            }

            @Override
            public long getRelationId() {
                return editor.getEditor().getRelation() != null ? editor.getEditor().getRelation().getId() : -1;
            }
        };
    }

    default Relation getRelation() {
        return null;
    }

    default long getRelationId() {
        return -1;
    }

    default boolean hasTag(String key, String value) {
        return value.equals(get(key));
    }

    default boolean isMultipolygon() {
        return hasTag("type", "multipolygon");
    }
}
