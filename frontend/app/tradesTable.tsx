import {Trade} from './interfaces';
import {useEffect, useState} from "react";

type Props = {
    trades: Trade[];
    handleHeaderClick: (newSortColumn: string) => void;
};

export default function TradesTable({trades, handleHeaderClick}: Props) {

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
        {name: 'symbol', entity: 'setup'},
        {name: 'status', entity: ''},
        //   {name: 'createdDateTime', entity: 'setup'},
        {name: 'rank', entity: 'setup'},
        {name: 'dayOfWeek', entity: 'setup'},
        {name: 'hourOfDay', entity: 'setup'},
        {name: 'stop', entity: 'setup'},
        {name: 'limit', entity: 'setup'},
        {name: 'tickOffset', entity: 'setup'},
        {name: 'tradeDuration', entity: 'setup'},
        {name: 'outOfTime', entity: 'setup'},
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
        {name: 'message', entity: ''}
    ];


    return (<div>
        <div className="bg-white text-blue-500 px-4 py-2 rounded-lg mb-4 text-left font-bold">
            Trade Count: {trades.length}
        </div>
        <table className="min-w-full bg-white rounded-lg overflow-hidden">
            <thead className="bg-gray-800 text-white">
            <tr>
                {columns.map((col) => (
                    <th
                        key={col.name}
                        onClick={() => handleHeaderClick(col.entity ? `${col.entity}.${col.name}` : col.name)}
                    >
                        {col.name.charAt(0).toUpperCase() + col.name.slice(1).replace(/([A-Z])/g, ' $1')}
                    </th>
                ))}
            </tr>

            </thead>
            <tbody>
            {trades.map((trade, index) => (<tr key={index} className={`${index % 2 === 0 ? 'bg-gray-100' : ''}`}>

                <td>{trade.id}</td>
                <td>{trade.setup.symbol}</td>
                <td>{trade.status}</td>
                <td>{trade.setup.rank}</td>
                {/*<td>{trade.setup.createdDateTime}</td>*/}
                <td>{trade.setup.dayOfWeek}</td>
                <td>{trade.setup.hourOfDay}</td>
                <td>{trade.setup.stop}</td>
                <td>{trade.setup.limit}</td>
                <td>{trade.setup.tickOffset}</td>
                <td>{trade.setup.tradeDuration}</td>
                <td>{trade.setup.outOfTime}</td>
                {/*<td>{trade.createdDateTime}</td>*/}
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
            </tr>))}
            </tbody>
        </table>
    </div>);

}
