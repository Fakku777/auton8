import {
	IExecuteFunctions,
	INodeExecutionData,
	INodeType,
	INodeTypeDescription,
	NodeOperationError,
} from 'n8n-workflow';

import axios from 'axios';
import { EventSource } from 'eventsource';

export class MinecraftSession implements INodeType {
	description: INodeTypeDescription = {
		displayName: 'Minecraft Session Manager',
		name: 'minecraftSession',
		icon: 'file:minecraft.svg',
		group: ['minecraft', 'automation'],
		version: 1,
		subtitle: '={{$parameter["operation"]}}',
		description: 'Manage Minecraft sessions and handle events',
		defaults: {
			name: 'Minecraft Session',
		},
		inputs: ['main'],
		outputs: ['main'],
		credentials: [
			{
				name: 'minescriptApi',
				required: true,
			},
		],
		properties: [
			{
				displayName: 'Operation',
				name: 'operation',
				type: 'options',
				noDataExpression: true,
				options: [
					{
						name: 'Start Session',
						value: 'startSession',
						description: 'Initialize a new session',
						action: 'Start session',
					},
					{
						name: 'End Session',
						value: 'endSession',
						description: 'Terminate current session',
						action: 'End session',
					},
					{
						name: 'Get Session Status',
						value: 'getStatus',
						description: 'Get current session information',
						action: 'Get session status',
					},
					{
						name: 'Listen for Events',
						value: 'listenEvents',
						description: 'Set up event listener for game events',
						action: 'Listen for events',
					},
					{
						name: 'Get Player Info',
						value: 'getPlayerInfo',
						description: 'Get current player information',
						action: 'Get player info',
					},
					{
						name: 'Get World Info',
						value: 'getWorldInfo',
						description: 'Get current world information',
						action: 'Get world info',
					},
					{
						name: 'Get Inventory',
						value: 'getInventory',
						description: 'Get player inventory contents',
						action: 'Get inventory',
					},
					{
						name: 'Get Entities',
						value: 'getEntities',
						description: 'Get nearby entities',
						action: 'Get entities',
					},
					{
						name: 'Get Block Info',
						value: 'getBlockInfo',
						description: 'Get block information at coordinates',
						action: 'Get block info',
					},
				],
				default: 'startSession',
			},

			// Session configuration
			{
				displayName: 'Session Name',
				name: 'sessionName',
				type: 'string',
				displayOptions: {
					show: {
						operation: ['startSession'],
					},
				},
				default: 'n8n-workflow',
				description: 'Name for the session',
				placeholder: 'automation-session',
			},

			// Event listening configuration
			{
				displayName: 'Event Types',
				name: 'eventTypes',
				type: 'multiOptions',
				displayOptions: {
					show: {
						operation: ['listenEvents'],
					},
				},
				options: [
					{ name: 'Chat Messages', value: 'chat' },
					{ name: 'Player Movement', value: 'movement' },
					{ name: 'Block Changes', value: 'blocks' },
					{ name: 'Inventory Changes', value: 'inventory' },
					{ name: 'Entity Updates', value: 'entities' },
					{ name: 'World Changes', value: 'world' },
					{ name: 'Health/Status', value: 'health' },
					{ name: 'Connection Events', value: 'connection' },
				],
				default: ['chat', 'movement'],
				description: 'Types of events to listen for',
			},
			{
				displayName: 'Event Timeout (seconds)',
				name: 'eventTimeout',
				type: 'number',
				displayOptions: {
					show: {
						operation: ['listenEvents'],
					},
				},
				default: 30,
				description: 'How long to listen for events',
			},

			// Block info coordinates
			{
				displayName: 'Block X',
				name: 'blockX',
				type: 'number',
				displayOptions: {
					show: {
						operation: ['getBlockInfo'],
					},
				},
				default: 0,
				description: 'X coordinate of block',
			},
			{
				displayName: 'Block Y',
				name: 'blockY',
				type: 'number',
				displayOptions: {
					show: {
						operation: ['getBlockInfo'],
					},
				},
				default: 64,
				description: 'Y coordinate of block',
			},
			{
				displayName: 'Block Z',
				name: 'blockZ',
				type: 'number',
				displayOptions: {
					show: {
						operation: ['getBlockInfo'],
					},
				},
				default: 0,
				description: 'Z coordinate of block',
			},

			// Entity filter options
			{
				displayName: 'Entity Options',
				name: 'entityOptions',
				type: 'collection',
				displayOptions: {
					show: {
						operation: ['getEntities'],
					},
				},
				placeholder: 'Add Option',
				default: {},
				options: [
					{
						displayName: 'Entity Type',
						name: 'entityType',
						type: 'options',
						options: [
							{ name: 'All', value: 'all' },
							{ name: 'Players', value: 'players' },
							{ name: 'Mobs', value: 'mobs' },
							{ name: 'Animals', value: 'animals' },
							{ name: 'Items', value: 'items' },
							{ name: 'Specific Type', value: 'specific' },
						],
						default: 'all',
						description: 'Type of entities to get',
					},
					{
						displayName: 'Specific Entity Type',
						name: 'specificType',
						type: 'string',
						displayOptions: {
							show: {
								entityType: ['specific'],
							},
						},
						default: 'zombie',
						description: 'Specific entity type name',
						placeholder: 'zombie, pig, item_frame',
					},
					{
						displayName: 'Range',
						name: 'range',
						type: 'number',
						default: 16,
						description: 'Range to search for entities',
					},
					{
						displayName: 'Limit',
						name: 'limit',
						type: 'number',
						default: 10,
						description: 'Maximum number of entities to return',
					},
					{
						displayName: 'Sort By',
						name: 'sortBy',
						type: 'options',
						options: [
							{ name: 'Distance', value: 'distance' },
							{ name: 'Health', value: 'health' },
							{ name: 'Name', value: 'name' },
						],
						default: 'distance',
						description: 'How to sort the results',
					},
				],
			},

			// Advanced options
			{
				displayName: 'Advanced Options',
				name: 'advancedOptions',
				type: 'collection',
				placeholder: 'Add Option',
				default: {},
				options: [
					{
						displayName: 'Include Coordinates',
						name: 'includeCoords',
						type: 'boolean',
						default: true,
						description: 'Include coordinate information in results',
					},
					{
						displayName: 'Include Metadata',
						name: 'includeMetadata',
						type: 'boolean',
						default: false,
						description: 'Include additional metadata in results',
					},
					{
						displayName: 'Auto-reconnect',
						name: 'autoReconnect',
						type: 'boolean',
						default: true,
						description: 'Automatically reconnect on connection loss',
					},
					{
						displayName: 'Retry Attempts',
						name: 'retryAttempts',
						type: 'number',
						default: 3,
						description: 'Number of retry attempts for failed requests',
					},
				],
			},
		],
	};

