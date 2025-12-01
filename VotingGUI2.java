import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.util.Enumeration;
import java.awt.event.*;
import java.io.File;
import java.nio.file.Files;
import java.sql.*;
import java.util.ArrayList;
import javax.swing.border.EmptyBorder;

// Note: model/DB/Admin classes are provided in VotingLogic2.java.
// Removed duplicate class definitions so the GUI reuses the canonical implementations.

// ------------------------ GUI ------------------------
public class VotingGUI2 extends JFrame {
    private Admin adminLogic = new Admin();
    private Voter currentVoter;

    private JPanel mainPanel;
    private CardLayout cardLayout;

    private final Color bgDark = Color.decode("#000000");
    private final Color bgGrey = Color.decode("#222222");
    private final Color accent1 = Color.decode("#1DCD9F");
    private final Color accent2 = Color.decode("#169976");

    public VotingGUI2() {
        setTitle("Voting System");
        setSize(900, 650);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        // Restore default window decorations (minimize/close buttons)
        setUndecorated(false);

        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);
        mainPanel.setBackground(bgDark);

        mainPanel.add(getWelcomePanel(), "WELCOME");
        mainPanel.add(getHomePanel(), "HOME");
        mainPanel.add(getAdminLoginPanel(), "ADMIN_LOGIN");
        mainPanel.add(getAdminPanel(), "ADMIN_PANEL");
        mainPanel.add(getVoterLoginPanel(), "VOTER_LOGIN");
        mainPanel.add(getVoterPanel(), "VOTER_PANEL");

