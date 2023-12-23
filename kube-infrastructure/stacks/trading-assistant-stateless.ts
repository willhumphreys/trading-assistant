// trading-assistant-stateful.ts

import {Construct} from "constructs";
import {TerraformStack, TerraformVariable} from "cdktf";
import * as kubernetes from "@cdktf/provider-kubernetes";
import {KubernetesProvider} from "@cdktf/provider-kubernetes/lib/provider";
import {MYSQL_LABEL, TRADING_ASSISTANT_LABEL} from "../constants";

export class TradingAssistantStatelessStack extends TerraformStack {
    constructor(scope: Construct, name: string) {
        super(scope, name);

        const clientCert = `-----BEGIN CERTIFICATE-----
MIIDQjCCAiqgAwIBAgIIUXXfCha1H6swDQYJKoZIhvcNAQELBQAwFTETMBEGA1UE
AxMKa3ViZXJuZXRlczAeFw0yMzEyMTAxMTQ1MzRaFw0yNDEyMTIxOTA3MDlaMDYx
FzAVBgNVBAoTDnN5c3RlbTptYXN0ZXJzMRswGQYDVQQDExJkb2NrZXItZm9yLWRl
c2t0b3AwggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIBAQDdJPLhaqnxDLxe
cm/k2LjskcUFv7HeGxmTjB5F5xytwppU7+bmDAtwmBHcYCcSAAHGFkVBj/+cLWYZ
Upg39oc0a4VqAbpFcm9bFFNFgg7sJTNG1SubhGXim71A8z+T2lX1C69G4+AyPuDq
PIaBWs9Jfd1pTB+t5yH2YEXY4vl+3iQ3yugydYK8JKGwuBSxGttlROfCW+UzfAA3
nEEMwHWx3DV8WZB/mdHC/iCZmiCxDlS9NnhCGeDx6h59eVzlmdavwlCG98ZBrTV+
O+w+h/sU/qrgDUdRvhT/ooxrYIwQqZdmrt+BBjc28d3cD6MyNLzRNORd1dyWCAos
i6JFHA0DAgMBAAGjdTBzMA4GA1UdDwEB/wQEAwIFoDATBgNVHSUEDDAKBggrBgEF
BQcDAjAMBgNVHRMBAf8EAjAAMB8GA1UdIwQYMBaAFJvAs5lC3sN/+QmqK6TWXRfO
SJjhMB0GA1UdEQQWMBSCEmRvY2tlci1mb3ItZGVza3RvcDANBgkqhkiG9w0BAQsF
AAOCAQEAJnajBbiiu4+LdU41cCRb89uYsAxkDkVoFD5FrOc0N3dfLUne0Lvt51UD
Cl47I2TOuOIpZG9nQo95ZSyJ7BFCFnj7z5xdmFVx2OlE7ehb4mxLDPC4Kr2deuz7
pB06gqdqzMjlowF35rpESQC2vRvSJvGcDq/oxnFyrbEAqVbIunLEyFKsgrRnA8wf
nh5qfDSfgggy9jmAAp7AUHni+iptlJ9vR5ZTnxNCodF609YuERA6pBuVgJc2fFBx
12nygDYwdlIySuuYYbGUyAVZyb09Hfb6tvrNohSCPp8wzCOF8k2Lz5+EakAuJDKZ
fJEuKd1EDaSuEEHtSyr+U2mmD6oSeA==
-----END CERTIFICATE-----
`

        const clientKey = `-----BEGIN RSA PRIVATE KEY-----
MIIEpAIBAAKCAQEA3STy4Wqp8Qy8XnJv5Ni47JHFBb+x3hsZk4weReccrcKaVO/m
5gwLcJgR3GAnEgABxhZFQY//nC1mGVKYN/aHNGuFagG6RXJvWxRTRYIO7CUzRtUr
m4Rl4pu9QPM/k9pV9QuvRuPgMj7g6jyGgVrPSX3daUwfrech9mBF2OL5ft4kN8ro
MnWCvCShsLgUsRrbZUTnwlvlM3wAN5xBDMB1sdw1fFmQf5nRwv4gmZogsQ5UvTZ4
Qhng8eoefXlc5ZnWr8JQhvfGQa01fjvsPof7FP6q4A1HUb4U/6KMa2CMEKmXZq7f
gQY3NvHd3A+jMjS80TTkXdXclggKLIuiRRwNAwIDAQABAoIBAAnAeV1rYLcagqv/
i+OhkvYLr46DV4qd01DoWuVmPaOD815PffUjDtYWPqNjMhXfHjToAoHTocf3UItJ
UyEUo/2xyB5WmSo606JGkS19gL71HP/Aor2m5v9JMt1MXL9eX3AA5efQYLCWiUCL
Drthj8aD4GGmU7dNe/frYukfiTUI7bkVAWOZvNgoR0LbPu8Or3HySc+8nFW2+hfA
ij8aB7rWes3QL2IwW4AWX9cksV37qOnWNyYhgVR3yHQND3OrX8Au8Ei2E3VeoAJS
1MmOmcAuurYbuc3wikQn0195qkb5MyDDOrwvUG1DCbwtt0YXknVxwcdSKsdtakW3
vOFouTECgYEA+ia8mRtU2zlGbwFl9wgwO6Vv9Q7CIxptwGHe5B5C+FgYEVaa0klj
bdgQnlrXOAh7xPOo32QypGMiRmP0FNLwzvTZi3dPqurWwr3ggL5sXrV1bfh+Q0Nt
ebI+Nvr9cd4Vl6iT0fgQQjI5ukeSnI/ySaKDpD+6n3dwh2xE0/7BsVUCgYEA4lCX
vtOHXoeNSULaMgG1bkPXcyRqcw30k+wu7KRpZYiyGjNMi1U9JmchPySr1RxYjNH0
+QkSsGyqtTaeTjV899MelfeH2SEmZ1Q2phtbRgaO0/M2cTe9ix5viWjatYM7w1Ve
813n6DYx8EWzG+kMuIpEL+Z7beAJRpe0l/YiJPcCgYEAyIrdj/E1ajrUmJGKaS0L
XgYXZ2LZiMLbUewkgMUx817MjLndjNCBAcLCL/GrsKQ8dXzR2aW+Y8OF6uCfAULL
A1+QOBsCp4d1fNI0dYIz5wmahvs8XjhkP7gB4Jn/LZZZwSfIugAhKLCTTx+qDhHo
r9e1qavdtZJPiIHn+/y007ECgYBckUZgITk/awZZLuT8i0JghZM8X2rDALRHRNW0
qVMK2qyulsj6PLU0Xf51S1SVaHC3uLEQo6tnSitQz7RUnQAuhcX+5S5Fm4+PuBxa
ONpOfCQzgUmFaZ03qA1LEP5UPAdX0DV0KbbPfktS0c9+3QX8/WECjq1xta9Vlnuh
pGU25QKBgQDSpHYxA1r1FM4EZf5B1L2N6P2t2wy450UJjLgNhu5r98VOv5k9uObH
98TisP+i/3GEQf2PCNNGuDeWGigU3gqANuAWYJZW2yqv20uBLwtcXCtTvj+jdS6R
8TI85LG798RsEH5BZm26MmJe0fQYf3bviLHVFyYjBblMfLYVgtD4Gg==
-----END RSA PRIVATE KEY-----`

        const clusterCaCertificate = `-----BEGIN CERTIFICATE-----
MIIDBTCCAe2gAwIBAgIIWG+f4ABWap8wDQYJKoZIhvcNAQELBQAwFTETMBEGA1UE
AxMKa3ViZXJuZXRlczAeFw0yMzEyMTAxMTQ1MzRaFw0zMzEyMDcxMTUwMzRaMBUx
EzARBgNVBAMTCmt1YmVybmV0ZXMwggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEK
AoIBAQDo+aZpv/xfSlnKuszOD46BnPsy025/DbVdsqJVkEiMPyi1odcZxXBXmG50
DG2iZkMaedOLpS2PHfidHmrc56gHkaDLusL8G7MhPReu/c9ig7rk+oNtmdoMdrbI
AYJXufJQENhZE1KhMRoOqhlrud9ReUX6xNOzvAB8QPQksB9NIy32Ae2/RZebvPJf
gvfr958ZbWlNyDrvqbM34WI7dF26kBvPGKA+YtEURE3ZXBM2eKGH2qD7RnK0NJEt
QTp/oB0h2fF3AtOlEixFW0/4b9LxTB3Fcs24CKgRnMwAYO4gkpbEytyNMjymb6FJ
wumSyirFwzFTBxjdTI1IDutZQTxRAgMBAAGjWTBXMA4GA1UdDwEB/wQEAwICpDAP
BgNVHRMBAf8EBTADAQH/MB0GA1UdDgQWBBSbwLOZQt7Df/kJqiuk1l0XzkiY4TAV
BgNVHREEDjAMggprdWJlcm5ldGVzMA0GCSqGSIb3DQEBCwUAA4IBAQCLADT+iEIT
qJtjC9QWaNkgXXFYfVc2zbeAewnNTpPMPYmn3L7W0iBIAyrYvgqByY/q/C7Pqy7l
O1ZlqAaFTrT9jcaxit6f3Mn4/3VD+hs3VZWio/AYPIoQZ4MW2lFKqXUbXEE1f7CH
YomRi51nuPW0TrXd45Jr7HBzqjGaUQlHGNGl96nOB29LG82V0TTooiH9X/kNnxe3
gvbpggEOR2+SSSySzxRULA57QC64PrPEUO31aqQLkTBCFLqKyJmqHVHkgyEma6HA
C0ttu3JZEkv4HgNX2w1NLAuWp8U9HLCmUk08z/r2lRY7FAlwyWF8NXssWgFxHjG1
D5FiIXKqwE/4
-----END CERTIFICATE-----
`

        new KubernetesProvider(this, 'K8s', {
            host: "https://localhost:6443",
            clientCertificate: clientCert,
            clientKey: clientKey,
            clusterCaCertificate: clusterCaCertificate,
        });


        let adminPassword = this.createDBTerraformSecret();
        this.createTradingAssistantDeployment(this.createSlackSecret(), this.createSumoLogicSecret(), adminPassword);
        this.createTradingAssistantService();
        this.createMysqlDeployment(adminPassword);
        this.createMySqlService();
    }


