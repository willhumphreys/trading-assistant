/** @type {import('next').NextConfig} */
const nextConfig = {
    // publicRuntimeConfig: {
    //     backendHost: process.env.NEXT_PUBLIC_BACKEND_HOST,
    // },

    output: 'standalone',
    async rewrites() {

        //const backendHost = 'http://trading-assistant-service:8080';
        return [
            {
                source: '/api/:path*',
                destination: `${process.env.NEXT_PUBLIC_BACKEND_HOST}/:path*`, // Dynamic proxy destination
            },
        ];
    },
}

module.exports = nextConfig;
