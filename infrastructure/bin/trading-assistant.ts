#!/usr/bin/env node
import 'source-map-support/register';
import * as cdk from 'aws-cdk-lib';
import {TradingAssistantStack} from '../lib/trading-assistant-stack';
import {GitHubStack} from "../lib/github-auth-stack";

const app = new cdk.App();
new GitHubStack(app, "GitHubOpenIDConnect", {
    env: {account: '573591465159', region: 'eu-central-1'},
});
new TradingAssistantStack(app, 'TradingAssistant', {
    env: {account: '573591465159', region: 'eu-central-1'},
});