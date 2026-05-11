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

# ─────────────────────────────────────────────────────────────────────────────
# Helpers
# ─────────────────────────────────────────────────────────────────────────────
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
echo -e "${BOLD}${BLUE}"
echo "  ██╗  ██╗ █████╗ ███████╗    ██████╗ ███████╗██████╗ ██╗      ██████╗ ██╗   ██╗"
echo "  ██║ ██╔╝██╔══██╗██╔════╝    ██╔══██╗██╔════╝██╔══██╗██║     ██╔═══██╗╚██╗ ██╔╝"
echo "  █████╔╝ ╚█████╔╝███████╗    ██║  ██║█████╗  ██████╔╝██║     ██║   ██║ ╚████╔╝ "
echo "  ██╔═██╗ ██╔══██╗╚════██║    ██║  ██║██╔══╝  ██╔═══╝ ██║     ██║   ██║  ╚██╔╝  "
echo "  ██║  ██╗╚█████╔╝███████║    ██████╔╝███████╗██║     ███████╗╚██████╔╝   ██║   "
echo "  ╚═╝  ╚═╝ ╚════╝ ╚══════╝    ╚═════╝ ╚══════╝╚═╝     ╚══════╝ ╚═════╝    ╚═╝   "
echo -e "${RESET}"
echo -e "${CYAN}         Hotel Microservices — AWS Kubernetes Deployment Automation${RESET}"
echo -e "${CYAN}                        Powered by Terraform + kubeadm${RESET}\n"

# ─────────────────────────────────────────────────────────────────────────────
# Step 1 — Check prerequisites
# ─────────────────────────────────────────────────────────────────────────────
section "STEP 1 — Checking Prerequisites"

check_tool() {
  if command -v $1 &>/dev/null; then
    success "$1 is installed ($(command -v $1))"
  else
    error "$1 is NOT installed. Please install it first."
    exit 1
  fi
}

check_tool terraform
check_tool aws
check_tool curl

# ─────────────────────────────────────────────────────────────────────────────
# Step 2 — AWS Configuration
# ─────────────────────────────────────────────────────────────────────────────
section "STEP 2 — AWS Configuration"

CURRENT_ACCOUNT=$(aws sts get-caller-identity --query "Account" --output text 2>/dev/null || echo "")
CURRENT_USER=$(aws sts get-caller-identity --query "Arn" --output text 2>/dev/null || echo "")
CURRENT_REGION=$(aws configure get region 2>/dev/null || echo "not set")

if [ -n "$CURRENT_ACCOUNT" ]; then
  echo -e "${GREEN}  Current AWS Account :${RESET} $CURRENT_ACCOUNT"
  echo -e "${GREEN}  Current IAM User    :${RESET} $CURRENT_USER"
  echo -e "${GREEN}  Current Region      :${RESET} $CURRENT_REGION"
  echo ""
  read -p "$(echo -e ${YELLOW}  Do you want to use this account? [Y/n]: ${RESET})" USE_CURRENT
  USE_CURRENT=${USE_CURRENT:-Y}
else
  warn "No AWS credentials configured."
  USE_CURRENT="n"
fi

if [[ "$USE_CURRENT" =~ ^[Nn]$ ]]; then
  echo ""
  info "Enter new AWS credentials:"
  read -p "$(echo -e ${CYAN}  AWS Access Key ID: ${RESET})" AWS_ACCESS_KEY
  read -s -p "$(echo -e ${CYAN}  AWS Secret Access Key: ${RESET})" AWS_SECRET_KEY
  echo ""
  read -p "$(echo -e ${CYAN}  AWS Region [us-east-1]: ${RESET})" AWS_REGION
  AWS_REGION=${AWS_REGION:-us-east-1}

  aws configure set aws_access_key_id "$AWS_ACCESS_KEY"
  aws configure set aws_secret_access_key "$AWS_SECRET_KEY"
  aws configure set region "$AWS_REGION"
  aws configure set output "json"

  NEW_ACCOUNT=$(aws sts get-caller-identity --query "Account" --output text 2>/dev/null || echo "")
  if [ -z "$NEW_ACCOUNT" ]; then
    error "Invalid AWS credentials. Please check and try again."
    exit 1
  fi
  success "AWS credentials configured. Account: $NEW_ACCOUNT"
else
  if [ -z "$CURRENT_ACCOUNT" ]; then
    error "No valid AWS credentials found. Exiting."
    exit 1
  fi
  success "Using existing AWS account: $CURRENT_ACCOUNT"
