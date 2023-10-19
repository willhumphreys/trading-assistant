/** @type {import('next').NextConfig} */
const nextConfig = {
    async rewrites() {
        return [
            {
                source: '/api/:path*',
                destination: 'http://localhost:8080/:path*', // Proxy to your Spring Boot application
            },
        ];
    },
}

module.exports = nextConfig
