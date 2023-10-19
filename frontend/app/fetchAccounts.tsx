export const fetchAccounts = async (): Promise<any> => {
    try {
        const res = await fetch('/api/accounts');
        if (res.ok) {
            return await res.json();
        } else {
            console.log('Failed to fetch accounts');
            return null;
        }
    } catch (error) {
        console.log('An error occurred:', error);
        return null;
    }
};
