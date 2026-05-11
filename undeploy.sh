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
echo -e "${BOLD}${RED}"
echo "  ██╗   ██╗███╗   ██╗██████╗ ███████╗██████╗ ██╗      ██████╗ ██╗   ██╗"
echo "  ██║   ██║████╗  ██║██╔══██╗██╔════╝██╔══██╗██║     ██╔═══██╗╚██╗ ██╔╝"
echo "  ██║   ██║██╔██╗ ██║██║  ██║█████╗  ██████╔╝██║     ██║   ██║ ╚████╔╝ "
echo "  ██║   ██║██║╚██╗██║██║  ██║██╔══╝  ██╔═══╝ ██║     ██║   ██║  ╚██╔╝  "
echo "  ╚██████╔╝██║ ╚████║██████╔╝███████╗██║     ███████╗╚██████╔╝   ██║   "
echo "   ╚═════╝ ╚═╝  ╚═══╝╚═════╝ ╚══════╝╚═╝     ╚══════╝ ╚═════╝    ╚═╝   "
echo -e "${RESET}"
echo -e "${CYAN}         Hotel Microservices — AWS Kubernetes Teardown${RESET}"
echo -e "${CYAN}              This will destroy ALL AWS resources${RESET}\n"

# ─────────────────────────────────────────────────────────────────────────────
# Step 1 — Verify AWS credentials
# ─────────────────────────────────────────────────────────────────────────────
section "STEP 1 — Verifying AWS Credentials"

CURRENT_ACCOUNT=$(aws sts get-caller-identity --query "Account" --output text 2>/dev/null || echo "")
CURRENT_USER=$(aws sts get-caller-identity --query "Arn" --output text 2>/dev/null || echo "")
CURRENT_REGION=$(aws configure get region 2>/dev/null || echo "us-east-1")

if [ -z "$CURRENT_ACCOUNT" ]; then
  error "No valid AWS credentials found. Please run 'aws configure' first."
  exit 1
fi

echo -e "${GREEN}  AWS Account :${RESET} $CURRENT_ACCOUNT"
echo -e "${GREEN}  IAM User    :${RESET} $CURRENT_USER"
echo -e "${GREEN}  Region      :${RESET} $CURRENT_REGION"
success "AWS credentials verified."

# ─────────────────────────────────────────────────────────────────────────────
# Step 2 — Locate terraform directory
# ─────────────────────────────────────────────────────────────────────────────
section "STEP 2 — Locating Terraform Directory"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
TERRAFORM_DIR="$SCRIPT_DIR/terraform"

if [ ! -d "$TERRAFORM_DIR" ]; then
  error "Terraform directory not found at: $TERRAFORM_DIR"
  exit 1
fi

cd "$TERRAFORM_DIR"
success "Terraform directory: $TERRAFORM_DIR"

# ─────────────────────────────────────────────────────────────────────────────
# Step 3 — Show what will be destroyed
# ─────────────────────────────────────────────────────────────────────────────
section "STEP 3 — Current Deployment Status"

info "Checking running EC2 instances..."
MASTER_ID=$(aws ec2 describe-instances \
  --filters "Name=tag:Name,Values=k8s-master" "Name=instance-state-name,Values=running,stopped" \
  --query "Reservations[].Instances[].InstanceId" \
  --output text --region $CURRENT_REGION 2>/dev/null || echo "")

WORKER_ID=$(aws ec2 describe-instances \
  --filters "Name=tag:Name,Values=k8s-worker" "Name=instance-state-name,Values=running,stopped" \
  --query "Reservations[].Instances[].InstanceId" \
  --output text --region $CURRENT_REGION 2>/dev/null || echo "")

MASTER_IP=$(aws ec2 describe-instances \
  --filters "Name=tag:Name,Values=k8s-master" "Name=instance-state-name,Values=running" \
  --query "Reservations[].Instances[].PublicIpAddress" \
  --output text --region $CURRENT_REGION 2>/dev/null || echo "")

S3_BUCKETS=$(aws s3 ls 2>/dev/null | grep "k8s-hotel-manifests" | awk '{print $3}')