fi

AWS_REGION=$(aws configure get region)

# ─────────────────────────────────────────────────────────────────────────────
# Step 3 — Navigate to terraform directory
# ─────────────────────────────────────────────────────────────────────────────
section "STEP 3 — Locating Terraform Directory"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
TERRAFORM_DIR="$SCRIPT_DIR/terraform"

if [ ! -d "$TERRAFORM_DIR" ]; then
  error "Terraform directory not found at: $TERRAFORM_DIR"
  exit 1
fi

cd "$TERRAFORM_DIR"
success "Terraform directory: $TERRAFORM_DIR"

# ─────────────────────────────────────────────────────────────────────────────
# Step 4 — Cleanup conflicting AWS resources
# ─────────────────────────────────────────────────────────────────────────────
section "STEP 4 — Cleaning Up Conflicting AWS Resources"

# Destroy existing terraform state first if exists
if [ -f "terraform.tfstate" ]; then
  warn "Existing Terraform state found. Destroying previous deployment first..."
  terraform destroy -auto-approve 2>&1 | while IFS= read -r line; do
    echo -e "${YELLOW}  $line${RESET}"
  done
  success "Previous deployment destroyed."
fi

# Delete leftover S3 buckets
info "Checking for leftover S3 buckets..."
OLD_BUCKETS=$(aws s3 ls 2>/dev/null | grep "k8s-hotel-manifests" | awk '{print $3}')
if [ -n "$OLD_BUCKETS" ]; then
  for BUCKET in $OLD_BUCKETS; do
    warn "Deleting old S3 bucket: $BUCKET"
    aws s3 rb "s3://$BUCKET" --force 2>/dev/null && success "Deleted bucket: $BUCKET" || warn "Could not delete $BUCKET"
  done
else
  success "No leftover S3 buckets found."
fi

# Delete leftover IAM resources
info "Checking for leftover IAM resources..."

# Remove role from instance profile
aws iam remove-role-from-instance-profile \
  --instance-profile-name k8s-ssm-profile \
  --role-name k8s-ssm-role 2>/dev/null && warn "Removed role from instance profile." || true

# Delete instance profile
aws iam delete-instance-profile \
  --instance-profile-name k8s-ssm-profile 2>/dev/null && warn "Deleted instance profile: k8s-ssm-profile" || true

# Delete role policy
aws iam delete-role-policy \
  --role-name k8s-ssm-role \
  --policy-name k8s-ssm-s3-policy 2>/dev/null && warn "Deleted role policy." || true

# Delete IAM role
aws iam delete-role \
  --role-name k8s-ssm-role 2>/dev/null && warn "Deleted IAM role: k8s-ssm-role" || true

success "IAM cleanup complete."

# Delete leftover key pair
info "Checking for leftover key pair..."
aws ec2 delete-key-pair --key-name k8s-hotel-key --region $AWS_REGION 2>/dev/null \
  && warn "Deleted old key pair: k8s-hotel-key" || true

# Delete leftover security group
info "Checking for leftover security group..."
SG_ID=$(aws ec2 describe-security-groups \
  --filters "Name=group-name,Values=k8s-cluster-sg" \
  --query "SecurityGroups[0].GroupId" \
  --output text --region $AWS_REGION 2>/dev/null || echo "")
if [ -n "$SG_ID" ] && [ "$SG_ID" != "None" ]; then
  aws ec2 delete-security-group --group-id $SG_ID --region $AWS_REGION 2>/dev/null \
    && warn "Deleted old security group: $SG_ID" || true
fi

# Delete leftover SSM parameters
info "Checking for leftover SSM parameters..."
aws ssm delete-parameter --name "/k8s/join-command" --region $AWS_REGION 2>/dev/null && warn "Deleted SSM /k8s/join-command" || true
aws ssm delete-parameter --name "/k8s/master-ready" --region $AWS_REGION 2>/dev/null && warn "Deleted SSM /k8s/master-ready" || true

# Delete old local pem file
[ -f "k8s-hotel-key.pem" ] && rm -f k8s-hotel-key.pem && warn "Deleted old k8s-hotel-key.pem"

# Delete old terraform state files
rm -f terraform.tfstate terraform.tfstate.backup tfplan .terraform.lock.hcl 2>/dev/null || true
rm -rf .terraform 2>/dev/null || true

success "All conflicting resources cleaned up."

