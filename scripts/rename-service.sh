#!/bin/bash

# MSA Service Template - Service Rename Script
# 이 스크립트는 템플릿의 모든 'template' 문자열을 실제 서비스 이름으로 변경합니다.

set -e

if [ -z "$1" ]; then
    echo "Usage: ./scripts/rename-service.sh <service-name>"
    echo "Example: ./scripts/rename-service.sh account"
    exit 1
fi

SERVICE_NAME=$1
SERVICE_NAME_LOWER=$(echo "$SERVICE_NAME" | tr '[:upper:]' '[:lower:]')
SERVICE_NAME_UPPER=$(echo "$SERVICE_NAME" | tr '[:lower:]' '[:upper:]')
SERVICE_NAME_CAMEL=$(echo "$SERVICE_NAME" | sed 's/\b\(.\)/\u\1/')

echo "=========================================="
echo "MSA Service Rename Script"
echo "=========================================="
echo "Renaming template to: $SERVICE_NAME"
echo "=========================================="

# Backup warning
read -p "이 작업은 되돌릴 수 없습니다. 계속하시겠습니까? (y/n) " -n 1 -r
echo
if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    echo "취소되었습니다."
    exit 1
fi

echo "1. Renaming in settings.gradle..."
sed -i.bak "s/template-service/${SERVICE_NAME_LOWER}-service/g" settings.gradle
rm -f settings.gradle.bak

echo "2. Renaming in application.yml..."
sed -i.bak "s/template-service/${SERVICE_NAME_LOWER}-service/g" src/main/resources/application.yml
sed -i.bak "s/template_db/${SERVICE_NAME_LOWER}_db/g" src/main/resources/application.yml
rm -f src/main/resources/application.yml.bak

echo "3. Renaming in Helm Chart.yaml..."
sed -i.bak "s/template-service/${SERVICE_NAME_LOWER}-service/g" helm/Chart.yaml
sed -i.bak "s/MSA Template Service/MSA ${SERVICE_NAME_CAMEL} Service/g" helm/Chart.yaml
rm -f helm/Chart.yaml.bak

echo "4. Renaming in Helm values.yaml..."
sed -i.bak "s/template-service/${SERVICE_NAME_LOWER}-service/g" helm/values.yaml
sed -i.bak "s/template_db/${SERVICE_NAME_LOWER}_db/g" helm/values.yaml
rm -f helm/values.yaml.bak

echo "5. Renaming in Kubernetes manifests..."
for file in k8s/*.yaml; do
    sed -i.bak "s/template-service/${SERVICE_NAME_LOWER}-service/g" "$file"
    sed -i.bak "s/template_db/${SERVICE_NAME_LOWER}_db/g" "$file"
    sed -i.bak "s/template-config/${SERVICE_NAME_LOWER}-config/g" "$file"
    sed -i.bak "s/template-secret/${SERVICE_NAME_LOWER}-secret/g" "$file"
    rm -f "${file}.bak"
done

echo "6. Renaming in docker-compose.yml..."
sed -i.bak "s/template-service/${SERVICE_NAME_LOWER}-service/g" docker-compose.yml
sed -i.bak "s/template-postgres/${SERVICE_NAME_LOWER}-postgres/g" docker-compose.yml
sed -i.bak "s/template_db/${SERVICE_NAME_LOWER}_db/g" docker-compose.yml
rm -f docker-compose.yml.bak

echo "7. Renaming Java package..."
# Rename package directory
if [ -d "src/main/java/com/company/template" ]; then
    mv src/main/java/com/company/template "src/main/java/com/company/${SERVICE_NAME_LOWER}"
    echo "   - Renamed src/main/java/com/company/template to ${SERVICE_NAME_LOWER}"
fi

if [ -d "src/test/java/com/company/template" ]; then
    mv src/test/java/com/company/template "src/test/java/com/company/${SERVICE_NAME_LOWER}"
    echo "   - Renamed src/test/java/com/company/template to ${SERVICE_NAME_LOWER}"
fi

# Update package declarations in Java files
find src -name "*.java" -type f -exec sed -i.bak "s/package com.company.template/package com.company.${SERVICE_NAME_LOWER}/g" {} \;
find src -name "*.java" -type f -exec sed -i.bak "s/import com.company.template/import com.company.${SERVICE_NAME_LOWER}/g" {} \;
find src -name "*.java.bak" -type f -delete

# Rename main application class
if [ -f "src/main/java/com/company/${SERVICE_NAME_LOWER}/TemplateApplication.java" ]; then
    mv "src/main/java/com/company/${SERVICE_NAME_LOWER}/TemplateApplication.java" \
       "src/main/java/com/company/${SERVICE_NAME_LOWER}/${SERVICE_NAME_CAMEL}Application.java"
    sed -i.bak "s/TemplateApplication/${SERVICE_NAME_CAMEL}Application/g" \
        "src/main/java/com/company/${SERVICE_NAME_LOWER}/${SERVICE_NAME_CAMEL}Application.java"
    rm -f "src/main/java/com/company/${SERVICE_NAME_LOWER}/${SERVICE_NAME_CAMEL}Application.java.bak"
fi

# Update test class
if [ -f "src/test/java/com/company/${SERVICE_NAME_LOWER}/TemplateApplicationTests.java" ]; then
    mv "src/test/java/com/company/${SERVICE_NAME_LOWER}/TemplateApplicationTests.java" \
       "src/test/java/com/company/${SERVICE_NAME_LOWER}/${SERVICE_NAME_CAMEL}ApplicationTests.java"
    sed -i.bak "s/TemplateApplicationTests/${SERVICE_NAME_CAMEL}ApplicationTests/g" \
        "src/test/java/com/company/${SERVICE_NAME_LOWER}/${SERVICE_NAME_CAMEL}ApplicationTests.java"
    rm -f "src/test/java/com/company/${SERVICE_NAME_LOWER}/${SERVICE_NAME_CAMEL}ApplicationTests.java.bak"
fi

# Update HealthController
sed -i.bak "s/\"service\", \"template\"/\"service\", \"${SERVICE_NAME_LOWER}\"/g" \
    "src/main/java/com/company/${SERVICE_NAME_LOWER}/controller/HealthController.java"
rm -f "src/main/java/com/company/${SERVICE_NAME_LOWER}/controller/HealthController.java.bak"

echo "=========================================="
echo "완료!"
echo "=========================================="
echo "변경 사항:"
echo "  - 서비스 이름: template → ${SERVICE_NAME_LOWER}"
echo "  - 패키지: com.company.template → com.company.${SERVICE_NAME_LOWER}"
echo "  - 메인 클래스: TemplateApplication → ${SERVICE_NAME_CAMEL}Application"
echo "  - 데이터베이스: template_db → ${SERVICE_NAME_LOWER}_db"
echo "=========================================="
echo ""
echo "다음 단계:"
echo "  1. git status로 변경사항 확인"
echo "  2. ./gradlew build로 빌드 테스트"
echo "  3. git add . && git commit -m \"chore: rename service to ${SERVICE_NAME_LOWER}\""
echo "=========================================="
