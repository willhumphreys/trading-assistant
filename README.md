# Trading Assistant

Automates trading on Metatrader via the Darwinex API.

- [Backend Documentation](./backend/README.md)
- [Infrastructure Documentation](./infrastructure/README.md)
- [Frontend Documentation](./frontend/README.md)
- [Kube Infrastructure Documentation](./kube-infrastructure/README.md)


# AWS

## Set the profile
```bash
export AWS_PROFILE=AdministratorAccess-573591465159
```
```powershell
setx AWS_PROFILE "AdministratorAccess-573591465159"
```

```powershell
 aws sts get-caller-identity
```

## Login to AWS SSO
```bash
aws sso login --profile AdministratorAccess-573591465159
```

## List the repositories
```bash
aws ecr describe-repositories
```

## Describe the repository
```bash
aws ecr describe-images --repository-name trading-assistant
```

## Get the region
```bash
 aws ec2 describe-availability-zones --output text --query 'AvailabilityZones[0].[RegionName]'
 ```

## Login
```bash
aws ecr get-login-password --region eu-central-1 | docker login --username AWS --password-stdin 573591465159.dkr.ecr.eu-central-1.amazonaws.com
```

## Pull the image
```bash
docker pull 573591465159.dkr.ecr.eu-central-1.amazonaws.com/trading-assistant:latest
```


## Run dev on Linux
```bash
docker run --env-file .env \
--env SPRING_PROFILE=dev \
--network="host" \
-p 9010:9010 -p 9011:9011 -p 8080:8080 -p 3306:3306 \
-v /home/will/code/darwinex-client/backend/accounts:/accounts \
-v /home/will/code/darwinex-client/backend/mochi-graphs:/mochi-graphs \
-v /home/will/.cxoffice/MetaTrader_5-3_584/drive_c/Program\ Files/MetaTrader\ 5/MQL5/Files/DWX:/home/will/.cxoffice/MetaTrader_5-3_584/drive_c/Program\ Files/MetaTrader\ 5/MQL5/Files/DWX \
573591465159.dkr.ecr.eu-central-1.amazonaws.com/trading-assistant:latest
```

## Run currencies on Windows

```powershell
docker run --env-file .env `
--env SPRING_PROFILE=currencies `
-p 8080:8080 `
-v C:\\Users\\user\\IdeaProjects\\darwinex-executor\\accounts:/accounts `
-v C:\\Users\\user\\IdeaProjects\\darwinex-executor\\mochi-graphs:/mochi-graphs `
-v C:\\Users\\user\\AppData\\Roaming\\MetaQuotes\\Terminal\\33BCAFEA70BFE62B7C2BC1AAFDFEEDB6\\MQL5\\Files:/mt `
573591465159.dkr.ecr.eu-central-1.amazonaws.com/trading-assistant:latest
```