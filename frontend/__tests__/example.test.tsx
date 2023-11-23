import {describe, expect, it, jest} from '@jest/globals';
import React from 'react';
import {fireEvent, render, screen} from '@testing-library/react';
import '@testing-library/jest-dom';
import AccountSelector from '@/app/components/accountSelector';

import '@/app/components/tradesAuditTable'

describe('AccountSelector', () => {
    it('handles selection changes', () => {
        const mockAccountSetupGroups = [
            {
                id: 1, name: 'Group 1', account: {id: 101, name: 'test', metatraderAdvisorPath: 'ksdjfs'},
                setupGroups: {
                    id: 1,
                    name: 'test',
                    scriptsDirectory: 'script'
                }
            },
        ];
        const mockSetQuery = jest.fn();
        const mockSetSelectedAccountSetupGroups = jest.fn();
        const mockQuery = {
            id: 2,
            account: {
                id: 3,
            },
            setup: {
                id: 4,
                createdDateTime: '2021-01-01T00:00:00.000Z',
                symbol: 'EURUSD',
                rank: 1,
                dayOfWeek: 1,
                hourOfDay: 3,
                stop: 500,
                limit: 600,
                tickOffset: -100,
                tradeDuration: 300,
                outOfTime: 8
            },
            status: 'PENDING',
            createdDateTime: '2021-01-01T00:00:00.000Z',
            lastUpdatedDateTime: '2021-01-01T00:00:00.000Z',
            targetPlaceDateTime: '2021-01-01T00:00:00.000Z',
            placedDateTime: '2021-01-01T00:00:00.000Z',
            placedPrice: '1.2',
            filledDateTime: '2021-01-01T00:00:00.000Z',
            filledPrice: '1.3',
            closedDateTime: '2021-01-01T00:00:00.000Z',
            closedPrice: '1.4',
            closeType: 'STOP',
            message: 'test'
        };


        render(
            <AccountSelector
                accountSetupGroups={mockAccountSetupGroups}
                setSelectedAccountSetupGroups={mockSetSelectedAccountSetupGroups}
                setQuery={mockSetQuery}
                query={mockQuery}
            />
        );

        fireEvent.change(screen.getByRole('combobox'), {target: {value: '1'}});
        expect(mockSetSelectedAccountSetupGroups).toHaveBeenCalledWith(mockAccountSetupGroups[0]);
        expect(mockSetQuery).toHaveBeenCalledWith({
            ...mockQuery,
            account: {id: 101}
        });
    });
});