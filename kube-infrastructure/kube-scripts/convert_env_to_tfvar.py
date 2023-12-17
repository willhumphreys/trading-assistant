# Python script to read an .env file and convert the contents to TF_VAR_ environment variables

def convert_env_to_tf_var(env_file_path):
    try:
        with open(env_file_path, 'r') as file:
            lines = file.readlines()

        tf_var_lines = []
        for line in lines:
            # Splitting each line into key and value
            if '=' in line:
                key, value = line.strip().split('=', 1)
                # Converting to TF_VAR format and adding to the list
                tf_var_line = f"export TF_VAR_{key.lower()}='{value}'"
                tf_var_lines.append(tf_var_line)

        return tf_var_lines
    except FileNotFoundError:
        return ["Error: .env file not found."]

# Provide the path to your .env file
env_file_path = '../.env'
tf_var_commands = convert_env_to_tf_var(env_file_path)

# Displaying the commands or error message
for command in tf_var_commands:
    print(command)

