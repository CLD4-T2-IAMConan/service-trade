#!/bin/bash

# MSA Service Template - Cleanup Script
# 로컬 개발 환경을 정리하는 스크립트

set -e

echo "=========================================="
echo "Cleanup Script"
echo "=========================================="

# Stop and remove Docker containers
echo "1. Stopping Docker containers..."
if [ -f "docker-compose.yml" ]; then
    docker-compose down -v
    echo "   ✓ Docker containers stopped and removed"
else
    echo "   - docker-compose.yml not found, skipping"
fi

# Clean Gradle build
echo "2. Cleaning Gradle build..."
if [ -f "gradlew" ]; then
    ./gradlew clean
    echo "   ✓ Gradle build cleaned"
else
    echo "   - gradlew not found, skipping"
fi

# Remove local Gradle cache (optional)
read -p "Remove Gradle cache? (y/n) " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    rm -rf .gradle
    echo "   ✓ Gradle cache removed"
fi

# Delete kind clusters
echo "3. Checking for kind clusters..."
if command -v kind &> /dev/null; then
    CLUSTERS=$(kind get clusters 2>/dev/null | grep "msa-" || true)
    if [ -n "$CLUSTERS" ]; then
        echo "   Found clusters: $CLUSTERS"
        read -p "   Delete all msa-* clusters? (y/n) " -n 1 -r
        echo
        if [[ $REPLY =~ ^[Yy]$ ]]; then
            echo "$CLUSTERS" | while read -r cluster; do
                kind delete cluster --name "$cluster"
                echo "   ✓ Deleted cluster: $cluster"
            done
        fi
    else
        echo "   - No msa-* clusters found"
    fi
else
    echo "   - kind not installed, skipping"
fi

# Remove Docker images
echo "4. Checking for Docker images..."
IMAGES=$(docker images | grep "template-service" | awk '{print $3}' || true)
if [ -n "$IMAGES" ]; then
    read -p "   Remove template-service Docker images? (y/n) " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        echo "$IMAGES" | while read -r image; do
            docker rmi -f "$image" 2>/dev/null || true
        done
        echo "   ✓ Docker images removed"
    fi
else
    echo "   - No template-service images found"
fi

# Clean IDE files (optional)
read -p "5. Remove IDE files (.idea, *.iml)? (y/n) " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    rm -rf .idea
    find . -name "*.iml" -type f -delete
    echo "   ✓ IDE files removed"
fi

echo "=========================================="
echo "Cleanup Complete!"
echo "=========================================="
