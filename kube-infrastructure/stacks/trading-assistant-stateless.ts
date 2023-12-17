// trading-assistant-stateful.ts

import {Construct} from "constructs";
import {TerraformStack, TerraformVariable} from "cdktf";
import * as kubernetes from "@cdktf/provider-kubernetes";
import * as path from "path";
import {KubernetesProvider} from "@cdktf/provider-kubernetes/lib/provider";
import {DEFAULT_HOME_DIR, MYSQL_LABEL, TRADING_ASSISTANT_LABEL} from "../constants";

export class TradingAssistantStatelessStack extends TerraformStack {
    constructor(scope: Construct, name: string) {
        super(scope, name);

        new KubernetesProvider(this, 'K8s', {
            configPath: path.join(process.env.HOME || DEFAULT_HOME_DIR, '.kube/config'),
            host: "https://kubernetes.default.svc",
            insecure: true,
        });


        let adminPassword = this.createDBTerraformSecret();
        this.createTradingAssistantDeployment(this.createSlackSecret(), this.createSumoLogicSecret(), adminPassword);
        this.createTradingAssistantService();
        this.createMysqlDeployment(adminPassword);
        this.createMySqlService();
    }

    private createMySqlService() {
        new kubernetes.service.Service(this, "mysql-service", {
            metadata: {
                labels: {
                    app: MYSQL_LABEL,
                },
                name: 'mysql-service',
                namespace: 'trading-assistant',
            },
            spec: {
                port: [{
                    port: 3306,
                    targetPort: "3306",
                }],
                selector: {
                    app: MYSQL_LABEL,
                },
                type: 'LoadBalancer',
            },
        });
    }

    private createMysqlDeployment(adminPassword: TerraformVariable) {
        new kubernetes.deployment.Deployment(this, MYSQL_LABEL, {
            metadata: {
                labels: {
                    app: MYSQL_LABEL,
                },
                name: 'mysql-container',
                namespace: 'trading-assistant',
            },
            spec: {
                replicas: '1',
                selector: {
                    matchLabels: {
                        app: MYSQL_LABEL,
                    },
                },
                template: {
                    metadata: {
                        labels: {
                            app: MYSQL_LABEL,
                        },
                    },
                    spec: {
                        container: [
                            {
                                image: 'mysql:latest',
                                name: MYSQL_LABEL,
                                port: [{
                                    containerPort: 3306,
                                }],
                                env: [{
                                    name: 'MYSQL_ROOT_PASSWORD',
                                    value: adminPassword.value,
                                }],
                                volumeMount: [
                                    {
                                        name: 'mysql-data',
                                        mountPath: '/var/lib/mysql',
                                    }],
                            },
                        ],
                        volume: [
                            {
                                name: 'mysql-data',
                                persistentVolumeClaim: {
                                    claimName: 'mysql-pv-claim'
                                }
                                ,
                            }]

                    },
                },
            },
        });
    }

    private createTradingAssistantService() {
        new kubernetes.service.Service(this, "trading-assistant-service", {
            metadata: {
                labels: {
                    app: TRADING_ASSISTANT_LABEL,
                    test: "IAmTest"
                },
                name: 'trading-assistant-service',
                namespace: 'trading-assistant',
            },
            spec: {
                port: [
                    {
                        port: 8080,
                        targetPort: "8080",
                    }

                ],

                selector: {
                    app: TRADING_ASSISTANT_LABEL,
                },
                type: 'LoadBalancer',
            },
        });
    }

    private createTradingAssistantDeployment(slackTerraformVariable: TerraformVariable, sumoLogicTerraformVariable: TerraformVariable, dbPasswordTerraformVariable: TerraformVariable) {
        new kubernetes.deployment.Deployment(this, TRADING_ASSISTANT_LABEL, {
            metadata: {
                labels: {
                    app: TRADING_ASSISTANT_LABEL,
                },
                name: TRADING_ASSISTANT_LABEL,
                namespace: 'trading-assistant',
            },
            spec: {
                replicas: '1',
                selector: {
                    matchLabels: {
                        app: TRADING_ASSISTANT_LABEL,
                    },
                },
                template: {
                    metadata: {
                        labels: {
                            app: TRADING_ASSISTANT_LABEL,
                        },
                    },
                    spec: {
                        container: [
                            {
                                image: 'ghcr.io/willhumphreys/trading-assistant:latest',
                                name: TRADING_ASSISTANT_LABEL,
                                port: [{
                                    containerPort: 8080,
                                }],
                                env: [{
                                    name: 'SPRING_PROFILE',
                                    value: 'currencies',
                                }, {
                                    name: 'DATABASE_URL',
                                    value: 'jdbc:mysql://mysql-service:3306/metatrader',
                                }, {
                                    name: 'DATABASE_PASSWORD',
                                    value: dbPasswordTerraformVariable.value,
                                }, {
                                    name: 'SLACK_WEBHOOK_URL',
                                    value: slackTerraformVariable.value,
                                }, {
                                    name: 'SUMO_LOGIC_WEBHOOK_URL',
                                    value: sumoLogicTerraformVariable.value,
                                }
                                ],
                                volumeMount: [
                                    {
                                        name: 'accounts-volume',
                                        mountPath: '/accounts',
                                    },
                                    {
                                        name: 'mochi-graphs-volume',
                                        mountPath: '/mochi-graphs',
                                    },
                                    {
                                        name: 'mt-volume',
                                        mountPath: '/mt',
                                    }]
                            },
                        ],
                        volume: [
                            {
                                name: 'accounts-volume',
                                hostPath: {
                                    path: '/run/desktop/mnt/host/c/Users/user/IdeaProjects/darwinex-executor/accounts',
                                },
                            },
                            {
                                name: 'mochi-graphs-volume',
                                hostPath: {
                                    path: '/run/desktop/mnt/host/c/Users/user/IdeaProjects/darwinex-executor/mochi-graphs',
                                },
                            },
                            {
                                name: 'mt-volume',
                                hostPath: {
                                    path: '/run/desktop/mnt/host/c/Users/user/AppData/Roaming/MetaQuotes/Terminal/33BCAFEA70BFE62B7C2BC1AAFDFEEDB6/MQL5/Files',
                                },
                            },
                        ],
                    },
                },
            },
        });
    }

    private createDBTerraformSecret() {

        return new TerraformVariable(this, "dbPassword", {
            type: "string",
            description: "root password for mysql",
            sensitive: true,
        });
    }

    private createSlackSecret() {

        return new TerraformVariable(this, "slackWebHook", {
            type: "string",
            description: "slack webhook url",
            sensitive: true,
        });
    }

    private createSumoLogicSecret() {

        return new TerraformVariable(this, "sumoLogicWebHook", {
            type: "string",
            description: "sumo logic webhook url",
            sensitive: true,
        });
    }
}
