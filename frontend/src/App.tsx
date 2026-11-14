import React from 'react';
import { BrowserRouter, Routes, Route, Navigate, Link } from 'react-router-dom';
import { AuthProvider, useAuth } from './context/AuthContext';
import { AppLayout } from './components/layout/AppLayout';
import {
  LandingPage,
  LoginPage,
  RegisterPage,
  ForgotPasswordPage,
} from './pages/auth/AuthPages';
import { StudentDashboardPage } from './pages/dashboard/StudentDashboardPage';
import {
  DiscoverItemsPage,
  ItemDetailsPage,
  CreateEditListingPage,
  MyListingsPage,
} from './pages/items/ItemPages';
import {
  BorrowingPage,
  LendingPage,
  ExchangeMarketplacePage,
  ExchangeDetailsPage,
  RentalManagementPage,
  DonationMarketplacePage,
  DonationRequestsPage,
  TransactionHistoryPage,
  TransactionDetailsPage,
} from './pages/sharing/SharingPages';
import {
  TrustScoreDetailsPage,
  ReviewsPage,
  NotificationsPage,
  DisputeReportingPage,
  UserProfilePage,
  AccountSettingsPage,
} from './pages/profile/ProfileCommunityPages';
import {
  AdminDashboardPage,
  AdminAnalyticsPage,
  AdminUsersPage,
  AdminListingsPage,
  AdminTransactionsPage,
  AdminDisputesPage,
  ModeratorDashboardPage,
} from './pages/admin/AdminPages';
import { UserRole } from './types';

const ProtectedRoute: React.FC<{
  children: React.ReactNode;
  allowedRoles?: UserRole[];
}> = ({ children, allowedRoles }) => {
  const { user, loading } = useAuth();

  if (loading) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-[#F8F7F0]">
        <div className="w-10 h-10 border-4 border-[#164B3B] border-t-transparent rounded-full animate-spin" />
      </div>
    );
  }

  if (!user) {
    return <Navigate to="/login" replace />;
  }

  if (allowedRoles && !allowedRoles.includes(user.role)) {
    return <Navigate to="/unauthorized" replace />;
  }

  return <AppLayout>{children}</AppLayout>;
};

const UnauthorizedPage: React.FC = () => (
  <div className="min-h-screen bg-[#F8F7F0] flex flex-col items-center justify-center p-6 text-center">
    <div className="bg-white p-8 rounded-3xl border border-[#A8C3A0] max-w-md shadow-sm space-y-4">
      <span className="inline-block px-3 py-1 rounded-full bg-red-100 text-red-800 text-xs font-bold uppercase">
        403 Access Restricted
      </span>
      <h1 className="text-2xl font-bold text-[#202923]">Insufficient Role Permissions</h1>
      <p className="text-sm text-[#737B75]">
        Your current campus role does not have authorization to access this administrative or moderation view.
      </p>
      <Link
        to="/dashboard"
        className="inline-block px-5 py-2.5 rounded-xl bg-[#164B3B] text-white text-sm font-semibold hover:bg-[#238B68]"
      >
        Return to Student Dashboard
      </Link>
    </div>
  </div>
);

const NotFoundPage: React.FC = () => (
  <div className="min-h-screen bg-[#F8F7F0] flex flex-col items-center justify-center p-6 text-center">
    <div className="bg-white p-8 rounded-3xl border border-[#A8C3A0] max-w-md shadow-sm space-y-4">
      <span className="inline-block px-3 py-1 rounded-full bg-[#E8F2EA] text-[#164B3B] text-xs font-bold uppercase">
        404 Page Not Found
      </span>
      <h1 className="text-2xl font-bold text-[#202923]">Resource Route Not Found</h1>
      <p className="text-sm text-[#737B75]">
        The campus page or listing you requested could not be found or has been archived.
      </p>
      <Link
        to="/"
        className="inline-block px-5 py-2.5 rounded-xl bg-[#164B3B] text-white text-sm font-semibold hover:bg-[#238B68]"
      >
        Back to ReCircle Home
      </Link>
    </div>
  </div>
);

