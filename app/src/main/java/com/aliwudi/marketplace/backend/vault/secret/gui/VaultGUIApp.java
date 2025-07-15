package com.aliwudi.marketplace.backend.vault.secret.gui;

import com.formdev.flatlaf.FlatLightLaf;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.vault.authentication.TokenAuthentication;
import org.springframework.vault.client.VaultEndpoint;
import org.springframework.vault.core.VaultTemplate;
import org.springframework.boot.ApplicationRunner;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.*;
import java.net.URI;
import java.nio.file.*;
import java.util.*;
import org.springframework.vault.authentication.ClientAuthentication;

@SpringBootApplication
public class VaultGUIApp {

    private static final String LOCAL_HISTORY_FILE = "secrets_history.dat";
    private static final String ENCRYPTION_KEY = "change_this_key";

    private static DefaultTableModel tableModel;
    private static final String VAULT_CONFIG_FILE = "vault-config.properties";

    public static void main(String[] args) {
        System.setProperty("java.awt.headless", "false");
        SwingUtilities.invokeLater(FlatLightLaf::setup);
        SpringApplication.run(VaultGUIApp.class, args);
    }

    @Bean
    public ApplicationRunner applicationRunner() {
        return args -> SwingUtilities.invokeLater(this::createAndShowGUI);
    }

    private void createAndShowGUI() {
        JFrame frame = new JFrame("Vault Secret Manager");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 600);
        frame.setLocationRelativeTo(null);

        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        final VaultTemplate[] vaultTemplateRef = {null};

        Properties vaultConfig = loadVaultConfig();
        String vaultAddress = vaultConfig.getProperty("vault.url", "http://127.0.0.1:8200");

        // Vault Configuration Panel
        JPanel configPanel = new JPanel(new GridBagLayout());
        configPanel.setBorder(BorderFactory.createTitledBorder("Vault Server Configuration"));

        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(5, 5, 5, 5);
        c.fill = GridBagConstraints.HORIZONTAL;

        // === Row 0: Vault Address and Apply Button ===
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 0;
        c.anchor = GridBagConstraints.EAST;
        configPanel.add(new JLabel("Vault Address:"), c);

        c.gridx = 1;
        c.weightx = 1.0;
        JTextField urlField = new JTextField(vaultAddress, 30);
        configPanel.add(urlField, c);

        c.gridx = 2;
        c.weightx = 0;
        c.fill = GridBagConstraints.NONE;
        JButton applyButton = new JButton("Apply");
        configPanel.add(applyButton, c);

        // === Row 1: Role ID ===
        c.gridy = 1;
        c.gridx = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.EAST;
        c.weightx = 0;
        configPanel.add(new JLabel("Role ID:"), c);

        c.gridx = 1;
        c.gridwidth = 2;
        c.weightx = 1.0;
        JTextField roleIdField = new JTextField(vaultConfig.getProperty("vault.roleId", ""), 30);
        configPanel.add(roleIdField, c);
        c.gridwidth = 1;

        // === Row 2: Secret ID and Toggle Button ===
        c.gridy = 2;
        c.gridx = 0;
        c.anchor = GridBagConstraints.EAST;
        c.weightx = 0;
        configPanel.add(new JLabel("Secret ID:"), c);

        c.gridx = 1;
        c.weightx = 1.0;
        c.fill = GridBagConstraints.HORIZONTAL;
        JPasswordField secretIdField = new JPasswordField(vaultConfig.getProperty("vault.secretId", ""), 30);
        secretIdField.setEchoChar('\u2022'); // default echo char
        configPanel.add(secretIdField, c);

        c.gridx = 2;
        c.weightx = 0;
        c.fill = GridBagConstraints.NONE;
        JToggleButton toggleButton = new JToggleButton("Show");
        toggleButton.addActionListener(e -> {
            boolean show = toggleButton.isSelected();
            secretIdField.setEchoChar(show ? (char) 0 : '\u2022');
            toggleButton.setText(show ? "Hide" : "Show");
        });
        configPanel.add(toggleButton, c);

