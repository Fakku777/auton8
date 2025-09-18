import {
	IExecuteFunctions,
	INodeExecutionData,
	INodeType,
	INodeTypeDescription,
	NodeOperationError,
} from 'n8n-workflow';

import axios from 'axios';

export class BaritonePathfinder implements INodeType {
	description: INodeTypeDescription = {
		displayName: 'Baritone Pathfinder',
		name: 'baritonePathfinder',
		icon: 'file:baritone.svg',
		group: ['minecraft', 'automation'],
		version: 1,
		subtitle: '={{$parameter["operation"]}}',
		description: 'Control Baritone pathfinding and movement',
		defaults: {
			name: 'Baritone Pathfinder',
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
						name: 'Go to Coordinates',
						value: 'goto',
						description: 'Path to specific coordinates',
						action: 'Go to coordinates',
					},
					{
						name: 'Go to Block Type',
						value: 'gotoBlock',
						description: 'Find and path to a specific block type',
						action: 'Find and go to block type',
					},
					{
						name: 'Follow Player',
						value: 'follow',
						description: 'Follow a specific player',
						action: 'Follow a player',
					},
					{
						name: 'Explore Area',
						value: 'explore',
						description: 'Explore from a specific location',
						action: 'Explore area',
					},
					{
						name: 'Stop/Cancel',
						value: 'stop',
						description: 'Stop current pathfinding',
						action: 'Stop pathfinding',
					},
					{
						name: 'Set Goal',
						value: 'goal',
						description: 'Set a goal without starting pathfinding',
						action: 'Set pathfinding goal',
					},
					{
						name: 'Path',
						value: 'path',
						description: 'Start pathfinding to current goal',
						action: 'Start pathfinding',
					},
				],
				default: 'goto',
			},

			// Goto coordinates fields
			{
				displayName: 'X Coordinate',
				name: 'x',
				type: 'number',
				displayOptions: {
					show: {
						operation: ['goto', 'goal', 'explore'],
					},
				},
				default: 0,
				description: 'X coordinate to path to',
			},
			{
				displayName: 'Y Coordinate',
				name: 'y',
				type: 'number',
				displayOptions: {
					show: {
						operation: ['goto', 'goal'],
					},
				},
				default: '',
				description: 'Y coordinate to path to (optional)',
			},
			{
				displayName: 'Z Coordinate',
				name: 'z',
				type: 'number',
				displayOptions: {
					show: {
						operation: ['goto', 'goal', 'explore'],
					},
				},
				default: 0,
				description: 'Z coordinate to path to',
			},

			// Block type fields
			{
				displayName: 'Block Type',
				name: 'blockType',
				type: 'string',
				displayOptions: {
					show: {
						operation: ['gotoBlock'],
					},
				},
				default: 'diamond_ore',
				description: 'Block type to find and path to (e.g., diamond_ore, iron_ore)',
				placeholder: 'diamond_ore',
			},

			// Follow player fields
			{
				displayName: 'Player Name',
				name: 'playerName',
				type: 'string',
				displayOptions: {
					show: {
						operation: ['follow'],
					},
				},
				default: '',
				description: 'Name of player to follow',
				placeholder: 'PlayerName',
			},
			{
				displayName: 'Follow Distance',
				name: 'followDistance',
				type: 'number',
				displayOptions: {
					show: {
						operation: ['follow'],
					},
				},
				default: 3,
				description: 'Distance to maintain from followed player',
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
						displayName: 'Allow Sprint',
						name: 'allowSprint',
						type: 'boolean',
						default: true,
						description: 'Whether to allow sprinting during pathfinding',
					},
					{
						displayName: 'Allow Break',
						name: 'allowBreak',
						type: 'boolean',
						default: true,
						description: 'Whether to allow breaking blocks during pathfinding',
					},
					{
						displayName: 'Allow Place',
						name: 'allowPlace',
						type: 'boolean',
						default: true,
						description: 'Whether to allow placing blocks during pathfinding',
					},
					{
						displayName: 'Allow Parkour',
						name: 'allowParkour',
						type: 'boolean',
						default: true,
						description: 'Whether to allow parkour moves during pathfinding',
					},
					{
						displayName: 'Timeout (seconds)',
						name: 'timeout',
						type: 'number',
						default: 300,
						description: 'Maximum time to spend pathfinding',
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

				// Apply advanced settings first if provided
				const settingsCommands: string[] = [];
				if (advancedOptions.allowSprint !== undefined) {
					settingsCommands.push(`allowSprint ${advancedOptions.allowSprint}`);
				}
				if (advancedOptions.allowBreak !== undefined) {
					settingsCommands.push(`allowBreak ${advancedOptions.allowBreak}`);
				}
				if (advancedOptions.allowPlace !== undefined) {
					settingsCommands.push(`allowPlace ${advancedOptions.allowPlace}`);
				}
				if (advancedOptions.allowParkour !== undefined) {
					settingsCommands.push(`allowParkour ${advancedOptions.allowParkour}`);
				}

				// Execute settings commands
				for (const settingCmd of settingsCommands) {
					await this.makeMinescriptRequest(baseUrl, credentials, `#${settingCmd}`);
				}

				let command = '';
				let result: any = {};

				switch (operation) {
					case 'goto':
						const x = this.getNodeParameter('x', i) as number;
						const z = this.getNodeParameter('z', i) as number;
						const y = this.getNodeParameter('y', i, '') as string | number;
						
						if (y !== '') {
							command = `#goto ${x} ${y} ${z}`;
						} else {
							command = `#goto ${x} ${z}`;
						}
						break;

					case 'gotoBlock':
						const blockType = this.getNodeParameter('blockType', i) as string;
						command = `#goto ${blockType}`;
						break;

					case 'follow':
						const playerName = this.getNodeParameter('playerName', i) as string;
						if (playerName) {
							command = `#follow player ${playerName}`;
						} else {
							command = `#follow players`;
						}
						break;

					case 'explore':
						const exploreX = this.getNodeParameter('x', i) as number;
						const exploreZ = this.getNodeParameter('z', i) as number;
						command = `#explore ${exploreX} ${exploreZ}`;
						break;

					case 'stop':
						command = '#stop';
						break;

					case 'goal':
						const goalX = this.getNodeParameter('x', i) as number;
						const goalZ = this.getNodeParameter('z', i) as number;
						const goalY = this.getNodeParameter('y', i, '') as string | number;
						
						if (goalY !== '') {
							command = `#goal ${goalX} ${goalY} ${goalZ}`;
						} else {
							command = `#goal ${goalX} ${goalZ}`;
						}
						break;

					case 'path':
						command = '#path';
						break;

					default:
						throw new NodeOperationError(this.getNode(), `Unknown operation: ${operation}`);
				}

				// Execute the main command
				result = await this.makeMinescriptRequest(baseUrl, credentials, command);

				returnData.push({
					json: {
						operation,
						command,
						result,
						timestamp: new Date().toISOString(),
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

	private async makeMinescriptRequest(
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
