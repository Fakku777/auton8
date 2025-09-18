import { IExecuteFunctions, INodeExecutionData, INodeType, INodeTypeDescription } from 'n8n-workflow';
export declare class MinecraftSession implements INodeType {
    description: INodeTypeDescription;
    execute(this: IExecuteFunctions): Promise<INodeExecutionData[][]>;
    private startSession;
    private endSession;
    private getSessionStatus;
    private listenForEvents;
    private getEntities;
    private executeMinescriptCommand;
    private generateUUID;
}
