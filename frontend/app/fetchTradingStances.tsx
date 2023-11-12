import {AccountSetupGroups, Page, TradingStanceInfo} from "@/app/interfaces";

export const fetchTradingStances = async (
    page: number,
    size: number,
    sortColumn: string,
    sortDirection: string,
    accountSetupGroups: AccountSetupGroups | undefined
): Promise<Page<TradingStanceInfo>> => {

    const sort = `${sortColumn},${sortDirection}`;
    const queryParams = [
        accountSetupGroups ? `accountSetupGroupsName=${accountSetupGroups.name}` : null,
        `page=${page}`,
        `size=${size}`,
        `sort=${sort}`
    ].filter(Boolean).join('&');

    const query = `/api/trading-stances-with-setup-count${queryParams ? `?${queryParams}` : ''}`;
    try {
        const res = await fetch(query, {
            headers: {
                'Content-Type': 'application/json'
            }
        });
        if (res.ok) {
            return await res.json();
        } else {
            console.log('Failed to fetch tradingStances');
            return Promise.reject(new Error('Failed to fetch trading stances'));
        }
    } catch (error) {
        console.error('An error occurred:', error);
        return Promise.reject(error);
    }
};
