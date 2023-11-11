import {AccountSetupGroups} from "@/app/interfaces";

export const fetchTradingStances = async (sortColumn: string, sortDirection: string, accountSetupGroups?: AccountSetupGroups): Promise<any> => {

    const queryParams = [
        accountSetupGroups ? `accountSetupGroupsName=${accountSetupGroups.name}` : null,
        `sortColumn=${sortColumn}`,
        `sortDirection=${sortDirection}`
    ].filter(Boolean).join('&');

    const query = `/api/trading-stances${queryParams ? `?${queryParams}` : ''}`;
    try {
        const res = await fetch(query, {
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
