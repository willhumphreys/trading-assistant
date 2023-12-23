import { Construct } from "constructs";
import { App, TerraformStack, CloudBackend, NamedCloudWorkspace } from "cdktf";
import {TradingAssistantStatefulStack} from "./stacks/trading-assistant-stateful";
import {TradingAssistantStatelessStack} from "./stacks/trading-assistant-stateless";

class MyStack extends TerraformStack {
  constructor(scope: Construct, id: string) {
    super(scope, id);

    // define resources here
  }
}

const app = new App();
const stack = new MyStack(app, "kube-infrastructure2");
new TradingAssistantStatefulStack(app, 'trading-assistant-stateful');
new TradingAssistantStatelessStack(app, 'trading-assistant-stateless');
new CloudBackend(stack, {
  hostname: "app.terraform.io",
  organization: "willhumphreys",
  workspaces: new NamedCloudWorkspace("trading-assistant")
});
app.synth();
