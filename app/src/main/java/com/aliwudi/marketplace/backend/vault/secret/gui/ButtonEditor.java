/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.aliwudi.marketplace.backend.vault.secret.gui;

import java.awt.Component;
import java.util.Map;
import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import org.springframework.vault.core.VaultTemplate;

/**
 *
 * @author user
 */
class ButtonEditor extends DefaultCellEditor {
    private JButton button;
    private String backend, context, key;
    private boolean clicked;
    private VaultTemplate vaultTemplate;

    public ButtonEditor(JCheckBox checkBox, VaultTemplate vaultTemplate) {
        super(checkBox);
        this.vaultTemplate = vaultTemplate;
        button = new JButton();
        button.setOpaque(true);
        button.addActionListener(e -> fireEditingStopped());
    }

    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
        backend = (String) table.getValueAt(row, 0);
        context = (String) table.getValueAt(row, 1);
        key = (String) table.getValueAt(row, 2);
        button.setText((value == null) ? "" : value.toString());
        clicked = true;
        return button;
    }

    public Object getCellEditorValue() {
        if (clicked) {
            try {
                String path = backend + "/data/" + context + "/" + key;
                Map<String, Object> read = vaultTemplate.read(path).getData();
                Map<String, Object> data = (Map<String, Object>) read.get("data");
                String realValue = (String) data.get(key);
                JOptionPane.showMessageDialog(button, "Value: " + realValue);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(button, "Unable to fetch secret.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
        clicked = false;
        return button.getText();
    }

    public boolean stopCellEditing() {
        clicked = false;
        return super.stopCellEditing();
    }
}

