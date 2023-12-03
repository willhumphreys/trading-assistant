import os
import platform
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

# Define the JDK path based on the OS
if platform.system() == 'Windows':
    java_home = r"C:\Users\user\.jdks\temurin-17.0.9"
else:
    java_home = "/home/will/.jdks/temurin-17.0.9"

# Define the path to the jar file
jar_path = "darwinex-client-0.0.1-SNAPSHOT.jar"

# Set JAVA_HOME for this session
os.environ["JAVA_HOME"] = java_home

# Set the executable based on the OS
java_executable = 'java.exe' if platform.system() == 'Windows' else 'java'

# Start the server with the provided profile name
subprocess.run(
    [os.path.join(java_home, 'bin', java_executable), '-jar', jar_path, f'--spring.profiles.active={profile_name}'])
