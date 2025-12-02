import java.sql.*;
import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.io.File;
import java.io.FileInputStream;

// ---------------- User superclass ----------------
class User {
    protected int id;
    protected String name;
    protected String password;

    public User(int id, String name, String password) {
        this.id = id;
        this.name = name;
        this.password = password;
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public String getPassword() { return password; }
}

// ---------------- Candidate class ----------------
class Candidate {
    public int id;
    public String name;
    public String symbol;
    public int age;
    public String position;
    public byte[] photo;  // store actual image as bytes
    public String bio;
    public int votes;

    public Candidate(int id, String name, String symbol, int age, String position, byte[] photo, String bio, int votes) {
        this.id = id;
        this.name = name;
        this.symbol = symbol;
        this.age = age;
        this.position = position;
        this.photo = photo;
        this.bio = bio;
        this.votes = votes;
    }
}

// ---------------- Voter class ----------------
class Voter extends User {
    private boolean hasVoted;
    private boolean verified;
    private LocalDate dob;

    public Voter(int id, String name, String password, boolean hasVoted, boolean verified, LocalDate dob) {
        super(id, name, password);
        this.hasVoted = hasVoted;
        this.verified = verified;
        this.dob = dob;
    }

    // Login: requires verified account
    public static Voter login(String name, String password) {
        String sql = "SELECT * FROM voters WHERE name=? AND password=?";
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setString(2, password);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    boolean verified = rs.getBoolean("verified");
                    if (!verified) {
                        System.out.println("Account not verified by admin yet.");
                        return null;
                    }
                    return new Voter(
                            rs.getInt("id"),
                            rs.getString("name"),
                            rs.getString("password"),
                            rs.getBoolean("has_voted"),
                            verified,
                            rs.getDate("dob").toLocalDate()
                    );
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
        return null;
    }

    // Register with DOB; only allow if age >= 18
    public static boolean register(String name, String password, LocalDate dob) {
        int age = Period.between(dob, LocalDate.now()).getYears();
        if (age < 18) {
            System.out.println("You must be at least 18 to register. Age: " + age);
            return false;
        }

        String sql = "INSERT INTO voters(name, password, dob, has_voted, verified) VALUES(?, ?, ?, 0, 0)";
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setString(2, password);
            ps.setDate(3, Date.valueOf(dob));
            ps.executeUpdate();
            System.out.println("Registration successful. Awaiting admin verification.");
            return true;
        } catch (SQLIntegrityConstraintViolationException dup) {
            System.out.println("User with this name already exists.");
            return false;
        } catch (Exception e) { e.printStackTrace(); return false; }
    }

    // Cast vote (only if verified and voting active)
    public void vote(int candidateId) {
        if (!this.verified) {
            System.out.println("Your account is not verified by admin.");
            return;
        }
        if (this.hasVoted) {
            System.out.println("You have already voted!");
            return;
        }

        String checkActive = "SELECT is_active FROM voting_status WHERE id=1";
        String updCandidate = "UPDATE candidates SET votes = votes + 1 WHERE id = ?";
        String updVoter = "UPDATE voters SET has_voted = 1 WHERE id = ?";

        try (Connection con = DBUtil.getConnection();
             PreparedStatement psCheck = con.prepareStatement(checkActive);
             ResultSet rs = psCheck.executeQuery()) {

            if (rs.next() && !rs.getBoolean("is_active")) {
                System.out.println("Voting is not active now!");
                return;
            }

            con.setAutoCommit(false);
            try (PreparedStatement ps1 = con.prepareStatement(updCandidate);
                 PreparedStatement ps2 = con.prepareStatement(updVoter)) {

                ps1.setInt(1, candidateId);
                ps1.executeUpdate();

                ps2.setInt(1, this.id);
                ps2.executeUpdate();

                con.commit();
                this.hasVoted = true;
                System.out.println("Vote cast successfully!");
            } catch (Exception e) {
                con.rollback();
                throw e;
            } finally {
                con.setAutoCommit(true);
            }

        } catch (Exception e) { e.printStackTrace(); }
    }

