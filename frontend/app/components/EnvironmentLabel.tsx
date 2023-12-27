// EnvironmentLabel.tsx
import React from 'react';

const EnvironmentLabel: React.FC = () => {
    const envMap: Record<string, { text: string; color: string }> = {
        'http://localhost:8080': {text: 'Development', color: 'bg-green-500'},
        'http://trading-assistant-service:8080': {text: 'Production', color: 'bg-red-500'},
        'http://192.168.86.218:8080': {text: 'Production', color: 'bg-blue-500'},
        'default': {text: 'Unknown', color: 'bg-gray-500'}
    };

    const {text, color} = envMap[process.env.NEXT_PUBLIC_BACKEND_HOST || 'default'];

    console.log(`backend host ${process.env.NEXT_PUBLIC_BACKEND_HOST}`)

    return (
        <div className={`${color} text-white text-xl p-2`}>
            Environment: {text}
        </div>
    );
};

export default EnvironmentLabel;
