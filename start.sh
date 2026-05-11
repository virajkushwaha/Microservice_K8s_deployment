#!/bin/bash
set -e

echo "=============================="
echo " Hotel Microservices Startup"
echo "=============================="

# Step 1: Start Docker if not running
echo "[1/5] Checking Docker..."
if ! sudo systemctl is-active --quiet docker; then
  echo "Starting Docker..."
  sudo systemctl start docker
  sleep 3
fi
echo "Docker is running."

# Step 2: Start Minikube if not running
echo "[2/5] Checking Minikube..."
MINIKUBE_STATUS=$(minikube status --format='{{.Host}}' 2>/dev/null || echo "Stopped")
if [ "$MINIKUBE_STATUS" != "Running" ]; then
  echo "Starting Minikube..."
  minikube start --cpus=2 --memory=6144 --driver=docker
  echo "Minikube started."
else
  echo "Minikube already running."
fi

# Step 3: Wait for API server
echo "[3/5] Waiting for Kubernetes API server..."
until kubectl get nodes &>/dev/null; do
  echo "  Waiting..."
  sleep 3
done
echo "API server is ready."

# Step 4: Apply manifests if namespace doesn't exist
echo "[4/5] Checking if services are deployed..."
if ! kubectl get namespace hotel &>/dev/null; then
  echo "Deploying all manifests..."
  kubectl apply -f k8s/namespace.yaml
  kubectl apply -f k8s/secrets.yaml
  kubectl apply -f k8s/configmaps/
  kubectl apply -f k8s/database/mysql.yaml
  kubectl apply -f k8s/infrastructure/
  kubectl apply -f k8s/services/
  kubectl apply -f k8s/frontend/
  kubectl apply -f k8s/monitoring/
  echo "Manifests applied."
else
  echo "Already deployed. Skipping."
fi

# Step 5: Wait for all pods to be ready
echo "[5/5] Waiting for all pods to be Running..."
kubectl wait --for=condition=ready pod --all -n hotel --timeout=300s
echo "All pods are ready."

# Step 6: Kill any existing port-forwards
echo "Cleaning up old port-forwards..."
pkill -f "kubectl port-forward" 2>/dev/null || true
sleep 2

# Step 7: Start port-forward with auto-restart loop
echo "Starting port-forward with auto-restart..."

cat > /tmp/pf-frontend.sh << 'EOF'
#!/bin/bash
while true; do
  echo "[$(date)] Starting frontend port-forward..."
  kubectl port-forward svc/frontend 30400:4000 -n hotel --address=0.0.0.0
  echo "[$(date)] frontend port-forward died. Restarting in 3s..."
  sleep 3
done
EOF

cat > /tmp/pf-gateway.sh << 'EOF'
#!/bin/bash
while true; do
  echo "[$(date)] Starting api-gateway port-forward..."
  kubectl port-forward svc/api-gateway 30080:8080 -n hotel --address=0.0.0.0
  echo "[$(date)] api-gateway port-forward died. Restarting in 3s..."
  sleep 3
done
EOF

cat > /tmp/pf-grafana.sh << 'EOF'
#!/bin/bash
while true; do
  echo "[$(date)] Starting grafana port-forward..."
  kubectl port-forward svc/grafana 3000:3000 -n hotel --address=0.0.0.0
  echo "[$(date)] grafana port-forward died. Restarting in 3s..."
  sleep 3
done
EOF

chmod +x /tmp/pf-frontend.sh /tmp/pf-gateway.sh /tmp/pf-grafana.sh

nohup /tmp/pf-frontend.sh > ~/logs/pf-frontend.log 2>&1 &
nohup /tmp/pf-gateway.sh > ~/logs/pf-gateway.log 2>&1 &
nohup /tmp/pf-grafana.sh > ~/logs/pf-grafana.log 2>&1 &

echo ""
echo "=============================="
echo " All services started!"
echo "=============================="
echo " Frontend  -> http://44.200.221.231:30400"
echo " API       -> http://44.200.221.231:30080"
echo " Grafana   -> http://44.200.221.231:3000  (admin/admin123)"
echo " Prometheus-> http://44.200.221.231:9090"
echo "=============================="
echo ""
echo "Logs:"
echo "  tail -f ~/logs/pf-frontend.log"
echo "  tail -f ~/logs/pf-gateway.log"
echo "  tail -f ~/logs/pf-grafana.log"
echo ""
echo "Pod status:"
kubectl get pods -n hotel
