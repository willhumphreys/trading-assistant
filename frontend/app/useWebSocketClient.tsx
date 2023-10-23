import {useEffect, useState} from 'react';
import {Client, IMessage} from '@stomp/stompjs';
import {ServerMessage} from "@/app/interfaces";

const newClient = new Client({
    brokerURL: 'ws://localhost:8080/gs-guide-websocket'
});

export function useWebSocketClient() {

    const [tickMessage, setTickMessage] = useState<ServerMessage>({content: ''});
    const [orderMessage, setOrderMessage] = useState<ServerMessage>({content: ''});


    useEffect(() => {

        // Setup connection behavior
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

        // Cleanup logic for disconnecting from STOMP server
        return () => {
            newClient.deactivate();
        };
    }, []);

    const sendHelloMessage = () => {
        const message: ServerMessage = {
            content: "Your Name"
        };

        if (newClient && newClient.connected) {
            console.log("sending message");
            newClient.publish({
                destination: "/app/hello",
                body: JSON.stringify(message)
            });
        }
    };

    return {
        client: newClient,
        sendHelloMessage,
        tickMessage: tickMessage,
        orderMessage: orderMessage
    };
}
