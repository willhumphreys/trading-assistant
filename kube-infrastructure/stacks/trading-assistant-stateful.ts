// trading-assistant-stateful.ts

import {Construct} from "constructs";
import {TerraformStack} from "cdktf";
import * as kubernetes from "@cdktf/provider-kubernetes";
import {KubernetesProvider} from "@cdktf/provider-kubernetes/lib/provider";
import * as path from "path";
import {DEFAULT_HOME_DIR} from "../constants";

export class TradingAssistantStatefulStack extends TerraformStack {
    constructor(scope: Construct, name: string) {
        super(scope, name);

        new KubernetesProvider(this, 'K8s', {
            configPath: path.join(process.env.HOME || DEFAULT_HOME_DIR, '.kube/config'),
            host: "https://kubernetes.default.svc",
            insecure: true,
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
}