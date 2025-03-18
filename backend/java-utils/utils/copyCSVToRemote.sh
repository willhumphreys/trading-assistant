#!/bin/bash

# Local directory containing the files to be copied
LOCAL_DIR="/home/will/code/trading-assistant/backend/java-utils/output"

# Remote directory where the files will be copied
REMOTE_DEST_DIR="/home/will/.cxoffice/MetaTrader_5-3/drive_c/Program Files/MetaTrader 5/MQL5/Files"

# Remote server details
REMOTE_USER="will"
REMOTE_HOST="k8s-worker01"

# Function to check if the local directory exists and contains files
check_local_dir() {
    echo "Checking if $LOCAL_DIR exists and contains files..."
    if [ ! -d "$LOCAL_DIR" ]; then
        echo "Error: Directory $LOCAL_DIR does not exist."
        exit 1
    fi

    if [ -z "$(ls -A "$LOCAL_DIR")" ]; then
        echo "Error: No files found in $LOCAL_DIR."
        exit 1
    fi
}

# Function to copy all files from the local directory to the remote server
copy_files_to_remote() {
    echo "Copying files from $LOCAL_DIR to $REMOTE_USER@$REMOTE_HOST:$REMOTE_DEST_DIR..."

    scp "$LOCAL_DIR"/* $REMOTE_USER@$REMOTE_HOST:"$REMOTE_DEST_DIR"
    if [ $? -eq 0 ]; then
        echo "File transfer completed successfully."
    else
        echo "Error: File transfer failed."
        exit 1
    fi
}

# Main script execution
check_local_dir
copy_files_to_remote

echo "Operation completed."