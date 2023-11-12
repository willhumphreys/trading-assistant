import React, {useCallback} from 'react';
import {AccountSetupGroups, Page, Query, Trade, TradeAudit, TradingStanceInfo} from "@/app/interfaces";
import {fetchTrades} from '@/app/fetchTrades';
import {fetchTradeAudits} from "@/app/fetchTradeAudits";
import {fetchTradingStances} from "@/app/fetchTradingStances";
import {fetchAccountSetupGroups} from "@/app/fetchAccountSetupGroups";

interface UseDataFetchingParams {
    setAccountSetupGroups: React.Dispatch<React.SetStateAction<AccountSetupGroups[]>>;
    setTrades: React.Dispatch<React.SetStateAction<Trade[]>>;
    setTradeAudits: React.Dispatch<React.SetStateAction<TradeAudit[]>>;
    setTradingStances: React.Dispatch<React.SetStateAction<Page<TradingStanceInfo> | undefined>>;
    query: Query;
    sortColumn: string;
    sortDirection: string;
    tradeAuditId: number;
}

export function useDataFetching({
                                    setAccountSetupGroups,
                                    setTrades,
                                    setTradeAudits,
                                    setTradingStances,
                                    query,
                                    sortColumn,
                                    sortDirection,
                                    tradeAuditId
                                }: UseDataFetchingParams) {

    const fetchAll = useCallback(async () => {
        // Fetch Account Setup Groups
        const accountSetupGroups = await fetchAccountSetupGroups();
        if (accountSetupGroups) {
            setAccountSetupGroups(accountSetupGroups);
        }

        // Fetch Trades
        const trades = await fetchTrades(query, sortColumn, sortDirection);
        if (trades) {
            setTrades(trades);
        }

        // Fetch Trade Audits
        const tradeAudits = await fetchTradeAudits(tradeAuditId);
        if (tradeAudits) {
            setTradeAudits(tradeAudits);
        }

        // Fetch Trading Stances
        const tradingStances = await fetchTradingStances(0, 100, sortColumn, sortDirection);
        if (tradingStances) {
            setTradingStances(tradingStances);
        }
    }, [setAccountSetupGroups, setTrades, setTradeAudits, setTradingStances, query, sortColumn, sortDirection, tradeAuditId]);

    const updateTrades = useCallback(async () => {
        const trades = await fetchTrades(query, sortColumn, sortDirection);
        if (trades) {
            setTrades(trades);
        }
    }, [query, sortColumn, sortDirection]);

    return {fetchAll, updateTrades};
}
