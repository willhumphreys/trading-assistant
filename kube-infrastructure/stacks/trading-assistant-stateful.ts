// trading-assistant-stateful.ts

import {Construct} from "constructs";
import {TerraformStack} from "cdktf";
import * as kubernetes from "@cdktf/provider-kubernetes";
import {KubernetesProvider} from "@cdktf/provider-kubernetes/lib/provider";

export class TradingAssistantStatefulStack extends TerraformStack {
    constructor(scope: Construct, name: string) {
        super(scope, name);

        new KubernetesProvider(this, 'K8s', {
            host: "https://localhost:6443",
            // configPath: this.createHomeVariable().value,
            // configContext: "docker-desktop",
        });
        this.createMysqlPVC();
    }

    private createMysqlPVC() {

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
                resources: {
                    requests: {
                        storage: '10Gi',
                    },
                },
            },
        })
    }

    // private createHomeVariable() {
    //
    //     return new TerraformVariable(this, "kubeHome", {
    //         type: "string",
    //         description: "kube home directory",
    //         sensitive: false,
    //     })
    // }
}