import {Construct} from "constructs";
import {TerraformStack} from "cdktf";
import * as kubernetes from "@cdktf/provider-kubernetes";
import {KubernetesProvider} from "@cdktf/provider-kubernetes/lib/provider";
import * as path from "path";

export class TradingAssistantStatefulStack extends TerraformStack {
    constructor(scope: Construct, name: string) {
        super(scope, name);

        new KubernetesProvider(this, 'K8s', {
            configPath: path.join(process.env.HOME || '/home/will', '.kube/config'),
        });
        //
        //
        // // Load environment variables from .env file
        // const env = dotenv.config().parsed;
        //
        // if (!env) {
        //     throw new Error('Failed to load .env file');
        // }


        new kubernetes.persistentVolumeClaim.PersistentVolumeClaim(this, "mysql-pvc", {
            metadata: {
                labels: {
                    app: 'mysql',
                },
                name: 'mysql-pv-claim',
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