	async execute(this: IExecuteFunctions): Promise<INodeExecutionData[][]> {
		const items = this.getInputData();
		const returnData: INodeExecutionData[] = [];

		const credentials = await this.getCredentials('minescriptApi');
		const baseUrl = credentials.baseUrl as string;

		for (let i = 0; i < items.length; i++) {
			try {
				const operation = this.getNodeParameter('operation', i) as string;
				const advancedOptions = this.getNodeParameter('advancedOptions', i, {}) as any;

				let result: any = {};

				switch (operation) {
					case 'startSession':
						const sessionName = this.getNodeParameter('sessionName', i) as string;
						result = await this.startSession(baseUrl, credentials, sessionName);
						break;

					case 'endSession':
						result = await this.endSession(baseUrl, credentials);
						break;

					case 'getStatus':
						result = await this.getSessionStatus(baseUrl, credentials);
						break;

					case 'listenEvents':
						const eventTypes = this.getNodeParameter('eventTypes', i) as string[];
						const eventTimeout = this.getNodeParameter('eventTimeout', i) as number;
						result = await this.listenForEvents(baseUrl, credentials, eventTypes, eventTimeout);
						break;

					case 'getPlayerInfo':
						result = await this.executeMinescriptCommand(
							baseUrl, 
							credentials, 
							'\\eval "minescript.player()"'
						);
						break;

					case 'getWorldInfo':
						result = await this.executeMinescriptCommand(
							baseUrl, 
							credentials, 
							'\\eval "minescript.world_info()"'
						);
						break;

					case 'getInventory':
						result = await this.executeMinescriptCommand(
							baseUrl, 
							credentials, 
							'\\eval "minescript.player_inventory()"'
						);
						break;

					case 'getEntities':
						const entityOptions = this.getNodeParameter('entityOptions', i, {}) as any;
						result = await this.getEntities(baseUrl, credentials, entityOptions);
						break;

					case 'getBlockInfo':
						const blockX = this.getNodeParameter('blockX', i) as number;
						const blockY = this.getNodeParameter('blockY', i) as number;
						const blockZ = this.getNodeParameter('blockZ', i) as number;
						result = await this.executeMinescriptCommand(
							baseUrl, 
							credentials, 
							`\\eval "minescript.getblock(${blockX}, ${blockY}, ${blockZ})"`
						);
						break;

					default:
						throw new NodeOperationError(this.getNode(), `Unknown operation: ${operation}`);
				}

				returnData.push({
					json: {
						operation,
						result,
						timestamp: new Date().toISOString(),
						sessionId: credentials.sessionId || 'auto-generated',
						...result,
					},
				});

			} catch (error) {
				if (this.continueOnFail()) {
					returnData.push({
						json: {
							error: error.message,
							operation: this.getNodeParameter('operation', i),
						},
					});
					continue;
				}
				throw error;
			}
		}

		return [returnData];
	}

