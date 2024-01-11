// trading-assistant-stateful.ts

import {Construct} from "constructs";
import {TerraformStack, TerraformVariable} from "cdktf";
import {KubernetesProvider} from "@cdktf/provider-kubernetes/lib/provider";
import {
    MYSQL_LABEL,
    TRADING_ASSISTANT_FRONTEND_LABEL,
    TRADING_ASSISTANT_LABEL,
    TRADING_ASSISTANT_NAMESPACE
} from "../constants";
import * as kubernetes from "@cdktf/provider-kubernetes";

export class TradingAssistantStatelessStack extends TerraformStack {
    constructor(scope: Construct, name: string) {
        super(scope, name);

        new KubernetesProvider(this, 'K8s', {
            host: "https://192.168.1.202:6443",
            token: process.env.TOKEN,
            insecure: true,
            ignoreAnnotations: ["metallb\\.universe\\.tf/ip-allocated-from-pool"],

            // "configPath": "~/.kube/config",
            // "configContext": "kubernetes-admin@kubernetes"
        });
        //  this.createTradingAssistantFrontendIngress();

        let adminPassword = this.createDBTerraformSecret();
        this.createTradingAssistantDeployment(this.createSlackSecret(), this.createSumoLogicSecret(), adminPassword);
        this.createTradingAssistantFrontendDeployment();
        this.createTradingAssistantService();
        this.createTradingAssistantFrontendService();
        this.createMysqlDeployment(adminPassword);
        this.createMySqlService();
    }

    // private createTradingAssistantFrontendIngress() {
    //     new kubernetes.manifest.Manifest(this, "trading-assistant-frontend-ingress", {
    //         manifest: {
    //             apiVersion: "networking.k8s.io/v1",
    //             kind: "Ingress",
    //             metadata: {
    //                 name: "trading-assistant-ingress",
    //                 namespace: "trading-assistant",
    //                 labels: {
    //                     app: TRADING_ASSISTANT_LABEL + "-frontend"
    //                 },
    //             },
    //             spec: {
    //                 ingressClassName: "nginx",
    //                 rules: [
    //                     {
    //                         host: "trading-assistant.mochi-trading.com",
    //                         http: {
    //                             paths: [
    //                                 {
    //                                     path: "/",
    //                                     pathType: "Prefix",
    //                                     backend: {
    //                                         service: {
    //                                             name: "trading-assistant-frontend-service",
    //                                             port: {
    //                                                 number: 3000,
    //                                             },
    //                                             selector: {
    //                                                 app: TRADING_ASSISTANT_LABEL + "-frontend",
    //                                             },
    //                                         },
    //                                     },
    //                                 },
    //                             ],
    //                         },
    //                     },
    //                 ],
    //             },
    //         },
    //     });
    // }

    private createMySqlService() {
        new kubernetes.service.Service(this, "mysql-service", {
            metadata: {
                labels: {
                    app: MYSQL_LABEL,
                },
                name: 'mysql-service',
                namespace: TRADING_ASSISTANT_NAMESPACE,
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
            lifecycle: {
                ignoreChanges: ['metadata[0].annotations["metallb.universe.tf/ip-allocated-from-pool"]'],
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
                namespace: TRADING_ASSISTANT_NAMESPACE,
            },
            spec: {
                replicas: '1',
                selector: {
                    matchLabels: {
                        app: MYSQL_LABEL,
                    },

                },
                strategy: {
                    type: "Recreate"
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
                },
                name: 'trading-assistant-service',
                namespace: TRADING_ASSISTANT_NAMESPACE,
                annotations: {
                    "service.beta.kubernetes.io/aws-load-balancer-proxy-protocol": "*"
    }
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
            lifecycle: {
                ignoreChanges: ['metadata[0].annotations["metallb.universe.tf/ip-allocated-from-pool"]'],
            },
        });
    }

    private createTradingAssistantFrontendService() {
        new kubernetes.service.Service(this, "trading-assistant-frontend-service", {
            metadata: {
                labels: {
                    app: TRADING_ASSISTANT_FRONTEND_LABEL,
                },
                annotations: {
                    "service.beta.kubernetes.io/aws-load-balancer-proxy-protocol": "*",
    },
                name: 'trading-assistant-frontend-service',
                namespace: TRADING_ASSISTANT_NAMESPACE,
            },
            spec: {
                port: [
                    {
                        name: "http",
                        port: 80,
                        targetPort: "3000",
                    }

                ],

                selector: {
                    app: TRADING_ASSISTANT_FRONTEND_LABEL,
                },
                type: 'LoadBalancer',
            },
            lifecycle: {
                ignoreChanges: ['metadata[0].annotations["metallb.universe.tf/ip-allocated-from-pool"]'],
            },
        });
    }

    private createTradingAssistantFrontendDeployment() {
        new kubernetes.deployment.Deployment(this, TRADING_ASSISTANT_FRONTEND_LABEL, {
            metadata: {
                labels: {
                    app: TRADING_ASSISTANT_FRONTEND_LABEL,
                },
                name: TRADING_ASSISTANT_FRONTEND_LABEL,
                namespace: TRADING_ASSISTANT_NAMESPACE,
            },
            spec: {
                replicas: '1',
                selector: {
                    matchLabels: {
                        app: TRADING_ASSISTANT_FRONTEND_LABEL,
                    },
                },
                template: {
                    metadata: {
                        labels: {
                            app: TRADING_ASSISTANT_FRONTEND_LABEL,
                        },
                    },
                    spec: {
                        container: [
                            {
                                image: 'ghcr.io/willhumphreys/trading-assistant:frontend-latest',
                                imagePullPolicy: 'Always',
                                name: TRADING_ASSISTANT_LABEL,
                                port: [{
                                    containerPort: 3000,
                                }],
                            },
                        ]
                    },
                },
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
                namespace: TRADING_ASSISTANT_NAMESPACE,
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
                                image: 'ghcr.io/willhumphreys/trading-assistant:backend-latest',
                                imagePullPolicy: 'Always',
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
                                    path: '/home/will/accounts',
                                },
                            },
                            {
                                name: 'mochi-graphs-volume',
                                hostPath: {
                                    path: '/home/will/mochi-graphs',
                                },
                            },
                            {
                                name: 'mt-volume',
                                hostPath: {
                                    path: '/home/will/.cxoffice/MetaTrader_5/drive_c/Program Files/MetaTrader 5/MQL5/Files',
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

    // private createHomeVariable() {
    //
    //     return new TerraformVariable(this, "kubeHome", {
    //         type: "string",
    //         description: "kube home directory",
    //         sensitive: false,
    //     });
    // }
}