echo ""
[ -n "$MASTER_ID" ] && echo -e "  ${YELLOW}EC2 Master    :${RESET} $MASTER_ID ${BLUE}($MASTER_IP)${RESET}" || echo -e "  ${GREEN}EC2 Master    :${RESET} not found"
[ -n "$WORKER_ID" ] && echo -e "  ${YELLOW}EC2 Worker    :${RESET} $WORKER_ID" || echo -e "  ${GREEN}EC2 Worker    :${RESET} not found"
[ -n "$S3_BUCKETS" ] && echo -e "  ${YELLOW}S3 Buckets    :${RESET} $S3_BUCKETS" || echo -e "  ${GREEN}S3 Buckets    :${RESET} not found"
echo -e "  ${YELLOW}IAM Role      :${RESET} k8s-ssm-role"
echo -e "  ${YELLOW}Key Pair      :${RESET} k8s-hotel-key"
echo -e "  ${YELLOW}Security Group:${RESET} k8s-cluster-sg"
echo -e "  ${YELLOW}SSM Params    :${RESET} /k8s/join-command, /k8s/master-ready"
echo ""

# ─────────────────────────────────────────────────────────────────────────────
# Step 4 — Confirm destruction
# ─────────────────────────────────────────────────────────────────────────────
section "STEP 4 — Confirm Teardown"

echo -e "${BOLD}${RED}  ⚠️  WARNING: This will permanently destroy all AWS resources.${RESET}"
echo -e "${RED}  All data in MySQL will be lost. This cannot be undone.${RESET}\n"
read -p "$(echo -e ${BOLD}${RED}  Type 'destroy' to confirm: ${RESET})" CONFIRM

if [ "$CONFIRM" != "destroy" ]; then
  warn "Teardown cancelled. Nothing was deleted."
  exit 0
fi

# ─────────────────────────────────────────────────────────────────────────────
# Step 5 — Terraform Destroy
# ─────────────────────────────────────────────────────────────────────────────
section "STEP 5 — Terraform Destroy"

if [ -f "terraform.tfstate" ]; then
  info "Running terraform destroy..."
  terraform destroy -auto-approve 2>&1 | while IFS= read -r line; do
    if echo "$line" | grep -q "Destruction complete\|destroyed"; then
      echo -e "${GREEN}  $line${RESET}"
    elif echo "$line" | grep -qiE "error|failed"; then
      echo -e "${RED}  $line${RESET}"
    elif echo "$line" | grep -q "Destroying\|Still destroying"; then
      echo -e "${YELLOW}  $line${RESET}"
    else
      echo -e "${BLUE}  $line${RESET}"
    fi
  done

  if [ ${PIPESTATUS[0]} -eq 0 ]; then
    success "Terraform destroy completed."
  else
    warn "Terraform destroy had some errors. Proceeding with manual cleanup..."
  fi
else
  warn "No terraform.tfstate found. Proceeding with manual cleanup..."
fi

# ─────────────────────────────────────────────────────────────────────────────
# Step 6 — Manual cleanup of any leftover resources
# ─────────────────────────────────────────────────────────────────────────────
section "STEP 6 — Cleaning Up Leftover Resources"

# S3 Buckets
info "Cleaning up S3 buckets..."
OLD_BUCKETS=$(aws s3 ls 2>/dev/null | grep "k8s-hotel-manifests" | awk '{print $3}')
if [ -n "$OLD_BUCKETS" ]; then
  for BUCKET in $OLD_BUCKETS; do
    aws s3 rb "s3://$BUCKET" --force 2>/dev/null \
      && success "Deleted S3 bucket: $BUCKET" \
      || warn "Could not delete bucket: $BUCKET"
  done
else
  success "No leftover S3 buckets."
fi

# IAM
info "Cleaning up IAM resources..."
aws iam remove-role-from-instance-profile \
  --instance-profile-name k8s-ssm-profile \
  --role-name k8s-ssm-role 2>/dev/null && warn "Removed role from instance profile." || true

aws iam delete-instance-profile \
  --instance-profile-name k8s-ssm-profile 2>/dev/null \
  && success "Deleted instance profile." || true

aws iam delete-role-policy \
  --role-name k8s-ssm-role \
  --policy-name k8s-ssm-s3-policy 2>/dev/null \
  && success "Deleted IAM role policy." || true

aws iam delete-role \
  --role-name k8s-ssm-role 2>/dev/null \
  && success "Deleted IAM role." || true

# Key Pair
info "Cleaning up key pair..."
aws ec2 delete-key-pair \
  --key-name k8s-hotel-key \
  --region $CURRENT_REGION 2>/dev/null \
  && success "Deleted key pair: k8s-hotel-key" || true

