#!/bin/bash

# Base directory containing the directories to transfer
SOURCE_DIR="."

# Destination details
DEST_USER="will"
DEST_HOST="k8s-worker01"
DEST_PATH="/home/will"

# Loop through directories in the source directory
for dir in "$SOURCE_DIR"/*; do
    if [ -d "$dir" ]; then
        echo "Transferring directory: $dir"
        scp -r "$dir" "$DEST_USER@$DEST_HOST:$DEST_PATH"
        if [ $? -eq 0 ]; then
            echo "Successfully transferred: $dir"
        else
            echo "Failed to transfer: $dir"
        fi
    fi
done

echo "All transfers complete."