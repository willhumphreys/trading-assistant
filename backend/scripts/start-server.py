import os
import subprocess
import sys

# Load environment variables from .env file
with open('.env') as f:
    for line in f:
        if '=' in line:
            key, value = line.strip().split('=', 1)
            os.environ[key] = value

# Check if the argument is passed
if len(sys.argv) < 2:
    print("Usage: python start_server.py <profile_name>")
    sys.exit(1)

profile_name: str = sys.argv[1]

print(f"Starting server with profile: {profile_name}")

# Define the path to your JDK and the jar file
java_home = "/home/will/.jdks/temurin-17.0.9"
jar_path = "target/darwinex-client-0.0.1-SNAPSHOT.jar"

# Set JAVA_HOME for this session
os.environ["JAVA_HOME"] = java_home

# Start the server with the provided profile name
subprocess.run([os.path.join(java_home, 'bin', 'java'), '-jar', jar_path, f'--spring.profiles.active={profile_name}'])
