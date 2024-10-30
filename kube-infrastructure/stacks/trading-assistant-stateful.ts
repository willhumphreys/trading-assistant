// trading-assistant-stateful.ts

import {Construct} from "constructs";
import {TerraformStack} from "cdktf";
import * as kubernetes from "@cdktf/provider-kubernetes";
import {KubernetesProvider} from "@cdktf/provider-kubernetes/lib/provider";
import {TRADING_ASSISTANT_NAMESPACE} from "../constants";

export class TradingAssistantStatefulStack extends TerraformStack {

    constructor(scope: Construct, name: string) {
        super(scope, name);

        new KubernetesProvider(this, 'K8s', {
            host: "https://192.168.1.89:6443",
            token: process.env.TOKEN,
            insecure: true,
            ignoreAnnotations: ["metallb\\.universe\\.tf/ip-allocated-from-pool"],
            // "configPath": "~/.kube/config",
            // "configContext": "kubernetes-admin@kubernetes"
        });

        this.createMysqlPVC();
    }

    private createMysqlPVC() {

        new kubernetes.namespace.Namespace(this, "trading-assistant-namespace", {
            metadata: {
                name: TRADING_ASSISTANT_NAMESPACE
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