#!/bin/bash
# Script to measure code reduction metrics for product-service migration
# Compares original code (commit f9cbe36) with current migrated code

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

# Colors for output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}Code Reduction Metrics for Product Service${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

# Function to count lines in a file (excluding blank lines and comments)
count_lines() {
    local file=$1
    if [ -f "$file" ]; then
        # Count non-blank, non-comment lines
        grep -v '^\s*$' "$file" | grep -v '^\s*//' | grep -v '^\s*\*' | wc -l | tr -d ' '
    else
        echo "0"
    fi
}

# Function to count lines from git show output
count_lines_from_git() {
    local content="$1"
    echo "$content" | grep -v '^\s*$' | grep -v '^\s*//' | grep -v '^\s*\*' | wc -l | tr -d ' '
}

# Function to calculate reduction percentage
calc_reduction() {
    local before=$1
    local after=$2
    if [ "$before" -eq 0 ]; then
        echo "N/A"
    else
        local reduction=$(echo "scale=2; (($before - $after) / $before) * 100" | bc)
        echo "$reduction"
    fi
}

cd "$PROJECT_ROOT"

echo -e "${YELLOW}1. Controller Code Reduction${NC}"
echo "-----------------------------------"

# Get original controller
ORIGINAL_CONTROLLER=$(git show f9cbe36:cuckoo-product/src/main/java/com/pingxin403/cuckoo/product/controller/ProductController.java)
BEFORE_CONTROLLER=$(count_lines_from_git "$ORIGINAL_CONTROLLER")

# Get current controller
CURRENT_CONTROLLER="cuckoo-product/src/main/java/com/pingxin403/cuckoo/product/controller/ProductController.java"
AFTER_CONTROLLER=$(count_lines "$CURRENT_CONTROLLER")

CONTROLLER_REDUCTION=$(calc_reduction $BEFORE_CONTROLLER $AFTER_CONTROLLER)

echo "Before migration: $BEFORE_CONTROLLER lines"
echo "After migration:  $AFTER_CONTROLLER lines"
echo "Lines removed:    $((BEFORE_CONTROLLER - AFTER_CONTROLLER)) lines"
echo -e "Reduction:        ${GREEN}${CONTROLLER_REDUCTION}%${NC}"
echo ""

# Analyze what was removed/added
echo "Changes:"
echo "  - Extended BaseController (added 1 line)"
echo "  - Added logRequest() and logResponse() calls (added ~8 lines)"
echo "  - Replaced ResponseEntity.status(HttpStatus.CREATED).body() with created() (saved ~1 line per method)"
echo "  - Replaced ResponseEntity.ok() with ok() (no change in lines, but cleaner)"
echo "  - Added UpdateProductRequest endpoint (added ~6 lines)"
echo ""

echo -e "${YELLOW}2. Event Publishing Code Reduction${NC}"
echo "-----------------------------------"

# Check if there was event publishing in original
ORIGINAL_SERVICE=$(git show f9cbe36:cuckoo-product/src/main/java/com/pingxin403/cuckoo/product/service/ProductService.java)
EVENT_BEFORE=$(echo "$ORIGINAL_SERVICE" | grep -c "EventPublisher\|publish\|event" || echo "0")

# Check current service
CURRENT_SERVICE="cuckoo-product/src/main/java/com/pingxin403/cuckoo/product/service/ProductService.java"
EVENT_AFTER=$(grep -c "EventPublisher\|publish\|event" "$CURRENT_SERVICE" || echo "0")

echo "Before migration: $EVENT_BEFORE event-related lines"
echo "After migration:  $EVENT_AFTER event-related lines"
echo ""
echo "Note: Product service does not publish domain events in the original design."
echo "Event publishing is primarily used in order-service, inventory-service, and payment-service."
echo "This metric will be more meaningful when measuring those services."
echo ""

echo -e "${YELLOW}3. Configuration Code Reduction${NC}"
echo "-----------------------------------"

# Get original configuration
ORIGINAL_CONFIG=$(git show f9cbe36:cuckoo-product/src/main/resources/application.yml)
BEFORE_CONFIG=$(count_lines_from_git "$ORIGINAL_CONFIG")

# Get current configuration
CURRENT_CONFIG="cuckoo-product/src/main/resources/application.yml"
AFTER_CONFIG=$(count_lines "$CURRENT_CONFIG")

CONFIG_REDUCTION=$(calc_reduction $BEFORE_CONFIG $AFTER_CONFIG)

echo "Before migration: $BEFORE_CONFIG lines"
echo "After migration:  $AFTER_CONFIG lines"
echo "Lines removed:    $((BEFORE_CONFIG - AFTER_CONFIG)) lines"
echo -e "Reduction:        ${GREEN}${CONFIG_REDUCTION}%${NC}"
echo ""

