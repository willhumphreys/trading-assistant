import {AccountSetupGroups} from "@/app/types/interfaces";

export const fetchAccountSetupGroups = async (): Promise<AccountSetupGroups[]> => {

    try {
        const res = await fetch('/api/accountSetupGroups');
        if (res.ok) {
            return await res.json();
        } else {
            console.log('Failed to fetch account setup groups');
            return [];
        }
    } catch (error) {
        console.log('An error occurred:', error);
        return [];
    }

}