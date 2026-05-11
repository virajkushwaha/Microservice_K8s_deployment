#!/bin/bash

# ─────────────────────────────────────────────────────────────────────────────
# Colors
# ─────────────────────────────────────────────────────────────────────────────
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
MAGENTA='\033[0;35m'
BOLD='\033[1m'
RESET='\033[0m'

info()    { echo -e "${CYAN}[INFO]${RESET}  $1"; }
success() { echo -e "${GREEN}[✔]${RESET}    $1"; }
warn()    { echo -e "${YELLOW}[WARN]${RESET}  $1"; }
error()   { echo -e "${RED}[✘]${RESET}    $1"; }
section() {
  echo -e "\n${BOLD}${MAGENTA}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${RESET}"
  echo -e "${BOLD}${MAGENTA}  $1${RESET}"
  echo -e "${BOLD}${MAGENTA}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${RESET}\n"
}

# ─────────────────────────────────────────────────────────────────────────────
# Banner
# ─────────────────────────────────────────────────────────────────────────────
clear
echo -e "${BOLD}${CYAN}"
echo "  ██╗      ██████╗  █████╗ ██████╗     ████████╗███████╗███████╗████████╗"
echo "  ██║     ██╔═══██╗██╔══██╗██╔══██╗       ██╔══╝██╔════╝██╔════╝╚══██╔══╝"
echo "  ██║     ██║   ██║███████║██║  ██║       ██║   █████╗  ███████╗   ██║   "
echo "  ██║     ██║   ██║██╔══██║██║  ██║       ██║   ██╔══╝  ╚════██║   ██║   "
echo "  ███████╗╚██████╔╝██║  ██║██████╔╝       ██║   ███████╗███████║   ██║   "
echo "  ╚══════╝ ╚═════╝ ╚═╝  ╚═╝╚═════╝        ╚═╝   ╚══════╝╚══════╝   ╚═╝   "
echo -e "${RESET}"
echo -e "${CYAN}            Hotel Microservices — Load Testing Tool${RESET}"
echo -e "${CYAN}         Simulate real traffic phases on AWS Kubernetes${RESET}\n"

# ─────────────────────────────────────────────────────────────────────────────
# Step 1 — Check and install hey
# ─────────────────────────────────────────────────────────────────────────────
section "STEP 1 — Checking Prerequisites"

if ! command -v hey &>/dev/null; then
  warn "'hey' not found. Installing..."
  if command -v apt-get &>/dev/null; then
    sudo apt-get install -y hey 2>/dev/null || {
      curl -sSL https://hey-release.s3.us-east-2.amazonaws.com/hey_linux_amd64 -o /usr/local/bin/hey
      chmod +x /usr/local/bin/hey
    }
  elif command -v brew &>/dev/null; then
    brew install hey
  else
    curl -sSL https://hey-release.s3.us-east-2.amazonaws.com/hey_linux_amd64 -o /usr/local/bin/hey
    chmod +x /usr/local/bin/hey
  fi
  command -v hey &>/dev/null && success "'hey' installed." || { error "Could not install 'hey'."; exit 1; }
else
  success "'hey' is installed."
fi

command -v curl &>/dev/null && success "curl is installed." || { error "curl not found."; exit 1; }

# ─────────────────────────────────────────────────────────────────────────────
# Step 2 — Get Master IP
# ─────────────────────────────────────────────────────────────────────────────
section "STEP 2 — Target Configuration"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
TERRAFORM_DIR="$SCRIPT_DIR/terraform"
AUTO_IP=""

if [ -d "$TERRAFORM_DIR" ] && [ -f "$TERRAFORM_DIR/terraform.tfstate" ]; then
  AUTO_IP=$(cd "$TERRAFORM_DIR" && terraform output -raw master_public_ip 2>/dev/null || echo "")
fi

if [ -n "$AUTO_IP" ]; then
  echo -e "${GREEN}  Auto-detected Master IP: ${BOLD}$AUTO_IP${RESET}"
  read -p "$(echo -e ${YELLOW}  Use this IP? [Y/n]: ${RESET})" USE_AUTO
  USE_AUTO=${USE_AUTO:-Y}
  [[ "$USE_AUTO" =~ ^[Yy]$ ]] && MASTER_IP=$AUTO_IP || read -p "$(echo -e ${CYAN}  Enter Master Public IP: ${RESET})" MASTER_IP