        add(mainPanel);
        cardLayout.show(mainPanel, "WELCOME");
        setVisible(true);
    }

    private JButton createStyledButton(String text){
        RoundedButton btn = new RoundedButton(text);
        btn.setBackground(accent1);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setFont(new Font("Arial", Font.PLAIN, 13));
        btn.setPreferredSize(new Dimension(120,34));
        return btn;
    }

    private JButton createPrimaryButton(String text) {
        JButton btn = createStyledButton(text);
        btn.setFont(new Font("Arial", Font.BOLD, 16));
        btn.setPreferredSize(new Dimension(180,48));
        return btn;
    }

    // Rounded button with a subtle 3D gradient and rounded corners
    private class RoundedButton extends JButton {
        private final int arc = 16;
        private float scale = 1f;
        private Timer bounceTimer;

        RoundedButton(String text) {
            super(text);
            setContentAreaFilled(false);
            setFocusPainted(false);
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(6,12,6,12));
            // trigger bounce on click
            addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    startBounce();
                }
            });
        }

        private void startBounce() {
            if (bounceTimer != null && bounceTimer.isRunning()) return;
            final float[] frames = new float[]{0.96f, 1.04f, 0.98f, 1f};
            final int[] idx = {0};
            bounceTimer = new Timer(40, ae -> {
                scale = frames[idx[0]++];
                repaint();
                if (idx[0] >= frames.length) {
                    bounceTimer.stop();
                    scale = 1f;
                    repaint();
                }
            });
            bounceTimer.setInitialDelay(0);
            bounceTimer.start();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth();
            int h = getHeight();

            // scaled dimensions centered
            int sw = Math.max(2, Math.round(w * scale));
            int sh = Math.max(2, Math.round(h * scale));
            int sx = (w - sw) / 2;
            int sy = (h - sh) / 2;

            RoundRectangle2D.Float rr = new RoundRectangle2D.Float(sx, sy, sw-1, sh-1, arc, arc);

            // gradient for 3D effect
            Color base = getBackground() != null ? getBackground() : accent1;
            Color top = base.brighter();
            Color bottom = base.darker();
            GradientPaint gp = new GradientPaint(0, sy, top, 0, sy + sh, bottom);
            g2.setPaint(gp);
            g2.fill(rr);

            // subtle inner highlight
            g2.setStroke(new BasicStroke(1f));
            g2.setColor(new Color(255,255,255,40));
            RoundRectangle2D.Float inner = new RoundRectangle2D.Float(sx+1, sy+1, sw-3, sh/2, Math.max(0, arc-4), Math.max(0, arc-4));
            g2.fill(inner);

            // border
            g2.setColor(base.darker().darker());
            g2.draw(rr);

            // draw text centered within scaled rect
            String text = getText();
            Font font = getFont();
            // slightly scale font with button
            Font use = font.deriveFont(font.getSize2D() * scale);
            g2.setFont(use);
            FontMetrics fm = g2.getFontMetrics(use);
            int tx = sx + (sw - fm.stringWidth(text)) / 2;
            int ty = sy + (sh - fm.getHeight()) / 2 + fm.getAscent();
            g2.setColor(getForeground());
            g2.drawString(text, tx, ty);

            g2.dispose();
        }
    }

    private void showStyledDialog(String title, String message, boolean info) {
        // Restore standard JOptionPane dialog box
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(bgDark);
        JLabel lbl = new JLabel("<html><body style='width:420px;color:#FFFFFF;'>" + message + "</body></html>");
        lbl.setForeground(Color.WHITE);
        lbl.setFont(new Font("Arial", Font.BOLD, 14));
        p.add(lbl, BorderLayout.CENTER);
        JOptionPane.showMessageDialog(this, p, title,
            info ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.PLAIN_MESSAGE);
    }

    // Styled text input (returns input string or null if cancelled)
    private String showTextInput(String title, String label, String initial) {
        JPanel p = new JPanel(new BorderLayout(6,6));
        p.setBackground(bgDark);
        JLabel l = new JLabel(label);
        // make label have white background with black text for visibility as requested
        l.setOpaque(true);
        l.setBackground(Color.WHITE);
        l.setForeground(Color.BLACK);
        l.setFont(new Font("Arial", Font.PLAIN, 16));
        JTextField tf = new JTextField(initial != null ? initial : "", 30);
        tf.setBackground(Color.WHITE); tf.setForeground(Color.BLACK);
        tf.setFont(new Font("Arial", Font.PLAIN, 16));
        p.add(l, BorderLayout.NORTH);
        p.add(tf, BorderLayout.CENTER);
        int res = JOptionPane.showConfirmDialog(this, p, title, JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (res == JOptionPane.OK_OPTION) return tf.getText();
        return null;
    }

    // Styled option selection input for choosing from options (returns selected or null)
    private String showOptionInput(String title, String label, Object[] options, Object initial) {
        JPanel p = new JPanel(new BorderLayout(6,6));
        p.setBackground(bgDark);
        JLabel l = new JLabel(label);
        // label as white background with black text
        l.setOpaque(true);
        l.setBackground(Color.WHITE);
        l.setForeground(Color.BLACK);
        l.setFont(new Font("Arial", Font.PLAIN, 16));
        JComboBox<String> cb = new JComboBox<>();
        for (Object o : options) cb.addItem(o.toString());
        if (initial != null) cb.setSelectedItem(initial.toString());
        cb.setBackground(Color.WHITE); cb.setForeground(Color.BLACK);
        cb.setFont(new Font("Arial", Font.PLAIN, 16));
        p.add(l, BorderLayout.NORTH);
        p.add(cb, BorderLayout.CENTER);
        int res = JOptionPane.showConfirmDialog(this, p, title, JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (res == JOptionPane.OK_OPTION) return cb.getSelectedItem().toString();
        return null;
    }

    // Styled yes/no/cancel dialog for messages (returns JOptionPane constants)
    private int showYesNoCancel(String title, String message) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(bgDark);
        JLabel l = new JLabel("<html><body style='width:420px;color:#FFFFFF;'>" + message + "</body></html>");
        l.setForeground(Color.WHITE);
            l.setFont(new Font("Arial", Font.PLAIN, 14));
        p.add(l, BorderLayout.CENTER);
        return JOptionPane.showConfirmDialog(this, p, title, JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
    }

    private JPanel getWelcomePanel() {
        JPanel panel = new JPanel(new BorderLayout());
    panel.setBackground(bgDark);
        JLabel welcome = new JLabel("Welcome to the Voting System", SwingConstants.CENTER);
        welcome.setForeground(accent1);
        welcome.setFont(new Font("Arial", Font.BOLD, 32));
        panel.add(welcome, BorderLayout.CENTER);

    JButton continueBtn = createPrimaryButton("Continue");
        continueBtn.addActionListener(e -> cardLayout.show(mainPanel, "HOME"));
        JPanel btnPanel = new JPanel(); btnPanel.setBackground(bgDark); btnPanel.add(continueBtn);
        panel.add(btnPanel, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel getHomePanel() {
        JPanel panel = new JPanel(new GridBagLayout());
    // use dark background consistently
    panel.setBackground(bgDark);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(20,20,20,20);

    JButton adminBtn = createPrimaryButton("Admin");
    JButton voterBtn = createPrimaryButton("Voter");

        adminBtn.addActionListener(e -> cardLayout.show(mainPanel,"ADMIN_LOGIN"));
        voterBtn.addActionListener(e -> cardLayout.show(mainPanel,"VOTER_LOGIN"));

        gbc.gridx = 0; gbc.gridy = 0; panel.add(adminBtn, gbc);
        gbc.gridx = 1; gbc.gridy = 0; panel.add(voterBtn, gbc);

        return panel;
    }

    private JPanel getAdminLoginPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
    panel.setBackground(bgDark);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10,10,10,10);

            JLabel userLabel = new JLabel("Username:"); userLabel.setForeground(Color.WHITE);
            JLabel passLabel = new JLabel("Password:"); passLabel.setForeground(Color.WHITE);
    JTextField userField = new JTextField(15);
    JPasswordField passField = new JPasswordField(15);
    JButton loginBtn = createPrimaryButton("Login");
        JButton backBtn = createPrimaryButton("Back");

        gbc.gridx=0; gbc.gridy=0; panel.add(userLabel, gbc);
        gbc.gridx=1; panel.add(userField, gbc);
        gbc.gridx=0; gbc.gridy=1; panel.add(passLabel, gbc);
        gbc.gridx=1; panel.add(passField, gbc);
        gbc.gridx=0; gbc.gridy=2; panel.add(loginBtn, gbc);
        gbc.gridx=1; panel.add(backBtn, gbc);

        loginBtn.addActionListener(e -> {
            String user = userField.getText();
            String pass = new String(passField.getPassword());
            if(adminLogic.login(user, pass)){
                showStyledDialog("Success", "Admin Login Successful", false);
                cardLayout.show(mainPanel,"ADMIN_PANEL");
            } else showStyledDialog("Error", "Login Failed", false);
        });

        backBtn.addActionListener(e -> cardLayout.show(mainPanel,"HOME"));
        return panel;
    }

    private JPanel getAdminPanel() {
    JPanel panel = new JPanel(new GridLayout(4,4,20,20));
    // use dark background consistently
    panel.setBackground(bgDark);
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));

        JButton addCandidateBtn = createStyledButton("Add Candidate");
        JButton editCandidateBtn = createStyledButton("Edit Candidate");
        JButton deleteCandidateBtn = createStyledButton("Delete Candidate");
        JButton displayCandidatesBtn = createStyledButton("View Candidates");
        JButton addVoterBtn = createStyledButton("Add Voter");
        JButton editVoterBtn = createStyledButton("Edit Voter");
        JButton deleteVoterBtn = createStyledButton("Delete Voter");
        JButton displayVotersBtn = createStyledButton("View Voters");
        JButton startVoteBtn = createStyledButton("Start Voting");
        JButton stopVoteBtn = createStyledButton("Stop Voting");
        JButton resetVoteBtn = createStyledButton("Reset Votes");
    JButton managePositionsBtn = createStyledButton("Manage Positions");
        JButton backBtn = createStyledButton("Back");

    panel.add(addCandidateBtn); panel.add(editCandidateBtn); panel.add(deleteCandidateBtn); panel.add(displayCandidatesBtn);
    panel.add(addVoterBtn); panel.add(editVoterBtn); panel.add(deleteVoterBtn); panel.add(displayVotersBtn);
    panel.add(startVoteBtn); panel.add(stopVoteBtn); panel.add(resetVoteBtn); panel.add(managePositionsBtn);
    panel.add(new JLabel()); panel.add(new JLabel()); panel.add(new JLabel()); panel.add(backBtn);

        addCandidateBtn.addActionListener(e -> addCandidateAction());
        editCandidateBtn.addActionListener(e -> editCandidateAction());
        deleteCandidateBtn.addActionListener(e -> deleteCandidateAction());
    displayCandidatesBtn.addActionListener(e -> showCandidateList(false));

        addVoterBtn.addActionListener(e -> addVoterAction());
    editVoterBtn.addActionListener(e -> manageVotersDialog());
    deleteVoterBtn.addActionListener(e -> manageVotersDialog());
        displayVotersBtn.addActionListener(e -> showVoterList());

        startVoteBtn.addActionListener(e -> { adminLogic.startVoting(); showStyledDialog("Success","Voting Started",false); });
        stopVoteBtn.addActionListener(e -> { adminLogic.stopVoting(); showStyledDialog("Success","Voting Stopped",false); });
        resetVoteBtn.addActionListener(e -> { adminLogic.resetVotes(); showStyledDialog("Success","Votes Reset",false); });
    managePositionsBtn.addActionListener(e -> managePositionsDialog());

        backBtn.addActionListener(e -> cardLayout.show(mainPanel,"HOME"));
        return panel;
    }

    // ---------------- Position management ----------------
    private ArrayList<String> getAllPositions() {
        ArrayList<String> positions = new ArrayList<>();
        String createSql = "CREATE TABLE IF NOT EXISTS positions (id INT AUTO_INCREMENT PRIMARY KEY, name VARCHAR(255) UNIQUE)";
        String sel = "SELECT name FROM positions ORDER BY name";
        try (Connection con = DBUtil.getConnection(); Statement st = con.createStatement()) {
            st.executeUpdate(createSql);
            try (ResultSet rs = st.executeQuery(sel)) {
                while (rs.next()) positions.add(rs.getString("name"));
            }
        } catch (Exception e) { e.printStackTrace(); }
        return positions;
    }

    private void addPositionToDB(String name) throws SQLException, ClassNotFoundException {
        String ins = "INSERT IGNORE INTO positions(name) VALUES(?)";
        try (Connection con = DBUtil.getConnection(); PreparedStatement ps = con.prepareStatement(ins)) {
            ps.setString(1, name);
            ps.executeUpdate();
        }
    }

    private void deletePositionFromDB(String name) throws SQLException, ClassNotFoundException {
        String cntSql = "SELECT COUNT(*) AS c FROM candidates WHERE position = ?";
        String delSql = "DELETE FROM positions WHERE name = ?";
        try (Connection con = DBUtil.getConnection();
             PreparedStatement pc = con.prepareStatement(cntSql);
             PreparedStatement pd = con.prepareStatement(delSql)) {
            pc.setString(1, name);
            try (ResultSet rs = pc.executeQuery()) {
                if (rs.next() && rs.getInt("c") > 0) {
                    throw new SQLException("Position has candidates and cannot be deleted");
                }
            }
            pd.setString(1, name);
            pd.executeUpdate();
        }
    }

    private void managePositionsDialog() {
        // present a small management dialog allowing Add / Delete / Close
        String[] actions = new String[]{"Add Position", "Delete Position", "Close"};
        while (true) {
            int sel = JOptionPane.showOptionDialog(this, "Manage positions:", "Positions",
                    JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null, actions, actions[0]);
            if (sel == 0) {
                String pos = showTextInput("Add Position", "Enter new position name:", "");
                if (pos == null) continue;
                pos = pos.trim();
                if (pos.isEmpty()) { showStyledDialog("Error","Position name cannot be empty",false); continue; }
                try { addPositionToDB(pos); showStyledDialog("Success","Position added.",true); }
                catch (Exception ex) { ex.printStackTrace(); showStyledDialog("Error","Failed to add position.",false); }
            } else if (sel == 1) {
                ArrayList<String> positions = getAllPositions();
                if (positions.isEmpty()) { showStyledDialog("Info","No positions to delete.",true); continue; }
                Object[] opts = positions.toArray();
                String chosen = showOptionInput("Delete Position", "Select position to delete:", opts, opts[0]);
                if (chosen == null) continue;
                int conf = JOptionPane.showConfirmDialog(this, "Delete position '" + chosen + "'? This cannot be undone.", "Confirm Delete", JOptionPane.YES_NO_OPTION);
                if (conf != JOptionPane.YES_OPTION) continue;
                try {
                    deletePositionFromDB(chosen);
                    showStyledDialog("Success", "Position deleted.", true);
                } catch (SQLException sq) {
                    // likely because candidates reference this position
                    showStyledDialog("Error", "Cannot delete position: it has candidates or an error occurred.", false);
                } catch (Exception ex) {
                    ex.printStackTrace(); showStyledDialog("Error","Failed to delete position.",false);
                }
            } else break; // Close or dialog dismissed
        }
    }

    private JPanel getVoterLoginPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
    panel.setBackground(bgDark);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10,10,10,10);

            JLabel nameLabel = new JLabel("Name:"); nameLabel.setForeground(Color.WHITE);
            JLabel passLabel = new JLabel("Password:"); passLabel.setForeground(Color.WHITE);
        JTextField nameField = new JTextField(15);
        JPasswordField passField = new JPasswordField(15);
    JButton loginBtn = createPrimaryButton("Login");
        JButton backBtn = createPrimaryButton("Back");

        gbc.gridx=0; gbc.gridy=0; panel.add(nameLabel, gbc);
        gbc.gridx=1; panel.add(nameField, gbc);
        gbc.gridx=0; gbc.gridy=1; panel.add(passLabel, gbc);
        gbc.gridx=1; panel.add(passField, gbc);
        gbc.gridx=0; gbc.gridy=2; panel.add(loginBtn, gbc);
        gbc.gridx=1; panel.add(backBtn, gbc);

        loginBtn.addActionListener(e -> {
            String name = nameField.getText();
            String pass = new String(passField.getPassword());

            // --- Voter login using Admin logic ---
            Voter v = adminLogic.getAllVoters().stream()
                    .filter(x -> x.getName().equals(name) && x.getPassword().equals(pass))
                    .findFirst()
                    .orElse(null);

            if(v != null){
                currentVoter = v;
                showStyledDialog("Success","Login Successful", false);
                cardLayout.show(mainPanel,"VOTER_PANEL");
            } else showStyledDialog("Error","Login Failed", false);
        });

        backBtn.addActionListener(e -> cardLayout.show(mainPanel,"HOME"));
        return panel;
    }

    private JPanel getVoterPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(bgDark);

        JPanel topPanel = new JPanel();
        topPanel.setBackground(bgDark);

        JButton voteBtn = createPrimaryButton("Vote");
        JButton viewResultsBtn = createPrimaryButton("View Results");
        JButton backBtn = createPrimaryButton("Back");

        topPanel.add(voteBtn);
        topPanel.add(viewResultsBtn);
        topPanel.add(backBtn);
        panel.add(topPanel, BorderLayout.NORTH);

        JLabel statusLabel = new JLabel();
        statusLabel.setHorizontalAlignment(SwingConstants.CENTER);
        statusLabel.setOpaque(true);
        statusLabel.setBackground(bgDark);
        statusLabel.setForeground(Color.WHITE);
        statusLabel.setPreferredSize(new Dimension(0,28));
        panel.add(statusLabel, BorderLayout.SOUTH);

        Timer timer = new Timer(1000, e -> {
            boolean isActive = updateVotingStatus(statusLabel);
            viewResultsBtn.setEnabled(!isActive);
            voteBtn.setEnabled(isActive && currentVoter != null && !currentVoter.isHasVoted());
        });
        timer.start();

        voteBtn.addActionListener(e -> votePanelAction());
        backBtn.addActionListener(e -> cardLayout.show(mainPanel, "HOME"));
        viewResultsBtn.addActionListener(e -> showCandidateList(false));

        // Removed manage positions button from voter panel

        return panel;
    }

    private boolean updateVotingStatus(JLabel statusLabel) {
        boolean isActive = false;
        try (Connection con = DBUtil.getConnection();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery("SELECT is_active FROM voting_status WHERE id=1")) {

            if(rs.next()) {
                isActive = rs.getBoolean("is_active");
                statusLabel.setText(isActive ? "Voting is ACTIVE" : "Voting is INACTIVE");
            } else {
                st.executeUpdate("INSERT INTO voting_status(id,is_active) VALUES(1,0)");
                statusLabel.setText("Voting is INACTIVE");
            }
        } catch (Exception e) { e.printStackTrace(); }
        return isActive;
    }

    // ---------------- Actions ----------------
    private void addCandidateAction() {
        JTextField nameF = new JTextField(); JTextField symF = new JTextField();
        JTextField ageF = new JTextField(); JTextField posF = new JTextField();
        JFileChooser fc = new JFileChooser();
        fc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Image files","jpg","png","jpeg"));
        int r = fc.showOpenDialog(this);
        String path = null;
        if(r==JFileChooser.APPROVE_OPTION) path = fc.getSelectedFile().getAbsolutePath();

    JPanel panel = new JPanel(new GridLayout(0,1));
    panel.setBackground(bgDark);
    JLabel nameLbl = new JLabel("Name:"); nameLbl.setForeground(Color.WHITE); nameLbl.setFont(new Font("Arial", Font.PLAIN, 16));
    JLabel symLbl = new JLabel("Symbol:"); symLbl.setForeground(Color.WHITE); symLbl.setFont(new Font("Arial", Font.PLAIN, 16));
    JLabel ageLbl = new JLabel("Age:"); ageLbl.setForeground(Color.WHITE); ageLbl.setFont(new Font("Arial", Font.PLAIN, 16));
    nameF.setBackground(Color.DARK_GRAY); nameF.setForeground(Color.WHITE); nameF.setFont(new Font("Arial", Font.PLAIN, 16));
    symF.setBackground(Color.DARK_GRAY); symF.setForeground(Color.WHITE); symF.setFont(new Font("Arial", Font.PLAIN, 16));
    ageF.setBackground(Color.DARK_GRAY); ageF.setForeground(Color.WHITE); ageF.setFont(new Font("Arial", Font.PLAIN, 16));
    panel.add(nameLbl); panel.add(nameF);
    panel.add(symLbl); panel.add(symF);
    panel.add(ageLbl); panel.add(ageF);

    // Positions dropdown (editable) populated from DB positions table
    ArrayList<String> positions = getAllPositions();
    JComboBox<String> posCombo = new JComboBox<>();
    posCombo.addItem("-- Select or type position --");
    for (String p : positions) posCombo.addItem(p);
    posCombo.setEditable(true);
    JLabel posLbl = new JLabel("Position:"); posLbl.setForeground(Color.WHITE); posLbl.setFont(new Font("Arial", Font.PLAIN, 16));
    posCombo.setBackground(Color.DARK_GRAY); posCombo.setForeground(Color.WHITE); posCombo.setFont(new Font("Arial", Font.PLAIN, 16));
    panel.add(posLbl); panel.add(posCombo);

    int res = JOptionPane.showConfirmDialog(this,panel,"Add Candidate",JOptionPane.OK_CANCEL_OPTION);
        if(res==JOptionPane.OK_OPTION){
            File photoFile = path != null ? new File(path) : null;
            String bio = showTextInput("Candidate Bio", "Enter short bio for candidate:", "");
            String chosenPos = ((String) posCombo.getSelectedItem()).trim();
            if (chosenPos.equals("-- Select or type position --") || chosenPos.isEmpty()) {
                showStyledDialog("Error","Please select or type a position.",false);
                return;
            }
            // ensure position exists in DB
            try { addPositionToDB(chosenPos); } catch (Exception ex) { /* ignore */ }
            // VotingLogic2.Admin.addCandidate expects (String name, String symbol, int age, String position, File photoFile, String bio)
            adminLogic.addCandidate(nameF.getText(), symF.getText(),
                    Integer.parseInt(ageF.getText()), chosenPos, photoFile, bio);
            showStyledDialog("Success","Candidate Added",false);
        }
    }

    private void editCandidateAction() { showCandidateList(true); }
    private void deleteCandidateAction() { showCandidateList(true); }
    private void addVoterAction() {
        JTextField nameF = new JTextField(); JTextField passF = new JTextField();
        JPanel panel = new JPanel(new GridLayout(0,1));
        panel.setBackground(bgDark);
    JLabel nLab = new JLabel("Name:"); nLab.setForeground(Color.WHITE); nLab.setFont(new Font("Arial", Font.PLAIN, 16));
    JLabel pLab = new JLabel("Password:"); pLab.setForeground(Color.WHITE); pLab.setFont(new Font("Arial", Font.PLAIN, 16));
    nameF.setBackground(Color.DARK_GRAY); nameF.setForeground(Color.WHITE); nameF.setFont(new Font("Arial", Font.PLAIN, 16));
    passF.setBackground(Color.DARK_GRAY); passF.setForeground(Color.WHITE); passF.setFont(new Font("Arial", Font.PLAIN, 16));
        panel.add(nLab); panel.add(nameF);
        panel.add(pLab); panel.add(passF);
        int res = JOptionPane.showConfirmDialog(this,panel,"Add Voter",JOptionPane.OK_CANCEL_OPTION);
        if(res==JOptionPane.OK_OPTION){
            // VotingLogic2 provides Voter.register(name, password, dob). Prompt for DOB.
            String dobStr = showTextInput("DOB", "Enter DOB (YYYY-MM-DD):", "2000-01-01");
            try {
                java.time.LocalDate dob = java.time.LocalDate.parse(dobStr);
                boolean ok = Voter.register(nameF.getText(), passF.getText(), dob);
                if(ok) showStyledDialog("Success","Voter Added (awaiting verification)",false);
                else showStyledDialog("Error","Failed to add voter",false);
            } catch(Exception ex) {
                showStyledDialog("Error","Invalid DOB format. Use YYYY-MM-DD.",false);
            }
        }
    }
    private void editVoterAction() { showVoterList(); }
    private void deleteVoterAction() { showVoterList(); }
    private void manageVotersDialog() {
        ArrayList<Voter> list = adminLogic.getAllVoters();
        if (list == null || list.isEmpty()) { showStyledDialog("Info", "No voters found.", true); return; }
        String[] opts = list.stream().map(v -> v.getId() + ": " + v.getName() + " (Verified:" + v.isVerified() + ")").toArray(String[]::new);
        String sel = showOptionInput("Voters", "Select a voter:", opts, opts[0]);
        if (sel == null) return;
        int selId;
        try { selId = Integer.parseInt(sel.split(":")[0].trim()); } catch (Exception ex) { return; }
        Voter selected = null; for (Voter v : list) if (v.getId() == selId) { selected = v; break; }
        if (selected == null) return;
        String[] acts = new String[]{"View Details", "Edit", selected.isVerified() ? "Unverify" : "Verify", "Delete", "Cancel"};
        int act = JOptionPane.showOptionDialog(this, "Choose action for " + selected.getName(), "Voter",
                JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null, acts, acts[0]);
        if (act == 0) {
            StringBuilder sb = new StringBuilder();
            sb.append("ID: ").append(selected.getId()).append('\n');
            sb.append("Name: ").append(selected.getName()).append('\n');
            sb.append("Has Voted: ").append(selected.isHasVoted()).append('\n');
            sb.append("Verified: ").append(selected.isVerified()).append('\n');
            sb.append("DOB: ").append(selected.getDob() != null ? selected.getDob().toString() : "").append('\n');
            JTextArea ta = new JTextArea(sb.toString()); ta.setEditable(false); ta.setFont(new Font("Arial", Font.PLAIN, 16)); ta.setBackground(Color.DARK_GRAY); ta.setForeground(Color.WHITE);
            JScrollPane sp = new JScrollPane(ta); sp.setPreferredSize(new Dimension(500,200));
            JOptionPane.showMessageDialog(this, sp, "Voter Details", JOptionPane.PLAIN_MESSAGE);
        } else if (act == 1) {
            // Edit
            String newName = showTextInput("Edit Voter", "Name:", selected.getName()); if (newName == null) return;
            String newPass = showTextInput("Edit Voter", "Password:", selected.getPassword()); if (newPass == null) return;
            String dobStr = showTextInput("Edit Voter", "DOB (YYYY-MM-DD):", selected.getDob() != null ? selected.getDob().toString() : "2000-01-01"); if (dobStr == null) return;
            java.time.LocalDate dob = null; try { dob = java.time.LocalDate.parse(dobStr); } catch (Exception e) { showStyledDialog("Error","Invalid DOB format.",false); return; }
            // keep hasVoted and verified as is (or you could add inputs)
            try { adminLogic.editVoter(selected.getId(), newName, newPass, dob, selected.isHasVoted(), selected.isVerified()); showStyledDialog("Success","Voter updated.",true); }
            catch (Exception ex) { ex.printStackTrace(); showStyledDialog("Error","Failed to update voter.",false); }
        } else if (act == 2) {
            // Verify/Unverify
            boolean to = !selected.isVerified();
            try { adminLogic.setVoterVerified(selected.getId(), to); showStyledDialog("Success", (to?"Voter verified.":"Voter unverified."), true); }
            catch (Exception ex) { ex.printStackTrace(); showStyledDialog("Error","Failed to change verification.",false); }
        } else if (act == 3) {
            int conf = JOptionPane.showConfirmDialog(this, "Are you sure you want to delete " + selected.getName() + "?", "Confirm", JOptionPane.YES_NO_OPTION);
            if (conf == JOptionPane.YES_OPTION) {
                try { adminLogic.deleteVoter(selected.getId()); showStyledDialog("Success","Voter deleted.",true); }
                catch (Exception ex) { ex.printStackTrace(); showStyledDialog("Error","Failed to delete voter.",false); }
            }
        }
    }
    private void showCandidateList(boolean forEdit) {
        // When viewing (not editing), allow filtering by position
        ArrayList<String> positions = getAllPositions();
        String[] posOptions = new String[positions.size() + 1];
        posOptions[0] = "All";
        for (int i = 0; i < positions.size(); i++) posOptions[i + 1] = positions.get(i);

        ArrayList<Candidate> list = null;
        if (!forEdit) {
            String chosenPos = (String) JOptionPane.showInputDialog(this, "Filter by position:", "Positions",
                    JOptionPane.PLAIN_MESSAGE, null, posOptions, posOptions[0]);
            if (chosenPos == null) return;
            if (chosenPos.equals("All")) list = adminLogic.getAllCandidates();
            else list = adminLogic.getCandidatesByPosition(chosenPos);
        } else {
            list = adminLogic.getAllCandidates();
        }

        if (list == null || list.isEmpty()) {
            showStyledDialog("Info", "No candidates found.", true);
            return;
        }

        String[] options = list.stream().map(x -> x.id + ": " + x.name + " (" + x.symbol + ") Votes:"+x.votes).toArray(String[]::new);

        if (!forEdit) {
            // Show full details for all candidates with photos in a scrollable panel
            JPanel container = new JPanel();
            container.setLayout(new BoxLayout(container, BoxLayout.Y_AXIS));
            container.setBackground(bgDark);

            for (Candidate c : list) {
                JPanel row = new JPanel(new BorderLayout(12, 12));
                row.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
                row.setBackground(bgDark);

                // Image (left)
                JLabel imgLabel = new JLabel();
                imgLabel.setHorizontalAlignment(SwingConstants.LEFT);
                imgLabel.setPreferredSize(new Dimension(140,140));
                if (c.photo != null && c.photo.length > 0) {
                    try {
                        ImageIcon icon = new ImageIcon(c.photo);
                        Image img = icon.getImage().getScaledInstance(140, 140, Image.SCALE_SMOOTH);
                        imgLabel.setIcon(new ImageIcon(img));
                    } catch (Exception ex) { imgLabel.setText("No photo"); }
                } else {
                    imgLabel.setText("No photo");
                }

                // Right side: details panel (name, symbol, meta) and bio next to image
                JPanel right = new JPanel();
                right.setLayout(new BorderLayout(6,6));
                right.setBackground(bgDark);

                // Top details (labels)
                JPanel meta = new JPanel(new GridLayout(0,1));
                meta.setBackground(bgDark);
                JLabel nameL = new JLabel("Name: " + c.name);
                nameL.setFont(new Font("Arial", Font.BOLD, 22));
                nameL.setForeground(accent1);
                JLabel symbolL = new JLabel("Symbol: " + c.symbol);
                symbolL.setFont(new Font("Arial", Font.PLAIN, 18)); symbolL.setForeground(Color.WHITE);
                JLabel posL = new JLabel("Position: " + c.position);
                posL.setFont(new Font("Arial", Font.PLAIN, 18)); posL.setForeground(Color.WHITE);
                JLabel ageL = new JLabel("Age: " + c.age + "    Votes: " + c.votes);
                ageL.setFont(new Font("Arial", Font.PLAIN, 18)); ageL.setForeground(Color.WHITE);
                meta.add(nameL); meta.add(symbolL); meta.add(posL); meta.add(ageL);

                // Bio area (wrapped)
                JTextArea bioArea = new JTextArea(c.bio != null ? c.bio : "");
                bioArea.setLineWrap(true);
                bioArea.setWrapStyleWord(true);
                bioArea.setEditable(false);
                bioArea.setFont(new Font("Arial", Font.PLAIN, 16));
                bioArea.setBackground(Color.DARK_GRAY);
                bioArea.setForeground(Color.WHITE);

                right.add(meta, BorderLayout.NORTH);
                JScrollPane bioSp = new JScrollPane(bioArea);
                bioSp.getViewport().setBackground(Color.DARK_GRAY);
                right.add(bioSp, BorderLayout.CENTER);

                row.add(imgLabel, BorderLayout.WEST);
                row.add(right, BorderLayout.CENTER);
                row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 170));

                container.add(row);
                container.add(Box.createRigidArea(new Dimension(0,8)));
                container.add(new JSeparator());
            }

            JScrollPane sp = new JScrollPane(container);
            sp.getViewport().setBackground(bgDark);
            sp.setPreferredSize(new Dimension(700, 400));
            JOptionPane.showMessageDialog(this, sp, "Candidates - Details", JOptionPane.PLAIN_MESSAGE);
            return;
        }

        // Edit mode: select a candidate and perform actions (View/Edit/Delete)
    String sel = showOptionInput("Candidates", "Select a candidate:", options, options[0]);
        if (sel == null) return;

        // find selected candidate by parsing id before ':'
        int selId;
        try {
            selId = Integer.parseInt(sel.split(":")[0].trim());
        } catch (Exception ex) { return; }

        Candidate selected = null;
        for (Candidate c : list) if (c.id == selId) { selected = c; break; }
        if (selected == null) return;

        String[] acts = new String[]{"View Details", "Edit", "Delete", "Cancel"};
        int act = JOptionPane.showOptionDialog(this, "Choose action for " + selected.name, "Candidate",
                JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null, acts, acts[0]);

        if (act == 0) {
            // View single candidate
            StringBuilder sb = new StringBuilder();
            sb.append("ID: ").append(selected.id).append('\n');
            sb.append("Name: ").append(selected.name).append('\n');
            sb.append("Symbol: ").append(selected.symbol).append('\n');
            sb.append("Position: ").append(selected.position).append('\n');
            sb.append("Age: ").append(selected.age).append('\n');
            sb.append("Votes: ").append(selected.votes).append('\n');
            sb.append("Bio: ").append(selected.bio != null ? selected.bio : "").append('\n');

            JPanel p = new JPanel(new BorderLayout(12,12));
            p.setBackground(bgDark);
            p.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));

            JLabel imgLabel = new JLabel();
            imgLabel.setPreferredSize(new Dimension(180,180));
            imgLabel.setHorizontalAlignment(SwingConstants.LEFT);
            if (selected.photo != null && selected.photo.length > 0) {
                try {
                    ImageIcon icon = new ImageIcon(selected.photo);
                    Image img = icon.getImage().getScaledInstance(180, 180, Image.SCALE_SMOOTH);
                    imgLabel.setIcon(new ImageIcon(img));
                } catch (Exception ex) { imgLabel.setText("No photo"); }
            } else imgLabel.setText("No photo");

            JPanel right = new JPanel(new BorderLayout(6,6));
            right.setBackground(bgDark);
            JPanel meta = new JPanel(new GridLayout(0,1));
            meta.setBackground(bgDark);
            JLabel nameL = new JLabel("Name: " + selected.name);
            nameL.setFont(new Font("Arial", Font.BOLD, 24));
            nameL.setForeground(accent1);
            JLabel symbolL = new JLabel("Symbol: " + selected.symbol);
            symbolL.setFont(new Font("Arial", Font.PLAIN, 18)); symbolL.setForeground(Color.WHITE);
            JLabel posL = new JLabel("Position: " + selected.position);
            posL.setFont(new Font("Arial", Font.PLAIN, 18)); posL.setForeground(Color.WHITE);
            JLabel ageL = new JLabel("Age: " + selected.age + "    Votes: " + selected.votes);
            ageL.setFont(new Font("Arial", Font.PLAIN, 18)); ageL.setForeground(Color.WHITE);
            meta.add(nameL); meta.add(symbolL); meta.add(posL); meta.add(ageL);

            JTextArea ta = new JTextArea(sb.toString());
            ta.setEditable(false);
            ta.setFont(new Font("Arial", Font.PLAIN, 18));
            ta.setLineWrap(true); ta.setWrapStyleWord(true);
            ta.setCaretPosition(0);
            ta.setBackground(Color.DARK_GRAY);
            ta.setForeground(Color.WHITE);

            right.add(meta, BorderLayout.NORTH);
            JScrollPane taSp = new JScrollPane(ta);
            taSp.getViewport().setBackground(Color.DARK_GRAY);
            right.add(taSp, BorderLayout.CENTER);

            p.add(imgLabel, BorderLayout.WEST);
            p.add(right, BorderLayout.CENTER);
            JOptionPane.showMessageDialog(this, p, "Candidate Details", JOptionPane.PLAIN_MESSAGE);
            return;
        } else if (act == 1) {
            // Edit flow - prompt for new values (leave blank to keep)
            String newName = showTextInput("Edit Candidate", "Name:", selected.name);
            if (newName == null) return; // cancelled
            String newSymbol = showTextInput("Edit Candidate", "Symbol:", selected.symbol);
            if (newSymbol == null) return;
            String ageStr = showTextInput("Edit Candidate", "Age (leave blank to keep):", String.valueOf(selected.age));
            if (ageStr == null) return;
            Integer newAge = null;
            try { if (!ageStr.trim().isEmpty()) newAge = Integer.parseInt(ageStr.trim()); } catch (Exception e) { showStyledDialog("Error","Invalid age.",false); return; }

            String newPos = showTextInput("Edit Candidate", "Position:", selected.position);
            if (newPos == null) return;
            // Use a file chooser for selecting a new photo (optional)
            File newPhoto = null;
            int choose = JOptionPane.showConfirmDialog(this, "Do you want to change the photo?", "Photo", JOptionPane.YES_NO_CANCEL_OPTION);
            if (choose == JOptionPane.YES_OPTION) {
                JFileChooser jfc = new JFileChooser();
                jfc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Image files","jpg","png","jpeg"));
                int r = jfc.showOpenDialog(this);
                if (r == JFileChooser.APPROVE_OPTION) newPhoto = jfc.getSelectedFile();
            } else if (choose == JOptionPane.CANCEL_OPTION) {
                return; // cancel edit
            }
            String newBio = showTextInput("Edit Candidate", "Bio:", selected.bio != null ? selected.bio : "");
            if (newBio == null) return;

            try {
                adminLogic.editCandidate(selected.id, newName, newSymbol, newAge, newPos, newPhoto, newBio);
                showStyledDialog("Success", "Candidate updated.", true);
            } catch (Exception ex) { ex.printStackTrace(); showStyledDialog("Error","Failed to update candidate.",false); }

        } else if (act == 2) {
            int conf = JOptionPane.showConfirmDialog(this, "Are you sure you want to delete " + selected.name + "?", "Confirm", JOptionPane.YES_NO_OPTION);
            if (conf == JOptionPane.YES_OPTION) {
                try { adminLogic.deleteCandidate(selected.id); showStyledDialog("Success","Candidate deleted.",true); }
                catch (Exception ex) { ex.printStackTrace(); showStyledDialog("Error","Failed to delete candidate.",false); }
            }
        }
    }
    private void showVoterList() {
        ArrayList<Voter> list = adminLogic.getAllVoters();
        if (list == null || list.isEmpty()) {
            showStyledDialog("Info", "No voters found.", true);
            return;
        }
        StringBuilder sb = new StringBuilder();
        for (Voter v : list) {
            sb.append("ID: ").append(v.getId()).append('\n');
            sb.append("Name: ").append(v.getName()).append('\n');
            sb.append("Has Voted: ").append(v.isHasVoted()).append('\n');
            sb.append("Verified: ").append(v.isVerified()).append('\n');
            sb.append("DOB: ").append(v.getDob() != null ? v.getDob().toString() : "").append('\n');
            sb.append("-------------------------").append('\n');
        }
        JTextArea ta = new JTextArea(sb.toString());
        ta.setEditable(false);
        ta.setFont(new Font("Arial", Font.PLAIN, 14));
        ta.setCaretPosition(0);
        ta.setBackground(Color.DARK_GRAY);
        ta.setForeground(Color.WHITE);
        JScrollPane sp = new JScrollPane(ta);
        sp.getViewport().setBackground(Color.DARK_GRAY);
        sp.setPreferredSize(new Dimension(600, 300));
        JOptionPane.showMessageDialog(this, sp, "Voters", JOptionPane.PLAIN_MESSAGE);
    }

    // New vote flow: show candidates grouped by position, sectioned list
    private void votePanelAction() {
        ArrayList<String> positions = getAllPositions();
        if (positions.isEmpty()) { showStyledDialog("Info", "No positions available.", true); return; }
        JPanel main = new JPanel();
        main.setLayout(new BoxLayout(main, BoxLayout.Y_AXIS));
        main.setBackground(bgDark);

        ArrayList<Candidate> allCandidates = adminLogic.getAllCandidates();
        boolean any = false;
        // Store ButtonGroups for each position
        java.util.Map<String, ButtonGroup> positionGroups = new java.util.LinkedHashMap<>();
        java.util.Map<String, JPanel> positionPanels = new java.util.LinkedHashMap<>();
        for (String pos : positions) {
            ArrayList<Candidate> candidates = adminLogic.getCandidatesByPosition(pos);
            if (candidates.isEmpty()) continue;
            any = true;
            JLabel posLabel = new JLabel(pos);
            posLabel.setFont(new Font("Arial", Font.BOLD, 20));
            posLabel.setForeground(accent1);
            main.add(posLabel);

            ButtonGroup group = new ButtonGroup();
            JPanel candPanel = new JPanel();
            candPanel.setLayout(new BoxLayout(candPanel, BoxLayout.Y_AXIS));
            candPanel.setBackground(bgDark);
            for (Candidate c : candidates) {
                JRadioButton rb = new JRadioButton(c.name + " (" + c.symbol + "), Age: " + c.age);
                rb.setFont(new Font("Arial", Font.PLAIN, 16));
                rb.setForeground(Color.WHITE);
                rb.setBackground(bgDark);
                rb.putClientProperty("cid", c.id);
                group.add(rb);
                candPanel.add(rb);
            }
            positionGroups.put(pos, group);
            positionPanels.put(pos, candPanel);
            main.add(candPanel);
            main.add(Box.createRigidArea(new Dimension(0,12)));
        }
        if (!any) { showStyledDialog("Info", "No candidates available.", true); return; }

        int res = JOptionPane.showConfirmDialog(this, main, "Vote - Select Candidate", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (res == JOptionPane.OK_OPTION) {
            // For each position, check if a candidate is selected
            java.util.List<Integer> selectedIds = new java.util.ArrayList<>();
            java.util.List<String> selectedNames = new java.util.ArrayList<>();
            for (String pos : positions) {
                ButtonGroup group = positionGroups.get(pos);
                if (group == null) continue;
                for (Enumeration<AbstractButton> e = group.getElements(); e.hasMoreElements();) {
                    AbstractButton ab = e.nextElement();
                    if (ab.isSelected()) {
                        Object cid = ab.getClientProperty("cid");
                        if (cid instanceof Integer) {
                            selectedIds.add((Integer) cid);
                            selectedNames.add(pos + ": " + ab.getText());
                        }
                    }
                }
            }
            if (selectedIds.isEmpty()) { showStyledDialog("Error", "Please select a candidate for at least one position.", false); return; }

            // Show confirmation dialog with selected candidates
            StringBuilder confMsg = new StringBuilder("You have selected:\n\n");
            for (String s : selectedNames) confMsg.append(s).append("\n");
            confMsg.append("\nDo you want to submit your vote?");
            int confirm = JOptionPane.showConfirmDialog(this, confMsg.toString(), "Confirm Vote", JOptionPane.YES_NO_OPTION);
            if (confirm != JOptionPane.YES_OPTION) return;

            // Only one candidate per position can be selected due to ButtonGroup
            try (Connection con = DBUtil.getConnection()) {
                for (int selectedId : selectedIds) {
                    try (PreparedStatement pst1 = con.prepareStatement("UPDATE candidates SET votes=votes+1 WHERE id=?")) {
                        pst1.setInt(1, selectedId); pst1.executeUpdate();
                    }
                }
                try (PreparedStatement pst2 = con.prepareStatement("UPDATE voters SET has_voted=1 WHERE id=?")) {
                    pst2.setInt(1, currentVoter.getId()); pst2.executeUpdate();
                }
                currentVoter = new Voter(currentVoter.getId(), currentVoter.getName(),
                    currentVoter.getPassword(), true, currentVoter.isVerified(), currentVoter.getDob());
                showStyledDialog("Success","Vote Cast Successfully", false);
            } catch(Exception ex) { ex.printStackTrace(); }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(VotingGUI2::new);
    }
}
