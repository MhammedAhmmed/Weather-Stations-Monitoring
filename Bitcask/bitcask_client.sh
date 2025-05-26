#!/bin/bash

# Configuration
PORT=9999
OUTPUT_DIR="output"
TIMESTAMP=$(date +%s)

# Check if netcat is available
if ! command -v nc &> /dev/null; then
    echo "Error: netcat (nc) is required but not installed."
    echo "On Ubuntu/Debian: sudo apt-get install netcat"
    echo "On macOS: brew install netcat"
    exit 1
fi

# Function to run a single client
run_client() {
    local thread_num=$1
    local output_file="${OUTPUT_DIR}/${TIMESTAMP}_thread_${thread_num}.csv"
    
    (echo "--perf-test"; sleep 1) | nc localhost "$PORT" > "$output_file"
    echo "Thread $thread_num completed - output in $output_file"
}

# Handle different commands
case "$1" in
    --view-all)
        mkdir -p "$OUTPUT_DIR"
        OUTPUT_FILE="${OUTPUT_DIR}/${TIMESTAMP}.csv"
        
        (echo "--view-all"; sleep 1) | nc localhost "$PORT" > "$OUTPUT_FILE"
        echo "Output written to $OUTPUT_FILE"
        ;;
        
    --view)
        if [ -z "$2" ]; then
            echo "Usage: ./bitcask_client.sh --view --key=KEY_VALUE"
            exit 1
        fi
        
        KEY=${2#--key=}
        if ! [[ "$KEY" =~ ^[0-9]+$ ]]; then
            echo "Error: Key must be a number"
            exit 1
        fi
        
        (echo "--view --key=$KEY"; sleep 1) | nc localhost "$PORT"
        ;;
        
    --perf)
        if [ -z "$2" ]; then
            echo "Usage: ./bitcask_client.sh --perf --clients=NUM_CLIENTS"
            exit 1
        fi
        
        CLIENTS=${2#--clients=}
        if ! [[ "$CLIENTS" =~ ^[0-9]+$ ]]; then
            echo "Error: Number of clients must be a positive integer"
            exit 1
        fi
        
        mkdir -p "$OUTPUT_DIR"
        echo "Starting performance test with $CLIENTS clients..."
        
        # Run clients in parallel
        for ((i=1; i<=$CLIENTS; i++)); do
            run_client $i &
        done
        
        # Wait for all clients to finish
        wait
        echo "Performance test completed"
        ;;
        
    *)
        echo "Usage:"
        echo "  ./bitcask_client.sh --view-all"
        echo "  ./bitcask_client.sh --view --key=KEY_VALUE"
        echo "  ./bitcask_client.sh --perf --clients=NUM_CLIENTS"
        exit 1
        ;;
esac
