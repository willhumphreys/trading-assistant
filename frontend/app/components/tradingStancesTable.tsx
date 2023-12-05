import {Page, TradingStanceInfo} from '@/app/types/interfaces';
import React from "react";
import UpdateTradingStanceDirection from "@/app/utils/updateTradingStanceDirection";

type Props = {
    tradingStances?: Page<TradingStanceInfo>;
    handleTradingStanceHeaderClick: (newSortColumn: string) => void;
};

export default function TradingStanceTable({tradingStances, handleTradingStanceHeaderClick}: Props) {
    const columns = [
        {name: 'id', entity: ''},
        {name: 'symbol', entity: ''},
        {name: 'new-direction', entity: ''},
        {name: 'direction', entity: ''},
        {name: 'accountSetupGroup / Account', entity: ''},
        {name: 'activeSetups', entity: ''},
        {name: 'disabledSetups', entity: ''},
        {name: 'updated-trades', entity: ''},
    ];


    if (!tradingStances) {
        return <div>Loading trading stances...</div>;
    }

    return (<div>
        <div className="bg-white text-blue-500 px-4 py-2 rounded-lg mb-4 text-left font-bold">
            TradingStances Count: {tradingStances.content.length}
        </div>
        <table className="min-w-full bg-white rounded-lg overflow-hidden">
            <thead className="bg-gray-800 text-white">
            <tr>
                {columns.map((col) => (
                    <th
                        key={col.name}
                        onClick={() => handleTradingStanceHeaderClick(col.entity ? `${col.entity}.${col.name}` : col.name)}
                    >
                        {col.name.charAt(0).toUpperCase() + col.name.slice(1).replace(/([A-Z])/g, ' $1')}
                    </th>
                ))}
            </tr>

            </thead>
            <tbody>
            {tradingStances.content.map((tradingStanceInfo, index) => (
                <tr key={index} className={`${index % 2 === 0 ? 'bg-gray-100' : ''}`}>

                    <td>{tradingStanceInfo.tradingStance.id}</td>
                    <td>{tradingStanceInfo.tradingStance.symbol}</td>
                    <td><UpdateTradingStanceDirection tradingStance={tradingStanceInfo.tradingStance}/>
                    </td>
                    <td>{tradingStanceInfo.tradingStance.direction}</td>
                    <td>{tradingStanceInfo.tradingStance.accountSetupGroups.name}: {tradingStanceInfo.tradingStance.accountSetupGroups.account.name}</td>
                    <td>{tradingStanceInfo.enabledSetupCount}</td>
                    <td>{tradingStanceInfo.disabledSetupCount}</td>
                    <td>{tradingStanceInfo.tradingStance.trades?.length}</td>
                </tr>))}
            </tbody>
        </table>
    </div>);

}
