#!/bin/bash

# Define source file
src="DWX_Server_MT5.mq5"

# Define destination server and path
dest_user="will"
dest_host="k8s-worker01"
dest_path="/home/will/.cxoffice/MetaTrader_5-3/drive_c/Program Files/MetaTrader 5/MQL5/Experts/Advisors"

# Use scp to copy the file to the destination
scp "$src" "$dest_user@$dest_host:$dest_path"

# Confirm the transfer was successful
if [ $? -eq 0 ]; then
    echo "File successfully transferred to $dest_user@$dest_host:$dest_path"
else
    echo "File transfer failed."
    exit 1
fi
