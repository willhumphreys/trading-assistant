import {TradingStance} from './interfaces';
import {useEffect} from "react";

type Props = {
    tradingStances: TradingStance[];

};

export default function TradingStanceTable({tradingStances}: Props) {


    useEffect(() => {


    }, [tradingStances]);

    const columns = [
        {name: 'id', entity: ''},
        {name: 'symbol', entity: ''},
        {name: 'direction', entity: ''},
        {name: 'accountSetupGroup / Account', entity: ''},


    ];


    return (<div>
        <div className="bg-white text-blue-500 px-4 py-2 rounded-lg mb-4 text-left font-bold">
            TradingStances Count: {tradingStances.length}
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
            {tradingStances.map((tradingStance, index) => (
                <tr key={index} className={`${index % 2 === 0 ? 'bg-gray-100' : ''}`}>

                    <td>{tradingStance.id}</td>
                    <td>{tradingStance.symbol}</td>
                    <td>{tradingStance.direction}</td>
                    <td>{tradingStance.accountSetupGroups.name}: {tradingStance.accountSetupGroups.account.name}</td>
                </tr>))}
            </tbody>
        </table>
    </div>);

}
