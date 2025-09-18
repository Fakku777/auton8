import { IAuthenticateGeneric, ICredentialTestRequest, ICredentialType, INodeProperties } from 'n8n-workflow';
export declare class MinescriptApi implements ICredentialType {
    name: string;
    displayName: string;
    documentationUrl: string;
    properties: INodeProperties[];
    authenticate: IAuthenticateGeneric;
    test: ICredentialTestRequest;
}
