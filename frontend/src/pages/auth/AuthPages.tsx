import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import {
  Recycle,
  ShieldCheck,
  ArrowRight,
  BookOpen,
  Cpu,
  Bike,
  Award,
  CheckCircle2,
  Lock,
  Mail,
  UserCheck,
  Sparkles,
} from 'lucide-react';
import { apiClient, extractErrorMessage } from '../../api/client';
import { useAuth } from '../../context/AuthContext';
import { UserSummary } from '../../types';

// ============================================================================
// 1. PUBLIC LANDING PAGE
// ============================================================================
export const LandingPage: React.FC = () => {
  const { user } = useAuth();

  return (
    <div className="min-h-screen bg-[#F8F7F0] text-[#202923] flex flex-col">
      {/* Top Navbar */}
      <header className="sticky top-0 z-30 bg-[#F8F7F0]/90 backdrop-blur border-b border-[#A8C3A0]/40">
        <div className="max-w-7xl mx-auto px-4 sm:px-8 h-20 flex items-center justify-between">
          <div className="flex items-center gap-3">
            <div className="w-11 h-11 rounded-2xl bg-[#164B3B] flex items-center justify-center shadow-sm">
              <Recycle className="w-6 h-6 text-[#A8C3A0]" />
            </div>
            <div>
              <span className="font-display text-2xl font-bold text-[#164B3B] tracking-tight block leading-none">
                ReCircle
              </span>
              <span className="text-xs font-medium text-[#737B75]">
                Share More. Waste Less. Build Trust.
              </span>
            </div>
          </div>

          <div className="flex items-center gap-3">
            <Link
              to="/discover"
              className="hidden sm:inline-flex px-4 py-2.5 rounded-xl text-sm font-semibold text-[#164B3B] hover:bg-[#E8F2EA] transition-colors"
            >
              Explore Campus Items
            </Link>
            {user ? (
              <Link
                to="/dashboard"
                className="px-5 py-2.5 rounded-xl bg-[#164B3B] hover:bg-[#238B68] text-white text-sm font-semibold shadow-xs transition-colors"
              >
                Go to Dashboard
              </Link>
            ) : (
              <>
                <Link
                  to="/login"
                  className="px-4 py-2.5 rounded-xl text-sm font-semibold text-[#164B3B] hover:bg-[#E8F2EA] transition-colors"
                >
                  Sign In
                </Link>
                <Link
                  to="/register"
                  className="px-5 py-2.5 rounded-xl bg-[#164B3B] hover:bg-[#238B68] text-white text-sm font-semibold shadow-xs transition-colors"
                >
                  Join Campus Community
                </Link>
              </>
            )}
          </div>
        </div>
      </header>

      {/* Hero Section */}
      <section className="relative overflow-hidden py-16 lg:py-24 px-4 sm:px-8 bg-gradient-to-b from-[#E8F2EA]/70 to-[#F8F7F0]">
        <div className="max-w-7xl mx-auto grid lg:grid-cols-12 gap-12 items-center">
          <div className="lg:col-span-7 space-y-6">
            <div className="inline-flex items-center gap-2 px-3.5 py-1.5 rounded-full bg-[#E8F2EA] border border-[#A8C3A0] text-[#164B3B] text-xs font-bold">
              <Sparkles className="w-3.5 h-3.5 text-[#238B68]" />
              SRM Institute of Science & Technology • Circular Campus Initiative
            </div>
            <h1 className="font-display text-4xl sm:text-6xl font-bold text-[#164B3B] leading-[1.08]">
              Share Campus Resources with{' '}
              <span className="text-[#238B68] underline decoration-[#A8C3A0] decoration-4 underline-offset-8">
                Verified Trust.
              </span>
            </h1>
            <p className="text-base sm:text-lg text-[#737B75] max-w-2xl leading-relaxed">
              Stop buying brand-new textbooks, calculators, Arduino kits, lab coats, and bicycles for a single
              semester. ReCircle connects verified students to borrow, lend, rent, exchange, and donate resources
              backed by a dynamic 0–100 reputation score.
            </p>
            <div className="flex flex-wrap items-center gap-4 pt-2">
              <Link
                to="/register"
                className="px-6 py-3.5 rounded-2xl bg-[#164B3B] hover:bg-[#238B68] text-white font-semibold text-sm inline-flex items-center gap-2 shadow-md transition-all"
              >
                Register with Campus Email
                <ArrowRight className="w-4 h-4" />
              </Link>
              <Link
                to="/discover"
                className="px-6 py-3.5 rounded-2xl bg-white hover:bg-[#E8F2EA] text-[#164B3B] border border-[#A8C3A0] font-semibold text-sm transition-colors"
              >
                Browse Live Listings
              </Link>
            </div>

            <div className="pt-6 grid grid-cols-3 gap-4 border-t border-[#A8C3A0]/40 max-w-xl">
              <div>
                <p className="font-display text-2xl font-bold text-[#164B3B]">100%</p>
                <p className="text-xs text-[#737B75]">Campus Email Verified</p>
              </div>
              <div>
                <p className="font-display text-2xl font-bold text-[#238B68]">4 Modes</p>
                <p className="text-xs text-[#737B75]">Borrow • Rent • Swap • Donate</p>
              </div>
              <div>
                <p className="font-display text-2xl font-bold text-[#164B3B]">SDG 11 & 12</p>
                <p className="text-xs text-[#737B75]">UN Sustainability Aligned</p>
              </div>
            </div>
          </div>

          {/* Interactive Trust Score & Resource Preview Card */}
          <div className="lg:col-span-5">
            <div className="bg-[#164B3B] text-white rounded-3xl p-6 sm:p-8 shadow-xl border border-[#238B68]/40 space-y-6">
              <div className="flex items-center justify-between">
                <div>
                  <span className="text-xs uppercase tracking-wider text-[#A8C3A0] font-bold">
                    Differentiating Engine
                  </span>
                  <h3 className="font-display text-xl font-bold mt-0.5">
                    Dynamic Weighted Trust Score
                  </h3>
                </div>
                <div className="w-14 h-14 rounded-2xl bg-[#238B68] flex flex-col items-center justify-center">
                  <span className="font-display text-xl font-bold leading-none">92.8</span>
                  <span className="text-[10px] text-[#E8F2EA]">EXCELLENT</span>
                </div>
              </div>

              <div className="space-y-3 text-xs">
                <div className="p-3 rounded-xl bg-white/8 flex items-center justify-between">
                  <span>Verified Campus Account (× 0.15)</span>
                  <span className="font-bold text-[#A8C3A0]">100 / 100</span>
                </div>
                <div className="p-3 rounded-xl bg-white/8 flex items-center justify-between">
                  <span>Completed Transactions (× 0.35)</span>
                  <span className="font-bold text-[#A8C3A0]">85 / 100</span>
                </div>
                <div className="p-3 rounded-xl bg-white/8 flex items-center justify-between">
                  <span>Peer 5-Star Reviews (× 0.30)</span>
                  <span className="font-bold text-[#A8C3A0]">96 / 100</span>
                </div>
                <div className="p-3 rounded-xl bg-white/8 flex items-center justify-between">
                  <span>On-Time Return Record (× 0.20)</span>
                  <span className="font-bold text-[#A8C3A0]">96.5 / 100</span>
                </div>
              </div>

              <div className="pt-2 border-t border-white/15 flex items-center justify-between text-xs text-[#E8F2EA]/90">
                <span>Zero unverified score penalties</span>
                <span className="font-semibold text-[#A8C3A0]">Full Audit Trail →</span>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* 4 Sharing Modes Section */}
      <section className="py-16 px-4 sm:px-8 max-w-7xl mx-auto">
        <div className="text-center max-w-2xl mx-auto mb-12">
          <h2 className="font-display text-3xl font-bold text-[#164B3B]">
            Four Structured Ways to Share Across Campus
          </h2>
          <p className="mt-2 text-sm text-[#737B75]">
            Every transaction is protected by conflict-free reservation checks, handover confirmation, and mutual
            accountability.
          </p>
        </div>

        <div className="grid sm:grid-cols-2 lg:grid-cols-4 gap-6">
          {[
            {
              icon: <BookOpen className="w-6 h-6 text-[#164B3B]" />,
              title: '1. Borrow & Lend',
              desc: 'Lend textbooks, calculators, and sports gear for up to 14 days with automated return reminders.',
            },
            {
              icon: <Bike className="w-6 h-6 text-[#164B3B]" />,
              title: '2. Campus Rentals',
              desc: 'Rent bicycles, guitars, or DSLR tripods at affordable daily student rates with transparent calculation.',
            },
            {
              icon: <Cpu className="w-6 h-6 text-[#164B3B]" />,
              title: '3. Peer Exchanges',
              desc: 'Propose direct item-for-item swaps (e.g., wireless mouse for rechargeable study lamp) with dual handover.',
            },
            {
              icon: <Award className="w-6 h-6 text-[#164B3B]" />,
              title: '4. Senior Donations',
              desc: 'Graduating seniors donate mini-drafters, lab coats, and GATE books to incoming first-year students.',
            },
          ].map((card) => (
            <div
              key={card.title}
              className="bg-white rounded-2xl p-6 border border-[#A8C3A0]/50 shadow-xs hover:border-[#238B68] transition-all"
            >
              <div className="w-12 h-12 rounded-2xl bg-[#E8F2EA] flex items-center justify-center mb-4">
                {card.icon}
              </div>
              <h3 className="font-display font-bold text-lg text-[#202923]">{card.title}</h3>
              <p className="mt-2 text-xs text-[#737B75] leading-relaxed">{card.desc}</p>
            </div>
          ))}
        </div>
      </section>

      <footer className="mt-auto bg-[#164B3B] text-[#E8F2EA] py-8 px-4 sm:px-8 border-t border-[#238B68]">
        <div className="max-w-7xl mx-auto flex flex-col sm:flex-row items-center justify-between gap-4 text-xs">
          <p className="font-medium">
            © 2026 ReCircle – Trust-Based Campus Community Resource Sharing System (SRM IST Kattankulathur).
          </p>
          <p className="text-[#A8C3A0]">Aligned with UN SDG 11 & SDG 12 • Share More. Waste Less. Build Trust.</p>
        </div>
      </footer>
    </div>
  );
};

