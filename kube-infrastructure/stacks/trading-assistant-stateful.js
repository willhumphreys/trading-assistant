"use strict";
// trading-assistant-stateful.ts
Object.defineProperty(exports, "__esModule", { value: true });
exports.TradingAssistantStatefulStack = void 0;
const cdktf_1 = require("cdktf");
const kubernetes = require("@cdktf/provider-kubernetes");
const provider_1 = require("@cdktf/provider-kubernetes/lib/provider");
class TradingAssistantStatefulStack extends cdktf_1.TerraformStack {
    constructor(scope, name) {
        super(scope, name);
        new provider_1.KubernetesProvider(this, 'K8s', {
            "configPath": "~/.kube/config",
            "configContext": "kubernetes-admin@kubernetes"
        });
        this.createMysqlPVC();
    }
    createMysqlPVC() {
        new kubernetes.namespace.Namespace(this, "trading-assistant-namespace", {
            metadata: {
                name: "trading-assistant"
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
//# sourceMappingURL=data:application/json;base64,eyJ2ZXJzaW9uIjozLCJmaWxlIjoidHJhZGluZy1hc3Npc3RhbnQtc3RhdGVmdWwuanMiLCJzb3VyY2VSb290IjoiIiwic291cmNlcyI6WyJ0cmFkaW5nLWFzc2lzdGFudC1zdGF0ZWZ1bC50cyJdLCJuYW1lcyI6W10sIm1hcHBpbmdzIjoiO0FBQUEsZ0NBQWdDOzs7QUFHaEMsaUNBQXFDO0FBQ3JDLHlEQUF5RDtBQUN6RCxzRUFBMkU7QUFFM0UsTUFBYSw2QkFBOEIsU0FBUSxzQkFBYztJQUM3RCxZQUFZLEtBQWdCLEVBQUUsSUFBWTtRQUN0QyxLQUFLLENBQUMsS0FBSyxFQUFFLElBQUksQ0FBQyxDQUFDO1FBRW5CLElBQUksNkJBQWtCLENBQUMsSUFBSSxFQUFFLEtBQUssRUFBRTtZQUNoQyxZQUFZLEVBQUUsZ0JBQWdCO1lBQzlCLGVBQWUsRUFBRSw2QkFBNkI7U0FDakQsQ0FBQyxDQUFDO1FBRUgsSUFBSSxDQUFDLGNBQWMsRUFBRSxDQUFDO0lBQzFCLENBQUM7SUFFTyxjQUFjO1FBRWxCLElBQUksVUFBVSxDQUFDLFNBQVMsQ0FBQyxTQUFTLENBQUMsSUFBSSxFQUFFLDZCQUE2QixFQUFFO1lBQ3BFLFFBQVEsRUFBRTtnQkFDTixJQUFJLEVBQUUsbUJBQW1CO2FBQzVCO1NBQ0osQ0FBQyxDQUFDO1FBR0gsSUFBSSxVQUFVLENBQUMscUJBQXFCLENBQUMscUJBQXFCLENBQUMsSUFBSSxFQUFFLFdBQVcsRUFBRTtZQUMxRSxRQUFRLEVBQUU7Z0JBQ04sTUFBTSxFQUFFO29CQUNKLEdBQUcsRUFBRSxPQUFPO2lCQUNmO2dCQUNELElBQUksRUFBRSxnQkFBZ0I7Z0JBQ3RCLFNBQVMsRUFBRSxtQkFBbUI7YUFDakM7WUFDRCxJQUFJLEVBQUU7Z0JBQ0YsV0FBVyxFQUFFLENBQUMsZUFBZSxDQUFDO2dCQUM5QixnQkFBZ0IsRUFBRSxZQUFZO2dCQUM5QixTQUFTLEVBQUU7b0JBQ1AsUUFBUSxFQUFFO3dCQUNOLE9BQU8sRUFBRSxNQUFNO3FCQUNsQjtpQkFDSjthQUNKO1NBQ0osQ0FBQyxDQUFBO0lBQ04sQ0FBQztDQVVKO0FBakRELHNFQWlEQyIsInNvdXJjZXNDb250ZW50IjpbIi8vIHRyYWRpbmctYXNzaXN0YW50LXN0YXRlZnVsLnRzXG5cbmltcG9ydCB7Q29uc3RydWN0fSBmcm9tIFwiY29uc3RydWN0c1wiO1xuaW1wb3J0IHtUZXJyYWZvcm1TdGFja30gZnJvbSBcImNka3RmXCI7XG5pbXBvcnQgKiBhcyBrdWJlcm5ldGVzIGZyb20gXCJAY2RrdGYvcHJvdmlkZXIta3ViZXJuZXRlc1wiO1xuaW1wb3J0IHtLdWJlcm5ldGVzUHJvdmlkZXJ9IGZyb20gXCJAY2RrdGYvcHJvdmlkZXIta3ViZXJuZXRlcy9saWIvcHJvdmlkZXJcIjtcblxuZXhwb3J0IGNsYXNzIFRyYWRpbmdBc3Npc3RhbnRTdGF0ZWZ1bFN0YWNrIGV4dGVuZHMgVGVycmFmb3JtU3RhY2sge1xuICAgIGNvbnN0cnVjdG9yKHNjb3BlOiBDb25zdHJ1Y3QsIG5hbWU6IHN0cmluZykge1xuICAgICAgICBzdXBlcihzY29wZSwgbmFtZSk7XG5cbiAgICAgICAgbmV3IEt1YmVybmV0ZXNQcm92aWRlcih0aGlzLCAnSzhzJywge1xuICAgICAgICAgICAgXCJjb25maWdQYXRoXCI6IFwifi8ua3ViZS9jb25maWdcIixcbiAgICAgICAgICAgIFwiY29uZmlnQ29udGV4dFwiOiBcImt1YmVybmV0ZXMtYWRtaW5Aa3ViZXJuZXRlc1wiXG4gICAgICAgIH0pO1xuXG4gICAgICAgIHRoaXMuY3JlYXRlTXlzcWxQVkMoKTtcbiAgICB9XG5cbiAgICBwcml2YXRlIGNyZWF0ZU15c3FsUFZDKCkge1xuXG4gICAgICAgIG5ldyBrdWJlcm5ldGVzLm5hbWVzcGFjZS5OYW1lc3BhY2UodGhpcywgXCJ0cmFkaW5nLWFzc2lzdGFudC1uYW1lc3BhY2VcIiwge1xuICAgICAgICAgICAgbWV0YWRhdGE6IHtcbiAgICAgICAgICAgICAgICBuYW1lOiBcInRyYWRpbmctYXNzaXN0YW50XCJcbiAgICAgICAgICAgIH1cbiAgICAgICAgfSk7XG5cblxuICAgICAgICBuZXcga3ViZXJuZXRlcy5wZXJzaXN0ZW50Vm9sdW1lQ2xhaW0uUGVyc2lzdGVudFZvbHVtZUNsYWltKHRoaXMsIFwibXlzcWwtcHZjXCIsIHtcbiAgICAgICAgICAgIG1ldGFkYXRhOiB7XG4gICAgICAgICAgICAgICAgbGFiZWxzOiB7XG4gICAgICAgICAgICAgICAgICAgIGFwcDogJ215c3FsJyxcbiAgICAgICAgICAgICAgICB9LFxuICAgICAgICAgICAgICAgIG5hbWU6ICdteXNxbC1wdi1jbGFpbScsXG4gICAgICAgICAgICAgICAgbmFtZXNwYWNlOiAndHJhZGluZy1hc3Npc3RhbnQnLFxuICAgICAgICAgICAgfSxcbiAgICAgICAgICAgIHNwZWM6IHtcbiAgICAgICAgICAgICAgICBhY2Nlc3NNb2RlczogWydSZWFkV3JpdGVPbmNlJ10sXG4gICAgICAgICAgICAgICAgc3RvcmFnZUNsYXNzTmFtZTogXCJsb2NhbC1wYXRoXCIsXG4gICAgICAgICAgICAgICAgcmVzb3VyY2VzOiB7XG4gICAgICAgICAgICAgICAgICAgIHJlcXVlc3RzOiB7XG4gICAgICAgICAgICAgICAgICAgICAgICBzdG9yYWdlOiAnMTBHaScsXG4gICAgICAgICAgICAgICAgICAgIH0sXG4gICAgICAgICAgICAgICAgfSxcbiAgICAgICAgICAgIH0sXG4gICAgICAgIH0pXG4gICAgfVxuXG4gICAgLy8gcHJpdmF0ZSBjcmVhdGVIb21lVmFyaWFibGUoKSB7XG4gICAgLy9cbiAgICAvLyAgICAgcmV0dXJuIG5ldyBUZXJyYWZvcm1WYXJpYWJsZSh0aGlzLCBcImt1YmVIb21lXCIsIHtcbiAgICAvLyAgICAgICAgIHR5cGU6IFwic3RyaW5nXCIsXG4gICAgLy8gICAgICAgICBkZXNjcmlwdGlvbjogXCJrdWJlIGhvbWUgZGlyZWN0b3J5XCIsXG4gICAgLy8gICAgICAgICBzZW5zaXRpdmU6IGZhbHNlLFxuICAgIC8vICAgICB9KVxuICAgIC8vIH1cbn0iXX0=