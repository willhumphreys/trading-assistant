#Docker

```bash
docker build -t trading-assistant .
```

```bash
docker run --env-file .env \
  --env SPRING_PROFILE=dev \
  --network="host" \
  -p 9010:9010 -p 9011:9011 -p 8080:8080 -p 3306:3306 \
  -v /home/will/code/darwinex-client/backend/accounts:/accounts \
  -v /home/will/code/darwinex-client/backend/mochi-graphs:/mochi-graphs \
  -v /home/will/.cxoffice/MetaTrader_5-3_584/drive_c/Program\ Files/MetaTrader\ 5/MQL5/Files/DWX:/home/will/.cxoffice/MetaTrader_5-3_584/drive_c/Program\ Files/MetaTrader\ 5/MQL5/Files/DWX \
  trading-assistant
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