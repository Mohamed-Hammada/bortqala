export interface DocFolder {
  id: string;
  name: string;
  parentId: string | null;
  createdAt: number;
}

export interface DocTag {
  id: string;
  name: string;
  color: string | null;
}

export interface DocSearchResult {
  id: string;
  name: string;
  kind: string;
  folderId: string | null;
  folderName: string | null;
  tags: DocTag[];
  createdAt: number;
}