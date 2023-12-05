import React, {useCallback} from 'react';
import {
    AccountSetupGroups,
    Page,
    Query,
    SortConfig,
    Trade,
    TradeAudit,
    TradingStanceInfo
} from "@/app/types/interfaces";
import {fetchTrades} from '@/app/utils/fetchTrades';
import {fetchTradeAudits} from "@/app/utils/fetchTradeAudits";
import {fetchTradingStances} from "@/app/utils/fetchTradingStances";
import {fetchAccountSetupGroups} from "@/app/utils/fetchAccountSetupGroups";

interface UseDataFetchingParams {
    setAccountSetupGroups: React.Dispatch<React.SetStateAction<AccountSetupGroups[]>>;
    setSelectedAccountSetupGroups: React.Dispatch<React.SetStateAction<AccountSetupGroups | undefined>>;
    selectedAccountSetupGroups: AccountSetupGroups | undefined;
    setTrades: React.Dispatch<React.SetStateAction<Trade[]>>;
    setTradeAudits: React.Dispatch<React.SetStateAction<TradeAudit[]>>;
    setTradingStances: React.Dispatch<React.SetStateAction<Page<TradingStanceInfo> | undefined>>;
    query: Query;
    sortConfig: SortConfig;
    tradeAuditId: number;
}

export function useDataFetching({
                                    setAccountSetupGroups,
                                    setTrades,
                                    setTradeAudits,
                                    setTradingStances,
                                    query,
                                    sortConfig,
                                    tradeAuditId,
                                    setSelectedAccountSetupGroups,
                                    selectedAccountSetupGroups
                                }: UseDataFetchingParams) {

    const fetchAll = useCallback(async () => {
        // Fetch Account Setup Groups
        const accountSetupGroups = await fetchAccountSetupGroups();
        if (accountSetupGroups) {
            setAccountSetupGroups(accountSetupGroups);
        }

        const trades = await fetchTrades(query, sortConfig);
        if (trades) {
            setTrades(trades);
        }

        const tradeAudits = await fetchTradeAudits(tradeAuditId);
        if (tradeAudits) {
            setTradeAudits(tradeAudits);
        }

        const tradingStances = await fetchTradingStances(0, 100, sortConfig, selectedAccountSetupGroups);
        if (tradingStances) {
            setTradingStances(tradingStances);
        }
    }, [setAccountSetupGroups, setTrades, setTradeAudits, setTradingStances, query, sortConfig, tradeAuditId]);

    const updateTrades = useCallback(async () => {
        const trades = await fetchTrades(query, sortConfig);
        if (trades) {
            setTrades(trades);
        }
    }, [query, sortConfig]);

    return {fetchAll, updateTrades};
}