// ============================================================================
// 2. LOGIN PAGE
// ============================================================================
export const LoginPage: React.FC = () => {
  const { setSession, showToast } = useAuth();
  const navigate = useNavigate();
  const [email, setEmail] = useState('aarav.krishnan@srmist.edu.in');
  const [password, setPassword] = useState('ReCircle@2026');
  const [error, setError] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setSubmitting(true);
    try {
      const res = await apiClient.post<{
        accessToken: string;
        refreshToken: string;
        user: UserSummary;
      }>('/auth/login', { email, password });
      setSession(res.data.accessToken, res.data.refreshToken, res.data.user);
      showToast(`Welcome back, ${res.data.user.fullName}!`, 'success');
      if (res.data.user.role === 'ADMIN') {
        navigate('/admin/dashboard');
      } else if (res.data.user.role === 'MODERATOR') {
        navigate('/moderator/dashboard');
      } else {
        navigate('/dashboard');
      }
    } catch (err) {
      setError(extractErrorMessage(err));
    } finally {
      setSubmitting(false);
    }
  };

  const fillDemoAccount = (demoEmail: string) => {
    setEmail(demoEmail);
    setPassword('ReCircle@2026');
  };

  return (
    <div className="min-h-screen bg-[#F8F7F0] flex flex-col justify-center py-12 px-4 sm:px-6 lg:px-8">
      <div className="sm:mx-auto sm:w-full sm:max-w-md text-center">
        <Link to="/" className="inline-flex items-center gap-2.5">
          <div className="w-11 h-11 rounded-2xl bg-[#164B3B] flex items-center justify-center">
            <Recycle className="w-6 h-6 text-[#A8C3A0]" />
          </div>
          <span className="font-display text-2xl font-bold text-[#164B3B]">ReCircle</span>
        </Link>
        <h2 className="mt-4 font-display text-2xl font-bold text-[#202923]">
          Sign in to your campus account
        </h2>
        <p className="mt-1 text-xs text-[#737B75]">
          Access verified student listings, transactions, and your Trust Score
        </p>
      </div>

      <div className="mt-6 sm:mx-auto sm:w-full sm:max-w-md">
        <div className="bg-white py-8 px-6 shadow-md rounded-3xl border border-[#A8C3A0]/50 space-y-6">
          {/* Quick Demo Role Switcher */}
          <div className="p-3.5 rounded-2xl bg-[#E8F2EA] border border-[#A8C3A0]">
            <p className="text-[11px] font-bold uppercase tracking-wider text-[#164B3B] mb-2">
              Quick Demo Credentials (Password: ReCircle@2026)
            </p>
            <div className="grid grid-cols-3 gap-2">
              <button
                type="button"
                onClick={() => fillDemoAccount('aarav.krishnan@srmist.edu.in')}
                className="px-2.5 py-1.5 rounded-xl bg-white hover:bg-[#164B3B] hover:text-white text-[#164B3B] text-xs font-semibold border border-[#A8C3A0] transition-colors cursor-pointer"
              >
                Student (Aarav)
              </button>
              <button
                type="button"
                onClick={() => fillDemoAccount('moderator@srmist.edu.in')}
                className="px-2.5 py-1.5 rounded-xl bg-white hover:bg-[#164B3B] hover:text-white text-[#164B3B] text-xs font-semibold border border-[#A8C3A0] transition-colors cursor-pointer"
              >
                Moderator
              </button>
              <button
                type="button"
                onClick={() => fillDemoAccount('admin@srmist.edu.in')}
                className="px-2.5 py-1.5 rounded-xl bg-white hover:bg-[#164B3B] hover:text-white text-[#164B3B] text-xs font-semibold border border-[#A8C3A0] transition-colors cursor-pointer"
              >
                Administrator
              </button>
            </div>
          </div>

          {error && (
            <div className="p-3 rounded-xl bg-red-50 border border-red-300 text-xs text-red-800 font-medium">
              {error}
            </div>
          )}

          <form onSubmit={handleSubmit} className="space-y-4">
            <div>
              <label className="block text-xs font-semibold text-[#202923] mb-1">
                Campus Email Address
              </label>
              <div className="relative">
                <Mail className="w-4 h-4 text-[#737B75] absolute left-3.5 top-3" />
                <input
                  type="email"
                  required
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  className="w-full pl-10 pr-4 py-2.5 rounded-xl border border-[#A8C3A0] text-sm focus:outline-none focus:border-[#164B3B]"
                  placeholder="student@srmist.edu.in"
                />
              </div>
            </div>

            <div>
              <div className="flex items-center justify-between mb-1">
                <label className="block text-xs font-semibold text-[#202923]">Password</label>
                <Link to="/forgot-password" className="text-xs font-semibold text-[#238B68] hover:underline">
                  Forgot password?
                </Link>
              </div>
              <div className="relative">
                <Lock className="w-4 h-4 text-[#737B75] absolute left-3.5 top-3" />
                <input
                  type="password"
                  required
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  className="w-full pl-10 pr-4 py-2.5 rounded-xl border border-[#A8C3A0] text-sm focus:outline-none focus:border-[#164B3B]"
                />
              </div>
            </div>

            <button
              type="submit"
              disabled={submitting}
              className="w-full py-3 px-4 rounded-xl bg-[#164B3B] hover:bg-[#238B68] text-white font-semibold text-sm transition-colors cursor-pointer disabled:opacity-50"
            >
              {submitting ? 'Authenticating...' : 'Sign In to ReCircle'}
            </button>
          </form>

          <p className="text-center text-xs text-[#737B75]">
            New to ReCircle?{' '}
            <Link to="/register" className="font-semibold text-[#164B3B] hover:underline">
              Create your verified student account
            </Link>
          </p>
        </div>
      </div>
    </div>
  );
};

