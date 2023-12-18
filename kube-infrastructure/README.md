# Kubernetes Infrastructure Project

## Project Setup

1. Install the project dependencies:

```bash
npm install
```

2. Compile the TypeScript code into JavaScript:

```bash
npm run compile
```

## Deploying the Infrastructure

1. Generate the Terraform configuration files:

```bash
cdktf synth
```

2. Navigate to the `cdktf.out` directory:

```bash
cd cdktf.out
```

3. Initialize your Terraform workspace:

```bash
terraform init
```

4. Apply the Terraform configuration:

```bash
terraform apply
```

This will prompt you to confirm that you want to apply the changes. Type `yes` to proceed.

## Destroying the Infrastructure

To destroy the resources you've created, run the following command in the `cdktf.out` directory:

```bash
terraform destroy
```

This will prompt you to confirm that you want to destroy the resources. Type `yes` to proceed.

## Checking the Status of Resources

You can use the `kubectl` command-line tool to check the status of your Kubernetes resources. Here are some useful
commands:

- List all pods:

```bash
kubectl get pods
```

- List all services:

```bash
kubectl get svc
```

- List all deployments:

```bash
kubectl get deployments
```

- List all Persistent Volume Claims (PVCs):

```bash
kubectl get pvc
```

- Describe a specific resource:

```bash
kubectl describe <resource-type> <resource-name>
```

Please replace the placeholders with the actual values for your project and add any additional information as needed.

| Task                                    | CDK for Terraform | Terraform           |
|-----------------------------------------|-------------------|---------------------|
| Initialize                              | `cdktf get`       | `terraform init`    |
| Compile TypeScript code into JavaScript | `npm run compile` | N/A                 |
| Generate Terraform configuration files  | `cdktf synth`     | N/A                 |
| Plan changes                            | `cdktf diff`      | `terraform plan`    |
| Apply changes                           | `cdktf deploy`    | `terraform apply`   |
| Destroy resources                       | `cdktf destroy`   | `terraform destroy` |

#Initialise the database

```sql
CREATE DATABASE IF NOT EXISTS metatrader;
CREATE USER IF NOT EXISTS 'metatrader'@'%' IDENTIFIED WITH mysql_native_password BY 'password';
CREATE USER IF NOT EXISTS 'metatrader'@'localhost' IDENTIFIED WITH mysql_native_password BY 'password';
GRANT ALL PRIVILEGES ON metatrader.* TO 'metatrader'@'%';
GRANT ALL PRIVILEGES ON *.* TO 'root'@'%' WITH GRANT OPTION;
```

https://www.reddit.com/r/kubernetes/comments/h7wfnc/how_do_i_derive_certificate_pem_data_from/