else
  read -p "$(echo -e ${CYAN}  Enter Master Public IP: ${RESET})" MASTER_IP
fi

info "Verifying master is reachable..."
HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" --max-time 5 "http://$MASTER_IP:30080/actuator/health" 2>/dev/null || echo "000")
if [ "$HTTP_CODE" = "200" ]; then
  success "API Gateway is reachable (HTTP 200)"
else
  error "API Gateway not reachable at http://$MASTER_IP:30080 (HTTP $HTTP_CODE)"
  exit 1
fi

BASE_URL="http://$MASTER_IP:30080"

# ─────────────────────────────────────────────────────────────────────────────
# Step 3 — Select Service
# ─────────────────────────────────────────────────────────────────────────────
section "STEP 3 — Select Service to Load Test"

echo -e "  ${BOLD}Available Services:${RESET}\n"
echo -e "  ${CYAN}  1)${RESET} auth-service        ${BLUE}→ POST /auth/login${RESET}"
echo -e "  ${CYAN}  2)${RESET} room-service        ${BLUE}→ GET  /rooms/all${RESET}"
echo -e "  ${CYAN}  3)${RESET} roomprice-service   ${BLUE}→ GET  /room-prices/all${RESET}"
echo -e "  ${CYAN}  4)${RESET} reservation-service ${BLUE}→ GET  /reservations/all${RESET}"
echo -e "  ${CYAN}  5)${RESET} staff-service       ${BLUE}→ GET  /staff/all${RESET}"
echo -e "  ${CYAN}  6)${RESET} api-gateway health  ${BLUE}→ GET  /actuator/health${RESET}"
echo -e "  ${CYAN}  7)${RESET} Custom endpoint     ${BLUE}→ you provide the path${RESET}"
echo -e "  ${CYAN}  8)${RESET} All services        ${BLUE}→ test all above${RESET}"
echo ""
read -p "$(echo -e ${BOLD}${YELLOW}  Select service [1-8]: ${RESET})" SERVICE_CHOICE

case $SERVICE_CHOICE in
  1) SERVICE_NAME="auth-service";        METHOD="POST"; ENDPOINT="/auth/login";        BODY='{"username":"testuser","password":"Test@1234"}'; CONTENT_TYPE="application/json" ;;
  2) SERVICE_NAME="room-service";        METHOD="GET";  ENDPOINT="/rooms/all";         BODY=""; CONTENT_TYPE="" ;;
  3) SERVICE_NAME="roomprice-service";   METHOD="GET";  ENDPOINT="/room-prices/all";   BODY=""; CONTENT_TYPE="" ;;
  4) SERVICE_NAME="reservation-service"; METHOD="GET";  ENDPOINT="/reservations/all";  BODY=""; CONTENT_TYPE="" ;;
  5) SERVICE_NAME="staff-service";       METHOD="GET";  ENDPOINT="/staff/all";         BODY=""; CONTENT_TYPE="" ;;
  6) SERVICE_NAME="api-gateway";         METHOD="GET";  ENDPOINT="/actuator/health";   BODY=""; CONTENT_TYPE="" ;;
  7)
    read -p "$(echo -e ${CYAN}  Service name: ${RESET})" SERVICE_NAME
    read -p "$(echo -e ${CYAN}  Endpoint path (e.g. /rooms/all): ${RESET})" ENDPOINT
    read -p "$(echo -e ${CYAN}  HTTP Method [GET/POST]: ${RESET})" METHOD
    METHOD=${METHOD:-GET}
    if [ "$METHOD" = "POST" ]; then
      read -p "$(echo -e ${CYAN}  JSON body: ${RESET})" BODY
      CONTENT_TYPE="application/json"
    else
      BODY=""; CONTENT_TYPE=""
    fi
    ;;
  8) SERVICE_NAME="ALL" ;;
  *) error "Invalid choice."; exit 1 ;;
esac

[ "$SERVICE_NAME" != "ALL" ] && success "Selected: ${BOLD}$SERVICE_NAME${RESET} → ${BOLD}$METHOD $BASE_URL$ENDPOINT${RESET}"

# ─────────────────────────────────────────────────────────────────────────────
# Step 4 — Define Load Phases with Time Intervals
# ─────────────────────────────────────────────────────────────────────────────
section "STEP 4 — Define Load Phases (Time Intervals)"

