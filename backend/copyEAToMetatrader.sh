#!/bin/bash

src="DWX_Server_MT5.mq5"

dest="/home/will/.cxoffice/MetaTrader_5_223/drive_c/Program Files/MetaTrader 5/MQL5/Experts/Advisors"
dest2="/home/will/.cxoffice/MetaTrader_5-2_604/drive_c/Program Files/MetaTrader 5/MQL5/Experts/Advisors"
dest3="/home/will/.cxoffice/MetaTrader_5-3_584/drive_c/Program Files/MetaTrader 5/MQL5/Experts/Advisors"

cp "$src" "$dest"
cp "$src" "$dest2"
cp "$src" "$dest3"