import React, { createContext, useContext, useState, useEffect, useCallback } from 'react';
import { UserSummary } from '../types';
import { apiClient } from '../api/client';

interface ToastMessage {
  id: string;
  type: 'success' | 'error' | 'info';
  text: string;
}

interface AuthContextValue {
  user: UserSummary | null;
  loading: boolean;
  unreadNotifications: number;
  setSession: (accessToken: string, refreshToken: string, userData: UserSummary) => void;
  refreshUserProfile: () => Promise<void>;
  logout: () => Promise<void>;
  showToast: (text: string, type?: 'success' | 'error' | 'info') => void;
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined);

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [user, setUser] = useState<UserSummary | null>(() => {
    const cached = localStorage.getItem('recircle_user');
    if (cached) {
      try {
        return JSON.parse(cached) as UserSummary;
      } catch {
        return null;
      }
    }
    return null;
  });
  const [loading, setLoading] = useState<boolean>(true);
  const [unreadNotifications, setUnreadNotifications] = useState<number>(0);
  const [toasts, setToasts] = useState<ToastMessage[]>([]);

  const showToast = useCallback((text: string, type: 'success' | 'error' | 'info' = 'success') => {
    const id = Math.random().toString(36).substring(2, 9);
    setToasts((prev) => [...prev, { id, type, text }]);
    setTimeout(() => {
      setToasts((prev) => prev.filter((t) => t.id !== id));
    }, 4000);
  }, []);

  const refreshUserProfile = useCallback(async () => {
    const token = localStorage.getItem('recircle_access_token');
    if (!token) {
      setUser(null);
      setLoading(false);
      return;
    }
    try {
      const [meRes, notifRes] = await Promise.all([
        apiClient.get<UserSummary>('/users/me'),
        apiClient.get<{ unreadCount: number }>('/notifications'),
      ]);
      setUser(meRes.data);
      localStorage.setItem('recircle_user', JSON.stringify(meRes.data));
      setUnreadNotifications(notifRes.data.unreadCount || 0);
    } catch {
      localStorage.removeItem('recircle_access_token');
      localStorage.removeItem('recircle_refresh_token');
      localStorage.removeItem('recircle_user');
      setUser(null);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    refreshUserProfile();
  }, [refreshUserProfile]);

  const setSession = (accessToken: string, refreshToken: string, userData: UserSummary) => {
    localStorage.setItem('recircle_access_token', accessToken);
    localStorage.setItem('recircle_refresh_token', refreshToken);
    localStorage.setItem('recircle_user', JSON.stringify(userData));
    setUser(userData);
    refreshUserProfile();
  };

  const logout = async () => {
    try {
      await apiClient.post('/auth/logout');
    } catch {
      // Ignore logout network errors
    } finally {
      localStorage.removeItem('recircle_access_token');
      localStorage.removeItem('recircle_refresh_token');
      localStorage.removeItem('recircle_user');
      setUser(null);
      setUnreadNotifications(0);
      showToast('Logged out safely.', 'info');
    }
  };

  return (
    <AuthContext.Provider
      value={{
        user,
        loading,
        unreadNotifications,
        setSession,
        refreshUserProfile,
        logout,
        showToast,
      }}
    >
      {children}
      {/* Floating Toast Container */}
      <div className="fixed bottom-5 right-5 z-50 flex flex-col gap-2 max-w-md">
        {toasts.map((t) => (
          <div
            key={t.id}
            className={`px-4 py-3 rounded-xl shadow-lg border text-sm font-medium flex items-center gap-2.5 transition-all ${
              t.type === 'error'
                ? 'bg-red-950 text-red-100 border-red-700'
                : t.type === 'info'
                ? 'bg-[#202923] text-white border-[#737B75]'
                : 'bg-[#164B3B] text-white border-[#238B68]'
            }`}
          >
            <span className="w-2 h-2 rounded-full bg-[#A8C3A0] shrink-0" />
            <span>{t.text}</span>
          </div>
        ))}
      </div>
    </AuthContext.Provider>
  );
};

export const useAuth = (): AuthContextValue => {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used within AuthProvider');
  return ctx;
};
