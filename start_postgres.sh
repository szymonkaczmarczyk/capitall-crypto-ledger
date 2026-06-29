#!/bin/zsh

# Dynamiczne wyszukiwanie ścieżki do postgresa
POSTGRES_BIN=$(find ~ -name postgres 2>/dev/null | head -n 1)

if [ -z "$POSTGRES_BIN" ]; then
    echo "Error: Could not find postgres binary in user home directory!"
    exit 1
fi

# Używamy podwójnego dirname do dokładnego wyznaczenia var-16
DB_DIR=$(dirname $(dirname "${POSTGRES_BIN}"))
PID_FILE="${DB_DIR}/postmaster.pid"
# Logi zapisujemy bezpośrednio w katalogu bazy danych, aby uniknąć problemów z TCC i workspace
LOG_FILE="${DB_DIR}/postgres_server.log"

echo "=== Starting PostgreSQL from Dynamic Shell Script ==="
echo "PostgreSQL Binary: ${POSTGRES_BIN}"
echo "Database Directory: ${DB_DIR}"
echo "Log File: ${LOG_FILE}"

# 1. Clean up old PID file
if [ -f "${PID_FILE}" ]; then
    echo "Found postmaster.pid. Checking if process is active..."
    PID=$(cat "${PID_FILE}")
    if kill -0 "${PID}" 2>/dev/null; then
        echo "PostgreSQL is already running with PID ${PID}."
        exit 0
    else
        echo "Stale PID file found. Removing: ${PID_FILE}"
        rm -f "${PID_FILE}"
    fi
fi

# 2. Start PostgreSQL in background with disown
"${POSTGRES_BIN}" -D "${DB_DIR}" > "${LOG_FILE}" 2>&1 &
BG_PID=$!
disown $BG_PID

# 3. Wait a bit and check if it started successfully
sleep 3
if kill -0 "${BG_PID}" 2>/dev/null; then
    echo "PostgreSQL successfully started in background and disowned. PID: ${BG_PID}"
    exit 0
else
    echo "PostgreSQL failed to start. Content of ${LOG_FILE}:"
    cat "${LOG_FILE}"
    exit 1
fi
