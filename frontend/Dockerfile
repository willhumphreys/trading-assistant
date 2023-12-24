FROM node:20.10.0

WORKDIR /app

COPY package*.json ./

RUN npm install

COPY . .

ENV NEXT_PUBLIC_BACKEND_HOST=http://trading-assistant-service:8080
ENV NEXT_PUBLIC_WEB_SOCKET_HOST=ws://trading-assistant-service:8080

RUN npm run build

EXPOSE 3000

CMD [ "npm", "start" ]