# Hotel Microservices — AWS Kubernetes Deployment Documentation

## Project Overview

This document covers the complete deployment of a Hotel Management Microservices application on AWS using Kubernetes (kubeadm), Terraform, and a 2-node EC2 cluster.

---

## Architecture Overview

```
                        Internet
                           │
                    ┌──────▼──────┐
                    │   Browser   │
                    └──────┬──────┘
                           │
              ┌────────────▼────────────┐
              │     AWS Default VPC      │
              │                          │
              │  ┌─────────────────────┐ │
              │  │   Master Node        │ │
              │  │   (t3.medium)        │ │
              │  │                      │ │
              │  │  • service-registry  │ │
              │  │  • api-gateway       │ │
              │  │                      │ │
              │  │  Ports:              │ │
              │  │  :30080 api-gateway  │ │
              │  │  :30761 eureka       │ │
              │  └──────────┬──────────┘ │
              │             │ Flannel CNI │
              │  ┌──────────▼──────────┐ │
              │  │   Worker Node        │ │
              │  │   (t3.medium)        │ │
              │  │                      │ │
              │  │  • mysql             │ │
              │  │  • auth-service      │ │
              │  │  • room-service      │ │
              │  │  • roomprice-service │ │
              │  │  • reservation-svc   │ │
              │  │  • staff-service     │ │
              │  │  • frontend          │ │
              │  │  • prometheus        │ │
              │  │  • grafana           │ │
              │  │                      │ │
              │  │  Ports:              │ │
              │  │  :30400 frontend     │ │
              │  │  :30090 prometheus   │ │
              │  │  :30300 grafana      │ │
              │  └─────────────────────┘ │
              └──────────────────────────┘
```

---

## Technology Stack

| Component | Technology |
|---|---|
| Cloud Provider | AWS |
| Infrastructure as Code | Terraform |
| Container Orchestration | Kubernetes v1.29 (kubeadm) |
| Container Runtime | containerd |
| CNI Plugin | Flannel |
| Service Discovery | Netflix Eureka |
| API Gateway | Spring Cloud Gateway |
| Database | MySQL 8.0 |
| Monitoring | Prometheus + Grafana |
| Secret Coordination | AWS SSM Parameter Store |
| Manifest Storage | AWS S3 |
| OS | Ubuntu 22.04 LTS |

---

## AWS Resources Created

| Resource | Name | Purpose |
|---|---|---|
| EC2 Instance | k8s-master | Kubernetes control plane + api-gateway + service-registry |
| EC2 Instance | k8s-worker | All microservices + database + monitoring |
| Security Group | k8s-cluster-sg | Controls inbound/outbound traffic |
| IAM Role | k8s-ssm-role | Allows EC2 to access SSM and S3 |
| IAM Instance Profile | k8s-ssm-profile | Attaches IAM role to EC2 |
| S3 Bucket | k8s-hotel-manifests-xxxx | Stores all Kubernetes YAML manifests |
| SSM Parameter | /k8s/join-command | Worker join token (SecureString) |
| SSM Parameter | /k8s/master-ready | Coordination flag for worker timing |
| Key Pair | k8s-hotel-key | SSH access to EC2 instances |

---

## Project Folder Structure