        applyButton.addActionListener(e -> {
            String newAddress = urlField.getText().trim();
            String roleId = roleIdField.getText().trim();
            String secretId = new String(secretIdField.getPassword()).trim();

            if (!newAddress.isEmpty() && !roleId.isEmpty() && !secretId.isEmpty()) {
                try {
                    String token = VaultTokenGenerator.getVaultTokenFromAppRole(newAddress, roleId, secretId);
                    VaultEndpoint endpoint = VaultEndpoint.from(new URI(newAddress));
                    ClientAuthentication auth = new TokenAuthentication(token);
                    VaultTemplate newTemplate = new VaultTemplate(endpoint, auth);
                    vaultTemplateRef[0] = newTemplate;

                    saveVaultConfig(newAddress, roleId, secretId); // store for next time

                    JOptionPane.showMessageDialog(frame, "Vault configuration applied.", "Success", JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(frame, "Login failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            } else {
                JOptionPane.showMessageDialog(frame, "Vault address, Role ID, and Secret ID must not be empty", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

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

        gbc.gridx = 0;
        gbc.gridy = 0;
        inputPanel.add(new JLabel("Backend:"), gbc);
        gbc.gridx = 1;
        inputPanel.add(backendField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        inputPanel.add(new JLabel("Context:"), gbc);
        gbc.gridx = 1;
        inputPanel.add(contextField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        inputPanel.add(new JLabel("Key:"), gbc);
        gbc.gridx = 1;
        inputPanel.add(keyField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 3;
        inputPanel.add(new JLabel("Value:"), gbc);
        gbc.gridx = 1;
        inputPanel.add(valueField, gbc);

        gbc.gridx = 1;
        gbc.gridy = 4;
        inputPanel.add(saveButton, gbc);

        String[] columnNames = {"Backend", "Context", "Key", "Value", "Action"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        JTable table = new JTable(tableModel) {
            public TableCellRenderer getCellRenderer(int row, int column) {
                if (column == 4) {
                    return new ActionRenderer();
                }
                return super.getCellRenderer(row, column);
            }
        };

        table.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                int row = table.rowAtPoint(e.getPoint());
                int col = table.columnAtPoint(e.getPoint());
                if (col == 4) {
                    JPopupMenu popup = new JPopupMenu();
                    JMenuItem viewItem = new JMenuItem("View");
                    JMenuItem deleteItem = new JMenuItem("Delete");

                    String backend = (String) tableModel.getValueAt(row, 0);
                    String context = (String) tableModel.getValueAt(row, 1);
                    String key = (String) tableModel.getValueAt(row, 2);
                    String compoundKey = backend + "::" + context + "::" + key;

                    viewItem.addActionListener(ev -> {
                        try {
                            for (String line : Files.readAllLines(Paths.get(LOCAL_HISTORY_FILE))) {
                                if (line.startsWith(compoundKey + "::")) {
                                    String[] parts = line.split("::", 4);
                                    if (parts.length == 4) {
                                        JOptionPane.showMessageDialog(frame, "Value: " + decrypt(parts[3]), "Secret", JOptionPane.INFORMATION_MESSAGE);
                                    }
                                    break;
                                }
                            }
                        } catch (IOException ex) {
                            ex.printStackTrace();
                        }
                    });

                    deleteItem.addActionListener(ev -> {
                        if (vaultTemplateRef[0] == null) {
                            return;
                        }
                        int confirm = JOptionPane.showConfirmDialog(frame, "Are you sure you want to delete this secret?", "Confirm", JOptionPane.YES_NO_OPTION);
                        if (confirm == JOptionPane.YES_OPTION) {
                            try {
                                String path = backend + "/data/" + context + "/" + key;
                                vaultTemplateRef[0].delete(path);
                                Files.write(Paths.get(LOCAL_HISTORY_FILE), Files.readAllLines(Paths.get(LOCAL_HISTORY_FILE)).stream().filter(l -> !l.startsWith(compoundKey + "::")).toList());
                                tableModel.removeRow(row);
                            } catch (IOException ex) {
                                ex.printStackTrace();
                            }
                        }
                    });

                    popup.add(viewItem);
                    popup.add(deleteItem);
                    popup.show(table, e.getX(), e.getY());
                }
            }
        });

        table.setRowHeight(28);
        JScrollPane scrollPane = new JScrollPane(table);
        JPanel tablePanel = new JPanel(new BorderLayout());
        tablePanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Saved Secrets", TitledBorder.LEFT, TitledBorder.TOP));
        tablePanel.add(scrollPane, BorderLayout.CENTER);

        loadSecretsHistory();

        saveButton.addActionListener(e -> {
            if (vaultTemplateRef[0] == null) {
                JOptionPane.showMessageDialog(frame, "Vault is not configured.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            String backend = ((String) backendField.getSelectedItem()).trim();
            String context = ((String) contextField.getSelectedItem()).trim();
            String key = keyField.getText().trim();
            String value = valueField.getText().trim();

            if (backend.isEmpty() || context.isEmpty() || key.isEmpty() || value.isEmpty()) {
                JOptionPane.showMessageDialog(frame, "All fields are required", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            String path = backend + "/data/" + context + "/" + key;
            try {
                Map<String, Object> data = new HashMap<>();
                data.put(key, value);
                vaultTemplateRef[0].write(path, Collections.singletonMap("data", data));

                for (int i = 0; i < tableModel.getRowCount(); i++) {
                    if (tableModel.getValueAt(i, 0).equals(backend) && tableModel.getValueAt(i, 1).equals(context) && tableModel.getValueAt(i, 2).equals(key)) {
                        tableModel.removeRow(i);
                        break;
                    }
                }
                tableModel.addRow(new Object[]{backend, context, key, "****", "⋮"});
                saveSecretToFile(backend, context, key, value);
                persistComboBoxItem("backends.txt", backend, backendField);
                persistComboBoxItem("contexts.txt", context, contextField);

                keyField.setText("");
                valueField.setText("");
                JOptionPane.showMessageDialog(frame, "Secret saved successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(frame, "Error saving secret: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                
            }
        });

        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));
        topPanel.add(configPanel);
        topPanel.add(Box.createRigidArea(new Dimension(0, 10))); // spacing
        topPanel.add(inputPanel);

        mainPanel.add(topPanel, BorderLayout.PAGE_START); // ✅ now stacks both correctly

        mainPanel.add(tablePanel, BorderLayout.CENTER);

        frame.getContentPane().add(mainPanel);
        SwingUtilities.invokeLater(() -> keyField.requestFocusInWindow());
        frame.setVisible(true);
    }

    private void saveVaultConfig(String url, String roleId, String secretId) {
        try (FileOutputStream out = new FileOutputStream(VAULT_CONFIG_FILE)) {
            Properties props = new Properties();
            props.setProperty("vault.url", url);
            props.setProperty("vault.roleId", roleId);
            props.setProperty("vault.secretId", secretId);
            props.store(out, null);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Properties loadVaultConfig() {
        Properties props = new Properties();
        try (FileInputStream in = new FileInputStream(VAULT_CONFIG_FILE)) {
            props.load(in);
        } catch (IOException ignored) {
        }
        return props;
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
            } catch (IOException ignored) {
            }
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
            } catch (IOException ignored) {
            }
        }
        if (all.add(item)) {
            try {
                Files.write(path, all);
                comboBox.addItem(item);
            } catch (IOException ignored) {
            }
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
        if (!Files.exists(path)) {
            return;
        }
        try {
            java.util.List<String> lines = Files.readAllLines(path);
            for (String line : lines) {
                String[] parts = line.split("::", 4);
                if (parts.length == 4) {
                    tableModel.addRow(new Object[]{parts[0], parts[1], parts[2], "****", "⋮"});
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

    static class ActionRenderer extends JLabel implements TableCellRenderer {

        public ActionRenderer() {
            setText("⋮");
            setHorizontalAlignment(SwingConstants.CENTER);
        }

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            return this;
        }
    }
}
