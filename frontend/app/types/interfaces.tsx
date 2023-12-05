export interface ServerMessage {
    id: number;
    field: string;
    value: string;
}

export interface Account {
    id: number;
    name: string;
    metatraderAdvisorPath: string;
}

export interface SetupGroups {
    id: number;
    name: string;
    scriptsDirectory: string;
}

export interface AccountSetupGroups {
    id: number;
    name: string;
    account: Account;
    setupGroups: SetupGroups;
}

export interface SetupGroup {
    id: number;
    setupGroups: SetupGroups;
    path: string;
    symbol: string;
    enabled: boolean;
}

export interface Setup {
    id: number;
    createdDateTime: string;
    setupGroup: SetupGroup;
    symbol: string;
    rank: number;
    dayOfWeek: number;
    hourOfDay: number;
    stop: number;
    limit: number;
    tickOffset: number;
    tradeDuration: number;
    outOfTime: number;
}

export enum Status {
    PENDING = 'PENDING',
    ORDER_SENT = 'ORDER_SENT',
    PLACED_IN_MT = 'PLACED_IN_MT',
    FILLED = 'FILLED',
    OUT_OF_TIME = 'OUT_OF_TIME',
    CLOSED_BY_STOP = 'CLOSED_BY_STOP',
    CLOSED_BY_LIMIT = 'CLOSED_BY_LIMIT',
    CLOSED_BY_TIME = 'CLOSED_BY_TIME',
    CLOSED_BY_USER = 'CLOSED_BY_USER'
}

export enum RevisionType {
    ADD = 'ADD',
    MOD = 'MOD',
    DEL = 'DEL'
}

export interface Trade {
    id: number;
    status: Status;
    setup: Setup;
    createdDateTime: string;
    lastUpdatedDateTime: string;
    targetPlaceDateTime: string;
    placedDateTime: string;
    placedPrice: number | null;
    filledDateTime: string | null;
    filledPrice: number | null;
    profit: number | null;
    closedDateTime: string | null;
    closedPrice: number | null;
    closeType: string | null;
    message: string | null;
}

export interface TradeAudit {
    id: number;
    status: Status;
    setup: Setup;
    createdDateTime: string;
    lastUpdatedDateTime: string;
    targetPlaceDateTime: string;
    placedDateTime: string;
    placedPrice: number | null;
    filledDateTime: string | null;
    filledPrice: number | null;
    profit: number | null;
    closedDateTime: string | null;
    closedPrice: number | null;
    closeType: string | null;
    message: string | null;
    revisionDate: string,
    revisionType: RevisionType
}

export interface TradingStance {
    id: number;
    symbol: string;
    direction: string;
    accountSetupGroups: AccountSetupGroups;
    trades: Trade[] | undefined;
}

export interface Query {
    id: number | null;
    account: {
        id: number | null
    };
    setup: {
        id: number | null;
        createdDateTime: string;
        // setupGroup?: SetupGroup | null;
        symbol: string;
        rank: number | null;
        dayOfWeek?: number | null;
        hourOfDay?: number | null;
        stop?: number | null;
        limit?: number | null;
        tickOffset?: number | null;
        tradeDuration?: number | null;
        outOfTime?: number | null;
    };
    status: string;
    createdDateTime: string;
    lastUpdatedDateTime: string;
    targetPlaceDateTime: string;
    placedDateTime: string;
    placedPrice: string;
    filledDateTime: string;
    filledPrice: string;
    closedDateTime: string;
    closedPrice: string;
    closeType: string;
    message: string;
}

export type SortConfig = {
    column: string;
    direction: 'ASC' | 'DESC';
};


export interface Page<T> {
    content: T[];
    pageable: {
        pageNumber: number;
        pageSize: number;
        sort: {
            sorted: boolean;
            unsorted: boolean;
            empty: boolean;
        };
        offset: number;
        paged: boolean;
        unpaged: boolean;
    };
    totalPages: number;
    totalElements: number;
    last: boolean;
    size: number;
    number: number;
    sort: {
        sorted: boolean;
        unsorted: boolean;
        empty: boolean;
    };
    first: boolean;
    numberOfElements: number;
    empty: boolean;
}

export interface TradingStanceInfo {
    tradingStance: TradingStance;
    enabledSetupCount: number;
    disabledSetupCount: number;
}

export interface UpdateTradingStanceDto {
    symbol: String,
    direction: String,
    accountSetupGroupsName: String
}
