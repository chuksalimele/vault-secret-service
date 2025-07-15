package com.aliwudi.marketplace.backend.vault.secret.gui;

import com.aliwudi.marketplace.backend.vault.secret.service.VaultSecretService;
import com.formdev.flatlaf.FlatLightLaf;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.vault.core.VaultTemplate;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import org.springframework.boot.ApplicationRunner;

@SpringBootApplication(scanBasePackages = "com.aliwudi.marketplace.backend.vault.secret.service")
public class VaultGUIApp {

    private static final String LOCAL_HISTORY_FILE = "secrets_history.dat";
    private static final String ENCRYPTION_KEY = "change_this_key"; // For demo purposes only

    private static DefaultTableModel tableModel;

    public static void main(String[] args) {
        System.setProperty("java.awt.headless", "false");
        SwingUtilities.invokeLater(FlatLightLaf::setup);
        SpringApplication.run(VaultGUIApp.class, args);
    }

    @Bean
    public ApplicationRunner applicationRunner(VaultTemplate vaultTemplate) {
        return args -> SwingUtilities.invokeLater(() -> createAndShowGUI(vaultTemplate));
    }

    private void createAndShowGUI(VaultTemplate vaultTemplate) {
        JFrame frame = new JFrame("Vault Secret Manager");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 600);
        frame.setLocationRelativeTo(null);

        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel inputPanel = new JPanel(new GridBagLayout());
        inputPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Secret Entry", TitledBorder.LEFT, TitledBorder.TOP));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JComboBox<String> backendField = new JComboBox<>();
        JComboBox<String> contextField = new JComboBox<>();
        backendField.setEditable(true);
        contextField.setEditable(true);
        configurePlaceholderBehavior(backendField, "Select backend...");
        configurePlaceholderBehavior(contextField, "Select context...");
        loadComboBoxItems("backends.txt", backendField, "Select backend...");
        loadComboBoxItems("contexts.txt", contextField, "Select context...");

        JTextField keyField = new JTextField();
        JTextField valueField = new JTextField();
        JButton saveButton = new JButton("Save Secret");

