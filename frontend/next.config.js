/** @type {import('next').NextConfig} */
const nextConfig = {
    async rewrites() {
        const backendHost = process.env.NEXT_PUBLIC_BACKEND_HOST || 'http://localhost:8080';
        return [
            {
                source: '/api/:path*',
                destination: `${backendHost}/:path*`, // Dynamic proxy destination
            },
        ];
    },
}

module.exports = nextConfig;
