import os
import platform
import subprocess
import sys

JAVA_VERSION = "temurin-21.0.1"

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
    java_home = r'C:\Users\user\.jdks\{0}'.format(JAVA_VERSION)
else:
    java_home = '/home/will/.jdks/{0}'.format(JAVA_VERSION)

# Define the path to the jar file
jar_path = "trading-assistant-backend-0.0.1-SNAPSHOT.jar"

# Set JAVA_HOME for this session
os.environ["JAVA_HOME"] = java_home

# Set the executable based on the OS
java_executable = 'java.exe' if platform.system() == 'Windows' else 'java'

# JMX monitoring port
jmx_port = "9010"
jmx_rmi_port = "9011"
host = "192.168.1.202"

# JMX enable arguments
jmx_arguments = [
    '-Dcom.sun.management.jmxremote',
    '-Dcom.sun.management.jmxremote.port=' + jmx_port,
    '-Dcom.sun.management.jmxremote.rmi.port=' + jmx_rmi_port,
    '-Dcom.sun.management.jmxremote.authenticate=false',
    '-Dcom.sun.management.jmxremote.ssl=false',
    '-Djava.rmi.server.hostname=' + host
]

# Start the server with JMX enabled and the provided profile name
subprocess.run(
    [os.path.join(java_home, 'bin', java_executable)] + jmx_arguments + ['-jar', jar_path,
                                                                         f'--spring.profiles.active={profile_name}'])
