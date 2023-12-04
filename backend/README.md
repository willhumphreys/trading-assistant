#Docker

```bash
docker build -t trading-assistant .
```

```bash
docker run --env-file .env --network="host" trading-assistant
```

#JMX
To connect via JMX

```
service:jmx:rmi:///jndi/rmi://192.168.1.202:9010/jmxrmi
```

VisualVM is installed here

```
/opt/visualvm/bin/visualvm
```