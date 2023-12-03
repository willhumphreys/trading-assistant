import {TradingStance, UpdateTradingStanceDto} from "@/app/types/interfaces";

export const fetchUpdateTradingStance = async (
    updateTradingStanceDto: UpdateTradingStanceDto,
    id: number
): Promise<TradingStance> => {

    const res = await fetch(`/api/actions/update-trading-stance/${id}`, {
        method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(updateTradingStanceDto)
        }
    );

    if (res.ok) {
        return await res.json();
    } else {
        console.log(`Failed to update tradingStance ${updateTradingStanceDto}`);
        return Promise.reject(new Error(`Failed to update tradingStance ${updateTradingStanceDto}`));
    }
}