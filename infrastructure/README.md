# Infrastructure for the TradingAssistant project

At the moment only the frontend is deployed to AWS. The backend is still running on a local machine.

This is a blank project for CDK development with TypeScript.

The `cdk.json` file tells the CDK Toolkit how to execute your app.

## Useful commands

* `npm run build`   compile typescript to js
* `npm run watch`   watch for changes and compile
* `npm run test`    perform the jest unit tests
* `cdk deploy`      deploy this stack to your default AWS account/region
* `cdk diff`        compare deployed stack with current state
* `cdk synth`       emits the synthesized CloudFormation template

To update the global packages

```bash
npx npm-check --global --update-all
```

To set the environmental variables
```bash
source ./setprodenv.sh
```

cdk synth EcrStack
cdk synth TradingAssistantStack