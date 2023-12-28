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
//# sourceMappingURL=data:application/json;base64,eyJ2ZXJzaW9uIjozLCJmaWxlIjoidHJhZGluZy1hc3Npc3RhbnQtc3RhdGVmdWwuanMiLCJzb3VyY2VSb290IjoiIiwic291cmNlcyI6WyJ0cmFkaW5nLWFzc2lzdGFudC1zdGF0ZWZ1bC50cyJdLCJuYW1lcyI6W10sIm1hcHBpbmdzIjoiO0FBQUEsZ0NBQWdDOzs7QUFHaEMsaUNBQXFDO0FBQ3JDLHlEQUF5RDtBQUN6RCxzRUFBMkU7QUFDM0UsNENBQXlEO0FBRXpELE1BQWEsNkJBQThCLFNBQVEsc0JBQWM7SUFFN0QsWUFBWSxLQUFnQixFQUFFLElBQVk7UUFDdEMsS0FBSyxDQUFDLEtBQUssRUFBRSxJQUFJLENBQUMsQ0FBQztRQUVuQixJQUFJLDZCQUFrQixDQUFDLElBQUksRUFBRSxLQUFLLEVBQUU7WUFDaEMsWUFBWSxFQUFFLGdCQUFnQjtZQUM5QixlQUFlLEVBQUUsNkJBQTZCO1NBQ2pELENBQUMsQ0FBQztRQUVILElBQUksQ0FBQyxjQUFjLEVBQUUsQ0FBQztJQUMxQixDQUFDO0lBRU8sY0FBYztRQUVsQixJQUFJLFVBQVUsQ0FBQyxTQUFTLENBQUMsU0FBUyxDQUFDLElBQUksRUFBRSw2QkFBNkIsRUFBRTtZQUNwRSxRQUFRLEVBQUU7Z0JBQ04sSUFBSSxFQUFFLHVDQUEyQjthQUNwQztTQUNKLENBQUMsQ0FBQztRQUdILElBQUksVUFBVSxDQUFDLHFCQUFxQixDQUFDLHFCQUFxQixDQUFDLElBQUksRUFBRSxXQUFXLEVBQUU7WUFDMUUsUUFBUSxFQUFFO2dCQUNOLE1BQU0sRUFBRTtvQkFDSixHQUFHLEVBQUUsT0FBTztpQkFDZjtnQkFDRCxJQUFJLEVBQUUsZ0JBQWdCO2dCQUN0QixTQUFTLEVBQUUsbUJBQW1CO2FBQ2pDO1lBQ0QsSUFBSSxFQUFFO2dCQUNGLFdBQVcsRUFBRSxDQUFDLGVBQWUsQ0FBQztnQkFDOUIsZ0JBQWdCLEVBQUUsWUFBWTtnQkFDOUIsU0FBUyxFQUFFO29CQUNQLFFBQVEsRUFBRTt3QkFDTixPQUFPLEVBQUUsTUFBTTtxQkFDbEI7aUJBQ0o7YUFDSjtTQUNKLENBQUMsQ0FBQTtJQUNOLENBQUM7Q0FVSjtBQWxERCxzRUFrREMiLCJzb3VyY2VzQ29udGVudCI6WyIvLyB0cmFkaW5nLWFzc2lzdGFudC1zdGF0ZWZ1bC50c1xuXG5pbXBvcnQge0NvbnN0cnVjdH0gZnJvbSBcImNvbnN0cnVjdHNcIjtcbmltcG9ydCB7VGVycmFmb3JtU3RhY2t9IGZyb20gXCJjZGt0ZlwiO1xuaW1wb3J0ICogYXMga3ViZXJuZXRlcyBmcm9tIFwiQGNka3RmL3Byb3ZpZGVyLWt1YmVybmV0ZXNcIjtcbmltcG9ydCB7S3ViZXJuZXRlc1Byb3ZpZGVyfSBmcm9tIFwiQGNka3RmL3Byb3ZpZGVyLWt1YmVybmV0ZXMvbGliL3Byb3ZpZGVyXCI7XG5pbXBvcnQge1RSQURJTkdfQVNTSVNUQU5UX05BTUVTUEFDRX0gZnJvbSBcIi4uL2NvbnN0YW50c1wiO1xuXG5leHBvcnQgY2xhc3MgVHJhZGluZ0Fzc2lzdGFudFN0YXRlZnVsU3RhY2sgZXh0ZW5kcyBUZXJyYWZvcm1TdGFjayB7XG5cbiAgICBjb25zdHJ1Y3RvcihzY29wZTogQ29uc3RydWN0LCBuYW1lOiBzdHJpbmcpIHtcbiAgICAgICAgc3VwZXIoc2NvcGUsIG5hbWUpO1xuXG4gICAgICAgIG5ldyBLdWJlcm5ldGVzUHJvdmlkZXIodGhpcywgJ0s4cycsIHtcbiAgICAgICAgICAgIFwiY29uZmlnUGF0aFwiOiBcIn4vLmt1YmUvY29uZmlnXCIsXG4gICAgICAgICAgICBcImNvbmZpZ0NvbnRleHRcIjogXCJrdWJlcm5ldGVzLWFkbWluQGt1YmVybmV0ZXNcIlxuICAgICAgICB9KTtcblxuICAgICAgICB0aGlzLmNyZWF0ZU15c3FsUFZDKCk7XG4gICAgfVxuXG4gICAgcHJpdmF0ZSBjcmVhdGVNeXNxbFBWQygpIHtcblxuICAgICAgICBuZXcga3ViZXJuZXRlcy5uYW1lc3BhY2UuTmFtZXNwYWNlKHRoaXMsIFwidHJhZGluZy1hc3Npc3RhbnQtbmFtZXNwYWNlXCIsIHtcbiAgICAgICAgICAgIG1ldGFkYXRhOiB7XG4gICAgICAgICAgICAgICAgbmFtZTogVFJBRElOR19BU1NJU1RBTlRfTkFNRVNQQUNFXG4gICAgICAgICAgICB9XG4gICAgICAgIH0pO1xuXG5cbiAgICAgICAgbmV3IGt1YmVybmV0ZXMucGVyc2lzdGVudFZvbHVtZUNsYWltLlBlcnNpc3RlbnRWb2x1bWVDbGFpbSh0aGlzLCBcIm15c3FsLXB2Y1wiLCB7XG4gICAgICAgICAgICBtZXRhZGF0YToge1xuICAgICAgICAgICAgICAgIGxhYmVsczoge1xuICAgICAgICAgICAgICAgICAgICBhcHA6ICdteXNxbCcsXG4gICAgICAgICAgICAgICAgfSxcbiAgICAgICAgICAgICAgICBuYW1lOiAnbXlzcWwtcHYtY2xhaW0nLFxuICAgICAgICAgICAgICAgIG5hbWVzcGFjZTogJ3RyYWRpbmctYXNzaXN0YW50JyxcbiAgICAgICAgICAgIH0sXG4gICAgICAgICAgICBzcGVjOiB7XG4gICAgICAgICAgICAgICAgYWNjZXNzTW9kZXM6IFsnUmVhZFdyaXRlT25jZSddLFxuICAgICAgICAgICAgICAgIHN0b3JhZ2VDbGFzc05hbWU6IFwibG9jYWwtcGF0aFwiLFxuICAgICAgICAgICAgICAgIHJlc291cmNlczoge1xuICAgICAgICAgICAgICAgICAgICByZXF1ZXN0czoge1xuICAgICAgICAgICAgICAgICAgICAgICAgc3RvcmFnZTogJzEwR2knLFxuICAgICAgICAgICAgICAgICAgICB9LFxuICAgICAgICAgICAgICAgIH0sXG4gICAgICAgICAgICB9LFxuICAgICAgICB9KVxuICAgIH1cblxuICAgIC8vIHByaXZhdGUgY3JlYXRlSG9tZVZhcmlhYmxlKCkge1xuICAgIC8vXG4gICAgLy8gICAgIHJldHVybiBuZXcgVGVycmFmb3JtVmFyaWFibGUodGhpcywgXCJrdWJlSG9tZVwiLCB7XG4gICAgLy8gICAgICAgICB0eXBlOiBcInN0cmluZ1wiLFxuICAgIC8vICAgICAgICAgZGVzY3JpcHRpb246IFwia3ViZSBob21lIGRpcmVjdG9yeVwiLFxuICAgIC8vICAgICAgICAgc2Vuc2l0aXZlOiBmYWxzZSxcbiAgICAvLyAgICAgfSlcbiAgICAvLyB9XG59Il19