// ============================================================================
// 3. REGISTRATION PAGE
// ============================================================================
export const RegisterPage: React.FC = () => {
  const { setSession, showToast } = useAuth();
  const navigate = useNavigate();

  const [form, setForm] = useState({
    fullName: '',
    email: '',
    password: '',
    confirmPassword: '',
    registrationNumber: '',
    department: 'Computer Science and Engineering',
    yearOfStudy: 3,
    contactNumber: '',
    hostelOrBlock: '',
    preferredPickupSpot: 'Tech Park Ground Floor Lobby',
  });
  const [error, setError] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setSubmitting(true);
    try {
      const res = await apiClient.post<{
        accessToken: string;
        refreshToken: string;
        devOtpPreview?: string;
        user: UserSummary;
      }>('/auth/register', form);
      setSession(res.data.accessToken, res.data.refreshToken, res.data.user);
      showToast(
        `Registered! Dev OTP: ${res.data.devOtpPreview || '123456'}. Verify in Profile.`,
        'success'
      );
      navigate('/dashboard');
    } catch (err) {
      setError(extractErrorMessage(err));
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="min-h-screen bg-[#F8F7F0] py-10 px-4 sm:px-6 lg:px-8 flex flex-col justify-center">
      <div className="sm:mx-auto sm:w-full sm:max-w-2xl text-center">
        <Link to="/" className="inline-flex items-center gap-2.5">
          <div className="w-10 h-10 rounded-xl bg-[#164B3B] flex items-center justify-center">
            <Recycle className="w-5 h-5 text-[#A8C3A0]" />
          </div>
          <span className="font-display text-2xl font-bold text-[#164B3B]">ReCircle</span>
        </Link>
        <h2 className="mt-3 font-display text-2xl font-bold text-[#202923]">
          Create Your Verified SRM Student Account
        </h2>
        <p className="mt-1 text-xs text-[#737B75]">
          Requires an authorized campus email domain (@srmist.edu.in, @campus.edu, @edu.in)
        </p>
      </div>

      <div className="mt-6 sm:mx-auto sm:w-full sm:max-w-2xl">
        <div className="bg-white py-8 px-6 sm:px-8 shadow-md rounded-3xl border border-[#A8C3A0]/50">
          {error && (
            <div className="mb-4 p-3 rounded-xl bg-red-50 border border-red-300 text-xs text-red-800 font-medium">
              {error}
            </div>
          )}

          <form onSubmit={handleSubmit} className="grid sm:grid-cols-2 gap-4">
            <div>
              <label className="block text-xs font-semibold text-[#202923] mb-1">Full Name *</label>
              <input
                type="text"
                required
                value={form.fullName}
                onChange={(e) => setForm({ ...form, fullName: e.target.value })}
                placeholder="e.g., Siddharth Menon"
                className="w-full px-3.5 py-2.5 rounded-xl border border-[#A8C3A0] text-sm"
              />
            </div>

            <div>
              <label className="block text-xs font-semibold text-[#202923] mb-1">
                Campus Email (@srmist.edu.in) *
              </label>
              <input
                type="email"
                required
                value={form.email}
                onChange={(e) => setForm({ ...form, email: e.target.value })}
                placeholder="sm1234@srmist.edu.in"
                className="w-full px-3.5 py-2.5 rounded-xl border border-[#A8C3A0] text-sm"
              />
            </div>

            <div>
              <label className="block text-xs font-semibold text-[#202923] mb-1">
                Student Registration Number *
              </label>
              <input
                type="text"
                required
                value={form.registrationNumber}
                onChange={(e) => setForm({ ...form, registrationNumber: e.target.value })}
                placeholder="RA2211003010512"
                className="w-full px-3.5 py-2.5 rounded-xl border border-[#A8C3A0] text-sm uppercase"
              />
            </div>

            <div>
              <label className="block text-xs font-semibold text-[#202923] mb-1">Department *</label>
              <select
                value={form.department}
                onChange={(e) => setForm({ ...form, department: e.target.value })}
                className="w-full px-3.5 py-2.5 rounded-xl border border-[#A8C3A0] text-sm bg-white"
              >
                <option>Computer Science and Engineering</option>
                <option>Electronics and Communication Engineering</option>
                <option>Mechanical Engineering</option>
                <option>Electrical and Electronics Engineering</option>
                <option>Biotechnology</option>
                <option>Civil Engineering</option>
                <option>Aerospace Engineering</option>
              </select>
            </div>

            <div>
              <label className="block text-xs font-semibold text-[#202923] mb-1">Year of Study *</label>
              <select
                value={form.yearOfStudy}
                onChange={(e) => setForm({ ...form, yearOfStudy: Number(e.target.value) })}
                className="w-full px-3.5 py-2.5 rounded-xl border border-[#A8C3A0] text-sm bg-white"
              >
                <option value={1}>1st Year</option>
                <option value={2}>2nd Year</option>
                <option value={3}>3rd Year</option>
                <option value={4}>4th Year</option>
                <option value={5}>5th Year / Integrated</option>
              </select>
            </div>

            <div>
              <label className="block text-xs font-semibold text-[#202923] mb-1">Contact Number *</label>
              <input
                type="text"
                required
                value={form.contactNumber}
                onChange={(e) => setForm({ ...form, contactNumber: e.target.value })}
                placeholder="+91 98400 12345"
                className="w-full px-3.5 py-2.5 rounded-xl border border-[#A8C3A0] text-sm"
              />
            </div>

            <div>
              <label className="block text-xs font-semibold text-[#202923] mb-1">
                Hostel / Campus Block
              </label>
              <input
                type="text"
                value={form.hostelOrBlock}
                onChange={(e) => setForm({ ...form, hostelOrBlock: e.target.value })}
                placeholder="Nelson Mandela Hostel / Tech Park"
                className="w-full px-3.5 py-2.5 rounded-xl border border-[#A8C3A0] text-sm"
              />
            </div>

            <div>
              <label className="block text-xs font-semibold text-[#202923] mb-1">
                Preferred Campus Exchange Spot
              </label>
              <input
                type="text"
                value={form.preferredPickupSpot}
                onChange={(e) => setForm({ ...form, preferredPickupSpot: e.target.value })}
                placeholder="Central Library Entrance"
                className="w-full px-3.5 py-2.5 rounded-xl border border-[#A8C3A0] text-sm"
              />
            </div>

            <div>
              <label className="block text-xs font-semibold text-[#202923] mb-1">
                Password (min 8 chars, upper, lower, digit) *
              </label>
              <input
                type="password"
                required
                value={form.password}
                onChange={(e) => setForm({ ...form, password: e.target.value })}
                className="w-full px-3.5 py-2.5 rounded-xl border border-[#A8C3A0] text-sm"
              />
            </div>

            <div>
              <label className="block text-xs font-semibold text-[#202923] mb-1">
                Confirm Password *
              </label>
              <input
                type="password"
                required
                value={form.confirmPassword}
                onChange={(e) => setForm({ ...form, confirmPassword: e.target.value })}
                className="w-full px-3.5 py-2.5 rounded-xl border border-[#A8C3A0] text-sm"
              />
            </div>

            <div className="sm:col-span-2 pt-2">
              <button
                type="submit"
                disabled={submitting}
                className="w-full py-3 px-4 rounded-xl bg-[#164B3B] hover:bg-[#238B68] text-white font-semibold text-sm transition-colors cursor-pointer"
              >
                {submitting ? 'Creating Verified Profile...' : 'Register Campus Account'}
              </button>
            </div>
          </form>

          <p className="mt-4 text-center text-xs text-[#737B75]">
            Already registered?{' '}
            <Link to="/login" className="font-semibold text-[#164B3B] hover:underline">
              Sign in here
            </Link>
          </p>
        </div>
      </div>
    </div>
  );
};

