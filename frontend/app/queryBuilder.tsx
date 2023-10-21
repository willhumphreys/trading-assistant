import {Query} from "@/app/interfaces";


type Props = {
    query: Query;
    setQuery: React.Dispatch<React.SetStateAction<Query>>;
};


export default function QueryBuilder({query, setQuery}: Props) {


    const fieldsToFilter = ['type', 'targetPlaceDateTime', 'placedDateTime', 'placedPrice', 'filledDateTime', 'filledPrice', 'closedDateTime', 'closedPrice', 'closeType', 'message'];

    return (

        <div className="query-builder flex flex-wrap">
            <label htmlFor="currency-select"
                   className="w-1/2 md:w-1/3 lg:w-1/4 xl:w-1/6 mb-2 text-sm font-bold text-gray-700">
                Symbol:
                <select
                    id="currency-select"
                    value={query.setup.symbol ?? "eurusd"}
                    onChange={(e) => {

                        const symbol = e.target.value;
                        setQuery({...query, setup: {...query.setup, symbol}});
                    }}
                    className="w-full mt-1 p-2 border rounded-md"
                >
                    <option value="">--Select--</option>
                    {[  'EUR/USD',
                        'USD/JPY',
                        'GBP/USD',
                        'USD/CHF',
                        'AUD/USD',
                        'USD/CAD',
                        'NZD/USD',
                        'EUR/JPY',
                        'EUR/AUD',
                        'EUR/GBP'   ].map((symbol, index) => (<option key={index} value={symbol.replace('/','')}>{symbol}</option>))}
                </select>
            </label>

            <label className="block mb-2 text-sm font-bold text-gray-700">
                Day of Week:
                <select
                    value={query.setup.dayOfWeek ?? ""}
                    onChange={(e) => {

                        const selectedValue = e.target.value;
                        const dayOfWeek = selectedValue ? parseInt(selectedValue, 10) : null;

                        setQuery({...query, setup: {...query.setup, dayOfWeek}});
                    }}
                    className="w-full mt-1 p-2 border rounded-md"
                >
                    <option value="">--Select--</option>
                    {['Sunday', 'Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday'].map((day, index) => (
                        <option key={index} value={index}>{day}</option>))}
                </select>
            </label>

            <label className="block mb-2 text-sm font-bold text-gray-700">
                Hour of Day:
                <select
                    value={query.setup.hourOfDay ?? ""}
                    onChange={(e) => {

                        const selectedValue = e.target.value;
                        const hourOfDay = selectedValue ? parseInt(selectedValue, 10) : null;

                        setQuery({...query, setup: {...query.setup, hourOfDay}});
                    }}
                    className="w-full mt-1 p-2 border rounded-md"
                >
                    <option value="">--Select--</option>
                    {Array.from({length: 24}, (_, i) => i).map((hour) => (
                        <option key={hour} value={hour}>{hour}</option>))}
                </select>
            </label>


            {['rank', 'stop', 'limit', 'tickOffset', 'tradeDuration', 'outOfTime'].map((field) => (
                <label key={field}
                       className="w-1/2 md:w-1/3 lg:w-1/4 xl:w-1/6 mb-2 text-sm font-bold text-gray-700">
                    {field.charAt(0).toUpperCase() + field.slice(1)}:
                    <input
                        type="number"
                        value={query.setup[field as keyof typeof query.setup] ?? ""}
                        onChange={(e) => {
                            const selectedValue = e.target.value;
                            const numValue = selectedValue ? parseFloat(selectedValue) : null;
                            setQuery({...query, setup: {...query.setup, [field]: numValue}});
                        }}
                        className="w-full mt-1 p-2 border rounded-md"
                    />
                </label>))}

            <div className="query-builder flex flex-wrap">
                {/* ... Your existing filters */}
                {fieldsToFilter.map((field) => {

                    const fieldValue = query[field as keyof typeof query];
                    if (typeof fieldValue === 'string' || fieldValue === null) {

                        return (<label key={field}
                                       className="w-1/2 md:w-1/3 lg:w-1/4 xl:w-1/6 mb-2 text-sm font-bold text-gray-700">
                            {field.charAt(0).toUpperCase() + field.slice(1)}
                            <input type="text" value={fieldValue ?? ""}
                                   onChange={(e) => setQuery({...query, [field]: e.target.value})}
                                   className="w-full mt-1 p-2 border rounded-md"/>
                        </label>);
                    }

                    return null;
                })}
            </div>

        </div>
    );

}