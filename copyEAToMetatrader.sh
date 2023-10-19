#!/bin/bash

src="/home/will/code/mochi-java/mochi-what-to-trade3/DWX_Server_MT5.mq5"

dest="/home/will/.cxoffice/MetaTrader_5/drive_c/Program Files/MetaTrader 5/MQL5/Experts/Advisors"
dest2="/home/will/.cxoffice/MetaTrader_5-2/drive_c/Program Files/MetaTrader 5/MQL5/Experts/Advisors"
dest3="/home/will/.cxoffice/MetaTrader_5-3/drive_c/Program Files/MetaTrader 5/MQL5/Experts/Advisors"

cp "$src" "$dest"
cp "$src" "$dest2"
cp "$src" "$dest3"