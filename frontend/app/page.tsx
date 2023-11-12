'use client'
import {useEffect, useState} from 'react';
import {AccountSetupGroups, Page, Query, Trade, TradeAudit, TradingStanceInfo} from "@/app/interfaces";
import TradesTable from "@/app/tradesTable";
import TradesAuditTable from "@/app/tradesAuditTable";
import QueryBuilder from "@/app/queryBuilder";
import {fetchTrades} from '@/app/fetchTrades';
import AccountSelector from '@/app/accountSelector';
import {useWebSocketClient} from '@/app/useWebSocketClient';
import {fetchTradeAudits} from "@/app/fetchTradeAudits";
import {fetchTradingStances} from "@/app/fetchTradingStances";
import TradingStanceTable from "@/app/tradingStancesTable";
import {fetchAccountSetupGroups} from "@/app/fetchAccountSetupGroups";

export default function FetchTradesClient() {

    const {client, sendHelloMessage, tickMessage, orderMessage} = useWebSocketClient();

    const [accountSetupGroups, setAccountSetupGroups] = useState<AccountSetupGroups[]>([]);

    const [selectedAccountSetupGroups, setSelectedAccountSetupGroups] = useState<AccountSetupGroups>();

    const [trades, setTrades] = useState<Trade[]>([]);
    const [tradeAudits, setTradeAudits] = useState<TradeAudit[]>([]);
    const [tradingStances, setTradingStances] = useState<Page<TradingStanceInfo>>();

    const [sortColumn, setSortColumn] = useState('id');  // Default sort column
    const [sortDirection, setSortDirection] = useState('ASC');  // Default sort direction

    const [sortColumnTS, setSortColumnTS] = useState('id');  // Default sort column
    const [sortDirectionTS, setSortDirectionTS] = useState('ASC');  // Default sort direction
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


    const fetchAllAccountSetupGroups = async () => {
        const fetchedAccountSetupGroups = await fetchAccountSetupGroups();
        if (fetchedAccountSetupGroups !== null) {
            setAccountSetupGroups(fetchedAccountSetupGroups);
        }
    }

    const fetchTradeAuditsWithId = async () => {
        const fetchedTradeAudits = await fetchTradeAudits(tradeAuditId);
        console.log(`tradeAuditId: ${tradeAuditId}`)
        if (fetchedTradeAudits !== null) {
            setTradeAudits(fetchedTradeAudits);
        }
    };

    const fetchTradingStancesWithId = async () => {
        const fetchedTradingStances = await fetchTradingStances(0, 100, 'symbol', 'asc', selectedAccountSetupGroups);
        console.log(`tradingStanceId: ${JSON.stringify(selectedAccountSetupGroups)}`)
        if (fetchedTradingStances !== null) {
            setTradingStances(fetchedTradingStances);
        }
    };


    const updateTrades = async () => {
        const fetchedTrades = await fetchTrades(query, sortColumn, sortDirection);
        if (fetchedTrades !== null) {
            setTrades(fetchedTrades);
        }
    };

    useEffect(() => {
        updateTrades();
    }, [sortColumn, sortDirection, query]);

    useEffect(() => {
        fetchAllAccountSetupGroups()
        fetchTradeAuditsWithId();
        fetchTradingStancesWithId();
    }, [query]);

    useEffect(() => {
        console.log(orderMessage)
        if (orderMessage.id && orderMessage.field === 'profitAndLoss') {
            console.log('here')
            const updatedTrades = trades.map((trade) => {
                if (trade.id === orderMessage.id) {
                    return {
                        ...trade,
                        profit: parseFloat(orderMessage.value), // assuming the value is a string that can be parsed to float
                    };
                }
                return trade;
            });
            setTrades(updatedTrades);
        }
    }, [orderMessage]);

    const handleHeaderClick = (newSortColumn: string) => {
        if (newSortColumn === sortColumn) {
            setSortDirection(sortDirection === 'ASC' ? 'DESC' : 'ASC');
        } else {
            setSortColumn(newSortColumn);
            setSortDirection('ASC');
        }
        updateTrades();
    };

    const handleAuditHeaderClick = (tradeAuditId: number) => {
        console.log(`click click tradeAuditId: ${tradeAuditId}`)
        setTradeAuditId(tradeAuditId);
        fetchTradeAuditsWithId();
    };

    return (<section className="bg-gray-100 py-10 min-h-screen">

        <div className="container mx-auto">
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


