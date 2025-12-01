#!/bin/bash
# ================================
# Voting System Launcher (Linux)
# ================================

# --- MySQL settings ---
MYSQL_JAR="./mysql-connector-java-9.4.0.jar"
MYSQL_USER="root"
MYSQL_PASS="Bloom@333"
MYSQL_DB="votingdb2"
SQL_FILE="./votingdb2.sql"

# --- Start MySQL service (systemd-based distros) ---
echo "Starting MySQL service..."
if command -v systemctl >/dev/null 2>&1; then
    sudo systemctl start mysql
else
    sudo service mysql start
fi

# --- Check if MySQL JAR exists ---
if [ ! -f "$MYSQL_JAR" ]; then
    echo "Error: MySQL connector JAR not found at $MYSQL_JAR"
    exit 1
fi

echo "Using MySQL Connector JAR: $MYSQL_JAR"

# --- Initialize Database (run schema if needed) ---
if [ -f "$SQL_FILE" ]; then
    echo "Ensuring database is initialized..."
    mysql -u"$MYSQL_USER" -p"$MYSQL_PASS" < "$SQL_FILE"
else
    echo "Warning: No $SQL_FILE found, skipping DB initialization."
fi

# --- Compile Java files ---
echo "Compiling Java files..."
javac -cp ".:$MYSQL_JAR" *.java
if [ $? -ne 0 ]; then
    echo "Compilation failed!"
    exit 1
fi
echo "Compilation successful."

# --- Launch GUI ---
echo "Launching Voting System GUI..."
java -cp ".:$MYSQL_JAR" VotingGUI2
