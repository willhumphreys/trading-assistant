import {Construct} from "constructs";
import {TerraformStack} from "cdktf";
import * as kubernetes from "@cdktf/provider-kubernetes";
import * as path from "path";
import {KubernetesProvider} from "@cdktf/provider-kubernetes/lib/provider";
import * as dotenv from 'dotenv';

export class TradingAssistantStatelessStack extends TerraformStack {
    constructor(scope: Construct, name: string) {
        super(scope, name);

        new KubernetesProvider(this, 'K8s', {
            configPath: path.join(process.env.HOME || '/home/will', '.kube/config'),
        });


        // Load environment variables from .env file
        const env = dotenv.config().parsed;

        if (!env) {
            throw new Error('Failed to load .env file');
        }

        new kubernetes.configMap.ConfigMap(this, "trading-assistant-config", {
            metadata: {
                name: "trading-assistant-env",
                labels: {
                    app: "trading-assistant"
                }
            },
            data: env
        })


        // Define the mysql-root-password Secret
        new kubernetes.secret.Secret(this, "mysql-root-password", {
            metadata: {
                name: 'mysql-root-password',
            },
            data: {password: env.DATABASE_PASSWORD},
        });


        console.log("ConfigMap created")


        // Define the trading-assistant deployment
        new kubernetes.deployment.Deployment(this, "trading-assistant", {
            metadata: {
                labels: {
                    app: 'trading-assistant',
                },
                name: 'trading-assistant',
            },
            spec: {
                replicas: '1',
                selector: {
                    matchLabels: {
                        app: 'trading-assistant',
                    },
                },
                template: {
                    metadata: {
                        labels: {
                            app: 'trading-assistant',
                        },
                    },
                    spec: {
                        container: [
                            {
                                image: 'ghcr.io/willhumphreys/trading-assistant:latest',
                                name: 'trading-assistant',
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

        // Define the trading-assistant service
        new kubernetes.service.Service(this, "trading-assistant-service", {
            metadata: {
                labels: {
                    app: 'trading-assistant',
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
                    app: 'trading-assistant',
                },
                type: 'LoadBalancer',
            },
        });

        // Define the mysql deployment
        new kubernetes.deployment.Deployment(this, "mysql", {
            metadata: {
                labels: {
                    app: 'mysql',
                },
                name: 'mysql-container',
            },
            spec: {
                replicas: '1',
                selector: {
                    matchLabels: {
                        app: 'mysql',
                    },
                },
                template: {
                    metadata: {
                        labels: {
                            app: 'mysql',
                        },
                    },
                    spec: {
                        container: [
                            {
                                image: 'mysql:latest',
                                name: 'mysql',
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

        // Define the mysql service
        new kubernetes.service.Service(this, "mysql-service", {
            metadata: {
                labels: {
                    app: 'mysql',
                },
                name: 'mysql-service',
            },
            spec: {
                port: [{
                    port: 3306,
                    targetPort: "3306",
                }],
                selector: {
                    app: 'mysql',
                },
                type: 'LoadBalancer',
            },
        });
    }
}