    private createMySqlService() {
        new kubernetes.service.Service(this, "mysql-service", {
            metadata: {
                labels: {
                    app: MYSQL_LABEL,
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
                    app: MYSQL_LABEL,
                },
                type: 'LoadBalancer',
            },
        });
    }

    private createMysqlDeployment(adminPassword: TerraformVariable) {
        new kubernetes.deployment.Deployment(this, MYSQL_LABEL, {
            metadata: {
                labels: {
                    app: MYSQL_LABEL,
                },
                name: 'mysql-container',
                namespace: 'trading-assistant',
            },
            spec: {
                replicas: '1',
                selector: {
                    matchLabels: {
                        app: MYSQL_LABEL,
                    },
                },
                template: {
                    metadata: {
                        labels: {
                            app: MYSQL_LABEL,
                        },
                    },
                    spec: {
                        container: [
                            {
                                image: 'mysql:latest',
                                name: MYSQL_LABEL,
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
                                    }],
                            },
                        ],
                        volume: [
                            {
                                name: 'mysql-data',
                                persistentVolumeClaim: {
                                    claimName: 'mysql-pv-claim'
                                }
                                ,
                            }]

                    },
                },
            },
        });
    }

    private createTradingAssistantService() {
        new kubernetes.service.Service(this, "trading-assistant-service", {
            metadata: {
                labels: {
                    app: TRADING_ASSISTANT_LABEL,
                    test: "IAmTest",
                    test2: "IAmTest2"
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
                    app: TRADING_ASSISTANT_LABEL,
                },
                type: 'LoadBalancer',
            },
        });
    }

    private createTradingAssistantDeployment(slackTerraformVariable: TerraformVariable, sumoLogicTerraformVariable: TerraformVariable, dbPasswordTerraformVariable: TerraformVariable) {
        new kubernetes.deployment.Deployment(this, TRADING_ASSISTANT_LABEL, {
            metadata: {
                labels: {
                    app: TRADING_ASSISTANT_LABEL,
                },
                name: TRADING_ASSISTANT_LABEL,
                namespace: 'trading-assistant',
            },
            spec: {
                replicas: '1',
                selector: {
                    matchLabels: {
                        app: TRADING_ASSISTANT_LABEL,
                    },
                },
                template: {
                    metadata: {
                        labels: {
                            app: TRADING_ASSISTANT_LABEL,
                        },
                    },
                    spec: {
                        container: [
                            {
                                image: 'ghcr.io/willhumphreys/trading-assistant:latest',
                                name: TRADING_ASSISTANT_LABEL,
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
                                    }]
                            },
                        ],
                        volume: [
                            {
                                name: 'accounts-volume',
                                hostPath: {
                                    path: '/run/desktop/mnt/host/c/Users/user/IdeaProjects/darwinex-executor/accounts',
                                },
                            },
                            {
                                name: 'mochi-graphs-volume',
                                hostPath: {
                                    path: '/run/desktop/mnt/host/c/Users/user/IdeaProjects/darwinex-executor/mochi-graphs',
                                },
                            },
                            {
                                name: 'mt-volume',
                                hostPath: {
                                    path: '/run/desktop/mnt/host/c/Users/user/AppData/Roaming/MetaQuotes/Terminal/33BCAFEA70BFE62B7C2BC1AAFDFEEDB6/MQL5/Files',
                                },
                            },
                        ],
                    },
                },
            },
        });
    }

    private createDBTerraformSecret() {

        return new TerraformVariable(this, "dbPassword", {
            type: "string",
            description: "root password for mysql",
            sensitive: true,
        });
    }

    private createSlackSecret() {

        return new TerraformVariable(this, "slackWebHook", {
            type: "string",
            description: "slack webhook url",
            sensitive: true,
        });
    }

    private createSumoLogicSecret() {

        return new TerraformVariable(this, "sumoLogicWebHook", {
            type: "string",
            description: "sumo logic webhook url",
            sensitive: true,
        });
    }

    // private createHomeVariable() {
    //
    //     return new TerraformVariable(this, "kubeHome", {
    //         type: "string",
    //         description: "kube home directory",
    //         sensitive: false,
    //     });
    // }
}
