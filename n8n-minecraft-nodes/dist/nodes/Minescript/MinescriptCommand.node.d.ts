import { IExecuteFunctions, INodeExecutionData, INodeType, INodeTypeDescription } from 'n8n-workflow';
export declare class MinescriptCommand implements INodeType {
    description: INodeTypeDescription;
    execute(this: IExecuteFunctions): Promise<INodeExecutionData[][]>;
    private makeMinescriptRequest;
    private generateUUID;
}
