import {Query, SortConfig, Trade} from "@/app/types/interfaces";

export const fetchTrades = async (query: Query, sortConfig: SortConfig): Promise<Trade[] | null> => {
    try {
        const nonEmptyQuery = Object.fromEntries(Object.entries(query).filter(([key, value]) => {
            if (key === 'setup') {
                return Object.keys(value).some(subKey => value[subKey] !== '');
            }
            return value !== '';
        }));

        if (nonEmptyQuery.setup) {
            nonEmptyQuery.setup = Object.fromEntries(Object.entries(nonEmptyQuery.setup).filter(([key, value]) => value !== ''));
        }

        const res = await fetch(`/api/trades/searchByExample?sortColumn=${sortConfig.column}&sortDirection=${sortConfig.direction}`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify(nonEmptyQuery),
        });

        if (res.ok) {
            return await res.json();
        } else {
            console.log('Failed to fetch trades');
            return null;
        }
    } catch (error) {
        console.log('An error occurred:', error);
        return null;
    }
};
