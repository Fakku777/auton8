"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.MinescriptApi = void 0;
class MinescriptApi {
    constructor() {
        this.name = 'minescriptApi';
        this.displayName = 'Minescript API';
        this.documentationUrl = 'https://github.com/maxuser0/minescript';
        this.properties = [
            {
                displayName: 'Base URL',
                name: 'baseUrl',
                type: 'string',
                default: 'http://127.0.0.1:5679/api',
                required: true,
                description: 'Base URL for the Minescript API server',
            },
            {
                displayName: 'Client ID',
                name: 'clientId',
                type: 'string',
                default: 'n8n-client',
                required: true,
                description: 'Client identifier for Minescript API authentication',
            },
            {
                displayName: 'Auth Key',
                name: 'authKey',
                type: 'string',
                typeOptions: {
                    password: true,
                },
                default: '',
                required: true,
                description: 'Authentication key for Minescript API access',
            },
            {
                displayName: 'Session ID',
                name: 'sessionId',
                type: 'string',
                default: '',
                required: false,
                description: 'Optional session ID for command correlation (auto-generated if empty)',
            },
        ];
        // Used to authenticate against the service
        this.authenticate = {
            type: 'generic',
            properties: {
                headers: {
                    'X-Client-ID': '={{$credentials.clientId}}',
                    'X-Auth-Key': '={{$credentials.authKey}}',
                    'X-Session-ID': '={{$credentials.sessionId || $uuid}}',
                },
            },
        };
        // Test the connection
        this.test = {
            request: {
                baseURL: '={{$credentials.baseUrl}}',
                url: '/status',
                method: 'GET',
            },
        };
    }
}
exports.MinescriptApi = MinescriptApi;