# ─────────────────────────────────────────────────────────────────────────────
# Step 5 — Terraform Init
# ─────────────────────────────────────────────────────────────────────────────
section "STEP 5 — Terraform Init"

info "Initializing Terraform..."
if terraform init -upgrade 2>&1 | while IFS= read -r line; do echo -e "${BLUE}  $line${RESET}"; done; then
  success "Terraform initialized successfully."
else
  error "Terraform init failed."
  exit 1
fi

# ─────────────────────────────────────────────────────────────────────────────
# Step 6 — Terraform Validate
# ─────────────────────────────────────────────────────────────────────────────
section "STEP 6 — Terraform Validate"

info "Validating Terraform configuration..."
if terraform validate 2>&1 | while IFS= read -r line; do echo -e "${BLUE}  $line${RESET}"; done; then
  success "Terraform configuration is valid."
else
  error "Terraform validation failed. Fix the errors above."
  exit 1
fi

# ─────────────────────────────────────────────────────────────────────────────
# Step 7 — Terraform Plan
# ─────────────────────────────────────────────────────────────────────────────
section "STEP 7 — Terraform Plan"

info "Generating Terraform plan..."
terraform plan -out=tfplan 2>&1 | while IFS= read -r line; do
  if echo "$line" | grep -q "will be created"; then
    echo -e "${GREEN}  $line${RESET}"
  elif echo "$line" | grep -q "will be destroyed"; then
    echo -e "${RED}  $line${RESET}"
  elif echo "$line" | grep -q "Plan:"; then
    echo -e "${BOLD}${YELLOW}  $line${RESET}"
  else
    echo -e "${BLUE}  $line${RESET}"
  fi
done

if [ ${PIPESTATUS[0]} -ne 0 ]; then
  error "Terraform plan failed."
  exit 1
fi
success "Terraform plan generated successfully."

# ─────────────────────────────────────────────────────────────────────────────
# Step 8 — Confirm Apply
# ─────────────────────────────────────────────────────────────────────────────
section "STEP 8 — Confirm Deployment"

echo -e "${YELLOW}  The above plan will be applied to your AWS account.${RESET}"
echo -e "${YELLOW}  Estimated cost: ~\$62/month (stop instances when not in use)${RESET}\n"
read -p "$(echo -e ${BOLD}${RED}  Proceed with deployment? [yes/no]: ${RESET})" CONFIRM

if [ "$CONFIRM" != "yes" ]; then
  warn "Deployment cancelled by user."
  exit 0
fi

# ─────────────────────────────────────────────────────────────────────────────
# Step 9 — Terraform Apply
# ─────────────────────────────────────────────────────────────────────────────
section "STEP 9 — Terraform Apply"

info "Applying Terraform plan..."
terraform apply tfplan 2>&1 | while IFS= read -r line; do
  if echo "$line" | grep -q "Creation complete"; then
    echo -e "${GREEN}  $line${RESET}"
  elif echo "$line" | grep -qiE "error|failed"; then
    echo -e "${RED}  $line${RESET}"
  elif echo "$line" | grep -q "Apply complete"; then
    echo -e "${BOLD}${GREEN}  $line${RESET}"
  elif echo "$line" | grep -q "Still creating"; then
    echo -e "${YELLOW}  $line${RESET}"
  else
    echo -e "${BLUE}  $line${RESET}"
  fi
done

if [ ${PIPESTATUS[0]} -ne 0 ]; then
  error "Terraform apply failed."
  exit 1
fi
success "Infrastructure deployed successfully."

# ─────────────────────────────────────────────────────────────────────────────
# Step 10 — Extract Outputs
# ─────────────────────────────────────────────────────────────────────────────
section "STEP 10 — Extracting Deployment Info"

MASTER_IP=$(terraform output -raw master_public_ip 2>/dev/null)
FRONTEND_URL=$(terraform output -raw frontend_url 2>/dev/null)
API_GW_URL=$(terraform output -raw api_gateway_url 2>/dev/null)
SSH_CMD=$(terraform output -raw ssh_master 2>/dev/null)

success "Master IP    : $MASTER_IP"
success "Frontend URL : $FRONTEND_URL"
success "API GW URL   : $API_GW_URL"

# ─────────────────────────────────────────────────────────────────────────────
# Step 11 — Wait for bootstrap to complete
# ─────────────────────────────────────────────────────────────────────────────
section "STEP 11 — Waiting for Kubernetes Bootstrap (~10 mins)"

info "Polling API Gateway every 15s until live (max 15 minutes)..."
echo ""

