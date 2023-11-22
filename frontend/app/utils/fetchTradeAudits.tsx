import {TradeAudit} from "@/app/types/interfaces";

export const fetchTradeAudits = async (tradeId: number): Promise<TradeAudit[] | null> => {
    try {
        const res = await fetch(`/api/audits/trades/${tradeId}`);
        if (res.ok) {
            return await res.json();
        } else {
            console.log('Failed to fetch tradeAudits');
            return null;
        }
    } catch (error) {
        console.log('An error occurred:', error);
        return null;
    }
};
