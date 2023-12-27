/** @type {import('next').NextConfig} */
const nextConfig = {
    publicRuntimeConfig: {
        backendHost: process.env.NEXT_PUBLIC_BACKEND_HOST,
    },

    output: 'standalone',
    async rewrites() {
        console.log(`backend host config ${this.publicRuntimeConfig.backendHost}`);
        //const backendHost = process.env.NEXT_PUBLIC_BACKEND_HOST || 'http://trading-assistant-service:8080';
        return [
            {
                source: '/api/:path*',
                destination: `${this.publicRuntimeConfig.backendHost}/:path*`, // Dynamic proxy destination
            },
        ];
    },
}

module.exports = nextConfig;
