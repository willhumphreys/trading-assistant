import {AccountSetupGroups, Query} from '@/app/types/interfaces';
import React, {Dispatch, FC, SetStateAction} from "react";

type AccountSelectorProps = {
    accountSetupGroups: AccountSetupGroups[];
    setSelectedAccountSetupGroups: Dispatch<SetStateAction<AccountSetupGroups | undefined>>;
    setQuery: Dispatch<SetStateAction<Query>>;
    query: Query
};

const AccountSelector: FC<AccountSelectorProps> =
    ({
         accountSetupGroups,
         setQuery,
         query,
         setSelectedAccountSetupGroups
     }) => {
        return (
            <select
                className="mt-1 p-2 border rounded-md"
                onChange={(e) => {
                    const asgId = e.target.value;
                    const selectedASG = asgId === 'all'
                        ? undefined
                        : accountSetupGroups.find((asg) => asg.id === parseInt(asgId));

                    setQuery({
                        ...query,
                        account: {
                            ...query.account,
                            id: selectedASG ? selectedASG.account.id : null
                        }
                    });

                    setSelectedAccountSetupGroups(selectedASG);
                }}
            >
                <option value="all">All</option>
                {accountSetupGroups.map((accountSetupGroups, index) => (
                    <option key={index} value={accountSetupGroups.id}>
                        {accountSetupGroups.name}
                    </option>
                ))}
            </select>
        );
    };

export default AccountSelector;