        gbc.gridx = 0; gbc.gridy = 0;
        inputPanel.add(new JLabel("Backend:"), gbc);
        gbc.gridx = 1;
        inputPanel.add(backendField, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        inputPanel.add(new JLabel("Context:"), gbc);
        gbc.gridx = 1;
        inputPanel.add(contextField, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        inputPanel.add(new JLabel("Key:"), gbc);
        gbc.gridx = 1;
        inputPanel.add(keyField, gbc);

        gbc.gridx = 0; gbc.gridy = 3;
        inputPanel.add(new JLabel("Value:"), gbc);
        gbc.gridx = 1;
        inputPanel.add(valueField, gbc);

        gbc.gridx = 1; gbc.gridy = 4;
        inputPanel.add(saveButton, gbc);

        String[] columnNames = {"Backend", "Context", "Key", "Value", "Action"};
        tableModel = new DefaultTableModel(columnNames, 0);
        JTable table = new JTable(tableModel);
        table.setRowHeight(28);
        table.getColumn("Action").setCellRenderer(new ButtonRenderer());
        table.getColumn("Action").setCellEditor(new ButtonEditorWithDelete(new JCheckBox(), vaultTemplate));


        JScrollPane scrollPane = new JScrollPane(table);
        JPanel tablePanel = new JPanel(new BorderLayout());
        tablePanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Saved Secrets", TitledBorder.LEFT, TitledBorder.TOP));
        tablePanel.add(scrollPane, BorderLayout.CENTER);

        loadSecretsHistory();

        saveButton.addActionListener(e -> {
            String backend = ((String) backendField.getSelectedItem()).trim();
            String context = ((String) contextField.getSelectedItem()).trim();
            String key = keyField.getText().trim();
            String value = valueField.getText().trim();

            if (backend.isEmpty() || context.isEmpty() || key.isEmpty() || value.isEmpty()) {
                JOptionPane.showMessageDialog(frame, "All fields are required", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (backend.startsWith("Select") || context.startsWith("Select")) {
                JOptionPane.showMessageDialog(frame, "Please select a valid backend and context", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            String path = backend + "/data/" + context + "/" + key;
            try {
                Map<String, Object> data = new HashMap<>();
                data.put(key, value);
                vaultTemplate.write(path, Collections.singletonMap("data", data));

                for (int i = 0; i < tableModel.getRowCount(); i++) {
                    if (tableModel.getValueAt(i, 0).equals(backend) && tableModel.getValueAt(i, 1).equals(context) && tableModel.getValueAt(i, 2).equals(key)) {
                        tableModel.removeRow(i);
                        break;
                    }
                }

                tableModel.addRow(new Object[]{backend, context, key, "****", "View/Delete"});
                saveSecretToFile(backend, context, key, value);
                persistComboBoxItem("backends.txt", backend, backendField);
                persistComboBoxItem("contexts.txt", context, contextField);

                keyField.setText("");
                valueField.setText("");
                JOptionPane.showMessageDialog(frame, "Secret saved successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(frame, "Error saving secret: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        mainPanel.add(inputPanel, BorderLayout.NORTH);
        mainPanel.add(tablePanel, BorderLayout.CENTER);
        frame.getContentPane().add(mainPanel);
        SwingUtilities.invokeLater(() -> keyField.requestFocusInWindow());
        frame.setVisible(true);
    }

    private void configurePlaceholderBehavior(JComboBox<String> comboBox, String placeholder) {
        comboBox.setEditable(true);
        JTextField editor = (JTextField) comboBox.getEditor().getEditorComponent();
        editor.setForeground(Color.GRAY);
        editor.setText(placeholder);

        comboBox.getEditor().getEditorComponent().addFocusListener(new java.awt.event.FocusAdapter() {
            @Override
            public void focusGained(java.awt.event.FocusEvent e) {
                if (editor.getText().equals(placeholder)) {
                    editor.setText("");
                    editor.setForeground(Color.BLACK);
                }
            }

            @Override
            public void focusLost(java.awt.event.FocusEvent e) {
                if (editor.getText().isEmpty()) {
                    editor.setForeground(Color.GRAY);
                    editor.setText(placeholder);
                }
            }
        });
    }

    private void loadComboBoxItems(String fileName, JComboBox<String> comboBox, String defaultItem) {
        Set<String> items = new LinkedHashSet<>();
        Path path = Paths.get(fileName);
        if (Files.exists(path)) {
            try (BufferedReader reader = Files.newBufferedReader(path)) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (!line.trim().isEmpty()) {
                        items.add(line.trim());
                    }
                }
            } catch (IOException ignored) {}
        }
        comboBox.removeAllItems();
        comboBox.addItem(defaultItem);
        items.forEach(comboBox::addItem);
        comboBox.setSelectedIndex(0);
    }

    private void persistComboBoxItem(String fileName, String item, JComboBox<String> comboBox) {
        Path path = Paths.get(fileName);
        Set<String> all = new LinkedHashSet<>();
        if (Files.exists(path)) {
            try {
                all.addAll(Files.readAllLines(path));
            } catch (IOException ignored) {}
        }
        if (all.add(item)) {
            try {
                Files.write(path, all);
                comboBox.addItem(item);
            } catch (IOException ignored) {}
        }
    }

    private void saveSecretToFile(String backend, String context, String key, String value) {
        String compoundKey = backend + "::" + context + "::" + key;
        String line = compoundKey + "::" + encrypt(value);
        try {
            java.util.List<String> lines = new ArrayList<>();
            Path path = Paths.get(LOCAL_HISTORY_FILE);
            if (Files.exists(path)) {
                for (String existing : Files.readAllLines(path)) {
                    if (!existing.startsWith(compoundKey + "::")) {
                        lines.add(existing);
                    }
                }
            }
            lines.add(line);
            Files.write(path, lines);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadSecretsHistory() {
        Path path = Paths.get(LOCAL_HISTORY_FILE);
        if (!Files.exists(path)) return;
        try {
            java.util.List<String> lines = Files.readAllLines(path);
            for (String line : lines) {
                String[] parts = line.split("::", 4);
                if (parts.length == 4) {
                    tableModel.addRow(new Object[]{parts[0], parts[1], parts[2], "****", "View/Delete"});
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String encrypt(String input) {
        char[] key = ENCRYPTION_KEY.toCharArray();
        char[] inChars = input.toCharArray();
        for (int i = 0; i < inChars.length; i++) {
            inChars[i] ^= key[i % key.length];
        }
        return Base64.getEncoder().encodeToString(new String(inChars).getBytes());
    }

    private static String decrypt(String input) {
        byte[] decoded = Base64.getDecoder().decode(input);
        char[] key = ENCRYPTION_KEY.toCharArray();
        char[] inChars = new String(decoded).toCharArray();
        for (int i = 0; i < inChars.length; i++) {
            inChars[i] ^= key[i % key.length];
        }
        return new String(inChars);
    }

    static class ButtonRenderer extends JButton implements javax.swing.table.TableCellRenderer {
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
            setText((value == null) ? "" : value.toString());
            return this;
        }
    }
}
