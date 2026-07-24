export interface BusinessParty {
  id: string;
  code: string;
  name: string;
  partyType: string;
  contactPerson: string | null;
  phone: string | null;
  notes: string | null;
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
  active: boolean;
  version: number | null;
}