```
microservice_project_new/
├── deploy.sh                          ← One-command deployment script
├── k8s/
│   ├── namespace.yaml
│   ├── secrets.yaml
│   ├── configmaps/
│   │   ├── api-gateway-configmap.yaml
│   │   ├── auth-service-configmap.yaml
│   │   ├── reservation-service-configmap.yaml
│   │   ├── room-service-configmap.yaml
│   │   ├── roomprice-service-configmap.yaml
│   │   ├── service-registry-configmap.yaml
│   │   └── staff-service-configmap.yaml
│   ├── database/
│   │   └── mysql.yaml
│   ├── frontend/
│   │   ├── frontend-deployment.yaml
│   │   └── frontend-service.yaml
│   ├── infrastructure/
│   │   ├── service-registry-deployment.yaml
│   │   └── service-registry-service.yaml
│   ├── monitoring/
│   │   ├── grafana.yaml
│   │   ├── prometheus-configmap.yaml
│   │   └── prometheus.yaml
│   └── services/
│       ├── api-gateway-deployment.yaml
│       ├── api-gateway-service.yaml
│       ├── auth-service-deployment.yaml
│       ├── auth-service-service.yaml
│       ├── reservation-service-deployment.yaml
│       ├── reservation-service-service.yaml
│       ├── room-service-deployment.yaml
│       ├── room-service-service.yaml
│       ├── roomprice-service-deployment.yaml
│       ├── roomprice-service-service.yaml
│       ├── staff-service-deployment.yaml
│       └── staff-service-service.yaml
└── terraform/
    ├── main.tf
    ├── iam.tf
    ├── variables.tf
    ├── outputs.tf
    ├── k8s-hotel-key.pem              ← Auto-generated SSH key
    └── user_data/
        ├── master.sh
        └── worker.sh
```

---

## Prerequisites

Install the following tools before running the deployment:

### 1. Terraform
```bash
# Windows (winget)
winget install HashiCorp.Terraform

# Verify
terraform --version
```

### 2. AWS CLI
```bash
# Windows (winget)
winget install Amazon.AWSCLI

# Verify
aws --version
```

### 3. curl
```bash
# Usually pre-installed on Linux/Mac
# Windows: included with Git Bash or WSL
curl --version
```

---

## Security Group Ports

| Port | Protocol | Purpose |
|---|---|---|
| 22 | TCP | SSH access |
| 6443 | TCP | Kubernetes API server |
| 30080 | TCP | API Gateway (NodePort) |
| 30400 | TCP | Frontend (NodePort) |
| 30761 | TCP | Eureka Dashboard (NodePort) |
| 30090 | TCP | Prometheus (NodePort) |
| 30300 | TCP | Grafana (NodePort) |
| All | All | Internal node-to-node communication |

---

## Terraform Files Explained

### main.tf
- Defines AWS provider with region
- Generates RSA 4096-bit key pair using `tls_private_key`
- Saves `.pem` file locally using `local_file`
- Creates S3 bucket with random suffix to avoid naming conflicts
- Uploads all k8s YAML files to S3 using `fileset()`
- Creates security group with all required ports
- Creates master EC2 instance with `master.sh` user_data
- Creates worker EC2 instance with `worker.sh` user_data

### iam.tf
- Creates IAM role with EC2 trust policy
- Attaches policy allowing:
  - `ssm:PutParameter`, `ssm:GetParameter`, `ssm:DeleteParameter` on `/k8s/*`
  - `s3:GetObject`, `s3:ListBucket` on the manifests bucket
- Creates instance profile and attaches role

### variables.tf
- `aws_region` — defaults to `us-east-1`
- `key_pair_name` — defaults to `k8s-hotel-key`
- `ami_id` — Ubuntu 22.04 LTS AMI for us-east-1
- `instance_type` — defaults to `t3.medium`

### outputs.tf
- `master_public_ip` — EC2 public IP
- `frontend_url` — `http://<ip>:30400`
- `api_gateway_url` — `http://<ip>:30080`
- `ssh_master` — ready-to-use SSH command
- `ssh_key_path` — path to `.pem` file

---

## Bootstrap Process (Automated)

### master.sh — What it does:

```
1.  apt-get update + install dependencies
2.  Disable swap (required for k8s)
3.  Load kernel modules: overlay, br_netfilter
4.  Set sysctl params for k8s networking
5.  Install containerd + configure SystemdCgroup=true
6.  Install kubeadm, kubelet, kubectl (v1.29)
7.  kubeadm init with pod-network-cidr=10.244.0.0/16
8.  Setup kubectl for root and ubuntu users
9.  Remove control-plane taint (allows master to schedule pods)
10. Install Flannel CNI
11. Wait for master node to be Ready
12. Label master node with role=master
13. Generate join command → store in SSM /k8s/join-command
14. Write ready flag → SSM /k8s/master-ready = true
15. Watch for worker to join → label it role=worker
16. Download all k8s YAMLs from S3
17. Replace MASTER_PUBLIC_IP placeholder in frontend manifest
18. Apply manifests in order:
    namespace → secrets → configmaps → database →
    infrastructure → services → frontend → monitoring
```