# Security Group
info "Cleaning up security group..."
SG_ID=$(aws ec2 describe-security-groups \
  --filters "Name=group-name,Values=k8s-cluster-sg" \
  --query "SecurityGroups[0].GroupId" \
  --output text --region $CURRENT_REGION 2>/dev/null || echo "")
if [ -n "$SG_ID" ] && [ "$SG_ID" != "None" ]; then
  aws ec2 delete-security-group \
    --group-id $SG_ID \
    --region $CURRENT_REGION 2>/dev/null \
    && success "Deleted security group: $SG_ID" || warn "Could not delete SG (may still have dependencies)"
else
  success "No leftover security group."
fi

# SSM Parameters
info "Cleaning up SSM parameters..."
aws ssm delete-parameter --name "/k8s/join-command" --region $CURRENT_REGION 2>/dev/null \
  && success "Deleted SSM /k8s/join-command" || true
aws ssm delete-parameter --name "/k8s/master-ready" --region $CURRENT_REGION 2>/dev/null \
  && success "Deleted SSM /k8s/master-ready" || true

# ─────────────────────────────────────────────────────────────────────────────
# Step 7 — Clean up local files
# ─────────────────────────────────────────────────────────────────────────────
section "STEP 7 — Cleaning Up Local Files"

[ -f "k8s-hotel-key.pem" ]          && rm -f k8s-hotel-key.pem          && success "Deleted k8s-hotel-key.pem"
[ -f "terraform.tfstate" ]          && rm -f terraform.tfstate           && success "Deleted terraform.tfstate"
[ -f "terraform.tfstate.backup" ]   && rm -f terraform.tfstate.backup    && success "Deleted terraform.tfstate.backup"
[ -f "tfplan" ]                     && rm -f tfplan                      && success "Deleted tfplan"
[ -f ".terraform.lock.hcl" ]        && rm -f .terraform.lock.hcl         && success "Deleted .terraform.lock.hcl"
[ -d ".terraform" ]                 && rm -rf .terraform                 && success "Deleted .terraform directory"

# ─────────────────────────────────────────────────────────────────────────────
# Step 8 — Verify everything is gone
# ─────────────────────────────────────────────────────────────────────────────
section "STEP 8 — Verifying Cleanup"

REMAINING_INSTANCES=$(aws ec2 describe-instances \
  --filters "Name=tag:Name,Values=k8s-master,k8s-worker" \
            "Name=instance-state-name,Values=running,stopped,pending" \
  --query "Reservations[].Instances[].InstanceId" \
  --output text --region $CURRENT_REGION 2>/dev/null || echo "")

REMAINING_BUCKETS=$(aws s3 ls 2>/dev/null | grep "k8s-hotel-manifests" | awk '{print $3}')

REMAINING_ROLE=$(aws iam get-role --role-name k8s-ssm-role 2>/dev/null && echo "exists" || echo "")

[ -z "$REMAINING_INSTANCES" ] && success "EC2 instances: cleaned" || warn "EC2 instances still exist: $REMAINING_INSTANCES"
[ -z "$REMAINING_BUCKETS" ]   && success "S3 buckets: cleaned"    || warn "S3 buckets still exist: $REMAINING_BUCKETS"
[ -z "$REMAINING_ROLE" ]      && success "IAM role: cleaned"      || warn "IAM role still exists"

# ─────────────────────────────────────────────────────────────────────────────
# Final Summary
# ─────────────────────────────────────────────────────────────────────────────
echo ""
echo -e "${BOLD}${RED}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${RESET}"
echo -e "${BOLD}${RED}                        🗑️   TEARDOWN COMPLETE                                  ${RESET}"
echo -e "${BOLD}${RED}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${RESET}"
echo ""
echo -e "  ${GREEN}  ✔  EC2 instances terminated${RESET}"
echo -e "  ${GREEN}  ✔  S3 bucket deleted${RESET}"
echo -e "  ${GREEN}  ✔  IAM role + profile deleted${RESET}"
echo -e "  ${GREEN}  ✔  Key pair deleted${RESET}"
echo -e "  ${GREEN}  ✔  Security group deleted${RESET}"
echo -e "  ${GREEN}  ✔  SSM parameters deleted${RESET}"
echo -e "  ${GREEN}  ✔  Local terraform files cleaned${RESET}"
echo ""
echo ""
echo -e "  ${BOLD}${CYAN}🚀  To redeploy:${RESET}"
echo -e "  ${YELLOW}  ./deploy.sh${RESET}"
echo ""
echo -e "${BOLD}${RED}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${RESET}"
