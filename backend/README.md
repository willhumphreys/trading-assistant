#Docker

```bash
docker build -t trading-assistant .
```

```bash
docker run --env-file .env \
  --env SPRING_PROFILE=dev \
  --network="host" \
  -p 9010:9010 -p 9011:9011 -p 8080:8080 -p 3306:3306 \
  -v /home/will/code/trading-assistant/backend/accounts:/accounts \
  -v /home/will/code/trading-assistant/backend/mochi-graphs:/mochi-graphs \
  -v /home/will/.cxoffice/MetaTrader_5-3_584/drive_c/Program\ Files/MetaTrader\ 5/MQL5/Files/DWX:/home/will/.cxoffice/MetaTrader_5-3_584/drive_c/Program\ Files/MetaTrader\ 5/MQL5/Files/DWX \
  trading-assistant
```

#JMX
To connect via JMX

```
service:jmx:rmi:///jndi/rmi://192.168.1.202:9010/jmxrmi
```

VisualVM is installed here

```
/opt/visualvm/bin/visualvm
```

## Azure Deployment

```bash
az group create --name TradingAssistantResourceGroup --location germanywestcentral
az container create   --resource-group TradingAssistantResourceGroup   --name tradingAssistantContainer   --image ghcr.io/willhumphreys/trading-assistant:latest   --dns-name-label tradingassistantapp-123   --ports 8080
az container show   --resource-group TradingAssistantResourceGroup   --name trading-assist-container    --out table
az container logs   --resource-group TradingAssistantResourceGroup   --name trading-assist-container
```

How to start mysql

```powershell
docker run --name mysql-container -e MYSQL_ROOT_PASSWORD = password -v mysql_data:/var/lib/mysql -p 3306:3306 -d mysql
```

## Set the profile

```powershell
$env:AWS_PROFILE = "AdministratorAccess-573591465159"
aws sts get-caller-identity     
```

## Create the kubernetes secret for the docker registry

```powershell
aws ecr get-login-password --region eu-central-1 > password.txt
kubectl delete secret regcred
kubectl create secret docker-registry regcred --docker-server = 573591465159.dkr.ecr.eu-central-1.amazonaws.com --docker-username = AWS --docker-password =$( cat password.txt ) --docker-email = whumphreys@gmail.com
```

```powershell
kubectl delete configmap trading-assistant-env
kubectl create configmap trading-assistant-env --from-env-file=.env


kubectl apply -f trading-assistant-service.yaml
kubectl apply -f trading-assistant-deployment.yaml
kubectl delete -f trading-assistant-deployment.yaml

kubectl create configmap trading-assistant-env --from-env-file=.env
kubectl delete configmap trading-assistant-env

kubectl describe deployment trading-assistant
kubectl get secrets
kubectl.exe apply -f .\kubernetes\manifests\dev\mysql-service.yaml
```

## Copying to config changes to the server

Copy account-setup-groups.json to the server

```bash
scp /home/will/code/trading-assistant-backend/backend/accounts/account-setup-groups.json will@192.168.1.202:/home/will/accounts
```

Copy the setup-group-both.json to the server

```bash
scp /home/will/code/trading-assistant-backend/backend/accounts/setup-groups/setup-group-both.json will@192.168.1.202:/home/will/accounts/setup-groups
```

Db backup and restore

```bash
DB_PASSWORD=$(kubectl get secret my-secrets -n trading-assistant -o jsonpath='{.data.dbPassword}' | base64 --decode)
echo "Password retrieved: ${#DB_PASSWORD} characters long"

# Copy mysql pod into next command
kubectl exec -n trading-assistant mysql-container-6c679bdfb7-bmqxk -- mysqldump -u root -p"$DB_PASSWORD" --all-databases > mysql_backup.sql

ls -lh mysql_backup.sql
kubectl get deployment -n trading-assistant | grep mysql
kubectl scale deployment mysql-container -n trading-assistant --replicas=0
cdktf deploy trading-assistant-stateful
kubectl get pods -n trading-assistant -w

# Copy mysql pod into next command
NEW_POD=mysql-container-6c679bdfb7-8t7qn

kubectl cp mysql_backup.sql trading-assistant/$NEW_POD:/tmp/
cat mysql_backup.sql | kubectl exec -i -n trading-assistant $NEW_POD -- mysql -u root -p"$DB_PASSWORD"
kubectl exec -n trading-assistant $NEW_POD -- mysql -u root -p"$DB_PASSWORD" -e "SHOW DATABASES;"
kubectl exec -n trading-assistant $NEW_POD -- rm /tmp/mysql_backup.sql
```

Manually recreate the metatrader user

```bash
CREATE DATABASE IF NOT EXISTS metatrader;
CREATE USER IF NOT EXISTS 'metatrader'@'%' IDENTIFIED  BY 'ReplaceWithPassword;
CREATE USER IF NOT EXISTS 'metatrader'@'localhost' IDENTIFIED BY 'ReplaceWithPassword';
```