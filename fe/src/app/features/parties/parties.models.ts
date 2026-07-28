export interface BusinessParty {
  id: string;
  code: string;
  name: string;
  partyType: string;
  contactPerson: string | null;
  phone: string | null;
  notes: string | null;
  managedType: string;
  responsiblePartyId: string | null;
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
  partyType: string;
  contactPerson: string | null;
  phone: string | null;
  notes: string | null;
  managedType: string;
  responsiblePartyId: string | null;
  currencyCode: string;
  invoicePolicy: string;
  paymentTerms: string;
  taxId: string | null;
  bankAccount: string | null;
  active: boolean;
  version: number | null;
}
