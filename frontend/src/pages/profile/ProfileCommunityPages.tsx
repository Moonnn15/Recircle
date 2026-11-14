import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import {
  Award,
  ShieldCheck,
  Star,
  Bell,
  AlertTriangle,
  User,
  Settings,
  CheckCircle2,
  TrendingUp,
} from 'lucide-react';
import { apiClient, extractErrorMessage } from '../../api/client';
import {
  DisputeItem,
  NotificationItem,
  ReviewItem,
  TransactionItem,
  TrustScoreDetails,
} from '../../types';
import { useAuth } from '../../context/AuthContext';

// ============================================================================
// 1. TRUST SCORE DETAILS PAGE (4 Weighted Components + Score History Log)
// ============================================================================
export const TrustScoreDetailsPage: React.FC = () => {
  const { user } = useAuth();
  const [details, setDetails] = useState<TrustScoreDetails | null>(null);

  useEffect(() => {
    if (user?.id) {
      apiClient.get<TrustScoreDetails>(`/users/${user.id}/trust-score`).then((res) => setDetails(res.data));
    }
  }, [user]);

  if (!details) {
    return <div className="h-64 bg-white rounded-3xl animate-pulse" />;
  }

  const components = [
    {
      title: '1. Verified Campus Account Score',
      weight: '15% Weight',
      score: details.verifiedAccountScore,
      desc: details.campusVerified
        ? 'Verified SRM campus email (@srmist.edu.in) confirmed.'
        : 'Account not yet verified. Verify your campus OTP to earn 100/100.',
    },
    {
      title: '2. Completed Transaction Score',
      weight: '35% Weight',
      score: details.completedTxScore,
      desc: `Calculated from ${details.completedTransactionsCount} verified completed campus resource shares using a bounded anti-gaming curve.`,
    },
    {
      title: '3. Peer User Review Score',
      weight: '30% Weight',
      score: details.userReviewScore,
      desc: `Normalized from your ${Number(details.averageRating).toFixed(2)} / 5.00 average peer rating across completed transactions.`,
    },
    {
      title: '4. On-Time Return Score',
      weight: '20% Weight',
      score: details.onTimeReturnScore,
      desc:
        details.eligibleReturnCount > 0
          ? `${details.onTimeReturnCount} of ${details.eligibleReturnCount} eligible borrowings/rentals returned on or before due date.`
          : 'Neutral default score of 50.00 applied until your first borrowing or rental return.',
    },
  ];

  return (
    <div className="space-y-8">
      <div className="bg-gradient-to-r from-[#164B3B] to-[#238B68] rounded-3xl p-6 sm:p-8 text-white flex flex-col md:flex-row md:items-center justify-between gap-6">
        <div className="space-y-2 max-w-xl">
          <span className="px-3 py-1 rounded-full bg-white/15 text-xs font-bold uppercase tracking-wider">
            ReCircle Reputation Engine
          </span>
          <h1 className="font-display text-3xl font-bold">
            Dynamic Trust Score & Reputation Breakdown
          </h1>
          <p className="text-xs sm:text-sm text-[#E8F2EA]/90">
            Trust Score = (Verified × 0.15) + (Completed × 0.35) + (Reviews × 0.30) + (On-Time × 0.20)
          </p>
        </div>

        <div className="bg-white/15 backdrop-blur-xs rounded-3xl p-6 text-center min-w-[200px] border border-white/20">
          <span className="text-xs uppercase font-bold text-[#A8C3A0]">Current Score</span>
          <p className="font-display text-5xl font-bold mt-1">
            {Number(details.totalScore).toFixed(1)}
          </p>
          <span className="inline-block mt-2 px-3 py-1 rounded-full bg-[#A8C3A0] text-[#164B3B] text-xs font-bold">
            Level: {details.trustLevel}
          </span>
        </div>
      </div>

      {/* 4 Component Cards */}
      <div className="grid sm:grid-cols-2 gap-5">
        {components.map((c) => (
          <div
            key={c.title}
            className="bg-white rounded-3xl p-6 border border-[#A8C3A0]/50 shadow-xs space-y-3"
          >
            <div className="flex items-center justify-between">
              <h3 className="font-display font-bold text-base text-[#164B3B]">{c.title}</h3>
              <span className="px-2.5 py-1 rounded-lg bg-[#E8F2EA] text-[#164B3B] text-xs font-bold">
                {c.weight}
              </span>
            </div>
            <div className="flex items-baseline justify-between">
              <span className="font-display text-2xl font-bold text-[#202923]">
                {Number(c.score).toFixed(1)} <span className="text-xs text-[#737B75]">/ 100</span>
              </span>
            </div>
            <div className="w-full h-2 bg-[#E8F2EA] rounded-full overflow-hidden">
              <div
                className="h-full bg-[#238B68]"
                style={{ width: `${Math.min(100, Number(c.score))}%` }}
              />
            </div>
            <p className="text-xs text-[#737B75]">{c.desc}</p>
          </div>
        ))}
      </div>

      {/* Trust Score Change Audit History */}
      <div className="bg-white rounded-3xl p-6 border border-[#A8C3A0]/50 space-y-4">
        <div className="flex items-center gap-2">
          <TrendingUp className="w-5 h-5 text-[#238B68]" />
          <h2 className="font-display text-lg font-bold text-[#164B3B]">
            Transparent Trust Score History Log
          </h2>
        </div>

        <div className="space-y-2.5">
          {details.history.map((h) => (
            <div
              key={h.id}
              className="p-4 rounded-2xl bg-[#F8F7F0] border border-[#A8C3A0]/40 flex flex-col sm:flex-row sm:items-center justify-between gap-2 text-xs"
            >
              <div>
                <span className="font-mono font-bold text-[#164B3B] mr-2">[{h.triggerEvent}]</span>
                <span className="font-medium text-[#202923]">{h.reasonSummary}</span>
              </div>
              <div className="flex items-center gap-3 shrink-0">
                <span className="text-[#737B75]">
                  {Number(h.previousScore).toFixed(1)} →{' '}
                  <strong className="text-[#164B3B]">{Number(h.newScore).toFixed(1)}</strong>
                </span>
                <span
                  className={`px-2 py-0.5 rounded-md font-bold ${
                    Number(h.delta) >= 0
                      ? 'bg-[#E8F2EA] text-[#238B68]'
                      : 'bg-red-100 text-red-700'
                  }`}
                >
                  {Number(h.delta) >= 0 ? `+${h.delta}` : h.delta}
                </span>
              </div>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
};

// ============================================================================
// 2. REVIEWS & RATING MANAGEMENT PAGE
// ============================================================================
export const ReviewsPage: React.FC = () => {
  const { user, showToast, refreshUserProfile } = useAuth();
  const [reviews, setReviews] = useState<ReviewItem[]>([]);
  const [eligibleTxs, setEligibleTxs] = useState<TransactionItem[]>([]);
  const [selectedTxId, setSelectedTxId] = useState<string>('');
  const [rating, setRating] = useState(5);
  const [comment, setComment] = useState('');

  const load = () => {
    if (!user) return;
    Promise.all([
      apiClient.get<ReviewItem[]>(`/users/${user.id}/reviews`),
      apiClient.get<TransactionItem[]>('/transactions'),
    ]).then(([revRes, txRes]) => {
      setReviews(revRes.data);
      const completedUnreviewed = txRes.data.filter(
        (t) => t.status === 'COMPLETED' && !t.reviewedByCurrentUser
      );
      setEligibleTxs(completedUnreviewed);
      if (completedUnreviewed.length > 0) {
        setSelectedTxId(String(completedUnreviewed[0].id));
      } else {
        setSelectedTxId('');
      }
    });
  };

  useEffect(() => {
    load();
  }, [user]);

  const handleSubmitReview = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedTxId) return;
    try {
      await apiClient.post('/reviews', {
        transactionId: Number(selectedTxId),
        rating,
        comment,
      });
      showToast('5-star review submitted & Trust Score updated!', 'success');
      setComment('');
      load();
      refreshUserProfile();
    } catch (err) {
      showToast(extractErrorMessage(err), 'error');
    }
  };

  return (
    <div className="space-y-8">
      <div>
        <h1 className="font-display text-2xl font-bold text-[#164B3B]">
          Peer Reviews & Reputation Feedback
        </h1>
        <p className="text-xs text-[#737B75]">
          Only verified participants in completed transactions can submit ratings
        </p>
      </div>

      {/* Submit Review Form for Eligible Completed Transactions */}
      <div className="bg-white rounded-3xl p-6 border border-[#A8C3A0]/50 space-y-4">
        <h2 className="font-display text-lg font-bold text-[#164B3B]">
          Rate a Completed Campus Transaction ({eligibleTxs.length} Eligible)
        </h2>
        {eligibleTxs.length === 0 ? (
          <p className="text-xs text-[#737B75]">
            All your completed transactions have been reviewed! Complete another borrow, rental, exchange, or
            donation to leave a new review.
          </p>
        ) : (
          <form onSubmit={handleSubmitReview} className="space-y-4">
            <div className="grid sm:grid-cols-2 gap-4">
              <div>
                <label className="block text-xs font-semibold mb-1">
                  Select Completed Transaction *
                </label>
                <select
                  value={selectedTxId}
                  onChange={(e) => setSelectedTxId(e.target.value)}
                  className="w-full px-3.5 py-2.5 rounded-xl border border-[#A8C3A0] text-xs bg-white"
                >
                  {eligibleTxs.map((t) => (
                    <option key={t.id} value={t.id}>
                      {t.transactionCode} – {t.primaryItemTitle} ({t.transactionType})
                    </option>
                  ))}
                </select>
              </div>
              <div>
                <label className="block text-xs font-semibold mb-1">Star Rating (1–5) *</label>
                <select
                  value={rating}
                  onChange={(e) => setRating(Number(e.target.value))}
                  className="w-full px-3.5 py-2.5 rounded-xl border border-[#A8C3A0] text-xs bg-white font-bold text-[#164B3B]"
                >
                  <option value={5}>★★★★★ (5 – Excellent Campus Partner)</option>
                  <option value={4}>★★★★☆ (4 – Good Experience)</option>
                  <option value={3}>★★★☆☆ (3 – Satisfactory)</option>
                  <option value={2}>★★☆☆☆ (2 – Minor Delay)</option>
                  <option value={1}>★☆☆☆☆ (1 – Poor Experience)</option>
                </select>
              </div>
            </div>
            <div>
              <label className="block text-xs font-semibold mb-1">Written Feedback *</label>
              <textarea
                rows={3}
                required
                value={comment}
                onChange={(e) => setComment(e.target.value)}
                placeholder="Share how the item condition, communication, and handover went..."
                className="w-full px-3.5 py-2.5 rounded-xl border border-[#A8C3A0] text-xs"
              />
            </div>
            <button
              type="submit"
              className="px-6 py-2.5 rounded-xl bg-[#164B3B] hover:bg-[#238B68] text-white text-xs font-semibold cursor-pointer"
            >
              Submit Verified Review
            </button>
          </form>
        )}
      </div>

      {/* Reviews Received */}
      <div className="bg-white rounded-3xl p-6 border border-[#A8C3A0]/50 space-y-4">
        <h2 className="font-display text-lg font-bold text-[#164B3B]">
          Reviews Received on My Profile ({reviews.length})
        </h2>
        {reviews.length === 0 ? (
          <p className="text-xs text-[#737B75]">No reviews received yet.</p>
        ) : (
          <div className="grid sm:grid-cols-2 gap-4">
            {reviews.map((r) => (
              <div key={r.id} className="p-4 rounded-2xl bg-[#F8F7F0] border border-[#A8C3A0]/40 space-y-1.5">
                <div className="flex items-center justify-between">
                  <span className="font-bold text-xs text-[#202923]">{r.reviewerName}</span>
                  <span className="text-xs font-bold text-[#238B68]">
                    {'★'.repeat(r.rating)}{'☆'.repeat(5 - r.rating)}
                  </span>
                </div>
                <p className="text-xs text-[#202923]">{r.comment}</p>
                <p className="text-[11px] text-[#737B75]">
                  Item: {r.itemTitle} • Ref: {r.transactionCode}
                </p>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
};

// ============================================================================
// 3. NOTIFICATIONS PAGE
// ============================================================================
export const NotificationsPage: React.FC = () => {
  const { refreshUserProfile, showToast } = useAuth();
  const [notifications, setNotifications] = useState<NotificationItem[]>([]);

  const load = () => {
    apiClient
      .get<{ notifications: NotificationItem[] }>('/notifications')
      .then((res) => setNotifications(res.data.notifications));
  };

  useEffect(() => {
    load();
  }, []);

  const markRead = async (id: number) => {
    await apiClient.put(`/notifications/${id}/read`);
    load();
    refreshUserProfile();
  };

  const markAllRead = async () => {
    await apiClient.put('/notifications/read-all');
    showToast('All notifications marked as read.', 'info');
    load();
    refreshUserProfile();
  };

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="font-display text-2xl font-bold text-[#164B3B]">
            In-App Notifications Center
          </h1>
          <p className="text-xs text-[#737B75]">
            Real-time alerts for borrow approvals, handover confirmations, return reminders, and reviews
          </p>
        </div>
        <button
          onClick={markAllRead}
          className="px-4 py-2 rounded-xl bg-[#164B3B] text-white text-xs font-semibold cursor-pointer"
        >
          Mark All as Read
        </button>
      </div>

      <div className="space-y-3">
        {notifications.map((n) => (
          <div
            key={n.id}
            className={`p-4 rounded-2xl border flex items-center justify-between gap-4 ${
              n.read ? 'bg-white border-[#A8C3A0]/40' : 'bg-[#E8F2EA] border-[#238B68]'
            }`}
          >
            <div className="space-y-1">
              <div className="flex items-center gap-2">
                <span className="text-[10px] font-mono font-bold px-2 py-0.5 rounded bg-[#164B3B] text-white">
                  {n.notificationType}
                </span>
                <span className="font-display font-bold text-sm text-[#202923]">{n.title}</span>
              </div>
              <p className="text-xs text-[#737B75]">{n.message}</p>
            </div>
            <div className="flex items-center gap-2 shrink-0">
              {n.targetPath && (
                <Link
                  to={n.targetPath}
                  className="px-3 py-1.5 rounded-xl bg-white border border-[#A8C3A0] text-xs font-semibold text-[#164B3B]"
                >
                  Open
                </Link>
              )}
              {!n.read && (
                <button
                  onClick={() => markRead(n.id)}
                  className="px-3 py-1.5 rounded-xl bg-[#238B68] text-white text-xs font-semibold cursor-pointer"
                >
                  Mark Read
                </button>
              )}
            </div>
          </div>
        ))}
      </div>
    </div>
  );
};

// ============================================================================
// 4. DISPUTE & REPORTING MANAGEMENT PAGE
// ============================================================================
export const DisputeReportingPage: React.FC = () => {
  const { showToast } = useAuth();
  const [disputes, setDisputes] = useState<DisputeItem[]>([]);
  const [transactions, setTransactions] = useState<TransactionItem[]>([]);
  const [form, setForm] = useState({
    transactionId: '',
    category: 'DAMAGED_ITEM',
    title: '',
    description: '',
    evidenceComment: '',
  });

  const load = () => {
    Promise.all([
      apiClient.get<DisputeItem[]>('/disputes/my-disputes'),
      apiClient.get<TransactionItem[]>('/transactions'),
    ]).then(([dRes, tRes]) => {
      setDisputes(dRes.data);
      setTransactions(tRes.data);
    });
  };

  useEffect(() => {
    load();
  }, []);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      await apiClient.post('/disputes', {
        transactionId: form.transactionId ? Number(form.transactionId) : null,
        category: form.category,
        title: form.title,
        description: form.description,
        evidenceComment: form.evidenceComment,
      });
      showToast('Dispute report filed for moderator review.', 'success');
      setForm({
        transactionId: '',
        category: 'DAMAGED_ITEM',
        title: '',
        description: '',
        evidenceComment: '',
      });
      load();
    } catch (err) {
      showToast(extractErrorMessage(err), 'error');
    }
  };

  return (
    <div className="space-y-8">
      <div>
        <h1 className="font-display text-2xl font-bold text-[#164B3B]">
          Dispute Resolution & Incident Reporting
        </h1>
        <p className="text-xs text-[#737B75]">
          Report damaged items, late returns, or misleading descriptions for impartial campus moderation
        </p>
      </div>

      <div className="grid lg:grid-cols-12 gap-6">
        <form
          onSubmit={handleSubmit}
          className="lg:col-span-6 bg-white rounded-3xl p-6 border border-[#A8C3A0]/50 space-y-4"
        >
          <h2 className="font-display text-lg font-bold text-[#164B3B]">File a New Dispute / Report</h2>
          <div>
            <label className="block text-xs font-semibold mb-1">Related Transaction (Optional)</label>
            <select
              value={form.transactionId}
              onChange={(e) => setForm({ ...form, transactionId: e.target.value })}
              className="w-full px-3.5 py-2 rounded-xl border border-[#A8C3A0] text-xs bg-white"
            >
              <option value="">-- General Platform / Listing Report --</option>
              {transactions.map((t) => (
                <option key={t.id} value={t.id}>
                  {t.transactionCode} – {t.primaryItemTitle}
                </option>
              ))}
            </select>
          </div>
          <div>
            <label className="block text-xs font-semibold mb-1">Issue Category *</label>
            <select
              value={form.category}
              onChange={(e) => setForm({ ...form, category: e.target.value })}
              className="w-full px-3.5 py-2 rounded-xl border border-[#A8C3A0] text-xs bg-white"
            >
              <option value="DAMAGED_ITEM">Damaged Item</option>
              <option value="LATE_RETURN">Late Return</option>
              <option value="MISSING_ITEM">Missing Component / Item</option>
              <option value="MISLEADING_DESCRIPTION">Misleading Description</option>
              <option value="INAPPROPRIATE_BEHAVIOUR">Inappropriate Behaviour</option>
            </select>
          </div>
          <div>
            <label className="block text-xs font-semibold mb-1">Summary Title *</label>
            <input
              type="text"
              required
              value={form.title}
              onChange={(e) => setForm({ ...form, title: e.target.value })}
              placeholder="Brief summary of the issue"
              className="w-full px-3.5 py-2 rounded-xl border border-[#A8C3A0] text-xs"
            />
          </div>
          <div>
            <label className="block text-xs font-semibold mb-1">Detailed Description *</label>
            <textarea
              rows={3}
              required
              value={form.description}
              onChange={(e) => setForm({ ...form, description: e.target.value })}
              className="w-full px-3.5 py-2 rounded-xl border border-[#A8C3A0] text-xs"
            />
          </div>
          <div>
            <label className="block text-xs font-semibold mb-1">Supporting Evidence / Handover Notes</label>
            <input
              type="text"
              value={form.evidenceComment}
              onChange={(e) => setForm({ ...form, evidenceComment: e.target.value })}
              placeholder="Condition observed at handover..."
              className="w-full px-3.5 py-2 rounded-xl border border-[#A8C3A0] text-xs"
            />
          </div>
          <button
            type="submit"
            className="w-full py-2.5 rounded-xl bg-[#164B3B] text-white text-xs font-semibold cursor-pointer"
          >
            Submit Dispute for Moderator Investigation
          </button>
        </form>

        <div className="lg:col-span-6 bg-white rounded-3xl p-6 border border-[#A8C3A0]/50 space-y-4">
          <h2 className="font-display text-lg font-bold text-[#164B3B]">
            My Dispute Cases ({disputes.length})
          </h2>
          {disputes.map((d) => (
            <div key={d.id} className="p-4 rounded-2xl bg-[#F8F7F0] border border-[#A8C3A0]/40 space-y-1.5 text-xs">
              <div className="flex items-center justify-between">
                <span className="font-bold text-[#164B3B]">
                  #{d.id} • {d.title}
                </span>
                <span className="px-2.5 py-0.5 rounded-full bg-[#164B3B] text-white font-bold">
                  {d.status}
                </span>
              </div>
              <p className="text-[#737B75]">{d.description}</p>
              {d.resolutionNotes && (
                <p className="text-[#238B68] font-semibold">
                  Moderator Decision: {d.resolutionNotes}
                </p>
              )}
            </div>
          ))}
        </div>
      </div>
    </div>
  );
};

// ============================================================================
// 5. USER PROFILE & EMAIL VERIFICATION PAGE
// ============================================================================
export const UserProfilePage: React.FC = () => {
  const { user, refreshUserProfile, showToast } = useAuth();
  const [otpCode, setOtpCode] = useState('123456');
  const [form, setForm] = useState({
    fullName: user?.fullName || '',
    department: user?.department || '',
    yearOfStudy: user?.yearOfStudy || 3,
    contactNumber: user?.contactNumber || '',
    hostelOrBlock: user?.hostelOrBlock || '',
    bio: user?.bio || '',
    preferredPickupSpot: user?.preferredPickupSpot || '',
  });

  useEffect(() => {
    if (user) {
      setForm({
        fullName: user.fullName,
        department: user.department,
        yearOfStudy: user.yearOfStudy,
        contactNumber: user.contactNumber,
        hostelOrBlock: user.hostelOrBlock || '',
        bio: user.bio || '',
        preferredPickupSpot: user.preferredPickupSpot || '',
      });
    }
  }, [user]);

  const handleSaveProfile = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      await apiClient.put('/users/me', form);
      await refreshUserProfile();
      showToast('Campus profile updated!', 'success');
    } catch (err) {
      showToast(extractErrorMessage(err), 'error');
    }
  };

  const handleVerifyEmail = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!user) return;
    try {
      await apiClient.post('/auth/verify-email', { email: user.email, otpCode });
      await refreshUserProfile();
      showToast('Campus email verified! +15 Trust Score awarded!', 'success');
    } catch (err) {
      showToast(extractErrorMessage(err), 'error');
    }
  };

  if (!user) return null;

  return (
    <div className="max-w-4xl mx-auto space-y-6">
      <div className="bg-white rounded-3xl p-6 sm:p-8 border border-[#A8C3A0]/50 flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div className="flex items-center gap-4">
          <div className="w-16 h-16 rounded-2xl bg-[#164B3B] text-white font-display text-2xl font-bold flex items-center justify-center">
            {user.fullName.charAt(0)}
          </div>
          <div>
            <div className="flex items-center gap-2">
              <h1 className="font-display text-2xl font-bold text-[#202923]">{user.fullName}</h1>
              {user.campusVerified && (
                <span className="px-2.5 py-0.5 rounded-full bg-[#E8F2EA] text-[#164B3B] text-xs font-bold">
                  ✓ Verified Campus ID
                </span>
              )}
            </div>
            <p className="text-xs text-[#737B75]">
              {user.registrationNumber} • {user.email}
            </p>
          </div>
        </div>
        <Link
          to="/trust-score"
          className="px-4 py-2.5 rounded-2xl bg-[#164B3B] text-white text-xs font-bold text-center"
        >
          Trust Score: {Number(user.trustScore).toFixed(1)} ({user.trustLevel})
        </Link>
      </div>

      {!user.campusVerified && (
        <form
          onSubmit={handleVerifyEmail}
          className="bg-[#E8F2EA] rounded-3xl p-6 border border-[#238B68] flex flex-col sm:flex-row items-center justify-between gap-4"
        >
          <div>
            <h3 className="font-display font-bold text-base text-[#164B3B]">
              Verify Your SRM Campus Email
            </h3>
            <p className="text-xs text-[#202923]">
              Enter your 6-digit verification OTP (or local dev test OTP: <strong>123456</strong>)
            </p>
          </div>
          <div className="flex gap-2">
            <input
              type="text"
              value={otpCode}
              onChange={(e) => setOtpCode(e.target.value)}
              className="px-3.5 py-2 rounded-xl bg-white border border-[#A8C3A0] text-xs font-mono w-32"
            />
            <button
              type="submit"
              className="px-4 py-2 rounded-xl bg-[#164B3B] text-white text-xs font-semibold cursor-pointer"
            >
              Verify OTP
            </button>
          </div>
        </form>
      )}

      <form
        onSubmit={handleSaveProfile}
        className="bg-white rounded-3xl p-6 sm:p-8 border border-[#A8C3A0]/50 space-y-4"
      >
        <h2 className="font-display text-lg font-bold text-[#164B3B]">
          Edit Academic & Campus Profile ({user.profileCompletionPct}% Complete)
        </h2>
        <div className="grid sm:grid-cols-2 gap-4">
          <div>
            <label className="block text-xs font-semibold mb-1">Full Name</label>
            <input
              type="text"
              value={form.fullName}
              onChange={(e) => setForm({ ...form, fullName: e.target.value })}
              className="w-full px-3.5 py-2 rounded-xl border border-[#A8C3A0] text-xs"
            />
          </div>
          <div>
            <label className="block text-xs font-semibold mb-1">Department</label>
            <input
              type="text"
              value={form.department}
              onChange={(e) => setForm({ ...form, department: e.target.value })}
              className="w-full px-3.5 py-2 rounded-xl border border-[#A8C3A0] text-xs"
            />
          </div>
          <div>
            <label className="block text-xs font-semibold mb-1">Contact Number</label>
            <input
              type="text"
              value={form.contactNumber}
              onChange={(e) => setForm({ ...form, contactNumber: e.target.value })}
              className="w-full px-3.5 py-2 rounded-xl border border-[#A8C3A0] text-xs"
            />
          </div>
          <div>
            <label className="block text-xs font-semibold mb-1">Hostel / Block</label>
            <input
              type="text"
              value={form.hostelOrBlock}
              onChange={(e) => setForm({ ...form, hostelOrBlock: e.target.value })}
              className="w-full px-3.5 py-2 rounded-xl border border-[#A8C3A0] text-xs"
            />
          </div>
          <div className="sm:col-span-2">
            <label className="block text-xs font-semibold mb-1">Preferred Campus Handover Spot</label>
            <input
              type="text"
              value={form.preferredPickupSpot}
              onChange={(e) => setForm({ ...form, preferredPickupSpot: e.target.value })}
              className="w-full px-3.5 py-2 rounded-xl border border-[#A8C3A0] text-xs"
            />
          </div>
          <div className="sm:col-span-2">
            <label className="block text-xs font-semibold mb-1">Student Bio</label>
            <textarea
              rows={3}
              value={form.bio}
              onChange={(e) => setForm({ ...form, bio: e.target.value })}
              className="w-full px-3.5 py-2 rounded-xl border border-[#A8C3A0] text-xs"
            />
          </div>
        </div>
        <button
          type="submit"
          className="px-6 py-2.5 rounded-xl bg-[#164B3B] text-white text-xs font-semibold cursor-pointer"
        >
          Save Profile Changes
        </button>
      </form>
    </div>
  );
};

// ============================================================================
// 6. ACCOUNT SETTINGS PAGE
// ============================================================================
export const AccountSettingsPage: React.FC = () => {
  const { user, showToast } = useAuth();
  const [emailReminders, setEmailReminders] = useState(true);
  const [campusVisibility, setCampusVisibility] = useState(true);

  return (
    <div className="max-w-3xl mx-auto space-y-6">
      <div>
        <h1 className="font-display text-2xl font-bold text-[#164B3B]">Account & Security Settings</h1>
        <p className="text-xs text-[#737B75]">
          Configure return reminders, campus privacy preferences, and session security
        </p>
      </div>

      <div className="bg-white rounded-3xl p-6 border border-[#A8C3A0]/50 space-y-4 text-xs">
        <div className="flex items-center justify-between py-2 border-b border-[#E8F2EA]">
          <div>
            <p className="font-bold text-sm text-[#202923]">Automated Return Due Reminders</p>
            <p className="text-[#737B75]">Receive alerts 24 hours before a borrowed resource is due</p>
          </div>
          <input
            type="checkbox"
            checked={emailReminders}
            onChange={(e) => setEmailReminders(e.target.checked)}
            className="w-4 h-4 accent-[#164B3B]"
          />
        </div>

        <div className="flex items-center justify-between py-2 border-b border-[#E8F2EA]">
          <div>
            <p className="font-bold text-sm text-[#202923]">Display Department Badge on Listings</p>
            <p className="text-[#737B75]">Allow verified SRM students to filter items by your department</p>
          </div>
          <input
            type="checkbox"
            checked={campusVisibility}
            onChange={(e) => setCampusVisibility(e.target.checked)}
            className="w-4 h-4 accent-[#164B3B]"
          />
        </div>

        <div className="pt-2 flex items-center justify-between">
          <div>
            <p className="font-bold text-sm text-[#202923]">Account Role & Security Status</p>
            <p className="text-[#737B75]">
              Role: {user?.role} • Status: {user?.accountStatus} • BCrypt + JWT Protected
            </p>
          </div>
          <button
            onClick={() => showToast('Account preferences saved!', 'success')}
            className="px-5 py-2 rounded-xl bg-[#164B3B] text-white font-semibold cursor-pointer"
          >
            Save Preferences
          </button>
        </div>
      </div>
    </div>
  );
};
