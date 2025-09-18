"use strict";
var __importDefault = (this && this.__importDefault) || function (mod) {
    return (mod && mod.__esModule) ? mod : { "default": mod };
};
Object.defineProperty(exports, "__esModule", { value: true });
exports.BaritoneBuilder = void 0;
const n8n_workflow_1 = require("n8n-workflow");
const axios_1 = __importDefault(require("axios"));
class BaritoneBuilder {
    constructor() {
        this.description = {
            displayName: 'Baritone Builder',
            name: 'baritoneBuilder',
            icon: 'file:baritone.svg',
            group: ['minecraft', 'automation'],
            version: 1,
            subtitle: '={{$parameter["operation"]}}',
            description: 'Control Baritone building and construction features',
            defaults: {
                name: 'Baritone Builder',
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
                            name: 'Build Schematic',
                            value: 'build',
                            description: 'Build a schematic file',
                            action: 'Build schematic',
                        },
                        {
                            name: 'Clear Area',
                            value: 'clearArea',
                            description: 'Clear blocks in a rectangular area',
                            action: 'Clear area',
                        },
                        {
                            name: 'Fill Area',
                            value: 'fillArea',
                            description: 'Fill area with specified block',
                            action: 'Fill area',
                        },
                        {
                            name: 'Mine',
                            value: 'mine',
                            description: 'Mine specific blocks or ores',
                            action: 'Mine blocks',
                        },
                        {
                            name: 'Farm',
                            value: 'farm',
                            description: 'Automatic farming operations',
                            action: 'Farm crops',
                        },
                        {
                            name: 'Tunnel',
                            value: 'tunnel',
                            description: 'Create tunnels',
                            action: 'Create tunnel',
                        },
                    ],
                    default: 'build',
                },
                // Build schematic fields
                {
                    displayName: 'Schematic File',
                    name: 'schematicFile',
                    type: 'string',
                    displayOptions: {
                        show: {
                            operation: ['build'],
                        },
                    },
                    default: '',
                    description: 'Path to schematic file (relative to schematics folder)',
                    placeholder: 'house.schematic',
                },
                {
                    displayName: 'Origin X',
                    name: 'originX',
                    type: 'string',
                    displayOptions: {
                        show: {
                            operation: ['build'],
                        },
                    },
                    default: '~',
                    description: 'X coordinate for build origin (use ~ for player position)',
                },
                {
                    displayName: 'Origin Y',
                    name: 'originY',
                    type: 'string',
                    displayOptions: {
                        show: {
                            operation: ['build'],
                        },
                    },
                    default: '~',
                    description: 'Y coordinate for build origin (use ~ for player position)',
                },
                {
                    displayName: 'Origin Z',
                    name: 'originZ',
                    type: 'string',
                    displayOptions: {
                        show: {
                            operation: ['build'],
                        },
                    },
                    default: '~',
                    description: 'Z coordinate for build origin (use ~ for player position)',
                },
                // Area selection fields (for clear/fill)
                {
                    displayName: 'Corner 1 X',
                    name: 'x1',
                    type: 'number',
                    displayOptions: {
                        show: {
                            operation: ['clearArea', 'fillArea'],
                        },
                    },
                    default: 0,
                    description: 'X coordinate of first corner',
                },
                {
                    displayName: 'Corner 1 Y',
                    name: 'y1',
                    type: 'number',
                    displayOptions: {
                        show: {
                            operation: ['clearArea', 'fillArea'],
                        },
                    },
                    default: 0,
                    description: 'Y coordinate of first corner',
                },
                {
                    displayName: 'Corner 1 Z',
                    name: 'z1',
                    type: 'number',
                    displayOptions: {
                        show: {
                            operation: ['clearArea', 'fillArea'],
                        },
                    },
                    default: 0,
                    description: 'Z coordinate of first corner',
                },
                {
                    displayName: 'Corner 2 X',
                    name: 'x2',
                    type: 'number',
                    displayOptions: {
                        show: {
                            operation: ['clearArea', 'fillArea'],
                        },
                    },
                    default: 10,
                    description: 'X coordinate of second corner',
                },
                {
                    displayName: 'Corner 2 Y',
                    name: 'y2',
                    type: 'number',
                    displayOptions: {
                        show: {
                            operation: ['clearArea', 'fillArea'],
                        },
                    },
                    default: 10,
                    description: 'Y coordinate of second corner',
                },
                {
                    displayName: 'Corner 2 Z',
                    name: 'z2',
                    type: 'number',
                    displayOptions: {
                        show: {
                            operation: ['clearArea', 'fillArea'],
                        },
                    },
                    default: 10,
                    description: 'Z coordinate of second corner',
                },
                // Fill block type
                {
                    displayName: 'Block Type',
                    name: 'blockType',
                    type: 'string',
                    displayOptions: {
                        show: {
                            operation: ['fillArea'],
                        },
                    },
                    default: 'stone',
                    description: 'Block type to fill with',
                    placeholder: 'stone, dirt, cobblestone, etc.',
                },
                // Mining fields
                {
                    displayName: 'Target Blocks',
                    name: 'targetBlocks',
                    type: 'string',
                    displayOptions: {
                        show: {
                            operation: ['mine'],
                        },
                    },
                    default: 'diamond_ore',
                    description: 'Block types to mine (space separated)',
                    placeholder: 'diamond_ore iron_ore coal_ore',
                },
                {
                    displayName: 'Mine Count',
                    name: 'mineCount',
                    type: 'number',
                    displayOptions: {
                        show: {
                            operation: ['mine'],
                        },
                    },
                    default: 0,
                    description: 'Number of blocks to mine (0 for unlimited)',
                },
                // Farm fields
                {
                    displayName: 'Farm Range',
                    name: 'farmRange',
                    type: 'number',
                    displayOptions: {
                        show: {
                            operation: ['farm'],
                        },
                    },
                    default: 64,
                    description: 'Range from starting point to farm',
                },
                // Tunnel fields
                {
                    displayName: 'Tunnel Length',
                    name: 'tunnelLength',
                    type: 'number',
                    displayOptions: {
                        show: {
                            operation: ['tunnel'],
                        },
                    },
                    default: 100,
                    description: 'Length of tunnel to create',
                },
                {
                    displayName: 'Tunnel Width',
                    name: 'tunnelWidth',
                    type: 'number',
                    displayOptions: {
                        show: {
                            operation: ['tunnel'],
                        },
                    },
                    default: 2,
                    description: 'Width of tunnel',
                },
                {
                    displayName: 'Tunnel Height',
                    name: 'tunnelHeight',
                    type: 'number',
                    displayOptions: {
                        show: {
                            operation: ['tunnel'],
                        },
                    },
                    default: 3,
                    description: 'Height of tunnel',
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
                            displayName: 'Build in Layers',
                            name: 'buildInLayers',
                            type: 'boolean',
                            default: false,
                            description: 'Build layer by layer from bottom to top',
                        },
                        {
                            displayName: 'Legit Mine',
                            name: 'legitMine',
                            type: 'boolean',
                            default: false,
                            description: 'Only mine ores that are actually visible',
                        },
                        {
                            displayName: 'Backfill',
                            name: 'backfill',
                            type: 'boolean',
                            default: false,
                            description: 'Fill in tunnels behind while mining/tunneling',
                        },
                        {
                            displayName: 'Allow Break Blocks',
                            name: 'allowBreak',
                            type: 'boolean',
                            default: true,
                            description: 'Allow breaking blocks during operations',
                        },
                        {
                            displayName: 'Allow Place Blocks',
                            name: 'allowPlace',
                            type: 'boolean',
                            default: true,
                            description: 'Allow placing blocks during operations',
                        },
                        {
                            displayName: 'Max Build Height',
                            name: 'maxBuildHeight',
                            type: 'number',
                            default: 320,
                            description: 'Maximum Y level to build at',
                        },
                    ],
                },
            ],
        };
    }
    async execute() {
        const items = this.getInputData();
        const returnData = [];
        const credentials = await this.getCredentials('minescriptApi');
        const baseUrl = credentials.baseUrl;
        for (let i = 0; i < items.length; i++) {
            try {
                const operation = this.getNodeParameter('operation', i);
                const advancedOptions = this.getNodeParameter('advancedOptions', i, {});
                // Apply advanced settings first if provided
                const settingsCommands = [];
                if (advancedOptions.buildInLayers !== undefined) {
                    settingsCommands.push(`buildInLayers ${advancedOptions.buildInLayers}`);
                }
                if (advancedOptions.legitMine !== undefined) {
                    settingsCommands.push(`legitMine ${advancedOptions.legitMine}`);
                }
                if (advancedOptions.backfill !== undefined) {
                    settingsCommands.push(`backfill ${advancedOptions.backfill}`);
                }
                if (advancedOptions.allowBreak !== undefined) {
                    settingsCommands.push(`allowBreak ${advancedOptions.allowBreak}`);
                }
                if (advancedOptions.allowPlace !== undefined) {
                    settingsCommands.push(`allowPlace ${advancedOptions.allowPlace}`);
                }
                if (advancedOptions.maxBuildHeight !== undefined) {
                    settingsCommands.push(`maxBuildHeight ${advancedOptions.maxBuildHeight}`);
                }
                // Execute settings commands
                for (const settingCmd of settingsCommands) {
                    await this.makeMinescriptRequest(baseUrl, credentials, `#${settingCmd}`);
                }
                let command = '';
                let result = {};
                switch (operation) {
                    case 'build':
                        const schematicFile = this.getNodeParameter('schematicFile', i);
                        const originX = this.getNodeParameter('originX', i);
                        const originY = this.getNodeParameter('originY', i);
                        const originZ = this.getNodeParameter('originZ', i);
                        if (originX === '~' && originY === '~' && originZ === '~') {
                            command = `#build ${schematicFile}`;
                        }
                        else {
                            command = `#build ${schematicFile} ${originX} ${originY} ${originZ}`;
                        }
                        break;
                    case 'clearArea':
                        const clearX1 = this.getNodeParameter('x1', i);
                        const clearY1 = this.getNodeParameter('y1', i);
                        const clearZ1 = this.getNodeParameter('z1', i);
                        const clearX2 = this.getNodeParameter('x2', i);
                        const clearY2 = this.getNodeParameter('y2', i);
                        const clearZ2 = this.getNodeParameter('z2', i);
                        // Use sel command to select area then cleararea
                        await this.makeMinescriptRequest(baseUrl, credentials, `#sel ${clearX1} ${clearY1} ${clearZ1} ${clearX2} ${clearY2} ${clearZ2}`);
                        command = '#cleararea';
                        break;
                    case 'fillArea':
                        const fillX1 = this.getNodeParameter('x1', i);
                        const fillY1 = this.getNodeParameter('y1', i);
                        const fillZ1 = this.getNodeParameter('z1', i);
                        const fillX2 = this.getNodeParameter('x2', i);
                        const fillY2 = this.getNodeParameter('y2', i);
                        const fillZ2 = this.getNodeParameter('z2', i);
                        const fillBlockType = this.getNodeParameter('blockType', i);
                        // Use sel command to select area then fill
                        await this.makeMinescriptRequest(baseUrl, credentials, `#sel ${fillX1} ${fillY1} ${fillZ1} ${fillX2} ${fillY2} ${fillZ2}`);
                        command = `#fill ${fillBlockType}`;
                        break;
                    case 'mine':
                        const targetBlocks = this.getNodeParameter('targetBlocks', i);
                        const mineCount = this.getNodeParameter('mineCount', i);
                        if (mineCount > 0) {
                            command = `#mine ${mineCount} ${targetBlocks}`;
                        }
                        else {
                            command = `#mine ${targetBlocks}`;
                        }
                        break;
                    case 'farm':
                        const farmRange = this.getNodeParameter('farmRange', i);
                        command = `#farm ${farmRange}`;
                        break;
                    case 'tunnel':
                        const tunnelHeight = this.getNodeParameter('tunnelHeight', i);
                        const tunnelWidth = this.getNodeParameter('tunnelWidth', i);
                        const tunnelLength = this.getNodeParameter('tunnelLength', i);
                        command = `#tunnel ${tunnelHeight} ${tunnelWidth} ${tunnelLength}`;
                        break;
                    default:
                        throw new n8n_workflow_1.NodeOperationError(this.getNode(), `Unknown operation: ${operation}`);
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
            }
            catch (error) {
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
    async makeMinescriptRequest(baseUrl, credentials, command) {
        const sessionId = credentials.sessionId || this.generateUUID();
        const response = await axios_1.default.post(`${baseUrl}/cmd`, {
            command: command,
            session_id: sessionId,
        }, {
            headers: {
                'X-Client-ID': credentials.clientId,
                'X-Auth-Key': credentials.authKey,
                'X-Session-ID': sessionId,
                'Content-Type': 'application/json',
            },
        });
        return response.data;
    }
    generateUUID() {
        return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function (c) {
            const r = Math.random() * 16 | 0;
            const v = c == 'x' ? r : (r & 0x3 | 0x8);
            return v.toString(16);
        });
    }
}
exports.BaritoneBuilder = BaritoneBuilder;
