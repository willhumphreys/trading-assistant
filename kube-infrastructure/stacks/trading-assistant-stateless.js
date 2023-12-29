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
            "host": "https://192.168.1.202:6443",
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
                    test: "test",
                    test2: "test2",
                    test3: "test3",
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
                type: 'LoadBalancer',
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
                replicas: '1',
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
//# sourceMappingURL=data:application/json;base64,eyJ2ZXJzaW9uIjozLCJmaWxlIjoidHJhZGluZy1hc3Npc3RhbnQtc3RhdGVsZXNzLmpzIiwic291cmNlUm9vdCI6IiIsInNvdXJjZXMiOlsidHJhZGluZy1hc3Npc3RhbnQtc3RhdGVsZXNzLnRzIl0sIm5hbWVzIjpbXSwibWFwcGluZ3MiOiI7QUFBQSxnQ0FBZ0M7OztBQUdoQyxpQ0FBd0Q7QUFDeEQsc0VBQTJFO0FBQzNFLDRDQUFpSTtBQUNqSSx5REFBeUQ7QUFFekQsTUFBYSw4QkFBK0IsU0FBUSxzQkFBYztJQUM5RCxZQUFZLEtBQWdCLEVBQUUsSUFBWTtRQUN0QyxLQUFLLENBQUMsS0FBSyxFQUFFLElBQUksQ0FBQyxDQUFDO1FBR25CLElBQUksNkJBQWtCLENBQUMsSUFBSSxFQUFFLEtBQUssRUFBRTtZQUNoQyxNQUFNLEVBQUUsNEJBQTRCO1lBQ3BDLFlBQVksRUFBRSxnQkFBZ0I7WUFDOUIsZUFBZSxFQUFFLDZCQUE2QjtTQUNqRCxDQUFDLENBQUM7UUFDTCxpREFBaUQ7UUFFL0MsSUFBSSxhQUFhLEdBQUcsSUFBSSxDQUFDLHVCQUF1QixFQUFFLENBQUM7UUFDbkQsSUFBSSxDQUFDLGdDQUFnQyxDQUFDLElBQUksQ0FBQyxpQkFBaUIsRUFBRSxFQUFFLElBQUksQ0FBQyxxQkFBcUIsRUFBRSxFQUFFLGFBQWEsQ0FBQyxDQUFDO1FBQzdHLElBQUksQ0FBQyx3Q0FBd0MsRUFBRSxDQUFDO1FBQ2hELElBQUksQ0FBQyw2QkFBNkIsRUFBRSxDQUFDO1FBQ3JDLElBQUksQ0FBQyxxQ0FBcUMsRUFBRSxDQUFDO1FBQzdDLElBQUksQ0FBQyxxQkFBcUIsQ0FBQyxhQUFhLENBQUMsQ0FBQztRQUMxQyxJQUFJLENBQUMsa0JBQWtCLEVBQUUsQ0FBQztJQUM5QixDQUFDO0lBRUQsb0RBQW9EO0lBQ3BELHFGQUFxRjtJQUNyRixzQkFBc0I7SUFDdEIsa0RBQWtEO0lBQ2xELCtCQUErQjtJQUMvQiwwQkFBMEI7SUFDMUIscURBQXFEO0lBQ3JELGtEQUFrRDtJQUNsRCw0QkFBNEI7SUFDNUIsaUVBQWlF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QztJQUVPLHVCQUF1QjtRQUUzQixPQUFPLElBQUkseUJBQWlCLENBQUMsSUFBSSxFQUFFLFlBQVksRUFBRTtZQUM3QyxJQUFJLEVBQUUsUUFBUTtZQUNkLFdBQVcsRUFBRSx5QkFBeUI7WUFDdEMsU0FBUyxFQUFFLElBQUk7U0FDbEIsQ0FBQyxDQUFDO0lBQ1AsQ0FBQztJQUVPLGlCQUFpQjtRQUVyQixPQUFPLElBQUkseUJBQWlCLENBQUMsSUFBSSxFQUFFLGNBQWMsRUFBRTtZQUMvQyxJQUFJLEVBQUUsUUFBUTtZQUNkLFdBQVcsRUFBRSxtQkFBbUI7WUFDaEMsU0FBUyxFQUFFLElBQUk7U0FDbEIsQ0FBQyxDQUFDO0lBQ1AsQ0FBQztJQUVPLHFCQUFxQjtRQUV6QixPQUFPLElBQUkseUJBQWlCLENBQUMsSUFBSSxFQUFFLGtCQUFrQixFQUFFO1lBQ25ELElBQUksRUFBRSxRQUFRO1lBQ2QsV0FBVyxFQUFFLHdCQUF3QjtZQUNyQyxTQUFTLEVBQUUsSUFBSTtTQUNsQixDQUFDLENBQUM7SUFDUCxDQUFDO0NBVUo7QUF6V0Qsd0VBeVdDIiwic291cmNlc0NvbnRlbnQiOlsiLy8gdHJhZGluZy1hc3Npc3RhbnQtc3RhdGVmdWwudHNcblxuaW1wb3J0IHtDb25zdHJ1Y3R9IGZyb20gXCJjb25zdHJ1Y3RzXCI7XG5pbXBvcnQge1RlcnJhZm9ybVN0YWNrLCBUZXJyYWZvcm1WYXJpYWJsZX0gZnJvbSBcImNka3RmXCI7XG5pbXBvcnQge0t1YmVybmV0ZXNQcm92aWRlcn0gZnJvbSBcIkBjZGt0Zi9wcm92aWRlci1rdWJlcm5ldGVzL2xpYi9wcm92aWRlclwiO1xuaW1wb3J0IHtNWVNRTF9MQUJFTCwgVFJBRElOR19BU1NJU1RBTlRfTEFCRUwsIFRSQURJTkdfQVNTSVNUQU5UX0ZST05URU5EX0xBQkVMLCBUUkFESU5HX0FTU0lTVEFOVF9OQU1FU1BBQ0V9IGZyb20gXCIuLi9jb25zdGFudHNcIjtcbmltcG9ydCAqIGFzIGt1YmVybmV0ZXMgZnJvbSBcIkBjZGt0Zi9wcm92aWRlci1rdWJlcm5ldGVzXCI7XG5cbmV4cG9ydCBjbGFzcyBUcmFkaW5nQXNzaXN0YW50U3RhdGVsZXNzU3RhY2sgZXh0ZW5kcyBUZXJyYWZvcm1TdGFjayB7XG4gICAgY29uc3RydWN0b3Ioc2NvcGU6IENvbnN0cnVjdCwgbmFtZTogc3RyaW5nKSB7XG4gICAgICAgIHN1cGVyKHNjb3BlLCBuYW1lKTtcblxuXG4gICAgICAgIG5ldyBLdWJlcm5ldGVzUHJvdmlkZXIodGhpcywgJ0s4cycsIHtcbiAgICAgICAgICAgIFwiaG9zdFwiOiBcImh0dHBzOi8vMTkyLjE2OC4xLjIwMjo2NDQzXCIsXG4gICAgICAgICAgICBcImNvbmZpZ1BhdGhcIjogXCJ+Ly5rdWJlL2NvbmZpZ1wiLFxuICAgICAgICAgICAgXCJjb25maWdDb250ZXh0XCI6IFwia3ViZXJuZXRlcy1hZG1pbkBrdWJlcm5ldGVzXCJcbiAgICAgICAgfSk7XG4gICAgICAvLyAgdGhpcy5jcmVhdGVUcmFkaW5nQXNzaXN0YW50RnJvbnRlbmRJbmdyZXNzKCk7XG5cbiAgICAgICAgbGV0IGFkbWluUGFzc3dvcmQgPSB0aGlzLmNyZWF0ZURCVGVycmFmb3JtU2VjcmV0KCk7XG4gICAgICAgIHRoaXMuY3JlYXRlVHJhZGluZ0Fzc2lzdGFudERlcGxveW1lbnQodGhpcy5jcmVhdGVTbGFja1NlY3JldCgpLCB0aGlzLmNyZWF0ZVN1bW9Mb2dpY1NlY3JldCgpLCBhZG1pblBhc3N3b3JkKTtcbiAgICAgICAgdGhpcy5jcmVhdGVUcmFkaW5nQXNzaXN0YW50RnJvbnRlbmREZXBsb3ltZW50KCk7XG4gICAgICAgIHRoaXMuY3JlYXRlVHJhZGluZ0Fzc2lzdGFudFNlcnZpY2UoKTtcbiAgICAgICAgdGhpcy5jcmVhdGVUcmFkaW5nQXNzaXN0YW50RnJvbnRlbmRTZXJ2aWNlKCk7XG4gICAgICAgIHRoaXMuY3JlYXRlTXlzcWxEZXBsb3ltZW50KGFkbWluUGFzc3dvcmQpO1xuICAgICAgICB0aGlzLmNyZWF0ZU15U3FsU2VydmljZSgpO1xuICAgIH1cblxuICAgIC8vIHByaXZhdGUgY3JlYXRlVHJhZGluZ0Fzc2lzdGFudEZyb250ZW5kSW5ncmVzcygpIHtcbiAgICAvLyAgICAgbmV3IGt1YmVybmV0ZXMubWFuaWZlc3QuTWFuaWZlc3QodGhpcywgXCJ0cmFkaW5nLWFzc2lzdGFudC1mcm9udGVuZC1pbmdyZXNzXCIsIHtcbiAgICAvLyAgICAgICAgIG1hbmlmZXN0OiB7XG4gICAgLy8gICAgICAgICAgICAgYXBpVmVyc2lvbjogXCJuZXR3b3JraW5nLms4cy5pby92MVwiLFxuICAgIC8vICAgICAgICAgICAgIGtpbmQ6IFwiSW5ncmVzc1wiLFxuICAgIC8vICAgICAgICAgICAgIG1ldGFkYXRhOiB7XG4gICAgLy8gICAgICAgICAgICAgICAgIG5hbWU6IFwidHJhZGluZy1hc3Npc3RhbnQtaW5ncmVzc1wiLFxuICAgIC8vICAgICAgICAgICAgICAgICBuYW1lc3BhY2U6IFwidHJhZGluZy1hc3Npc3RhbnRcIixcbiAgICAvLyAgICAgICAgICAgICAgICAgbGFiZWxzOiB7XG4gICAgLy8gICAgICAgICAgICAgICAgICAgICBhcHA6IFRSQURJTkdfQVNTSVNUQU5UX0xBQkVMICsgXCItZnJvbnRlbmRcIlxuICAgIC8vICAgICAgICAgICAgICAgICB9LFxuICAgIC8vICAgICAgICAgICAgIH0sXG4gICAgLy8gICAgICAgICAgICAgc3BlYzoge1xuICAgIC8vICAgICAgICAgICAgICAgICBpbmdyZXNzQ2xhc3NOYW1lOiBcIm5naW54XCIsXG4gICAgLy8gICAgICAgICAgICAgICAgIHJ1bGVzOiBbXG4gICAgLy8gICAgICAgICAgICAgICAgICAgICB7XG4gICAgLy8gICAgICAgICAgICAgICAgICAgICAgICAgaG9zdDogXCJ0cmFkaW5nLWFzc2lzdGFudC5tb2NoaS10cmFkaW5nLmNvbVwiLFxuICAgIC8vICAgICAgICAgICAgICAgICAgICAgICAgIGh0dHA6IHtcbiAgICAvLyAgICAgICAgICAgICAgICAgICAgICAgICAgICAgcGF0aHM6IFtcbiAgICAvLyAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIHtcbiAgICAvLyAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICBwYXRoOiBcIi9cIixcbiAgICAvLyAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICBwYXRoVHlwZTogXCJQcmVmaXhcIixcbiAgICAvLyAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICBiYWNrZW5kOiB7XG4gICAgLy8gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIHNlcnZpY2U6IHtcbiAgICAvLyAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIG5hbWU6IFwidHJhZGluZy1hc3Npc3RhbnQtZnJvbnRlbmQtc2VydmljZVwiLFxuICAgIC8vICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgcG9ydDoge1xuICAgIC8vICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIG51bWJlcjogMzAwMCxcbiAgICAvLyAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIH0sXG4gICAgLy8gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICBzZWxlY3Rvcjoge1xuICAgIC8vICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIGFwcDogVFJBRElOR19BU1NJU1RBTlRfTEFCRUwgKyBcIi1mcm9udGVuZFwiLFxuICAgIC8vICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgfSxcbiAgICAvLyAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgfSxcbiAgICAvLyAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICB9LFxuICAgIC8vICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgfSxcbiAgICAvLyAgICAgICAgICAgICAgICAgICAgICAgICAgICAgXSxcbiAgICAvLyAgICAgICAgICAgICAgICAgICAgICAgICB9LFxuICAgIC8vICAgICAgICAgICAgICAgICAgICAgfSxcbiAgICAvLyAgICAgICAgICAgICAgICAgXSxcbiAgICAvLyAgICAgICAgICAgICB9LFxuICAgIC8vICAgICAgICAgfSxcbiAgICAvLyAgICAgfSk7XG4gICAgLy8gfVxuXG4gICAgcHJpdmF0ZSBjcmVhdGVNeVNxbFNlcnZpY2UoKSB7XG4gICAgICAgIG5ldyBrdWJlcm5ldGVzLnNlcnZpY2UuU2VydmljZSh0aGlzLCBcIm15c3FsLXNlcnZpY2VcIiwge1xuICAgICAgICAgICAgbWV0YWRhdGE6IHtcbiAgICAgICAgICAgICAgICBsYWJlbHM6IHtcbiAgICAgICAgICAgICAgICAgICAgYXBwOiBNWVNRTF9MQUJFTCxcbiAgICAgICAgICAgICAgICB9LFxuICAgICAgICAgICAgICAgIG5hbWU6ICdteXNxbC1zZXJ2aWNlJyxcbiAgICAgICAgICAgICAgICBuYW1lc3BhY2U6IFRSQURJTkdfQVNTSVNUQU5UX05BTUVTUEFDRSxcbiAgICAgICAgICAgIH0sXG4gICAgICAgICAgICBzcGVjOiB7XG4gICAgICAgICAgICAgICAgcG9ydDogW3tcbiAgICAgICAgICAgICAgICAgICAgcG9ydDogMzMwNixcbiAgICAgICAgICAgICAgICAgICAgdGFyZ2V0UG9ydDogXCIzMzA2XCIsXG4gICAgICAgICAgICAgICAgfV0sXG4gICAgICAgICAgICAgICAgc2VsZWN0b3I6IHtcbiAgICAgICAgICAgICAgICAgICAgYXBwOiBNWVNRTF9MQUJFTCxcbiAgICAgICAgICAgICAgICB9LFxuICAgICAgICAgICAgICAgIHR5cGU6ICdOb2RlUG9ydCcsXG4gICAgICAgICAgICB9LFxuICAgICAgICB9KTtcbiAgICB9XG5cbiAgICBwcml2YXRlIGNyZWF0ZU15c3FsRGVwbG95bWVudChhZG1pblBhc3N3b3JkOiBUZXJyYWZvcm1WYXJpYWJsZSkge1xuICAgICAgICBuZXcga3ViZXJuZXRlcy5kZXBsb3ltZW50LkRlcGxveW1lbnQodGhpcywgTVlTUUxfTEFCRUwsIHtcbiAgICAgICAgICAgIG1ldGFkYXRhOiB7XG4gICAgICAgICAgICAgICAgbGFiZWxzOiB7XG4gICAgICAgICAgICAgICAgICAgIGFwcDogTVlTUUxfTEFCRUwsXG4gICAgICAgICAgICAgICAgfSxcbiAgICAgICAgICAgICAgICBuYW1lOiAnbXlzcWwtY29udGFpbmVyJyxcbiAgICAgICAgICAgICAgICBuYW1lc3BhY2U6IFRSQURJTkdfQVNTSVNUQU5UX05BTUVTUEFDRSxcbiAgICAgICAgICAgIH0sXG4gICAgICAgICAgICBzcGVjOiB7XG4gICAgICAgICAgICAgICAgcmVwbGljYXM6ICcxJyxcbiAgICAgICAgICAgICAgICBzZWxlY3Rvcjoge1xuICAgICAgICAgICAgICAgICAgICBtYXRjaExhYmVsczoge1xuICAgICAgICAgICAgICAgICAgICAgICAgYXBwOiBNWVNRTF9MQUJFTCxcbiAgICAgICAgICAgICAgICAgICAgfSxcbiAgICAgICAgICAgICAgICB9LFxuICAgICAgICAgICAgICAgIHRlbXBsYXRlOiB7XG4gICAgICAgICAgICAgICAgICAgIG1ldGFkYXRhOiB7XG4gICAgICAgICAgICAgICAgICAgICAgICBsYWJlbHM6IHtcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICBhcHA6IE1ZU1FMX0xBQkVMLFxuICAgICAgICAgICAgICAgICAgICAgICAgfSxcbiAgICAgICAgICAgICAgICAgICAgfSxcbiAgICAgICAgICAgICAgICAgICAgc3BlYzoge1xuICAgICAgICAgICAgICAgICAgICAgICAgY29udGFpbmVyOiBbXG4gICAgICAgICAgICAgICAgICAgICAgICAgICAge1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICBpbWFnZTogJ215c3FsOmxhdGVzdCcsXG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIG5hbWU6IE1ZU1FMX0xBQkVMLFxuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICBwb3J0OiBbe1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgY29udGFpbmVyUG9ydDogMzMwNixcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgfV0sXG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIGVudjogW3tcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIG5hbWU6ICdNWVNRTF9ST09UX1BBU1NXT1JEJyxcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIHZhbHVlOiBhZG1pblBhc3N3b3JkLnZhbHVlLFxuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICB9XSxcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgdm9sdW1lTW91bnQ6IFtcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIHtcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICBuYW1lOiAnbXlzcWwtZGF0YScsXG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgbW91bnRQYXRoOiAnL3Zhci9saWIvbXlzcWwnLFxuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgfV0sXG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgfSxcbiAgICAgICAgICAgICAgICAgICAgICAgIF0sXG4gICAgICAgICAgICAgICAgICAgICAgICB2b2x1bWU6IFtcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICB7XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIG5hbWU6ICdteXNxbC1kYXRhJyxcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgcGVyc2lzdGVudFZvbHVtZUNsYWltOiB7XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICBjbGFpbU5hbWU6ICdteXNxbC1wdi1jbGFpbSdcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAsXG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgfV1cblxuICAgICAgICAgICAgICAgICAgICB9LFxuICAgICAgICAgICAgICAgIH0sXG4gICAgICAgICAgICB9LFxuICAgICAgICB9KTtcbiAgICB9XG5cbiAgICBwcml2YXRlIGNyZWF0ZVRyYWRpbmdBc3Npc3RhbnRTZXJ2aWNlKCkge1xuICAgICAgICBuZXcga3ViZXJuZXRlcy5zZXJ2aWNlLlNlcnZpY2UodGhpcywgXCJ0cmFkaW5nLWFzc2lzdGFudC1zZXJ2aWNlXCIsIHtcbiAgICAgICAgICAgIG1ldGFkYXRhOiB7XG4gICAgICAgICAgICAgICAgbGFiZWxzOiB7XG4gICAgICAgICAgICAgICAgICAgIGFwcDogVFJBRElOR19BU1NJU1RBTlRfTEFCRUwsXG4gICAgICAgICAgICAgICAgICAgIHRlc3Q6IFwidGVzdFwiLFxuICAgICAgICAgICAgICAgICAgICB0ZXN0MjogXCJ0ZXN0MlwiLFxuICAgICAgICAgICAgICAgICAgICB0ZXN0MzogXCJ0ZXN0M1wiLFxuICAgICAgICAgICAgICAgIH0sXG4gICAgICAgICAgICAgICAgbmFtZTogJ3RyYWRpbmctYXNzaXN0YW50LXNlcnZpY2UnLFxuICAgICAgICAgICAgICAgIG5hbWVzcGFjZTogVFJBRElOR19BU1NJU1RBTlRfTkFNRVNQQUNFLFxuICAgICAgICAgICAgfSxcbiAgICAgICAgICAgIHNwZWM6IHtcbiAgICAgICAgICAgICAgICBwb3J0OiBbXG4gICAgICAgICAgICAgICAgICAgIHtcbiAgICAgICAgICAgICAgICAgICAgICAgIHBvcnQ6IDgwODAsXG4gICAgICAgICAgICAgICAgICAgICAgICB0YXJnZXRQb3J0OiBcIjgwODBcIixcbiAgICAgICAgICAgICAgICAgICAgfVxuXG4gICAgICAgICAgICAgICAgXSxcblxuICAgICAgICAgICAgICAgIHNlbGVjdG9yOiB7XG4gICAgICAgICAgICAgICAgICAgIGFwcDogVFJBRElOR19BU1NJU1RBTlRfTEFCRUwsXG4gICAgICAgICAgICAgICAgfSxcbiAgICAgICAgICAgICAgICB0eXBlOiAnTG9hZEJhbGFuY2VyJyxcbiAgICAgICAgICAgIH0sXG4gICAgICAgIH0pO1xuICAgIH1cblxuICAgIHByaXZhdGUgY3JlYXRlVHJhZGluZ0Fzc2lzdGFudEZyb250ZW5kU2VydmljZSgpIHtcbiAgICAgICAgbmV3IGt1YmVybmV0ZXMuc2VydmljZS5TZXJ2aWNlKHRoaXMsIFwidHJhZGluZy1hc3Npc3RhbnQtZnJvbnRlbmQtc2VydmljZVwiLCB7XG4gICAgICAgICAgICBtZXRhZGF0YToge1xuICAgICAgICAgICAgICAgIGxhYmVsczoge1xuICAgICAgICAgICAgICAgICAgICBhcHA6IFRSQURJTkdfQVNTSVNUQU5UX0ZST05URU5EX0xBQkVMLFxuICAgICAgICAgICAgICAgIH0sXG4gICAgICAgICAgICAgICAgbmFtZTogJ3RyYWRpbmctYXNzaXN0YW50LWZyb250ZW5kLXNlcnZpY2UnLFxuICAgICAgICAgICAgICAgIG5hbWVzcGFjZTogVFJBRElOR19BU1NJU1RBTlRfTkFNRVNQQUNFLFxuICAgICAgICAgICAgfSxcbiAgICAgICAgICAgIHNwZWM6IHtcbiAgICAgICAgICAgICAgICBwb3J0OiBbXG4gICAgICAgICAgICAgICAgICAgIHtcbiAgICAgICAgICAgICAgICAgICAgICAgIG5hbWU6IFwiaHR0cFwiLFxuICAgICAgICAgICAgICAgICAgICAgICAgcG9ydDogODAsXG4gICAgICAgICAgICAgICAgICAgICAgICB0YXJnZXRQb3J0OiBcIjMwMDBcIixcbiAgICAgICAgICAgICAgICAgICAgfVxuXG4gICAgICAgICAgICAgICAgXSxcblxuICAgICAgICAgICAgICAgIHNlbGVjdG9yOiB7XG4gICAgICAgICAgICAgICAgICAgIGFwcDogVFJBRElOR19BU1NJU1RBTlRfRlJPTlRFTkRfTEFCRUwsXG4gICAgICAgICAgICAgICAgfSxcbiAgICAgICAgICAgICAgICB0eXBlOiAnTG9hZEJhbGFuY2VyJyxcbiAgICAgICAgICAgIH0sXG4gICAgICAgIH0pO1xuICAgIH1cblxuICAgIHByaXZhdGUgY3JlYXRlVHJhZGluZ0Fzc2lzdGFudEZyb250ZW5kRGVwbG95bWVudCgpIHtcbiAgICAgICAgbmV3IGt1YmVybmV0ZXMuZGVwbG95bWVudC5EZXBsb3ltZW50KHRoaXMsIFRSQURJTkdfQVNTSVNUQU5UX0ZST05URU5EX0xBQkVMLCB7XG4gICAgICAgICAgICBtZXRhZGF0YToge1xuICAgICAgICAgICAgICAgIGxhYmVsczoge1xuICAgICAgICAgICAgICAgICAgICBhcHA6IFRSQURJTkdfQVNTSVNUQU5UX0ZST05URU5EX0xBQkVMLFxuICAgICAgICAgICAgICAgIH0sXG4gICAgICAgICAgICAgICAgbmFtZTogVFJBRElOR19BU1NJU1RBTlRfRlJPTlRFTkRfTEFCRUwsXG4gICAgICAgICAgICAgICAgbmFtZXNwYWNlOiBUUkFESU5HX0FTU0lTVEFOVF9OQU1FU1BBQ0UsXG4gICAgICAgICAgICB9LFxuICAgICAgICAgICAgc3BlYzoge1xuICAgICAgICAgICAgICAgIHJlcGxpY2FzOiAnMScsXG4gICAgICAgICAgICAgICAgc2VsZWN0b3I6IHtcbiAgICAgICAgICAgICAgICAgICAgbWF0Y2hMYWJlbHM6IHtcbiAgICAgICAgICAgICAgICAgICAgICAgIGFwcDogVFJBRElOR19BU1NJU1RBTlRfRlJPTlRFTkRfTEFCRUwsXG4gICAgICAgICAgICAgICAgICAgIH0sXG4gICAgICAgICAgICAgICAgfSxcbiAgICAgICAgICAgICAgICB0ZW1wbGF0ZToge1xuICAgICAgICAgICAgICAgICAgICBtZXRhZGF0YToge1xuICAgICAgICAgICAgICAgICAgICAgICAgbGFiZWxzOiB7XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgYXBwOiBUUkFESU5HX0FTU0lTVEFOVF9GUk9OVEVORF9MQUJFTCxcbiAgICAgICAgICAgICAgICAgICAgICAgIH0sXG4gICAgICAgICAgICAgICAgICAgIH0sXG4gICAgICAgICAgICAgICAgICAgIHNwZWM6IHtcbiAgICAgICAgICAgICAgICAgICAgICAgIGNvbnRhaW5lcjogW1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgIHtcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgaW1hZ2U6ICdnaGNyLmlvL3dpbGxodW1waHJleXMvdHJhZGluZy1hc3Npc3RhbnQ6ZnJvbnRlbmQtbGF0ZXN0JyxcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgaW1hZ2VQdWxsUG9saWN5OiAnQWx3YXlzJyxcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgbmFtZTogVFJBRElOR19BU1NJU1RBTlRfTEFCRUwsXG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIHBvcnQ6IFt7XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICBjb250YWluZXJQb3J0OiAzMDAwLFxuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICB9XSxcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICB9LFxuICAgICAgICAgICAgICAgICAgICAgICAgXVxuICAgICAgICAgICAgICAgICAgICB9LFxuICAgICAgICAgICAgICAgIH0sXG4gICAgICAgICAgICB9LFxuICAgICAgICB9KTtcbiAgICB9XG5cbiAgICBwcml2YXRlIGNyZWF0ZVRyYWRpbmdBc3Npc3RhbnREZXBsb3ltZW50KHNsYWNrVGVycmFmb3JtVmFyaWFibGU6IFRlcnJhZm9ybVZhcmlhYmxlLCBzdW1vTG9naWNUZXJyYWZvcm1WYXJpYWJsZTogVGVycmFmb3JtVmFyaWFibGUsIGRiUGFzc3dvcmRUZXJyYWZvcm1WYXJpYWJsZTogVGVycmFmb3JtVmFyaWFibGUpIHtcbiAgICAgICAgbmV3IGt1YmVybmV0ZXMuZGVwbG95bWVudC5EZXBsb3ltZW50KHRoaXMsIFRSQURJTkdfQVNTSVNUQU5UX0xBQkVMLCB7XG4gICAgICAgICAgICBtZXRhZGF0YToge1xuICAgICAgICAgICAgICAgIGxhYmVsczoge1xuICAgICAgICAgICAgICAgICAgICBhcHA6IFRSQURJTkdfQVNTSVNUQU5UX0xBQkVMLFxuICAgICAgICAgICAgICAgIH0sXG4gICAgICAgICAgICAgICAgbmFtZTogVFJBRElOR19BU1NJU1RBTlRfTEFCRUwsXG4gICAgICAgICAgICAgICAgbmFtZXNwYWNlOiBUUkFESU5HX0FTU0lTVEFOVF9OQU1FU1BBQ0UsXG4gICAgICAgICAgICB9LFxuICAgICAgICAgICAgc3BlYzoge1xuICAgICAgICAgICAgICAgIHJlcGxpY2FzOiAnMScsXG4gICAgICAgICAgICAgICAgc2VsZWN0b3I6IHtcbiAgICAgICAgICAgICAgICAgICAgbWF0Y2hMYWJlbHM6IHtcbiAgICAgICAgICAgICAgICAgICAgICAgIGFwcDogVFJBRElOR19BU1NJU1RBTlRfTEFCRUwsXG4gICAgICAgICAgICAgICAgICAgIH0sXG4gICAgICAgICAgICAgICAgfSxcbiAgICAgICAgICAgICAgICB0ZW1wbGF0ZToge1xuICAgICAgICAgICAgICAgICAgICBtZXRhZGF0YToge1xuICAgICAgICAgICAgICAgICAgICAgICAgbGFiZWxzOiB7XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgYXBwOiBUUkFESU5HX0FTU0lTVEFOVF9MQUJFTCxcbiAgICAgICAgICAgICAgICAgICAgICAgIH0sXG4gICAgICAgICAgICAgICAgICAgIH0sXG4gICAgICAgICAgICAgICAgICAgIHNwZWM6IHtcbiAgICAgICAgICAgICAgICAgICAgICAgIGNvbnRhaW5lcjogW1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgIHtcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgaW1hZ2U6ICdnaGNyLmlvL3dpbGxodW1waHJleXMvdHJhZGluZy1hc3Npc3RhbnQ6YmFja2VuZC1sYXRlc3QnLFxuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICBpbWFnZVB1bGxQb2xpY3k6ICdBbHdheXMnLFxuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICBuYW1lOiBUUkFESU5HX0FTU0lTVEFOVF9MQUJFTCxcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgcG9ydDogW3tcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIGNvbnRhaW5lclBvcnQ6IDgwODAsXG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIH1dLFxuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICBlbnY6IFt7XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICBuYW1lOiAnU1BSSU5HX1BST0ZJTEUnLFxuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgdmFsdWU6ICdjdXJyZW5jaWVzJyxcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgfSwge1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgbmFtZTogJ0RBVEFCQVNFX1VSTCcsXG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICB2YWx1ZTogJ2pkYmM6bXlzcWw6Ly9teXNxbC1zZXJ2aWNlOjMzMDYvbWV0YXRyYWRlcicsXG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIH0sIHtcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIG5hbWU6ICdEQVRBQkFTRV9QQVNTV09SRCcsXG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICB2YWx1ZTogZGJQYXNzd29yZFRlcnJhZm9ybVZhcmlhYmxlLnZhbHVlLFxuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICB9LCB7XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICBuYW1lOiAnU0xBQ0tfV0VCSE9PS19VUkwnLFxuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgdmFsdWU6IHNsYWNrVGVycmFmb3JtVmFyaWFibGUudmFsdWUsXG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIH0sIHtcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIG5hbWU6ICdTVU1PX0xPR0lDX1dFQkhPT0tfVVJMJyxcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIHZhbHVlOiBzdW1vTG9naWNUZXJyYWZvcm1WYXJpYWJsZS52YWx1ZSxcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICBdLFxuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICB2b2x1bWVNb3VudDogW1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAge1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIG5hbWU6ICdhY2NvdW50cy12b2x1bWUnLFxuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIG1vdW50UGF0aDogJy9hY2NvdW50cycsXG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICB9LFxuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAge1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIG5hbWU6ICdtb2NoaS1ncmFwaHMtdm9sdW1lJyxcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICBtb3VudFBhdGg6ICcvbW9jaGktZ3JhcGhzJyxcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIH0sXG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICB7XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgbmFtZTogJ210LXZvbHVtZScsXG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgbW91bnRQYXRoOiAnL210JyxcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIH1dXG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgfSxcbiAgICAgICAgICAgICAgICAgICAgICAgIF0sXG4gICAgICAgICAgICAgICAgICAgICAgICB2b2x1bWU6IFtcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICB7XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIG5hbWU6ICdhY2NvdW50cy12b2x1bWUnLFxuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICBob3N0UGF0aDoge1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgcGF0aDogJy9ob21lL3dpbGwvYWNjb3VudHMnLFxuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICB9LFxuICAgICAgICAgICAgICAgICAgICAgICAgICAgIH0sXG4gICAgICAgICAgICAgICAgICAgICAgICAgICAge1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICBuYW1lOiAnbW9jaGktZ3JhcGhzLXZvbHVtZScsXG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIGhvc3RQYXRoOiB7XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICBwYXRoOiAnL2hvbWUvd2lsbC9tb2NoaS1ncmFwaHMnLFxuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICB9LFxuICAgICAgICAgICAgICAgICAgICAgICAgICAgIH0sXG4gICAgICAgICAgICAgICAgICAgICAgICAgICAge1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICBuYW1lOiAnbXQtdm9sdW1lJyxcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgaG9zdFBhdGg6IHtcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIHBhdGg6ICcvaG9tZS93aWxsLy5jeG9mZmljZS9NZXRhVHJhZGVyXzUvZHJpdmVfYy9Qcm9ncmFtIEZpbGVzL01ldGFUcmFkZXIgNS9NUUw1L0ZpbGVzJyxcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgfSxcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICB9LFxuICAgICAgICAgICAgICAgICAgICAgICAgXSxcbiAgICAgICAgICAgICAgICAgICAgfSxcbiAgICAgICAgICAgICAgICB9LFxuICAgICAgICAgICAgfSxcbiAgICAgICAgfSk7XG4gICAgfVxuXG4gICAgcHJpdmF0ZSBjcmVhdGVEQlRlcnJhZm9ybVNlY3JldCgpIHtcblxuICAgICAgICByZXR1cm4gbmV3IFRlcnJhZm9ybVZhcmlhYmxlKHRoaXMsIFwiZGJQYXNzd29yZFwiLCB7XG4gICAgICAgICAgICB0eXBlOiBcInN0cmluZ1wiLFxuICAgICAgICAgICAgZGVzY3JpcHRpb246IFwicm9vdCBwYXNzd29yZCBmb3IgbXlzcWxcIixcbiAgICAgICAgICAgIHNlbnNpdGl2ZTogdHJ1ZSxcbiAgICAgICAgfSk7XG4gICAgfVxuXG4gICAgcHJpdmF0ZSBjcmVhdGVTbGFja1NlY3JldCgpIHtcblxuICAgICAgICByZXR1cm4gbmV3IFRlcnJhZm9ybVZhcmlhYmxlKHRoaXMsIFwic2xhY2tXZWJIb29rXCIsIHtcbiAgICAgICAgICAgIHR5cGU6IFwic3RyaW5nXCIsXG4gICAgICAgICAgICBkZXNjcmlwdGlvbjogXCJzbGFjayB3ZWJob29rIHVybFwiLFxuICAgICAgICAgICAgc2Vuc2l0aXZlOiB0cnVlLFxuICAgICAgICB9KTtcbiAgICB9XG5cbiAgICBwcml2YXRlIGNyZWF0ZVN1bW9Mb2dpY1NlY3JldCgpIHtcblxuICAgICAgICByZXR1cm4gbmV3IFRlcnJhZm9ybVZhcmlhYmxlKHRoaXMsIFwic3Vtb0xvZ2ljV2ViSG9va1wiLCB7XG4gICAgICAgICAgICB0eXBlOiBcInN0cmluZ1wiLFxuICAgICAgICAgICAgZGVzY3JpcHRpb246IFwic3VtbyBsb2dpYyB3ZWJob29rIHVybFwiLFxuICAgICAgICAgICAgc2Vuc2l0aXZlOiB0cnVlLFxuICAgICAgICB9KTtcbiAgICB9XG5cbiAgICAvLyBwcml2YXRlIGNyZWF0ZUhvbWVWYXJpYWJsZSgpIHtcbiAgICAvL1xuICAgIC8vICAgICByZXR1cm4gbmV3IFRlcnJhZm9ybVZhcmlhYmxlKHRoaXMsIFwia3ViZUhvbWVcIiwge1xuICAgIC8vICAgICAgICAgdHlwZTogXCJzdHJpbmdcIixcbiAgICAvLyAgICAgICAgIGRlc2NyaXB0aW9uOiBcImt1YmUgaG9tZSBkaXJlY3RvcnlcIixcbiAgICAvLyAgICAgICAgIHNlbnNpdGl2ZTogZmFsc2UsXG4gICAgLy8gICAgIH0pO1xuICAgIC8vIH1cbn1cbiJdfQ==