import {
	IExecuteFunctions,
	INodeExecutionData,
	INodeType,
	INodeTypeDescription,
	NodeOperationError,
} from 'n8n-workflow';

import axios from 'axios';

export class MeteorModule implements INodeType {
	description: INodeTypeDescription = {
		displayName: 'Meteor Client Module',
		name: 'meteorModule',
		icon: 'file:meteor.svg',
		group: ['minecraft', 'automation'],
		version: 1,
		subtitle: '={{$parameter["operation"]}}',
		description: 'Control Meteor Client modules and settings',
		defaults: {
			name: 'Meteor Module',
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
						name: 'Toggle Module',
						value: 'toggle',
						description: 'Toggle a module on/off',
						action: 'Toggle module',
					},
					{
						name: 'Configure Setting',
						value: 'configure',
						description: 'Change a module setting',
						action: 'Configure setting',
					},
					{
						name: 'Get Module Status',
						value: 'status',
						description: 'Get current status of a module',
						action: 'Get module status',
					},
					{
						name: 'List Modules',
						value: 'list',
						description: 'List all available modules',
						action: 'List modules',
					},
					{
						name: 'Create Profile',
						value: 'saveProfile',
						description: 'Save current configuration as a profile',
						action: 'Save profile',
					},
					{
						name: 'Load Profile',
						value: 'loadProfile',
						description: 'Load a configuration profile',
						action: 'Load profile',
					},
				],
				default: 'toggle',
			},

			// Module selection
			{
				displayName: 'Module Category',
				name: 'moduleCategory',
				type: 'options',
				displayOptions: {
					show: {
						operation: ['toggle', 'configure', 'status'],
					},
				},
				options: [
					{ name: 'Combat', value: 'combat' },
					{ name: 'Player', value: 'player' },
					{ name: 'Movement', value: 'movement' },
					{ name: 'Render', value: 'render' },
					{ name: 'World', value: 'world' },
					{ name: 'Misc', value: 'misc' },
				],
				default: 'combat',
				description: 'Category of the module',
			},
			{
				displayName: 'Module Name',
				name: 'moduleName',
				type: 'options',
				displayOptions: {
					show: {
						operation: ['toggle', 'configure', 'status'],
						moduleCategory: ['combat'],
					},
				},
				options: [
					{ name: 'Kill Aura', value: 'kill-aura' },
					{ name: 'Velocity', value: 'velocity' },
					{ name: 'Crystal Aura', value: 'crystal-aura' },
					{ name: 'Auto Trap', value: 'auto-trap' },
					{ name: 'Auto Anchor', value: 'auto-anchor' },
					{ name: 'Auto City', value: 'auto-city' },
					{ name: 'Bed Aura', value: 'bed-aura' },
					{ name: 'Trigger', value: 'trigger' },
				],
				default: 'kill-aura',
				description: 'Combat module to control',
			},
			{
				displayName: 'Module Name',
				name: 'moduleName',
				type: 'options',
				displayOptions: {
					show: {
						operation: ['toggle', 'configure', 'status'],
						moduleCategory: ['player'],
					},
				},
				options: [
					{ name: 'Auto Eat', value: 'auto-eat' },
					{ name: 'Auto Tool', value: 'auto-tool' },
					{ name: 'Auto Armor', value: 'auto-armor' },
					{ name: 'Rotation', value: 'rotation' },
					{ name: 'Reach', value: 'reach' },
					{ name: 'Auto Respawn', value: 'auto-respawn' },
					{ name: 'Auto Gap', value: 'auto-gap' },
					{ name: 'Inventory Tweaks', value: 'inventory-tweaks' },
				],
				default: 'auto-eat',
				description: 'Player module to control',
			},
			{
				displayName: 'Module Name',
				name: 'moduleName',
				type: 'options',
				displayOptions: {
					show: {
						operation: ['toggle', 'configure', 'status'],
						moduleCategory: ['movement'],
					},
				},
				options: [
					{ name: 'Speed', value: 'speed' },
					{ name: 'Flight', value: 'flight' },
					{ name: 'Step', value: 'step' },
					{ name: 'Jesus', value: 'jesus' },
					{ name: 'Spider', value: 'spider' },
					{ name: 'No Fall', value: 'no-fall' },
					{ name: 'Scaffold', value: 'scaffold' },
					{ name: 'Auto Walk', value: 'auto-walk' },
				],
				default: 'speed',
				description: 'Movement module to control',
			},
			{
				displayName: 'Module Name',
				name: 'moduleName',
				type: 'options',
				displayOptions: {
					show: {
						operation: ['toggle', 'configure', 'status'],
						moduleCategory: ['render'],
					},
				},
				options: [
					{ name: 'ESP', value: 'esp' },
					{ name: 'Tracers', value: 'tracers' },
					{ name: 'Fullbright', value: 'fullbright' },
					{ name: 'X-Ray', value: 'xray' },
					{ name: 'Nametags', value: 'nametags' },
					{ name: 'HUD', value: 'hud' },
					{ name: 'Search', value: 'search' },
					{ name: 'Block Selection', value: 'block-selection' },
				],
				default: 'esp',
				description: 'Render module to control',
			},
			{
				displayName: 'Module Name',
				name: 'moduleName',
				type: 'options',
				displayOptions: {
					show: {
						operation: ['toggle', 'configure', 'status'],
						moduleCategory: ['world'],
					},
				},
				options: [
					{ name: 'Timer', value: 'timer' },
					{ name: 'Ambience', value: 'ambience' },
					{ name: 'Better Tooltips', value: 'better-tooltips' },
					{ name: 'Highlight', value: 'highlight' },
					{ name: 'ViaVersion', value: 'viaversion' },
				],
				default: 'timer',
				description: 'World module to control',
			},
			{
				displayName: 'Module Name',
				name: 'moduleName',
				type: 'options',
				displayOptions: {
					show: {
						operation: ['toggle', 'configure', 'status'],
						moduleCategory: ['misc'],
					},
				},
				options: [
					{ name: 'Auto Reconnect', value: 'auto-reconnect' },
					{ name: 'Better Chat', value: 'better-chat' },
					{ name: 'Discord Presence', value: 'discord-presence' },
					{ name: 'Name Protect', value: 'name-protect' },
					{ name: 'Spam', value: 'spam' },
					{ name: 'Auto Log', value: 'auto-log' },
					{ name: 'Notifier', value: 'notifier' },
				],
				default: 'auto-reconnect',
				description: 'Misc module to control',
			},

			// Configuration fields
			{
				displayName: 'Setting Name',
				name: 'settingName',
				type: 'string',
				displayOptions: {
					show: {
						operation: ['configure'],
					},
				},
				default: '',
				description: 'Name of the setting to change',
				placeholder: 'range, damage, delay',
			},
			{
				displayName: 'Setting Value',
				name: 'settingValue',
				type: 'string',
				displayOptions: {
					show: {
						operation: ['configure'],
					},
				},
				default: '',
				description: 'New value for the setting',
				placeholder: '4.0, true, 100',
			},

			// Profile management
			{
				displayName: 'Profile Name',
				name: 'profileName',
				type: 'string',
				displayOptions: {
					show: {
						operation: ['saveProfile', 'loadProfile'],
					},
				},
				default: '',
				description: 'Name of the profile',
				placeholder: 'pvp_config, mining_setup',
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
						displayName: 'Wait for Confirmation',
						name: 'waitForConfirmation',
						type: 'boolean',
						default: false,
						description: 'Wait for module to confirm state change',
					},
					{
						displayName: 'Send HUD Update',
						name: 'sendHudUpdate',
						type: 'boolean',
						default: true,
						description: 'Send HUD update after module changes',
					},
					{
						displayName: 'Log Changes',
						name: 'logChanges',
						type: 'boolean',
						default: true,
						description: 'Log module state changes to console',
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

				let command = '';
				let result: any = {};

				switch (operation) {
					case 'toggle':
						const moduleCategory = this.getNodeParameter('moduleCategory', i) as string;
						const moduleName = this.getNodeParameter('moduleName', i) as string;
						
						// Use Minescript to send Meteor command
						command = `\\eval "minescript.execute('.toggle ${moduleName}')"`;
						break;

					case 'configure':
						const configModuleCategory = this.getNodeParameter('moduleCategory', i) as string;
						const configModuleName = this.getNodeParameter('moduleName', i) as string;
						const settingName = this.getNodeParameter('settingName', i) as string;
						const settingValue = this.getNodeParameter('settingValue', i) as string;
						
						command = `\\eval "minescript.execute('.${configModuleName} ${settingName} ${settingValue}')"`;
						break;

					case 'status':
						const statusModuleCategory = this.getNodeParameter('moduleCategory', i) as string;
						const statusModuleName = this.getNodeParameter('moduleName', i) as string;
						
						// Get module status via Python code
						command = `\\eval "import meteorclient; meteorclient.get_module_status('${statusModuleName}')"`;
						break;

					case 'list':
						// List all modules
						command = `\\eval "import meteorclient; meteorclient.list_modules()"`;
						break;

					case 'saveProfile':
						const saveProfileName = this.getNodeParameter('profileName', i) as string;
						command = `\\eval "minescript.execute('.profiles save ${saveProfileName}')"`;
						break;

					case 'loadProfile':
						const loadProfileName = this.getNodeParameter('profileName', i) as string;
						command = `\\eval "minescript.execute('.profiles load ${loadProfileName}')"`;
						break;

					default:
						throw new NodeOperationError(this.getNode(), `Unknown operation: ${operation}`);
				}

				// Execute the command
				result = await this.makeMinescriptRequest(baseUrl, credentials, command);

				// Send HUD update if requested
				if (advancedOptions.sendHudUpdate && (operation === 'toggle' || operation === 'configure')) {
					await this.makeMinescriptRequest(
						baseUrl, 
						credentials, 
						'\\eval "minescript.execute(\'.hud reload\')"'
					);
				}

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
