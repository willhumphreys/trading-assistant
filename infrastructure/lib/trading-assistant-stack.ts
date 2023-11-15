import * as cdk from 'aws-cdk-lib';
import {SecretValue} from 'aws-cdk-lib';
import {Construct} from 'constructs';
import * as codebuild from "@aws-cdk/aws-codebuild";


import * as amplify from "@aws-cdk/aws-amplify-alpha";

import {CfnApp, CfnBranch} from "aws-cdk-lib/aws-amplify";

export class TradingAssistantStack extends cdk.Stack {
  constructor(scope: Construct, id: string, props?: cdk.StackProps) {
    super(scope, id, props);

    // const DOMAIN_NAME = process.env.DOMAIN_NAME;
    //
    // if (!DOMAIN_NAME) {
    //   throw new Error("DOMAIN_NAME must be set in the environment");
    // }
    //
    // const WEB_APP_DOMAIN = DOMAIN_NAME + ".com";

    const GITHUB_BRANCH = process.env.GITHUB_BRANCH;

    if (!GITHUB_BRANCH) {
      throw new Error("GITHUB_BRANCH must be set in the environment");
    }

    let stage;

    if (GITHUB_BRANCH === "main") {
      stage = "PRODUCTION";
    } else if (GITHUB_BRANCH === "dev") {
      stage = "DEVELOPMENT";
    } else {
      throw new Error(`Invalid GITHUB_BRANCH: ${GITHUB_BRANCH}. Must be 'main' or 'dev'.`);
    }

    const environments: { [key: string]: { stage: string; branch: string } } = {
      // dev: {
      //   domain: 'dev.dancing-vitamins.com',
      //   stage: 'DEVELOPMENT',
      //   branch: 'dev',
      // },
      prod: {

        stage,
        branch: GITHUB_BRANCH,
      },
    };


    const currentEnv =
        environments[(process.env.VITAMINS_AMPLIFY_ENV as keyof typeof environments) || "prod"];

    const amplifyApp = new amplify.App(this, "tradingAssistantAmplify", {
      sourceCodeProvider: new amplify.GitHubSourceCodeProvider({
        owner: "willhumphreys",
        repository: "darwinex-client",
        oauthToken: SecretValue.secretsManager("github-token"),
      }),
      environmentVariables: {
        AMPLIFY_DIFF_DEPLOY: "false",
        AMPLIFY_MONOREPO_APP_ROOT: "frontend",

      },
      buildSpec: codebuild.BuildSpec.fromObjectToYaml({
        version: 1,
        applications: [
          {
            frontend: {
              phases: {
                preBuild: {
                  commands: ["nvm use 18", "npm ci"],
                },
                build: {
                  commands: ["nvm use 18", "npm run build"],
                },
              },
              artifacts: {
                baseDirectory: ".next",
                files: ["**/*"],
              },
              cache: {
                paths: ["node_modules/**/*"],
              },
            },
            appRoot: "frontend",
          },
        ],
      }),
    });

    amplifyApp.addCustomRule({
      source: "/<*>",
      target: "/index.html",
      status: amplify.RedirectStatus.NOT_FOUND_REWRITE,
    });

    const branch = amplifyApp.addBranch(currentEnv.branch, {
      stage: currentEnv.stage,
    });

    // let domain = amplifyApp.addDomain(currentEnv.domain);
    // domain.mapRoot(branch);

    (amplifyApp.node.defaultChild as CfnApp).addPropertyOverride("Platform", "WEB_COMPUTE");

    (branch.node.defaultChild as CfnBranch).addPropertyOverride("Framework", "Next.js - SSR");


  }
}
