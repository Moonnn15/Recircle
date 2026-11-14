import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import {
  Award,
  Package,
  Clock,
  CheckCircle2,
  Bell,
  ArrowRight,
  ShieldCheck,
  Sparkles,
  Calendar,
  AlertCircle,
} from 'lucide-react';
import { apiClient } from '../../api/client';
import { BorrowRequestItem, ItemListing, NotificationItem, UserSummary } from '../../types';
import { ItemCard } from '../../components/cards/ItemCard';

interface DashboardData {
  user: UserSummary;
  totalListedItems: number;
  activeBorrowRequests: number;
  currentlyBorrowedCount: number;
  currentlyLentCount: number;
  pendingApprovals: number;
  completedTransactions: number;
  currentlyBorrowedItems: BorrowRequestItem[];
  currentlyLentItems: BorrowRequestItem[];
  upcomingReturns: BorrowRequestItem[];
  recentNotifications: NotificationItem[];
  recommendedItems: ItemListing[];
}

export const StudentDashboardPage: React.FC = () => {
  const [data, setData] = useState<DashboardData | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    apiClient
      .get<DashboardData>('/users/me/dashboard')
      .then((res) => setData(res.data))
      .finally(() => setLoading(false));
  }, []);

  if (loading || !data) {
    return (
      <div className="space-y-6 animate-pulse">
        <div className="h-36 bg-white rounded-3xl border border-[#A8C3A0]/40" />
        <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
          {[1, 2, 3, 4].map((n) => (
            <div key={n} className="h-28 bg-white rounded-2xl border border-[#A8C3A0]/40" />
          ))}
        </div>
      </div>
    );
  }

  const { user } = data;

  return (
    <div className="space-y-8">
      {/* Welcome Banner with Trust Score & Profile Completion */}
      <div className="bg-gradient-to-r from-[#164B3B] to-[#238B68] rounded-3xl p-6 sm:p-8 text-white shadow-md flex flex-col lg:flex-row lg:items-center justify-between gap-6">
        <div className="space-y-2">
          <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-white/15 text-xs font-semibold">
            <Sparkles className="w-3.5 h-3.5 text-[#A8C3A0]" />
            {user.department || 'Community Member'}{user.yearOfStudy ? ` • Year ${user.yearOfStudy}` : ''}
          </div>
          <h1 className="font-display text-2xl sm:text-3xl font-bold">
            Welcome back, {user.fullName}!
          </h1>
          <p className="text-xs sm:text-sm text-[#E8F2EA]/90 max-w-xl">
            Your community resource-sharing hub is synced in real time. Share unused items, tools, electronics, and
            equipment to strengthen your community reputation.
          </p>
        </div>

        <div className="flex flex-wrap items-center gap-4">
          <Link
            to="/trust-score"
            className="bg-white/12 hover:bg-white/20 backdrop-blur-xs border border-white/20 rounded-2xl p-4 min-w-[160px] transition-colors"
          >
            <div className="flex items-center justify-between">
              <span className="text-xs text-[#E8F2EA]">Trust Score</span>
              <Award className="w-4 h-4 text-[#A8C3A0]" />
            </div>
            <p className="font-display text-3xl font-bold mt-1">
              {Number(user.trustScore).toFixed(1)}
              <span className="text-xs font-normal text-[#A8C3A0]"> / 100</span>
            </p>
            <span className="inline-block mt-1 px-2 py-0.5 rounded-md bg-[#A8C3A0] text-[#164B3B] text-[10px] font-bold">
              {user.trustLevel}
            </span>
          </Link>

          <Link
            to="/profile"
            className="bg-white/12 hover:bg-white/20 backdrop-blur-xs border border-white/20 rounded-2xl p-4 min-w-[160px] transition-colors"
          >
            <div className="flex items-center justify-between">
              <span className="text-xs text-[#E8F2EA]">Profile Completion</span>
              <ShieldCheck className="w-4 h-4 text-[#A8C3A0]" />
            </div>
            <p className="font-display text-3xl font-bold mt-1">{user.profileCompletionPct}%</p>
            <div className="mt-2 w-full h-1.5 bg-white/20 rounded-full overflow-hidden">
              <div
                className="h-full bg-[#A8C3A0]"
                style={{ width: `${user.profileCompletionPct}%` }}
              />
            </div>
          </Link>
        </div>
      </div>

      {/* Unverified Account Email Alert */}
      {!user.campusVerified && (
        <div className="p-4 rounded-2xl bg-amber-50 border border-amber-300 flex flex-col sm:flex-row items-start sm:items-center justify-between gap-3">
          <div className="flex items-center gap-3">
            <AlertCircle className="w-5 h-5 text-amber-700 shrink-0" />
            <div>
              <p className="text-xs font-bold text-amber-900">
                Verify your email address to boost your Trust Score by +15 points!
              </p>
              <p className="text-xs text-amber-800">
                Use your 6-digit registration OTP (or local dev test OTP: 123456) on your Profile page.
              </p>
            </div>
          </div>
          <Link
            to="/profile"
            className="px-4 py-2 rounded-xl bg-amber-800 text-white text-xs font-semibold shrink-0"
          >
            Verify Now
          </Link>
        </div>
      )}

      {/* Live Backend KPI Grid */}
      <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-6 gap-4">
        {[
          { label: 'My Listed Items', value: data.totalListedItems, to: '/my-listings', icon: <Package className="w-4 h-4 text-[#238B68]" /> },
          { label: 'Active Requests', value: data.activeBorrowRequests, to: '/borrowing', icon: <Clock className="w-4 h-4 text-[#238B68]" /> },
          { label: 'Currently Borrowed', value: data.currentlyBorrowedCount, to: '/borrowing', icon: <Calendar className="w-4 h-4 text-[#238B68]" /> },
          { label: 'Currently Lent', value: data.currentlyLentCount, to: '/lending', icon: <ShieldCheck className="w-4 h-4 text-[#238B68]" /> },
          { label: 'Pending Approvals', value: data.pendingApprovals, to: '/lending', icon: <Bell className="w-4 h-4 text-amber-600" /> },
          { label: 'Completed Shares', value: data.completedTransactions, to: '/transactions', icon: <CheckCircle2 className="w-4 h-4 text-[#164B3B]" /> },
        ].map((stat) => (
          <Link
            key={stat.label}
            to={stat.to}
            className="bg-white p-4 rounded-2xl border border-[#A8C3A0]/50 hover:border-[#238B68] shadow-xs transition-all"
          >
            <div className="flex items-center justify-between">
              <span className="text-[11px] font-semibold text-[#737B75]">{stat.label}</span>
              {stat.icon}
            </div>
            <p className="font-display text-2xl font-bold text-[#202923] mt-2">{stat.value}</p>
          </Link>
        ))}
      </div>

      {/* Active Borrowings / Upcoming Returns & Recent Notifications */}
      <div className="grid lg:grid-cols-12 gap-6">
        <div className="lg:col-span-7 bg-white rounded-3xl p-6 border border-[#A8C3A0]/50 shadow-xs space-y-4">
          <div className="flex items-center justify-between">
            <div>
              <h2 className="font-display text-lg font-bold text-[#164B3B]">
                Active Borrowings & Upcoming Return Dates
              </h2>
              <p className="text-xs text-[#737B75]">
                Track handover confirmations and due dates to maintain a 100% On-Time Return Score
              </p>
            </div>
            <Link to="/borrowing" className="text-xs font-semibold text-[#238B68] hover:underline">
              View All →
            </Link>
          </div>

          {data.upcomingReturns.length === 0 ? (
            <div className="p-8 rounded-2xl bg-[#F8F7F0] text-center space-y-2">
              <p className="text-sm font-semibold text-[#202923]">No active borrowed items right now</p>
              <p className="text-xs text-[#737B75]">
                Explore textbooks, tools, electronics, and gear shared by fellow community members.
              </p>
              <Link
                to="/discover"
                className="inline-flex items-center gap-1.5 mt-2 px-4 py-2 rounded-xl bg-[#164B3B] text-white text-xs font-semibold"
              >
                Browse Marketplace
                <ArrowRight className="w-3.5 h-3.5" />
              </Link>
            </div>
          ) : (
            <div className="space-y-3">
              {data.upcomingReturns.map((b) => (
                <div
                  key={b.id}
                  className="p-4 rounded-2xl bg-[#F8F7F0] border border-[#A8C3A0]/40 flex items-center justify-between gap-4"
                >
                  <div>
                    <p className="text-sm font-bold text-[#202923]">{b.itemTitle}</p>
                    <p className="text-xs text-[#737B75]">
                      Owner: {b.lenderName} • Pickup: {b.campusLocation}
                    </p>
                  </div>
                  <div className="text-right shrink-0">
                    <span className="px-2.5 py-1 rounded-full bg-[#E8F2EA] text-[#164B3B] text-xs font-bold">
                      Due: {b.endDate}
                    </span>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>

        {/* Recent Notifications */}
        <div className="lg:col-span-5 bg-white rounded-3xl p-6 border border-[#A8C3A0]/50 shadow-xs space-y-4">
          <div className="flex items-center justify-between">
            <h2 className="font-display text-lg font-bold text-[#164B3B]">Recent Notifications</h2>
            <Link to="/notifications" className="text-xs font-semibold text-[#238B68] hover:underline">
              Notification Center →
            </Link>
          </div>

          {data.recentNotifications.length === 0 ? (
            <p className="text-xs text-[#737B75] py-6 text-center">No recent notifications.</p>
          ) : (
            <div className="space-y-2.5">
              {data.recentNotifications.map((n) => (
                <Link
                  key={n.id}
                  to={n.targetPath || '/notifications'}
                  className={`block p-3.5 rounded-2xl border transition-colors ${
                    n.read
                      ? 'bg-[#F8F7F0] border-transparent'
                      : 'bg-[#E8F2EA]/70 border-[#238B68]/40'
                  }`}
                >
                  <div className="flex items-center justify-between">
                    <span className="text-xs font-bold text-[#164B3B]">{n.title}</span>
                    {!n.read && <span className="w-2 h-2 rounded-full bg-[#238B68]" />}
                  </div>
                  <p className="text-xs text-[#737B75] mt-1 line-clamp-2">{n.message}</p>
                </Link>
              ))}
            </div>
          )}
        </div>
      </div>

      {/* Recommended Available Community Resources */}
      <div className="space-y-4">
        <div className="flex items-center justify-between">
          <div>
            <h2 className="font-display text-xl font-bold text-[#164B3B]">
              Recommended Community Resources
            </h2>
            <p className="text-xs text-[#737B75]">
              Available items from high-trust community members
            </p>
          </div>
          <Link
            to="/discover"
            className="inline-flex items-center gap-1 text-xs font-semibold text-[#164B3B] hover:text-[#238B68]"
          >
            Explore All Listings <ArrowRight className="w-3.5 h-3.5" />
          </Link>
        </div>

        <div className="grid sm:grid-cols-2 lg:grid-cols-4 gap-5">
          {data.recommendedItems.map((item) => (
            <ItemCard key={item.id} item={item} />
          ))}
        </div>
      </div>
    </div>
  );
};
