import {App} from "cdktf";
import {TradingAssistantStatefulStack} from "./stacks/trading-assistant-stateful";
import {TradingAssistantStatelessStack} from "./stacks/trading-assistant-stateless";

const app = new App();
new TradingAssistantStatefulStack(app, 'trading-assistant-stateful');
new TradingAssistantStatelessStack(app, 'trading-assistant-stateless');
app.synth();