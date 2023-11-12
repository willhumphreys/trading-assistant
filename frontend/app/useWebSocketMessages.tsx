import React, {useEffect, useState} from 'react';
import {ServerMessage, Trade} from "@/app/interfaces";
import {Client, IMessage} from '@stomp/stompjs';


const newClient = new Client({
    brokerURL: 'ws://localhost:8080/gs-guide-websocket'
});

export function useWebSocketMessages(setTrades: React.Dispatch<React.SetStateAction<Trade[]>>) {
    const [tickMessage, setTickMessage] = useState<ServerMessage>({id: 0, field: '', value: ''});
    const [orderMessage, setOrderMessage] = useState<ServerMessage>({id: 0, field: '', value: ''});
    const [client, setClient] = useState<WebSocket | null>(null);

    useEffect(() => {

        newClient.onConnect = (frame) => {
            console.log('Connected: ' + frame);

            newClient.subscribe('/topic/ticks', (message: IMessage) => {
                if (message.body) {
                    console.log('Received message: ', message.body);
                    setTickMessage(JSON.parse(message.body));
                }
            });

            newClient.subscribe('/topic/order-change', (message: IMessage) => {
                if (message.body) {
                    console.log('Received message: ', message.body);
                    setOrderMessage(JSON.parse(message.body));
                }
            });
        };

        newClient.onStompError = (frame) => {
            console.log('Broker reported error: ' + frame.headers['message']);
            console.log('Additional details: ' + frame.body);
        };

        // Activate the client
        newClient.activate();


        return () => {
            newClient.deactivate();
        };
    }, []);

    const sendHelloMessage = () => {
        const message: ServerMessage = {
            id: 0,
            field: "dummyMessage",
            value: "Will"
        };

        if (newClient && newClient.connected) {
            console.log("sending message");
            newClient.publish({
                destination: "/app/hello",
                body: JSON.stringify(message)
            });
        }
    };
    // Logic to handle updates in trades when an order message is received
    useEffect(() => {
        if (orderMessage && orderMessage.id && orderMessage.field === 'profitAndLoss') {
            setTrades(prevTrades => prevTrades.map(trade =>
                trade.id === orderMessage.id
                    ? {...trade, profit: parseFloat(orderMessage.value)}
                    : trade
            ));
        }
    }, [orderMessage, setTrades]);

    return {client, sendHelloMessage, tickMessage, orderMessage};
}
