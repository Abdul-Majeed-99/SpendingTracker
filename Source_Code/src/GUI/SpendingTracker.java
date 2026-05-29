/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package GUI;
import DB.DbConnect;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.ZoneId;
import javax.swing.JOptionPane;
import javax.swing.table.DefaultTableModel;


/**
 *
 * @author majee
 */
public class SpendingTracker extends javax.swing.JFrame {
    
    private static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(SpendingTracker.class.getName());

    /**
     * Creates new form SpendingTracker
     */
    public SpendingTracker() {
        initComponents();
        checkSpendingsIdColumn();
        displayCategory();
        displayExpenses();
        setupAmountFieldValidation();
        setupDateChooser();
    }

    private void setupAmountFieldValidation() {
        // Create a document that only allows digits
        javax.swing.text.PlainDocument doc = new javax.swing.text.PlainDocument() {
            @Override
            public void insertString(int offset, String str, javax.swing.text.AttributeSet attr) throws javax.swing.text.BadLocationException {
                if (str == null) return;
                // Only allow digits
                if (str.matches("[0-9]*")) {
                    super.insertString(offset, str, attr);
                }
            }
        };
        jTextField1.setDocument(doc);
    }

    private void setupDateChooser() {
        // Set today as the default date if the chooser is empty.
        if (jDateChooser1.getDate() == null) {
            jDateChooser1.setDate(new java.util.Date());
        }
    }

    private boolean hasIdColumn = false;

    private void checkSpendingsIdColumn() {
        try (Connection c = DbConnect.getConnection()) {
            java.sql.DatabaseMetaData md = c.getMetaData();
            try (ResultSet rs = md.getColumns(null, null, "spendings", "id")) {
                hasIdColumn = rs.next();
            }
        } catch (Exception ex) {
            hasIdColumn = false;
        }
    }

