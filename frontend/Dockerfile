# Step 1: Specify the base image
FROM node:20.10.0

# Step 2: Set the working directory
WORKDIR /app

# Step 3: Copy package.json and package-lock.json
COPY package*.json ./

# Step 4: Install dependencies
RUN npm install

# Step 5: Copy the rest of your application
COPY . .

# Step 6: Build the Next.js app
RUN npm run build

# Step 7: Expose the port
EXPOSE 3000

# Step 8: Define the command to run your app
CMD [ "npm", "start" ]