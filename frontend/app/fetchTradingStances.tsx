export const fetchTradingStances = async (accountName: string, sortColumn: string, sortDirection: string): Promise<any> => {
    try {
        const res = await fetch(`/api/trading-stances?accountName=${accountName}&sortColumn=${sortColumn}&sortDirection=${sortDirection}`, {
            headers: {
                'Content-Type': 'application/json'
            }
        });
        if (res.ok) {
            return await res.json();
        } else {
            console.log('Failed to fetch tradingStances')
            return null;
        }
    } catch (error) {
        console.log('An error occurred:', error);
        return null;
    }
};