// ============================================================================
// 4. FORGOT & RESET PASSWORD PAGE
// ============================================================================
export const ForgotPasswordPage: React.FC = () => {
  const { showToast } = useAuth();
  const navigate = useNavigate();
  const [email, setEmail] = useState('');
  const [resetToken, setResetToken] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [step, setStep] = useState<'request' | 'reset'>('request');
  const [error, setError] = useState('');

  const handleRequestToken = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    try {
      const res = await apiClient.post<{ message: string; devResetToken: string }>(
        '/auth/forgot-password',
        { email }
      );
      setResetToken(res.data.devResetToken || '');
      setStep('reset');
      showToast('Password reset token generated for local dev testing!', 'info');
    } catch (err) {
      setError(extractErrorMessage(err));
    }
  };

  const handleResetPassword = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    try {
      await apiClient.post('/auth/reset-password', {
        resetToken,
        newPassword,
        confirmPassword,
      });
      showToast('Password updated! Please sign in.', 'success');
      navigate('/login');
    } catch (err) {
      setError(extractErrorMessage(err));
    }
  };

  return (
    <div className="min-h-screen bg-[#F8F7F0] flex flex-col justify-center py-12 px-4 sm:px-6 lg:px-8">
      <div className="sm:mx-auto sm:w-full sm:max-w-md">
        <div className="bg-white py-8 px-6 shadow-md rounded-3xl border border-[#A8C3A0]/50 space-y-5">
          <div className="text-center">
            <h2 className="font-display text-2xl font-bold text-[#164B3B]">
              {step === 'request' ? 'Recover Campus Password' : 'Set New Password'}
            </h2>
            <p className="mt-1 text-xs text-[#737B75]">
              {step === 'request'
                ? 'Enter your registered campus email to generate a secure recovery token'
                : 'Enter the recovery token and choose a strong new password'}
            </p>
          </div>

          {error && (
            <div className="p-3 rounded-xl bg-red-50 border border-red-300 text-xs text-red-800">
              {error}
            </div>
          )}

          {step === 'request' ? (
            <form onSubmit={handleRequestToken} className="space-y-4">
              <div>
                <label className="block text-xs font-semibold mb-1">Campus Email</label>
                <input
                  type="email"
                  required
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  placeholder="aarav.krishnan@srmist.edu.in"
                  className="w-full px-3.5 py-2.5 rounded-xl border border-[#A8C3A0] text-sm"
                />
              </div>
              <button
                type="submit"
                className="w-full py-2.5 rounded-xl bg-[#164B3B] text-white font-semibold text-sm cursor-pointer"
              >
                Send Reset Token
              </button>
            </form>
          ) : (
            <form onSubmit={handleResetPassword} className="space-y-4">
              <div>
                <label className="block text-xs font-semibold mb-1">Reset Token</label>
                <input
                  type="text"
                  required
                  value={resetToken}
                  onChange={(e) => setResetToken(e.target.value)}
                  className="w-full px-3.5 py-2.5 rounded-xl border border-[#A8C3A0] text-sm font-mono"
                />
              </div>
              <div>
                <label className="block text-xs font-semibold mb-1">New Password</label>
                <input
                  type="password"
                  required
                  value={newPassword}
                  onChange={(e) => setNewPassword(e.target.value)}
                  className="w-full px-3.5 py-2.5 rounded-xl border border-[#A8C3A0] text-sm"
                />
              </div>
              <div>
                <label className="block text-xs font-semibold mb-1">Confirm New Password</label>
                <input
                  type="password"
                  required
                  value={confirmPassword}
                  onChange={(e) => setConfirmPassword(e.target.value)}
                  className="w-full px-3.5 py-2.5 rounded-xl border border-[#A8C3A0] text-sm"
                />
              </div>
              <button
                type="submit"
                className="w-full py-2.5 rounded-xl bg-[#238B68] text-white font-semibold text-sm cursor-pointer"
              >
                Confirm Password Reset
              </button>
            </form>
          )}

          <div className="text-center">
            <Link to="/login" className="text-xs font-semibold text-[#164B3B] hover:underline">
              ← Back to Login
            </Link>
          </div>
        </div>
      </div>
    </div>
  );
};
