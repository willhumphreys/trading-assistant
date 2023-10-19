export const fetchSetupGroups = async (): Promise<any> => {
    try {
        const res = await fetch('/api/setupGroups');
        if (res.ok) {
            return await res.json();
        } else {
            console.log('Failed to fetch setup groups');
            return null;
        }
    } catch (error) {
        console.log('An error occurred:', error);
        return null;
    }
};
