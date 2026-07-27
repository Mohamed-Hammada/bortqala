export interface Company {
  id: string;
  code: string;
  name: string;
  taxNumber?: string;
  commercialRegistry?: string;
  active: boolean;
  createdAt: number;
  updatedAt: number;
}

export interface Branch {
  id: string;
  companyId: string;
  code: string;
  name: string;
  location?: string;
  active: boolean;
  createdAt: number;
  updatedAt: number;
}

export interface Warehouse {
  id: string;
  branchId: string;
  code: string;
  name: string;
  location?: string;
  active: boolean;
  createdAt: number;
  updatedAt: number;
}

export interface Department {
  id: string;
  companyId: string;
  code: string;
  name: string;
  managerId?: string;
  active: boolean;
  createdAt: number;
  updatedAt: number;
}

export interface OrganizationHierarchy {
  companies: Company[];
  branches: Branch[];
  warehouses: Warehouse[];
  departments: Department[];
}
