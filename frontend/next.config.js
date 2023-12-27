/** @type {import('next').NextConfig} */
const nextConfig = {
    output: 'standalone',
    async rewrites() {
        console.log(`backend host config ${process.env.NEXT_PUBLIC_BACKEND_HOST}`);
        const backendHost = process.env.NEXT_PUBLIC_BACKEND_HOST || 'http://trading-assistant-service:8080';
        return [
            {
                source: '/api/:path*',
                destination: `${backendHost}/:path*`, // Dynamic proxy destination
            },
        ];
    },
}

module.exports = nextConfig;