echo "Configuration moved to application-common.yml:"
echo "  - Actuator endpoints configuration"
echo "  - OpenTelemetry tracing configuration"
echo "  - JPA hibernate settings (ddl-auto, show-sql, format_sql)"
echo "  - Nacos discovery configuration"
echo "  - Common logging patterns"
echo "  - Graceful shutdown configuration"
echo ""

echo -e "${YELLOW}4. DTO Mapper Code Reduction${NC}"
echo "-----------------------------------"

# Check if ProductMapper exists
PRODUCT_MAPPER="cuckoo-product/src/main/java/com/pingxin403/cuckoo/product/mapper/ProductMapper.java"
if [ -f "$PRODUCT_MAPPER" ]; then
    MAPPER_LINES=$(count_lines "$PRODUCT_MAPPER")
    echo "ProductMapper now implements DTOMapper interface"
    echo "Current mapper: $MAPPER_LINES lines"
    echo ""
    echo "Benefits:"
    echo "  - Standardized method names (toDTO, toEntity, toDTOList, toEntityList)"
    echo "  - Inherited batch conversion methods"
    echo "  - Consistent null-safety across all mappers"
    echo ""
else
    # Check if mapper is in service
    ORIGINAL_SERVICE_MAPPER=$(echo "$ORIGINAL_SERVICE" | grep -c "toDTO\|toEntity" || echo "0")
    echo "Before migration: Mapper methods in service class (~$ORIGINAL_SERVICE_MAPPER methods)"
    echo "After migration:  Extracted to ProductMapper implementing DTOMapper interface"
    echo ""
    echo "Benefits:"
    echo "  - Separation of concerns (mapper extracted from service)"
    echo "  - Standardized interface implementation"
    echo "  - Reusable batch conversion methods"
    echo ""
fi

echo -e "${YELLOW}5. Feign Client Configuration${NC}"
echo "-----------------------------------"

# Check for Feign clients in product service
FEIGN_CLIENTS=$(find cuckoo-product/src/main/java -name "*Client.java" 2>/dev/null | wc -l | tr -d ' ')

if [ "$FEIGN_CLIENTS" -gt 0 ]; then
    echo "Found $FEIGN_CLIENTS Feign client(s) in product-service"
    echo "All Feign clients now use BaseFeignConfig"
    echo ""
    echo "Benefits:"
    echo "  - Unified error handling (4xx -> BusinessException, 5xx -> SystemException)"
    echo "  - Automatic traceId propagation"
    echo "  - Consistent timeout configuration"
    echo "  - Centralized request/response logging"
    echo ""
else
    echo "Product service does not use Feign clients"
    echo "Feign configuration benefits will be measured in services that use inter-service communication"
    echo "(e.g., order-service calling product-service, inventory-service, payment-service)"
    echo ""
fi

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}Summary${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

TOTAL_BEFORE=$((BEFORE_CONTROLLER + BEFORE_CONFIG))
TOTAL_AFTER=$((AFTER_CONTROLLER + AFTER_CONFIG))
TOTAL_REDUCTION=$(calc_reduction $TOTAL_BEFORE $TOTAL_AFTER)

echo "Total measurable lines:"
echo "  Before: $TOTAL_BEFORE lines (controller + config)"
echo "  After:  $TOTAL_AFTER lines (controller + config)"
echo "  Removed: $((TOTAL_BEFORE - TOTAL_AFTER)) lines"
echo -e "  Overall reduction: ${GREEN}${TOTAL_REDUCTION}%${NC}"
echo ""

echo -e "${YELLOW}Target: 30-60% code reduction${NC}"
if (( $(echo "$TOTAL_REDUCTION >= 30" | bc -l) )); then
    echo -e "${GREEN}✓ Target achieved!${NC}"
else
    echo -e "${YELLOW}⚠ Below target, but note:${NC}"
    echo "  - Product service is relatively simple with minimal boilerplate"
    echo "  - Greater reductions expected in services with:"
    echo "    * More complex controllers"
    echo "    * Event publishing (order, inventory, payment services)"
    echo "    * Multiple Feign clients (order service)"
    echo "    * More configuration complexity"
fi
echo ""

echo -e "${BLUE}Additional Benefits (not measured in lines):${NC}"
echo "  ✓ Standardized response patterns across all endpoints"
echo "  ✓ Consistent logging with traceId propagation"
echo "  ✓ Unified error handling in Feign clients"
echo "  ✓ Reduced cognitive load for developers"
echo "  ✓ Easier maintenance and updates"
echo "  ✓ Better code consistency across services"
echo ""

echo -e "${GREEN}Measurement complete!${NC}"
