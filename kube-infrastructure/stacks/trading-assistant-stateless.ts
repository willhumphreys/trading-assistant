// trading-assistant-stateful.ts

import {Construct} from "constructs";
import {TerraformStack} from "cdktf";
import * as kubernetes from "@cdktf/provider-kubernetes";
import * as path from "path";
import {KubernetesProvider} from "@cdktf/provider-kubernetes/lib/provider";
import * as dotenv from 'dotenv';
import {DotenvParseOutput} from 'dotenv';
import {DEFAULT_HOME_DIR, MYSQL_LABEL, TRADING_ASSISTANT_LABEL} from "../constants";

export class TradingAssistantStatelessStack extends TerraformStack {
    constructor(scope: Construct, name: string) {
        super(scope, name);

        new KubernetesProvider(this, 'K8s', {
            configPath: path.join(process.env.HOME || DEFAULT_HOME_DIR, '.kube/config'),
        });

        const env = this.loadEnv();

        this.createConfigMap(env);
        this.createDBPasswordSecret(env);
        this.createTradingAssistantDeployment();
        this.createTradingAssistantService();
        this.createMysqlDeployment();
        this.createMySqlService();
    }

    private createMySqlService() {
        new kubernetes.service.Service(this, "mysql-service", {
            metadata: {
                labels: {
                    app: MYSQL_LABEL,
                },
                name: 'mysql-service',
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

    private createMysqlDeployment() {
        new kubernetes.deployment.Deployment(this, MYSQL_LABEL, {
            metadata: {
                labels: {
                    app: MYSQL_LABEL,
                },
                name: 'mysql-container',
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
                                    valueFrom: {
                                        secretKeyRef: {
                                            name: 'mysql-root-password',
                                            key: 'password',
                                        },
                                    },
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
                },
                name: 'trading-assistant-service',
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

    private createTradingAssistantDeployment() {
        new kubernetes.deployment.Deployment(this, TRADING_ASSISTANT_LABEL, {
            metadata: {
                labels: {
                    app: TRADING_ASSISTANT_LABEL,
                },
                name: TRADING_ASSISTANT_LABEL,
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
                                envFrom: [{
                                    configMapRef: {
                                        name: 'trading-assistant-env',
                                    },
                                }],
                                env: [{
                                    name: 'SPRING_PROFILE',
                                    value: 'currencies',
                                }],
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

    private createDBPasswordSecret(env: DotenvParseOutput) {
        new kubernetes.secret.Secret(this, "mysql-root-password", {
            metadata: {
                name: 'mysql-root-password',
            },
            data: {password: env.DATABASE_PASSWORD},
        });
    }

    private createConfigMap(env: DotenvParseOutput) {
        new kubernetes.configMap.ConfigMap(this, "trading-assistant-config", {
            metadata: {
                name: "trading-assistant-env",
                labels: {
                    app: TRADING_ASSISTANT_LABEL
                }
            },
            data: env
        })
    }

    private loadEnv() {
        const env = dotenv.config().parsed;

        if (!env) {

            return {
                DATABASE_URL: 'jdbc:mysql://mysql-service:3306/metatrader',
                DATABASE_PASSWORD: 'XXXXX',
                SLACK_WEBHOOK_URL: 'XXXX',
                SUMO_LOGIC_WEBHOOK_URL: 'XXXX'
            }
        }
        return env;
    }
}
