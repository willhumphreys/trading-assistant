import * as cdk from 'aws-cdk-lib';
import {Construct} from 'constructs';


export class EcrStack extends cdk.Stack {
    constructor(scope: Construct, id: string, props?: cdk.StackProps) {
        super(scope, id, props);

        new cdk.aws_ecr.Repository(this, 'TradingAssistantEcrRepository', {
            repositoryName: 'trading-assistant',
            imageScanOnPush: true,
            lifecycleRules: [{
                rulePriority: 1,
                description: 'Keep only last 5 images',
                maxImageCount: 5
            }]
        });
    }
}
