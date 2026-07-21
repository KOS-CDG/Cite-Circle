import { create } from 'zustand';

import { clearAuthToken } from '@/lib/token-storage';
import type { User } from '@/types';

interface AuthState {
  user: User | null;
  isAuthenticated: boolean;
  /** True right after signup until the profile-setup wizard finishes — keeps the root nav in the (auth) group. */
  needsProfileSetup: boolean;
  setUser: (user: User | null, options?: { needsProfileSetup?: boolean }) => void;
  completeProfileSetup: (user: User) => void;
  signOut: () => Promise<void>;
}

export const useAuthStore = create<AuthState>((set) => ({
  user: null,
  isAuthenticated: false,
  needsProfileSetup: false,
  setUser: (user, options) =>
    set({ user, isAuthenticated: user !== null, needsProfileSetup: options?.needsProfileSetup ?? false }),
  completeProfileSetup: (user) => set({ user, needsProfileSetup: false }),
  signOut: async () => {
    await clearAuthToken();
    set({ user: null, isAuthenticated: false, needsProfileSetup: false });
  },
}));