	private async startSession(baseUrl: string, credentials: any, sessionName: string): Promise<any> {
		const sessionId = this.generateUUID();
		
		// Send session start event
		const response = await axios.post(
			`${baseUrl}/events`,
			{
				event_type: 'session_start',
				session_id: sessionId,
				session_name: sessionName,
				timestamp: new Date().toISOString(),
			},
			{
				headers: {
					'X-Client-ID': credentials.clientId,
					'X-Auth-Key': credentials.authKey,
					'X-Session-ID': sessionId,
					'Content-Type': 'application/json',
				},
			},
		);

		return {
			status: 'started',
			sessionId: sessionId,
			sessionName: sessionName,
			startTime: new Date().toISOString(),
		};
	}

	private async endSession(baseUrl: string, credentials: any): Promise<any> {
		const sessionId = credentials.sessionId || 'unknown';
		
		// Send session end event
		const response = await axios.post(
			`${baseUrl}/events`,
			{
				event_type: 'session_end',
				session_id: sessionId,
				timestamp: new Date().toISOString(),
			},
			{
				headers: {
					'X-Client-ID': credentials.clientId,
					'X-Auth-Key': credentials.authKey,
					'X-Session-ID': sessionId,
					'Content-Type': 'application/json',
				},
			},
		);

		return {
			status: 'ended',
			sessionId: sessionId,
			endTime: new Date().toISOString(),
		};
	}

	private async getSessionStatus(baseUrl: string, credentials: any): Promise<any> {
		const response = await axios.get(
			`${baseUrl}/status`,
			{
				headers: {
					'X-Client-ID': credentials.clientId,
					'X-Auth-Key': credentials.authKey,
					'X-Session-ID': credentials.sessionId || 'unknown',
				},
			},
		);

		return response.data;
	}

	private async listenForEvents(
		baseUrl: string, 
		credentials: any, 
		eventTypes: string[], 
		timeout: number
	): Promise<any> {
		const sessionId = credentials.sessionId || this.generateUUID();
		
		return new Promise((resolve, reject) => {
			const events: any[] = [];
			const eventSource = new EventSource(
				`${baseUrl}/events/stream?session_id=${sessionId}&types=${eventTypes.join(',')}`,
				{
					headers: {
						'X-Client-ID': credentials.clientId,
						'X-Auth-Key': credentials.authKey,
					},
				}
			);

			const timeoutId = setTimeout(() => {
				eventSource.close();
				resolve({
					events: events,
					totalEvents: events.length,
					eventTypes: eventTypes,
					timeout: timeout,
				});
			}, timeout * 1000);

			eventSource.onmessage = (event) => {
				try {
					const data = JSON.parse(event.data);
					events.push(data);
				} catch (error) {
					console.warn('Failed to parse event data:', error);
				}
			};

			eventSource.onerror = (error) => {
				clearTimeout(timeoutId);
				eventSource.close();
				reject(new Error('Event stream error: ' + error));
			};
		});
	}

	private async getEntities(baseUrl: string, credentials: any, options: any): Promise<any> {
		let command = '\\eval "';
		
		if (options.entityType === 'players') {
			command += 'minescript.players()';
		} else if (options.entityType === 'specific' && options.specificType) {
			command += `minescript.entities('${options.specificType}')`;
		} else {
			command += 'minescript.entities()';
		}

		// Add filters if specified
		if (options.range || options.limit || options.sortBy) {
			const filters: string[] = [];
			if (options.range) filters.push(`range=${options.range}`);
			if (options.limit) filters.push(`limit=${options.limit}`);
			if (options.sortBy) filters.push(`sort='${options.sortBy}'`);
			
			if (filters.length > 0) {
				command = command.replace(')', `, ${filters.join(', ')})`);
			}
		}

		command += '"';

		return await this.executeMinescriptCommand(baseUrl, credentials, command);
	}

	private async executeMinescriptCommand(
		baseUrl: string,
		credentials: any,
		command: string,
	): Promise<any> {
		const sessionId = credentials.sessionId || this.generateUUID();
		
		const response = await axios.post(
			`${baseUrl}/cmd`,
			{
				command: command,
				session_id: sessionId,
			},
			{
				headers: {
					'X-Client-ID': credentials.clientId,
					'X-Auth-Key': credentials.authKey,
					'X-Session-ID': sessionId,
					'Content-Type': 'application/json',
				},
			},
		);

		return response.data;
	}

	private generateUUID(): string {
		return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) {
			const r = Math.random() * 16 | 0;
			const v = c == 'x' ? r : (r & 0x3 | 0x8);
			return v.toString(16);
		});
	}
}