echo -e "  ${BOLD}You can define multiple load phases.${RESET}"
echo -e "  ${CYAN}  Each phase has its own duration and concurrent users.${RESET}"
echo -e "  ${CYAN}  This simulates real traffic patterns (warm up → peak → cool down).${RESET}\n"

echo -e "  ${BOLD}Example:${RESET}"
echo -e "  ${YELLOW}  Phase 1: 30s,  50 users  → warm up${RESET}"
echo -e "  ${YELLOW}  Phase 2: 60s, 100 users  → normal load${RESET}"
echo -e "  ${YELLOW}  Phase 3: 60s, 200 users  → peak load${RESET}"
echo -e "  ${YELLOW}  Phase 4: 30s,  50 users  → cool down${RESET}\n"

echo -e "  ${BOLD}Or use a preset:${RESET}\n"
echo -e "  ${CYAN}  1)${RESET} Light test    ${GREEN}→ 2 phases: warmup(30s/10) + load(60s/50)${RESET}"
echo -e "  ${CYAN}  2)${RESET} Normal test   ${YELLOW}→ 3 phases: warmup + normal + cooldown${RESET}"
echo -e "  ${CYAN}  3)${RESET} Heavy test    ${RED}→ 4 phases: warmup + normal + peak + cooldown${RESET}"
echo -e "  ${CYAN}  4)${RESET} Custom phases ${BLUE}→ you define each phase manually${RESET}"
echo ""
read -p "$(echo -e ${BOLD}${YELLOW}  Select option [1-4]: ${RESET})" PHASE_CHOICE

# Arrays to store phases
PHASE_DURATIONS=()
PHASE_CONCURRENCIES=()
PHASE_REQUESTS=()
PHASE_LABELS=()

case $PHASE_CHOICE in
  1)
    PHASE_LABELS=("Warm Up" "Load")
    PHASE_DURATIONS=(30 60)
    PHASE_CONCURRENCIES=(10 50)
    PHASE_REQUESTS=(100 500)
    ;;
  2)
    PHASE_LABELS=("Warm Up" "Normal Load" "Cool Down")
    PHASE_DURATIONS=(30 60 30)
    PHASE_CONCURRENCIES=(10 100 20)
    PHASE_REQUESTS=(100 1000 200)
    ;;
  3)
    PHASE_LABELS=("Warm Up" "Normal Load" "Peak Load" "Cool Down")
    PHASE_DURATIONS=(30 60 60 30)
    PHASE_CONCURRENCIES=(10 100 200 20)
    PHASE_REQUESTS=(100 1000 2000 200)
    ;;
  4)
    PHASE_NUM=1
    while true; do
      echo ""
      echo -e "  ${BOLD}${CYAN}  Phase $PHASE_NUM:${RESET}"
      read -p "$(echo -e ${CYAN}    Label (e.g. Warm Up / Peak / Cool Down): ${RESET})" PLABEL
      read -p "$(echo -e ${CYAN}    Duration in seconds: ${RESET})" PDURATION
      read -p "$(echo -e ${CYAN}    Concurrent users: ${RESET})" PCONCURRENCY
      read -p "$(echo -e ${CYAN}    Total requests: ${RESET})" PREQUESTS

      PHASE_LABELS+=("$PLABEL")
      PHASE_DURATIONS+=("$PDURATION")
      PHASE_CONCURRENCIES+=("$PCONCURRENCY")
      PHASE_REQUESTS+=("$PREQUESTS")

      echo ""
      read -p "$(echo -e ${YELLOW}    Add another phase? [y/N]: ${RESET})" ADD_MORE
      [[ "$ADD_MORE" =~ ^[Yy]$ ]] && PHASE_NUM=$((PHASE_NUM + 1)) || break
    done
    ;;
  *) error "Invalid choice."; exit 1 ;;
esac

# ─────────────────────────────────────────────────────────────────────────────
# Step 5 — Show Phase Summary and Confirm
# ─────────────────────────────────────────────────────────────────────────────
section "STEP 5 — Load Test Plan"

TOTAL_DURATION=0
TOTAL_REQUESTS=0

echo -e "  ${BOLD}Phase Plan:${RESET}\n"
echo -e "  ${BOLD}${BLUE}  ┌──────────────────┬──────────┬─────────────┬──────────┐${RESET}"
echo -e "  ${BOLD}${BLUE}  │ Phase            │ Duration │ Concurrent  │ Requests │${RESET}"
echo -e "  ${BOLD}${BLUE}  ├──────────────────┼──────────┼─────────────┼──────────┤${RESET}"

