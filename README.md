# Trading Assistant

Automates trading on Metatrader via the Darwinex API.

- [Backend Documentation](./backend/README.md)
- [Infrastructure Documentation](./infrastructure/README.md)
- [Frontend Documentation](./frontend/README.md)


# AWS

## Set the profile
```bash
export AWS_PROFILE=AdministratorAccess-573591465159
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


docker pull 573591465159.dkr.ecr.eu-central-1.amazonaws.com/trading-assistant:latest