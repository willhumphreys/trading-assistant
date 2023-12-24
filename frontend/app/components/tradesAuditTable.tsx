import {TradeAudit} from '@/app/types/interfaces';
import {useEffect, useState} from "react";

type Props = {
    trades: TradeAudit[];

};

export default function TradesAuditTable({trades}: Props) {

    type CellColors = { [key: number]: string };
    type PreviousProfits = { [key: number]: number };

    const [previousProfits, setPreviousProfits] = useState<PreviousProfits>({});
    const [cellColors, setCellColors] = useState<CellColors>({});

    useEffect(() => {
        const newCellColors: CellColors = {};
        const newPreviousProfits: PreviousProfits = {};

        trades.forEach((trade) => {
            if (trade.profit === null) return;

            const prevProfit = previousProfits[trade.id] || 0;
            newPreviousProfits[trade.id] = trade.profit;

            if (trade.profit > prevProfit) {
                newCellColors[trade.id] = 'bg-green-400';
            } else if (trade.profit < prevProfit) {
                newCellColors[trade.id] = 'bg-red-400';
            } else {
                newCellColors[trade.id] = '';
            }
        });

        setPreviousProfits(newPreviousProfits);
        setCellColors(newCellColors);

        const timer = setTimeout(() => {
            setCellColors({});
        }, 1000); // 1 second duration

        return () => clearTimeout(timer);
    }, [trades]);

    const columns = [
        {name: 'id', entity: ''},
        {name: 'status', entity: ''},
        //   {name: 'createdDateTime', entity: 'setup'},

        //   {name: 'createdDateTime', entity: ''},
        {name: 'lastUpdatedDateTime', entity: ''},
        {name: 'targetPlaceDateTime', entity: ''},
        {name: 'placedDateTime', entity: ''},
        {name: 'placedPrice', entity: ''},
        {name: 'filledDateTime', entity: ''},
        {name: 'filledPrice', entity: ''},
        {name: 'profit', entity: ''},
        {name: 'closedDateTime', entity: ''},
        {name: 'closedPrice', entity: ''},
        {name: 'closeType', entity: ''},
        {name: 'message', entity: ''},
        {name: 'revisionDate', entity: ''},
        {name: 'revisionType', entity: ''}
    ];


    return (<div>
        <div className="bg-white text-blue-500 px-4 py-2 rounded-lg mb-4 text-left font-bold">
            TradeAudit Count: {trades.length}
        </div>
        <table className="min-w-full bg-white rounded-lg overflow-hidden">
            <thead className="bg-gray-800 text-white">
            <tr>
                {columns.map((col) => (
                    <th key={col.name}>
                        {col.name.charAt(0).toUpperCase() + col.name.slice(1).replace(/([A-Z])/g, ' $1')}
                    </th>
                ))}
            </tr>

            </thead>
            <tbody>
            {trades.map((trade, index) => (<tr key={index} className={`${index % 2 === 0 ? 'bg-gray-100' : ''}`}>

                <td>{trade.id}</td>
                <td>{trade.status}</td>
                <td>{trade.lastUpdatedDateTime}</td>
                <td>{trade.targetPlaceDateTime}</td>
                <td>{trade.placedDateTime}</td>
                <td>{trade.placedPrice}</td>
                <td>{trade.filledDateTime}</td>
                <td>{trade.filledPrice}</td>
                <td className={cellColors[trade.id] || ''}>{trade.profit}</td>
                <td>{trade.closedDateTime}</td>
                <td>{trade.closedPrice}</td>
                <td>{trade.closeType}</td>
                <td>{trade.message}</td>
                <td>{trade.revisionDate}</td>
                <td>{trade.revisionType}</td>
            </tr>))}
            </tbody>
        </table>
    </div>);

}