### worker.sh — What it does:

```
1.  apt-get update + install dependencies
2.  Disable swap
3.  Load kernel modules + sysctl params
4.  Install containerd
5.  Install kubeadm, kubelet, kubectl (v1.29)
6.  Poll SSM /k8s/master-ready every 10s (max 15 min)
7.  Fetch join command from SSM /k8s/join-command
8.  Execute kubeadm join
```

---

## Node Distribution (Load Balancing)

### Master Node
| Service | Port |
|---|---|
| service-registry (Eureka) | 8761 |
| api-gateway | 8080 |

### Worker Node
| Service | Port |
|---|---|
| mysql | 3306 |
| auth-service | 8082 |
| room-service | 8084 |
| roomprice-service | 8094 |
| reservation-service | 8083 |
| staff-service | 8087 |
| frontend | 4000 |
| prometheus | 9090 |
| grafana | 3000 |

---

## Kubernetes Resources

### Namespace
```
hotel
```

### Secrets
| Secret | Keys |
|---|---|
| db-secret | mysql-root-password, mysql-username, mysql-password |
| jwt-secret | jwt-secret |

### ConfigMaps
One ConfigMap per service containing all environment variables
(Spring datasource URLs, Eureka URLs, logging levels, etc.)

### Services (NodePort)
| Service | NodePort |
|---|---|
| api-gateway | 30080 |
| frontend | 30400 |
| service-registry | 30761 |
| prometheus | 30090 |
| grafana | 30300 |

### Services (ClusterIP)
auth-service, room-service, roomprice-service,
reservation-service, staff-service, mysql

---

## Deployment — One Command

```bash
cd microservice_project_new
chmod +x deploy.sh
./deploy.sh
```

### What deploy.sh does step by step:

| Step | Action |
|---|---|
| 1 | Check terraform, aws, curl are installed |
| 2 | Show current AWS account, offer to switch credentials |
| 3 | Locate terraform directory |
| 4 | Clean up all conflicting AWS resources (S3, IAM, key pair, SG, SSM) |
| 5 | `terraform init -upgrade` |
| 6 | `terraform validate` |
| 7 | `terraform plan -out=tfplan` |
| 8 | Ask for confirmation |
| 9 | `terraform apply tfplan` |
| 10 | Extract master IP and URLs from outputs |
| 11 | Poll API Gateway every 15s until live (max 15 min) |
| 12 | Verify all endpoints (frontend, api-gateway, eureka, prometheus, grafana) |
| ✅ | Print all access links |

---

## Access Links (After Deployment)

| Service | URL | Credentials |
|---|---|---|
| Frontend | `http://<master-ip>:30400` | — |
| API Gateway | `http://<master-ip>:30080` | — |
| Eureka Dashboard | `http://<master-ip>:30761` | — |
| Prometheus | `http://<master-ip>:30090` | — |
| Grafana | `http://<master-ip>:30300` | admin / admin123 |

---

## SSH Access

```bash
# SSH into master
ssh -i terraform/k8s-hotel-key.pem ubuntu@<master-ip>

# Check all pods
kubectl get pods -n hotel -o wide

# Check nodes
kubectl get nodes

# Check logs of a service
kubectl logs -n hotel deployment/auth-service --tail=50

# Check bootstrap log
tail -f /var/log/k8s-master-init.log
```

---

## Monitoring Setup

### Prometheus
- Scrapes all microservices every 15 seconds
- Endpoint: `/actuator/prometheus` on each service
- Targets: api-gateway, auth-service, room-service,
  roomprice-service, reservation-service, staff-service

