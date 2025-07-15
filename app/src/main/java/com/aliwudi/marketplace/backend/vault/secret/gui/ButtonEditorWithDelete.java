package com.aliwudi.marketplace.backend.vault.secret.gui;

import org.springframework.vault.core.VaultTemplate;

import javax.swing.*;
import javax.swing.table.TableCellEditor;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.nio.file.*;
import java.util.Base64;
import javax.swing.table.DefaultTableModel;

public class ButtonEditorWithDelete extends DefaultCellEditor {
    private JButton button;
    private String label;
    private boolean clicked;
    private JTable table;
    private VaultTemplate vaultTemplate;

    public ButtonEditorWithDelete(JCheckBox checkBox, VaultTemplate vaultTemplate) {
        super(checkBox);
        this.vaultTemplate = vaultTemplate;
        button = new JButton();
        button.setOpaque(true);
        button.addActionListener(e -> fireEditingStopped());
    }

    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
        this.table = table;
        this.label = (value == null) ? "" : value.toString();
        button.setText(label);
        clicked = true;
        return button;
    }

    public Object getCellEditorValue() {
        if (clicked && table != null) {
            int selectedRow = table.getSelectedRow();
            if (selectedRow >= 0) {
                String backend = table.getValueAt(selectedRow, 0).toString();
                String context = table.getValueAt(selectedRow, 1).toString();
                String key = table.getValueAt(selectedRow, 2).toString();

                String[] options = {"View", "Delete", "Cancel"};
                int choice = JOptionPane.showOptionDialog(button,
                        "What do you want to do with this secret?",
                        "Secret Action",
                        JOptionPane.DEFAULT_OPTION,
                        JOptionPane.QUESTION_MESSAGE,
                        null,
                        options,
                        options[0]);

                if (choice == 0) { // View
                    String value = readSecretFromHistory(backend, context, key);
                    JOptionPane.showMessageDialog(button, "Secret value: " + value, "Secret", JOptionPane.INFORMATION_MESSAGE);
                } else if (choice == 1) { // Delete
                    int confirm = JOptionPane.showConfirmDialog(button, "Are you sure you want to delete this secret?", "Confirm Delete", JOptionPane.YES_NO_OPTION);
                    if (confirm == JOptionPane.YES_OPTION) {
                        String path = backend + "/data/" + context + "/" + key;
                        try {
                            vaultTemplate.delete(path);
                            deleteSecretFromHistory(backend, context, key);
                            ((DefaultTableModel) table.getModel()).removeRow(selectedRow);
                            JOptionPane.showMessageDialog(button, "Secret deleted successfully.", "Deleted", JOptionPane.INFORMATION_MESSAGE);
                        } catch (Exception e) {
                            JOptionPane.showMessageDialog(button, "Failed to delete secret: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                        }
                    }
                }
            }
        }
        clicked = false;
        return label;
    }

    private void deleteSecretFromHistory(String backend, String context, String key) {
        Path path = Paths.get("secrets_history.dat");
        if (!Files.exists(path)) return;

        String compoundKey = backend + "::" + context + "::" + key;
        try {
            java.util.List<String> all = Files.readAllLines(path);
            all.removeIf(line -> line.startsWith(compoundKey + "::"));
            Files.write(path, all);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String readSecretFromHistory(String backend, String context, String key) {
        Path path = Paths.get("secrets_history.dat");
        if (!Files.exists(path)) return "[Secret not found]";
        String compoundKey = backend + "::" + context + "::" + key;
        try {
            java.util.List<String> lines = Files.readAllLines(path);
            for (String line : lines) {
                if (line.startsWith(compoundKey + "::")) {
                    String[] parts = line.split("::", 4);
                    if (parts.length == 4) {
                        return decrypt(parts[3]);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "[Secret not found]";
    }

    // Same XOR encryption method used in VaultGUIApp
    private String decrypt(String input) {
        String ENCRYPTION_KEY = "change_this_key"; // match VaultGUIApp
        byte[] decoded = Base64.getDecoder().decode(input);
        char[] key = ENCRYPTION_KEY.toCharArray();
        char[] inChars = new String(decoded).toCharArray();
        for (int i = 0; i < inChars.length; i++) {
            inChars[i] ^= key[i % key.length];
        }
        return new String(inChars);
    }

    public boolean stopCellEditing() {
        clicked = false;
        return super.stopCellEditing();
    }
}
