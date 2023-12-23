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
                                    }]
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
//# sourceMappingURL=data:application/json;base64,eyJ2ZXJzaW9uIjozLCJmaWxlIjoidHJhZGluZy1hc3Npc3RhbnQtc3RhdGVsZXNzLmpzIiwic291cmNlUm9vdCI6IiIsInNvdXJjZXMiOlsidHJhZGluZy1hc3Npc3RhbnQtc3RhdGVsZXNzLnRzIl0sIm5hbWVzIjpbXSwibWFwcGluZ3MiOiI7QUFBQSxnQ0FBZ0M7OztBQUdoQyxpQ0FBd0Q7QUFDeEQsc0VBQTJFO0FBQzNFLDRDQUFrRTtBQUNsRSx5REFBeUQ7QUFFekQsTUFBYSw4QkFBK0IsU0FBUSxzQkFBYztJQUM5RCxZQUFZLEtBQWdCLEVBQUUsSUFBWTtRQUN0QyxLQUFLLENBQUMsS0FBSyxFQUFFLElBQUksQ0FBQyxDQUFDO1FBR25CLElBQUksNkJBQWtCLENBQUMsSUFBSSxFQUFFLEtBQUssRUFBRTtZQUNoQyxZQUFZLEVBQUUsZ0JBQWdCO1lBQzlCLGVBQWUsRUFBRSw2QkFBNkI7U0FDakQsQ0FBQyxDQUFDO1FBQ0gsSUFBSSxDQUFDLHNDQUFzQyxFQUFFLENBQUM7UUFFOUMsSUFBSSxhQUFhLEdBQUcsSUFBSSxDQUFDLHVCQUF1QixFQUFFLENBQUM7UUFDbkQsSUFBSSxDQUFDLGdDQUFnQyxDQUFDLElBQUksQ0FBQyxpQkFBaUIsRUFBRSxFQUFFLElBQUksQ0FBQyxxQkFBcUIsRUFBRSxFQUFFLGFBQWEsQ0FBQyxDQUFDO1FBQzdHLElBQUksQ0FBQyx3Q0FBd0MsRUFBRSxDQUFDO1FBQ2hELElBQUksQ0FBQyw2QkFBNkIsRUFBRSxDQUFDO1FBQ3JDLElBQUksQ0FBQyxxQ0FBcUMsRUFBRSxDQUFDO1FBQzdDLElBQUksQ0FBQyxxQkFBcUIsQ0FBQyxhQUFhLENBQUMsQ0FBQztRQUMxQyxJQUFJLENBQUMsa0JBQWtCLEVBQUUsQ0FBQztJQUM5QixDQUFDO0lBRU8sc0NBQXNDO1FBQzFDLElBQUksVUFBVSxDQUFDLFFBQVEsQ0FBQyxRQUFRLENBQUMsSUFBSSxFQUFFLDJCQUEyQixFQUFFO1lBQ2hFLFFBQVEsRUFBRTtnQkFDTixVQUFVLEVBQUUsc0JBQXNCO2dCQUNsQyxJQUFJLEVBQUUs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QUk7U0FDbEIsQ0FBQyxDQUFDO0lBQ1AsQ0FBQztJQUVPLGlCQUFpQjtRQUVyQixPQUFPLElBQUkseUJBQWlCLENBQUMsSUFBSSxFQUFFLGNBQWMsRUFBRTtZQUMvQyxJQUFJLEVBQUUsUUFBUTtZQUNkLFdBQVcsRUFBRSxtQkFBbUI7WUFDaEMsU0FBUyxFQUFFLElBQUk7U0FDbEIsQ0FBQyxDQUFDO0lBQ1AsQ0FBQztJQUVPLHFCQUFxQjtRQUV6QixPQUFPLElBQUkseUJBQWlCLENBQUMsSUFBSSxFQUFFLGtCQUFrQixFQUFFO1lBQ25ELElBQUksRUFBRSxRQUFRO1lBQ2QsV0FBVyxFQUFFLHdCQUF3QjtZQUNyQyxTQUFTLEVBQUUsSUFBSTtTQUNsQixDQUFDLENBQUM7SUFDUCxDQUFDO0NBVUo7QUE5VkQsd0VBOFZDIiwic291cmNlc0NvbnRlbnQiOlsiLy8gdHJhZGluZy1hc3Npc3RhbnQtc3RhdGVmdWwudHNcblxuaW1wb3J0IHtDb25zdHJ1Y3R9IGZyb20gXCJjb25zdHJ1Y3RzXCI7XG5pbXBvcnQge1RlcnJhZm9ybVN0YWNrLCBUZXJyYWZvcm1WYXJpYWJsZX0gZnJvbSBcImNka3RmXCI7XG5pbXBvcnQge0t1YmVybmV0ZXNQcm92aWRlcn0gZnJvbSBcIkBjZGt0Zi9wcm92aWRlci1rdWJlcm5ldGVzL2xpYi9wcm92aWRlclwiO1xuaW1wb3J0IHtNWVNRTF9MQUJFTCwgVFJBRElOR19BU1NJU1RBTlRfTEFCRUx9IGZyb20gXCIuLi9jb25zdGFudHNcIjtcbmltcG9ydCAqIGFzIGt1YmVybmV0ZXMgZnJvbSBcIkBjZGt0Zi9wcm92aWRlci1rdWJlcm5ldGVzXCI7XG5cbmV4cG9ydCBjbGFzcyBUcmFkaW5nQXNzaXN0YW50U3RhdGVsZXNzU3RhY2sgZXh0ZW5kcyBUZXJyYWZvcm1TdGFjayB7XG4gICAgY29uc3RydWN0b3Ioc2NvcGU6IENvbnN0cnVjdCwgbmFtZTogc3RyaW5nKSB7XG4gICAgICAgIHN1cGVyKHNjb3BlLCBuYW1lKTtcblxuXG4gICAgICAgIG5ldyBLdWJlcm5ldGVzUHJvdmlkZXIodGhpcywgJ0s4cycsIHtcbiAgICAgICAgICAgIFwiY29uZmlnUGF0aFwiOiBcIn4vLmt1YmUvY29uZmlnXCIsXG4gICAgICAgICAgICBcImNvbmZpZ0NvbnRleHRcIjogXCJrdWJlcm5ldGVzLWFkbWluQGt1YmVybmV0ZXNcIlxuICAgICAgICB9KTtcbiAgICAgICAgdGhpcy5jcmVhdGVUcmFkaW5nQXNzaXN0YW50QW5kTXlzcWxJbmdyZXNzMigpO1xuXG4gICAgICAgIGxldCBhZG1pblBhc3N3b3JkID0gdGhpcy5jcmVhdGVEQlRlcnJhZm9ybVNlY3JldCgpO1xuICAgICAgICB0aGlzLmNyZWF0ZVRyYWRpbmdBc3Npc3RhbnREZXBsb3ltZW50KHRoaXMuY3JlYXRlU2xhY2tTZWNyZXQoKSwgdGhpcy5jcmVhdGVTdW1vTG9naWNTZWNyZXQoKSwgYWRtaW5QYXNzd29yZCk7XG4gICAgICAgIHRoaXMuY3JlYXRlVHJhZGluZ0Fzc2lzdGFudEZyb250ZW5kRGVwbG95bWVudCgpO1xuICAgICAgICB0aGlzLmNyZWF0ZVRyYWRpbmdBc3Npc3RhbnRTZXJ2aWNlKCk7XG4gICAgICAgIHRoaXMuY3JlYXRlVHJhZGluZ0Fzc2lzdGFudEZyb250ZW5kU2VydmljZSgpO1xuICAgICAgICB0aGlzLmNyZWF0ZU15c3FsRGVwbG95bWVudChhZG1pblBhc3N3b3JkKTtcbiAgICAgICAgdGhpcy5jcmVhdGVNeVNxbFNlcnZpY2UoKTtcbiAgICB9XG5cbiAgICBwcml2YXRlIGNyZWF0ZVRyYWRpbmdBc3Npc3RhbnRBbmRNeXNxbEluZ3Jlc3MyKCkge1xuICAgICAgICBuZXcga3ViZXJuZXRlcy5tYW5pZmVzdC5NYW5pZmVzdCh0aGlzLCBcInRyYWRpbmctYXNzaXN0YW50LWluZ3Jlc3NcIiwge1xuICAgICAgICAgICAgbWFuaWZlc3Q6IHtcbiAgICAgICAgICAgICAgICBhcGlWZXJzaW9uOiBcIm5ldHdvcmtpbmcuazhzLmlvL3YxXCIsXG4gICAgICAgICAgICAgICAga2luZDogXCJJbmdyZXNzXCIsXG4gICAgICAgICAgICAgICAgbWV0YWRhdGE6IHtcbiAgICAgICAgICAgICAgICAgICAgbmFtZTogXCJ0cmFkaW5nLWFzc2lzdGFudC1pbmdyZXNzXCIsXG4gICAgICAgICAgICAgICAgICAgIG5hbWVzcGFjZTogXCJ0cmFkaW5nLWFzc2lzdGFudFwiLFxuICAgICAgICAgICAgICAgIH0sXG4gICAgICAgICAgICAgICAgc3BlYzoge1xuICAgICAgICAgICAgICAgICAgICBydWxlczogW1xuICAgICAgICAgICAgICAgICAgICAgICAge1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgIGhvc3Q6IFwidHJhZGluZy1hc3Npc3RhbnQubW9jaGktdHJhZGluZy5jb21cIixcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICBodHRwOiB7XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIHBhdGhzOiBbXG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICB7XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgcGF0aDogXCIvXCIsXG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgcGF0aFR5cGU6IFwiUHJlZml4XCIsXG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgYmFja2VuZDoge1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICBzZXJ2aWNlOiB7XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICBuYW1lOiBcInRyYWRpbmctYXNzaXN0YW50LXNlcnZpY2VcIixcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIHBvcnQ6IHtcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICBudW1iZXI6IDgwODAsXG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICB9LFxuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICB9LFxuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIH0sXG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICB9LFxuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICBdLFxuICAgICAgICAgICAgICAgICAgICAgICAgICAgIH0sXG4gICAgICAgICAgICAgICAgICAgICAgICB9LFxuICAgICAgICAgICAgICAgICAgICBdLFxuICAgICAgICAgICAgICAgIH0sXG4gICAgICAgICAgICB9LFxuICAgICAgICB9KTtcbiAgICB9XG5cbiAgICBwcml2YXRlIGNyZWF0ZU15U3FsU2VydmljZSgpIHtcbiAgICAgICAgbmV3IGt1YmVybmV0ZXMuc2VydmljZS5TZXJ2aWNlKHRoaXMsIFwibXlzcWwtc2VydmljZVwiLCB7XG4gICAgICAgICAgICBtZXRhZGF0YToge1xuICAgICAgICAgICAgICAgIGxhYmVsczoge1xuICAgICAgICAgICAgICAgICAgICBhcHA6IE1ZU1FMX0xBQkVMLFxuICAgICAgICAgICAgICAgIH0sXG4gICAgICAgICAgICAgICAgbmFtZTogJ215c3FsLXNlcnZpY2UnLFxuICAgICAgICAgICAgICAgIG5hbWVzcGFjZTogJ3RyYWRpbmctYXNzaXN0YW50JyxcbiAgICAgICAgICAgIH0sXG4gICAgICAgICAgICBzcGVjOiB7XG4gICAgICAgICAgICAgICAgcG9ydDogW3tcbiAgICAgICAgICAgICAgICAgICAgcG9ydDogMzMwNixcbiAgICAgICAgICAgICAgICAgICAgdGFyZ2V0UG9ydDogXCIzMzA2XCIsXG4gICAgICAgICAgICAgICAgfV0sXG4gICAgICAgICAgICAgICAgc2VsZWN0b3I6IHtcbiAgICAgICAgICAgICAgICAgICAgYXBwOiBNWVNRTF9MQUJFTCxcbiAgICAgICAgICAgICAgICB9LFxuICAgICAgICAgICAgICAgIHR5cGU6ICdOb2RlUG9ydCcsXG4gICAgICAgICAgICB9LFxuICAgICAgICB9KTtcbiAgICB9XG5cbiAgICBwcml2YXRlIGNyZWF0ZU15c3FsRGVwbG95bWVudChhZG1pblBhc3N3b3JkOiBUZXJyYWZvcm1WYXJpYWJsZSkge1xuICAgICAgICBuZXcga3ViZXJuZXRlcy5kZXBsb3ltZW50LkRlcGxveW1lbnQodGhpcywgTVlTUUxfTEFCRUwsIHtcbiAgICAgICAgICAgIG1ldGFkYXRhOiB7XG4gICAgICAgICAgICAgICAgbGFiZWxzOiB7XG4gICAgICAgICAgICAgICAgICAgIGFwcDogTVlTUUxfTEFCRUwsXG4gICAgICAgICAgICAgICAgfSxcbiAgICAgICAgICAgICAgICBuYW1lOiAnbXlzcWwtY29udGFpbmVyJyxcbiAgICAgICAgICAgICAgICBuYW1lc3BhY2U6ICd0cmFkaW5nLWFzc2lzdGFudCcsXG4gICAgICAgICAgICB9LFxuICAgICAgICAgICAgc3BlYzoge1xuICAgICAgICAgICAgICAgIHJlcGxpY2FzOiAnMScsXG4gICAgICAgICAgICAgICAgc2VsZWN0b3I6IHtcbiAgICAgICAgICAgICAgICAgICAgbWF0Y2hMYWJlbHM6IHtcbiAgICAgICAgICAgICAgICAgICAgICAgIGFwcDogTVlTUUxfTEFCRUwsXG4gICAgICAgICAgICAgICAgICAgIH0sXG4gICAgICAgICAgICAgICAgfSxcbiAgICAgICAgICAgICAgICB0ZW1wbGF0ZToge1xuICAgICAgICAgICAgICAgICAgICBtZXRhZGF0YToge1xuICAgICAgICAgICAgICAgICAgICAgICAgbGFiZWxzOiB7XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgYXBwOiBNWVNRTF9MQUJFTCxcbiAgICAgICAgICAgICAgICAgICAgICAgIH0sXG4gICAgICAgICAgICAgICAgICAgIH0sXG4gICAgICAgICAgICAgICAgICAgIHNwZWM6IHtcbiAgICAgICAgICAgICAgICAgICAgICAgIGNvbnRhaW5lcjogW1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgIHtcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgaW1hZ2U6ICdteXNxbDpsYXRlc3QnLFxuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICBuYW1lOiBNWVNRTF9MQUJFTCxcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgcG9ydDogW3tcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIGNvbnRhaW5lclBvcnQ6IDMzMDYsXG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIH1dLFxuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICBlbnY6IFt7XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICBuYW1lOiAnTVlTUUxfUk9PVF9QQVNTV09SRCcsXG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICB2YWx1ZTogYWRtaW5QYXNzd29yZC52YWx1ZSxcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgfV0sXG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIHZvbHVtZU1vdW50OiBbXG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICB7XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgbmFtZTogJ215c3FsLWRhdGEnLFxuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIG1vdW50UGF0aDogJy92YXIvbGliL215c3FsJyxcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIH1dLFxuICAgICAgICAgICAgICAgICAgICAgICAgICAgIH0sXG4gICAgICAgICAgICAgICAgICAgICAgICBdLFxuICAgICAgICAgICAgICAgICAgICAgICAgdm9sdW1lOiBbXG4gICAgICAgICAgICAgICAgICAgICAgICAgICAge1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICBuYW1lOiAnbXlzcWwtZGF0YScsXG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIHBlcnNpc3RlbnRWb2x1bWVDbGFpbToge1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgY2xhaW1OYW1lOiAnbXlzcWwtcHYtY2xhaW0nXG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgLFxuICAgICAgICAgICAgICAgICAgICAgICAgICAgIH1dXG5cbiAgICAgICAgICAgICAgICAgICAgfSxcbiAgICAgICAgICAgICAgICB9LFxuICAgICAgICAgICAgfSxcbiAgICAgICAgfSk7XG4gICAgfVxuXG4gICAgcHJpdmF0ZSBjcmVhdGVUcmFkaW5nQXNzaXN0YW50U2VydmljZSgpIHtcbiAgICAgICAgbmV3IGt1YmVybmV0ZXMuc2VydmljZS5TZXJ2aWNlKHRoaXMsIFwidHJhZGluZy1hc3Npc3RhbnQtc2VydmljZVwiLCB7XG4gICAgICAgICAgICBtZXRhZGF0YToge1xuICAgICAgICAgICAgICAgIGxhYmVsczoge1xuICAgICAgICAgICAgICAgICAgICBhcHA6IFRSQURJTkdfQVNTSVNUQU5UX0xBQkVMLFxuICAgICAgICAgICAgICAgIH0sXG4gICAgICAgICAgICAgICAgbmFtZTogJ3RyYWRpbmctYXNzaXN0YW50LXNlcnZpY2UnLFxuICAgICAgICAgICAgICAgIG5hbWVzcGFjZTogJ3RyYWRpbmctYXNzaXN0YW50JyxcbiAgICAgICAgICAgIH0sXG4gICAgICAgICAgICBzcGVjOiB7XG4gICAgICAgICAgICAgICAgcG9ydDogW1xuICAgICAgICAgICAgICAgICAgICB7XG4gICAgICAgICAgICAgICAgICAgICAgICBwb3J0OiA4MDgwLFxuICAgICAgICAgICAgICAgICAgICAgICAgdGFyZ2V0UG9ydDogXCI4MDgwXCIsXG4gICAgICAgICAgICAgICAgICAgIH1cblxuICAgICAgICAgICAgICAgIF0sXG5cbiAgICAgICAgICAgICAgICBzZWxlY3Rvcjoge1xuICAgICAgICAgICAgICAgICAgICBhcHA6IFRSQURJTkdfQVNTSVNUQU5UX0xBQkVMLFxuICAgICAgICAgICAgICAgIH0sXG4gICAgICAgICAgICAgICAgdHlwZTogJ05vZGVQb3J0JyxcbiAgICAgICAgICAgIH0sXG4gICAgICAgIH0pO1xuICAgIH1cblxuICAgIHByaXZhdGUgY3JlYXRlVHJhZGluZ0Fzc2lzdGFudEZyb250ZW5kU2VydmljZSgpIHtcbiAgICAgICAgbmV3IGt1YmVybmV0ZXMuc2VydmljZS5TZXJ2aWNlKHRoaXMsIFwidHJhZGluZy1hc3Npc3RhbnQtZnJvbnRlbmQtc2VydmljZVwiLCB7XG4gICAgICAgICAgICBtZXRhZGF0YToge1xuICAgICAgICAgICAgICAgIGxhYmVsczoge1xuICAgICAgICAgICAgICAgICAgICBhcHA6IFRSQURJTkdfQVNTSVNUQU5UX0xBQkVMLFxuICAgICAgICAgICAgICAgIH0sXG4gICAgICAgICAgICAgICAgbmFtZTogJ3RyYWRpbmctYXNzaXN0YW50LWZyb250ZW5kLXNlcnZpY2UnLFxuICAgICAgICAgICAgICAgIG5hbWVzcGFjZTogJ3RyYWRpbmctYXNzaXN0YW50JyxcbiAgICAgICAgICAgIH0sXG4gICAgICAgICAgICBzcGVjOiB7XG4gICAgICAgICAgICAgICAgcG9ydDogW1xuICAgICAgICAgICAgICAgICAgICB7XG4gICAgICAgICAgICAgICAgICAgICAgICBwb3J0OiAzMDAwLFxuICAgICAgICAgICAgICAgICAgICAgICAgdGFyZ2V0UG9ydDogXCIzMDAwXCIsXG4gICAgICAgICAgICAgICAgICAgIH1cblxuICAgICAgICAgICAgICAgIF0sXG5cbiAgICAgICAgICAgICAgICBzZWxlY3Rvcjoge1xuICAgICAgICAgICAgICAgICAgICBhcHA6IFRSQURJTkdfQVNTSVNUQU5UX0xBQkVMLFxuICAgICAgICAgICAgICAgIH0sXG4gICAgICAgICAgICAgICAgdHlwZTogJ05vZGVQb3J0JyxcbiAgICAgICAgICAgIH0sXG4gICAgICAgIH0pO1xuICAgIH1cblxuXG5cbiAgICBwcml2YXRlIGNyZWF0ZVRyYWRpbmdBc3Npc3RhbnRGcm9udGVuZERlcGxveW1lbnQoKSB7XG4gICAgICAgIG5ldyBrdWJlcm5ldGVzLmRlcGxveW1lbnQuRGVwbG95bWVudCh0aGlzLCBUUkFESU5HX0FTU0lTVEFOVF9MQUJFTCsgJy1mcm9udGVuZCcsIHtcbiAgICAgICAgICAgIG1ldGFkYXRhOiB7XG4gICAgICAgICAgICAgICAgbGFiZWxzOiB7XG4gICAgICAgICAgICAgICAgICAgIGFwcDogVFJBRElOR19BU1NJU1RBTlRfTEFCRUwsXG4gICAgICAgICAgICAgICAgfSxcbiAgICAgICAgICAgICAgICBuYW1lOiBUUkFESU5HX0FTU0lTVEFOVF9MQUJFTCArICctZnJvbnRlbmQnLFxuICAgICAgICAgICAgICAgIG5hbWVzcGFjZTogJ3RyYWRpbmctYXNzaXN0YW50JyxcbiAgICAgICAgICAgIH0sXG4gICAgICAgICAgICBzcGVjOiB7XG4gICAgICAgICAgICAgICAgcmVwbGljYXM6ICcxJyxcbiAgICAgICAgICAgICAgICBzZWxlY3Rvcjoge1xuICAgICAgICAgICAgICAgICAgICBtYXRjaExhYmVsczoge1xuICAgICAgICAgICAgICAgICAgICAgICAgYXBwOiBUUkFESU5HX0FTU0lTVEFOVF9MQUJFTCxcbiAgICAgICAgICAgICAgICAgICAgfSxcbiAgICAgICAgICAgICAgICB9LFxuICAgICAgICAgICAgICAgIHRlbXBsYXRlOiB7XG4gICAgICAgICAgICAgICAgICAgIG1ldGFkYXRhOiB7XG4gICAgICAgICAgICAgICAgICAgICAgICBsYWJlbHM6IHtcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICBhcHA6IFRSQURJTkdfQVNTSVNUQU5UX0xBQkVMLFxuICAgICAgICAgICAgICAgICAgICAgICAgfSxcbiAgICAgICAgICAgICAgICAgICAgfSxcbiAgICAgICAgICAgICAgICAgICAgc3BlYzoge1xuICAgICAgICAgICAgICAgICAgICAgICAgY29udGFpbmVyOiBbXG4gICAgICAgICAgICAgICAgICAgICAgICAgICAge1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICBpbWFnZTogJ2doY3IuaW8vd2lsbGh1bXBocmV5cy90cmFkaW5nLWFzc2lzdGFudDpmcm9udGVuZC1sYXRlc3QnLFxuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICBuYW1lOiBUUkFESU5HX0FTU0lTVEFOVF9MQUJFTCxcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgcG9ydDogW3tcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIGNvbnRhaW5lclBvcnQ6IDMwMDAsXG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIH1dXG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgfSxcbiAgICAgICAgICAgICAgICAgICAgICAgIF1cbiAgICAgICAgICAgICAgICAgICAgfSxcbiAgICAgICAgICAgICAgICB9LFxuICAgICAgICAgICAgfSxcbiAgICAgICAgfSk7XG4gICAgfVxuXG5cbiAgICBwcml2YXRlIGNyZWF0ZVRyYWRpbmdBc3Npc3RhbnREZXBsb3ltZW50KHNsYWNrVGVycmFmb3JtVmFyaWFibGU6IFRlcnJhZm9ybVZhcmlhYmxlLCBzdW1vTG9naWNUZXJyYWZvcm1WYXJpYWJsZTogVGVycmFmb3JtVmFyaWFibGUsIGRiUGFzc3dvcmRUZXJyYWZvcm1WYXJpYWJsZTogVGVycmFmb3JtVmFyaWFibGUpIHtcbiAgICAgICAgbmV3IGt1YmVybmV0ZXMuZGVwbG95bWVudC5EZXBsb3ltZW50KHRoaXMsIFRSQURJTkdfQVNTSVNUQU5UX0xBQkVMLCB7XG4gICAgICAgICAgICBtZXRhZGF0YToge1xuICAgICAgICAgICAgICAgIGxhYmVsczoge1xuICAgICAgICAgICAgICAgICAgICBhcHA6IFRSQURJTkdfQVNTSVNUQU5UX0xBQkVMLFxuICAgICAgICAgICAgICAgIH0sXG4gICAgICAgICAgICAgICAgbmFtZTogVFJBRElOR19BU1NJU1RBTlRfTEFCRUwsXG4gICAgICAgICAgICAgICAgbmFtZXNwYWNlOiAndHJhZGluZy1hc3Npc3RhbnQnLFxuICAgICAgICAgICAgfSxcbiAgICAgICAgICAgIHNwZWM6IHtcbiAgICAgICAgICAgICAgICByZXBsaWNhczogJzEnLFxuICAgICAgICAgICAgICAgIHNlbGVjdG9yOiB7XG4gICAgICAgICAgICAgICAgICAgIG1hdGNoTGFiZWxzOiB7XG4gICAgICAgICAgICAgICAgICAgICAgICBhcHA6IFRSQURJTkdfQVNTSVNUQU5UX0xBQkVMLFxuICAgICAgICAgICAgICAgICAgICB9LFxuICAgICAgICAgICAgICAgIH0sXG4gICAgICAgICAgICAgICAgdGVtcGxhdGU6IHtcbiAgICAgICAgICAgICAgICAgICAgbWV0YWRhdGE6IHtcbiAgICAgICAgICAgICAgICAgICAgICAgIGxhYmVsczoge1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgIGFwcDogVFJBRElOR19BU1NJU1RBTlRfTEFCRUwsXG4gICAgICAgICAgICAgICAgICAgICAgICB9LFxuICAgICAgICAgICAgICAgICAgICB9LFxuICAgICAgICAgICAgICAgICAgICBzcGVjOiB7XG4gICAgICAgICAgICAgICAgICAgICAgICBjb250YWluZXI6IFtcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICB7XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIGltYWdlOiAnZ2hjci5pby93aWxsaHVtcGhyZXlzL3RyYWRpbmctYXNzaXN0YW50OmJhY2tlbmQtbGF0ZXN0JyxcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgbmFtZTogVFJBRElOR19BU1NJU1RBTlRfTEFCRUwsXG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIHBvcnQ6IFt7XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICBjb250YWluZXJQb3J0OiA4MDgwLFxuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICB9XSxcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgZW52OiBbe1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgbmFtZTogJ1NQUklOR19QUk9GSUxFJyxcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIHZhbHVlOiAnY3VycmVuY2llcycsXG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIH0sIHtcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIG5hbWU6ICdEQVRBQkFTRV9VUkwnLFxuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgdmFsdWU6ICdqZGJjOm15c3FsOi8vbXlzcWwtc2VydmljZTozMzA2L21ldGF0cmFkZXInLFxuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICB9LCB7XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICBuYW1lOiAnREFUQUJBU0VfUEFTU1dPUkQnLFxuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgdmFsdWU6IGRiUGFzc3dvcmRUZXJyYWZvcm1WYXJpYWJsZS52YWx1ZSxcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgfSwge1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgbmFtZTogJ1NMQUNLX1dFQkhPT0tfVVJMJyxcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIHZhbHVlOiBzbGFja1RlcnJhZm9ybVZhcmlhYmxlLnZhbHVlLFxuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICB9LCB7XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICBuYW1lOiAnU1VNT19MT0dJQ19XRUJIT09LX1VSTCcsXG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICB2YWx1ZTogc3Vtb0xvZ2ljVGVycmFmb3JtVmFyaWFibGUudmFsdWUsXG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgXSxcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgdm9sdW1lTW91bnQ6IFtcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIHtcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICBuYW1lOiAnYWNjb3VudHMtdm9sdW1lJyxcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICBtb3VudFBhdGg6ICcvYWNjb3VudHMnLFxuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgfSxcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIHtcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICBuYW1lOiAnbW9jaGktZ3JhcGhzLXZvbHVtZScsXG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgbW91bnRQYXRoOiAnL21vY2hpLWdyYXBocycsXG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICB9LFxuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAge1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIG5hbWU6ICdtdC12b2x1bWUnLFxuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIG1vdW50UGF0aDogJy9tdCcsXG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICB9XVxuICAgICAgICAgICAgICAgICAgICAgICAgICAgIH0sXG4gICAgICAgICAgICAgICAgICAgICAgICBdLFxuICAgICAgICAgICAgICAgICAgICAgICAgdm9sdW1lOiBbXG4gICAgICAgICAgICAgICAgICAgICAgICAgICAge1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICBuYW1lOiAnYWNjb3VudHMtdm9sdW1lJyxcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgaG9zdFBhdGg6IHtcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIHBhdGg6ICcvaG9tZS93aWxsL2FjY291bnRzJyxcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgfSxcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICB9LFxuICAgICAgICAgICAgICAgICAgICAgICAgICAgIHtcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgbmFtZTogJ21vY2hpLWdyYXBocy12b2x1bWUnLFxuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICBob3N0UGF0aDoge1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgcGF0aDogJy9ob21lL3dpbGwvbW9jaGktZ3JhcGhzJyxcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgfSxcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICB9LFxuICAgICAgICAgICAgICAgICAgICAgICAgICAgIHtcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgbmFtZTogJ210LXZvbHVtZScsXG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIGhvc3RQYXRoOiB7XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICBwYXRoOiAnL2hvbWUvd2lsbC8uY3hvZmZpY2UvTWV0YVRyYWRlcl81L2RyaXZlX2MvUHJvZ3JhbSBGaWxlcy9NZXRhVHJhZGVyIDUvTVFMNS9GaWxlcycsXG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIH0sXG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgfSxcbiAgICAgICAgICAgICAgICAgICAgICAgIF0sXG4gICAgICAgICAgICAgICAgICAgIH0sXG4gICAgICAgICAgICAgICAgfSxcbiAgICAgICAgICAgIH0sXG4gICAgICAgIH0pO1xuICAgIH1cblxuICAgIHByaXZhdGUgY3JlYXRlREJUZXJyYWZvcm1TZWNyZXQoKSB7XG5cbiAgICAgICAgcmV0dXJuIG5ldyBUZXJyYWZvcm1WYXJpYWJsZSh0aGlzLCBcImRiUGFzc3dvcmRcIiwge1xuICAgICAgICAgICAgdHlwZTogXCJzdHJpbmdcIixcbiAgICAgICAgICAgIGRlc2NyaXB0aW9uOiBcInJvb3QgcGFzc3dvcmQgZm9yIG15c3FsXCIsXG4gICAgICAgICAgICBzZW5zaXRpdmU6IHRydWUsXG4gICAgICAgIH0pO1xuICAgIH1cblxuICAgIHByaXZhdGUgY3JlYXRlU2xhY2tTZWNyZXQoKSB7XG5cbiAgICAgICAgcmV0dXJuIG5ldyBUZXJyYWZvcm1WYXJpYWJsZSh0aGlzLCBcInNsYWNrV2ViSG9va1wiLCB7XG4gICAgICAgICAgICB0eXBlOiBcInN0cmluZ1wiLFxuICAgICAgICAgICAgZGVzY3JpcHRpb246IFwic2xhY2sgd2ViaG9vayB1cmxcIixcbiAgICAgICAgICAgIHNlbnNpdGl2ZTogdHJ1ZSxcbiAgICAgICAgfSk7XG4gICAgfVxuXG4gICAgcHJpdmF0ZSBjcmVhdGVTdW1vTG9naWNTZWNyZXQoKSB7XG5cbiAgICAgICAgcmV0dXJuIG5ldyBUZXJyYWZvcm1WYXJpYWJsZSh0aGlzLCBcInN1bW9Mb2dpY1dlYkhvb2tcIiwge1xuICAgICAgICAgICAgdHlwZTogXCJzdHJpbmdcIixcbiAgICAgICAgICAgIGRlc2NyaXB0aW9uOiBcInN1bW8gbG9naWMgd2ViaG9vayB1cmxcIixcbiAgICAgICAgICAgIHNlbnNpdGl2ZTogdHJ1ZSxcbiAgICAgICAgfSk7XG4gICAgfVxuXG4gICAgLy8gcHJpdmF0ZSBjcmVhdGVIb21lVmFyaWFibGUoKSB7XG4gICAgLy9cbiAgICAvLyAgICAgcmV0dXJuIG5ldyBUZXJyYWZvcm1WYXJpYWJsZSh0aGlzLCBcImt1YmVIb21lXCIsIHtcbiAgICAvLyAgICAgICAgIHR5cGU6IFwic3RyaW5nXCIsXG4gICAgLy8gICAgICAgICBkZXNjcmlwdGlvbjogXCJrdWJlIGhvbWUgZGlyZWN0b3J5XCIsXG4gICAgLy8gICAgICAgICBzZW5zaXRpdmU6IGZhbHNlLFxuICAgIC8vICAgICB9KTtcbiAgICAvLyB9XG59XG4iXX0=