### Grafana
- Pre-configured to connect to Prometheus
- Default login: admin / admin123
- Add Prometheus datasource: `http://prometheus:9090`

---

## Cost Breakdown

| Resource | Cost/month (running) | Cost/month (stopped) |
|---|---|---|
| Master EC2 t3.medium | ~$30 | $0 |
| Worker EC2 t3.medium | ~$30 | $0 |
| Master EBS 20GB gp3 | ~$1.60 | ~$1.60 |
| Worker EBS 25GB gp3 | ~$2.00 | ~$2.00 |
| S3 (manifests) | ~$0.01 | ~$0.01 |
| SSM Parameters | Free | Free |
| **Total** | **~$62/month** | **~$3.61/month** |

---

## Cost Saving Commands

### Stop instances when not in use:
```bash
aws ec2 stop-instances --instance-ids \
  $(aws ec2 describe-instances \
    --filters "Name=tag:Name,Values=k8s-master,k8s-worker" \
    --query "Reservations[].Instances[].InstanceId" \
    --output text)
```

### Start instances:
```bash
aws ec2 start-instances --instance-ids \
  $(aws ec2 describe-instances \
    --filters "Name=tag:Name,Values=k8s-master,k8s-worker" \
    --query "Reservations[].Instances[].InstanceId" \
    --output text)
```

### Destroy everything (cost = $0):
```bash
cd terraform
terraform destroy -auto-approve
```

---

## Troubleshooting

### Check bootstrap progress
```bash
ssh -i terraform/k8s-hotel-key.pem ubuntu@<master-ip>
tail -f /var/log/k8s-master-init.log
```

### Check worker bootstrap
```bash
ssh -i terraform/k8s-hotel-key.pem ubuntu@<worker-ip>
tail -f /var/log/k8s-worker-init.log
```

### Pod stuck in Pending
```bash
kubectl describe pod <pod-name> -n hotel
# Usually means nodeSelector has no matching node yet
```

### 503 from API Gateway
```bash
# Check if service is registered in Eureka
kubectl exec -n hotel deployment/api-gateway -- \
  curl -s http://service-registry:8761/eureka/apps/AUTH-SERVICE
# Wait 30-60 seconds for Eureka cache to refresh
```

### Prometheus targets down
```bash
# Test if Prometheus can reach the service
kubectl exec -n hotel deployment/prometheus -- \
  wget -qO- http://auth-service:8082/actuator/prometheus | head -3
# If metrics returned, targets will turn UP on next scrape (15s)
```

### CORS error on frontend
```bash
# Means API_BASE_URL is set to localhost instead of master public IP
kubectl set env deployment/frontend -n hotel \
  API_BASE_URL=http://<master-public-ip>:30080
```

---

## Key Design Decisions

| Decision | Reason |
|---|---|
| kubeadm over EKS | ~55% cheaper (~$62 vs ~$132/month) |
| Default VPC | No NAT Gateway cost (~$32/month saved) |
| SSM for join token | Solves worker timing race condition |
| S3 for manifests | Keeps user_data under 16KB EC2 limit |
| Flannel CNI | Simplest CNI, works well for 2-node demo |
| NodePort services | No Load Balancer cost (~$16/month saved) |
| Single MySQL pod | Demo purpose, no PV needed |
| nodeSelector labels | Ensures correct pod placement on each node |

---

## Comparison: This Setup vs Alternatives

| Feature | Kind on EC2 | This Setup | EKS |
|---|---|---|---|
| Real separate nodes | ❌ | ✅ | ✅ |
| Public accessibility | ✅ | ✅ | ✅ |
| Real load distribution | ❌ | ✅ | ✅ |
| Production-like | ❌ | ✅ | ✅ |
| Monthly cost | ~$30 | ~$62 | ~$132+ |
| Setup automation | Manual | One command | Managed |
| Managed control plane | ❌ | ❌ | ✅ |

---

*Document prepared for Hotel Microservices AWS Kubernetes Deployment*
