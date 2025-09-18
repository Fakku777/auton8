"use strict";
var __importDefault = (this && this.__importDefault) || function (mod) {
    return (mod && mod.__esModule) ? mod : { "default": mod };
};
Object.defineProperty(exports, "__esModule", { value: true });
exports.MinescriptCommand = void 0;
const n8n_workflow_1 = require("n8n-workflow");
const axios_1 = __importDefault(require("axios"));
class MinescriptCommand {
    constructor() {
        this.description = {
            displayName: 'Minescript Command',
            name: 'minescriptCommand',
            icon: 'file:minescript.svg',
            group: ['minecraft', 'automation'],
            version: 1,
            subtitle: '={{$parameter["operation"]}}',
            description: 'Execute Minescript commands and Python scripts',
            defaults: {
                name: 'Minescript Command',
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
                            name: 'Execute Python Script',
                            value: 'executeScript',
                            description: 'Run a Python script file',
                            action: 'Execute Python script',
                        },
                        {
                            name: 'Evaluate Python Code',
                            value: 'eval',
                            description: 'Evaluate Python expression or statements',
                            action: 'Evaluate Python code',
                        },
                        {
                            name: 'Chat Message',
                            value: 'chat',
                            description: 'Send a chat message',
                            action: 'Send chat message',
                        },
                        {
                            name: 'Echo Message',
                            value: 'echo',
                            description: 'Send message visible only to player',
                            action: 'Echo message',
                        },
                        {
                            name: 'Execute Minecraft Command',
                            value: 'execute',
                            description: 'Execute a Minecraft command',
                            action: 'Execute Minecraft command',
                        },
                        {
                            name: 'Copy Region',
                            value: 'copy',
                            description: 'Copy blocks in a region',
                            action: 'Copy region',
                        },
                        {
                            name: 'Paste Region',
                            value: 'paste',
                            description: 'Paste copied blocks',
                            action: 'Paste region',
                        },
                        {
                            name: 'Take Screenshot',
                            value: 'screenshot',
                            description: 'Take a screenshot',
                            action: 'Take screenshot',
                        },
                        {
                            name: 'List Jobs',
                            value: 'jobs',
                            description: 'List running Minescript jobs',
                            action: 'List jobs',
                        },
                        {
                            name: 'Kill Job',
                            value: 'killjob',
                            description: 'Terminate a Minescript job',
                            action: 'Kill job',
                        },
                        {
                            name: 'Suspend Job',
                            value: 'suspend',
                            description: 'Suspend a Minescript job',
                            action: 'Suspend job',
                        },
                        {
                            name: 'Resume Job',
                            value: 'resume',
                            description: 'Resume a suspended job',
                            action: 'Resume job',
                        },
                        {
                            name: 'Undo Last Command',
                            value: 'undo',
                            description: 'Undo the last block-changing command',
                            action: 'Undo last command',
                        },
                    ],
                    default: 'executeScript',
                },
                // Script execution fields
                {
                    displayName: 'Script Name',
                    name: 'scriptName',
                    type: 'string',
                    displayOptions: {
                        show: {
                            operation: ['executeScript'],
                        },
                    },
                    default: '',
                    description: 'Name of Python script to execute (without .py extension)',
                    placeholder: 'my_script',
                },
                {
                    displayName: 'Script Arguments',
                    name: 'scriptArgs',
                    type: 'string',
                    displayOptions: {
                        show: {
                            operation: ['executeScript'],
                        },
                    },
                    default: '',
                    description: 'Arguments to pass to the script',
                    placeholder: 'arg1 arg2 --flag value',
                },
                // Python code evaluation
                {
                    displayName: 'Python Code',
                    name: 'pythonCode',
                    type: 'string',
                    typeOptions: {
                        rows: 4,
                    },
                    displayOptions: {
                        show: {
                            operation: ['eval'],
                        },
                    },
                    default: '',
                    description: 'Python code to evaluate',
                    placeholder: 'minescript.echo("Hello World!")',
                },
                {
                    displayName: 'Additional Lines',
                    name: 'additionalLines',
                    type: 'fixedCollection',
                    displayOptions: {
                        show: {
                            operation: ['eval'],
                        },
                    },
                    default: {},
                    placeholder: 'Add Line',
                    options: [
                        {
                            displayName: 'Lines',
                            name: 'lines',
                            values: [
                                {
                                    displayName: 'Line',
                                    name: 'line',
                                    type: 'string',
                                    default: '',
                                    description: 'Additional line of Python code',
                                },
                            ],
                        },
                    ],
                },
                // Message fields
                {
                    displayName: 'Message',
                    name: 'message',
                    type: 'string',
                    displayOptions: {
                        show: {
                            operation: ['chat', 'echo'],
                        },
                    },
                    default: '',
                    description: 'Message to send',
                    placeholder: 'Hello, world!',
                },
                // Minecraft command
                {
                    displayName: 'Minecraft Command',
                    name: 'minecraftCommand',
                    type: 'string',
                    displayOptions: {
                        show: {
                            operation: ['execute'],
                        },
                    },
                    default: '',
                    description: 'Minecraft command to execute (without leading /)',
                    placeholder: 'tp @s 0 100 0',
                },
                // Copy region fields
                {
                    displayName: 'Copy From X1',
                    name: 'copyX1',
                    type: 'number',
                    displayOptions: {
                        show: {
                            operation: ['copy'],
                        },
                    },
                    default: 0,
                    description: 'X coordinate of first corner',
                },
                {
                    displayName: 'Copy From Y1',
                    name: 'copyY1',
                    type: 'number',
                    displayOptions: {
                        show: {
                            operation: ['copy'],
                        },
                    },
                    default: 0,
                    description: 'Y coordinate of first corner',
                },
                {
                    displayName: 'Copy From Z1',
                    name: 'copyZ1',
                    type: 'number',
                    displayOptions: {
                        show: {
                            operation: ['copy'],
                        },
                    },
                    default: 0,
                    description: 'Z coordinate of first corner',
                },
                {
                    displayName: 'Copy To X2',
                    name: 'copyX2',
                    type: 'number',
                    displayOptions: {
                        show: {
                            operation: ['copy'],
                        },
                    },
                    default: 10,
                    description: 'X coordinate of second corner',
                },
                {
                    displayName: 'Copy To Y2',
                    name: 'copyY2',
                    type: 'number',
                    displayOptions: {
                        show: {
                            operation: ['copy'],
                        },
                    },
                    default: 10,
                    description: 'Y coordinate of second corner',
                },
                {
                    displayName: 'Copy To Z2',
                    name: 'copyZ2',
                    type: 'number',
                    displayOptions: {
                        show: {
                            operation: ['copy'],
                        },
                    },
                    default: 10,
                    description: 'Z coordinate of second corner',
                },
                {
                    displayName: 'Copy Label',
                    name: 'copyLabel',
                    type: 'string',
                    displayOptions: {
                        show: {
                            operation: ['copy'],
                        },
                    },
                    default: '',
                    description: 'Optional label for the copied region',
                    placeholder: 'house_foundation',
                },
                {
                    displayName: 'No Limit',
                    name: 'noLimit',
                    type: 'boolean',
                    displayOptions: {
                        show: {
                            operation: ['copy'],
                        },
                    },
                    default: false,
                    description: 'Allow copying regions larger than 1600 chunks',
                },
                // Paste region fields
                {
                    displayName: 'Paste At X',
                    name: 'pasteX',
                    type: 'number',
                    displayOptions: {
                        show: {
                            operation: ['paste'],
                        },
                    },
                    default: 0,
                    description: 'X coordinate to paste at',
                },
                {
                    displayName: 'Paste At Y',
                    name: 'pasteY',
                    type: 'number',
                    displayOptions: {
                        show: {
                            operation: ['paste'],
                        },
                    },
                    default: 0,
                    description: 'Y coordinate to paste at',
                },
                {
                    displayName: 'Paste At Z',
                    name: 'pasteZ',
                    type: 'number',
                    displayOptions: {
                        show: {
                            operation: ['paste'],
                        },
                    },
                    default: 0,
                    description: 'Z coordinate to paste at',
                },
                {
                    displayName: 'Paste Label',
                    name: 'pasteLabel',
                    type: 'string',
                    displayOptions: {
                        show: {
                            operation: ['paste'],
                        },
                    },
                    default: '',
                    description: 'Label of region to paste (empty for most recent)',
                    placeholder: 'house_foundation',
                },
                // Screenshot options
                {
                    displayName: 'Screenshot Name',
                    name: 'screenshotName',
                    type: 'string',
                    displayOptions: {
                        show: {
                            operation: ['screenshot'],
                        },
                    },
                    default: '',
                    description: 'Optional name for screenshot file',
                    placeholder: 'my_screenshot',
                },
                // Job management
                {
                    displayName: 'Job ID',
                    name: 'jobId',
                    type: 'string',
                    displayOptions: {
                        show: {
                            operation: ['killjob', 'suspend', 'resume'],
                        },
                    },
                    default: '',
                    description: 'Job ID to operate on (use -1 for all jobs with killjob)',
                    placeholder: '1, 2, -1',
                },
                {
                    displayName: 'Show All Jobs',
                    name: 'showAllJobs',
                    type: 'boolean',
                    displayOptions: {
                        show: {
                            operation: ['jobs'],
                        },
                    },
                    default: false,
                    description: 'Show all jobs including child jobs',
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
                            displayName: 'Wait for Completion',
                            name: 'waitForCompletion',
                            type: 'boolean',
                            default: false,
                            description: 'Wait for script execution to complete before continuing',
                        },
                        {
                            displayName: 'Timeout (seconds)',
                            name: 'timeout',
                            type: 'number',
                            default: 300,
                            description: 'Maximum time to wait for completion',
                        },
                        {
                            displayName: 'Capture Output',
                            name: 'captureOutput',
                            type: 'boolean',
                            default: true,
                            description: 'Capture and return command output',
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
                let command = '';
                let result = {};
                switch (operation) {
                    case 'executeScript':
                        const scriptName = this.getNodeParameter('scriptName', i);
                        const scriptArgs = this.getNodeParameter('scriptArgs', i, '');
                        if (scriptArgs) {
                            command = `\\${scriptName} ${scriptArgs}`;
                        }
                        else {
                            command = `\\${scriptName}`;
                        }
                        break;
                    case 'eval':
                        const pythonCode = this.getNodeParameter('pythonCode', i);
                        const additionalLines = this.getNodeParameter('additionalLines', i, {});
                        const codeLines = [pythonCode];
                        if (additionalLines.lines && Array.isArray(additionalLines.lines)) {
                            for (const lineObj of additionalLines.lines) {
                                if (lineObj.line) {
                                    codeLines.push(lineObj.line);
                                }
                            }
                        }
                        command = `\\eval ${codeLines.map(line => `"${line}"`).join(' ')}`;
                        break;
                    case 'chat':
                        const chatMessage = this.getNodeParameter('message', i);
                        command = `\\eval "minescript.chat('${chatMessage.replace(/'/g, "\\'")}')"`;
                        break;
                    case 'echo':
                        const echoMessage = this.getNodeParameter('message', i);
                        command = `\\eval "minescript.echo('${echoMessage.replace(/'/g, "\\'")}')"`;
                        break;
                    case 'execute':
                        const minecraftCommand = this.getNodeParameter('minecraftCommand', i);
                        command = `\\eval "minescript.execute('${minecraftCommand.replace(/'/g, "\\'")}')"`;
                        break;
                    case 'copy':
                        const x1 = this.getNodeParameter('copyX1', i);
                        const y1 = this.getNodeParameter('copyY1', i);
                        const z1 = this.getNodeParameter('copyZ1', i);
                        const x2 = this.getNodeParameter('copyX2', i);
                        const y2 = this.getNodeParameter('copyY2', i);
                        const z2 = this.getNodeParameter('copyZ2', i);
                        const copyLabel = this.getNodeParameter('copyLabel', i, '');
                        const noLimit = this.getNodeParameter('noLimit', i);
                        if (copyLabel && noLimit) {
                            command = `\\copy ${x1} ${y1} ${z1} ${x2} ${y2} ${z2} ${copyLabel} no_limit`;
                        }
                        else if (copyLabel) {
                            command = `\\copy ${x1} ${y1} ${z1} ${x2} ${y2} ${z2} ${copyLabel}`;
                        }
                        else if (noLimit) {
                            command = `\\copy ${x1} ${y1} ${z1} ${x2} ${y2} ${z2} no_limit`;
                        }
                        else {
                            command = `\\copy ${x1} ${y1} ${z1} ${x2} ${y2} ${z2}`;
                        }
                        break;
                    case 'paste':
                        const pasteX = this.getNodeParameter('pasteX', i);
                        const pasteY = this.getNodeParameter('pasteY', i);
                        const pasteZ = this.getNodeParameter('pasteZ', i);
                        const pasteLabel = this.getNodeParameter('pasteLabel', i, '');
                        if (pasteLabel) {
                            command = `\\paste ${pasteX} ${pasteY} ${pasteZ} ${pasteLabel}`;
                        }
                        else {
                            command = `\\paste ${pasteX} ${pasteY} ${pasteZ}`;
                        }
                        break;
                    case 'screenshot':
                        const screenshotName = this.getNodeParameter('screenshotName', i, '');
                        if (screenshotName) {
                            command = `\\eval "minescript.screenshot('${screenshotName}')"`;
                        }
                        else {
                            command = `\\eval "minescript.screenshot()"`;
                        }
                        break;
                    case 'jobs':
                        const showAllJobs = this.getNodeParameter('showAllJobs', i);
                        if (showAllJobs) {
                            command = '\\jobs all';
                        }
                        else {
                            command = '\\jobs';
                        }
                        break;
                    case 'killjob':
                        const killJobId = this.getNodeParameter('jobId', i);
                        command = `\\killjob ${killJobId}`;
                        break;
                    case 'suspend':
                        const suspendJobId = this.getNodeParameter('jobId', i, '');
                        if (suspendJobId) {
                            command = `\\suspend ${suspendJobId}`;
                        }
                        else {
                            command = '\\suspend';
                        }
                        break;
                    case 'resume':
                        const resumeJobId = this.getNodeParameter('jobId', i, '');
                        if (resumeJobId) {
                            command = `\\resume ${resumeJobId}`;
                        }
                        else {
                            command = '\\resume';
                        }
                        break;
                    case 'undo':
                        command = '\\undo';
                        break;
                    default:
                        throw new n8n_workflow_1.NodeOperationError(this.getNode(), `Unknown operation: ${operation}`);
                }
                // Execute the command
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
exports.MinescriptCommand = MinescriptCommand;
