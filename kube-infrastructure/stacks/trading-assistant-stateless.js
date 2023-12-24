"use strict";
// trading-assistant-stateful.ts
Object.defineProperty(exports, "__esModule", { value: true });
exports.TradingAssistantStatelessStack = void 0;
const cdktf_1 = require("cdktf");
const provider_1 = require("@cdktf/provider-kubernetes/lib/provider");
const constants_1 = require("../constants");
const kubernetes = require("@cdktf/provider-kubernetes");
class TradingAssistantStatelessStack extends cdktf_1.TerraformStack {
    constructor(scope, name) {
        super(scope, name);
        new provider_1.KubernetesProvider(this, 'K8s', {
            "configPath": "~/.kube/config",
            "configContext": "kubernetes-admin@kubernetes"
        });
        this.createTradingAssistantAndMysqlIngress2();
        let adminPassword = this.createDBTerraformSecret();
        this.createTradingAssistantDeployment(this.createSlackSecret(), this.createSumoLogicSecret(), adminPassword);
        this.createTradingAssistantFrontendDeployment();
        this.createTradingAssistantService();
        this.createTradingAssistantFrontendService();
        this.createMysqlDeployment(adminPassword);
        this.createMySqlService();
    }
    createTradingAssistantAndMysqlIngress2() {
        new kubernetes.manifest.Manifest(this, "trading-assistant-ingress", {
            manifest: {
                apiVersion: "networking.k8s.io/v1",
                kind: "Ingress",
                metadata: {
                    name: "trading-assistant-ingress",
                    namespace: "trading-assistant",
                },
                spec: {
                    rules: [
                        {
                            host: "trading-assistant.mochi-trading.com",
                            http: {
                                paths: [
                                    {
                                        path: "/",
                                        pathType: "Prefix",
                                        backend: {
                                            service: {
                                                name: "trading-assistant-service",
                                                port: {
                                                    number: 8080,
                                                },
                                            },
                                        },
                                    },
                                ],
                            },
                        },
                    ],
                },
            },
        });
    }
    createMySqlService() {
        new kubernetes.service.Service(this, "mysql-service", {
            metadata: {
                labels: {
                    app: constants_1.MYSQL_LABEL,
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
                    app: constants_1.MYSQL_LABEL,
                },
                type: 'NodePort',
            },
        });
    }
    createMysqlDeployment(adminPassword) {
        new kubernetes.deployment.Deployment(this, constants_1.MYSQL_LABEL, {
            metadata: {
                labels: {
                    app: constants_1.MYSQL_LABEL,
                },
                name: 'mysql-container',
                namespace: 'trading-assistant',
            },
            spec: {
                replicas: '1',
                selector: {
                    matchLabels: {
                        app: constants_1.MYSQL_LABEL,
                    },
                },
                template: {
                    metadata: {
                        labels: {
                            app: constants_1.MYSQL_LABEL,
                        },
                    },
                    spec: {
                        container: [
                            {
                                image: 'mysql:latest',
                                name: constants_1.MYSQL_LABEL,
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
                                    }
                                ],
                            },
                        ],
                        volume: [
                            {
                                name: 'mysql-data',
                                persistentVolumeClaim: {
                                    claimName: 'mysql-pv-claim'
                                },
                            }
                        ]
                    },
                },
            },
        });
    }
    createTradingAssistantService() {
        new kubernetes.service.Service(this, "trading-assistant-service", {
            metadata: {
                labels: {
                    app: constants_1.TRADING_ASSISTANT_LABEL,
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
                    app: constants_1.TRADING_ASSISTANT_LABEL,
                },
                type: 'NodePort',
            },
        });
    }
    createTradingAssistantFrontendService() {
        new kubernetes.service.Service(this, "trading-assistant-frontend-service", {
            metadata: {
                labels: {
                    app: constants_1.TRADING_ASSISTANT_LABEL,
                },
                name: 'trading-assistant-frontend-service',
                namespace: 'trading-assistant',
            },
            spec: {
                port: [
                    {
                        port: 3000,
                        targetPort: "3000",
                    }
                ],
                selector: {
                    app: constants_1.TRADING_ASSISTANT_LABEL,
                },
                type: 'NodePort',
            },
        });
    }
    createTradingAssistantFrontendDeployment() {
        new kubernetes.deployment.Deployment(this, constants_1.TRADING_ASSISTANT_LABEL + '-frontend', {
            metadata: {
                labels: {
                    app: constants_1.TRADING_ASSISTANT_LABEL,
                },
                name: constants_1.TRADING_ASSISTANT_LABEL + '-frontend',
                namespace: 'trading-assistant',
            },
            spec: {
                replicas: '1',
                selector: {
                    matchLabels: {
                        app: constants_1.TRADING_ASSISTANT_LABEL,
                    },
                },
                template: {
                    metadata: {
                        labels: {
                            app: constants_1.TRADING_ASSISTANT_LABEL,
                        },
                    },
                    spec: {
                        container: [
                            {
                                image: 'ghcr.io/willhumphreys/trading-assistant:frontend-latest',
                                name: constants_1.TRADING_ASSISTANT_LABEL,
                                port: [{
                                        containerPort: 3000,
                                    }],
                                env: [{
                                        name: 'NEXT_PUBLIC_BACKEND_HOST',
                                        value: 'http://trading-assistant-service:8080',
                                    }, {
                                        name: 'NEXT_PUBLIC_WEB_SOCKET_HOST',
                                        value: 'ws://trading-assistant-service:8080',
                                    }
                                ]
                            },
                        ]
                    },
                },
            },
        });
    }
    createTradingAssistantDeployment(slackTerraformVariable, sumoLogicTerraformVariable, dbPasswordTerraformVariable) {
        new kubernetes.deployment.Deployment(this, constants_1.TRADING_ASSISTANT_LABEL, {
            metadata: {
                labels: {
                    app: constants_1.TRADING_ASSISTANT_LABEL,
                },
                name: constants_1.TRADING_ASSISTANT_LABEL,
                namespace: 'trading-assistant',
            },
            spec: {
                replicas: '1',
                selector: {
                    matchLabels: {
                        app: constants_1.TRADING_ASSISTANT_LABEL,
                    },
                },
                template: {
                    metadata: {
                        labels: {
                            app: constants_1.TRADING_ASSISTANT_LABEL,
                        },
                    },
                    spec: {
                        container: [
                            {
                                image: 'ghcr.io/willhumphreys/trading-assistant:backend-latest',
                                name: constants_1.TRADING_ASSISTANT_LABEL,
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
                                    }
                                ]
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
    createDBTerraformSecret() {
        return new cdktf_1.TerraformVariable(this, "dbPassword", {
            type: "string",
            description: "root password for mysql",
            sensitive: true,
        });
    }
    createSlackSecret() {
        return new cdktf_1.TerraformVariable(this, "slackWebHook", {
            type: "string",
            description: "slack webhook url",
            sensitive: true,
        });
    }
    createSumoLogicSecret() {
        return new cdktf_1.TerraformVariable(this, "sumoLogicWebHook", {
            type: "string",
            description: "sumo logic webhook url",
            sensitive: true,
        });
    }
}
exports.TradingAssistantStatelessStack = TradingAssistantStatelessStack;
//# sourceMappingURL=data:application/json;base64,eyJ2ZXJzaW9uIjozLCJmaWxlIjoidHJhZGluZy1hc3Npc3RhbnQtc3RhdGVsZXNzLmpzIiwic291cmNlUm9vdCI6IiIsInNvdXJjZXMiOlsidHJhZGluZy1hc3Npc3RhbnQtc3RhdGVsZXNzLnRzIl0sIm5hbWVzIjpbXSwibWFwcGluZ3MiOiI7QUFBQSxnQ0FBZ0M7OztBQUdoQyxpQ0FBd0Q7QUFDeEQsc0VBQTJFO0FBQzNFLDRDQUFrRTtBQUNsRSx5REFBeUQ7QUFFekQsTUFBYSw4QkFBK0IsU0FBUSxzQkFBYztJQUM5RCxZQUFZLEtBQWdCLEVBQUUsSUFBWTtRQUN0QyxLQUFLLENBQUMsS0FBSyxFQUFFLElBQUksQ0FBQyxDQUFDO1FBR25CLElBQUksNkJBQWtCLENBQUMsSUFBSSxFQUFFLEtBQUssRUFBRTtZQUNoQyxZQUFZLEVBQUUsZ0JBQWdCO1lBQzlCLGVBQWUsRUFBRSw2QkFBNkI7U0FDakQsQ0FBQyxDQUFDO1FBQ0gsSUFBSSxDQUFDLHNDQUFzQyxFQUFFLENBQUM7UUFFOUMsSUFBSSxhQUFhLEdBQUcsSUFBSSxDQUFDLHVCQUF1QixFQUFFLENBQUM7UUFDbkQsSUFBSSxDQUFDLGdDQUFnQyxDQUFDLElBQUksQ0FBQyxpQkFBaUIsRUFBRSxFQUFFLElBQUksQ0FBQyxxQkFBcUIsRUFBRSxFQUFFLGFBQWEsQ0FBQyxDQUFDO1FBQzdHLElBQUksQ0FBQyx3Q0FBd0MsRUFBRSxDQUFDO1FBQ2hELElBQUksQ0FBQyw2QkFBNkIsRUFBRSxDQUFDO1FBQ3JDLElBQUksQ0FBQyxxQ0FBcUMsRUFBRSxDQUFDO1FBQzdDLElBQUksQ0FBQyxxQkFBcUIsQ0FBQyxhQUFhLENBQUMsQ0FBQztRQUMxQyxJQUFJLENBQUMsa0JBQWtCLEVBQUUsQ0FBQztJQUM5QixDQUFDO0lBRU8sc0NBQXNDO1FBQzFDLElBQUksVUFBVSxDQUFDLFFBQVEsQ0FBQyxRQUFRLENBQUMsSUFBSSxFQUFFLDJCQUEyQixFQUFFO1lBQ2hFLFFBQVEsRUFBRTtnQkFDTixVQUFVLEVBQUUsc0JBQXNCO2dCQUNsQyxJQUFJLEVBQUUsU0FBUztnQkFDZixRQUFRLEVBQUU7b0JBQ04sSUFBSSxFQUFFLDJCQUEyQjtvQkFDakMsU0FBUyxFQUFFLG1CQUFtQjtpQkFDakM7Z0JBQ0QsSUFBSSxFQUFFO29CQUNGLEtBQUssRUFBRTt3QkFDSDs0QkFDSSxJQUFJLEVBQUUscUNBQXFDOzRCQUMzQyxJQUFJLEVBQUU7Z0NBQ0YsS0FBSyxFQUFFO29DQUNIO3dDQUNJLElBQUksRUFBRSxHQUFHO3dDQUNULFFBQVEsRUFBRSxRQUFRO3dDQUNsQixPQUFPLEVBQUU7NENBQ0wsT0FBTyxFQUFFO2dEQUNMLElBQUksRUFBRSwyQkFBMkI7Z0RBQ2pDLElBQUksRUFBRTtvREFDRixNQUFNLEVBQUUsSUFBSTtpREFDZjs2Q0FDSjt5Q0FDSjtxQ0FDSjtpQ0FDSjs2QkFDSjt5QkFDSjtxQkFDSjtpQkFDSjthQUNKO1NBQ0osQ0FBQyxDQUFDO0lBQ1AsQ0FBQztJQUVPLGtCQUFrQjtRQUN0QixJQUFJLFVBQVUsQ0FBQyxPQUFPLENBQUMsT0FBTyxDQUFDLElBQUksRUFBRSxlQUFlLEVBQUU7WUFDbEQsUUFBUSxFQUFFO2dCQUNOLE1BQU0sRUFBRTtvQkFDSixHQUFHLEVBQUUsdUJBQVc7aUJBQ25CO2dCQUNELElBQUksRUFBRSxlQUFlO2dCQUNyQixTQUFTLEVBQUUsbUJBQW1CO2FBQ2pDO1lBQ0QsSUFBSSxFQUFFO2dCQUNGLElBQUksRUFBRSxDQUFDO3dCQUNILElBQUksRUFBRSxJQUFJO3dCQUNWLFVBQVUsRUFBRSxNQUFNO3FCQUNyQixDQUFDO2dCQUNGLFFBQVEsRUFBRTtvQkFDTixHQUFHLEVBQUUsdUJBQVc7aUJBQ25CO2dCQUNELElBQUksRUFBRSxVQUFVO2FBQ25CO1NBQ0osQ0FBQyxDQUFDO0lBQ1AsQ0FBQztJQUVPLHFCQUFxQixDQUFDLGFBQWdDO1FBQzFELElBQUksVUFBVSxDQUFDLFVBQVUsQ0FBQyxVQUFVLENBQUMsSUFBSSxFQUFFLHVCQUFXLEVBQUU7WUFDcEQsUUFBUSxFQUFFO2dCQUNOLE1BQU0sRUFBRTtvQkFDSixHQUFHLEVBQUUsdUJBQVc7aUJBQ25CO2dCQUNELElBQUksRUFBRSxpQkFBaUI7Z0JBQ3ZCLFNBQVMsRUFBRSxtQkFBbUI7YUFDakM7WUFDRCxJQUFJLEVBQUU7Z0JBQ0YsUUFBUSxFQUFFLEdBQUc7Z0JBQ2IsUUFBUSxFQUFFO29CQUNOLFdBQVcsRUFBRTt3QkFDVCxHQUFHLEVBQUUsdUJBQVc7cUJBQ25CO2lCQUNKO2dCQUNELFFBQVEsRUFBRTtvQkFDTixRQUFRLEVBQUU7d0JBQ04sTUFBTSxFQUFFOzRCQUNKLEdBQUcsRUFBRSx1QkFBVzt5QkFDbkI7cUJBQ0o7b0JBQ0QsSUFBSSxFQUFFO3dCQUNGLFNBQVMsRUFBRTs0QkFDUDtnQ0FDSSxLQUFLLEVBQUUsY0FBYztnQ0FDckIsSUFBSSxFQUFFLHVCQUFXO2dDQUNqQixJQUFJLEVBQUUsQ0FBQzt3Q0FDSCxhQUFhLEVBQUUsSUFBSTtxQ0FDdEIsQ0FBQztnQ0FDRixHQUFHLEVBQUUsQ0FBQzt3Q0FDRixJQUFJLEVBQUUscUJBQXFCO3dDQUMzQixLQUFLLEVBQUUsYUFBYSxDQUFDLEtBQUs7cUNBQzdCLENBQUM7Z0NBQ0YsV0FBVyxFQUFFO29DQUNUO3dDQUNJLElBQUksRUFBRSxZQUFZO3dDQUNsQixTQUFTLEVBQUUsZ0JBQWdCO3FDQUM5QjtpQ0FBQzs2QkFDVDt5QkFDSjt3QkFDRCxNQUFNLEVBQUU7NEJBQ0o7Z0NBQ0ksSUFBSSxFQUFFLFlBQVk7Z0NBQ2xCLHFCQUFxQixFQUFFO29DQUNuQixTQUFTLEVBQUUsZ0JBQWdCO2lDQUM5Qjs2QkFFSjt5QkFBQztxQkFFVDtpQkFDSjthQUNKO1NBQ0osQ0FBQyxDQUFDO0lBQ1AsQ0FBQztJQUVPLDZCQUE2QjtRQUNqQyxJQUFJLFVBQVUsQ0FBQyxPQUFPLENBQUMsT0FBTyxDQUFDLElBQUksRUFBRSwyQkFBMkIsRUFBRTtZQUM5RCxRQUFRLEVBQUU7Z0JBQ04sTUFBTSxFQUFFO29CQUNKLEdBQUcsRUFBRSxtQ0FBdUI7aUJBQy9CO2dCQUNELElBQUksRUFBRSwyQkFBMkI7Z0JBQ2pDLFNBQVMsRUFBRSxtQkFBbUI7YUFDakM7WUFDRCxJQUFJLEVBQUU7Z0JBQ0YsSUFBSSxFQUFFO29CQUNGO3dCQUNJLElBQUksRUFBRSxJQUFJO3dCQUNWLFVBQVUsRUFBRSxNQUFNO3FCQUNyQjtpQkFFSjtnQkFFRCxRQUFRLEVBQUU7b0JBQ04sR0FBRyxFQUFFLG1DQUF1QjtpQkFDL0I7Z0JBQ0QsSUFBSSxFQUFFLFVBQVU7YUFDbkI7U0FDSixDQUFDLENBQUM7SUFDUCxDQUFDO0lBRU8scUNBQXFDO1FBQ3pDLElBQUksVUFBVSxDQUFDLE9BQU8sQ0FBQyxPQUFPLENBQUMsSUFBSSxFQUFFLG9DQUFvQyxFQUFFO1lBQ3ZFLFFBQVEsRUFBRTtnQkFDTixNQUFNLEVBQUU7b0JBQ0osR0FBRyxFQUFFLG1DQUF1QjtpQkFDL0I7Z0JBQ0QsSUFBSSxFQUFFLG9DQUFvQztnQkFDMUMsU0FBUyxFQUFFLG1CQUFtQjthQUNqQztZQUNELElBQUksRUFBRTtnQkFDRixJQUFJLEVBQUU7b0JBQ0Y7d0JBQ0ksSUFBSSxFQUFFLElBQUk7d0JBQ1YsVUFBVSxFQUFFLE1BQU07cUJBQ3JCO2lCQUVKO2dCQUVELFFBQVEsRUFBRTtvQkFDTixHQUFHLEVBQUUsbUNBQXVCO2lCQUMvQjtnQkFDRCxJQUFJLEVBQUUsVUFBVTthQUNuQjtTQUNKLENBQUMsQ0FBQztJQUNQLENBQUM7SUFFTyx3Q0FBd0M7UUFDNUMsSUFBSSxVQUFVLENBQUMsVUFBVSxDQUFDLFVBQVUsQ0FBQyxJQUFJLEVBQUUsbUNBQXVCLEdBQUcsV0FBVyxFQUFFO1lBQzlFLFFBQVEsRUFBRTtnQkFDTixNQUFNLEVBQUU7b0JBQ0osR0FBRyxFQUFFLG1DQUF1QjtpQkFDL0I7Z0JBQ0QsSUFBSSxFQUFFLG1DQUF1QixHQUFHLFdBQVc7Z0JBQzNDLFNBQVMsRUFBRSxtQkFBbUI7YUFDakM7WUFDRCxJQUFJLEVBQUU7Z0JBQ0YsUUFBUSxFQUFFLEdBQUc7Z0JBQ2IsUUFBUSxFQUFFO29CQUNOLFdBQVcsRUFBRTt3QkFDVCxHQUFHLEVBQUUsbUNBQXVCO3FCQUMvQjtpQkFDSjtnQkFDRCxRQUFRLEVBQUU7b0JBQ04sUUFBUSxFQUFFO3dCQUNOLE1BQU0sRUFBRTs0QkFDSixHQUFHLEVBQUUsbUNBQXVCO3lCQUMvQjtxQkFDSjtvQkFDRCxJQUFJLEVBQUU7d0JBQ0YsU0FBUyxFQUFFOzRCQUNQO2dDQUNJLEtBQUssRUFBRSx5REFBeUQ7Z0NBQ2hFLElBQUksRUFBRSxtQ0FBdUI7Z0NBQzdCLElBQUksRUFBRSxDQUFDO3dDQUNILGFBQWEsRUFBRSxJQUFJO3FDQUN0QixDQUFDO2dDQUNGLEdBQUcsRUFBRSxDQUFDO3dDQUNGLElBQUksRUFBRSwwQkFBMEI7d0NBQ2hDLEtBQUssRUFBRSx1Q0FBdUM7cUNBQ2pELEVBQUU7d0NBQ0MsSUFBSSxFQUFFLDZCQUE2Qjt3Q0FDbkMsS0FBSyxFQUFFLHFDQUFxQztxQ0FDL0M7aUNBQ0E7NkJBQ0o7eUJBQ0o7cUJBQ0o7aUJBQ0o7YUFDSjtTQUNKLENBQUMsQ0FBQztJQUNQLENBQUM7SUFHTyxnQ0FBZ0MsQ0FBQyxzQkFBeUMsRUFBRSwwQkFBNkMsRUFBRSwyQkFBOEM7UUFDN0ssSUFBSSxVQUFVLENBQUMsVUFBVSxDQUFDLFVBQVUsQ0FBQyxJQUFJLEVBQUUsbUNBQXVCLEVBQUU7WUFDaEUsUUFBUSxFQUFFO2dCQUNOLE1BQU0sRUFBRTtvQkFDSixHQUFHLEVBQUUsbUNBQXVCO2lCQUMvQjtnQkFDRCxJQUFJLEVBQUUsbUNBQXVCO2dCQUM3QixTQUFTLEVBQUUsbUJBQW1CO2FBQ2pDO1lBQ0QsSUFBSSxFQUFFO2dCQUNGLFFBQVEsRUFBRSxHQUFHO2dCQUNiLFFBQVEsRUFBRTtvQkFDTixXQUFXLEVBQUU7d0JBQ1QsR0FBRyxFQUFFLG1DQUF1QjtxQkFDL0I7aUJBQ0o7Z0JBQ0QsUUFBUSxFQUFFO29CQUNOLFFBQVEsRUFBRTt3QkFDTixNQUFNLEVBQUU7NEJBQ0osR0FBRyxFQUFFLG1DQUF1Qjt5QkFDL0I7cUJBQ0o7b0JBQ0QsSUFBSSxFQUFFO3dCQUNGLFNBQVMsRUFBRTs0QkFDUDtnQ0FDSSxLQUFLLEVBQUUsd0RBQXdEO2dDQUMvRCxJQUFJLEVBQUUsbUNBQXVCO2dDQUM3QixJQUFJLEVBQUUsQ0FBQzt3Q0FDSCxhQUFhLEVBQUUsSUFBSTtxQ0FDdEIsQ0FBQztnQ0FDRixHQUFHLEVBQUUsQ0FBQzt3Q0FDRixJQUFJLEVBQUUsZ0JBQWdCO3dDQUN0QixLQUFLLEVBQUUsWUFBWTtxQ0FDdEIsRUFBRTt3Q0FDQyxJQUFJLEVBQUUsY0FBYzt3Q0FDcEIsS0FBSyxFQUFFLDRDQUE0QztxQ0FDdEQsRUFBRTt3Q0FDQyxJQUFJLEVBQUUsbUJBQW1CO3dDQUN6QixLQUFLLEVBQUUsMkJBQTJCLENBQUMsS0FBSztxQ0FDM0MsRUFBRTt3Q0FDQyxJQUFJLEVBQUUsbUJBQW1CO3dDQUN6QixLQUFLLEVBQUUsc0JBQXNCLENBQUMsS0FBSztxQ0FDdEMsRUFBRTt3Q0FDQyxJQUFJLEVBQUUsd0JBQXdCO3dDQUM5QixLQUFLLEVBQUUsMEJBQTBCLENBQUMsS0FBSztxQ0FDMUM7aUNBQ0E7Z0NBQ0QsV0FBVyxFQUFFO29DQUNUO3dDQUNJLElBQUksRUFBRSxpQkFBaUI7d0NBQ3ZCLFNBQVMsRUFBRSxXQUFXO3FDQUN6QjtvQ0FDRDt3Q0FDSSxJQUFJLEVBQUUscUJBQXFCO3dDQUMzQixTQUFTLEVBQUUsZUFBZTtxQ0FDN0I7b0NBQ0Q7d0NBQ0ksSUFBSSxFQUFFLFdBQVc7d0NBQ2pCLFNBQVMsRUFBRSxLQUFLO3FDQUNuQjtpQ0FBQzs2QkFDVDt5QkFDSjt3QkFDRCxNQUFNLEVBQUU7NEJBQ0o7Z0NBQ0ksSUFBSSxFQUFFLGlCQUFpQjtnQ0FDdkIsUUFBUSxFQUFFO29DQUNOLElBQUksRUFBRSxxQkFBcUI7aUNBQzlCOzZCQUNKOzRCQUNEO2dDQUNJLElBQUksRUFBRSxxQkFBcUI7Z0NBQzNCLFFBQVEsRUFBRTtvQ0FDTixJQUFJLEVBQUUseUJBQXlCO2lDQUNsQzs2QkFDSjs0QkFDRDtnQ0FDSSxJQUFJLEVBQUUsV0FBVztnQ0FDakIsUUFBUSxFQUFFO29DQUNOLElBQUksRUFBRSxpRkFBaUY7aUNBQzFGOzZCQUNKO3lCQUNKO3FCQUNKO2lCQUNKO2FBQ0o7U0FDSixDQUFDLENBQUM7SUFDUCxDQUFDO0lBRU8sdUJBQXVCO1FBRTNCLE9BQU8sSUFBSSx5QkFBaUIsQ0FBQyxJQUFJLEVBQUUsWUFBWSxFQUFFO1lBQzdDLElBQUksRUFBRSxRQUFRO1lBQ2QsV0FBVyxFQUFFLHlCQUF5QjtZQUN0QyxTQUFTLEVBQUUsSUFBSTtTQUNsQixDQUFDLENBQUM7SUFDUCxDQUFDO0lBRU8saUJBQWlCO1FBRXJCLE9BQU8sSUFBSSx5QkFBaUIsQ0FBQyxJQUFJLEVBQUUsY0FBYyxFQUFFO1lBQy9DLElBQUksRUFBRSxRQUFRO1lBQ2QsV0FBVyxFQUFFLG1CQUFtQjtZQUNoQyxTQUFTLEVBQUUsSUFBSTtTQUNsQixDQUFDLENBQUM7SUFDUCxDQUFDO0lBRU8scUJBQXFCO1FBRXpCLE9BQU8sSUFBSSx5QkFBaUIsQ0FBQyxJQUFJLEVBQUUsa0JBQWtCLEVBQUU7WUFDbkQsSUFBSSxFQUFFLFFBQVE7WUFDZCxXQUFXLEVBQUUsd0JBQXdCO1lBQ3JDLFNBQVMsRUFBRSxJQUFJO1NBQ2xCLENBQUMsQ0FBQztJQUNQLENBQUM7Q0FVSjtBQXBXRCx3RUFvV0MiLCJzb3VyY2VzQ29udGVudCI6WyIvLyB0cmFkaW5nLWFzc2lzdGFudC1zdGF0ZWZ1bC50c1xuXG5pbXBvcnQge0NvbnN0cnVjdH0gZnJvbSBcImNvbnN0cnVjdHNcIjtcbmltcG9ydCB7VGVycmFmb3JtU3RhY2ssIFRlcnJhZm9ybVZhcmlhYmxlfSBmcm9tIFwiY2RrdGZcIjtcbmltcG9ydCB7S3ViZXJuZXRlc1Byb3ZpZGVyfSBmcm9tIFwiQGNka3RmL3Byb3ZpZGVyLWt1YmVybmV0ZXMvbGliL3Byb3ZpZGVyXCI7XG5pbXBvcnQge01ZU1FMX0xBQkVMLCBUUkFESU5HX0FTU0lTVEFOVF9MQUJFTH0gZnJvbSBcIi4uL2NvbnN0YW50c1wiO1xuaW1wb3J0ICogYXMga3ViZXJuZXRlcyBmcm9tIFwiQGNka3RmL3Byb3ZpZGVyLWt1YmVybmV0ZXNcIjtcblxuZXhwb3J0IGNsYXNzIFRyYWRpbmdBc3Npc3RhbnRTdGF0ZWxlc3NTdGFjayBleHRlbmRzIFRlcnJhZm9ybVN0YWNrIHtcbiAgICBjb25zdHJ1Y3RvcihzY29wZTogQ29uc3RydWN0LCBuYW1lOiBzdHJpbmcpIHtcbiAgICAgICAgc3VwZXIoc2NvcGUsIG5hbWUpO1xuXG5cbiAgICAgICAgbmV3IEt1YmVybmV0ZXNQcm92aWRlcih0aGlzLCAnSzhzJywge1xuICAgICAgICAgICAgXCJjb25maWdQYXRoXCI6IFwifi8ua3ViZS9jb25maWdcIixcbiAgICAgICAgICAgIFwiY29uZmlnQ29udGV4dFwiOiBcImt1YmVybmV0ZXMtYWRtaW5Aa3ViZXJuZXRlc1wiXG4gICAgICAgIH0pO1xuICAgICAgICB0aGlzLmNyZWF0ZVRyYWRpbmdBc3Npc3RhbnRBbmRNeXNxbEluZ3Jlc3MyKCk7XG5cbiAgICAgICAgbGV0IGFkbWluUGFzc3dvcmQgPSB0aGlzLmNyZWF0ZURCVGVycmFmb3JtU2VjcmV0KCk7XG4gICAgICAgIHRoaXMuY3JlYXRlVHJhZGluZ0Fzc2lzdGFudERlcGxveW1lbnQodGhpcy5jcmVhdGVTbGFja1NlY3JldCgpLCB0aGlzLmNyZWF0ZVN1bW9Mb2dpY1NlY3JldCgpLCBhZG1pblBhc3N3b3JkKTtcbiAgICAgICAgdGhpcy5jcmVhdGVUcmFkaW5nQXNzaXN0YW50RnJvbnRlbmREZXBsb3ltZW50KCk7XG4gICAgICAgIHRoaXMuY3JlYXRlVHJhZGluZ0Fzc2lzdGFudFNlcnZpY2UoKTtcbiAgICAgICAgdGhpcy5jcmVhdGVUcmFkaW5nQXNzaXN0YW50RnJvbnRlbmRTZXJ2aWNlKCk7XG4gICAgICAgIHRoaXMuY3JlYXRlTXlzcWxEZXBsb3ltZW50KGFkbWluUGFzc3dvcmQpO1xuICAgICAgICB0aGlzLmNyZWF0ZU15U3FsU2VydmljZSgpO1xuICAgIH1cblxuICAgIHByaXZhdGUgY3JlYXRlVHJhZGluZ0Fzc2lzdGFudEFuZE15c3FsSW5ncmVzczIoKSB7XG4gICAgICAgIG5ldyBrdWJlcm5ldGVzLm1hbmlmZXN0Lk1hbmlmZXN0KHRoaXMsIFwidHJhZGluZy1hc3Npc3RhbnQtaW5ncmVzc1wiLCB7XG4gICAgICAgICAgICBtYW5pZmVzdDoge1xuICAgICAgICAgICAgICAgIGFwaVZlcnNpb246IFwibmV0d29ya2luZy5rOHMuaW8vdjFcIixcbiAgICAgICAgICAgICAgICBraW5kOiBcIkluZ3Jlc3NcIixcbiAgICAgICAgICAgICAgICBtZXRhZGF0YToge1xuICAgICAgICAgICAgICAgICAgICBuYW1lOiBcInRyYWRpbmctYXNzaXN0YW50LWluZ3Jlc3NcIixcbiAgICAgICAgICAgICAgICAgICAgbmFtZXNwYWNlOiBcInRyYWRpbmctYXNzaXN0YW50XCIsXG4gICAgICAgICAgICAgICAgfSxcbiAgICAgICAgICAgICAgICBzcGVjOiB7XG4gICAgICAgICAgICAgICAgICAgIHJ1bGVzOiBbXG4gICAgICAgICAgICAgICAgICAgICAgICB7XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgaG9zdDogXCJ0cmFkaW5nLWFzc2lzdGFudC5tb2NoaS10cmFkaW5nLmNvbVwiLFxuICAgICAgICAgICAgICAgICAgICAgICAgICAgIGh0dHA6IHtcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgcGF0aHM6IFtcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIHtcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICBwYXRoOiBcIi9cIixcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICBwYXRoVHlwZTogXCJQcmVmaXhcIixcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICBiYWNrZW5kOiB7XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIHNlcnZpY2U6IHtcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIG5hbWU6IFwidHJhZGluZy1hc3Npc3RhbnQtc2VydmljZVwiLFxuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgcG9ydDoge1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIG51bWJlcjogODA4MCxcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIH0sXG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIH0sXG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgfSxcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIH0sXG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIF0sXG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgfSxcbiAgICAgICAgICAgICAgICAgICAgICAgIH0sXG4gICAgICAgICAgICAgICAgICAgIF0sXG4gICAgICAgICAgICAgICAgfSxcbiAgICAgICAgICAgIH0sXG4gICAgICAgIH0pO1xuICAgIH1cblxuICAgIHByaXZhdGUgY3JlYXRlTXlTcWxTZXJ2aWNlKCkge1xuICAgICAgICBuZXcga3ViZXJuZXRlcy5zZXJ2aWNlLlNlcnZpY2UodGhpcywgXCJteXNxbC1zZXJ2aWNlXCIsIHtcbiAgICAgICAgICAgIG1ldGFkYXRhOiB7XG4gICAgICAgICAgICAgICAgbGFiZWxzOiB7XG4gICAgICAgICAgICAgICAgICAgIGFwcDogTVlTUUxfTEFCRUwsXG4gICAgICAgICAgICAgICAgfSxcbiAgICAgICAgICAgICAgICBuYW1lOiAnbXlzcWwtc2VydmljZScsXG4gICAgICAgICAgICAgICAgbmFtZXNwYWNlOiAndHJhZGluZy1hc3Npc3RhbnQnLFxuICAgICAgICAgICAgfSxcbiAgICAgICAgICAgIHNwZWM6IHtcbiAgICAgICAgICAgICAgICBwb3J0OiBbe1xuICAgICAgICAgICAgICAgICAgICBwb3J0OiAzMzA2LFxuICAgICAgICAgICAgICAgICAgICB0YXJnZXRQb3J0OiBcIjMzMDZcIixcbiAgICAgICAgICAgICAgICB9XSxcbiAgICAgICAgICAgICAgICBzZWxlY3Rvcjoge1xuICAgICAgICAgICAgICAgICAgICBhcHA6IE1ZU1FMX0xBQkVMLFxuICAgICAgICAgICAgICAgIH0sXG4gICAgICAgICAgICAgICAgdHlwZTogJ05vZGVQb3J0JyxcbiAgICAgICAgICAgIH0sXG4gICAgICAgIH0pO1xuICAgIH1cblxuICAgIHByaXZhdGUgY3JlYXRlTXlzcWxEZXBsb3ltZW50KGFkbWluUGFzc3dvcmQ6IFRlcnJhZm9ybVZhcmlhYmxlKSB7XG4gICAgICAgIG5ldyBrdWJlcm5ldGVzLmRlcGxveW1lbnQuRGVwbG95bWVudCh0aGlzLCBNWVNRTF9MQUJFTCwge1xuICAgICAgICAgICAgbWV0YWRhdGE6IHtcbiAgICAgICAgICAgICAgICBsYWJlbHM6IHtcbiAgICAgICAgICAgICAgICAgICAgYXBwOiBNWVNRTF9MQUJFTCxcbiAgICAgICAgICAgICAgICB9LFxuICAgICAgICAgICAgICAgIG5hbWU6ICdteXNxbC1jb250YWluZXInLFxuICAgICAgICAgICAgICAgIG5hbWVzcGFjZTogJ3RyYWRpbmctYXNzaXN0YW50JyxcbiAgICAgICAgICAgIH0sXG4gICAgICAgICAgICBzcGVjOiB7XG4gICAgICAgICAgICAgICAgcmVwbGljYXM6ICcxJyxcbiAgICAgICAgICAgICAgICBzZWxlY3Rvcjoge1xuICAgICAgICAgICAgICAgICAgICBtYXRjaExhYmVsczoge1xuICAgICAgICAgICAgICAgICAgICAgICAgYXBwOiBNWVNRTF9MQUJFTCxcbiAgICAgICAgICAgICAgICAgICAgfSxcbiAgICAgICAgICAgICAgICB9LFxuICAgICAgICAgICAgICAgIHRlbXBsYXRlOiB7XG4gICAgICAgICAgICAgICAgICAgIG1ldGFkYXRhOiB7XG4gICAgICAgICAgICAgICAgICAgICAgICBsYWJlbHM6IHtcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICBhcHA6IE1ZU1FMX0xBQkVMLFxuICAgICAgICAgICAgICAgICAgICAgICAgfSxcbiAgICAgICAgICAgICAgICAgICAgfSxcbiAgICAgICAgICAgICAgICAgICAgc3BlYzoge1xuICAgICAgICAgICAgICAgICAgICAgICAgY29udGFpbmVyOiBbXG4gICAgICAgICAgICAgICAgICAgICAgICAgICAge1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICBpbWFnZTogJ215c3FsOmxhdGVzdCcsXG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIG5hbWU6IE1ZU1FMX0xBQkVMLFxuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICBwb3J0OiBbe1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgY29udGFpbmVyUG9ydDogMzMwNixcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgfV0sXG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIGVudjogW3tcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIG5hbWU6ICdNWVNRTF9ST09UX1BBU1NXT1JEJyxcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIHZhbHVlOiBhZG1pblBhc3N3b3JkLnZhbHVlLFxuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICB9XSxcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgdm9sdW1lTW91bnQ6IFtcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIHtcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICBuYW1lOiAnbXlzcWwtZGF0YScsXG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgbW91bnRQYXRoOiAnL3Zhci9saWIvbXlzcWwnLFxuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgfV0sXG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgfSxcbiAgICAgICAgICAgICAgICAgICAgICAgIF0sXG4gICAgICAgICAgICAgICAgICAgICAgICB2b2x1bWU6IFtcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICB7XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIG5hbWU6ICdteXNxbC1kYXRhJyxcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgcGVyc2lzdGVudFZvbHVtZUNsYWltOiB7XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICBjbGFpbU5hbWU6ICdteXNxbC1wdi1jbGFpbSdcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAsXG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgfV1cblxuICAgICAgICAgICAgICAgICAgICB9LFxuICAgICAgICAgICAgICAgIH0sXG4gICAgICAgICAgICB9LFxuICAgICAgICB9KTtcbiAgICB9XG5cbiAgICBwcml2YXRlIGNyZWF0ZVRyYWRpbmdBc3Npc3RhbnRTZXJ2aWNlKCkge1xuICAgICAgICBuZXcga3ViZXJuZXRlcy5zZXJ2aWNlLlNlcnZpY2UodGhpcywgXCJ0cmFkaW5nLWFzc2lzdGFudC1zZXJ2aWNlXCIsIHtcbiAgICAgICAgICAgIG1ldGFkYXRhOiB7XG4gICAgICAgICAgICAgICAgbGFiZWxzOiB7XG4gICAgICAgICAgICAgICAgICAgIGFwcDogVFJBRElOR19BU1NJU1RBTlRfTEFCRUwsXG4gICAgICAgICAgICAgICAgfSxcbiAgICAgICAgICAgICAgICBuYW1lOiAndHJhZGluZy1hc3Npc3RhbnQtc2VydmljZScsXG4gICAgICAgICAgICAgICAgbmFtZXNwYWNlOiAndHJhZGluZy1hc3Npc3RhbnQnLFxuICAgICAgICAgICAgfSxcbiAgICAgICAgICAgIHNwZWM6IHtcbiAgICAgICAgICAgICAgICBwb3J0OiBbXG4gICAgICAgICAgICAgICAgICAgIHtcbiAgICAgICAgICAgICAgICAgICAgICAgIHBvcnQ6IDgwODAsXG4gICAgICAgICAgICAgICAgICAgICAgICB0YXJnZXRQb3J0OiBcIjgwODBcIixcbiAgICAgICAgICAgICAgICAgICAgfVxuXG4gICAgICAgICAgICAgICAgXSxcblxuICAgICAgICAgICAgICAgIHNlbGVjdG9yOiB7XG4gICAgICAgICAgICAgICAgICAgIGFwcDogVFJBRElOR19BU1NJU1RBTlRfTEFCRUwsXG4gICAgICAgICAgICAgICAgfSxcbiAgICAgICAgICAgICAgICB0eXBlOiAnTm9kZVBvcnQnLFxuICAgICAgICAgICAgfSxcbiAgICAgICAgfSk7XG4gICAgfVxuXG4gICAgcHJpdmF0ZSBjcmVhdGVUcmFkaW5nQXNzaXN0YW50RnJvbnRlbmRTZXJ2aWNlKCkge1xuICAgICAgICBuZXcga3ViZXJuZXRlcy5zZXJ2aWNlLlNlcnZpY2UodGhpcywgXCJ0cmFkaW5nLWFzc2lzdGFudC1mcm9udGVuZC1zZXJ2aWNlXCIsIHtcbiAgICAgICAgICAgIG1ldGFkYXRhOiB7XG4gICAgICAgICAgICAgICAgbGFiZWxzOiB7XG4gICAgICAgICAgICAgICAgICAgIGFwcDogVFJBRElOR19BU1NJU1RBTlRfTEFCRUwsXG4gICAgICAgICAgICAgICAgfSxcbiAgICAgICAgICAgICAgICBuYW1lOiAndHJhZGluZy1hc3Npc3RhbnQtZnJvbnRlbmQtc2VydmljZScsXG4gICAgICAgICAgICAgICAgbmFtZXNwYWNlOiAndHJhZGluZy1hc3Npc3RhbnQnLFxuICAgICAgICAgICAgfSxcbiAgICAgICAgICAgIHNwZWM6IHtcbiAgICAgICAgICAgICAgICBwb3J0OiBbXG4gICAgICAgICAgICAgICAgICAgIHtcbiAgICAgICAgICAgICAgICAgICAgICAgIHBvcnQ6IDMwMDAsXG4gICAgICAgICAgICAgICAgICAgICAgICB0YXJnZXRQb3J0OiBcIjMwMDBcIixcbiAgICAgICAgICAgICAgICAgICAgfVxuXG4gICAgICAgICAgICAgICAgXSxcblxuICAgICAgICAgICAgICAgIHNlbGVjdG9yOiB7XG4gICAgICAgICAgICAgICAgICAgIGFwcDogVFJBRElOR19BU1NJU1RBTlRfTEFCRUwsXG4gICAgICAgICAgICAgICAgfSxcbiAgICAgICAgICAgICAgICB0eXBlOiAnTm9kZVBvcnQnLFxuICAgICAgICAgICAgfSxcbiAgICAgICAgfSk7XG4gICAgfVxuXG4gICAgcHJpdmF0ZSBjcmVhdGVUcmFkaW5nQXNzaXN0YW50RnJvbnRlbmREZXBsb3ltZW50KCkge1xuICAgICAgICBuZXcga3ViZXJuZXRlcy5kZXBsb3ltZW50LkRlcGxveW1lbnQodGhpcywgVFJBRElOR19BU1NJU1RBTlRfTEFCRUwgKyAnLWZyb250ZW5kJywge1xuICAgICAgICAgICAgbWV0YWRhdGE6IHtcbiAgICAgICAgICAgICAgICBsYWJlbHM6IHtcbiAgICAgICAgICAgICAgICAgICAgYXBwOiBUUkFESU5HX0FTU0lTVEFOVF9MQUJFTCxcbiAgICAgICAgICAgICAgICB9LFxuICAgICAgICAgICAgICAgIG5hbWU6IFRSQURJTkdfQVNTSVNUQU5UX0xBQkVMICsgJy1mcm9udGVuZCcsXG4gICAgICAgICAgICAgICAgbmFtZXNwYWNlOiAndHJhZGluZy1hc3Npc3RhbnQnLFxuICAgICAgICAgICAgfSxcbiAgICAgICAgICAgIHNwZWM6IHtcbiAgICAgICAgICAgICAgICByZXBsaWNhczogJzEnLFxuICAgICAgICAgICAgICAgIHNlbGVjdG9yOiB7XG4gICAgICAgICAgICAgICAgICAgIG1hdGNoTGFiZWxzOiB7XG4gICAgICAgICAgICAgICAgICAgICAgICBhcHA6IFRSQURJTkdfQVNTSVNUQU5UX0xBQkVMLFxuICAgICAgICAgICAgICAgICAgICB9LFxuICAgICAgICAgICAgICAgIH0sXG4gICAgICAgICAgICAgICAgdGVtcGxhdGU6IHtcbiAgICAgICAgICAgICAgICAgICAgbWV0YWRhdGE6IHtcbiAgICAgICAgICAgICAgICAgICAgICAgIGxhYmVsczoge1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgIGFwcDogVFJBRElOR19BU1NJU1RBTlRfTEFCRUwsXG4gICAgICAgICAgICAgICAgICAgICAgICB9LFxuICAgICAgICAgICAgICAgICAgICB9LFxuICAgICAgICAgICAgICAgICAgICBzcGVjOiB7XG4gICAgICAgICAgICAgICAgICAgICAgICBjb250YWluZXI6IFtcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICB7XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIGltYWdlOiAnZ2hjci5pby93aWxsaHVtcGhyZXlzL3RyYWRpbmctYXNzaXN0YW50OmZyb250ZW5kLWxhdGVzdCcsXG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIG5hbWU6IFRSQURJTkdfQVNTSVNUQU5UX0xBQkVMLFxuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICBwb3J0OiBbe1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgY29udGFpbmVyUG9ydDogMzAwMCxcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgfV0sXG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIGVudjogW3tcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIG5hbWU6ICdORVhUX1BVQkxJQ19CQUNLRU5EX0hPU1QnLFxuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgdmFsdWU6ICdodHRwOi8vdHJhZGluZy1hc3Npc3RhbnQtc2VydmljZTo4MDgwJyxcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgfSwge1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgbmFtZTogJ05FWFRfUFVCTElDX1dFQl9TT0NLRVRfSE9TVCcsXG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICB2YWx1ZTogJ3dzOi8vdHJhZGluZy1hc3Npc3RhbnQtc2VydmljZTo4MDgwJyxcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICBdXG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgfSxcbiAgICAgICAgICAgICAgICAgICAgICAgIF1cbiAgICAgICAgICAgICAgICAgICAgfSxcbiAgICAgICAgICAgICAgICB9LFxuICAgICAgICAgICAgfSxcbiAgICAgICAgfSk7XG4gICAgfVxuXG5cbiAgICBwcml2YXRlIGNyZWF0ZVRyYWRpbmdBc3Npc3RhbnREZXBsb3ltZW50KHNsYWNrVGVycmFmb3JtVmFyaWFibGU6IFRlcnJhZm9ybVZhcmlhYmxlLCBzdW1vTG9naWNUZXJyYWZvcm1WYXJpYWJsZTogVGVycmFmb3JtVmFyaWFibGUsIGRiUGFzc3dvcmRUZXJyYWZvcm1WYXJpYWJsZTogVGVycmFmb3JtVmFyaWFibGUpIHtcbiAgICAgICAgbmV3IGt1YmVybmV0ZXMuZGVwbG95bWVudC5EZXBsb3ltZW50KHRoaXMsIFRSQURJTkdfQVNTSVNUQU5UX0xBQkVMLCB7XG4gICAgICAgICAgICBtZXRhZGF0YToge1xuICAgICAgICAgICAgICAgIGxhYmVsczoge1xuICAgICAgICAgICAgICAgICAgICBhcHA6IFRSQURJTkdfQVNTSVNUQU5UX0xBQkVMLFxuICAgICAgICAgICAgICAgIH0sXG4gICAgICAgICAgICAgICAgbmFtZTogVFJBRElOR19BU1NJU1RBTlRfTEFCRUwsXG4gICAgICAgICAgICAgICAgbmFtZXNwYWNlOiAndHJhZGluZy1hc3Npc3RhbnQnLFxuICAgICAgICAgICAgfSxcbiAgICAgICAgICAgIHNwZWM6IHtcbiAgICAgICAgICAgICAgICByZXBsaWNhczogJzEnLFxuICAgICAgICAgICAgICAgIHNlbGVjdG9yOiB7XG4gICAgICAgICAgICAgICAgICAgIG1hdGNoTGFiZWxzOiB7XG4gICAgICAgICAgICAgICAgICAgICAgICBhcHA6IFRSQURJTkdfQVNTSVNUQU5UX0xBQkVMLFxuICAgICAgICAgICAgICAgICAgICB9LFxuICAgICAgICAgICAgICAgIH0sXG4gICAgICAgICAgICAgICAgdGVtcGxhdGU6IHtcbiAgICAgICAgICAgICAgICAgICAgbWV0YWRhdGE6IHtcbiAgICAgICAgICAgICAgICAgICAgICAgIGxhYmVsczoge1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgIGFwcDogVFJBRElOR19BU1NJU1RBTlRfTEFCRUwsXG4gICAgICAgICAgICAgICAgICAgICAgICB9LFxuICAgICAgICAgICAgICAgICAgICB9LFxuICAgICAgICAgICAgICAgICAgICBzcGVjOiB7XG4gICAgICAgICAgICAgICAgICAgICAgICBjb250YWluZXI6IFtcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICB7XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIGltYWdlOiAnZ2hjci5pby93aWxsaHVtcGhyZXlzL3RyYWRpbmctYXNzaXN0YW50OmJhY2tlbmQtbGF0ZXN0JyxcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgbmFtZTogVFJBRElOR19BU1NJU1RBTlRfTEFCRUwsXG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIHBvcnQ6IFt7XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICBjb250YWluZXJQb3J0OiA4MDgwLFxuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICB9XSxcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgZW52OiBbe1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgbmFtZTogJ1NQUklOR19QUk9GSUxFJyxcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIHZhbHVlOiAnY3VycmVuY2llcycsXG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIH0sIHtcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIG5hbWU6ICdEQVRBQkFTRV9VUkwnLFxuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgdmFsdWU6ICdqZGJjOm15c3FsOi8vbXlzcWwtc2VydmljZTozMzA2L21ldGF0cmFkZXInLFxuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICB9LCB7XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICBuYW1lOiAnREFUQUJBU0VfUEFTU1dPUkQnLFxuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgdmFsdWU6IGRiUGFzc3dvcmRUZXJyYWZvcm1WYXJpYWJsZS52YWx1ZSxcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgfSwge1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgbmFtZTogJ1NMQUNLX1dFQkhPT0tfVVJMJyxcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIHZhbHVlOiBzbGFja1RlcnJhZm9ybVZhcmlhYmxlLnZhbHVlLFxuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICB9LCB7XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICBuYW1lOiAnU1VNT19MT0dJQ19XRUJIT09LX1VSTCcsXG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICB2YWx1ZTogc3Vtb0xvZ2ljVGVycmFmb3JtVmFyaWFibGUudmFsdWUsXG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgXSxcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgdm9sdW1lTW91bnQ6IFtcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIHtcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICBuYW1lOiAnYWNjb3VudHMtdm9sdW1lJyxcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICBtb3VudFBhdGg6ICcvYWNjb3VudHMnLFxuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgfSxcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIHtcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICBuYW1lOiAnbW9jaGktZ3JhcGhzLXZvbHVtZScsXG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgbW91bnRQYXRoOiAnL21vY2hpLWdyYXBocycsXG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICB9LFxuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAge1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIG5hbWU6ICdtdC12b2x1bWUnLFxuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIG1vdW50UGF0aDogJy9tdCcsXG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICB9XVxuICAgICAgICAgICAgICAgICAgICAgICAgICAgIH0sXG4gICAgICAgICAgICAgICAgICAgICAgICBdLFxuICAgICAgICAgICAgICAgICAgICAgICAgdm9sdW1lOiBbXG4gICAgICAgICAgICAgICAgICAgICAgICAgICAge1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICBuYW1lOiAnYWNjb3VudHMtdm9sdW1lJyxcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgaG9zdFBhdGg6IHtcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIHBhdGg6ICcvaG9tZS93aWxsL2FjY291bnRzJyxcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgfSxcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICB9LFxuICAgICAgICAgICAgICAgICAgICAgICAgICAgIHtcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgbmFtZTogJ21vY2hpLWdyYXBocy12b2x1bWUnLFxuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICBob3N0UGF0aDoge1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgcGF0aDogJy9ob21lL3dpbGwvbW9jaGktZ3JhcGhzJyxcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgfSxcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICB9LFxuICAgICAgICAgICAgICAgICAgICAgICAgICAgIHtcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgbmFtZTogJ210LXZvbHVtZScsXG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIGhvc3RQYXRoOiB7XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICBwYXRoOiAnL2hvbWUvd2lsbC8uY3hvZmZpY2UvTWV0YVRyYWRlcl81L2RyaXZlX2MvUHJvZ3JhbSBGaWxlcy9NZXRhVHJhZGVyIDUvTVFMNS9GaWxlcycsXG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIH0sXG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgfSxcbiAgICAgICAgICAgICAgICAgICAgICAgIF0sXG4gICAgICAgICAgICAgICAgICAgIH0sXG4gICAgICAgICAgICAgICAgfSxcbiAgICAgICAgICAgIH0sXG4gICAgICAgIH0pO1xuICAgIH1cblxuICAgIHByaXZhdGUgY3JlYXRlREJUZXJyYWZvcm1TZWNyZXQoKSB7XG5cbiAgICAgICAgcmV0dXJuIG5ldyBUZXJyYWZvcm1WYXJpYWJsZSh0aGlzLCBcImRiUGFzc3dvcmRcIiwge1xuICAgICAgICAgICAgdHlwZTogXCJzdHJpbmdcIixcbiAgICAgICAgICAgIGRlc2NyaXB0aW9uOiBcInJvb3QgcGFzc3dvcmQgZm9yIG15c3FsXCIsXG4gICAgICAgICAgICBzZW5zaXRpdmU6IHRydWUsXG4gICAgICAgIH0pO1xuICAgIH1cblxuICAgIHByaXZhdGUgY3JlYXRlU2xhY2tTZWNyZXQoKSB7XG5cbiAgICAgICAgcmV0dXJuIG5ldyBUZXJyYWZvcm1WYXJpYWJsZSh0aGlzLCBcInNsYWNrV2ViSG9va1wiLCB7XG4gICAgICAgICAgICB0eXBlOiBcInN0cmluZ1wiLFxuICAgICAgICAgICAgZGVzY3JpcHRpb246IFwic2xhY2sgd2ViaG9vayB1cmxcIixcbiAgICAgICAgICAgIHNlbnNpdGl2ZTogdHJ1ZSxcbiAgICAgICAgfSk7XG4gICAgfVxuXG4gICAgcHJpdmF0ZSBjcmVhdGVTdW1vTG9naWNTZWNyZXQoKSB7XG5cbiAgICAgICAgcmV0dXJuIG5ldyBUZXJyYWZvcm1WYXJpYWJsZSh0aGlzLCBcInN1bW9Mb2dpY1dlYkhvb2tcIiwge1xuICAgICAgICAgICAgdHlwZTogXCJzdHJpbmdcIixcbiAgICAgICAgICAgIGRlc2NyaXB0aW9uOiBcInN1bW8gbG9naWMgd2ViaG9vayB1cmxcIixcbiAgICAgICAgICAgIHNlbnNpdGl2ZTogdHJ1ZSxcbiAgICAgICAgfSk7XG4gICAgfVxuXG4gICAgLy8gcHJpdmF0ZSBjcmVhdGVIb21lVmFyaWFibGUoKSB7XG4gICAgLy9cbiAgICAvLyAgICAgcmV0dXJuIG5ldyBUZXJyYWZvcm1WYXJpYWJsZSh0aGlzLCBcImt1YmVIb21lXCIsIHtcbiAgICAvLyAgICAgICAgIHR5cGU6IFwic3RyaW5nXCIsXG4gICAgLy8gICAgICAgICBkZXNjcmlwdGlvbjogXCJrdWJlIGhvbWUgZGlyZWN0b3J5XCIsXG4gICAgLy8gICAgICAgICBzZW5zaXRpdmU6IGZhbHNlLFxuICAgIC8vICAgICB9KTtcbiAgICAvLyB9XG59XG4iXX0=