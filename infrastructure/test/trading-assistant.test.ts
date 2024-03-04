import * as cdk from 'aws-cdk-lib';
import {Template} from 'aws-cdk-lib/assertions';
import {TradingAssistantStack} from '../lib/trading-assistant-stack';
import {RedirectStatus} from "@aws-cdk/aws-amplify-alpha";

test('TradingAssistantStack', () => {
    process.env.DOMAIN_NAME = 'dancing-vitamins';
    process.env.GITHUB_BRANCH = 'main';

    const app = new cdk.App();
    const stack = new TradingAssistantStack(app, 'TradingAssistantStack');

    const template = Template.fromStack(stack);

    template.hasResourceProperties('AWS::IAM::Role', {
        AssumeRolePolicyDocument: {
            Statement: [
                {
                    Action: "sts:AssumeRole",
                    Effect: "Allow",
                    Principal: {
                        Service: "amplify.amazonaws.com"
                    }
                }
            ],
            Version: "2012-10-17"
        }
    });

    template.hasResourceProperties('AWS::Amplify::App', {
        Name: "tradingAssistantAmplify",
        Repository: "https://github.com/willhumphreys/trading-assistant",
        EnvironmentVariables: [
            {
                Name: "AMPLIFY_DIFF_DEPLOY",
                Value: "false"
            },
            {
                Name: "AMPLIFY_MONOREPO_APP_ROOT",
                Value: "frontend"
            }
        ],
        IAMServiceRole: {
            "Fn::GetAtt": [
                "tradingAssistantAmplifyRole39C0CC37",
                "Arn"
            ]
        },
        CustomRules: [
            {
                Source: "/<*>",
                Target: "/index.html",
                Status: RedirectStatus.NOT_FOUND_REWRITE
            }
        ],

    });


    template.hasResourceProperties('AWS::Amplify::Branch', {
        AppId: {
            "Fn::GetAtt": [
                "tradingAssistantAmplify079D9D31",
                "AppId"
            ]
        },
        BranchName: "main",
        EnableAutoBuild: true,
        EnablePullRequestPreview: true,
        Framework: "Next.js - SSR",
        Stage: "PRODUCTION",
    });

    delete process.env.DOMAIN_NAME;
    delete process.env.GITHUB_BRANCH;
});
