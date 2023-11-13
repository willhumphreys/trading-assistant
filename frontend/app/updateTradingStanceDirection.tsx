import React, {FC, useEffect, useState} from "react";
import {TradingStance} from "./interfaces";
import {fetchUpdateTradingStance} from "@/app/fetchUpdateTradingStance";

type UpdateTradingStanceProps = {
    tradingStance: TradingStance;
};

const UpdateTradingStanceDirection: FC<UpdateTradingStanceProps> = ({tradingStance}) => {
    const [selectedDirection, setSelectedDirection] = useState<string>(tradingStance.direction);

    const handleUpdateClick = () => {
        if (selectedDirection !== tradingStance.direction) {
            const updatedTradingStance = {
                symbol: tradingStance.symbol,
                direction: selectedDirection.toUpperCase(),
                accountSetupGroupsName: tradingStance.accountSetupGroups.name
            };
            fetchUpdateTradingStance(updatedTradingStance, tradingStance.id)
                .then(r => {
                    console.log(`Updated tradingStance ${JSON.stringify(r)}`);
                    tradingStance.direction = r.direction;
                });
        }
    };

    useEffect(() => {
        setSelectedDirection(tradingStance.direction);
    }, [tradingStance]);

    return (
        <div>
            <select
                className="mt-1 p-2 border rounded-md"
                value={selectedDirection}
                onChange={(e) => setSelectedDirection(e.target.value)}
            >
                <option value="long">Long</option>
                <option value="flat">Flat</option>
                <option value="short">Short</option>
            </select>
            <button
                className="bg-blue-500 hover:bg-blue-700 text-white font-bold py-2 px-4 rounded"
                onClick={handleUpdateClick}
            >
                Update
            </button>
        </div>
    );
};

export default UpdateTradingStanceDirection;