for i in "${!PHASE_LABELS[@]}"; do
  LABEL="${PHASE_LABELS[$i]}"
  DUR="${PHASE_DURATIONS[$i]}"
  CON="${PHASE_CONCURRENCIES[$i]}"
  REQ="${PHASE_REQUESTS[$i]}"
  TOTAL_DURATION=$((TOTAL_DURATION + DUR))
  TOTAL_REQUESTS=$((TOTAL_REQUESTS + REQ))

  if [ "$CON" -ge 150 ] 2>/dev/null; then
    COLOR=$RED
  elif [ "$CON" -ge 75 ] 2>/dev/null; then
    COLOR=$YELLOW
  else
    COLOR=$GREEN
  fi
  printf "  ${BLUE}  │${RESET} ${COLOR}%-16s${RESET} ${BLUE}│${RESET} ${CYAN}%6ss${RESET}   ${BLUE}│${RESET} ${COLOR}%8s${RESET}    ${BLUE}│${RESET} ${CYAN}%6s${RESET}   ${BLUE}│${RESET}\n" \
    "$LABEL" "$DUR" "$CON" "$REQ"
done

echo -e "  ${BOLD}${BLUE}  ├──────────────────┼──────────┼─────────────┼──────────┤${RESET}"
echo -e "  ${BOLD}${BLUE}  │ TOTAL            │ ${TOTAL_DURATION}s     │             │ ${TOTAL_REQUESTS}   │${RESET}"
echo -e "  ${BOLD}${BLUE}  └──────────────────┴──────────┴─────────────┴──────────┘${RESET}"
echo ""
echo -e "  ${YELLOW}  Target  : $BASE_URL${RESET}"
[ "$SERVICE_NAME" != "ALL" ] && echo -e "  ${YELLOW}  Service : $SERVICE_NAME → $METHOD $ENDPOINT${RESET}" || echo -e "  ${YELLOW}  Service : ALL services${RESET}"
echo -e "  ${YELLOW}  Total   : ${TOTAL_DURATION}s duration, ${TOTAL_REQUESTS} requests${RESET}"
echo ""
read -p "$(echo -e ${BOLD}${RED}  Start load test? [yes/no]: ${RESET})" CONFIRM
[ "$CONFIRM" != "yes" ] && warn "Load test cancelled." && exit 0

# ─────────────────────────────────────────────────────────────────────────────
# Results directory
# ─────────────────────────────────────────────────────────────────────────────
RESULTS_DIR="/tmp/load-test-$(date +%Y%m%d-%H%M%S)"
mkdir -p "$RESULTS_DIR"
SUMMARY_FILE="$RESULTS_DIR/summary.txt"

