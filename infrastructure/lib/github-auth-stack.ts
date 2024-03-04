import * as cdk from "aws-cdk-lib";
import * as iam from "aws-cdk-lib/aws-iam";

export interface GitHubStackProps extends cdk.StackProps {
  /**
   * Name of the deploy role to assume in GitHub Actions.
   *
   * @default - 'exampleGitHubDeployRole'
   */
  readonly deployRole: string;
  /**
   * The sub prefix string from the JWT token used to be validated by AWS. Appended after `repo:${owner}/${repo}:`
   * in an IAM role trust relationship. The default value '*' indicates all branches and all tags from this repo.
   *
   * Example:
   * repo:octo-org/octo-repo:ref:refs/heads/demo-branch - only allowed from `demo-branch`
   * repo:octo-org/octo-repo:ref:refs/tags/demo-tag - only allowed from `demo-tag`.
   * repo:octo-org/octo-repo:pull_request - only allowed from the `pull_request` event.
   * repo:octo-org/octo-repo:environment:Production - only allowd from `Production` environment name.
   *
   * @default '*'
   * @see https://docs.github.com/en/actions/deployment/security-hardening-your-deployments/about-security-hardening-with-openid-connect#configuring-the-oidc-trust-with-the-cloud
   */
  readonly repositoryConfig: { owner: string; repo: string; filter?: string }[];
}

export class GitHubStack extends cdk.Stack {
  constructor(scope: cdk.App, id: string, props?: cdk.StackProps) {
    super(scope, id, props);

    const deployRole = "gitHubDeployRole";
    const repositoryConfig = [
      {owner: "willhumphreys", repo: "trading-assistant", filter: `ref:refs/heads/${process.env.GITHUB_BRANCH}`},
      {owner: "willhumphreys", repo: "trading-assistant", filter: `pull_request`},
    ];

    const githubDomain = "token.actions.githubusercontent.com";

    const ghProvider = new iam.OpenIdConnectProvider(this, "githubProvider", {
      url: `https://${githubDomain}`,
      clientIds: ["sts.amazonaws.com"],
    });

    const iamRepoDeployAccess = repositoryConfig.map(
        (r) => `repo:${r.owner}/${r.repo}:${r.filter ?? "*"}`
    );

    // grant only requests coming from a specific GitHub repository.
    const conditions: iam.Conditions = {
      StringLike: {
        [`${githubDomain}:sub`]: iamRepoDeployAccess,
      },
    };

    new iam.Role(this, "cloudNationGitHubDeployRole", {
      assumedBy: new iam.WebIdentityPrincipal(ghProvider.openIdConnectProviderArn, conditions),
      managedPolicies: [iam.ManagedPolicy.fromAwsManagedPolicyName("AdministratorAccess")],
      roleName: deployRole,
      description:
          "This role is used via GitHub Actions to deploy with AWS CDK or Terraform on the target AWS account",
      maxSessionDuration: cdk.Duration.hours(1),
    });
  }
}
