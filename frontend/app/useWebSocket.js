// useWebSocket.js
const useWebSocket = () => {
    const connectWebSocket = () => {

        const webSocket = new WebSocket("ws://localhost:8080/ws");

        webSocket.onmessage = function (event) {
            console.log("Server says: " + event.data);
        };

        webSocket.onopen = function () {
            webSocket.send("Hello, server!");
        };
    };

    return {connectWebSocket};
};

export default useWebSocket;
