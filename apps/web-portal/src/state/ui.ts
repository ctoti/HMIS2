import { create } from 'zustand';

interface UiState {
  readonly navOpen: boolean;
  toggleNav: () => void;
  closeNav: () => void;
}

export const useUiStore = create<UiState>((set) => ({
  navOpen: false,
  toggleNav: () => set((s) => ({ navOpen: !s.navOpen })),
  closeNav: () => set({ navOpen: false }),
}));