ELAPSED=0
MAX_WAIT=900

while [ $ELAPSED -lt $MAX_WAIT ]; do
  RESPONSE=$(curl -s -o /dev/null -w "%{http_code}" --max-time 5 \
    "http://$MASTER_IP:30080/actuator/health" 2>/dev/null || echo "000")

  if [ "$RESPONSE" = "200" ]; then
    echo ""
    success "API Gateway is live! (HTTP 200)"
    break
  fi

  MINS=$((ELAPSED / 60))
  SECS=$((ELAPSED % 60))
  echo -ne "${YELLOW}  ⏳ Waiting... ${MINS}m ${SECS}s elapsed — API Gateway HTTP $RESPONSE${RESET}\r"
  sleep 15
  ELAPSED=$((ELAPSED + 15))
done

if [ $ELAPSED -ge $MAX_WAIT ]; then
  warn "Bootstrap taking longer than expected. Check: ssh into master → tail -f /var/log/k8s-master-init.log"
fi

# ─────────────────────────────────────────────────────────────────────────────
# Step 12 — Verify all endpoints
# ─────────────────────────────────────────────────────────────────────────────
section "STEP 12 — Verifying Application Endpoints"

check_endpoint() {
  local NAME=$1
  local URL=$2
  RESPONSE=$(curl -s -o /dev/null -w "%{http_code}" --max-time 10 "$URL" 2>/dev/null || echo "000")
  if [[ "$RESPONSE" =~ ^(200|301|302|401|403)$ ]]; then
    echo -e "  ${GREEN}[✔]${RESET} $NAME ${BLUE}→ $URL${RESET} ${GREEN}[HTTP $RESPONSE]${RESET}"
  else
    echo -e "  ${YELLOW}[~]${RESET} $NAME ${BLUE}→ $URL${RESET} ${YELLOW}[HTTP $RESPONSE — may still be starting]${RESET}"
  fi
}

check_endpoint "Frontend"         "http://$MASTER_IP:30400"
check_endpoint "API Gateway"      "http://$MASTER_IP:30080/actuator/health"
check_endpoint "Eureka Dashboard" "http://$MASTER_IP:30761"
check_endpoint "Prometheus"       "http://$MASTER_IP:30090"
check_endpoint "Grafana"          "http://$MASTER_IP:30300"

# ─────────────────────────────────────────────────────────────────────────────
# Final Summary
# ─────────────────────────────────────────────────────────────────────────────
echo ""
echo -e "${BOLD}${GREEN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${RESET}"
echo -e "${BOLD}${GREEN}                        🚀  DEPLOYMENT COMPLETE                                 ${RESET}"
echo -e "${BOLD}${GREEN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${RESET}"
echo ""
echo -e "  ${BOLD}${CYAN}🌐  Application Links:${RESET}"
echo -e "  ${GREEN}  Frontend          →  ${BOLD}http://$MASTER_IP:30400${RESET}"
echo -e "  ${GREEN}  API Gateway        →  ${BOLD}http://$MASTER_IP:30080${RESET}"
echo -e "  ${GREEN}  Eureka Dashboard   →  ${BOLD}http://$MASTER_IP:30761${RESET}"
echo -e "  ${GREEN}  Prometheus         →  ${BOLD}http://$MASTER_IP:30090${RESET}"
echo -e "  ${GREEN}  Grafana            →  ${BOLD}http://$MASTER_IP:30300${RESET}  ${YELLOW}(admin / admin123)${RESET}"
echo ""
echo -e "  ${BOLD}${CYAN}🔑  SSH Access:${RESET}"
echo -e "  ${YELLOW}  $SSH_CMD${RESET}"
echo ""
echo -e "  ${BOLD}${CYAN}💰  Stop instances to save cost:${RESET}"
echo -e "  ${BLUE}  aws ec2 stop-instances --instance-ids \$(aws ec2 describe-instances \\${RESET}"
echo -e "  ${BLUE}    --filters 'Name=tag:Name,Values=k8s-master,k8s-worker' \\${RESET}"
echo -e "  ${BLUE}    --query 'Reservations[].Instances[].InstanceId' --output text)${RESET}"
echo ""
echo -e "  ${BOLD}${CYAN}🗑️   Destroy everything:${RESET}"
echo -e "  ${RED}  cd terraform && terraform destroy -auto-approve${RESET}"
echo ""
echo -e "${BOLD}${GREEN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${RESET}"
