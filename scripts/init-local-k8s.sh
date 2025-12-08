#!/bin/bash

# MSA Service Template - Local Kubernetes Setup Script
# kind 클러스터를 생성하고 서비스를 배포하는 스크립트

set -e

CLUSTER_NAME=${1:-msa-local}
SERVICE_NAME=${2:-template-service}

echo "=========================================="
echo "Local Kubernetes Setup"
echo "=========================================="
echo "Cluster: $CLUSTER_NAME"
echo "Service: $SERVICE_NAME"
echo "=========================================="

# Check if kind is installed
if ! command -v kind &> /dev/null; then
    echo "Error: kind is not installed"
    echo "Please install kind: https://kind.sigs.k8s.io/docs/user/quick-start/#installation"
    exit 1
fi

# Check if kubectl is installed
if ! command -v kubectl &> /dev/null; then
    echo "Error: kubectl is not installed"
    echo "Please install kubectl: https://kubernetes.io/docs/tasks/tools/"
    exit 1
fi

# Check if cluster already exists
if kind get clusters 2>/dev/null | grep -q "^${CLUSTER_NAME}$"; then
    echo "Cluster '${CLUSTER_NAME}' already exists"
    read -p "Delete and recreate? (y/n) " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        echo "Deleting existing cluster..."
        kind delete cluster --name "$CLUSTER_NAME"
    else
        echo "Using existing cluster"
        kubectl cluster-info --context "kind-${CLUSTER_NAME}"
        exit 0
    fi
fi

# Create kind cluster
echo "Creating kind cluster..."
kind create cluster --name "$CLUSTER_NAME"

# Wait for cluster to be ready
echo "Waiting for cluster to be ready..."
kubectl wait --for=condition=Ready nodes --all --timeout=60s

# Build Docker image
echo "Building Docker image..."
docker build -t "${SERVICE_NAME}:latest" .

# Load image into kind
echo "Loading image into kind cluster..."
kind load docker-image "${SERVICE_NAME}:latest" --name "$CLUSTER_NAME"

# Deploy to Kubernetes
echo "Deploying to Kubernetes..."
kubectl apply -f k8s/

# Wait for deployments to be ready
echo "Waiting for deployments to be ready..."
kubectl wait --for=condition=available --timeout=300s deployment/postgres
kubectl wait --for=condition=available --timeout=300s deployment/${SERVICE_NAME}

# Show status
echo "=========================================="
echo "Deployment Complete!"
echo "=========================================="
echo ""
kubectl get pods
echo ""
kubectl get services
echo ""
echo "=========================================="
echo "To access the service:"
echo "  kubectl port-forward svc/${SERVICE_NAME} 8080:80"
echo ""
echo "To view logs:"
echo "  kubectl logs -f deployment/${SERVICE_NAME}"
echo ""
echo "To delete cluster:"
echo "  kind delete cluster --name ${CLUSTER_NAME}"
echo "=========================================="
