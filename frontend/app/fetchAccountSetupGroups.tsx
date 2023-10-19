export const fetchAccountSetupGroups = async (): Promise<any> => {

    try {
        const res = await fetch('/api/accountSetupGroups');
        if (res.ok) {
            return await res.json();
        } else {
            console.log('Failed to fetch account setup groups');
            return null;
        }
    } catch (error) {
        console.log('An error occurred:', error);
        return null;
    }

}