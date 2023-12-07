import * as cdk from 'aws-cdk-lib';
import {Template} from 'aws-cdk-lib/assertions';
import {EcrStack} from '../lib/ecr-stack';

test('EcrStack', () => {
    process.env.DOMAIN_NAME = 'dancing-vitamins';
    process.env.GITHUB_BRANCH = 'main';

    const app = new cdk.App();
    const stack = new EcrStack(app, 'EcrStack');

    const template = Template.fromStack(stack);

    template.hasResourceProperties('AWS::ECR::Repository', {
        RepositoryName: "trading-assistant",
        ImageScanningConfiguration: {
            ScanOnPush: true
        },
        LifecyclePolicy: {
            LifecyclePolicyText: "{\"rules\":[{\"rulePriority\":1,\"description\":\"Keep only last 5 images\",\"selection\":{\"tagStatus\":\"any\",\"countType\":\"imageCountMoreThan\",\"countNumber\":5},\"action\":{\"type\":\"expire\"}}]}"
        }
    });

    delete process.env.DOMAIN_NAME;
    delete process.env.GITHUB_BRANCH;
});
