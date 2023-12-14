#Docker

```bash
docker build -t trading-assistant .
```

```bash
docker run --env-file .env \
  --env SPRING_PROFILE=dev \
  --network="host" \
  -p 9010:9010 -p 9011:9011 -p 8080:8080 -p 3306:3306 \
  -v /home/will/code/darwinex-client/backend/accounts:/accounts \
  -v /home/will/code/darwinex-client/backend/mochi-graphs:/mochi-graphs \
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
kubectl delete -f trading-assistant-deployment.yaml

kubectl create configmap trading-assistant-env --from-env-file=.env
kubectl delete configmap trading-assistant-env

kubectl describe deployment trading-assistant
kubectl get secrets
kubectl.exe apply -f .\kubernetes\manifests\dev\mysql-service.yaml
```