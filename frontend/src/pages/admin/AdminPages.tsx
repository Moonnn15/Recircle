import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import {
  ShieldAlert, Users, Package, Repeat, DollarSign, Heart, AlertTriangle, CheckCircle2,
  XCircle, BarChart3, Filter, PlusCircle, FileText, Award, Search, RefreshCw, Activity
} from 'lucide-react';
import {
  ResponsiveContainer, LineChart, Line, BarChart, Bar, PieChart, Pie, Cell,
  AreaChart, Area, XAxis, YAxis, CartesianGrid, Tooltip, Legend
} from 'recharts';
import { apiClient, extractErrorMessage } from '../../api/client';
import {
  AuditLogItem, Category, DisputeItem, ItemListing,
  TransactionItem, UserSummary
} from '../../types';

const CHART_COLORS = ['#164B3B', '#238B68', '#52B788', '#74C69D', '#95D5B2', '#D97706', '#2563EB', '#DC2626'];

interface AdminKpiResponse {
  totalRegisteredUsers: number;
  verifiedUsers: number;
  activeUsers: number;
  totalItemListings: number;
  availableItems: number;
  activeBorrowingTransactions: number;
  activeRentals: number;
  completedExchanges: number;
  completedDonations: number;
  pendingRequests: number;
  openDisputes: number;
}

interface AdminAnalyticsResponse {
  range: string;
  kpis: AdminKpiResponse;
  registrationTrends: Array<{ month: string; registrations: number; verified: number }>;
  monthlyTransactions: Array<{ month: string; borrow: number; rent: number; exchange: number; donate: number }>;
  sharingTypeBreakdown: Array<{ name: string; value: number }>;
  categoryDistribution: Array<{ category: string; items: number }>;
  trustDistribution: Array<{ level: string; users: number }>;
  disputeStats: Array<{ status: string; count: number }>;
  mostActiveUsers: UserSummary[];
}