    public boolean isHasVoted() { return hasVoted; }
    public boolean isVerified() { return verified; }
    public LocalDate getDob() { return dob; }
}

// ---------------- Admin class ----------------
class Admin extends User {
    // Default admin credentials from DB
    public Admin() {
        super(1, "admin", "admin123");
    }

    public boolean login(String username, String pwd) {
        String sql = "SELECT * FROM admin WHERE username=? AND password=?";
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, pwd);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (Exception e) { e.printStackTrace(); return false; }
    }

    public void startVoting() {
        String sql = "UPDATE voting_status SET is_active=1 WHERE id=1";
        try (Connection con = DBUtil.getConnection();
             Statement st = con.createStatement()) {
            st.executeUpdate(sql);
            System.out.println("Voting started!");
        } catch (Exception e) { e.printStackTrace(); }
    }

    public void stopVoting() {
        String sql = "UPDATE voting_status SET is_active=0 WHERE id=1";
        try (Connection con = DBUtil.getConnection();
             Statement st = con.createStatement()) {
            st.executeUpdate(sql);
            System.out.println("Voting stopped!");
        } catch (Exception e) { e.printStackTrace(); }
    }

    public boolean isVotingActive() {
        String sql = "SELECT is_active FROM voting_status WHERE id=1";
        try (Connection con = DBUtil.getConnection();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            if (rs.next()) return rs.getBoolean("is_active");
        } catch (Exception e) { e.printStackTrace(); }
        return false;
    }

    public void resetVotes() {
        try (Connection con = DBUtil.getConnection();
             Statement st = con.createStatement()) {
            st.executeUpdate("DELETE FROM candidates");
            st.executeUpdate("ALTER TABLE candidates AUTO_INCREMENT = 1");

            st.executeUpdate("DELETE FROM voters");
            st.executeUpdate("ALTER TABLE voters AUTO_INCREMENT = 1");

            st.executeUpdate("UPDATE voting_status SET is_active = 0");

            System.out.println("All votes, candidates and voters reset. Voting inactive.");
        } catch (Exception e) { e.printStackTrace(); }
    }

    // ---------------- Candidate operations ----------------
    public void addCandidate(String name, String symbol, int age, String position, File photoFile, String bio) {
        String sql = "INSERT INTO candidates(name, symbol, age, position, photo, bio, votes) VALUES(?, ?, ?, ?, ?, ?, 0)";
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             FileInputStream fis = new FileInputStream(photoFile)) {

            ps.setString(1, name);
            ps.setString(2, symbol);
            ps.setInt(3, age);
            ps.setString(4, position);
            ps.setBinaryStream(5, fis, (int)photoFile.length());
            ps.setString(6, bio);
            ps.executeUpdate();
            System.out.println("Candidate added with photo.");
        } catch (Exception e) { e.printStackTrace(); }
    }

    public void editCandidate(int id, String newName, String newSymbol, Integer newAge, String newPosition, File newPhotoFile, String newBio) {
        String sql = "UPDATE candidates SET name=?, symbol=?, age=?, position=?, photo=?, bio=? WHERE id=?";
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             FileInputStream fis = newPhotoFile != null ? new FileInputStream(newPhotoFile) : null) {

            ps.setString(1, newName);
            ps.setString(2, newSymbol);
            if (newAge != null) ps.setInt(3, newAge); else ps.setNull(3, Types.INTEGER);
            ps.setString(4, newPosition);
            if (newPhotoFile != null) ps.setBinaryStream(5, fis, (int)newPhotoFile.length());
            else ps.setNull(5, Types.BLOB);
            ps.setString(6, newBio);
            ps.setInt(7, id);
            ps.executeUpdate();
            System.out.println("Candidate updated.");
        } catch (Exception e) { e.printStackTrace(); }
    }

    public void deleteCandidate(int id) {
        String sql = "DELETE FROM candidates WHERE id=?";
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
            System.out.println("Candidate deleted.");
        } catch (Exception e) { e.printStackTrace(); }
    }

    public ArrayList<Candidate> getAllCandidates() {
        ArrayList<Candidate> list = new ArrayList<>();
        String sql = "SELECT * FROM candidates";
        try (Connection con = DBUtil.getConnection();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                list.add(new Candidate(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("symbol"),
                        rs.getInt("age"),
                        rs.getString("position"),
                        rs.getBytes("photo"),
                        rs.getString("bio"),
                        rs.getInt("votes")
                ));
            }
        } catch (Exception e) { e.printStackTrace(); }
        return list;
    }

    public ArrayList<Candidate> getCandidatesByPosition(String position) {
        ArrayList<Candidate> list = new ArrayList<>();
        String sql = "SELECT * FROM candidates WHERE position = ?";
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, position);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new Candidate(
                            rs.getInt("id"),
                            rs.getString("name"),
                            rs.getString("symbol"),
                            rs.getInt("age"),
                            rs.getString("position"),
                            rs.getBytes("photo"),
                            rs.getString("bio"),
                            rs.getInt("votes")
                    ));
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
        return list;
    }

    // ---------------- Voter verification (admin-side) ----------------
    public ArrayList<Voter> getUnverifiedVoters() {
        ArrayList<Voter> list = new ArrayList<>();
        String sql = "SELECT * FROM voters WHERE verified = 0";
        try (Connection con = DBUtil.getConnection();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                list.add(new Voter(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("password"),
                        rs.getBoolean("has_voted"),
                        rs.getBoolean("verified"),
                        rs.getDate("dob").toLocalDate()
                ));
            }
        } catch (Exception e) { e.printStackTrace(); }
        return list;
    }

    public void setVoterVerified(int voterId, boolean verified) {
        String sql = "UPDATE voters SET verified = ? WHERE id = ?";
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setBoolean(1, verified);
            ps.setInt(2, voterId);
            ps.executeUpdate();
            System.out.println("Voter " + voterId + " verification set to " + verified);
        } catch (Exception e) { e.printStackTrace(); }
    }

    public ArrayList<Voter> getAllVoters() {
        ArrayList<Voter> list = new ArrayList<>();
        String sql = "SELECT * FROM voters";
        try (Connection con = DBUtil.getConnection();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                list.add(new Voter(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("password"),
                        rs.getBoolean("has_voted"),
                        rs.getBoolean("verified"),
                        rs.getDate("dob").toLocalDate()
                ));
            }
        } catch (Exception e) { e.printStackTrace(); }
        return list;
    }

    // Edit voter details. Any nullable parameter left as null will not be updated.
    public void editVoter(int id, String newName, String newPassword, java.time.LocalDate newDob, Boolean hasVoted, Boolean verified) {
        String sql = "UPDATE voters SET name = COALESCE(?, name), password = COALESCE(?, password), dob = COALESCE(?, dob), has_voted = COALESCE(?, has_voted), verified = COALESCE(?, verified) WHERE id = ?";
        try (Connection con = DBUtil.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            if (newName != null) ps.setString(1, newName); else ps.setNull(1, Types.VARCHAR);
            if (newPassword != null) ps.setString(2, newPassword); else ps.setNull(2, Types.VARCHAR);
            if (newDob != null) ps.setDate(3, Date.valueOf(newDob)); else ps.setNull(3, Types.DATE);
            if (hasVoted != null) ps.setBoolean(4, hasVoted); else ps.setNull(4, Types.BOOLEAN);
            if (verified != null) ps.setBoolean(5, verified); else ps.setNull(5, Types.BOOLEAN);
            ps.setInt(6, id);
            ps.executeUpdate();
            System.out.println("Voter " + id + " updated.");
        } catch (Exception e) { e.printStackTrace(); }
    }

    public void deleteVoter(int id) {
        String sql = "DELETE FROM voters WHERE id = ?";
        try (Connection con = DBUtil.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
            System.out.println("Voter " + id + " deleted.");
        } catch (Exception e) { e.printStackTrace(); }
    }
}

// ---------------- DB Utility ----------------
class DBUtil {
    public static Connection getConnection() throws SQLException, ClassNotFoundException {
        Class.forName("com.mysql.cj.jdbc.Driver");
        return DriverManager.getConnection("jdbc:mysql://localhost:3306/votingdb2", "root", "password");
    }
}
