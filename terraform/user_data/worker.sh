#!/bin/bash
set -e
exec > /var/log/k8s-worker-init.log 2>&1

AWS_REGION="${aws_region}"

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

# ── 4. Poll SSM for master-ready flag (up to 15 minutes) ─────────────────────
echo "Waiting for master to be ready..."
for i in $(seq 1 90); do
  READY=$(aws ssm get-parameter \
    --name "/k8s/master-ready" \
    --query "Parameter.Value" \
    --output text \
    --region $AWS_REGION 2>/dev/null || echo "")
  [ "$READY" = "true" ] && echo "Master is ready." && break
  echo "Attempt $i: master not ready yet, retrying in 10s..."
  sleep 10
done

# ── 5. Get join command from SSM ─────────────────────────────────────────────
JOIN_CMD=$(aws ssm get-parameter \
  --name "/k8s/join-command" \
  --with-decryption \
  --query "Parameter.Value" \
  --output text \
  --region $AWS_REGION)

if [ -z "$JOIN_CMD" ]; then
  echo "ERROR: Could not get join command from SSM."
  exit 1
fi

# ── 6. Join the cluster ───────────────────────────────────────────────────────
eval $JOIN_CMD

echo "✅ Worker joined the cluster successfully."
