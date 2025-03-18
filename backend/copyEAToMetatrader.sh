#!/bin/bash

# Define source directory
src_dir="expert-advisors"

# Define destination server and path
dest_user="will"
dest_host="k8s-worker01"
#dest_path="/home/will/.cxoffice/MetaTrader_5-3/drive_c/Program Files/MetaTrader 5/MQL5/Experts/Advisors"
dest_path="/home/will/.cxoffice/MetaTrader_5-2/drive_c/Program Files/MetaTrader 5/MQL5/Experts/Advisors"


# Check if the source directory exists
if [ ! -d "$src_dir" ]; then
    echo "Source directory '$src_dir' does not exist. Exiting."
    exit 1
fi

# Iterate over all files in the source directory
for file in "$src_dir"/*; do
    if [ -f "$file" ]; then
        echo "Copying $file to $dest_user@$dest_host:$dest_path"
        scp "$file" "$dest_user@$dest_host:$dest_path"

        # Check if the transfer was successful
        if [ $? -eq 0 ]; then
            echo "File $file successfully transferred."
        else
            echo "Failed to transfer $file."
        fi
    fi
done