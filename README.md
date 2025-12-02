# Voting-System-using-JAVA

A simple Java + MySQL based **Digital Voting System** with both a GUI and backend logic.  
This project supports admin controls, voter management, candidate handling, and secure one-time voting. 

## ⭐ Features
- Voter registration with age verification (18+)
- Admin login (credentials stored in database)
- Admin can verify/unverify voters
- Add / edit / delete candidates (photo supported)
- Start & stop voting session
- Voters can vote only once
- Voting disabled when election is inactive
- MySQL persistent storage
- GUI built using Java Swing
- Shell script launcher (Linux)

## 🛠️ Technologies Used
- **Java (Swing + JDBC)**
- **MySQL**
- **MySQL Connector/J**
- **Object-Oriented Programming**
- **Bash scripting**
- **File handling (LONGBLOB for images)**

## 📁 Project Structure
src/
- VotingLogic.java (Backend logic)
- VotingGUI.java (GUI for login, admin, and voting)
- LaunchVoting.sh (Script to run the application)
- votingdb.sql (Database schema)

## How to Run

1. **Install Java**
   - Make sure Java (JDK 8 or higher) is installed on your computer.
2. **Set Up the Database**
   - Open MySQL.
   - Create a new database.
   - Import the file `votingdb.sql` into it.
3. **Check Database Details**
   - If needed, open `VotingLogic.java` and update the MySQL username, password, and database name.
4. **Run the Program**
   - On Linux/macOS: double-click `LaunchVoting.sh` or run:
     ```
     sh LaunchVoting.sh
     ```
   - On Windows: open a terminal in the project folder and run:
     ```
     java VotingGUI
     ```
5. **Start Using the App**
   - When the window opens, log in with the admin username and password added by `votingdb.sql`.
   - Add candidates, register voters, and start voting.

