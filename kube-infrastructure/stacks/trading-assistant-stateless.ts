// trading-assistant-stateful.ts

import {Construct} from "constructs";
import {TerraformStack} from "cdktf";
import {KubernetesProvider} from "@cdktf/provider-kubernetes/lib/provider";
import {
    MYSQL_LABEL,
    TRADING_ASSISTANT_DATA_STREAMER_LABEL,
    TRADING_ASSISTANT_FRONTEND_LABEL,
    TRADING_ASSISTANT_LABEL,
    TRADING_ASSISTANT_NAMESPACE
} from "../constants";
import * as kubernetes from "@cdktf/provider-kubernetes";

export class TradingAssistantStatelessStack extends TerraformStack {
    constructor(scope: Construct, name: string) {
        super(scope, name);

        new KubernetesProvider(this, 'K8s', {
            host: "https://192.168.86.89:6443",
            token: process.env.TOKEN,
            insecure: true,
            ignoreAnnotations: ["metallb\\.universe\\.tf/ip-allocated-from-pool"],

            // "configPath": "~/.kube/config",
            // "configContext": "kubernetes-admin@kubernetes"
        });
        //  this.createTradingAssistantFrontendIngress();

        //let adminPassword = this.createDBTerraformSecret();
        this.createTradingAssistantDeployment();
        this.createDataStreamerDeployment();
        this.createTradingAssistantFrontendDeployment();
        this.createTradingAssistantService();
        this.createTradingAssistantFrontendService();
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
                loadBalancerIp: "192.168.86.242"
            },
            lifecycle: {
                ignoreChanges: ['metadata[0].annotations["metallb.universe.tf/ip-allocated-from-pool"]'],
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
                                    valueFrom: {
                                        secretKeyRef: {
                                            name: "my-secrets",
                                            key: "dbPassword",
                                        }
                                    }
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
                loadBalancerIp: "192.168.86.240",
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
                loadBalancerIp: "192.168.86.241"
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

    private createDataStreamerDeployment() {
        new kubernetes.deployment.Deployment(this, TRADING_ASSISTANT_DATA_STREAMER_LABEL, {
            metadata: {
                labels: {
                    app: TRADING_ASSISTANT_DATA_STREAMER_LABEL,
                },
                name: TRADING_ASSISTANT_DATA_STREAMER_LABEL,
                namespace: TRADING_ASSISTANT_NAMESPACE,
            },
            spec: {
                replicas: '1',
                selector: {
                    matchLabels: {
                        app: TRADING_ASSISTANT_DATA_STREAMER_LABEL,
                    },
                },
                template: {
                    metadata: {
                        labels: {
                            app: TRADING_ASSISTANT_DATA_STREAMER_LABEL,
                        },
                        annotations: {
                            "prometheus.io/scrape": "true",
                            "prometheus.io/path": "/actuator/prometheus",
                            "prometheus.io/port": "8080",
                        },
                    },
                    spec: {
                        nodeSelector: {
                            type: "mt5"
                        },
                        container: [
                            {
                                image: 'ghcr.io/willhumphreys/trading-assistant:data-streamer-latest',
                                imagePullPolicy: 'Always',
                                name: TRADING_ASSISTANT_LABEL,
                                port: [{
                                    containerPort: 8080,
                                }],
                                livenessProbe: {
                                    httpGet: {
                                        path: "/actuator/health",
                                        port: "8080"
                                    },
                                    initialDelaySeconds: 30,
                                    periodSeconds: 2
                                },
                                readinessProbe: {
                                    httpGet: {
                                        path: "/actuator/health",
                                        port: "8080"
                                    },
                                    initialDelaySeconds: 10
                                },
                                env: [{
                                    name: 'SPRING_PROFILE',
                                    value: 'prod',
                                }
                                ],
                                volumeMount: [
                                    {
                                        name: 'mt-volume',
                                        mountPath: '/mt',
                                    }]
                            },
                        ],
                        volume: [
                            {
                                name: 'mt-volume',
                                hostPath: {
                                    path: '/home/will/mt-files',
                                },
                            },
                        ],
                    },
                },
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
                        annotations: {
                            "prometheus.io/scrape": "true",
                            "prometheus.io/path": "/actuator/prometheus",
                            "prometheus.io/port": "8080",
                        },
                    },
                    spec: {
                        nodeSelector: {
                            type: "mt5"
                        },
                        container: [
                            {
                                image: 'ghcr.io/willhumphreys/trading-assistant:backend-latest',
                                imagePullPolicy: 'Always',
                                name: TRADING_ASSISTANT_LABEL,
                                port: [{
                                    containerPort: 8080,
                                }],
                                livenessProbe: {
                                    httpGet: {
                                        path: "/actuator/health",
                                        port: "8080"
                                    },
                                    initialDelaySeconds: 30,
                                    periodSeconds: 2
                                },
                                readinessProbe: {
                                    httpGet: {
                                        path: "/actuator/health",
                                        port: "8080"
                                    },
                                    initialDelaySeconds: 10
                                },
                                env: [{
                                    name: 'SPRING_PROFILE',
                                    value: 'currencies',
                                }, {
                                    name: 'DATABASE_URL',
                                    value: 'jdbc:mysql://mysql-service:3306/metatrader',
                                }, {
                                    name: 'DATABASE_PASSWORD',
                                    valueFrom: {
                                        secretKeyRef: {
                                            name: "my-secrets",
                                            key: "dbPassword",
                                        }
                                    }
                                }, {
                                    name: 'SLACK_WEBHOOK_URL',
                                    valueFrom: {
                                        secretKeyRef: {
                                            name: "my-secrets",
                                            key: "slackWebHook",
                                        }
                                    }
                                }, {
                                    name: 'SUMO_LOGIC_WEBHOOK_URL',
                                    valueFrom: {
                                        secretKeyRef: {
                                            name: "my-secrets",
                                            key: "sumoLogicWebHook",
                                        }
                                    }
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
                                    path: '/home/will/mt-files',
                                },
                            },
                        ],
                    },
                },
            },
        });
    }
}