export const App: React.FC = () => {
  return (
    <AuthProvider>
      <BrowserRouter>
        <Routes>
          {/* Public Routes */}
          <Route path="/" element={<LandingPage />} />
          <Route path="/login" element={<LoginPage />} />
          <Route path="/register" element={<RegisterPage />} />
          <Route path="/forgot-password" element={<ForgotPasswordPage />} />
          <Route path="/reset-password" element={<ForgotPasswordPage />} />
          <Route path="/unauthorized" element={<UnauthorizedPage />} />

          {/* Authenticated Student & Community Routes */}
          <Route
            path="/dashboard"
            element={
              <ProtectedRoute>
                <StudentDashboardPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/discover"
            element={
              <ProtectedRoute>
                <DiscoverItemsPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/items/:id"
            element={
              <ProtectedRoute>
                <ItemDetailsPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/listings/create"
            element={
              <ProtectedRoute>
                <CreateEditListingPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/listings/:id/edit"
            element={
              <ProtectedRoute>
                <CreateEditListingPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/my-listings"
            element={
              <ProtectedRoute>
                <MyListingsPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/borrowing"
            element={
              <ProtectedRoute>
                <BorrowingPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/lending"
            element={
              <ProtectedRoute>
                <LendingPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/exchanges"
            element={
              <ProtectedRoute>
                <ExchangeMarketplacePage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/exchanges/:id"
            element={
              <ProtectedRoute>
                <ExchangeDetailsPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/rentals"
            element={
              <ProtectedRoute>
                <RentalManagementPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/donations"
            element={
              <ProtectedRoute>
                <DonationMarketplacePage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/donation-requests"
            element={
              <ProtectedRoute>
                <DonationRequestsPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/transactions"
            element={
              <ProtectedRoute>
                <TransactionHistoryPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/transactions/:id"
            element={
              <ProtectedRoute>
                <TransactionDetailsPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/trust-score"
            element={
              <ProtectedRoute>
                <TrustScoreDetailsPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/reviews"
            element={
              <ProtectedRoute>
                <ReviewsPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/notifications"
            element={
              <ProtectedRoute>
                <NotificationsPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/disputes"
            element={
              <ProtectedRoute>
                <DisputeReportingPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/profile"
            element={
              <ProtectedRoute>
                <UserProfilePage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/settings"
            element={
              <ProtectedRoute>
                <AccountSettingsPage />
              </ProtectedRoute>
            }
          />

          {/* Moderator & Administrator Protected Routes */}
          <Route
            path="/moderator/dashboard"
            element={
              <ProtectedRoute allowedRoles={['MODERATOR', 'ADMIN']}>
                <ModeratorDashboardPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/admin/dashboard"
            element={
              <ProtectedRoute allowedRoles={['ADMIN', 'MODERATOR']}>
                <AdminDashboardPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/admin/analytics"
            element={
              <ProtectedRoute allowedRoles={['ADMIN', 'MODERATOR']}>
                <AdminAnalyticsPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/admin/users"
            element={
              <ProtectedRoute allowedRoles={['ADMIN', 'MODERATOR']}>
                <AdminUsersPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/admin/listings"
            element={
              <ProtectedRoute allowedRoles={['ADMIN', 'MODERATOR']}>
                <AdminListingsPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/admin/transactions"
            element={
              <ProtectedRoute allowedRoles={['ADMIN', 'MODERATOR']}>
                <AdminTransactionsPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/admin/disputes"
            element={
              <ProtectedRoute allowedRoles={['ADMIN', 'MODERATOR']}>
                <AdminDisputesPage />
              </ProtectedRoute>
            }
          />

          {/* Fallback 404 */}
          <Route path="*" element={<NotFoundPage />} />
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  );
};

export default App;
