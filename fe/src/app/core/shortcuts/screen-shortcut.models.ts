export type ShortcutAvailability =
  | 'AVAILABLE'
  | 'NO_ROLE'
  | 'MENU_NOT_ALLOWED'
  | 'FEATURE_DISABLED'
  | 'PAGE_REMOVED'
  | 'DISABLED';

export interface ScreenShortcutDestination {
  pageCode: string;
  menuId: string;
  route: string;
  titleKey: string;
  module: string;
  requiredFeature?: string | null;
}

export interface ScreenShortcut {
  id?: string | null;
  pageCode: string;
  menuId: string;
  route: string;
  titleKey: string;
  secondKeyCode: string;
  displayKey: string;
  enabled: boolean;
  defaultShortcut: boolean;
  availabilityStatus: ShortcutAvailability;
  unavailableReasonKey?: string | null;
}

export interface ScreenShortcutProfile {
  profileMode: 'DEFAULT' | 'CUSTOM';
  version: number;
  shortcuts: ScreenShortcut[];
  availableDestinations: ScreenShortcutDestination[];
  updatedAt?: string | null;
}

export interface ShortcutItemRequest {
  secondKeyCode: string;
  pageCode: string;
  enabled: boolean;
}

export interface ReplaceScreenShortcutsRequest {
  expectedVersion: number;
  shortcuts: ShortcutItemRequest[];
}