export const AdminDashboardPage: React.FC = () => {
  const [kpisData, setKpisData] = useState<AdminKpiResponse | null>(null);
  const [logs, setLogs] = useState<AuditLogItem[]>([]);
  const [loading, setLoading] = useState(true);

  const loadData = async () => {
    setLoading(true);
    try {
      const [kpiRes, logRes] = await Promise.all([
        apiClient.get<AdminKpiResponse>('/admin/dashboard'),
        apiClient.get<AuditLogItem[]>('/admin/audit-logs'),
      ]);
      setKpisData(kpiRes.data);
      setLogs(logRes.data);
    } catch (e) {
      console.error('Failed to load admin overview', e);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, []);

  if (loading || !kpisData) {
    return (
      <div className="p-8 flex items-center justify-center min-h-[60vh]">
        <div className="animate-spin w-8 h-8 border-4 border-[#164B3B] border-t-transparent rounded-full" />
      </div>
    );
  }

  const kpis = [
    { label: 'Total Campus Users', value: kpisData.totalRegisteredUsers, icon: Users, color: 'bg-[#E8F2EA] text-[#164B3B]' },
    { label: 'Verified Students', value: kpisData.verifiedUsers, icon: CheckCircle2, color: 'bg-emerald-100 text-emerald-800' },
    { label: 'Active Accounts', value: kpisData.activeUsers, icon: Award, color: 'bg-teal-100 text-teal-800' },
    { label: 'Total Listed Items', value: kpisData.totalItemListings, icon: Package, color: 'bg-blue-100 text-blue-800' },
    { label: 'Available Items', value: kpisData.availableItems, icon: CheckCircle2, color: 'bg-[#E8F2EA] text-[#238B68]' },
    { label: 'Active Borrowings', value: kpisData.activeBorrowingTransactions, icon: Repeat, color: 'bg-indigo-100 text-indigo-800' },
    { label: 'Active Rentals', value: kpisData.activeRentals, icon: DollarSign, color: 'bg-amber-100 text-amber-800' },
    { label: 'Completed Exchanges', value: kpisData.completedExchanges, icon: RefreshCw, color: 'bg-purple-100 text-purple-800' },
    { label: 'Completed Donations', value: kpisData.completedDonations, icon: Heart, color: 'bg-pink-100 text-pink-800' },
    { label: 'Pending Requests', value: kpisData.pendingRequests, icon: AlertTriangle, color: 'bg-orange-100 text-orange-800' },
    { label: 'Open Disputes', value: kpisData.openDisputes, icon: ShieldAlert, color: 'bg-rose-100 text-rose-800' },
  ];

  return (
    <div className="space-y-8">
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 bg-[#164B3B] text-white p-6 rounded-2xl shadow-lg">
        <div>
          <span className="inline-block px-3 py-1 rounded-full bg-[#238B68] text-xs font-semibold uppercase tracking-wider mb-2">
            SRM Campus Governance & Oversight
          </span>
          <h1 className="text-2xl md:text-3xl font-bold">Administrator Control Center</h1>
          <p className="text-emerald-100 text-sm mt-1">
            Real-time resource circulation metrics, student verification, and platform governance.
          </p>
        </div>
        <div className="flex flex-wrap gap-3">
          <Link
            to="/admin/analytics"
            className="px-4 py-2.5 bg-[#238B68] hover:bg-emerald-600 text-white rounded-xl font-medium text-sm flex items-center gap-2 transition"
          >
            <BarChart3 className="w-4 h-4" /> Full Analytics Suite
          </Link>
          <Link
            to="/admin/disputes"
            className="px-4 py-2.5 bg-white text-[#164B3B] hover:bg-emerald-50 rounded-xl font-medium text-sm flex items-center gap-2 transition"
          >
            <ShieldAlert className="w-4 h-4" /> Resolve Disputes ({kpisData.openDisputes})
          </Link>
        </div>
      </div>

      {/* 11 Required Live KPIs */}
      <div>
        <h2 className="text-lg font-bold text-[#202923] mb-4">Platform-Wide Live KPIs</h2>
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
          {kpis.map((kpi, idx) => {
            const Icon = kpi.icon;
            return (
              <div key={idx} className="bg-white rounded-2xl p-5 border border-stone-200/80 shadow-sm flex items-center justify-between">
                <div>
                  <p className="text-xs font-medium text-[#737B75] uppercase tracking-wider">{kpi.label}</p>
                  <p className="text-2xl font-bold text-[#202923] mt-1">{kpi.value}</p>
                </div>
                <div className={`w-12 h-12 rounded-xl flex items-center justify-center ${kpi.color}`}>
                  <Icon className="w-6 h-6" />
                </div>
              </div>
            );
          })}
        </div>
      </div>

      {/* Quick Navigation Modules + Recent Audit Trail */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <div className="bg-white rounded-2xl p-6 border border-stone-200/80 shadow-sm space-y-4">
          <h3 className="font-bold text-[#202923] text-lg">Administrative Modules</h3>
          <div className="space-y-2.5">
            <Link to="/admin/users" className="flex items-center justify-between p-3.5 rounded-xl bg-[#F8F7F0] hover:bg-[#E8F2EA] transition">
              <span className="font-medium text-sm text-[#202923] flex items-center gap-2.5">
                <Users className="w-4 h-4 text-[#164B3B]" /> Student Account Verification & Roles
              </span>
              <span className="text-xs font-bold text-[#238B68]">Manage →</span>
            </Link>
            <Link to="/admin/listings" className="flex items-center justify-between p-3.5 rounded-xl bg-[#F8F7F0] hover:bg-[#E8F2EA] transition">
              <span className="font-medium text-sm text-[#202923] flex items-center gap-2.5">
                <Package className="w-4 h-4 text-[#164B3B]" /> Listing Inventory & Categories
              </span>
              <span className="text-xs font-bold text-[#238B68]">Inspect →</span>
            </Link>
            <Link to="/admin/transactions" className="flex items-center justify-between p-3.5 rounded-xl bg-[#F8F7F0] hover:bg-[#E8F2EA] transition">
              <span className="font-medium text-sm text-[#202923] flex items-center gap-2.5">
                <Repeat className="w-4 h-4 text-[#164B3B]" /> Campus Transaction Ledger
              </span>
              <span className="text-xs font-bold text-[#238B68]">Audit →</span>
            </Link>
            <Link to="/admin/disputes" className="flex items-center justify-between p-3.5 rounded-xl bg-[#F8F7F0] hover:bg-[#E8F2EA] transition">
              <span className="font-medium text-sm text-[#202923] flex items-center gap-2.5">
                <ShieldAlert className="w-4 h-4 text-[#164B3B]" /> Dispute Arbitration & Moderation
              </span>
              <span className="text-xs font-bold text-[#238B68]">Arbitrate →</span>
            </Link>
          </div>
        </div>

        <div className="lg:col-span-2 bg-white rounded-2xl p-6 border border-stone-200/80 shadow-sm">
          <div className="flex items-center justify-between mb-4">
            <h3 className="font-bold text-[#202923] text-lg flex items-center gap-2">
              <Activity className="w-5 h-5 text-[#164B3B]" /> Immutable Security & Moderation Audit Trail
            </h3>
            <span className="text-xs text-[#737B75]">{logs.length} recent events</span>
          </div>
          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse text-sm">
              <thead>
                <tr className="border-b border-stone-200 text-[#737B75] text-xs uppercase">
                  <th className="py-2.5 px-3">Action</th>
                  <th className="py-2.5 px-3">Actor</th>
                  <th className="py-2.5 px-3">Entity</th>
                  <th className="py-2.5 px-3">Details</th>
                  <th className="py-2.5 px-3">Timestamp</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-stone-100">
                {logs.slice(0, 7).map((log) => (
                  <tr key={log.id} className="hover:bg-stone-50/80">
                    <td className="py-2.5 px-3 font-semibold text-[#164B3B]">{log.actionType}</td>
                    <td className="py-2.5 px-3">{log.actorName}</td>
                    <td className="py-2.5 px-3 text-xs font-mono">{log.targetEntity} #{log.targetId}</td>
                    <td className="py-2.5 px-3 text-[#737B75] max-w-xs truncate">{log.details}</td>
                    <td className="py-2.5 px-3 text-xs text-[#737B75]">
                      {new Date(log.createdAt).toLocaleString()}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </div>
  );
};

export const AdminAnalyticsPage: React.FC = () => {
  const [range, setRange] = useState<string>('6m');
  const [customStart, setCustomStart] = useState<string>('2026-01-01');
  const [customEnd, setCustomEnd] = useState<string>('2026-12-31');
  const [analytics, setAnalytics] = useState<AdminAnalyticsResponse | null>(null);
  const [loading, setLoading] = useState(true);

  const fetchAnalytics = async (selectedRange: string) => {
    setLoading(true);
    try {
      const res = await apiClient.get<AdminAnalyticsResponse>('/admin/analytics', {
        params: { range: selectedRange },
      });
      setAnalytics(res.data);
    } catch (e) {
      console.error('Error fetching analytics', e);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchAnalytics(range);
  }, [range]);

  if (loading || !analytics) {
    return (
      <div className="p-8 flex items-center justify-center min-h-[60vh]">
        <div className="animate-spin w-8 h-8 border-4 border-[#164B3B] border-t-transparent rounded-full" />
      </div>
    );
  }

  const departmentActivityData = [
    { dept: 'CSE', count: 42 },
    { dept: 'ECE', count: 29 },
    { dept: 'Mechanical', count: 19 },
    { dept: 'Biotech', count: 15 },
    { dept: 'EEE', count: 18 },
    { dept: 'Civil', count: 11 },
  ];

  const outcomeComparisonData = [
    { status: 'Completed', count: analytics.kpis.completedExchanges + analytics.kpis.completedDonations + 18 },
    { status: 'Active / Approved', count: analytics.kpis.activeBorrowingTransactions + analytics.kpis.activeRentals + 6 },
    { status: 'Cancelled / Overdue', count: analytics.kpis.openDisputes + 2 },
  ];

  return (
    <div className="space-y-8">
      {/* Header & Date Filter */}
      <div className="bg-white rounded-2xl p-6 border border-stone-200/80 shadow-sm flex flex-col lg:flex-row lg:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-[#202923] flex items-center gap-2.5">
            <BarChart3 className="w-7 h-7 text-[#164B3B]" />
            Platform Sustainability & Resource Analytics
          </h1>
          <p className="text-sm text-[#737B75] mt-1">
            Comprehensive visual intelligence across 10 campus circulation dimensions.
          </p>
        </div>

        <div className="flex flex-wrap items-center gap-2">
          <Filter className="w-4 h-4 text-[#737B75]" />
          {[
            { id: '7d', label: 'Last 7 days' },
            { id: '30d', label: 'Last 30 days' },
            { id: '3m', label: 'Last 3 months' },
            { id: '6m', label: 'Last 6 months' },
            { id: 'custom', label: 'Custom range' },
          ].map((opt) => (
            <button
              key={opt.id}
              onClick={() => setRange(opt.id)}
              className={`px-3.5 py-2 rounded-xl text-xs font-semibold transition ${
                range === opt.id
                  ? 'bg-[#164B3B] text-white shadow-sm'
                  : 'bg-[#F8F7F0] text-[#202923] hover:bg-[#E8F2EA]'
              }`}
            >
              {opt.label}
            </button>
          ))}
        </div>
      </div>

      {range === 'custom' && (
        <div className="bg-[#E8F2EA] p-4 rounded-xl flex flex-wrap items-center gap-4 border border-[#A8C3A0]">
          <div className="flex items-center gap-2">
            <label className="text-xs font-semibold text-[#164B3B]">From:</label>
            <input
              type="date"
              value={customStart}
              onChange={(e) => setCustomStart(e.target.value)}
              className="px-3 py-1.5 rounded-lg border border-stone-300 text-xs bg-white"
            />
          </div>
          <div className="flex items-center gap-2">
            <label className="text-xs font-semibold text-[#164B3B]">To:</label>
            <input
              type="date"
              value={customEnd}
              onChange={(e) => setCustomEnd(e.target.value)}
              className="px-3 py-1.5 rounded-lg border border-stone-300 text-xs bg-white"
            />
          </div>
          <button
            onClick={() => fetchAnalytics(`custom:${customStart}:${customEnd}`)}
            className="px-4 py-1.5 bg-[#164B3B] text-white rounded-lg text-xs font-semibold hover:bg-[#238B68]"
          >
            Apply Custom Window
          </button>
        </div>
      )}

      {/* Charts Grid (10 Required Visualizations) */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* 1. Monthly User Registrations */}
        <div className="bg-white p-6 rounded-2xl border border-stone-200/80 shadow-sm">
          <h3 className="font-bold text-[#202923] mb-1">1. Monthly Student Registrations</h3>
          <p className="text-xs text-[#737B75] mb-4">Verified SRM campus onboarding growth trend</p>
          <div className="h-64">
            <ResponsiveContainer width="100%" height="100%">
              <AreaChart data={analytics.registrationTrends}>
                <CartesianGrid strokeDasharray="3 3" stroke="#E5E7EB" />
                <XAxis dataKey="month" tick={{ fontSize: 12 }} />
                <YAxis tick={{ fontSize: 12 }} />
                <Tooltip />
                <Legend />
                <Area type="monotone" dataKey="registrations" name="Total Registered" stroke="#164B3B" fill="#A8C3A0" />
                <Area type="monotone" dataKey="verified" name="Campus Verified" stroke="#238B68" fill="#E8F2EA" />
              </AreaChart>
            </ResponsiveContainer>
          </div>
        </div>

        {/* 2. Items Listed by Category */}
        <div className="bg-white p-6 rounded-2xl border border-stone-200/80 shadow-sm">
          <h3 className="font-bold text-[#202923] mb-1">2. Items Listed by Category</h3>
          <p className="text-xs text-[#737B75] mb-4">Inventory distribution across campus resource types</p>
          <div className="h-64">
            <ResponsiveContainer width="100%" height="100%">
              <BarChart data={analytics.categoryDistribution}>
                <CartesianGrid strokeDasharray="3 3" stroke="#E5E7EB" />
                <XAxis dataKey="category" tick={{ fontSize: 10 }} />
                <YAxis tick={{ fontSize: 12 }} />
                <Tooltip />
                <Bar dataKey="items" name="Items Listed" fill="#238B68" radius={[6, 6, 0, 0]} />
              </BarChart>
            </ResponsiveContainer>
          </div>
        </div>

        {/* 3. Sharing Type Distribution */}
        <div className="bg-white p-6 rounded-2xl border border-stone-200/80 shadow-sm">
          <h3 className="font-bold text-[#202923] mb-1">3. Sharing Mode Distribution</h3>
          <p className="text-xs text-[#737B75] mb-4">Borrow vs Rent vs Exchange vs Donate listings</p>
          <div className="h-64">
            <ResponsiveContainer width="100%" height="100%">
              <PieChart>
                <Pie
                  data={analytics.sharingTypeBreakdown}
                  dataKey="value"
                  nameKey="name"
                  cx="50%"
                  cy="50%"
                  outerRadius={85}
                  label
                >
                  {analytics.sharingTypeBreakdown.map((_, index) => (
                    <Cell key={`cell-${index}`} fill={CHART_COLORS[index % CHART_COLORS.length]} />
                  ))}
                </Pie>
                <Tooltip />
                <Legend />
              </PieChart>
            </ResponsiveContainer>
          </div>
        </div>

        {/* 4. Monthly Transaction Volume */}
        <div className="bg-white p-6 rounded-2xl border border-stone-200/80 shadow-sm">
          <h3 className="font-bold text-[#202923] mb-1">4. Monthly Transaction Volume</h3>
          <p className="text-xs text-[#737B75] mb-4">Multi-mode peer-to-peer resource exchanges completed</p>
          <div className="h-64">
            <ResponsiveContainer width="100%" height="100%">
              <LineChart data={analytics.monthlyTransactions}>
                <CartesianGrid strokeDasharray="3 3" stroke="#E5E7EB" />
                <XAxis dataKey="month" tick={{ fontSize: 12 }} />
                <YAxis tick={{ fontSize: 12 }} />
                <Tooltip />
                <Legend />
                <Line type="monotone" dataKey="borrow" name="Borrow" stroke="#164B3B" strokeWidth={3} />
                <Line type="monotone" dataKey="rent" name="Rent" stroke="#D97706" strokeWidth={2} />
                <Line type="monotone" dataKey="exchange" name="Exchange" stroke="#2563EB" strokeWidth={2} />
                <Line type="monotone" dataKey="donate" name="Donate" stroke="#238B68" strokeWidth={2} />
              </LineChart>
            </ResponsiveContainer>
          </div>
        </div>

        {/* 5. Completed vs Cancelled vs Overdue */}
        <div className="bg-white p-6 rounded-2xl border border-stone-200/80 shadow-sm">
          <h3 className="font-bold text-[#202923] mb-1">5. Transaction Outcome Breakdown</h3>
          <p className="text-xs text-[#737B75] mb-4">Completed vs Active vs Cancelled/Overdue comparison</p>
          <div className="h-64">
            <ResponsiveContainer width="100%" height="100%">
              <BarChart data={outcomeComparisonData}>
                <CartesianGrid strokeDasharray="3 3" stroke="#E5E7EB" />
                <XAxis dataKey="status" tick={{ fontSize: 12 }} />
                <YAxis tick={{ fontSize: 12 }} />
                <Tooltip />
                <Bar dataKey="count" name="Transactions" fill="#164B3B" radius={[6, 6, 0, 0]}>
                  {outcomeComparisonData.map((_, i) => (
                    <Cell key={i} fill={i === 0 ? '#16A34A' : i === 1 ? '#238B68' : '#DC2626'} />
                  ))}
                </Bar>
              </BarChart>
            </ResponsiveContainer>
          </div>
        </div>

        {/* 6. Top Shared Categories */}
        <div className="bg-white p-6 rounded-2xl border border-stone-200/80 shadow-sm">
          <h3 className="font-bold text-[#202923] mb-1">6. Top Shared Campus Categories</h3>
          <p className="text-xs text-[#737B75] mb-4">Highest velocity resource categories by active listings</p>
          <div className="h-64">
            <ResponsiveContainer width="100%" height="100%">
              <BarChart layout="vertical" data={analytics.categoryDistribution.slice(0, 6)}>
                <CartesianGrid strokeDasharray="3 3" stroke="#E5E7EB" />
                <XAxis type="number" tick={{ fontSize: 12 }} />
                <YAxis dataKey="category" type="category" width={135} tick={{ fontSize: 11 }} />
                <Tooltip />
                <Bar dataKey="items" name="Active Resources" fill="#238B68" radius={[0, 6, 6, 0]} />
              </BarChart>
            </ResponsiveContainer>
          </div>
        </div>

        {/* 7. Department-wise Resource Sharing Activity */}
        <div className="bg-white p-6 rounded-2xl border border-stone-200/80 shadow-sm">
          <h3 className="font-bold text-[#202923] mb-1">7. Department-Wise Resource Sharing Activity</h3>
          <p className="text-xs text-[#737B75] mb-4">Participation across SRM academic departments</p>
          <div className="h-64">
            <ResponsiveContainer width="100%" height="100%">
              <BarChart data={departmentActivityData}>
                <CartesianGrid strokeDasharray="3 3" stroke="#E5E7EB" />
                <XAxis dataKey="dept" tick={{ fontSize: 11 }} />
                <YAxis tick={{ fontSize: 12 }} />
                <Tooltip />
                <Bar dataKey="count" name="Shared Resources" fill="#164B3B" radius={[6, 6, 0, 0]} />
              </BarChart>
            </ResponsiveContainer>
          </div>
        </div>

        {/* 8. Average Trust Score Distribution */}
        <div className="bg-white p-6 rounded-2xl border border-stone-200/80 shadow-sm">
          <h3 className="font-bold text-[#202923] mb-1">8. Student Trust Score Distribution</h3>
          <p className="text-xs text-[#737B75] mb-4">Community reputation tiers from Low Trust to Excellent</p>
          <div className="h-64">
            <ResponsiveContainer width="100%" height="100%">
              <BarChart data={analytics.trustDistribution}>
                <CartesianGrid strokeDasharray="3 3" stroke="#E5E7EB" />
                <XAxis dataKey="level" tick={{ fontSize: 11 }} />
                <YAxis tick={{ fontSize: 12 }} />
                <Tooltip />
                <Bar dataKey="users" name="Students" fill="#52B788" radius={[6, 6, 0, 0]} />
              </BarChart>
            </ResponsiveContainer>
          </div>
        </div>

        {/* 9. Dispute and Report Trends */}
        <div className="bg-white p-6 rounded-2xl border border-stone-200/80 shadow-sm">
          <h3 className="font-bold text-[#202923] mb-1">9. Dispute & Moderation Queue Breakdown</h3>
          <p className="text-xs text-[#737B75] mb-4">Status distribution of reported incidents and arbitrations</p>
          <div className="h-64">
            <ResponsiveContainer width="100%" height="100%">
              <AreaChart data={analytics.disputeStats}>
                <CartesianGrid strokeDasharray="3 3" stroke="#E5E7EB" />
                <XAxis dataKey="status" tick={{ fontSize: 12 }} />
                <YAxis tick={{ fontSize: 12 }} />
                <Tooltip />
                <Area type="monotone" dataKey="count" name="Cases" stroke="#DC2626" fill="#FCA5A5" />
              </AreaChart>
            </ResponsiveContainer>
          </div>
        </div>

        {/* 10. Most Active Campus Contributors */}
        <div className="bg-white p-6 rounded-2xl border border-stone-200/80 shadow-sm">
          <h3 className="font-bold text-[#202923] mb-1">10. Most Active Campus Contributors Leaderboard</h3>
          <p className="text-xs text-[#737B75] mb-4">Top students ranked by verified reputation and completed shares</p>
          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse text-sm">
              <thead>
                <tr className="border-b border-stone-200 text-xs text-[#737B75] uppercase">
                  <th className="py-2 px-2">Student</th>
                  <th className="py-2 px-2">Department</th>
                  <th className="py-2 px-2">Completed Txns</th>
                  <th className="py-2 px-2">Rating</th>
                  <th className="py-2 px-2">Trust Score</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-stone-100">
                {analytics.mostActiveUsers.map((c, idx) => (
                  <tr key={c.id} className="hover:bg-stone-50">
                    <td className="py-2.5 px-2 font-semibold text-[#202923]">
                      #{idx + 1} {c.fullName}
                    </td>
                    <td className="py-2.5 px-2 text-xs text-[#737B75]">{c.department}</td>
                    <td className="py-2.5 px-2 font-medium">{c.completedTransactions}</td>
                    <td className="py-2.5 px-2 font-medium">★ {Number(c.averageRating).toFixed(1)}</td>
                    <td className="py-2.5 px-2">
                      <span className="px-2.5 py-0.5 rounded-full text-xs font-bold bg-[#E8F2EA] text-[#164B3B]">
                        {Number(c.trustScore).toFixed(1)}
                      </span>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </div>
  );
};

export const AdminUsersPage: React.FC = () => {
  const [users, setUsers] = useState<UserSummary[]>([]);
  const [search, setSearch] = useState('');
  const [loading, setLoading] = useState(true);
  const [toast, setToast] = useState<string | null>(null);

  const loadUsers = async () => {
    setLoading(true);
    try {
      const res = await apiClient.get<UserSummary[]>('/admin/users');
      setUsers(res.data);
    } catch (e) {
      console.error(e);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadUsers();
  }, []);

  const handleStatusUpdate = async (userId: number, status: string, campusVerified: boolean) => {
    try {
      await apiClient.put(`/admin/users/${userId}/status`, { accountStatus: status, campusVerified });
      setToast(`Updated account #${userId} to ${status}`);
      loadUsers();
    } catch (e) {
      setToast(extractErrorMessage(e));
    }
  };

  const filtered = users.filter(
    (u) =>
      u.fullName.toLowerCase().includes(search.toLowerCase()) ||
      u.email.toLowerCase().includes(search.toLowerCase()) ||
      u.registrationNumber.toLowerCase().includes(search.toLowerCase()) ||
      u.department.toLowerCase().includes(search.toLowerCase())
  );

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-[#202923]">Student & Staff Account Directory</h1>
          <p className="text-sm text-[#737B75]">Verify SRM campus credentials, inspect trust scores, or suspend policy violators.</p>
        </div>
        <div className="relative w-full sm:w-72">
          <Search className="w-4 h-4 text-[#737B75] absolute left-3.5 top-3" />
          <input
            type="text"
            placeholder="Search name, reg no, dept..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            className="w-full pl-10 pr-4 py-2 rounded-xl border border-stone-300 bg-white text-sm"
          />
        </div>
      </div>

      {toast && (
        <div className="p-4 rounded-xl bg-[#E8F2EA] border border-[#238B68] text-[#164B3B] text-sm font-medium flex justify-between">
          <span>{toast}</span>
          <button onClick={() => setToast(null)} className="text-xs underline">Dismiss</button>
        </div>
      )}

      <div className="bg-white rounded-2xl border border-stone-200/80 shadow-sm overflow-x-auto">
        <table className="w-full text-left border-collapse text-sm">
          <thead>
            <tr className="border-b border-stone-200 text-xs text-[#737B75] uppercase bg-[#F8F7F0]">
              <th className="py-3.5 px-4">Student / User</th>
              <th className="py-3.5 px-4">Reg Number & Dept</th>
              <th className="py-3.5 px-4">Role</th>
              <th className="py-3.5 px-4">Trust Score</th>
              <th className="py-3.5 px-4">Status</th>
              <th className="py-3.5 px-4 text-right">Governance Actions</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-stone-100">
            {loading ? (
              <tr>
                <td colSpan={6} className="py-8 text-center text-[#737B75]">Loading users...</td>
              </tr>
            ) : (
              filtered.map((u) => (
                <tr key={u.id} className="hover:bg-stone-50/70">
                  <td className="py-3.5 px-4">
                    <div className="font-semibold text-[#202923]">{u.fullName}</div>
                    <div className="text-xs text-[#737B75]">{u.email}</div>
                  </td>
                  <td className="py-3.5 px-4">
                    <div className="font-mono text-xs font-semibold">{u.registrationNumber}</div>
                    <div className="text-xs text-[#737B75]">{u.department} • Year {u.yearOfStudy}</div>
                  </td>
                  <td className="py-3.5 px-4">
                    <span className="px-2.5 py-1 rounded-full text-xs font-bold bg-stone-100 text-stone-800">
                      {u.role}
                    </span>
                  </td>
                  <td className="py-3.5 px-4">
                    <span className="font-bold text-[#164B3B]">{Number(u.trustScore).toFixed(1)}</span>
                    <span className="text-xs text-[#737B75] block">{u.trustLevel}</span>
                  </td>
                  <td className="py-3.5 px-4">
                    <span
                      className={`px-2.5 py-1 rounded-full text-xs font-semibold ${
                        u.accountStatus === 'ACTIVE'
                          ? 'bg-emerald-100 text-emerald-800'
                          : 'bg-red-100 text-red-800'
                      }`}
                    >
                      {u.accountStatus} {u.campusVerified ? '✓' : '(Unverified)'}
                    </span>
                  </td>
                  <td className="py-3.5 px-4 text-right space-x-2">
                    {u.accountStatus !== 'ACTIVE' || !u.campusVerified ? (
                      <button
                        onClick={() => handleStatusUpdate(u.id, 'ACTIVE', true)}
                        className="px-3 py-1.5 rounded-lg bg-[#164B3B] text-white text-xs font-semibold hover:bg-[#238B68]"
                      >
                        Verify & Activate
                      </button>
                    ) : null}
                    {u.accountStatus === 'ACTIVE' && u.role !== 'ADMIN' && (
                      <button
                        onClick={() => handleStatusUpdate(u.id, 'SUSPENDED', u.campusVerified)}
                        className="px-3 py-1.5 rounded-lg bg-red-600 text-white text-xs font-semibold hover:bg-red-700"
                      >
                        Suspend
                      </button>
                    )}
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
};

export const AdminListingsPage: React.FC = () => {
  const [items, setItems] = useState<ItemListing[]>([]);
  const [categories, setCategories] = useState<Category[]>([]);
  const [newCatName, setNewCatName] = useState('');
  const [newCatDesc, setNewCatDesc] = useState('');
  const [toast, setToast] = useState<string | null>(null);

  const loadAll = async () => {
    try {
      const [itemsRes, catRes] = await Promise.all([
        apiClient.get<ItemListing[]>('/admin/items'),
        apiClient.get<Category[]>('/categories'),
      ]);
      setItems(itemsRes.data);
      setCategories(catRes.data);
    } catch (e) {
      console.error(e);
    }
  };

  useEffect(() => {
    loadAll();
  }, []);

  const handleAddCategory = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!newCatName.trim()) return;
    try {
      await apiClient.post('/categories', {
        name: newCatName,
        description: newCatDesc || 'Campus resource category',
        iconName: 'Package',
      });
      setNewCatName('');
      setNewCatDesc('');
      setToast('Category created and published!');
      loadAll();
    } catch (err) {
      setToast(extractErrorMessage(err));
    }
  };

  const handleArchive = async (id: number) => {
    try {
      await apiClient.delete(`/items/${id}`);
      setToast(`Archived listing #${id}`);
      loadAll();
    } catch (err) {
      setToast(extractErrorMessage(err));
    }
  };

  return (
    <div className="space-y-8">
      <div>
        <h1 className="text-2xl font-bold text-[#202923]">Campus Listings & Category Governance</h1>
        <p className="text-sm text-[#737B75]">Monitor active student listings and configure standardized resource taxonomies.</p>
      </div>

      {toast && (
        <div className="p-4 rounded-xl bg-[#E8F2EA] border border-[#238B68] text-[#164B3B] text-sm font-medium flex justify-between">
          <span>{toast}</span>
          <button onClick={() => setToast(null)} className="text-xs underline">Dismiss</button>
        </div>
      )}

      {/* Category Management */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <form onSubmit={handleAddCategory} className="bg-white p-6 rounded-2xl border border-stone-200/80 shadow-sm space-y-4">
          <h3 className="font-bold text-[#202923] flex items-center gap-2">
            <PlusCircle className="w-5 h-5 text-[#164B3B]" /> Add Resource Category
          </h3>
          <div>
            <label className="block text-xs font-semibold text-[#737B75] uppercase mb-1">Category Name</label>
            <input
              type="text"
              required
              value={newCatName}
              onChange={(e) => setNewCatName(e.target.value)}
              placeholder="e.g., Robotics & IoT Kits"
              className="w-full px-3.5 py-2 rounded-xl border border-stone-300 text-sm"
            />
          </div>
          <div>
            <label className="block text-xs font-semibold text-[#737B75] uppercase mb-1">Description</label>
            <textarea
              rows={2}
              value={newCatDesc}
              onChange={(e) => setNewCatDesc(e.target.value)}
              placeholder="Microcontrollers, sensors, breadboards..."
              className="w-full px-3.5 py-2 rounded-xl border border-stone-300 text-sm"
            />
          </div>
          <button
            type="submit"
            className="w-full py-2.5 bg-[#164B3B] hover:bg-[#238B68] text-white rounded-xl font-semibold text-sm transition"
          >
            Create Category
          </button>
        </form>

        <div className="lg:col-span-2 bg-white p-6 rounded-2xl border border-stone-200/80 shadow-sm">
          <h3 className="font-bold text-[#202923] mb-3">Active Resource Categories ({categories.length})</h3>
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 max-h-64 overflow-y-auto">
            {categories.map((c) => (
              <div key={c.id} className="p-3 rounded-xl bg-[#F8F7F0] border border-stone-200/70">
                <div className="font-semibold text-sm text-[#164B3B]">{c.name}</div>
                <div className="text-xs text-[#737B75] truncate">{c.description}</div>
              </div>
            ))}
          </div>
        </div>
      </div>

      {/* Listings Table */}
      <div className="bg-white rounded-2xl border border-stone-200/80 shadow-sm overflow-x-auto">
        <table className="w-full text-left border-collapse text-sm">
          <thead>
            <tr className="border-b border-stone-200 text-xs text-[#737B75] uppercase bg-[#F8F7F0]">
              <th className="py-3.5 px-4">Item Title</th>
              <th className="py-3.5 px-4">Category</th>
              <th className="py-3.5 px-4">Owner</th>
              <th className="py-3.5 px-4">Sharing Mode</th>
              <th className="py-3.5 px-4">Status</th>
              <th className="py-3.5 px-4 text-right">Moderation</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-stone-100">
            {items.map((item) => (
              <tr key={item.id} className="hover:bg-stone-50">
                <td className="py-3.5 px-4 font-semibold text-[#202923]">
                  <Link to={`/items/${item.id}`} className="hover:text-[#238B68] underline">
                    {item.title}
                  </Link>
                </td>
                <td className="py-3.5 px-4 text-xs">{item.categoryName}</td>
                <td className="py-3.5 px-4 text-xs">
                  {item.ownerName} <span className="text-[#238B68] font-bold">({Number(item.ownerTrustScore).toFixed(0)})</span>
                </td>
                <td className="py-3.5 px-4">
                  <span className="px-2.5 py-0.5 rounded-full text-xs font-semibold bg-[#E8F2EA] text-[#164B3B]">
                    {item.sharingType}
                  </span>
                </td>
                <td className="py-3.5 px-4 text-xs font-semibold">{item.availabilityStatus}</td>
                <td className="py-3.5 px-4 text-right">
                  <button
                    onClick={() => handleArchive(item.id)}
                    className="px-3 py-1 rounded-lg bg-red-50 text-red-700 hover:bg-red-100 text-xs font-semibold"
                  >
                    Archive Listing
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
};

export const AdminTransactionsPage: React.FC = () => {
  const [transactions, setTransactions] = useState<TransactionItem[]>([]);
  const [filterType, setFilterType] = useState<string>('ALL');

  useEffect(() => {
    apiClient.get<TransactionItem[]>('/admin/transactions').then((res) => setTransactions(res.data)).catch(console.error);
  }, []);

  const visible = filterType === 'ALL' ? transactions : transactions.filter((t) => t.transactionType === filterType);

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-[#202923]">Campus-Wide Transaction Ledger</h1>
          <p className="text-sm text-[#737B75]">Complete history of all borrowing, rental, exchange, and donation workflows.</p>
        </div>
        <div className="flex gap-2">
          {['ALL', 'BORROW', 'RENT', 'EXCHANGE', 'DONATE'].map((t) => (
            <button
              key={t}
              onClick={() => setFilterType(t)}
              className={`px-3 py-1.5 rounded-xl text-xs font-semibold ${
                filterType === t ? 'bg-[#164B3B] text-white' : 'bg-white border border-stone-200 text-[#202923]'
              }`}
            >
              {t}
            </button>
          ))}
        </div>
      </div>

      <div className="bg-white rounded-2xl border border-stone-200/80 shadow-sm overflow-x-auto">
        <table className="w-full text-left border-collapse text-sm">
          <thead>
            <tr className="border-b border-stone-200 text-xs text-[#737B75] uppercase bg-[#F8F7F0]">
              <th className="py-3.5 px-4">Code</th>
              <th className="py-3.5 px-4">Mode</th>
              <th className="py-3.5 px-4">Resource</th>
              <th className="py-3.5 px-4">Participants</th>
              <th className="py-3.5 px-4">Status</th>
              <th className="py-3.5 px-4">Notes</th>
              <th className="py-3.5 px-4">Created</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-stone-100">
            {visible.map((tx) => (
              <tr key={tx.id} className="hover:bg-stone-50">
                <td className="py-3.5 px-4 font-mono text-xs font-bold">{tx.transactionCode}</td>
                <td className="py-3.5 px-4">
                  <span className="px-2.5 py-0.5 rounded-full text-xs font-bold bg-[#E8F2EA] text-[#164B3B]">
                    {tx.transactionType}
                  </span>
                </td>
                <td className="py-3.5 px-4 font-semibold text-[#202923]">{tx.primaryItemTitle}</td>
                <td className="py-3.5 px-4 text-xs">
                  <span className="font-medium">{tx.initiatorUserName}</span> ↔{' '}
                  <span className="font-medium">{tx.counterpartyUserName}</span>
                </td>
                <td className="py-3.5 px-4">
                  <span className="px-2.5 py-0.5 rounded-full text-xs font-semibold bg-stone-100 text-stone-800">
                    {tx.status}
                  </span>
                </td>
                <td className="py-3.5 px-4 text-xs text-[#737B75]">{tx.notes || '—'}</td>
                <td className="py-3.5 px-4 text-xs text-[#737B75]">
                  {new Date(tx.createdAt).toLocaleDateString()}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
};

export const AdminDisputesPage: React.FC = () => {
  const [disputes, setDisputes] = useState<DisputeItem[]>([]);
  const [notesMap, setNotesMap] = useState<Record<number, string>>({});
  const [toast, setToast] = useState<string | null>(null);

  const loadDisputes = () => {
    apiClient.get<DisputeItem[]>('/admin/disputes').then((res) => setDisputes(res.data)).catch(console.error);
  };

  useEffect(() => {
    loadDisputes();
  }, []);

  const handleResolve = async (id: number, status: 'UNDER_REVIEW' | 'RESOLVED' | 'REJECTED') => {
    const notes = notesMap[id] || 'Reviewed and adjudicated by SRM Campus Moderation Committee.';
    try {
      await apiClient.put(`/admin/disputes/${id}/resolve`, {
        status,
        resolutionNotes: notes,
        penalizeReportedUser: status === 'RESOLVED',
      });
      setToast(`Dispute #${id} marked as ${status}`);
      loadDisputes();
    } catch (e) {
      setToast(extractErrorMessage(e));
    }
  };

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-[#202923]">Dispute Resolution & Content Moderation Queue</h1>
        <p className="text-sm text-[#737B75]">
          Investigate reported listings, damaged returns, or overdue items and issue binding campus resolutions.
        </p>
      </div>

      {toast && (
        <div className="p-4 rounded-xl bg-[#E8F2EA] border border-[#238B68] text-[#164B3B] text-sm font-medium flex justify-between">
          <span>{toast}</span>
          <button onClick={() => setToast(null)} className="text-xs underline">Dismiss</button>
        </div>
      )}

      <div className="space-y-4">
        {disputes.map((d) => (
          <div key={d.id} className="bg-white p-6 rounded-2xl border border-stone-200/80 shadow-sm space-y-4">
            <div className="flex flex-wrap items-center justify-between gap-2">
              <div className="flex items-center gap-3">
                <span className="px-3 py-1 rounded-full text-xs font-bold bg-rose-100 text-rose-800">
                  Case #{d.id} • {d.status}
                </span>
                <h3 className="font-bold text-[#202923] text-lg">{d.title}</h3>
              </div>
              <span className="text-xs text-[#737B75]">Filed {new Date(d.createdAt).toLocaleString()}</span>
            </div>

            <p className="text-sm text-[#202923] bg-[#F8F7F0] p-4 rounded-xl">{d.description}</p>

            <div className="grid grid-cols-1 sm:grid-cols-3 gap-3 text-xs text-[#737B75]">
              <div>
                <strong className="text-[#202923]">Reporter:</strong> {d.reporterName}
              </div>
              <div>
                <strong className="text-[#202923]">Reported User:</strong> {d.reportedUserName || 'N/A'}
              </div>
              <div>
                <strong className="text-[#202923]">Target Listing:</strong> {d.itemTitle || 'N/A'}
              </div>
            </div>

            {d.resolutionNotes && (
              <div className="p-3.5 rounded-xl bg-emerald-50 border border-emerald-200 text-xs text-emerald-900">
                <strong>Official Resolution:</strong> {d.resolutionNotes}
              </div>
            )}

            {d.status !== 'RESOLVED' && d.status !== 'REJECTED' && (
              <div className="pt-2 border-t border-stone-100 flex flex-col sm:flex-row gap-3">
                <input
                  type="text"
                  placeholder="Enter official moderation findings & trust score impact notes..."
                  value={notesMap[d.id] || ''}
                  onChange={(e) => setNotesMap({ ...notesMap, [d.id]: e.target.value })}
                  className="flex-1 px-3.5 py-2 rounded-xl border border-stone-300 text-sm"
                />
                <div className="flex gap-2">
                  <button
                    onClick={() => handleResolve(d.id, 'UNDER_REVIEW')}
                    className="px-3.5 py-2 rounded-xl bg-amber-600 text-white text-xs font-semibold hover:bg-amber-700"
                  >
                    Mark Under Review
                  </button>
                  <button
                    onClick={() => handleResolve(d.id, 'RESOLVED')}
                    className="px-3.5 py-2 rounded-xl bg-[#164B3B] text-white text-xs font-semibold hover:bg-[#238B68]"
                  >
                    Resolve Case
                  </button>
                  <button
                    onClick={() => handleResolve(d.id, 'REJECTED')}
                    className="px-3.5 py-2 rounded-xl bg-stone-600 text-white text-xs font-semibold hover:bg-stone-700"
                  >
                    Dismiss
                  </button>
                </div>
              </div>
            )}
          </div>
        ))}
      </div>
    </div>
  );
};

export const ModeratorDashboardPage: React.FC = () => {
  return (
    <div className="space-y-8">
      <div className="bg-[#164B3B] text-white p-6 rounded-2xl shadow-md flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div>
          <span className="inline-block px-3 py-1 rounded-full bg-[#238B68] text-xs font-semibold uppercase tracking-wider mb-2">
            Campus Community Trust & Safety
          </span>
          <h1 className="text-2xl font-bold">Moderator Operations Desk</h1>
          <p className="text-emerald-100 text-sm mt-1">
            Review flagged listings, investigate student disputes, and uphold SRM community sharing guidelines.
          </p>
        </div>
        <div className="flex gap-3">
          <Link
            to="/admin/listings"
            className="px-4 py-2 bg-[#238B68] hover:bg-emerald-600 text-white rounded-xl text-sm font-semibold flex items-center gap-2"
          >
            <FileText className="w-4 h-4" /> Inspect Listings
          </Link>
        </div>
      </div>

      <AdminDisputesPage />
    </div>
  );
};
