#!/bin/bash

# Source directory on the remote server
REMOTE_SOURCE_DIR="/home/will/.cxoffice/MetaTrader_5-3/drive_c/Program Files/MetaTrader 5/MQL5/Files/*"

# Destination directory on your local machine
LOCAL_DEST_DIR="output"

# Remote server details
REMOTE_USER="will"
REMOTE_HOST="k8s-worker01"

# Function to check if the local destination directory exists
check_or_create_local_directory() {
    if [ ! -d "$LOCAL_DEST_DIR" ]; then
        echo "Local destination directory $LOCAL_DEST_DIR does not exist. Creating it now..."
        mkdir -p "$LOCAL_DEST_DIR"
        if [ $? -ne 0 ]; then
            echo "Error: Failed to create local destination directory $LOCAL_DEST_DIR."
            exit 1
        fi
    fi
}

# Function to copy files from the remote server to the local machine
copy_files_from_remote() {
    echo "Copying files from $REMOTE_USER@$REMOTE_HOST:$REMOTE_SOURCE_DIR to $LOCAL_DEST_DIR..."
    scp -r $REMOTE_USER@$REMOTE_HOST:"$REMOTE_SOURCE_DIR" $LOCAL_DEST_DIR
    if [ $? -eq 0 ]; then
        echo "File transfer completed successfully."
    else
        echo "Error: File transfer failed."
        exit 1
    fi
}

# Main script execution
echo "Checking local destination directory..."
check_or_create_local_directory

echo "Starting file transfer from remote server to local machine..."
copy_files_from_remote

echo "Script finished. All operations complete."