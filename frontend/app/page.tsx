'use client'
import React, {useEffect, useState} from 'react';
import {
    AccountSetupGroups,
    Page,
    Query,
    SortConfig,
    Trade,
    TradeAudit,
    TradingStanceInfo
} from "@/app/types/interfaces";
import TradesTable from "@/app/components/tradesTable";
import TradesAuditTable from "@/app/components/tradesAuditTable";
import QueryBuilder from "@/app/utils/queryBuilder";
import AccountSelector from '@/app/components/accountSelector';
import {fetchTradeAudits} from "@/app/utils/fetchTradeAudits";
import TradingStanceTable from "@/app/components/tradingStancesTable";
import {useDataFetching} from "@/app/utils/dataFetching";
import {useWebSocketMessages} from "@/app/hooks/useWebSocketMessages";
import EnvironmentLabel from "@/app/components/EnvironmentLabel";

export default function FetchTradesClient() {

    const [accountSetupGroups, setAccountSetupGroups] = useState<AccountSetupGroups[]>([]);

    const [selectedAccountSetupGroups, setSelectedAccountSetupGroups] = useState<AccountSetupGroups | undefined>();

    const [trades, setTrades] = useState<Trade[]>([]);
    const [tradeAudits, setTradeAudits] = useState<TradeAudit[]>([]);
    const [tradingStances, setTradingStances] = useState<Page<TradingStanceInfo>>();

    const [tradesSortConfig, setTradesSortConfig] = useState<SortConfig>({
        column: 'id', // Default sort column
        direction: 'ASC' // Default sort direction
    });

    const [tradingStanceSortConfig, setTradingStanceSortConfig] = useState<SortConfig>({
        column: 'id', // Default sort column
        direction: 'ASC' // Default sort direction
    });

    const handleSortChange = (newSortColumn: string) => {
        setTradesSortConfig(prevConfig => {
            if (newSortColumn === prevConfig.column) {
                return {
                    ...prevConfig,
                    direction: prevConfig.direction === 'ASC' ? 'DESC' : 'ASC'
                };
            } else {
                return {column: newSortColumn, direction: 'ASC'};
            }
        });
    };


    const [tradeAuditId, setTradeAuditId] = useState(1);

    const [query, setQuery] = useState<Query>({
        id: null, account: {id: null}, setup: {
            id: null,
            createdDateTime: '',
            symbol: '',
            rank: null,
            dayOfWeek: null,
            hourOfDay: null,
            stop: null,
            limit: null,
            tickOffset: null,
            tradeDuration: null,
            outOfTime: null
        },
        status: '',
        createdDateTime: '',
        lastUpdatedDateTime: '',
        placedDateTime: '',
        targetPlaceDateTime: '',
        placedPrice: '',
        filledDateTime: '',
        filledPrice: '',
        closedDateTime: '',
        closedPrice: '',
        closeType: '',
        message: ''
    });


    const fetchTradeAuditsWithId = async () => {
        const fetchedTradeAudits = await fetchTradeAudits(tradeAuditId);
        console.log(`tradeAuditId: ${tradeAuditId}`)
        if (fetchedTradeAudits !== null) {
            setTradeAudits(fetchedTradeAudits);
        }
    };


    // Custom hooks for data fetching and WebSocket message handling
    const {fetchAll, updateTrades} = useDataFetching({
        setAccountSetupGroups,
        setTrades,
        setTradeAudits,
        setTradingStances,
        query,
        tradesSortConfig: tradesSortConfig,
        tradingStanceSortConfig: tradingStanceSortConfig,
        tradeAuditId,
        selectedAccountSetupGroups
    });

    const {tickMessage, orderMessage, sendHelloMessage} = useWebSocketMessages(setTrades);


    useEffect(() => {
        fetchAll();
    }, [query, tradesSortConfig, tradeAuditId, fetchAll]);

    const handleHeaderClick = (newSortColumn: string) => {
        handleSortChange(newSortColumn);
        updateTrades();
    };


    const handleAuditHeaderClick = (tradeAuditId: number) => {
        console.log(`click click tradeAuditId: ${tradeAuditId}`)
        setTradeAuditId(tradeAuditId);
        fetchTradeAuditsWithId();
    };

    return (<section className="bg-gray-100 py-10 min-h-screen">

        <div className="container mx-auto">
            <EnvironmentLabel/>
            <div className="w-full h-16 bg-gray-700 flex items-center pl-6 space-x-4">
                <div>
                    <AccountSelector accountSetupGroups={accountSetupGroups}
                                     setSelectedAccountSetupGroups={setSelectedAccountSetupGroups}
                                     query={query}
                                     setQuery={setQuery}
                    />

                </div>
                <div className="text-white">
                    account: {selectedAccountSetupGroups?.account.name}
                </div>
                <div className="text-white text-xl">{tickMessage.id}:{tickMessage.field}:{tickMessage.value}</div>
                {/*<div className="text-white text-xl">{orderMessage.id}:{orderMessage.field}:{orderMessage.value}</div>*/}
            </div>


            <h1 className="text-3xl font-bold mb-4">Trades</h1>
            <div className="mb-6 flex flex-wrap items-center">
                <QueryBuilder query={query} setQuery={setQuery}/>
                <button onClick={updateTrades}
                        className="bg-blue-500 hover:bg-blue-700 text-white font-bold py-2 px-4 rounded">
                    Apply Filters
                </button>

                <button onClick={sendHelloMessage}
                        className="bg-green-500 hover:bg-green-700 text-white font-bold py-2 px-4 rounded ml-4">
                    Connect WebSocket
                </button>

            </div>

            <div>
                <TradesTable
                    trades={trades}
                    handleHeaderClick={handleHeaderClick}
                    handleAuditHeaderClick={handleAuditHeaderClick}
                />
            </div>
            <div>
                <TradesAuditTable
                    trades={tradeAudits}
                />
            </div>
            <div>
                <TradingStanceTable tradingStances={tradingStances}/>
            </div>

        </div>
    </section>);
}


