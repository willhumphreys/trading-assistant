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
    createMySqlService() {
        new kubernetes.service.Service(this, "mysql-service", {
            metadata: {
                labels: {
                    app: constants_1.MYSQL_LABEL,
                },
                name: 'mysql-service',
                namespace: constants_1.TRADING_ASSISTANT_NAMESPACE,
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
                namespace: constants_1.TRADING_ASSISTANT_NAMESPACE,
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
                namespace: constants_1.TRADING_ASSISTANT_NAMESPACE,
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
                    app: constants_1.TRADING_ASSISTANT_FRONTEND_LABEL,
                },
                name: 'trading-assistant-frontend-service',
                namespace: constants_1.TRADING_ASSISTANT_NAMESPACE,
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
                    app: constants_1.TRADING_ASSISTANT_FRONTEND_LABEL,
                },
                type: 'LoadBalancer',
            },
        });
    }
    createTradingAssistantFrontendDeployment() {
        new kubernetes.deployment.Deployment(this, constants_1.TRADING_ASSISTANT_FRONTEND_LABEL, {
            metadata: {
                labels: {
                    app: constants_1.TRADING_ASSISTANT_FRONTEND_LABEL,
                },
                name: constants_1.TRADING_ASSISTANT_FRONTEND_LABEL,
                namespace: constants_1.TRADING_ASSISTANT_NAMESPACE,
            },
            spec: {
                replicas: '2',
                selector: {
                    matchLabels: {
                        app: constants_1.TRADING_ASSISTANT_FRONTEND_LABEL,
                    },
                },
                template: {
                    metadata: {
                        labels: {
                            app: constants_1.TRADING_ASSISTANT_FRONTEND_LABEL,
                        },
                    },
                    spec: {
                        container: [
                            {
                                image: 'ghcr.io/willhumphreys/trading-assistant:frontend-latest',
                                imagePullPolicy: 'Always',
                                name: constants_1.TRADING_ASSISTANT_LABEL,
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
    createTradingAssistantDeployment(slackTerraformVariable, sumoLogicTerraformVariable, dbPasswordTerraformVariable) {
        new kubernetes.deployment.Deployment(this, constants_1.TRADING_ASSISTANT_LABEL, {
            metadata: {
                labels: {
                    app: constants_1.TRADING_ASSISTANT_LABEL,
                },
                name: constants_1.TRADING_ASSISTANT_LABEL,
                namespace: constants_1.TRADING_ASSISTANT_NAMESPACE,
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
                                imagePullPolicy: 'Always',
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
//# sourceMappingURL=data:application/json;base64,eyJ2ZXJzaW9uIjozLCJmaWxlIjoidHJhZGluZy1hc3Npc3RhbnQtc3RhdGVsZXNzLmpzIiwic291cmNlUm9vdCI6IiIsInNvdXJjZXMiOlsidHJhZGluZy1hc3Npc3RhbnQtc3RhdGVsZXNzLnRzIl0sIm5hbWVzIjpbXSwibWFwcGluZ3MiOiI7QUFBQSxnQ0FBZ0M7OztBQUdoQyxpQ0FBd0Q7QUFDeEQsc0VBQTJFO0FBQzNFLDRDQUFpSTtBQUNqSSx5REFBeUQ7QUFFekQsTUFBYSw4QkFBK0IsU0FBUSxzQkFBYztJQUM5RCxZQUFZLEtBQWdCLEVBQUUsSUFBWTtRQUN0QyxLQUFLLENBQUMsS0FBSyxFQUFFLElBQUksQ0FBQyxDQUFDO1FBR25CLElBQUksNkJBQWtCLENBQUMsSUFBSSxFQUFFLEtBQUssRUFBRTtZQUNoQyxZQUFZLEVBQUUsZ0JBQWdCO1lBQzlCLGVBQWUsRUFBRSw2QkFBNkI7U0FDakQsQ0FBQyxDQUFDO1FBQ0wsaURBQWlEO1FBRS9DLElBQUksYUFBYSxHQUFHLElBQUksQ0FBQyx1QkFBdUIsRUFBRSxDQUFDO1FBQ25ELElBQUksQ0FBQyxnQ0FBZ0MsQ0FBQyxJQUFJLENBQUMsaUJBQWlCLEVBQUUsRUFBRSxJQUFJLENBQUMscUJBQXFCLEVBQUUsRUFBRSxhQUFhLENBQUMsQ0FBQztRQUM3RyxJQUFJLENBQUMsd0NBQXdDLEVBQUUsQ0FBQztRQUNoRCxJQUFJLENBQUMsNkJBQTZCLEVBQUUsQ0FBQztRQUNyQyxJQUFJLENBQUMscUNBQXFDLEVBQUUsQ0FBQztRQUM3QyxJQUFJLENBQUMscUJBQXFCLENBQUMsYUFBYSxDQUFDLENBQUM7UUFDMUMsSUFBSSxDQUFDLGtCQUFrQixFQUFFLENBQUM7SUFDOUIsQ0FBQztJQUVELG9EQUFvRDtJQUNwRCxxRkFBcUY7SUFDckYsc0JBQXNCO0lBQ3RCLGtEQUFrRDtJQUNsRCwrQkFBK0I7SUFDL0IsMEJBQTBCO0lBQzFCLHFEQUFxRDtJQUNyRCxrREFBa0Q7SUFDbEQsNEJBQTRCO0lBQzVCLGlFQUFpRTtJQUNqRSxxQkFBcUI7SUFDckIsaUJB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LFdBQVcsRUFBRSx5QkFBeUI7WUFDdEMsU0FBUyxFQUFFLElBQUk7U0FDbEIsQ0FBQyxDQUFDO0lBQ1AsQ0FBQztJQUVPLGlCQUFpQjtRQUVyQixPQUFPLElBQUkseUJBQWlCLENBQUMsSUFBSSxFQUFFLGNBQWMsRUFBRTtZQUMvQyxJQUFJLEVBQUUsUUFBUTtZQUNkLFdBQVcsRUFBRSxtQkFBbUI7WUFDaEMsU0FBUyxFQUFFLElBQUk7U0FDbEIsQ0FBQyxDQUFDO0lBQ1AsQ0FBQztJQUVPLHFCQUFxQjtRQUV6QixPQUFPLElBQUkseUJBQWlCLENBQUMsSUFBSSxFQUFFLGtCQUFrQixFQUFFO1lBQ25ELElBQUksRUFBRSxRQUFRO1lBQ2QsV0FBVyxFQUFFLHdCQUF3QjtZQUNyQyxTQUFTLEVBQUUsSUFBSTtTQUNsQixDQUFDLENBQUM7SUFDUCxDQUFDO0NBVUo7QUFyV0Qsd0VBcVdDIiwic291cmNlc0NvbnRlbnQiOlsiLy8gdHJhZGluZy1hc3Npc3RhbnQtc3RhdGVmdWwudHNcblxuaW1wb3J0IHtDb25zdHJ1Y3R9IGZyb20gXCJjb25zdHJ1Y3RzXCI7XG5pbXBvcnQge1RlcnJhZm9ybVN0YWNrLCBUZXJyYWZvcm1WYXJpYWJsZX0gZnJvbSBcImNka3RmXCI7XG5pbXBvcnQge0t1YmVybmV0ZXNQcm92aWRlcn0gZnJvbSBcIkBjZGt0Zi9wcm92aWRlci1rdWJlcm5ldGVzL2xpYi9wcm92aWRlclwiO1xuaW1wb3J0IHtNWVNRTF9MQUJFTCwgVFJBRElOR19BU1NJU1RBTlRfTEFCRUwsIFRSQURJTkdfQVNTSVNUQU5UX0ZST05URU5EX0xBQkVMLCBUUkFESU5HX0FTU0lTVEFOVF9OQU1FU1BBQ0V9IGZyb20gXCIuLi9jb25zdGFudHNcIjtcbmltcG9ydCAqIGFzIGt1YmVybmV0ZXMgZnJvbSBcIkBjZGt0Zi9wcm92aWRlci1rdWJlcm5ldGVzXCI7XG5cbmV4cG9ydCBjbGFzcyBUcmFkaW5nQXNzaXN0YW50U3RhdGVsZXNzU3RhY2sgZXh0ZW5kcyBUZXJyYWZvcm1TdGFjayB7XG4gICAgY29uc3RydWN0b3Ioc2NvcGU6IENvbnN0cnVjdCwgbmFtZTogc3RyaW5nKSB7XG4gICAgICAgIHN1cGVyKHNjb3BlLCBuYW1lKTtcblxuXG4gICAgICAgIG5ldyBLdWJlcm5ldGVzUHJvdmlkZXIodGhpcywgJ0s4cycsIHtcbiAgICAgICAgICAgIFwiY29uZmlnUGF0aFwiOiBcIn4vLmt1YmUvY29uZmlnXCIsXG4gICAgICAgICAgICBcImNvbmZpZ0NvbnRleHRcIjogXCJrdWJlcm5ldGVzLWFkbWluQGt1YmVybmV0ZXNcIlxuICAgICAgICB9KTtcbiAgICAgIC8vICB0aGlzLmNyZWF0ZVRyYWRpbmdBc3Npc3RhbnRGcm9udGVuZEluZ3Jlc3MoKTtcblxuICAgICAgICBsZXQgYWRtaW5QYXNzd29yZCA9IHRoaXMuY3JlYXRlREJUZXJyYWZvcm1TZWNyZXQoKTtcbiAgICAgICAgdGhpcy5jcmVhdGVUcmFkaW5nQXNzaXN0YW50RGVwbG95bWVudCh0aGlzLmNyZWF0ZVNsYWNrU2VjcmV0KCksIHRoaXMuY3JlYXRlU3Vtb0xvZ2ljU2VjcmV0KCksIGFkbWluUGFzc3dvcmQpO1xuICAgICAgICB0aGlzLmNyZWF0ZVRyYWRpbmdBc3Npc3RhbnRGcm9udGVuZERlcGxveW1lbnQoKTtcbiAgICAgICAgdGhpcy5jcmVhdGVUcmFkaW5nQXNzaXN0YW50U2VydmljZSgpO1xuICAgICAgICB0aGlzLmNyZWF0ZVRyYWRpbmdBc3Npc3RhbnRGcm9udGVuZFNlcnZpY2UoKTtcbiAgICAgICAgdGhpcy5jcmVhdGVNeXNxbERlcGxveW1lbnQoYWRtaW5QYXNzd29yZCk7XG4gICAgICAgIHRoaXMuY3JlYXRlTXlTcWxTZXJ2aWNlKCk7XG4gICAgfVxuXG4gICAgLy8gcHJpdmF0ZSBjcmVhdGVUcmFkaW5nQXNzaXN0YW50RnJvbnRlbmRJbmdyZXNzKCkge1xuICAgIC8vICAgICBuZXcga3ViZXJuZXRlcy5tYW5pZmVzdC5NYW5pZmVzdCh0aGlzLCBcInRyYWRpbmctYXNzaXN0YW50LWZyb250ZW5kLWluZ3Jlc3NcIiwge1xuICAgIC8vICAgICAgICAgbWFuaWZlc3Q6IHtcbiAgICAvLyAgICAgICAgICAgICBhcGlWZXJzaW9uOiBcIm5ldHdvcmtpbmcuazhzLmlvL3YxXCIsXG4gICAgLy8gICAgICAgICAgICAga2luZDogXCJJbmdyZXNzXCIsXG4gICAgLy8gICAgICAgICAgICAgbWV0YWRhdGE6IHtcbiAgICAvLyAgICAgICAgICAgICAgICAgbmFtZTogXCJ0cmFkaW5nLWFzc2lzdGFudC1pbmdyZXNzXCIsXG4gICAgLy8gICAgICAgICAgICAgICAgIG5hbWVzcGFjZTogXCJ0cmFkaW5nLWFzc2lzdGFudFwiLFxuICAgIC8vICAgICAgICAgICAgICAgICBsYWJlbHM6IHtcbiAgICAvLyAgICAgICAgICAgICAgICAgICAgIGFwcDogVFJBRElOR19BU1NJU1RBTlRfTEFCRUwgKyBcIi1mcm9udGVuZFwiXG4gICAgLy8gICAgICAgICAgICAgICAgIH0sXG4gICAgLy8gICAgICAgICAgICAgfSxcbiAgICAvLyAgICAgICAgICAgICBzcGVjOiB7XG4gICAgLy8gICAgICAgICAgICAgICAgIGluZ3Jlc3NDbGFzc05hbWU6IFwibmdpbnhcIixcbiAgICAvLyAgICAgICAgICAgICAgICAgcnVsZXM6IFtcbiAgICAvLyAgICAgICAgICAgICAgICAgICAgIHtcbiAgICAvLyAgICAgICAgICAgICAgICAgICAgICAgICBob3N0OiBcInRyYWRpbmctYXNzaXN0YW50Lm1vY2hpLXRyYWRpbmcuY29tXCIsXG4gICAgLy8gICAgICAgICAgICAgICAgICAgICAgICAgaHR0cDoge1xuICAgIC8vICAgICAgICAgICAgICAgICAgICAgICAgICAgICBwYXRoczogW1xuICAgIC8vICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAge1xuICAgIC8vICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIHBhdGg6IFwiL1wiLFxuICAgIC8vICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIHBhdGhUeXBlOiBcIlByZWZpeFwiLFxuICAgIC8vICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIGJhY2tlbmQ6IHtcbiAgICAvLyAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgc2VydmljZToge1xuICAgIC8vICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgbmFtZTogXCJ0cmFkaW5nLWFzc2lzdGFudC1mcm9udGVuZC1zZXJ2aWNlXCIsXG4gICAgLy8gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICBwb3J0OiB7XG4gICAgLy8gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgbnVtYmVyOiAzMDAwLFxuICAgIC8vICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgfSxcbiAgICAvLyAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIHNlbGVjdG9yOiB7XG4gICAgLy8gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgYXBwOiBUUkFESU5HX0FTU0lTVEFOVF9MQUJFTCArIFwiLWZyb250ZW5kXCIsXG4gICAgLy8gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICB9LFxuICAgIC8vICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICB9LFxuICAgIC8vICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIH0sXG4gICAgLy8gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICB9LFxuICAgIC8vICAgICAgICAgICAgICAgICAgICAgICAgICAgICBdLFxuICAgIC8vICAgICAgICAgICAgICAgICAgICAgICAgIH0sXG4gICAgLy8gICAgICAgICAgICAgICAgICAgICB9LFxuICAgIC8vICAgICAgICAgICAgICAgICBdLFxuICAgIC8vICAgICAgICAgICAgIH0sXG4gICAgLy8gICAgICAgICB9LFxuICAgIC8vICAgICB9KTtcbiAgICAvLyB9XG5cbiAgICBwcml2YXRlIGNyZWF0ZU15U3FsU2VydmljZSgpIHtcbiAgICAgICAgbmV3IGt1YmVybmV0ZXMuc2VydmljZS5TZXJ2aWNlKHRoaXMsIFwibXlzcWwtc2VydmljZVwiLCB7XG4gICAgICAgICAgICBtZXRhZGF0YToge1xuICAgICAgICAgICAgICAgIGxhYmVsczoge1xuICAgICAgICAgICAgICAgICAgICBhcHA6IE1ZU1FMX0xBQkVMLFxuICAgICAgICAgICAgICAgIH0sXG4gICAgICAgICAgICAgICAgbmFtZTogJ215c3FsLXNlcnZpY2UnLFxuICAgICAgICAgICAgICAgIG5hbWVzcGFjZTogVFJBRElOR19BU1NJU1RBTlRfTkFNRVNQQUNFLFxuICAgICAgICAgICAgfSxcbiAgICAgICAgICAgIHNwZWM6IHtcbiAgICAgICAgICAgICAgICBwb3J0OiBbe1xuICAgICAgICAgICAgICAgICAgICBwb3J0OiAzMzA2LFxuICAgICAgICAgICAgICAgICAgICB0YXJnZXRQb3J0OiBcIjMzMDZcIixcbiAgICAgICAgICAgICAgICB9XSxcbiAgICAgICAgICAgICAgICBzZWxlY3Rvcjoge1xuICAgICAgICAgICAgICAgICAgICBhcHA6IE1ZU1FMX0xBQkVMLFxuICAgICAgICAgICAgICAgIH0sXG4gICAgICAgICAgICAgICAgdHlwZTogJ05vZGVQb3J0JyxcbiAgICAgICAgICAgIH0sXG4gICAgICAgIH0pO1xuICAgIH1cblxuICAgIHByaXZhdGUgY3JlYXRlTXlzcWxEZXBsb3ltZW50KGFkbWluUGFzc3dvcmQ6IFRlcnJhZm9ybVZhcmlhYmxlKSB7XG4gICAgICAgIG5ldyBrdWJlcm5ldGVzLmRlcGxveW1lbnQuRGVwbG95bWVudCh0aGlzLCBNWVNRTF9MQUJFTCwge1xuICAgICAgICAgICAgbWV0YWRhdGE6IHtcbiAgICAgICAgICAgICAgICBsYWJlbHM6IHtcbiAgICAgICAgICAgICAgICAgICAgYXBwOiBNWVNRTF9MQUJFTCxcbiAgICAgICAgICAgICAgICB9LFxuICAgICAgICAgICAgICAgIG5hbWU6ICdteXNxbC1jb250YWluZXInLFxuICAgICAgICAgICAgICAgIG5hbWVzcGFjZTogVFJBRElOR19BU1NJU1RBTlRfTkFNRVNQQUNFLFxuICAgICAgICAgICAgfSxcbiAgICAgICAgICAgIHNwZWM6IHtcbiAgICAgICAgICAgICAgICByZXBsaWNhczogJzEnLFxuICAgICAgICAgICAgICAgIHNlbGVjdG9yOiB7XG4gICAgICAgICAgICAgICAgICAgIG1hdGNoTGFiZWxzOiB7XG4gICAgICAgICAgICAgICAgICAgICAgICBhcHA6IE1ZU1FMX0xBQkVMLFxuICAgICAgICAgICAgICAgICAgICB9LFxuICAgICAgICAgICAgICAgIH0sXG4gICAgICAgICAgICAgICAgdGVtcGxhdGU6IHtcbiAgICAgICAgICAgICAgICAgICAgbWV0YWRhdGE6IHtcbiAgICAgICAgICAgICAgICAgICAgICAgIGxhYmVsczoge1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgIGFwcDogTVlTUUxfTEFCRUwsXG4gICAgICAgICAgICAgICAgICAgICAgICB9LFxuICAgICAgICAgICAgICAgICAgICB9LFxuICAgICAgICAgICAgICAgICAgICBzcGVjOiB7XG4gICAgICAgICAgICAgICAgICAgICAgICBjb250YWluZXI6IFtcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICB7XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIGltYWdlOiAnbXlzcWw6bGF0ZXN0JyxcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgbmFtZTogTVlTUUxfTEFCRUwsXG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIHBvcnQ6IFt7XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICBjb250YWluZXJQb3J0OiAzMzA2LFxuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICB9XSxcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgZW52OiBbe1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgbmFtZTogJ01ZU1FMX1JPT1RfUEFTU1dPUkQnLFxuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgdmFsdWU6IGFkbWluUGFzc3dvcmQudmFsdWUsXG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIH1dLFxuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICB2b2x1bWVNb3VudDogW1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAge1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIG5hbWU6ICdteXNxbC1kYXRhJyxcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICBtb3VudFBhdGg6ICcvdmFyL2xpYi9teXNxbCcsXG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICB9XSxcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICB9LFxuICAgICAgICAgICAgICAgICAgICAgICAgXSxcbiAgICAgICAgICAgICAgICAgICAgICAgIHZvbHVtZTogW1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgIHtcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgbmFtZTogJ215c3FsLWRhdGEnLFxuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICBwZXJzaXN0ZW50Vm9sdW1lQ2xhaW06IHtcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIGNsYWltTmFtZTogJ215c3FsLXB2LWNsYWltJ1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICxcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICB9XVxuXG4gICAgICAgICAgICAgICAgICAgIH0sXG4gICAgICAgICAgICAgICAgfSxcbiAgICAgICAgICAgIH0sXG4gICAgICAgIH0pO1xuICAgIH1cblxuICAgIHByaXZhdGUgY3JlYXRlVHJhZGluZ0Fzc2lzdGFudFNlcnZpY2UoKSB7XG4gICAgICAgIG5ldyBrdWJlcm5ldGVzLnNlcnZpY2UuU2VydmljZSh0aGlzLCBcInRyYWRpbmctYXNzaXN0YW50LXNlcnZpY2VcIiwge1xuICAgICAgICAgICAgbWV0YWRhdGE6IHtcbiAgICAgICAgICAgICAgICBsYWJlbHM6IHtcbiAgICAgICAgICAgICAgICAgICAgYXBwOiBUUkFESU5HX0FTU0lTVEFOVF9MQUJFTCxcbiAgICAgICAgICAgICAgICB9LFxuICAgICAgICAgICAgICAgIG5hbWU6ICd0cmFkaW5nLWFzc2lzdGFudC1zZXJ2aWNlJyxcbiAgICAgICAgICAgICAgICBuYW1lc3BhY2U6IFRSQURJTkdfQVNTSVNUQU5UX05BTUVTUEFDRSxcbiAgICAgICAgICAgIH0sXG4gICAgICAgICAgICBzcGVjOiB7XG4gICAgICAgICAgICAgICAgcG9ydDogW1xuICAgICAgICAgICAgICAgICAgICB7XG4gICAgICAgICAgICAgICAgICAgICAgICBwb3J0OiA4MDgwLFxuICAgICAgICAgICAgICAgICAgICAgICAgdGFyZ2V0UG9ydDogXCI4MDgwXCIsXG4gICAgICAgICAgICAgICAgICAgIH1cblxuICAgICAgICAgICAgICAgIF0sXG5cbiAgICAgICAgICAgICAgICBzZWxlY3Rvcjoge1xuICAgICAgICAgICAgICAgICAgICBhcHA6IFRSQURJTkdfQVNTSVNUQU5UX0xBQkVMLFxuICAgICAgICAgICAgICAgIH0sXG4gICAgICAgICAgICAgICAgdHlwZTogJ05vZGVQb3J0JyxcbiAgICAgICAgICAgIH0sXG4gICAgICAgIH0pO1xuICAgIH1cblxuICAgIHByaXZhdGUgY3JlYXRlVHJhZGluZ0Fzc2lzdGFudEZyb250ZW5kU2VydmljZSgpIHtcbiAgICAgICAgbmV3IGt1YmVybmV0ZXMuc2VydmljZS5TZXJ2aWNlKHRoaXMsIFwidHJhZGluZy1hc3Npc3RhbnQtZnJvbnRlbmQtc2VydmljZVwiLCB7XG4gICAgICAgICAgICBtZXRhZGF0YToge1xuICAgICAgICAgICAgICAgIGxhYmVsczoge1xuICAgICAgICAgICAgICAgICAgICBhcHA6IFRSQURJTkdfQVNTSVNUQU5UX0ZST05URU5EX0xBQkVMLFxuICAgICAgICAgICAgICAgIH0sXG4gICAgICAgICAgICAgICAgbmFtZTogJ3RyYWRpbmctYXNzaXN0YW50LWZyb250ZW5kLXNlcnZpY2UnLFxuICAgICAgICAgICAgICAgIG5hbWVzcGFjZTogVFJBRElOR19BU1NJU1RBTlRfTkFNRVNQQUNFLFxuICAgICAgICAgICAgfSxcbiAgICAgICAgICAgIHNwZWM6IHtcbiAgICAgICAgICAgICAgICBwb3J0OiBbXG4gICAgICAgICAgICAgICAgICAgIHtcbiAgICAgICAgICAgICAgICAgICAgICAgIG5hbWU6IFwiaHR0cFwiLFxuICAgICAgICAgICAgICAgICAgICAgICAgcG9ydDogODAsXG4gICAgICAgICAgICAgICAgICAgICAgICB0YXJnZXRQb3J0OiBcIjMwMDBcIixcbiAgICAgICAgICAgICAgICAgICAgfVxuXG4gICAgICAgICAgICAgICAgXSxcblxuICAgICAgICAgICAgICAgIHNlbGVjdG9yOiB7XG4gICAgICAgICAgICAgICAgICAgIGFwcDogVFJBRElOR19BU1NJU1RBTlRfRlJPTlRFTkRfTEFCRUwsXG4gICAgICAgICAgICAgICAgfSxcbiAgICAgICAgICAgICAgICB0eXBlOiAnTG9hZEJhbGFuY2VyJyxcbiAgICAgICAgICAgIH0sXG4gICAgICAgIH0pO1xuICAgIH1cblxuICAgIHByaXZhdGUgY3JlYXRlVHJhZGluZ0Fzc2lzdGFudEZyb250ZW5kRGVwbG95bWVudCgpIHtcbiAgICAgICAgbmV3IGt1YmVybmV0ZXMuZGVwbG95bWVudC5EZXBsb3ltZW50KHRoaXMsIFRSQURJTkdfQVNTSVNUQU5UX0ZST05URU5EX0xBQkVMLCB7XG4gICAgICAgICAgICBtZXRhZGF0YToge1xuICAgICAgICAgICAgICAgIGxhYmVsczoge1xuICAgICAgICAgICAgICAgICAgICBhcHA6IFRSQURJTkdfQVNTSVNUQU5UX0ZST05URU5EX0xBQkVMLFxuICAgICAgICAgICAgICAgIH0sXG4gICAgICAgICAgICAgICAgbmFtZTogVFJBRElOR19BU1NJU1RBTlRfRlJPTlRFTkRfTEFCRUwsXG4gICAgICAgICAgICAgICAgbmFtZXNwYWNlOiBUUkFESU5HX0FTU0lTVEFOVF9OQU1FU1BBQ0UsXG4gICAgICAgICAgICB9LFxuICAgICAgICAgICAgc3BlYzoge1xuICAgICAgICAgICAgICAgIHJlcGxpY2FzOiAnMicsXG4gICAgICAgICAgICAgICAgc2VsZWN0b3I6IHtcbiAgICAgICAgICAgICAgICAgICAgbWF0Y2hMYWJlbHM6IHtcbiAgICAgICAgICAgICAgICAgICAgICAgIGFwcDogVFJBRElOR19BU1NJU1RBTlRfRlJPTlRFTkRfTEFCRUwsXG4gICAgICAgICAgICAgICAgICAgIH0sXG4gICAgICAgICAgICAgICAgfSxcbiAgICAgICAgICAgICAgICB0ZW1wbGF0ZToge1xuICAgICAgICAgICAgICAgICAgICBtZXRhZGF0YToge1xuICAgICAgICAgICAgICAgICAgICAgICAgbGFiZWxzOiB7XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgYXBwOiBUUkFESU5HX0FTU0lTVEFOVF9GUk9OVEVORF9MQUJFTCxcbiAgICAgICAgICAgICAgICAgICAgICAgIH0sXG4gICAgICAgICAgICAgICAgICAgIH0sXG4gICAgICAgICAgICAgICAgICAgIHNwZWM6IHtcbiAgICAgICAgICAgICAgICAgICAgICAgIGNvbnRhaW5lcjogW1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgIHtcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgaW1hZ2U6ICdnaGNyLmlvL3dpbGxodW1waHJleXMvdHJhZGluZy1hc3Npc3RhbnQ6ZnJvbnRlbmQtbGF0ZXN0JyxcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgaW1hZ2VQdWxsUG9saWN5OiAnQWx3YXlzJyxcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgbmFtZTogVFJBRElOR19BU1NJU1RBTlRfTEFCRUwsXG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIHBvcnQ6IFt7XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICBjb250YWluZXJQb3J0OiAzMDAwLFxuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICB9XSxcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICB9LFxuICAgICAgICAgICAgICAgICAgICAgICAgXVxuICAgICAgICAgICAgICAgICAgICB9LFxuICAgICAgICAgICAgICAgIH0sXG4gICAgICAgICAgICB9LFxuICAgICAgICB9KTtcbiAgICB9XG5cbiAgICBwcml2YXRlIGNyZWF0ZVRyYWRpbmdBc3Npc3RhbnREZXBsb3ltZW50KHNsYWNrVGVycmFmb3JtVmFyaWFibGU6IFRlcnJhZm9ybVZhcmlhYmxlLCBzdW1vTG9naWNUZXJyYWZvcm1WYXJpYWJsZTogVGVycmFmb3JtVmFyaWFibGUsIGRiUGFzc3dvcmRUZXJyYWZvcm1WYXJpYWJsZTogVGVycmFmb3JtVmFyaWFibGUpIHtcbiAgICAgICAgbmV3IGt1YmVybmV0ZXMuZGVwbG95bWVudC5EZXBsb3ltZW50KHRoaXMsIFRSQURJTkdfQVNTSVNUQU5UX0xBQkVMLCB7XG4gICAgICAgICAgICBtZXRhZGF0YToge1xuICAgICAgICAgICAgICAgIGxhYmVsczoge1xuICAgICAgICAgICAgICAgICAgICBhcHA6IFRSQURJTkdfQVNTSVNUQU5UX0xBQkVMLFxuICAgICAgICAgICAgICAgIH0sXG4gICAgICAgICAgICAgICAgbmFtZTogVFJBRElOR19BU1NJU1RBTlRfTEFCRUwsXG4gICAgICAgICAgICAgICAgbmFtZXNwYWNlOiBUUkFESU5HX0FTU0lTVEFOVF9OQU1FU1BBQ0UsXG4gICAgICAgICAgICB9LFxuICAgICAgICAgICAgc3BlYzoge1xuICAgICAgICAgICAgICAgIHJlcGxpY2FzOiAnMScsXG4gICAgICAgICAgICAgICAgc2VsZWN0b3I6IHtcbiAgICAgICAgICAgICAgICAgICAgbWF0Y2hMYWJlbHM6IHtcbiAgICAgICAgICAgICAgICAgICAgICAgIGFwcDogVFJBRElOR19BU1NJU1RBTlRfTEFCRUwsXG4gICAgICAgICAgICAgICAgICAgIH0sXG4gICAgICAgICAgICAgICAgfSxcbiAgICAgICAgICAgICAgICB0ZW1wbGF0ZToge1xuICAgICAgICAgICAgICAgICAgICBtZXRhZGF0YToge1xuICAgICAgICAgICAgICAgICAgICAgICAgbGFiZWxzOiB7XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgYXBwOiBUUkFESU5HX0FTU0lTVEFOVF9MQUJFTCxcbiAgICAgICAgICAgICAgICAgICAgICAgIH0sXG4gICAgICAgICAgICAgICAgICAgIH0sXG4gICAgICAgICAgICAgICAgICAgIHNwZWM6IHtcbiAgICAgICAgICAgICAgICAgICAgICAgIGNvbnRhaW5lcjogW1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgIHtcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgaW1hZ2U6ICdnaGNyLmlvL3dpbGxodW1waHJleXMvdHJhZGluZy1hc3Npc3RhbnQ6YmFja2VuZC1sYXRlc3QnLFxuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICBpbWFnZVB1bGxQb2xpY3k6ICdBbHdheXMnLFxuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICBuYW1lOiBUUkFESU5HX0FTU0lTVEFOVF9MQUJFTCxcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgcG9ydDogW3tcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIGNvbnRhaW5lclBvcnQ6IDgwODAsXG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIH1dLFxuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICBlbnY6IFt7XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICBuYW1lOiAnU1BSSU5HX1BST0ZJTEUnLFxuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgdmFsdWU6ICdjdXJyZW5jaWVzJyxcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgfSwge1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgbmFtZTogJ0RBVEFCQVNFX1VSTCcsXG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICB2YWx1ZTogJ2pkYmM6bXlzcWw6Ly9teXNxbC1zZXJ2aWNlOjMzMDYvbWV0YXRyYWRlcicsXG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIH0sIHtcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIG5hbWU6ICdEQVRBQkFTRV9QQVNTV09SRCcsXG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICB2YWx1ZTogZGJQYXNzd29yZFRlcnJhZm9ybVZhcmlhYmxlLnZhbHVlLFxuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICB9LCB7XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICBuYW1lOiAnU0xBQ0tfV0VCSE9PS19VUkwnLFxuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgdmFsdWU6IHNsYWNrVGVycmFmb3JtVmFyaWFibGUudmFsdWUsXG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIH0sIHtcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIG5hbWU6ICdTVU1PX0xPR0lDX1dFQkhPT0tfVVJMJyxcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIHZhbHVlOiBzdW1vTG9naWNUZXJyYWZvcm1WYXJpYWJsZS52YWx1ZSxcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICBdLFxuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICB2b2x1bWVNb3VudDogW1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAge1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIG5hbWU6ICdhY2NvdW50cy12b2x1bWUnLFxuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIG1vdW50UGF0aDogJy9hY2NvdW50cycsXG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICB9LFxuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAge1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIG5hbWU6ICdtb2NoaS1ncmFwaHMtdm9sdW1lJyxcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICBtb3VudFBhdGg6ICcvbW9jaGktZ3JhcGhzJyxcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIH0sXG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICB7XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgbmFtZTogJ210LXZvbHVtZScsXG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgbW91bnRQYXRoOiAnL210JyxcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIH1dXG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgfSxcbiAgICAgICAgICAgICAgICAgICAgICAgIF0sXG4gICAgICAgICAgICAgICAgICAgICAgICB2b2x1bWU6IFtcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICB7XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIG5hbWU6ICdhY2NvdW50cy12b2x1bWUnLFxuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICBob3N0UGF0aDoge1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgcGF0aDogJy9ob21lL3dpbGwvYWNjb3VudHMnLFxuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICB9LFxuICAgICAgICAgICAgICAgICAgICAgICAgICAgIH0sXG4gICAgICAgICAgICAgICAgICAgICAgICAgICAge1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICBuYW1lOiAnbW9jaGktZ3JhcGhzLXZvbHVtZScsXG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIGhvc3RQYXRoOiB7XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICBwYXRoOiAnL2hvbWUvd2lsbC9tb2NoaS1ncmFwaHMnLFxuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICB9LFxuICAgICAgICAgICAgICAgICAgICAgICAgICAgIH0sXG4gICAgICAgICAgICAgICAgICAgICAgICAgICAge1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICBuYW1lOiAnbXQtdm9sdW1lJyxcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgaG9zdFBhdGg6IHtcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIHBhdGg6ICcvaG9tZS93aWxsLy5jeG9mZmljZS9NZXRhVHJhZGVyXzUvZHJpdmVfYy9Qcm9ncmFtIEZpbGVzL01ldGFUcmFkZXIgNS9NUUw1L0ZpbGVzJyxcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgfSxcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICB9LFxuICAgICAgICAgICAgICAgICAgICAgICAgXSxcbiAgICAgICAgICAgICAgICAgICAgfSxcbiAgICAgICAgICAgICAgICB9LFxuICAgICAgICAgICAgfSxcbiAgICAgICAgfSk7XG4gICAgfVxuXG4gICAgcHJpdmF0ZSBjcmVhdGVEQlRlcnJhZm9ybVNlY3JldCgpIHtcblxuICAgICAgICByZXR1cm4gbmV3IFRlcnJhZm9ybVZhcmlhYmxlKHRoaXMsIFwiZGJQYXNzd29yZFwiLCB7XG4gICAgICAgICAgICB0eXBlOiBcInN0cmluZ1wiLFxuICAgICAgICAgICAgZGVzY3JpcHRpb246IFwicm9vdCBwYXNzd29yZCBmb3IgbXlzcWxcIixcbiAgICAgICAgICAgIHNlbnNpdGl2ZTogdHJ1ZSxcbiAgICAgICAgfSk7XG4gICAgfVxuXG4gICAgcHJpdmF0ZSBjcmVhdGVTbGFja1NlY3JldCgpIHtcblxuICAgICAgICByZXR1cm4gbmV3IFRlcnJhZm9ybVZhcmlhYmxlKHRoaXMsIFwic2xhY2tXZWJIb29rXCIsIHtcbiAgICAgICAgICAgIHR5cGU6IFwic3RyaW5nXCIsXG4gICAgICAgICAgICBkZXNjcmlwdGlvbjogXCJzbGFjayB3ZWJob29rIHVybFwiLFxuICAgICAgICAgICAgc2Vuc2l0aXZlOiB0cnVlLFxuICAgICAgICB9KTtcbiAgICB9XG5cbiAgICBwcml2YXRlIGNyZWF0ZVN1bW9Mb2dpY1NlY3JldCgpIHtcblxuICAgICAgICByZXR1cm4gbmV3IFRlcnJhZm9ybVZhcmlhYmxlKHRoaXMsIFwic3Vtb0xvZ2ljV2ViSG9va1wiLCB7XG4gICAgICAgICAgICB0eXBlOiBcInN0cmluZ1wiLFxuICAgICAgICAgICAgZGVzY3JpcHRpb246IFwic3VtbyBsb2dpYyB3ZWJob29rIHVybFwiLFxuICAgICAgICAgICAgc2Vuc2l0aXZlOiB0cnVlLFxuICAgICAgICB9KTtcbiAgICB9XG5cbiAgICAvLyBwcml2YXRlIGNyZWF0ZUhvbWVWYXJpYWJsZSgpIHtcbiAgICAvL1xuICAgIC8vICAgICByZXR1cm4gbmV3IFRlcnJhZm9ybVZhcmlhYmxlKHRoaXMsIFwia3ViZUhvbWVcIiwge1xuICAgIC8vICAgICAgICAgdHlwZTogXCJzdHJpbmdcIixcbiAgICAvLyAgICAgICAgIGRlc2NyaXB0aW9uOiBcImt1YmUgaG9tZSBkaXJlY3RvcnlcIixcbiAgICAvLyAgICAgICAgIHNlbnNpdGl2ZTogZmFsc2UsXG4gICAgLy8gICAgIH0pO1xuICAgIC8vIH1cbn1cbiJdfQ==