# ─────────────────────────────────────────────────────────────────────────────
# Run single phase function
# ─────────────────────────────────────────────────────────────────────────────
run_phase() {
  local PHASE_LABEL=$1
  local SVC=$2
  local MTD=$3
  local EP=$4
  local BDY=$5
  local CT=$6
  local DUR=$7
  local CON=$8
  local REQ=$9
  local URL="$BASE_URL$EP"
  local RESULT_FILE="$RESULTS_DIR/${SVC}-${PHASE_LABEL// /_}.txt"

  echo ""
  echo -e "${BOLD}${BLUE}  ┌─────────────────────────────────────────────────────┐${RESET}"
  echo -e "${BOLD}${BLUE}  │  Phase : $PHASE_LABEL${RESET}"
  echo -e "${BOLD}${BLUE}  │  Target: $MTD $URL${RESET}"
  echo -e "${BOLD}${BLUE}  │  Load  : $CON concurrent users for ${DUR}s ($REQ requests)${RESET}"
  echo -e "${BOLD}${BLUE}  └─────────────────────────────────────────────────────┘${RESET}"
  echo ""

  if [ "$MTD" = "POST" ] && [ -n "$BDY" ]; then
    hey -n $REQ -c $CON -z ${DUR}s \
      -m POST \
      -H "Content-Type: $CT" \
      -d "$BDY" \
      "$URL" > "$RESULT_FILE" 2>&1
  else
    hey -n $REQ -c $CON -z ${DUR}s \
      -m GET \
      "$URL" > "$RESULT_FILE" 2>&1
  fi

  # Parse results
  TOTAL_T=$(grep "Total:" "$RESULT_FILE" 2>/dev/null | awk '{print $2, $3}')
  SLOWEST=$(grep "Slowest:" "$RESULT_FILE" 2>/dev/null | awk '{print $2, $3}')
  FASTEST=$(grep "Fastest:" "$RESULT_FILE" 2>/dev/null | awk '{print $2, $3}')
  AVERAGE=$(grep "Average:" "$RESULT_FILE" 2>/dev/null | awk '{print $2, $3}')
  RPS=$(grep "Requests/sec:" "$RESULT_FILE" 2>/dev/null | awk '{print $2}')
  SUCCESS=$(grep "\[200\]" "$RESULT_FILE" 2>/dev/null | awk '{print $2}')
  ERRORS=$(grep -E "\[4[0-9][0-9]\]|\[5[0-9][0-9]\]" "$RESULT_FILE" 2>/dev/null | awk '{sum+=$2} END {print sum+0}')

  echo -e "  ${BOLD}  ── Results: $PHASE_LABEL ──${RESET}"
  echo -e "  ${GREEN}  Requests/sec  :${RESET} ${BOLD}$RPS${RESET}"
  echo -e "  ${GREEN}  Total time    :${RESET} $TOTAL_T"
  echo -e "  ${GREEN}  Fastest       :${RESET} $FASTEST"
  echo -e "  ${YELLOW}  Average       :${RESET} $AVERAGE"
  echo -e "  ${RED}  Slowest       :${RESET} $SLOWEST"
  echo -e "  ${GREEN}  Success (200) :${RESET} ${BOLD}${GREEN}$SUCCESS${RESET}"
  echo -e "  ${RED}  Errors        :${RESET} ${BOLD}${RED}$ERRORS${RESET}"

  echo ""
  echo -e "  ${BOLD}  Status Distribution:${RESET}"
  grep -E "\[[0-9]{3}\]" "$RESULT_FILE" 2>/dev/null | while read line; do
    CODE=$(echo $line | grep -o '\[[0-9]*\]' | tr -d '[]')
    COUNT=$(echo $line | awk '{print $2}')
    if [[ "$CODE" =~ ^2 ]]; then
      echo -e "  ${GREEN}    HTTP $CODE : $COUNT requests ✔${RESET}"
    elif [[ "$CODE" =~ ^4 ]]; then
      echo -e "  ${YELLOW}    HTTP $CODE : $COUNT requests ⚠${RESET}"
    elif [[ "$CODE" =~ ^5 ]]; then
      echo -e "  ${RED}    HTTP $CODE : $COUNT requests ✘${RESET}"
    fi
  done

  echo ""
  echo -e "  ${BOLD}  Latency Histogram:${RESET}"
  grep -A 12 "Response time histogram:" "$RESULT_FILE" 2>/dev/null | grep -v "Response time histogram:" | head -10 | while read line; do
    echo -e "  ${BLUE}    $line${RESET}"
  done

  # Save to summary
  echo "Phase: $PHASE_LABEL | Service: $SVC | RPS: $RPS | Avg: $AVERAGE | Success: $SUCCESS | Errors: $ERRORS" >> "$SUMMARY_FILE"

  # Show pod resources after each phase
  echo ""
  echo -e "  ${BOLD}  Pod Resources after $PHASE_LABEL:${RESET}"
  kubectl top pods -n hotel 2>/dev/null | while IFS= read -r line; do
    CPU=$(echo "$line" | awk '{print $2}' | tr -d 'm')
    if echo "$line" | grep -q "NAME"; then
      echo -e "  ${BOLD}${BLUE}    $line${RESET}"
    elif [ -n "$CPU" ] && [ "$CPU" -gt 500 ] 2>/dev/null; then
      echo -e "  ${RED}    $line  ⚠ HIGH CPU${RESET}"
    elif [ -n "$CPU" ] && [ "$CPU" -gt 200 ] 2>/dev/null; then
      echo -e "  ${YELLOW}    $line${RESET}"
    else
      echo -e "  ${GREEN}    $line${RESET}"
    fi
  done || warn "kubectl top not available (metrics-server not installed)"
}

