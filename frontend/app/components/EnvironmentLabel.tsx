// EnvironmentLabel.tsx
import React from 'react';

const EnvironmentLabel: React.FC = () => {
    const backendHost = process.env.NEXT_PUBLIC_BACKEND_HOST || 'default';
    let color: string;
    let text: string;

    if (backendHost.includes('localhost')) {
        color = 'bg-green-500';
        text = 'Development';
    } else {
        color = 'bg-red-500';
        text = 'Production';
    }

    console.log(`backend host ${backendHost}`)

    return (
        <div className={`${color} text-white text-xl p-2`}>
            Environment: {text}
        </div>
    );
};

export default EnvironmentLabel;