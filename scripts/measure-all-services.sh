#!/bin/bash
# Measure code reduction metrics for all 6 services
# This script analyzes controller, event publishing, and configuration code

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

# Services to analyze
SERVICES=(
    "cuckoo-user"
    "cuckoo-product"
    "cuckoo-inventory"
    "cuckoo-order"
    "cuckoo-payment"
    "cuckoo-notification"
)

# Colors
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m'

log() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

section() {
    echo -e "\n${BLUE}=== $1 ===${NC}\n"
}

# Count non-empty, non-comment lines in a file
count_lines() {
    local file=$1
    if [ -f "$file" ]; then
        grep -v '^\s*$' "$file" | grep -v '^\s*//' | grep -v '^\s*/\*' | grep -v '^\s*\*' | wc -l | tr -d ' '
    else
        echo "0"
    fi
}

# Analyze controller files
analyze_controller() {
    local service=$1
    local controller_file="$PROJECT_ROOT/$service/src/main/java/com/pingxin403/cuckoo/${service#cuckoo-}/controller/*Controller.java"
    
    local total_lines=0
    local extends_base=0
    local log_calls=0
    local helper_methods=0
    
    for file in $controller_file; do
        if [ -f "$file" ]; then
            local lines=$(count_lines "$file")
            total_lines=$((total_lines + lines))
            
            # Check if extends BaseController
            if grep -q "extends BaseController" "$file"; then
                extends_base=1
            fi
            
            # Count logRequest/logResponse calls
            local logs=$(grep -c "logRequest\|logResponse" "$file" || echo "0")
            log_calls=$((log_calls + logs))
            
            # Count usage of created(), ok(), noContent()
            local helpers=$(grep -c "return created(\|return ok(\|return noContent(" "$file" || echo "0")
            helper_methods=$((helper_methods + helpers))
        fi
    done
    
    echo "$total_lines|$extends_base|$log_calls|$helper_methods"
}

# Analyze event publishing
analyze_events() {
    local service=$1
    local service_dir="$PROJECT_ROOT/$service/src/main/java"
    
    local event_publisher_usage=0
    local kafka_publisher_usage=0
    local event_files=0
    
    # Count EventPublisherUtil usage
    event_publisher_usage=$(find "$service_dir" -name "*.java" -exec grep -l "EventPublisherUtil" {} \; 2>/dev/null | wc -l | tr -d ' ')
    
    # Count direct KafkaEventPublisher usage
    kafka_publisher_usage=$(find "$service_dir" -name "*.java" -exec grep -l "KafkaEventPublisher" {} \; 2>/dev/null | wc -l | tr -d ' ')
    
    # Count event publishing calls
    event_files=$(find "$service_dir" -name "*.java" -exec grep -c "\.publish(" {} \; 2>/dev/null | awk '{sum+=$1} END {print sum}')
    if [ -z "$event_files" ]; then
        event_files=0
    fi
    
    echo "$event_publisher_usage|$kafka_publisher_usage|$event_files"
}

# Analyze configuration
analyze_config() {
    local service=$1
    local config_file="$PROJECT_ROOT/$service/src/main/resources/application.yml"
    
    local total_lines=0
    local imports_common=0
    
    if [ -f "$config_file" ]; then
        total_lines=$(count_lines "$config_file")
        
        # Check if imports common config
        if grep -q "import.*application-common" "$config_file"; then
            imports_common=1
        fi
    fi
    
    echo "$total_lines|$imports_common"
}

# Analyze DTOMapper usage
analyze_mapper() {
    local service=$1
    local mapper_dir="$PROJECT_ROOT/$service/src/main/java/com/pingxin403/cuckoo/${service#cuckoo-}/mapper"
    
    local mapper_count=0
    local implements_dto_mapper=0
    
    if [ -d "$mapper_dir" ]; then
        mapper_count=$(find "$mapper_dir" -name "*Mapper.java" | wc -l | tr -d ' ')
        
        # Check if any mapper implements DTOMapper
        if find "$mapper_dir" -name "*Mapper.java" -exec grep -q "implements DTOMapper" {} \; 2>/dev/null; then
            implements_dto_mapper=1
        fi
    fi
    
    echo "$mapper_count|$implements_dto_mapper"
}

# Main analysis
section "Analyzing All Services"

echo "Service,Controller_Lines,Extends_Base,Log_Calls,Helper_Methods,EventPublisher_Files,Kafka_Files,Publish_Calls,Config_Lines,Imports_Common,Mapper_Count,Implements_DTOMapper"

for service in "${SERVICES[@]}"; do
    log "Analyzing $service..."
    
    controller_data=$(analyze_controller "$service")
    event_data=$(analyze_events "$service")
    config_data=$(analyze_config "$service")
    mapper_data=$(analyze_mapper "$service")
    
    echo "$service,$controller_data,$event_data,$config_data,$mapper_data"
done

section "Analysis Complete"
log "Results saved to CSV format above"
