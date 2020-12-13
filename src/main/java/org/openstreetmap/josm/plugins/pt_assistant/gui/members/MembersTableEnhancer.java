package org.openstreetmap.josm.plugins.pt_assistant.gui.members;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.Objects;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

import org.openstreetmap.josm.gui.dialogs.relation.MemberTableColumnModel;
import org.openstreetmap.josm.gui.dialogs.relation.actions.IRelationEditorActionAccess;
import org.openstreetmap.josm.plugins.customizepublictransportstop.OSMTags;
import org.openstreetmap.josm.plugins.pt_assistant.gui.linear.RelationAccess;
import org.openstreetmap.josm.tools.ImageProvider;

public class MembersTableEnhancer {
    private final IRelationEditorActionAccess editorAccess;

    private RelationMemberValidator validator;

    public MembersTableEnhancer(IRelationEditorActionAccess editorAccess) {
        this.editorAccess = Objects.requireNonNull(editorAccess, "editorAccess");

        updateValidator();

        // Role column
        enhanceRoleColumn();

        // Primitive column
        enhancePrimitiveColumn();

        // event listeners
        editorAccess.getTagModel().addTableModelListener(l -> {
            updateValidator();
        });
        editorAccess.getMemberTableModel().addTableModelListener(l -> {
            updateValidator();
        });
    }

    private void enhancePrimitiveColumn() {
        JLabel primitiveLabel = new JLabel();
        primitiveLabel.setOpaque(false);
        enhanceTableRenderer(getTableColumn(1), (table, row, column) -> {
            String text = validator.getPrimitiveText(row, editorAccess.getMemberTableModel().getValue(row));
            if (text != null && !text.isEmpty()) {
                primitiveLabel.setText(text);
                return primitiveLabel;
            } else {
                return null;
            }
        });
    }

    private TableColumn getTableColumn(int columnIndex) {
        MemberTableColumnModel model = (MemberTableColumnModel) editorAccess.getMemberTable().getColumnModel();
        return model.getColumn(columnIndex);
    }

    private void enhanceRoleColumn() {
        TableColumn tableColumn = getTableColumn(0);
        JLabel icon = new JLabel();
        icon.setOpaque(false);
        enhanceTableRenderer(tableColumn, (table, row, column) -> {
            RoleValidationResult result = validator.validateAndSuggest(row, editorAccess.getMemberTableModel().getValue(row));
            if (!result.isValid()) {
                Dimension cellSize = table.getCellRect(row, column, false).getSize();
                int size = Math.min(cellSize.width, cellSize.height);
                icon.setIcon(new ImageProvider("warning-small").setSize(new Dimension(size, size)).get());
                icon.setToolTipText(tr("Expected this role to be: " + result.getCorrectedRole()));
                return icon;
            } else {
                return null;
            }
        });
    }

    private void updateValidator() {
        RelationAccess relation = RelationAccess.of(editorAccess);
        if (relation.hasTag(OSMTags.KEY_RELATION_TYPE, OSMTags.KEY_ROUTE)) {
            validator = new RouteRelationMemberValidator(relation);
        } else {
            validator = new PresetRelationMemberValidator(relation);
        }
        // Triggers a repaint
        editorAccess.getMemberTable().invalidate();
    }

    private void enhanceTableRenderer(TableColumn tableColumn, EastComponentSupplier eastComponent) {
        TableCellRenderer parent = tableColumn.getCellRenderer();
        JPanel panel = new JPanel(new BorderLayout());
        tableColumn.setCellRenderer((table, value, isSelected, hasFocus, row, column) -> {
            panel.removeAll();

            panel.add(parent.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column));

            JComponent e = eastComponent.get(table, row, column);
            if (e != null) {
                panel.add(e, BorderLayout.EAST);
            }
            return panel;
        });
    }

    interface EastComponentSupplier {
        JComponent get(JTable table, int row, int column);
    }
}
