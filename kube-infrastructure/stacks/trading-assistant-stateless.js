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
                    app: constants_1.TRADING_ASSISTANT_LABEL + '-frontend',
                },
                name: 'trading-assistant-frontend-service',
                namespace: 'trading-assistant',
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
                    app: constants_1.TRADING_ASSISTANT_LABEL + '-frontend',
                },
                type: 'LoadBalancer',
            },
        });
    }
    createTradingAssistantFrontendDeployment() {
        new kubernetes.deployment.Deployment(this, constants_1.TRADING_ASSISTANT_LABEL + '-frontend', {
            metadata: {
                labels: {
                    app: constants_1.TRADING_ASSISTANT_LABEL + '-frontend',
                },
                name: constants_1.TRADING_ASSISTANT_LABEL + '-frontend',
                namespace: 'trading-assistant',
            },
            spec: {
                replicas: '1',
                selector: {
                    matchLabels: {
                        app: constants_1.TRADING_ASSISTANT_LABEL + '-frontend',
                    },
                },
                template: {
                    metadata: {
                        labels: {
                            app: constants_1.TRADING_ASSISTANT_LABEL + '-frontend',
                        },
                    },
                    spec: {
                        container: [
                            {
                                image: 'ghcr.io/willhumphreys/trading-assistant:frontend-bba5e55299454b008cbdfca7cddeb02c52e2a915',
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
//# sourceMappingURL=data:application/json;base64,eyJ2ZXJzaW9uIjozLCJmaWxlIjoidHJhZGluZy1hc3Npc3RhbnQtc3RhdGVsZXNzLmpzIiwic291cmNlUm9vdCI6IiIsInNvdXJjZXMiOlsidHJhZGluZy1hc3Npc3RhbnQtc3RhdGVsZXNzLnRzIl0sIm5hbWVzIjpbXSwibWFwcGluZ3MiOiI7QUFBQSxnQ0FBZ0M7OztBQUdoQyxpQ0FBd0Q7QUFDeEQsc0VBQTJFO0FBQzNFLDRDQUFrRTtBQUNsRSx5REFBeUQ7QUFFekQsTUFBYSw4QkFBK0IsU0FBUSxzQkFBYztJQUM5RCxZQUFZLEtBQWdCLEVBQUUsSUFBWTtRQUN0QyxLQUFLLENBQUMsS0FBSyxFQUFFLElBQUksQ0FBQyxDQUFDO1FBR25CLElBQUksNkJBQWtCLENBQUMsSUFBSSxFQUFFLEtBQUssRUFBRTtZQUNoQyxZQUFZLEVBQUUsZ0JBQWdCO1lBQzlCLGVBQWUsRUFBRSw2QkFBNkI7U0FDakQsQ0FBQyxDQUFDO1FBQ0wsaURBQWlEO1FBRS9DLElBQUksYUFBYSxHQUFHLElBQUksQ0FBQyx1QkFBdUIsRUFBRSxDQUFDO1FBQ25ELElBQUksQ0FBQyxnQ0FBZ0MsQ0FBQyxJQUFJLENBQUMsaUJBQWlCLEVBQUUsRUFBRSxJQUFJLENBQUMscUJBQXFCLEVBQUUsRUFBRSxhQUFhLENBQUMsQ0FBQztRQUM3RyxJQUFJLENBQUMsd0NBQXdDLEVBQUUsQ0FBQztRQUNoRCxJQUFJLENBQUMsNkJBQTZCLEVBQUUsQ0FBQztRQUNyQyxJQUFJLENBQUMscUNBQXFDLEVBQUUsQ0FBQztRQUM3QyxJQUFJLENBQUMscUJBQXFCLENBQUMsYUFBYSxDQUFDLENBQUM7UUFDMUMsSUFBSSxDQUFDLGtCQUFrQixFQUFFLENBQUM7SUFDOUIsQ0FBQztJQUVELG9EQUFvRDtJQUNwRCxxRkFBcUY7SUFDckYsc0JBQXNCO0lBQ3RCLGtEQUFrRDtJQUNsRCwrQkFBK0I7SUFDL0IsMEJBQTBCO0lBQzFCLHFEQUFxRDtJQUNyRCxrREFBa0Q7SUFDbEQsNEJBQTRCO0lBQzVCLGlFQUFpRTtJQUNqRSxxQkFBcUI7SUFDckIsaUJB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Q04sSUFBSSxFQUFFLGlGQUFpRjtpQ0FDMUY7NkJBQ0o7eUJBQ0o7cUJBQ0o7aUJBQ0o7YUFDSjtTQUNKLENBQUMsQ0FBQztJQUNQLENBQUM7SUFFTyx1QkFBdUI7UUFFM0IsT0FBTyxJQUFJLHlCQUFpQixDQUFDLElBQUksRUFBRSxZQUFZLEVBQUU7WUFDN0MsSUFBSSxFQUFFLFFBQVE7WUFDZCxXQUFXLEVBQUUseUJBQXlCO1lBQ3RDLFNBQVMsRUFBRSxJQUFJO1NBQ2xCLENBQUMsQ0FBQztJQUNQLENBQUM7SUFFTyxpQkFBaUI7UUFFckIsT0FBTyxJQUFJLHlCQUFpQixDQUFDLElBQUksRUFBRSxjQUFjLEVBQUU7WUFDL0MsSUFBSSxFQUFFLFFBQVE7WUFDZCxXQUFXLEVBQUUsbUJBQW1CO1lBQ2hDLFNBQVMsRUFBRSxJQUFJO1NBQ2xCLENBQUMsQ0FBQztJQUNQLENBQUM7SUFFTyxxQkFBcUI7UUFFekIsT0FBTyxJQUFJLHlCQUFpQixDQUFDLElBQUksRUFBRSxrQkFBa0IsRUFBRTtZQUNuRCxJQUFJLEVBQUUsUUFBUTtZQUNkLFdBQVcsRUFBRSx3QkFBd0I7WUFDckMsU0FBUyxFQUFFLElBQUk7U0FDbEIsQ0FBQyxDQUFDO0lBQ1AsQ0FBQztDQVVKO0FBNVdELHdFQTRXQyIsInNvdXJjZXNDb250ZW50IjpbIi8vIHRyYWRpbmctYXNzaXN0YW50LXN0YXRlZnVsLnRzXG5cbmltcG9ydCB7Q29uc3RydWN0fSBmcm9tIFwiY29uc3RydWN0c1wiO1xuaW1wb3J0IHtUZXJyYWZvcm1TdGFjaywgVGVycmFmb3JtVmFyaWFibGV9IGZyb20gXCJjZGt0ZlwiO1xuaW1wb3J0IHtLdWJlcm5ldGVzUHJvdmlkZXJ9IGZyb20gXCJAY2RrdGYvcHJvdmlkZXIta3ViZXJuZXRlcy9saWIvcHJvdmlkZXJcIjtcbmltcG9ydCB7TVlTUUxfTEFCRUwsIFRSQURJTkdfQVNTSVNUQU5UX0xBQkVMfSBmcm9tIFwiLi4vY29uc3RhbnRzXCI7XG5pbXBvcnQgKiBhcyBrdWJlcm5ldGVzIGZyb20gXCJAY2RrdGYvcHJvdmlkZXIta3ViZXJuZXRlc1wiO1xuXG5leHBvcnQgY2xhc3MgVHJhZGluZ0Fzc2lzdGFudFN0YXRlbGVzc1N0YWNrIGV4dGVuZHMgVGVycmFmb3JtU3RhY2sge1xuICAgIGNvbnN0cnVjdG9yKHNjb3BlOiBDb25zdHJ1Y3QsIG5hbWU6IHN0cmluZykge1xuICAgICAgICBzdXBlcihzY29wZSwgbmFtZSk7XG5cblxuICAgICAgICBuZXcgS3ViZXJuZXRlc1Byb3ZpZGVyKHRoaXMsICdLOHMnLCB7XG4gICAgICAgICAgICBcImNvbmZpZ1BhdGhcIjogXCJ+Ly5rdWJlL2NvbmZpZ1wiLFxuICAgICAgICAgICAgXCJjb25maWdDb250ZXh0XCI6IFwia3ViZXJuZXRlcy1hZG1pbkBrdWJlcm5ldGVzXCJcbiAgICAgICAgfSk7XG4gICAgICAvLyAgdGhpcy5jcmVhdGVUcmFkaW5nQXNzaXN0YW50RnJvbnRlbmRJbmdyZXNzKCk7XG5cbiAgICAgICAgbGV0IGFkbWluUGFzc3dvcmQgPSB0aGlzLmNyZWF0ZURCVGVycmFmb3JtU2VjcmV0KCk7XG4gICAgICAgIHRoaXMuY3JlYXRlVHJhZGluZ0Fzc2lzdGFudERlcGxveW1lbnQodGhpcy5jcmVhdGVTbGFja1NlY3JldCgpLCB0aGlzLmNyZWF0ZVN1bW9Mb2dpY1NlY3JldCgpLCBhZG1pblBhc3N3b3JkKTtcbiAgICAgICAgdGhpcy5jcmVhdGVUcmFkaW5nQXNzaXN0YW50RnJvbnRlbmREZXBsb3ltZW50KCk7XG4gICAgICAgIHRoaXMuY3JlYXRlVHJhZGluZ0Fzc2lzdGFudFNlcnZpY2UoKTtcbiAgICAgICAgdGhpcy5jcmVhdGVUcmFkaW5nQXNzaXN0YW50RnJvbnRlbmRTZXJ2aWNlKCk7XG4gICAgICAgIHRoaXMuY3JlYXRlTXlzcWxEZXBsb3ltZW50KGFkbWluUGFzc3dvcmQpO1xuICAgICAgICB0aGlzLmNyZWF0ZU15U3FsU2VydmljZSgpO1xuICAgIH1cblxuICAgIC8vIHByaXZhdGUgY3JlYXRlVHJhZGluZ0Fzc2lzdGFudEZyb250ZW5kSW5ncmVzcygpIHtcbiAgICAvLyAgICAgbmV3IGt1YmVybmV0ZXMubWFuaWZlc3QuTWFuaWZlc3QodGhpcywgXCJ0cmFkaW5nLWFzc2lzdGFudC1mcm9udGVuZC1pbmdyZXNzXCIsIHtcbiAgICAvLyAgICAgICAgIG1hbmlmZXN0OiB7XG4gICAgLy8gICAgICAgICAgICAgYXBpVmVyc2lvbjogXCJuZXR3b3JraW5nLms4cy5pby92MVwiLFxuICAgIC8vICAgICAgICAgICAgIGtpbmQ6IFwiSW5ncmVzc1wiLFxuICAgIC8vICAgICAgICAgICAgIG1ldGFkYXRhOiB7XG4gICAgLy8gICAgICAgICAgICAgICAgIG5hbWU6IFwidHJhZGluZy1hc3Npc3RhbnQtaW5ncmVzc1wiLFxuICAgIC8vICAgICAgICAgICAgICAgICBuYW1lc3BhY2U6IFwidHJhZGluZy1hc3Npc3RhbnRcIixcbiAgICAvLyAgICAgICAgICAgICAgICAgbGFiZWxzOiB7XG4gICAgLy8gICAgICAgICAgICAgICAgICAgICBhcHA6IFRSQURJTkdfQVNTSVNUQU5UX0xBQkVMICsgXCItZnJvbnRlbmRcIlxuICAgIC8vICAgICAgICAgICAgICAgICB9LFxuICAgIC8vICAgICAgICAgICAgIH0sXG4gICAgLy8gICAgICAgICAgICAgc3BlYzoge1xuICAgIC8vICAgICAgICAgICAgICAgICBpbmdyZXNzQ2xhc3NOYW1lOiBcIm5naW54XCIsXG4gICAgLy8gICAgICAgICAgICAgICAgIHJ1bGVzOiBbXG4gICAgLy8gICAgICAgICAgICAgICAgICAgICB7XG4gICAgLy8gICAgICAgICAgICAgICAgICAgICAgICAgaG9zdDogXCJ0cmFkaW5nLWFzc2lzdGFudC5tb2NoaS10cmFkaW5nLmNvbVwiLFxuICAgIC8vICAgICAgICAgICAgICAgICAgICAgICAgIGh0dHA6IHtcbiAgICAvLyAgICAgICAgICAgICAgICAgICAgICAgICAgICAgcGF0aHM6IFtcbiAgICAvLyAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIHtcbiAgICAvLyAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICBwYXRoOiBcIi9cIixcbiAgICAvLyAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICBwYXRoVHlwZTogXCJQcmVmaXhcIixcbiAgICAvLyAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICBiYWNrZW5kOiB7XG4gICAgLy8gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIHNlcnZpY2U6IHtcbiAgICAvLyAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIG5hbWU6IFwidHJhZGluZy1hc3Npc3RhbnQtZnJvbnRlbmQtc2VydmljZVwiLFxuICAgIC8vICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgcG9ydDoge1xuICAgIC8vICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIG51bWJlcjogMzAwMCxcbiAgICAvLyAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIH0sXG4gICAgLy8gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICBzZWxlY3Rvcjoge1xuICAgIC8vICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIGFwcDogVFJBRElOR19BU1NJU1RBTlRfTEFCRUwgKyBcIi1mcm9udGVuZFwiLFxuICAgIC8vICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgfSxcbiAgICAvLyAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgfSxcbiAgICAvLyAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICB9LFxuICAgIC8vICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgfSxcbiAgICAvLyAgICAgICAgICAgICAgICAgICAgICAgICAgICAgXSxcbiAgICAvLyAgICAgICAgICAgICAgICAgICAgICAgICB9LFxuICAgIC8vICAgICAgICAgICAgICAgICAgICAgfSxcbiAgICAvLyAgICAgICAgICAgICAgICAgXSxcbiAgICAvLyAgICAgICAgICAgICB9LFxuICAgIC8vICAgICAgICAgfSxcbiAgICAvLyAgICAgfSk7XG4gICAgLy8gfVxuXG4gICAgcHJpdmF0ZSBjcmVhdGVNeVNxbFNlcnZpY2UoKSB7XG4gICAgICAgIG5ldyBrdWJlcm5ldGVzLnNlcnZpY2UuU2VydmljZSh0aGlzLCBcIm15c3FsLXNlcnZpY2VcIiwge1xuICAgICAgICAgICAgbWV0YWRhdGE6IHtcbiAgICAgICAgICAgICAgICBsYWJlbHM6IHtcbiAgICAgICAgICAgICAgICAgICAgYXBwOiBNWVNRTF9MQUJFTCxcbiAgICAgICAgICAgICAgICB9LFxuICAgICAgICAgICAgICAgIG5hbWU6ICdteXNxbC1zZXJ2aWNlJyxcbiAgICAgICAgICAgICAgICBuYW1lc3BhY2U6ICd0cmFkaW5nLWFzc2lzdGFudCcsXG4gICAgICAgICAgICB9LFxuICAgICAgICAgICAgc3BlYzoge1xuICAgICAgICAgICAgICAgIHBvcnQ6IFt7XG4gICAgICAgICAgICAgICAgICAgIHBvcnQ6IDMzMDYsXG4gICAgICAgICAgICAgICAgICAgIHRhcmdldFBvcnQ6IFwiMzMwNlwiLFxuICAgICAgICAgICAgICAgIH1dLFxuICAgICAgICAgICAgICAgIHNlbGVjdG9yOiB7XG4gICAgICAgICAgICAgICAgICAgIGFwcDogTVlTUUxfTEFCRUwsXG4gICAgICAgICAgICAgICAgfSxcbiAgICAgICAgICAgICAgICB0eXBlOiAnTm9kZVBvcnQnLFxuICAgICAgICAgICAgfSxcbiAgICAgICAgfSk7XG4gICAgfVxuXG4gICAgcHJpdmF0ZSBjcmVhdGVNeXNxbERlcGxveW1lbnQoYWRtaW5QYXNzd29yZDogVGVycmFmb3JtVmFyaWFibGUpIHtcbiAgICAgICAgbmV3IGt1YmVybmV0ZXMuZGVwbG95bWVudC5EZXBsb3ltZW50KHRoaXMsIE1ZU1FMX0xBQkVMLCB7XG4gICAgICAgICAgICBtZXRhZGF0YToge1xuICAgICAgICAgICAgICAgIGxhYmVsczoge1xuICAgICAgICAgICAgICAgICAgICBhcHA6IE1ZU1FMX0xBQkVMLFxuICAgICAgICAgICAgICAgIH0sXG4gICAgICAgICAgICAgICAgbmFtZTogJ215c3FsLWNvbnRhaW5lcicsXG4gICAgICAgICAgICAgICAgbmFtZXNwYWNlOiAndHJhZGluZy1hc3Npc3RhbnQnLFxuICAgICAgICAgICAgfSxcbiAgICAgICAgICAgIHNwZWM6IHtcbiAgICAgICAgICAgICAgICByZXBsaWNhczogJzEnLFxuICAgICAgICAgICAgICAgIHNlbGVjdG9yOiB7XG4gICAgICAgICAgICAgICAgICAgIG1hdGNoTGFiZWxzOiB7XG4gICAgICAgICAgICAgICAgICAgICAgICBhcHA6IE1ZU1FMX0xBQkVMLFxuICAgICAgICAgICAgICAgICAgICB9LFxuICAgICAgICAgICAgICAgIH0sXG4gICAgICAgICAgICAgICAgdGVtcGxhdGU6IHtcbiAgICAgICAgICAgICAgICAgICAgbWV0YWRhdGE6IHtcbiAgICAgICAgICAgICAgICAgICAgICAgIGxhYmVsczoge1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgIGFwcDogTVlTUUxfTEFCRUwsXG4gICAgICAgICAgICAgICAgICAgICAgICB9LFxuICAgICAgICAgICAgICAgICAgICB9LFxuICAgICAgICAgICAgICAgICAgICBzcGVjOiB7XG4gICAgICAgICAgICAgICAgICAgICAgICBjb250YWluZXI6IFtcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICB7XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIGltYWdlOiAnbXlzcWw6bGF0ZXN0JyxcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgbmFtZTogTVlTUUxfTEFCRUwsXG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIHBvcnQ6IFt7XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICBjb250YWluZXJQb3J0OiAzMzA2LFxuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICB9XSxcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgZW52OiBbe1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgbmFtZTogJ01ZU1FMX1JPT1RfUEFTU1dPUkQnLFxuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgdmFsdWU6IGFkbWluUGFzc3dvcmQudmFsdWUsXG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIH1dLFxuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICB2b2x1bWVNb3VudDogW1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAge1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIG5hbWU6ICdteXNxbC1kYXRhJyxcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICBtb3VudFBhdGg6ICcvdmFyL2xpYi9teXNxbCcsXG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICB9XSxcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICB9LFxuICAgICAgICAgICAgICAgICAgICAgICAgXSxcbiAgICAgICAgICAgICAgICAgICAgICAgIHZvbHVtZTogW1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgIHtcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgbmFtZTogJ215c3FsLWRhdGEnLFxuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICBwZXJzaXN0ZW50Vm9sdW1lQ2xhaW06IHtcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIGNsYWltTmFtZTogJ215c3FsLXB2LWNsYWltJ1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICxcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICB9XVxuXG4gICAgICAgICAgICAgICAgICAgIH0sXG4gICAgICAgICAgICAgICAgfSxcbiAgICAgICAgICAgIH0sXG4gICAgICAgIH0pO1xuICAgIH1cblxuICAgIHByaXZhdGUgY3JlYXRlVHJhZGluZ0Fzc2lzdGFudFNlcnZpY2UoKSB7XG4gICAgICAgIG5ldyBrdWJlcm5ldGVzLnNlcnZpY2UuU2VydmljZSh0aGlzLCBcInRyYWRpbmctYXNzaXN0YW50LXNlcnZpY2VcIiwge1xuICAgICAgICAgICAgbWV0YWRhdGE6IHtcbiAgICAgICAgICAgICAgICBsYWJlbHM6IHtcbiAgICAgICAgICAgICAgICAgICAgYXBwOiBUUkFESU5HX0FTU0lTVEFOVF9MQUJFTCxcbiAgICAgICAgICAgICAgICB9LFxuICAgICAgICAgICAgICAgIG5hbWU6ICd0cmFkaW5nLWFzc2lzdGFudC1zZXJ2aWNlJyxcbiAgICAgICAgICAgICAgICBuYW1lc3BhY2U6ICd0cmFkaW5nLWFzc2lzdGFudCcsXG4gICAgICAgICAgICB9LFxuICAgICAgICAgICAgc3BlYzoge1xuICAgICAgICAgICAgICAgIHBvcnQ6IFtcbiAgICAgICAgICAgICAgICAgICAge1xuICAgICAgICAgICAgICAgICAgICAgICAgcG9ydDogODA4MCxcbiAgICAgICAgICAgICAgICAgICAgICAgIHRhcmdldFBvcnQ6IFwiODA4MFwiLFxuICAgICAgICAgICAgICAgICAgICB9XG5cbiAgICAgICAgICAgICAgICBdLFxuXG4gICAgICAgICAgICAgICAgc2VsZWN0b3I6IHtcbiAgICAgICAgICAgICAgICAgICAgYXBwOiBUUkFESU5HX0FTU0lTVEFOVF9MQUJFTCxcbiAgICAgICAgICAgICAgICB9LFxuICAgICAgICAgICAgICAgIHR5cGU6ICdOb2RlUG9ydCcsXG4gICAgICAgICAgICB9LFxuICAgICAgICB9KTtcbiAgICB9XG5cbiAgICBwcml2YXRlIGNyZWF0ZVRyYWRpbmdBc3Npc3RhbnRGcm9udGVuZFNlcnZpY2UoKSB7XG4gICAgICAgIG5ldyBrdWJlcm5ldGVzLnNlcnZpY2UuU2VydmljZSh0aGlzLCBcInRyYWRpbmctYXNzaXN0YW50LWZyb250ZW5kLXNlcnZpY2VcIiwge1xuICAgICAgICAgICAgbWV0YWRhdGE6IHtcbiAgICAgICAgICAgICAgICBsYWJlbHM6IHtcbiAgICAgICAgICAgICAgICAgICAgYXBwOiBUUkFESU5HX0FTU0lTVEFOVF9MQUJFTCArICctZnJvbnRlbmQnLFxuICAgICAgICAgICAgICAgIH0sXG4gICAgICAgICAgICAgICAgbmFtZTogJ3RyYWRpbmctYXNzaXN0YW50LWZyb250ZW5kLXNlcnZpY2UnLFxuICAgICAgICAgICAgICAgIG5hbWVzcGFjZTogJ3RyYWRpbmctYXNzaXN0YW50JyxcbiAgICAgICAgICAgIH0sXG4gICAgICAgICAgICBzcGVjOiB7XG4gICAgICAgICAgICAgICAgcG9ydDogW1xuICAgICAgICAgICAgICAgICAgICB7XG4gICAgICAgICAgICAgICAgICAgICAgICBuYW1lOiBcImh0dHBcIixcbiAgICAgICAgICAgICAgICAgICAgICAgIHBvcnQ6IDgwLFxuICAgICAgICAgICAgICAgICAgICAgICAgdGFyZ2V0UG9ydDogXCIzMDAwXCIsXG4gICAgICAgICAgICAgICAgICAgIH1cblxuICAgICAgICAgICAgICAgIF0sXG5cbiAgICAgICAgICAgICAgICBzZWxlY3Rvcjoge1xuICAgICAgICAgICAgICAgICAgICBhcHA6IFRSQURJTkdfQVNTSVNUQU5UX0xBQkVMICsgJy1mcm9udGVuZCcsXG4gICAgICAgICAgICAgICAgfSxcbiAgICAgICAgICAgICAgICB0eXBlOiAnTG9hZEJhbGFuY2VyJyxcbiAgICAgICAgICAgIH0sXG4gICAgICAgIH0pO1xuICAgIH1cblxuICAgIHByaXZhdGUgY3JlYXRlVHJhZGluZ0Fzc2lzdGFudEZyb250ZW5kRGVwbG95bWVudCgpIHtcbiAgICAgICAgbmV3IGt1YmVybmV0ZXMuZGVwbG95bWVudC5EZXBsb3ltZW50KHRoaXMsIFRSQURJTkdfQVNTSVNUQU5UX0xBQkVMICsgJy1mcm9udGVuZCcsIHtcbiAgICAgICAgICAgIG1ldGFkYXRhOiB7XG4gICAgICAgICAgICAgICAgbGFiZWxzOiB7XG4gICAgICAgICAgICAgICAgICAgIGFwcDogVFJBRElOR19BU1NJU1RBTlRfTEFCRUwgKyAnLWZyb250ZW5kJyxcbiAgICAgICAgICAgICAgICB9LFxuICAgICAgICAgICAgICAgIG5hbWU6IFRSQURJTkdfQVNTSVNUQU5UX0xBQkVMICsgJy1mcm9udGVuZCcsXG4gICAgICAgICAgICAgICAgbmFtZXNwYWNlOiAndHJhZGluZy1hc3Npc3RhbnQnLFxuICAgICAgICAgICAgfSxcbiAgICAgICAgICAgIHNwZWM6IHtcbiAgICAgICAgICAgICAgICByZXBsaWNhczogJzEnLFxuICAgICAgICAgICAgICAgIHNlbGVjdG9yOiB7XG4gICAgICAgICAgICAgICAgICAgIG1hdGNoTGFiZWxzOiB7XG4gICAgICAgICAgICAgICAgICAgICAgICBhcHA6IFRSQURJTkdfQVNTSVNUQU5UX0xBQkVMICsgJy1mcm9udGVuZCcsXG4gICAgICAgICAgICAgICAgICAgIH0sXG4gICAgICAgICAgICAgICAgfSxcbiAgICAgICAgICAgICAgICB0ZW1wbGF0ZToge1xuICAgICAgICAgICAgICAgICAgICBtZXRhZGF0YToge1xuICAgICAgICAgICAgICAgICAgICAgICAgbGFiZWxzOiB7XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgYXBwOiBUUkFESU5HX0FTU0lTVEFOVF9MQUJFTCArICctZnJvbnRlbmQnLFxuICAgICAgICAgICAgICAgICAgICAgICAgfSxcbiAgICAgICAgICAgICAgICAgICAgfSxcbiAgICAgICAgICAgICAgICAgICAgc3BlYzoge1xuICAgICAgICAgICAgICAgICAgICAgICAgY29udGFpbmVyOiBbXG4gICAgICAgICAgICAgICAgICAgICAgICAgICAge1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICBpbWFnZTogJ2doY3IuaW8vd2lsbGh1bXBocmV5cy90cmFkaW5nLWFzc2lzdGFudDpmcm9udGVuZC1iYmE1ZTU1Mjk5NDU0YjAwOGNiZGZjYTdjZGRlYjAyYzUyZTJhOTE1JyxcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgbmFtZTogVFJBRElOR19BU1NJU1RBTlRfTEFCRUwsXG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIHBvcnQ6IFt7XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICBjb250YWluZXJQb3J0OiAzMDAwLFxuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICB9XSxcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgZW52OiBbe1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgbmFtZTogJ05FWFRfUFVCTElDX0JBQ0tFTkRfSE9TVCcsXG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICB2YWx1ZTogJ2h0dHA6Ly90cmFkaW5nLWFzc2lzdGFudC1zZXJ2aWNlOjgwODAnLFxuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICB9LCB7XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICBuYW1lOiAnTkVYVF9QVUJMSUNfV0VCX1NPQ0tFVF9IT1NUJyxcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIHZhbHVlOiAnd3M6Ly90cmFkaW5nLWFzc2lzdGFudC1zZXJ2aWNlOjgwODAnLFxuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIF1cbiAgICAgICAgICAgICAgICAgICAgICAgICAgICB9LFxuICAgICAgICAgICAgICAgICAgICAgICAgXVxuICAgICAgICAgICAgICAgICAgICB9LFxuICAgICAgICAgICAgICAgIH0sXG4gICAgICAgICAgICB9LFxuICAgICAgICB9KTtcbiAgICB9XG5cblxuICAgIHByaXZhdGUgY3JlYXRlVHJhZGluZ0Fzc2lzdGFudERlcGxveW1lbnQoc2xhY2tUZXJyYWZvcm1WYXJpYWJsZTogVGVycmFmb3JtVmFyaWFibGUsIHN1bW9Mb2dpY1RlcnJhZm9ybVZhcmlhYmxlOiBUZXJyYWZvcm1WYXJpYWJsZSwgZGJQYXNzd29yZFRlcnJhZm9ybVZhcmlhYmxlOiBUZXJyYWZvcm1WYXJpYWJsZSkge1xuICAgICAgICBuZXcga3ViZXJuZXRlcy5kZXBsb3ltZW50LkRlcGxveW1lbnQodGhpcywgVFJBRElOR19BU1NJU1RBTlRfTEFCRUwsIHtcbiAgICAgICAgICAgIG1ldGFkYXRhOiB7XG4gICAgICAgICAgICAgICAgbGFiZWxzOiB7XG4gICAgICAgICAgICAgICAgICAgIGFwcDogVFJBRElOR19BU1NJU1RBTlRfTEFCRUwsXG4gICAgICAgICAgICAgICAgfSxcbiAgICAgICAgICAgICAgICBuYW1lOiBUUkFESU5HX0FTU0lTVEFOVF9MQUJFTCxcbiAgICAgICAgICAgICAgICBuYW1lc3BhY2U6ICd0cmFkaW5nLWFzc2lzdGFudCcsXG4gICAgICAgICAgICB9LFxuICAgICAgICAgICAgc3BlYzoge1xuICAgICAgICAgICAgICAgIHJlcGxpY2FzOiAnMScsXG4gICAgICAgICAgICAgICAgc2VsZWN0b3I6IHtcbiAgICAgICAgICAgICAgICAgICAgbWF0Y2hMYWJlbHM6IHtcbiAgICAgICAgICAgICAgICAgICAgICAgIGFwcDogVFJBRElOR19BU1NJU1RBTlRfTEFCRUwsXG4gICAgICAgICAgICAgICAgICAgIH0sXG4gICAgICAgICAgICAgICAgfSxcbiAgICAgICAgICAgICAgICB0ZW1wbGF0ZToge1xuICAgICAgICAgICAgICAgICAgICBtZXRhZGF0YToge1xuICAgICAgICAgICAgICAgICAgICAgICAgbGFiZWxzOiB7XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgYXBwOiBUUkFESU5HX0FTU0lTVEFOVF9MQUJFTCxcbiAgICAgICAgICAgICAgICAgICAgICAgIH0sXG4gICAgICAgICAgICAgICAgICAgIH0sXG4gICAgICAgICAgICAgICAgICAgIHNwZWM6IHtcbiAgICAgICAgICAgICAgICAgICAgICAgIGNvbnRhaW5lcjogW1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgIHtcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgaW1hZ2U6ICdnaGNyLmlvL3dpbGxodW1waHJleXMvdHJhZGluZy1hc3Npc3RhbnQ6YmFja2VuZC1sYXRlc3QnLFxuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICBuYW1lOiBUUkFESU5HX0FTU0lTVEFOVF9MQUJFTCxcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgcG9ydDogW3tcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIGNvbnRhaW5lclBvcnQ6IDgwODAsXG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIH1dLFxuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICBlbnY6IFt7XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICBuYW1lOiAnU1BSSU5HX1BST0ZJTEUnLFxuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgdmFsdWU6ICdjdXJyZW5jaWVzJyxcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgfSwge1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgbmFtZTogJ0RBVEFCQVNFX1VSTCcsXG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICB2YWx1ZTogJ2pkYmM6bXlzcWw6Ly9teXNxbC1zZXJ2aWNlOjMzMDYvbWV0YXRyYWRlcicsXG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIH0sIHtcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIG5hbWU6ICdEQVRBQkFTRV9QQVNTV09SRCcsXG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICB2YWx1ZTogZGJQYXNzd29yZFRlcnJhZm9ybVZhcmlhYmxlLnZhbHVlLFxuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICB9LCB7XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICBuYW1lOiAnU0xBQ0tfV0VCSE9PS19VUkwnLFxuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgdmFsdWU6IHNsYWNrVGVycmFmb3JtVmFyaWFibGUudmFsdWUsXG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIH0sIHtcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIG5hbWU6ICdTVU1PX0xPR0lDX1dFQkhPT0tfVVJMJyxcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIHZhbHVlOiBzdW1vTG9naWNUZXJyYWZvcm1WYXJpYWJsZS52YWx1ZSxcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICBdLFxuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICB2b2x1bWVNb3VudDogW1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAge1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIG5hbWU6ICdhY2NvdW50cy12b2x1bWUnLFxuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIG1vdW50UGF0aDogJy9hY2NvdW50cycsXG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICB9LFxuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAge1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIG5hbWU6ICdtb2NoaS1ncmFwaHMtdm9sdW1lJyxcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICBtb3VudFBhdGg6ICcvbW9jaGktZ3JhcGhzJyxcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIH0sXG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICB7XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgbmFtZTogJ210LXZvbHVtZScsXG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgbW91bnRQYXRoOiAnL210JyxcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIH1dXG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgfSxcbiAgICAgICAgICAgICAgICAgICAgICAgIF0sXG4gICAgICAgICAgICAgICAgICAgICAgICB2b2x1bWU6IFtcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICB7XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIG5hbWU6ICdhY2NvdW50cy12b2x1bWUnLFxuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICBob3N0UGF0aDoge1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgcGF0aDogJy9ob21lL3dpbGwvYWNjb3VudHMnLFxuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICB9LFxuICAgICAgICAgICAgICAgICAgICAgICAgICAgIH0sXG4gICAgICAgICAgICAgICAgICAgICAgICAgICAge1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICBuYW1lOiAnbW9jaGktZ3JhcGhzLXZvbHVtZScsXG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIGhvc3RQYXRoOiB7XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICBwYXRoOiAnL2hvbWUvd2lsbC9tb2NoaS1ncmFwaHMnLFxuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICB9LFxuICAgICAgICAgICAgICAgICAgICAgICAgICAgIH0sXG4gICAgICAgICAgICAgICAgICAgICAgICAgICAge1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICBuYW1lOiAnbXQtdm9sdW1lJyxcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgaG9zdFBhdGg6IHtcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIHBhdGg6ICcvaG9tZS93aWxsLy5jeG9mZmljZS9NZXRhVHJhZGVyXzUvZHJpdmVfYy9Qcm9ncmFtIEZpbGVzL01ldGFUcmFkZXIgNS9NUUw1L0ZpbGVzJyxcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgfSxcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICB9LFxuICAgICAgICAgICAgICAgICAgICAgICAgXSxcbiAgICAgICAgICAgICAgICAgICAgfSxcbiAgICAgICAgICAgICAgICB9LFxuICAgICAgICAgICAgfSxcbiAgICAgICAgfSk7XG4gICAgfVxuXG4gICAgcHJpdmF0ZSBjcmVhdGVEQlRlcnJhZm9ybVNlY3JldCgpIHtcblxuICAgICAgICByZXR1cm4gbmV3IFRlcnJhZm9ybVZhcmlhYmxlKHRoaXMsIFwiZGJQYXNzd29yZFwiLCB7XG4gICAgICAgICAgICB0eXBlOiBcInN0cmluZ1wiLFxuICAgICAgICAgICAgZGVzY3JpcHRpb246IFwicm9vdCBwYXNzd29yZCBmb3IgbXlzcWxcIixcbiAgICAgICAgICAgIHNlbnNpdGl2ZTogdHJ1ZSxcbiAgICAgICAgfSk7XG4gICAgfVxuXG4gICAgcHJpdmF0ZSBjcmVhdGVTbGFja1NlY3JldCgpIHtcblxuICAgICAgICByZXR1cm4gbmV3IFRlcnJhZm9ybVZhcmlhYmxlKHRoaXMsIFwic2xhY2tXZWJIb29rXCIsIHtcbiAgICAgICAgICAgIHR5cGU6IFwic3RyaW5nXCIsXG4gICAgICAgICAgICBkZXNjcmlwdGlvbjogXCJzbGFjayB3ZWJob29rIHVybFwiLFxuICAgICAgICAgICAgc2Vuc2l0aXZlOiB0cnVlLFxuICAgICAgICB9KTtcbiAgICB9XG5cbiAgICBwcml2YXRlIGNyZWF0ZVN1bW9Mb2dpY1NlY3JldCgpIHtcblxuICAgICAgICByZXR1cm4gbmV3IFRlcnJhZm9ybVZhcmlhYmxlKHRoaXMsIFwic3Vtb0xvZ2ljV2ViSG9va1wiLCB7XG4gICAgICAgICAgICB0eXBlOiBcInN0cmluZ1wiLFxuICAgICAgICAgICAgZGVzY3JpcHRpb246IFwic3VtbyBsb2dpYyB3ZWJob29rIHVybFwiLFxuICAgICAgICAgICAgc2Vuc2l0aXZlOiB0cnVlLFxuICAgICAgICB9KTtcbiAgICB9XG5cbiAgICAvLyBwcml2YXRlIGNyZWF0ZUhvbWVWYXJpYWJsZSgpIHtcbiAgICAvL1xuICAgIC8vICAgICByZXR1cm4gbmV3IFRlcnJhZm9ybVZhcmlhYmxlKHRoaXMsIFwia3ViZUhvbWVcIiwge1xuICAgIC8vICAgICAgICAgdHlwZTogXCJzdHJpbmdcIixcbiAgICAvLyAgICAgICAgIGRlc2NyaXB0aW9uOiBcImt1YmUgaG9tZSBkaXJlY3RvcnlcIixcbiAgICAvLyAgICAgICAgIHNlbnNpdGl2ZTogZmFsc2UsXG4gICAgLy8gICAgIH0pO1xuICAgIC8vIH1cbn1cbiJdfQ==