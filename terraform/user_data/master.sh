#!/bin/bash
set -e
exec > /var/log/k8s-master-init.log 2>&1

AWS_REGION="${aws_region}"
S3_BUCKET="${s3_bucket}"
MASTER_PRIVATE_IP=$(curl -s http://169.254.169.254/latest/meta-data/local-ipv4)
MASTER_PUBLIC_IP=$(curl -s http://169.254.169.254/latest/meta-data/public-ipv4)

# ── 1. System prep ────────────────────────────────────────────────────────────
apt-get update -y
apt-get install -y apt-transport-https ca-certificates curl gnupg awscli

swapoff -a
sed -i '/swap/d' /etc/fstab

modprobe overlay && modprobe br_netfilter
cat <<EOF > /etc/modules-load.d/k8s.conf
overlay
br_netfilter
EOF
cat <<EOF > /etc/sysctl.d/k8s.conf
net.bridge.bridge-nf-call-iptables  = 1
net.ipv4.ip_forward                 = 1
EOF
sysctl --system

# ── 2. Install containerd ─────────────────────────────────────────────────────
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | gpg --dearmor -o /etc/apt/keyrings/docker.gpg
echo "deb [arch=amd64 signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu $(lsb_release -cs) stable" > /etc/apt/sources.list.d/docker.list
apt-get update -y && apt-get install -y containerd.io
mkdir -p /etc/containerd
containerd config default > /etc/containerd/config.toml
sed -i 's/SystemdCgroup = false/SystemdCgroup = true/' /etc/containerd/config.toml
systemctl restart containerd && systemctl enable containerd

# ── 3. Install kubeadm kubelet kubectl ───────────────────────────────────────
curl -fsSL https://pkgs.k8s.io/core:/stable:/v1.29/deb/Release.key | gpg --dearmor -o /etc/apt/keyrings/kubernetes-apt-keyring.gpg
echo "deb [signed-by=/etc/apt/keyrings/kubernetes-apt-keyring.gpg] https://pkgs.k8s.io/core:/stable:/v1.29/deb/ /" > /etc/apt/sources.list.d/kubernetes.list
apt-get update -y && apt-get install -y kubelet kubeadm kubectl
apt-mark hold kubelet kubeadm kubectl

# ── 4. kubeadm init ───────────────────────────────────────────────────────────
kubeadm init \
  --pod-network-cidr=10.244.0.0/16 \
  --apiserver-advertise-address=$MASTER_PRIVATE_IP \
  --ignore-preflight-errors=NumCPU

mkdir -p /root/.kube /home/ubuntu/.kube
cp /etc/kubernetes/admin.conf /root/.kube/config
cp /etc/kubernetes/admin.conf /home/ubuntu/.kube/config
chown ubuntu:ubuntu /home/ubuntu/.kube/config
export KUBECONFIG=/etc/kubernetes/admin.conf

# ── 5. Remove master taint so master can schedule its own pods ───────────────
kubectl taint nodes --all node-role.kubernetes.io/control-plane- || true

# ── 6. Install Flannel CNI ────────────────────────────────────────────────────
kubectl apply -f https://github.com/flannel-io/flannel/releases/latest/download/kube-flannel.yml

# ── 7. Wait for master node Ready ────────────────────────────────────────────
echo "Waiting for master node to be Ready..."
for i in $(seq 1 30); do
  STATUS=$(kubectl get nodes --no-headers 2>/dev/null | awk '{print $2}' | head -1)
  [ "$STATUS" = "Ready" ] && break
  sleep 10
done

# ── 8. Label master node ─────────────────────────────────────────────────────
MASTER_NODE=$(kubectl get nodes --no-headers | awk '{print $1}' | head -1)
kubectl label node $MASTER_NODE role=master --overwrite

# ── 9. Store join command + ready flag in SSM ────────────────────────────────
JOIN_CMD=$(kubeadm token create --print-join-command)
aws ssm put-parameter \
  --name "/k8s/join-command" \
  --value "$JOIN_CMD" \
  --type "SecureString" \
  --overwrite \
  --region $AWS_REGION

aws ssm put-parameter \
  --name "/k8s/master-ready" \
  --value "true" \
  --type "String" \
  --overwrite \
  --region $AWS_REGION

echo "Join command stored in SSM. Worker can now join."

# ── 10. Watch for worker to join and label it ────────────────────────────────
echo "Waiting for worker node to join..."
for i in $(seq 1 60); do
  WORKER_NODE=$(kubectl get nodes --no-headers 2>/dev/null | grep -v "control-plane" | grep -v "$MASTER_NODE" | awk '{print $1}' | head -1)
  if [ -n "$WORKER_NODE" ]; then
    kubectl label node $WORKER_NODE role=worker --overwrite
    echo "Worker $WORKER_NODE joined and labeled."
    break
  fi
  sleep 10
done

# ── 11. Download manifests from S3 ───────────────────────────────────────────
mkdir -p /tmp/k8s
aws s3 sync s3://$S3_BUCKET/k8s/ /tmp/k8s/ --region $AWS_REGION

# ── 12. Inject real public IP into frontend manifest ─────────────────────────
sed -i "s|MASTER_PUBLIC_IP|$MASTER_PUBLIC_IP|g" /tmp/k8s/frontend/frontend-deployment.yaml

# ── 13. Apply all manifests in order ─────────────────────────────────────────
kubectl apply -f /tmp/k8s/namespace.yaml
kubectl apply -f /tmp/k8s/secrets.yaml
kubectl apply -f /tmp/k8s/configmaps/
kubectl apply -f /tmp/k8s/database/
kubectl apply -f /tmp/k8s/infrastructure/
kubectl apply -f /tmp/k8s/services/
kubectl apply -f /tmp/k8s/frontend/
kubectl apply -f /tmp/k8s/monitoring/

echo "✅ Cluster setup complete!"
echo "Frontend  → http://$MASTER_PUBLIC_IP:30400"
echo "API GW    → http://$MASTER_PUBLIC_IP:30080"
