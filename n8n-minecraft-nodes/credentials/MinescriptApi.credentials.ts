import {
	IAuthenticateGeneric,
	ICredentialTestRequest,
	ICredentialType,
	INodeProperties,
} from 'n8n-workflow';

export class MinescriptApi implements ICredentialType {
	name = 'minescriptApi';
	displayName = 'Minescript API';
	documentationUrl = 'https://github.com/maxuser0/minescript';
	properties: INodeProperties[] = [
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
	authenticate: IAuthenticateGeneric = {
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
	test: ICredentialTestRequest = {
		request: {
			baseURL: '={{$credentials.baseUrl}}',
			url: '/status',
			method: 'GET',
		},
	};
}
