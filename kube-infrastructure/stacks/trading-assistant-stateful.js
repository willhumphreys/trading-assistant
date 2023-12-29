"use strict";
// trading-assistant-stateful.ts
Object.defineProperty(exports, "__esModule", { value: true });
exports.TradingAssistantStatefulStack = void 0;
const cdktf_1 = require("cdktf");
const kubernetes = require("@cdktf/provider-kubernetes");
const provider_1 = require("@cdktf/provider-kubernetes/lib/provider");
const constants_1 = require("../constants");
class TradingAssistantStatefulStack extends cdktf_1.TerraformStack {
    constructor(scope, name) {
        super(scope, name);
        new provider_1.KubernetesProvider(this, 'K8s', {
            "host": "https://192.168.1.202:6443",
            "configPath": "~/.kube/config",
            "configContext": "kubernetes-admin@kubernetes"
        });
        this.createMysqlPVC();
    }
    createMysqlPVC() {
        new kubernetes.namespace.Namespace(this, "trading-assistant-namespace", {
            metadata: {
                name: constants_1.TRADING_ASSISTANT_NAMESPACE
            }
        });
        new kubernetes.persistentVolumeClaim.PersistentVolumeClaim(this, "mysql-pvc", {
            metadata: {
                labels: {
                    app: 'mysql',
                },
                name: 'mysql-pv-claim',
                namespace: 'trading-assistant',
            },
            spec: {
                accessModes: ['ReadWriteOnce'],
                storageClassName: "local-path",
                resources: {
                    requests: {
                        storage: '10Gi',
                    },
                },
            },
        });
    }
}
exports.TradingAssistantStatefulStack = TradingAssistantStatefulStack;
//# sourceMappingURL=data:application/json;base64,eyJ2ZXJzaW9uIjozLCJmaWxlIjoidHJhZGluZy1hc3Npc3RhbnQtc3RhdGVmdWwuanMiLCJzb3VyY2VSb290IjoiIiwic291cmNlcyI6WyJ0cmFkaW5nLWFzc2lzdGFudC1zdGF0ZWZ1bC50cyJdLCJuYW1lcyI6W10sIm1hcHBpbmdzIjoiO0FBQUEsZ0NBQWdDOzs7QUFHaEMsaUNBQXFDO0FBQ3JDLHlEQUF5RDtBQUN6RCxzRUFBMkU7QUFDM0UsNENBQXlEO0FBRXpELE1BQWEsNkJBQThCLFNBQVEsc0JBQWM7SUFFN0QsWUFBWSxLQUFnQixFQUFFLElBQVk7UUFDdEMsS0FBSyxDQUFDLEtBQUssRUFBRSxJQUFJLENBQUMsQ0FBQztRQUVuQixJQUFJLDZCQUFrQixDQUFDLElBQUksRUFBRSxLQUFLLEVBQUU7WUFDaEMsTUFBTSxFQUFFLDRCQUE0QjtZQUNwQyxZQUFZLEVBQUUsZ0JBQWdCO1lBQzlCLGVBQWUsRUFBRSw2QkFBNkI7U0FDakQsQ0FBQyxDQUFDO1FBRUgsSUFBSSxDQUFDLGNBQWMsRUFBRSxDQUFDO0lBQzFCLENBQUM7SUFFTyxjQUFjO1FBRWxCLElBQUksVUFBVSxDQUFDLFNBQVMsQ0FBQyxTQUFTLENBQUMsSUFBSSxFQUFFLDZCQUE2QixFQUFFO1lBQ3BFLFFBQVEsRUFBRTtnQkFDTixJQUFJLEVBQUUsdUNBQTJCO2FBQ3BDO1NBQ0osQ0FBQyxDQUFDO1FBR0gsSUFBSSxVQUFVLENBQUMscUJBQXFCLENBQUMscUJBQXFCLENBQUMsSUFBSSxFQUFFLFdBQVcsRUFBRTtZQUMxRSxRQUFRLEVBQUU7Z0JBQ04sTUFBTSxFQUFFO29CQUNKLEdBQUcsRUFBRSxPQUFPO2lCQUNmO2dCQUNELElBQUksRUFBRSxnQkFBZ0I7Z0JBQ3RCLFNBQVMsRUFBRSxtQkFBbUI7YUFDakM7WUFDRCxJQUFJLEVBQUU7Z0JBQ0YsV0FBVyxFQUFFLENBQUMsZUFBZSxDQUFDO2dCQUM5QixnQkFBZ0IsRUFBRSxZQUFZO2dCQUM5QixTQUFTLEVBQUU7b0JBQ1AsUUFBUSxFQUFFO3dCQUNOLE9BQU8sRUFBRSxNQUFNO3FCQUNsQjtpQkFDSjthQUNKO1NBQ0osQ0FBQyxDQUFBO0lBQ04sQ0FBQztDQVVKO0FBbkRELHNFQW1EQyIsInNvdXJjZXNDb250ZW50IjpbIi8vIHRyYWRpbmctYXNzaXN0YW50LXN0YXRlZnVsLnRzXG5cbmltcG9ydCB7Q29uc3RydWN0fSBmcm9tIFwiY29uc3RydWN0c1wiO1xuaW1wb3J0IHtUZXJyYWZvcm1TdGFja30gZnJvbSBcImNka3RmXCI7XG5pbXBvcnQgKiBhcyBrdWJlcm5ldGVzIGZyb20gXCJAY2RrdGYvcHJvdmlkZXIta3ViZXJuZXRlc1wiO1xuaW1wb3J0IHtLdWJlcm5ldGVzUHJvdmlkZXJ9IGZyb20gXCJAY2RrdGYvcHJvdmlkZXIta3ViZXJuZXRlcy9saWIvcHJvdmlkZXJcIjtcbmltcG9ydCB7VFJBRElOR19BU1NJU1RBTlRfTkFNRVNQQUNFfSBmcm9tIFwiLi4vY29uc3RhbnRzXCI7XG5cbmV4cG9ydCBjbGFzcyBUcmFkaW5nQXNzaXN0YW50U3RhdGVmdWxTdGFjayBleHRlbmRzIFRlcnJhZm9ybVN0YWNrIHtcblxuICAgIGNvbnN0cnVjdG9yKHNjb3BlOiBDb25zdHJ1Y3QsIG5hbWU6IHN0cmluZykge1xuICAgICAgICBzdXBlcihzY29wZSwgbmFtZSk7XG5cbiAgICAgICAgbmV3IEt1YmVybmV0ZXNQcm92aWRlcih0aGlzLCAnSzhzJywge1xuICAgICAgICAgICAgXCJob3N0XCI6IFwiaHR0cHM6Ly8xOTIuMTY4LjEuMjAyOjY0NDNcIixcbiAgICAgICAgICAgIFwiY29uZmlnUGF0aFwiOiBcIn4vLmt1YmUvY29uZmlnXCIsXG4gICAgICAgICAgICBcImNvbmZpZ0NvbnRleHRcIjogXCJrdWJlcm5ldGVzLWFkbWluQGt1YmVybmV0ZXNcIlxuICAgICAgICB9KTtcblxuICAgICAgICB0aGlzLmNyZWF0ZU15c3FsUFZDKCk7XG4gICAgfVxuXG4gICAgcHJpdmF0ZSBjcmVhdGVNeXNxbFBWQygpIHtcblxuICAgICAgICBuZXcga3ViZXJuZXRlcy5uYW1lc3BhY2UuTmFtZXNwYWNlKHRoaXMsIFwidHJhZGluZy1hc3Npc3RhbnQtbmFtZXNwYWNlXCIsIHtcbiAgICAgICAgICAgIG1ldGFkYXRhOiB7XG4gICAgICAgICAgICAgICAgbmFtZTogVFJBRElOR19BU1NJU1RBTlRfTkFNRVNQQUNFXG4gICAgICAgICAgICB9XG4gICAgICAgIH0pO1xuXG5cbiAgICAgICAgbmV3IGt1YmVybmV0ZXMucGVyc2lzdGVudFZvbHVtZUNsYWltLlBlcnNpc3RlbnRWb2x1bWVDbGFpbSh0aGlzLCBcIm15c3FsLXB2Y1wiLCB7XG4gICAgICAgICAgICBtZXRhZGF0YToge1xuICAgICAgICAgICAgICAgIGxhYmVsczoge1xuICAgICAgICAgICAgICAgICAgICBhcHA6ICdteXNxbCcsXG4gICAgICAgICAgICAgICAgfSxcbiAgICAgICAgICAgICAgICBuYW1lOiAnbXlzcWwtcHYtY2xhaW0nLFxuICAgICAgICAgICAgICAgIG5hbWVzcGFjZTogJ3RyYWRpbmctYXNzaXN0YW50JyxcbiAgICAgICAgICAgIH0sXG4gICAgICAgICAgICBzcGVjOiB7XG4gICAgICAgICAgICAgICAgYWNjZXNzTW9kZXM6IFsnUmVhZFdyaXRlT25jZSddLFxuICAgICAgICAgICAgICAgIHN0b3JhZ2VDbGFzc05hbWU6IFwibG9jYWwtcGF0aFwiLFxuICAgICAgICAgICAgICAgIHJlc291cmNlczoge1xuICAgICAgICAgICAgICAgICAgICByZXF1ZXN0czoge1xuICAgICAgICAgICAgICAgICAgICAgICAgc3RvcmFnZTogJzEwR2knLFxuICAgICAgICAgICAgICAgICAgICB9LFxuICAgICAgICAgICAgICAgIH0sXG4gICAgICAgICAgICB9LFxuICAgICAgICB9KVxuICAgIH1cblxuICAgIC8vIHByaXZhdGUgY3JlYXRlSG9tZVZhcmlhYmxlKCkge1xuICAgIC8vXG4gICAgLy8gICAgIHJldHVybiBuZXcgVGVycmFmb3JtVmFyaWFibGUodGhpcywgXCJrdWJlSG9tZVwiLCB7XG4gICAgLy8gICAgICAgICB0eXBlOiBcInN0cmluZ1wiLFxuICAgIC8vICAgICAgICAgZGVzY3JpcHRpb246IFwia3ViZSBob21lIGRpcmVjdG9yeVwiLFxuICAgIC8vICAgICAgICAgc2Vuc2l0aXZlOiBmYWxzZSxcbiAgICAvLyAgICAgfSlcbiAgICAvLyB9XG59Il19