# ─────────────────────────────────────────────────────────────────────────────
# Step 6 — Execute all phases
# ─────────────────────────────────────────────────────────────────────────────
section "STEP 6 — Running Load Test Phases"

ELAPSED_TIME=0

for i in "${!PHASE_LABELS[@]}"; do
  LABEL="${PHASE_LABELS[$i]}"
  DUR="${PHASE_DURATIONS[$i]}"
  CON="${PHASE_CONCURRENCIES[$i]}"
  REQ="${PHASE_REQUESTS[$i]}"

  PHASE_NUM=$((i + 1))
  TOTAL_PHASES=${#PHASE_LABELS[@]}

  echo ""
  echo -e "${BOLD}${MAGENTA}  ▶ Phase $PHASE_NUM/$TOTAL_PHASES — $LABEL${RESET}"
  echo -e "${MAGENTA}    Time: ${ELAPSED_TIME}s → $((ELAPSED_TIME + DUR))s | Users: $CON | Requests: $REQ${RESET}"

  if [ "$SERVICE_NAME" = "ALL" ]; then
    for SVC_ITEM in "api-gateway:GET:/actuator/health::" \
                    "auth-service:POST:/auth/login:{\"username\":\"testuser\",\"password\":\"Test@1234\"}:application/json" \
                    "room-service:GET:/rooms/all::" \
                    "roomprice-service:GET:/room-prices/all::" \
                    "reservation-service:GET:/reservations/all::" \
                    "staff-service:GET:/staff/all::"; do
      IFS=':' read -r S_NAME S_METHOD S_EP S_BODY S_CT <<< "$SVC_ITEM"
      run_phase "$LABEL" "$S_NAME" "$S_METHOD" "$S_EP" "$S_BODY" "$S_CT" "$DUR" "$CON" "$REQ"
    done
  else
    run_phase "$LABEL" "$SERVICE_NAME" "$METHOD" "$ENDPOINT" "$BODY" "$CONTENT_TYPE" "$DUR" "$CON" "$REQ"
  fi

  ELAPSED_TIME=$((ELAPSED_TIME + DUR))

  # Pause between phases
  if [ $i -lt $((${#PHASE_LABELS[@]} - 1)) ]; then
    echo ""
    info "Phase $PHASE_NUM complete. Pausing 5s before next phase..."
    sleep 5
  fi
done

# ─────────────────────────────────────────────────────────────────────────────
# Step 7 — Final node resource usage
# ─────────────────────────────────────────────────────────────────────────────
section "STEP 7 — Final Resource Usage"

echo -e "  ${BOLD}Node Resource Usage:${RESET}\n"
kubectl top nodes 2>/dev/null | while IFS= read -r line; do
  echo -e "  ${CYAN}  $line${RESET}"
done || warn "Node metrics not available. Install metrics-server to enable."

# ─────────────────────────────────────────────────────────────────────────────
# Final Summary
# ─────────────────────────────────────────────────────────────────────────────
echo ""
echo -e "${BOLD}${GREEN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${RESET}"
echo -e "${BOLD}${GREEN}                        📊  LOAD TEST COMPLETE                                  ${RESET}"
echo -e "${BOLD}${GREEN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${RESET}"
echo ""
echo -e "  ${BOLD}${CYAN}Phase Summary:${RESET}\n"
if [ -f "$SUMMARY_FILE" ]; then
  while IFS= read -r line; do
    echo -e "  ${YELLOW}  $line${RESET}"
  done < "$SUMMARY_FILE"
fi
echo ""
echo -e "  ${BOLD}${CYAN}Total Test Duration :${RESET} ${TOTAL_DURATION}s"
echo -e "  ${BOLD}${CYAN}Total Requests      :${RESET} ${TOTAL_REQUESTS}"
echo -e "  ${BOLD}${CYAN}Results saved at    :${RESET} ${BLUE}$RESULTS_DIR/${RESET}"
echo ""
echo -e "  ${BOLD}${CYAN}📈  View metrics in Grafana:${RESET}"
echo -e "  ${GREEN}  http://$MASTER_IP:30300${RESET}  ${YELLOW}(admin / admin123)${RESET}"
echo ""
echo -e "  ${BOLD}${CYAN}🔍  Check Prometheus targets:${RESET}"
echo -e "  ${GREEN}  http://$MASTER_IP:30090/targets${RESET}"
echo ""
echo -e "${BOLD}${GREEN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${RESET}"
