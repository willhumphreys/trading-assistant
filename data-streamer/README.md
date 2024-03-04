```bash
docker run -p 8080:8080 -p 9092:9092 -p 9010:9010 -p 9011:9011 \
--network="host" \
-v /home/will/.cxoffice/MetaTrader_5-2_604/drive_c/Program\ Files/MetaTrader\ 5/MQL5/Files/DWX:/mnt/dwx \
-e METATRADER_DIRECTORY="/mnt/dwx" \
-e ORDERS_TOPIC="DWX_Orders" \
-e SPRING_PROFILE="prod" \
--name my-data-streamer data-streamer
```