    private void displayCategory() {
        category.removeAllItems();
        try (Connection c = DbConnect.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT DISTINCT category FROM category_info ORDER BY category" );
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                category.addItem(rs.getString("category"));
            }

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error loading categories: " + ex.getMessage());
        }
    }

    private void displayExpenses() {
        java.util.Date today = java.util.Date.from(LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant());
        LocalDate fromDate = LocalDate.now().minusDays(20);
        java.sql.Date sqlFrom = java.sql.Date.valueOf(fromDate);
        java.sql.Date sqlTo = java.sql.Date.valueOf(LocalDate.now());

        String[] columnNames = hasIdColumn ? new String[]{"ID", "Date", "Category", "Amount"} : new String[]{"Date", "Category", "Amount"};
        DefaultTableModel model = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        int total = 0;

        String sql = hasIdColumn
                ? "SELECT id, sdate, category, amount FROM spendings WHERE sdate BETWEEN ? AND ? ORDER BY sdate DESC"
                : "SELECT sdate, category, amount FROM spendings WHERE sdate BETWEEN ? AND ? ORDER BY sdate DESC";

        try (Connection c = DbConnect.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setDate(1, sqlFrom);
            ps.setDate(2, sqlTo);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    if (hasIdColumn) {
                        int id = rs.getInt("id");
                        java.sql.Date date = rs.getDate("sdate");
                        String categoryName = rs.getString("category");
                        int amount = rs.getInt("amount");
                        model.addRow(new Object[]{id, date, categoryName, amount});
                        total += amount;
                    } else {
                        java.sql.Date date = rs.getDate("sdate");
                        String categoryName = rs.getString("category");
                        int amount = rs.getInt("amount");
                        model.addRow(new Object[]{date, categoryName, amount});
                        total += amount;
                    }
                }
            }
            jTable1.setModel(model);
            jTextField4.setText(String.valueOf(total));
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error loading expenses: " + ex.getMessage());
        }
    }

    private void openCategoryWindow() {
        Category categoryWindow = new Category();
        categoryWindow.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosed(java.awt.event.WindowEvent e) {
                // When category dialog is closed, refresh the combo box and table
                displayCategory();
                displayExpenses();
            }
        });
        categoryWindow.setVisible(true);
    }

    private void addExpense() {
        // Refresh categories in case new ones were added
        if (category.getItemCount() == 0) {
            displayCategory();
        }
        
        java.util.Date selectedDate = jDateChooser1.getDate();
        String amountText = jTextField1.getText().trim();
        String selectedCategory = (String) category.getSelectedItem();

        if (selectedDate == null || amountText.isEmpty() || selectedCategory == null || selectedCategory.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please select a date, amount, and category.");
            return;
        }

        // Check if selected date is in the future
        LocalDate selectedLocalDate = selectedDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        LocalDate todayLocalDate = LocalDate.now();
        if (selectedLocalDate.isAfter(todayLocalDate)) {
            JOptionPane.showMessageDialog(this, "Cannot add expense for future dates. Please select today or a past date.", "Future Date Not Allowed", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int amount;
        try {
            amount = Integer.parseInt(amountText);
            if (amount < 0) {
                throw new NumberFormatException("Negative amount");
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Please enter a valid positive amount.");
            return;
        }

        java.sql.Date sqlDate = new java.sql.Date(selectedDate.getTime());
        try (Connection c = DbConnect.getConnection();
             PreparedStatement ps = c.prepareStatement("INSERT INTO spendings(category, sdate, amount) VALUES(?,?,?)")) {
            ps.setString(1, selectedCategory);
            ps.setDate(2, sqlDate);
            ps.setInt(3, amount);
            ps.executeUpdate();
            JOptionPane.showMessageDialog(this, "Expense added successfully.");
            jTextField1.setText("");
            jDateChooser1.setDate(new java.util.Date());
            displayCategory();
            displayExpenses();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error adding expense: " + ex.getMessage());
        }
    }

    private void removeSelectedExpense() {
        int selectedRow = jTable1.getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this, "Please select an expense to remove.");
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(this, "Delete selected expense?", "Confirm Delete", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        DefaultTableModel model = (DefaultTableModel) jTable1.getModel();
        try (Connection c = DbConnect.getConnection()) {
            if (hasIdColumn) {
                int id;
                try {
                    id = Integer.parseInt(model.getValueAt(selectedRow, 0).toString());
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this, "Unable to determine the selected expense ID.");
                    return;
                }
                // Before deleting, ensure the selected entry is within the last 20 days
                Object dateValue = model.getValueAt(selectedRow, 1);
                java.sql.Date rowDate;
                if (dateValue instanceof java.sql.Date) {
                    rowDate = (java.sql.Date) dateValue;
                } else if (dateValue instanceof java.util.Date) {
                    rowDate = new java.sql.Date(((java.util.Date) dateValue).getTime());
                } else {
                    try {
                        rowDate = java.sql.Date.valueOf(dateValue.toString());
                    } catch (IllegalArgumentException ex) {
                        JOptionPane.showMessageDialog(this, "Unable to parse the selected date.");
                        return;
                    }
                }
                LocalDate selectedLocal = rowDate.toLocalDate();
                LocalDate cutoff = LocalDate.now().minusDays(20);
                if (selectedLocal.isBefore(cutoff)) {
                    JOptionPane.showMessageDialog(this, "Cannot remove entries older than 20 days.");
                    return;
                }

                try (PreparedStatement ps = c.prepareStatement("DELETE FROM spendings WHERE id = ?")) {
                    ps.setInt(1, id);
                    int deleted = ps.executeUpdate();
                    if (deleted > 0) {
                        JOptionPane.showMessageDialog(this, "Expense removed successfully.");
                        displayExpenses();
                    } else {
                        JOptionPane.showMessageDialog(this, "Expense could not be removed.");
                    }
                }
            } else {
                // No id column: delete by sdate + category + amount (may remove multiple rows if duplicates exist)
                Object dateValue = model.getValueAt(selectedRow, 0);
                java.sql.Date date;
                if (dateValue instanceof java.sql.Date) {
                    date = (java.sql.Date) dateValue;
                } else if (dateValue instanceof java.util.Date) {
                    date = new java.sql.Date(((java.util.Date) dateValue).getTime());
                } else {
                    try {
                        date = java.sql.Date.valueOf(dateValue.toString());
                    } catch (IllegalArgumentException ex) {
                        JOptionPane.showMessageDialog(this, "Unable to parse the selected date.");
                        return;
                    }
                }
                // Ensure this row is within the last 20 days before allowing deletion
                LocalDate selLocal = date.toLocalDate();
                LocalDate cutoff2 = LocalDate.now().minusDays(20);
                if (selLocal.isBefore(cutoff2)) {
                    JOptionPane.showMessageDialog(this, "Cannot remove entries older than 20 days.");
                    return;
                }
                String categoryName = model.getValueAt(selectedRow, 1).toString();
                int amount = Integer.parseInt(model.getValueAt(selectedRow, 2).toString());

                try (PreparedStatement ps = c.prepareStatement("DELETE FROM spendings WHERE sdate = ? AND category = ? AND amount = ?")) {
                    ps.setDate(1, date);
                    ps.setString(2, categoryName);
                    ps.setInt(3, amount);
                    int deleted = ps.executeUpdate();
                    if (deleted > 0) {
                        JOptionPane.showMessageDialog(this, "Expense removed successfully.");
                        displayExpenses();
                    } else {
                        JOptionPane.showMessageDialog(this, "Expense could not be removed.");
                    }
                }
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error removing expense: " + ex.getMessage());
        }
    }
    
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jLabel2 = new javax.swing.JLabel();
        jMenu1 = new javax.swing.JMenu();
        jMenu2 = new javax.swing.JMenu();
        jPanel3 = new javax.swing.JPanel();
        jPanel1 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jDateChooser1 = new com.toedter.calendar.JDateChooser();
        jLabel4 = new javax.swing.JLabel();
        jTextField1 = new javax.swing.JTextField();
        jLabel5 = new javax.swing.JLabel();
        category = new javax.swing.JComboBox<>();
        jButton1 = new javax.swing.JButton();
        jButton2 = new javax.swing.JButton();
        jTextField2 = new javax.swing.JTextField();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTable1 = new javax.swing.JTable();
        jButton3 = new javax.swing.JButton();
        jPanel2 = new javax.swing.JPanel();
        jTextField3 = new javax.swing.JTextField();
        jTextField4 = new javax.swing.JTextField();
        jMenuBar1 = new javax.swing.JMenuBar();
        jMenu3 = new javax.swing.JMenu();
        jMenuItem1 = new javax.swing.JMenuItem();
        jMenuItem2 = new javax.swing.JMenuItem();
        jMenuItem3 = new javax.swing.JMenuItem();
        jSeparator1 = new javax.swing.JPopupMenu.Separator();
        jMenu4 = new javax.swing.JMenu();
        jMenuItem4 = new javax.swing.JMenuItem();

        jLabel2.setText("jLabel2");

        jMenu1.setText("jMenu1");

        jMenu2.setText("jMenu2");

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("Spending Tracker");

        jPanel3.setBackground(new java.awt.Color(0, 0, 153));

        jPanel1.setBackground(new java.awt.Color(51, 102, 255));

        jLabel1.setFont(new java.awt.Font("Segoe UI", 1, 36)); // NOI18N
        jLabel1.setForeground(new java.awt.Color(255, 255, 255));
        jLabel1.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel1.setText("Add New Expense");

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jLabel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(29, 29, 29)
                .addComponent(jLabel1)
                .addContainerGap(36, Short.MAX_VALUE))
        );

        jLabel3.setFont(new java.awt.Font("Segoe UI", 1, 24)); // NOI18N
        jLabel3.setForeground(new java.awt.Color(255, 255, 255));
        jLabel3.setText("Date :");

        jLabel4.setFont(new java.awt.Font("Segoe UI", 1, 24)); // NOI18N
        jLabel4.setForeground(new java.awt.Color(255, 255, 255));
        jLabel4.setText("Amount:");

        jTextField1.addActionListener(this::jTextField1ActionPerformed);

        jLabel5.setFont(new java.awt.Font("Segoe UI", 1, 24)); // NOI18N
        jLabel5.setForeground(new java.awt.Color(255, 255, 255));
        jLabel5.setText("Category:");

        category.addActionListener(this::categoryActionPerformed);

        jButton1.setText("Add New Category");
        jButton1.addActionListener(this::jButton1ActionPerformed);

        jButton2.setBackground(new java.awt.Color(204, 255, 204));
        jButton2.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        jButton2.setForeground(new java.awt.Color(0, 255, 0));
        jButton2.setText("Add");
        jButton2.addActionListener(this::jButton2ActionPerformed);

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel3Layout.createSequentialGroup()
                .addGap(29, 29, 29)
                .addComponent(jLabel3)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jDateChooser1, javax.swing.GroupLayout.PREFERRED_SIZE, 141, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 183, Short.MAX_VALUE)
                .addComponent(jLabel4)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jTextField1, javax.swing.GroupLayout.PREFERRED_SIZE, 152, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(135, 135, 135)
                .addComponent(jLabel5)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jButton1, javax.swing.GroupLayout.PREFERRED_SIZE, 156, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addComponent(category, javax.swing.GroupLayout.PREFERRED_SIZE, 156, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButton2, javax.swing.GroupLayout.PREFERRED_SIZE, 129, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addGap(22, 22, 22))
            .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jButton2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jLabel3, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 37, Short.MAX_VALUE)
                    .addComponent(jDateChooser1, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jTextField1)
                    .addComponent(category, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel3Layout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(jLabel5))
                    .addComponent(jLabel4, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jButton1, javax.swing.GroupLayout.PREFERRED_SIZE, 35, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(25, 25, 25))
        );

        jTextField2.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        jTextField2.setText("Last 20 Days Spendings:");
        jTextField2.addActionListener(this::jTextField2ActionPerformed);

        jTable1.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "ID", "Date", "Category", "Amount"
            }
        ) {
            boolean[] canEdit = new boolean [] {
                false, false, false, false
            };

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        jTable1.getTableHeader().setReorderingAllowed(false);
        jScrollPane1.setViewportView(jTable1);

        jButton3.setBackground(new java.awt.Color(255, 204, 204));
        jButton3.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        jButton3.setForeground(new java.awt.Color(255, 0, 0));
        jButton3.setText("Remove");
        jButton3.addActionListener(this::jButton3ActionPerformed);

        jPanel2.setBackground(new java.awt.Color(204, 204, 204));

        jTextField3.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        jTextField3.setText(" Total Amount: ");

        jTextField4.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jTextField3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jTextField4, javax.swing.GroupLayout.PREFERRED_SIZE, 195, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGap(20, 20, 20)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jTextField3)
                    .addComponent(jTextField4, javax.swing.GroupLayout.PREFERRED_SIZE, 50, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(23, Short.MAX_VALUE))
        );

        jMenu3.setText("Master ");

        jMenuItem1.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_V, java.awt.event.InputEvent.ALT_DOWN_MASK));
        jMenuItem1.setIcon(new javax.swing.ImageIcon(getClass().getResource("/GUI/download.png"))); // NOI18N
        jMenuItem1.setText("View All Spendings");
        jMenuItem1.addActionListener(this::jMenuItem1ActionPerformed);
        jMenu3.add(jMenuItem1);

        jMenuItem2.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_G, java.awt.event.InputEvent.ALT_DOWN_MASK));
        jMenuItem2.setIcon(new javax.swing.ImageIcon(getClass().getResource("/GUI/category.png"))); // NOI18N
        jMenuItem2.setText("Add/View Category");
        jMenuItem2.addActionListener(this::jMenuItem2ActionPerformed);
        jMenu3.add(jMenuItem2);

        jMenuItem3.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_ESCAPE, 0));
        jMenuItem3.setIcon(new javax.swing.ImageIcon(getClass().getResource("/GUI/exit.png"))); // NOI18N
        jMenuItem3.setText("Exit App");
        jMenuItem3.addActionListener(this::jMenuItem3ActionPerformed);
        jMenu3.add(jMenuItem3);
        jMenu3.add(jSeparator1);

        jMenu4.setIcon(new javax.swing.ImageIcon(getClass().getResource("/GUI/more.png"))); // NOI18N
        jMenu4.setText("More");

        jMenuItem4.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_A, java.awt.event.InputEvent.ALT_DOWN_MASK | java.awt.event.InputEvent.SHIFT_DOWN_MASK));
        jMenuItem4.setIcon(new javax.swing.ImageIcon(getClass().getResource("/GUI/About us.png"))); // NOI18N
        jMenuItem4.setText("About App");
        jMenuItem4.addActionListener(this::jMenuItem4ActionPerformed);
        jMenu4.add(jMenuItem4);

        jMenu3.add(jMenu4);

        jMenuBar1.add(jMenu3);

        setJMenuBar(jMenuBar1);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(15, 15, 15)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 1260, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jTextField2, javax.swing.GroupLayout.PREFERRED_SIZE, 1117, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButton3, javax.swing.GroupLayout.PREFERRED_SIZE, 114, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap(15, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(23, 23, 23)
                .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jTextField2)
                    .addComponent(jButton3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 310, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        pack();
        setLocationRelativeTo(null);
    }// </editor-fold>//GEN-END:initComponents

    private void jTextField1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTextField1ActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jTextField1ActionPerformed

    private void jTextField2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTextField2ActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jTextField2ActionPerformed

    private void categoryActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_categoryActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_categoryActionPerformed

    private void jButton2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton2ActionPerformed
        addExpense();
    }//GEN-LAST:event_jButton2ActionPerformed

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
        openCategoryWindow();
    }//GEN-LAST:event_jButton1ActionPerformed

    private void jButton3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton3ActionPerformed
        removeSelectedExpense();
    }//GEN-LAST:event_jButton3ActionPerformed

    private void jMenuItem1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem1ActionPerformed
        ViewSpending view = new ViewSpending();
        view.setVisible(true);
    }//GEN-LAST:event_jMenuItem1ActionPerformed

    private void jMenuItem2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem2ActionPerformed
        openCategoryWindow();
    }//GEN-LAST:event_jMenuItem2ActionPerformed

    private void jMenuItem3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem3ActionPerformed
        int confirm = JOptionPane.showConfirmDialog(this, "Exit application?", "Confirm Exit", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            System.exit(0);
        }
    }//GEN-LAST:event_jMenuItem3ActionPerformed

    private void jMenuItem4ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem4ActionPerformed
        String about = "This software was designed by Abdul Majeed, Abdul Haseeb, Fahad, and Muhammad Hassan.\n"
            + "We are second-semester students at Air University Islamabad working on a Java OOPs course project.\n"
            + "We come from different cities and collaborated to do our best.";
        JOptionPane.showMessageDialog(this, about, "About", JOptionPane.INFORMATION_MESSAGE);
    }//GEN-LAST:event_jMenuItem4ActionPerformed

    private void jButton4ActionPerformed(java.awt.event.ActionEvent evt) {
        displayCategory();
        displayExpenses();
        JOptionPane.showMessageDialog(this, "Data refreshed successfully.");
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ReflectiveOperationException | javax.swing.UnsupportedLookAndFeelException ex) {
            logger.log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(() -> new SpendingTracker().setVisible(true));
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JComboBox<String> category;
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton2;
    private javax.swing.JButton jButton3;
    private com.toedter.calendar.JDateChooser jDateChooser1;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JMenu jMenu1;
    private javax.swing.JMenu jMenu2;
    private javax.swing.JMenu jMenu3;
    private javax.swing.JMenu jMenu4;
    private javax.swing.JMenuBar jMenuBar1;
    private javax.swing.JMenuItem jMenuItem1;
    private javax.swing.JMenuItem jMenuItem2;
    private javax.swing.JMenuItem jMenuItem3;
    private javax.swing.JMenuItem jMenuItem4;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JPopupMenu.Separator jSeparator1;
    private javax.swing.JTable jTable1;
    private javax.swing.JTextField jTextField1;
    private javax.swing.JTextField jTextField2;
    private javax.swing.JTextField jTextField3;
    private javax.swing.JTextField jTextField4;
    // End of variables declaration//GEN-END:variables
}
