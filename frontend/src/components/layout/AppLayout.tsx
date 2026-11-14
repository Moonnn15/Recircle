import React, { useState } from 'react';
import { Link, NavLink, useNavigate } from 'react-router-dom';
import {
  LayoutDashboard,
  Compass,
  Package,
  ArrowLeftRight,
  HandHeart,
  Clock,
  Receipt,
  Star,
  Bell,
  User,
  Settings,
  ShieldCheck,
  BarChart3,
  AlertTriangle,
  Users,
  FileText,
  LogOut,
  Menu,
  X,
  PlusCircle,
  Recycle,
  Award,
} from 'lucide-react';
import { useAuth } from '../../context/AuthContext';

interface NavEntry {
  to: string;
  label: string;
  icon: React.ReactNode;
  badge?: number;
}

export const AppLayout: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const { user, unreadNotifications, logout } = useAuth();
  const navigate = useNavigate();
  const [mobileOpen, setMobileOpen] = useState(false);

  const studentNav: NavEntry[] = [
    { to: '/dashboard', label: 'Community Dashboard', icon: <LayoutDashboard className="w-4 h-4" /> },
    { to: '/discover', label: 'Explore', icon: <Compass className="w-4 h-4" /> },
    { to: '/listings/create', label: 'List an Item', icon: <PlusCircle className="w-4 h-4" /> },
    { to: '/my-listings', label: 'My Listings', icon: <Package className="w-4 h-4" /> },
    { to: '/borrowing', label: 'Borrow & Lend', icon: <Clock className="w-4 h-4" /> },
    { to: '/exchanges', label: 'Exchanges', icon: <ArrowLeftRight className="w-4 h-4" /> },
    { to: '/rentals', label: 'Rentals', icon: <Receipt className="w-4 h-4" /> },
    { to: '/donations', label: 'Donations', icon: <HandHeart className="w-4 h-4" /> },
    { to: '/transactions', label: 'My Transactions', icon: <FileText className="w-4 h-4" /> },
    { to: '/reviews', label: 'Trust & Reviews', icon: <Star className="w-4 h-4" /> },
    {
      to: '/notifications',
      label: 'Notifications',
      icon: <Bell className="w-4 h-4" />,
      badge: unreadNotifications,
    },
    { to: '/trust-score', label: 'Trust Score Breakdown', icon: <Award className="w-4 h-4" /> },
    { to: '/disputes', label: 'Disputes & Reports', icon: <AlertTriangle className="w-4 h-4" /> },
    { to: '/profile', label: 'My Profile', icon: <User className="w-4 h-4" /> },
    { to: '/settings', label: 'Settings', icon: <Settings className="w-4 h-4" /> },
  ];

  const adminNav: NavEntry[] = [
    { to: '/admin/dashboard', label: 'Admin Overview', icon: <LayoutDashboard className="w-4 h-4" /> },
    { to: '/admin/analytics', label: 'Platform Analytics', icon: <BarChart3 className="w-4 h-4" /> },
    { to: '/admin/users', label: 'User Management', icon: <Users className="w-4 h-4" /> },
    { to: '/admin/listings', label: 'Listing Monitoring', icon: <Package className="w-4 h-4" /> },
    { to: '/admin/transactions', label: 'Transaction Ledger', icon: <Receipt className="w-4 h-4" /> },
    { to: '/admin/disputes', label: 'Dispute Resolution', icon: <AlertTriangle className="w-4 h-4" /> },
    { to: '/moderator/dashboard', label: 'Moderator Desk', icon: <ShieldCheck className="w-4 h-4" /> },
  ];

  const handleLogout = async () => {
    await logout();
    navigate('/login');
  };

  return (
    <div className="min-h-screen flex bg-[#F8F7F0] text-[#202923]">
      {/* Desktop Sidebar */}
      <aside className="hidden lg:flex lg:flex-col lg:w-68 bg-[#164B3B] text-white shrink-0 border-r border-[#238B68]/30">
        <div className="p-5 flex items-center justify-between border-b border-white/10">
          <Link to="/" className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-xl bg-[#238B68] flex items-center justify-center shadow-inner">
              <Recycle className="w-6 h-6 text-[#E8F2EA]" />
            </div>
            <div>
              <span className="font-display text-xl font-bold tracking-tight text-white block leading-none">
                ReCircle
              </span>
              <span className="text-[11px] text-[#A8C3A0] font-medium">
                Community Trust Sharing Network
              </span>
            </div>
          </Link>
        </div>

        {/* User Trust Summary Banner */}
        {user && (
          <div className="mx-4 mt-4 p-3.5 rounded-xl bg-white/8 border border-white/10">
            <div className="flex items-center justify-between">
              <div className="truncate pr-2">
                <p className="text-xs font-semibold text-white truncate">{user.fullName}</p>
                <p className="text-[11px] text-[#A8C3A0] truncate">{user.registrationNumber}</p>
              </div>
              <Link
                to="/trust-score"
                className="px-2.5 py-1 rounded-lg bg-[#238B68] text-white text-xs font-bold shrink-0"
              >
                {Number(user.trustScore).toFixed(0)} TS
              </Link>
            </div>
            <div className="mt-2 flex items-center justify-between text-[11px] text-[#E8F2EA]/80">
              <span>Role: {user.role}</span>
              <span className="inline-flex items-center gap-1 text-[#A8C3A0]">
                {user.campusVerified ? '✓ Verified Status' : 'Unverified'}
              </span>
            </div>
          </div>
        )}

        {/* Navigation Links */}
        <nav className="flex-1 overflow-y-auto px-3 py-4 space-y-1">
          {(user?.role === 'ADMIN' || user?.role === 'MODERATOR') && (
            <div className="mb-4">
              <p className="px-3 mb-1.5 text-[10px] font-bold uppercase tracking-wider text-[#A8C3A0]">
                Administration & Governance
              </p>
              {adminNav.map((item) => (
                <NavLink
                  key={item.to}
                  to={item.to}
                  className={({ isActive }) =>
                    `flex items-center justify-between px-3 py-2.5 rounded-xl text-xs font-medium transition-colors ${
                      isActive
                        ? 'bg-[#238B68] text-white shadow-sm'
                        : 'text-[#E8F2EA]/80 hover:bg-white/10 hover:text-white'
                    }`
                  }
                >
                  <span className="flex items-center gap-2.5">
                    {item.icon}
                    {item.label}
                  </span>
                </NavLink>
              ))}
              <div className="my-3 border-t border-white/10" />
            </div>
          )}

          <p className="px-3 mb-1.5 text-[10px] font-bold uppercase tracking-wider text-[#A8C3A0]">
            Community Sharing Hub
          </p>
          {studentNav.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              className={({ isActive }) =>
                `flex items-center justify-between px-3 py-2.5 rounded-xl text-xs font-medium transition-colors ${
                  isActive
                    ? 'bg-[#238B68] text-white shadow-sm'
                    : 'text-[#E8F2EA]/80 hover:bg-white/10 hover:text-white'
                }`
              }
            >
              <span className="flex items-center gap-2.5">
                {item.icon}
                {item.label}
              </span>
              {item.badge !== undefined && item.badge > 0 && (
                <span className="px-2 py-0.5 text-[10px] font-bold rounded-full bg-[#A8C3A0] text-[#164B3B]">
                  {item.badge}
                </span>
              )}
            </NavLink>
          ))}
        </nav>

        <div className="p-4 border-t border-white/10 space-y-2">
          <Link
            to="/listings/create"
            className="w-full py-2.5 px-4 rounded-xl bg-[#238B68] hover:bg-[#1e7a5a] text-white font-semibold text-xs flex items-center justify-center gap-2 shadow-sm transition-colors"
          >
            <PlusCircle className="w-4 h-4" />
            List an Item
          </Link>
          <button
            onClick={handleLogout}
            className="w-full py-2 px-4 rounded-xl text-[#E8F2EA]/80 hover:bg-white/10 hover:text-white text-xs font-medium flex items-center justify-center gap-2 transition-colors cursor-pointer"
          >
            <LogOut className="w-4 h-4" />
            Sign Out
          </button>
        </div>
      </aside>

      {/* Main Content Area */}
      <div className="flex-1 flex flex-col min-w-0">
        {/* Top Header */}
        <header className="sticky top-0 z-30 bg-[#F8F7F0]/95 backdrop-blur border-b border-[#A8C3A0]/40 px-4 lg:px-8 h-16 flex items-center justify-between">
          <div className="flex items-center gap-3">
            <button
              onClick={() => setMobileOpen(!mobileOpen)}
              className="lg:hidden p-2 rounded-lg text-[#164B3B] hover:bg-[#E8F2EA]"
              aria-label="Toggle Menu"
            >
              {mobileOpen ? <X className="w-5 h-5" /> : <Menu className="w-5 h-5" />}
            </button>
            <div>
              <span className="text-xs font-semibold uppercase tracking-wider text-[#238B68]">
                Share More. Waste Less. Build Trust.
              </span>
            </div>
          </div>

          <div className="flex items-center gap-3">
            <Link
              to="/discover"
              className="hidden sm:inline-flex items-center gap-1.5 px-3.5 py-1.5 rounded-xl bg-[#E8F2EA] text-[#164B3B] text-xs font-semibold hover:bg-[#A8C3A0]/40 transition-colors"
            >
              <Compass className="w-3.5 h-3.5" />
              Browse Marketplace
            </Link>
            <Link
              to="/notifications"
              className="relative p-2 rounded-xl bg-white border border-[#A8C3A0]/40 text-[#164B3B] hover:bg-[#E8F2EA]"
              title="Notifications"
            >
              <Bell className="w-4 h-4" />
              {unreadNotifications > 0 && (
                <span className="absolute -top-1 -right-1 w-4 h-4 rounded-full bg-[#238B68] text-white text-[10px] font-bold flex items-center justify-center">
                  {unreadNotifications}
                </span>
              )}
            </Link>
            {user && (
              <Link
                to="/profile"
                className="flex items-center gap-2 pl-2 pr-3 py-1 rounded-xl bg-white border border-[#A8C3A0]/40 hover:border-[#238B68]"
              >
                <div className="w-7 h-7 rounded-lg bg-[#164B3B] text-white text-xs font-bold flex items-center justify-center">
                  {user.fullName.charAt(0)}
                </div>
                <div className="hidden md:block text-left">
                  <p className="text-xs font-semibold text-[#202923] leading-tight">
                    {user.fullName.split(' ')[0]}
                  </p>
                  <p className="text-[10px] text-[#238B68] font-medium">
                    Trust: {Number(user.trustScore).toFixed(1)}
                  </p>
                </div>
              </Link>
            )}
          </div>
        </header>

        {/* Mobile Drawer */}
        {mobileOpen && (
          <div className="lg:hidden bg-[#164B3B] text-white px-4 py-4 space-y-1 border-b border-white/10">
            {[...(user?.role === 'ADMIN' || user?.role === 'MODERATOR' ? adminNav : []), ...studentNav].map(
              (item) => (
                <NavLink
                  key={item.to}
                  to={item.to}
                  onClick={() => setMobileOpen(false)}
                  className="flex items-center gap-2.5 px-3 py-2 rounded-lg text-xs font-medium text-[#E8F2EA] hover:bg-white/10"
                >
                  {item.icon}
                  {item.label}
                </NavLink>
              )
            )}
          </div>
        )}

        {/* Page Body */}
        <main className="flex-1 p-4 lg:p-8 max-w-7xl w-full mx-auto">{children}</main>
      </div>
    </div>
  );
};
