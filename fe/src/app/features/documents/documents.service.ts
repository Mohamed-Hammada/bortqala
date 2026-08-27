import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { DocFolder, DocSearchResult, DocTag } from './documents.models';

@Injectable({ providedIn: 'root' })
export class DocumentsService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/v1/documents';

  listFolders(): Observable<DocFolder[]> {
    return this.http.get<DocFolder[]>(`${this.baseUrl}/folders`);
  }

  createFolder(name: string, parentId?: string): Observable<DocFolder> {
    return this.http.post<DocFolder>(`${this.baseUrl}/folders`, { name, parentId });
  }

  renameFolder(id: string, name: string): Observable<void> {
    return this.http.put<void>(`${this.baseUrl}/folders/${id}/rename`, null, {
      params: new HttpParams().set('name', name),
    });
  }

  moveFolder(id: string, parentId?: string): Observable<void> {
    return this.http.put<void>(`${this.baseUrl}/folders/${id}/move`, null, {
      params: parentId ? new HttpParams().set('parentId', parentId) : new HttpParams(),
    });
  }

  deleteFolder(id: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/folders/${id}`);
  }

  listTags(): Observable<DocTag[]> {
    return this.http.get<DocTag[]>(`${this.baseUrl}/tags`);
  }

  createTag(name: string, color?: string): Observable<DocTag> {
    return this.http.post<DocTag>(`${this.baseUrl}/tags`, { name, color });
  }

  deleteTag(id: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/tags/${id}`);
  }

  searchDocuments(query: string, tagId?: string): Observable<DocSearchResult[]> {
    let params = new HttpParams().set('q', query);
    if (tagId) params = params.set('tag', tagId);
    return this.http.get<DocSearchResult[]>(`${this.baseUrl}/attachments`, { params });
  }

  assignTag(attachmentId: string, tagId: string): Observable<void> {
    return this.http.post<void>(`${this.baseUrl}/attachments/${attachmentId}/tags/${tagId}`, {});
  }

  removeTag(attachmentId: string, tagId: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/attachments/${attachmentId}/tags/${tagId}`);
  }
}