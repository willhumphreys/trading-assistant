import {Account, Query} from "@/app/interfaces";

type AccountSelectorProps = {
    accounts: Account[];
    setQuery: React.Dispatch<React.SetStateAction<Query>>;
    query: Query
};

const AccountSelector: React.FC<AccountSelectorProps> = ({accounts, setQuery, query}) => {
    return (
        <select
            className="mt-1 p-2 border rounded-md"
            onChange={(e) => {
                const accountId = e.target.value;
                setQuery({
                    ...query,
                    account: {
                        ...query.account,
                        id: accountId === '' ? null : parseInt(accountId)
                    }
                });
            }}>
            <option value="all">All</option>
            {accounts.map((account, index) => (
                <option key={index} value={account.id}>
                    {account.name}
                </option>
            ))}
        </select>

    );
};

export default AccountSelector;
