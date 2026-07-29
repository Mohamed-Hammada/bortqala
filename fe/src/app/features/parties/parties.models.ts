export interface BusinessParty {
  id: string;
  code: string;
  name: string;
  nameEn: string | null;
  partyType: string;
  contactPerson: string | null;
  phone: string | null;
  email: string | null;
  address: string | null;
  notes: string | null;
  managedType: string;
  responsiblePartyId: string | null;
  relationshipStartDate: string | null;
  relationshipEndDate: string | null;
  currencyCode: string;
  invoicePolicy: string;
  paymentTerms: string;
  taxId: string | null;
  bankAccount: string | null;
  active: boolean;
  version: number;
  createdAt: number;
  updatedAt: number;
}

export interface BusinessPartyPayload {
  code: string;
  name: string;
  nameEn: string | null;
  partyType: string;
  contactPerson: string | null;
  phone: string | null;
  email: string | null;
  address: string | null;
  notes: string | null;
  managedType: string;
  responsiblePartyId: string | null;
  relationshipStartDate: string | null;
  relationshipEndDate: string | null;
  currencyCode: string;
  invoicePolicy: string;
  paymentTerms: string;
  taxId: string | null;
  bankAccount: string | null;
  active: boolean;
  version: number | null;
}
