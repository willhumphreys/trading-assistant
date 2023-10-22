import {useEffect, useState} from 'react';
import {Client, IMessage} from '@stomp/stompjs';
import {HelloMessage} from "@/app/interfaces";

const newClient = new Client({
    brokerURL: 'ws://localhost:8080/gs-guide-websocket'
});

export function useWebSocketClient() {

    const [serverMessage, setServerMessage] = useState('');  // Default sort column


    useEffect(() => {

        // Setup connection behavior
        newClient.onConnect = (frame) => {
            console.log('Connected: ' + frame);

            newClient.subscribe('/topic/greetings', (message: IMessage) => {
                if (message.body) {
                    console.log('Received message: ', message.body);
                    setServerMessage(message.body);
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
        const message: HelloMessage = {
            name: "Your Name"
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
        serverMessage
    };
}
