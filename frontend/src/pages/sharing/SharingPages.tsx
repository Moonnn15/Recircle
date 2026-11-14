import React, { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import {
  Clock,
  ShieldCheck,
  ArrowLeftRight,
  Receipt,
  HandHeart,
  CheckCircle2,
  XCircle,
  MapPin,
  AlertTriangle,
  Calendar,
  RefreshCw,
} from 'lucide-react';
import { apiClient, extractErrorMessage } from '../../api/client';
import {
  BorrowRequestItem,
  DonationRequestItem,
  ExchangeOfferItem,
  ItemListing,
  RentalItem,
  TransactionItem,
} from '../../types';
import { useAuth } from '../../context/AuthContext';
import { ItemCard } from '../../components/cards/ItemCard';

// ============================================================================
// SHARED UI HELPERS: STATUS BADGES, TIMELINES & CONFIRMATION MODAL
// ============================================================================

const statusBadgeClass = (status: string): string => {
  switch (status) {
    case 'COMPLETED':
      return 'bg-emerald-700 text-white';
    case 'APPROVED':
    case 'ACCEPTED':
      return 'bg-[#238B68] text-white';
    case 'ACTIVE':
    case 'HANDOVER_PENDING':
    case 'RETURN_REQUESTED':
      return 'bg-[#164B3B] text-white';
    case 'REJECTED':
    case 'CANCELLED':
    case 'OVERDUE':
    case 'DISPUTED':
      return 'bg-red-100 text-red-800 border border-red-300';
    default:
      return 'bg-[#E8F2EA] text-[#164B3B] border border-[#A8C3A0]';
  }
};

const WorkflowTimeline: React.FC<{
  steps: string[];
  currentStatus: string;
}> = ({ steps, currentStatus }) => {
  const isTerminalNegative = ['REJECTED', 'CANCELLED', 'DISPUTED', 'OVERDUE'].includes(currentStatus);
  const activeIdx = steps.indexOf(currentStatus);

  return (
    <div className="bg-[#F8F7F0] rounded-2xl p-4 border border-[#A8C3A0]/50 space-y-2">
      <div className="flex items-center justify-between text-[11px] font-bold text-[#164B3B]">
        <span>Workflow Lifecycle Progression</span>
        <span className={`px-2.5 py-0.5 rounded-full text-[10px] ${statusBadgeClass(currentStatus)}`}>
          {currentStatus}
        </span>
      </div>
      <div className="grid grid-cols-2 sm:grid-cols-5 gap-2 pt-1">
        {steps.map((step, idx) => {
          const completed = !isTerminalNegative && activeIdx >= idx;
          const isCurrent = currentStatus === step;
          return (
            <div
              key={step}
              className={`p-2.5 rounded-xl border text-center text-[11px] font-semibold transition-all ${
                isCurrent
                  ? 'bg-[#164B3B] text-white border-[#164B3B] shadow-xs'
                  : completed
                  ? 'bg-[#E8F2EA] text-[#164B3B] border-[#238B68]'
                  : 'bg-white text-[#737B75] border-[#A8C3A0]/40'
              }`}
            >
              <div className="text-[10px] opacity-75">Step {idx + 1}</div>
              <div className="truncate">{step.replace('_', ' ')}</div>
            </div>
          );
        })}
      </div>
      {isTerminalNegative && (
        <div className="flex items-center gap-2 text-xs text-red-700 font-semibold pt-1">
          <AlertTriangle className="w-4 h-4 shrink-0" />
          <span>Workflow transitioned to terminal/exception state: {currentStatus}</span>
        </div>
      )}
    </div>
  );
};

interface ConfirmModalState {
  open: boolean;
  title: string;
  description: string;
  confirmLabel: string;
  allowNote?: boolean;
  defaultNote?: string;
  danger?: boolean;
  onConfirm: (note?: string) => Promise<void>;
}

const ConfirmActionModal: React.FC<{
  modal: ConfirmModalState | null;
  onClose: () => void;
}> = ({ modal, onClose }) => {
  const [note, setNote] = useState(modal?.defaultNote || '');
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    setNote(modal?.defaultNote || '');
  }, [modal]);

  if (!modal || !modal.open) return null;

  const handleConfirm = async () => {
    setSubmitting(true);
    try {
      await modal.onConfirm(note);
      onClose();
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 bg-black/50 flex items-center justify-center p-4">
      <div className="bg-white rounded-3xl max-w-md w-full p-6 border border-[#A8C3A0] shadow-xl space-y-4">
        <h3 className="font-display text-lg font-bold text-[#164B3B]">{modal.title}</h3>
        <p className="text-xs text-[#737B75] leading-relaxed">{modal.description}</p>
        {modal.allowNote && (
          <div>
            <label className="block text-xs font-semibold text-[#202923] mb-1">
              Optional Note for Counterparty
            </label>
            <textarea
              rows={2}
              value={note}
              onChange={(e) => setNote(e.target.value)}
              placeholder="Add pickup instructions or reason..."
              className="w-full px-3 py-2 rounded-xl border border-[#A8C3A0] text-xs"
            />
          </div>
        )}
        <div className="flex justify-end gap-2 pt-2">
          <button
            type="button"
            disabled={submitting}
            onClick={onClose}
            className="px-4 py-2 rounded-xl border border-[#A8C3A0] text-xs font-semibold text-[#202923] cursor-pointer"
          >
            Cancel
          </button>
          <button
            type="button"
            disabled={submitting}
            onClick={handleConfirm}
            className={`px-4 py-2 rounded-xl text-white text-xs font-semibold cursor-pointer ${
              modal.danger ? 'bg-red-600 hover:bg-red-700' : 'bg-[#164B3B] hover:bg-[#238B68]'
            }`}
          >
            {submitting ? 'Processing...' : modal.confirmLabel}
          </button>
        </div>
      </div>
    </div>
  );
};

// ============================================================================
// 1. BORROW REQUESTS PAGE (Borrower View — /borrowing)
// ============================================================================
export const BorrowingPage: React.FC = () => {
  const { showToast } = useAuth();
  const [requests, setRequests] = useState<BorrowRequestItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [statusFilter, setStatusFilter] = useState<string>('ALL');
  const [modal, setModal] = useState<ConfirmModalState | null>(null);

  const load = () => {
    setLoading(true);
    setError(null);
    apiClient
      .get<BorrowRequestItem[]>('/borrow-requests/my-requests')
      .then((res) => setRequests(res.data))
      .catch((err) => setError(extractErrorMessage(err)))
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    load();
  }, []);

  const promptAction = (
    r: BorrowRequestItem,
    action: 'confirm-handover' | 'request-return' | 'cancel'
  ) => {
    const titles: Record<string, string> = {
      'confirm-handover': `Confirm Physical Handover for "${r.itemTitle}"`,
      'request-return': `Initiate Return for "${r.itemTitle}"`,
      cancel: `Cancel Borrow Request #${r.id}`,
    };
    const descriptions: Record<string, string> = {
      'confirm-handover': `Confirm that you have met ${r.lenderName} at ${r.campusLocation} and received the item in good condition. Status will transition to ACTIVE.`,
      'request-return': `Notify ${r.lenderName} that you are ready to return "${r.itemTitle}". Status will transition to RETURN_REQUESTED.`,
      cancel: `Are you sure you want to cancel your borrow request for "${r.itemTitle}" (${r.startDate} to ${r.endDate})?`,
    };
    setModal({
      open: true,
      title: titles[action],
      description: descriptions[action],
      confirmLabel: action === 'cancel' ? 'Yes, Cancel Request' : 'Confirm Action',
      danger: action === 'cancel',
      onConfirm: async () => {
        try {
          await apiClient.put(`/borrow-requests/${r.id}/${action}`);
          showToast(`Borrow request updated (${action})!`, 'success');
          load();
        } catch (err) {
          showToast(extractErrorMessage(err), 'error');
        }
      },
    });
  };

  const filtered = requests.filter((r) => statusFilter === 'ALL' || r.status === statusFilter);

  return (
    <div className="space-y-6">
      <ConfirmActionModal modal={modal} onClose={() => setModal(null)} />

      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
        <div>
          <h1 className="font-display text-2xl font-bold text-[#164B3B]">My Borrowing Requests</h1>
          <p className="text-xs text-[#737B75]">
            Track resources you have requested to borrow, confirm handovers, and initiate timely returns
          </p>
        </div>
        <button
          onClick={load}
          className="inline-flex items-center gap-1.5 px-3.5 py-2 rounded-xl bg-white border border-[#A8C3A0] text-xs font-semibold text-[#164B3B] self-start cursor-pointer"
        >
          <RefreshCw className="w-3.5 h-3.5" />
          Refresh
        </button>
      </div>

      {/* Status Filter Pills */}
      <div className="flex flex-wrap gap-2">
        {['ALL', 'PENDING', 'APPROVED', 'ACTIVE', 'RETURN_REQUESTED', 'COMPLETED', 'REJECTED', 'CANCELLED'].map(
          (st) => (
            <button
              key={st}
              onClick={() => setStatusFilter(st)}
              className={`px-3 py-1.5 rounded-xl text-xs font-semibold cursor-pointer ${
                statusFilter === st
                  ? 'bg-[#164B3B] text-white'
                  : 'bg-white text-[#737B75] border border-[#A8C3A0]/50'
              }`}
            >
              {st.replace('_', ' ')}
            </button>
          )
        )}
      </div>

      {error && (
        <div className="p-4 rounded-2xl bg-red-50 border border-red-200 text-xs text-red-800 flex items-center justify-between">
          <span>{error}</span>
          <button onClick={load} className="font-bold underline cursor-pointer">
            Retry
          </button>
        </div>
      )}

      {loading ? (
        <div className="h-44 bg-white rounded-3xl animate-pulse" />
      ) : filtered.length === 0 ? (
        <div className="bg-white rounded-3xl p-10 text-center border border-[#A8C3A0]/50 space-y-3">
          <Clock className="w-8 h-8 text-[#238B68] mx-auto" />
          <p className="font-display text-base font-bold">No matching borrow requests found</p>
          <Link
            to="/discover"
            className="inline-block px-4 py-2 rounded-xl bg-[#164B3B] text-white text-xs font-semibold"
          >
            Browse Available Items
          </Link>
        </div>
      ) : (
        <div className="space-y-4">
          {filtered.map((r) => (
            <div
              key={r.id}
              className="bg-white rounded-2xl p-5 border border-[#A8C3A0]/50 shadow-xs flex flex-col sm:flex-row sm:items-center justify-between gap-4"
            >
              <div className="space-y-1.5">
                <div className="flex flex-wrap items-center gap-2">
                  <span className={`px-2.5 py-0.5 rounded-full text-xs font-bold ${statusBadgeClass(r.status)}`}>
                    {r.status}
                  </span>
                  <span className="text-xs text-[#737B75] inline-flex items-center gap-1">
                    <Calendar className="w-3.5 h-3.5 text-[#238B68]" />
                    {r.startDate} → {r.endDate}
                  </span>
                  {r.returnedOnTime !== null && (
                    <span
                      className={`px-2 py-0.5 rounded-full text-[10px] font-bold ${
                        r.returnedOnTime ? 'bg-emerald-100 text-emerald-800' : 'bg-amber-100 text-amber-900'
                      }`}
                    >
                      {r.returnedOnTime ? 'Returned On Time ✓' : 'Returned Late'}
                    </span>
                  )}
                </div>
                <Link
                  to={`/borrowing/${r.id}`}
                  className="font-display font-bold text-base text-[#202923] hover:text-[#164B3B] block"
                >
                  {r.itemTitle}
                </Link>
                <p className="text-xs text-[#737B75]">
                  Lender: <strong>{r.lenderName}</strong> • Pickup: {r.campusLocation}
                </p>
                <p className="text-xs text-[#737B75]">Purpose: {r.purpose}</p>
                {r.ownerResponseNote && (
                  <p className="text-xs text-[#238B68] font-medium">
                    Owner Note: &ldquo;{r.ownerResponseNote}&rdquo;
                  </p>
                )}
              </div>

              <div className="flex flex-wrap items-center gap-2 shrink-0">
                <Link
                  to={`/borrowing/${r.id}`}
                  className="px-3.5 py-2 rounded-xl bg-[#F8F7F0] border border-[#A8C3A0] text-[#164B3B] text-xs font-semibold"
                >
                  View Details
                </Link>
                {r.status === 'APPROVED' && (
                  <button
                    onClick={() => promptAction(r.id ? r : r, 'confirm-handover')}
                    className="px-4 py-2 rounded-xl bg-[#164B3B] text-white text-xs font-semibold cursor-pointer"
                  >
                    Confirm Physical Handover
                  </button>
                )}
                {r.status === 'ACTIVE' && (
                  <button
                    onClick={() => promptAction(r, 'request-return')}
                    className="px-4 py-2 rounded-xl bg-[#238B68] text-white text-xs font-semibold cursor-pointer"
                  >
                    Mark Ready for Return
                  </button>
                )}
                {(r.status === 'PENDING' || r.status === 'APPROVED') && (
                  <button
                    onClick={() => promptAction(r, 'cancel')}
                    className="px-3.5 py-2 rounded-xl border border-red-300 text-red-700 text-xs font-semibold cursor-pointer"
                  >
                    Cancel
                  </button>
                )}
                {r.status === 'COMPLETED' && (
                  <Link
                    to="/reviews"
                    className="px-4 py-2 rounded-xl bg-[#E8F2EA] text-[#164B3B] text-xs font-semibold"
                  >
                    Rate Owner ★
                  </Link>
                )}
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
};

// ============================================================================
// 2. LENDING MANAGEMENT PAGE (Owner View — /lending)
// ============================================================================
export const LendingPage: React.FC = () => {
  const { showToast } = useAuth();
  const [requests, setRequests] = useState<BorrowRequestItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [statusFilter, setStatusFilter] = useState<string>('ALL');
  const [modal, setModal] = useState<ConfirmModalState | null>(null);

  const load = () => {
    setLoading(true);
    setError(null);
    apiClient
      .get<BorrowRequestItem[]>('/borrow-requests/received')
      .then((res) => setRequests(res.data))
      .catch((err) => setError(extractErrorMessage(err)))
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    load();
  }, []);

  const promptDecision = (
    r: BorrowRequestItem,
    action: 'approve' | 'reject' | 'confirm-handover' | 'confirm-return'
  ) => {
    setModal({
      open: true,
      title:
        action === 'approve'
          ? `Approve Borrow Request for "${r.itemTitle}"`
          : action === 'reject'
          ? `Decline Borrow Request for "${r.itemTitle}"`
          : action === 'confirm-handover'
          ? `Confirm Item Handover to ${r.borrowerName}`
          : `Confirm Return of "${r.itemTitle}"`,
      description:
        action === 'approve'
          ? `Approve ${r.borrowerName} (${Number(r.borrowerTrustScore).toFixed(1)} TS) to borrow "${r.itemTitle}" from ${r.startDate} to ${r.endDate}. Overlapping pending requests for these dates will be automatically declined.`
          : action === 'reject'
          ? `Decline ${r.borrowerName}'s borrow request for "${r.itemTitle}".`
          : action === 'confirm-handover'
          ? `Confirm that "${r.itemTitle}" has been handed over to ${r.borrowerName}.`
          : `Confirm that ${r.borrowerName} has returned "${r.itemTitle}". The system will record the return timestamp, evaluate on-time return compliance, and update both Trust Scores.`,
      confirmLabel:
        action === 'approve'
          ? 'Approve Booking'
          : action === 'reject'
          ? 'Decline Request'
          : 'Confirm',
      allowNote: action === 'approve' || action === 'reject',
      defaultNote:
        action === 'approve'
          ? `Approved! Let's meet at ${r.campusLocation}.`
          : action === 'reject'
          ? 'Unavailable during the requested dates.'
          : '',
      danger: action === 'reject',
      onConfirm: async (note) => {
        try {
          await apiClient.put(`/borrow-requests/${r.id}/${action}`, note ? { note } : {});
          showToast(`Lending action (${action}) recorded!`, 'success');
          load();
        } catch (err) {
          showToast(extractErrorMessage(err), 'error');
        }
      },
    });
  };

  const filtered = requests.filter((r) => statusFilter === 'ALL' || r.status === statusFilter);

  return (
    <div className="space-y-6">
      <ConfirmActionModal modal={modal} onClose={() => setModal(null)} />

      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
        <div>
          <h1 className="font-display text-2xl font-bold text-[#164B3B]">
            Incoming Lending Requests
          </h1>
          <p className="text-xs text-[#737B75]">
            Review borrower Trust Scores, approve non-conflicting dates, and confirm item returns
          </p>
        </div>
        <button
          onClick={load}
          className="inline-flex items-center gap-1.5 px-3.5 py-2 rounded-xl bg-white border border-[#A8C3A0] text-xs font-semibold text-[#164B3B] self-start cursor-pointer"
        >
          <RefreshCw className="w-3.5 h-3.5" />
          Refresh
        </button>
      </div>

      {/* Status Filter Pills */}
      <div className="flex flex-wrap gap-2">
        {['ALL', 'PENDING', 'APPROVED', 'ACTIVE', 'RETURN_REQUESTED', 'COMPLETED', 'REJECTED'].map(
          (st) => (
            <button
              key={st}
              onClick={() => setStatusFilter(st)}
              className={`px-3 py-1.5 rounded-xl text-xs font-semibold cursor-pointer ${
                statusFilter === st
                  ? 'bg-[#164B3B] text-white'
                  : 'bg-white text-[#737B75] border border-[#A8C3A0]/50'
              }`}
            >
              {st.replace('_', ' ')}
            </button>
          )
        )}
      </div>

      {error && (
        <div className="p-4 rounded-2xl bg-red-50 border border-red-200 text-xs text-red-800">
          {error}
        </div>
      )}

      {loading ? (
        <div className="h-44 bg-white rounded-3xl animate-pulse" />
      ) : filtered.length === 0 ? (
        <div className="bg-white rounded-3xl p-10 text-center border border-[#A8C3A0]/50">
          <p className="text-sm font-semibold">No incoming borrow requests matching this filter.</p>
        </div>
      ) : (
        <div className="space-y-4">
          {filtered.map((r) => (
            <div
              key={r.id}
              className="bg-white rounded-2xl p-5 border border-[#A8C3A0]/50 shadow-xs flex flex-col sm:flex-row sm:items-center justify-between gap-4"
            >
              <div className="space-y-1.5">
                <div className="flex flex-wrap items-center gap-2">
                  <span className={`px-2.5 py-0.5 rounded-full text-xs font-bold ${statusBadgeClass(r.status)}`}>
                    {r.status}
                  </span>
                  <span className="px-2.5 py-0.5 rounded-full bg-[#E8F2EA] text-[#164B3B] text-xs font-bold">
                    Borrower Trust: {Number(r.borrowerTrustScore).toFixed(1)} TS
                  </span>
                  {r.returnedOnTime !== null && (
                    <span
                      className={`px-2 py-0.5 rounded-full text-[10px] font-bold ${
                        r.returnedOnTime ? 'bg-emerald-100 text-emerald-800' : 'bg-amber-100 text-amber-900'
                      }`}
                    >
                      {r.returnedOnTime ? 'On-Time Return ✓' : 'Late Return'}
                    </span>
                  )}
                </div>
                <Link
                  to={`/borrowing/${r.id}`}
                  className="font-display font-bold text-base text-[#202923] hover:text-[#164B3B] block"
                >
                  {r.itemTitle}
                </Link>
                <p className="text-xs text-[#737B75]">
                  Requested by <strong>{r.borrowerName}</strong> • {r.startDate} to {r.endDate}
                </p>
                <p className="text-xs text-[#737B75]">Purpose: &ldquo;{r.purpose}&rdquo;</p>
              </div>

              <div className="flex flex-wrap items-center gap-2 shrink-0">
                <Link
                  to={`/borrowing/${r.id}`}
                  className="px-3.5 py-2 rounded-xl bg-[#F8F7F0] border border-[#A8C3A0] text-[#164B3B] text-xs font-semibold"
                >
                  Details
                </Link>
                {r.status === 'PENDING' && (
                  <>
                    <button
                      onClick={() => promptDecision(r, 'approve')}
                      className="px-4 py-2 rounded-xl bg-[#238B68] text-white text-xs font-semibold cursor-pointer"
                    >
                      Approve Request
                    </button>
                    <button
                      onClick={() => promptDecision(r, 'reject')}
                      className="px-4 py-2 rounded-xl border border-red-300 text-red-700 text-xs font-semibold cursor-pointer"
                    >
                      Decline
                    </button>
                  </>
                )}
                {r.status === 'APPROVED' && (
                  <button
                    onClick={() => promptDecision(r, 'confirm-handover')}
                    className="px-4 py-2 rounded-xl bg-[#164B3B] text-white text-xs font-semibold cursor-pointer"
                  >
                    Confirm Handover
                  </button>
                )}
                {(r.status === 'ACTIVE' || r.status === 'RETURN_REQUESTED') && (
                  <button
                    onClick={() => promptDecision(r, 'confirm-return')}
                    className="px-4 py-2 rounded-xl bg-[#164B3B] text-white text-xs font-semibold cursor-pointer"
                  >
                    Confirm Item Returned
                  </button>
                )}
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
};

// ============================================================================
// 3. BORROW REQUEST DETAILS PAGE (/borrowing/:id & /borrow-requests/:id)
// ============================================================================
export const BorrowRequestDetailsPage: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const { user, showToast } = useAuth();
  const [reqItem, setReqItem] = useState<BorrowRequestItem | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [modal, setModal] = useState<ConfirmModalState | null>(null);

  const load = () => {
    if (!id) return;
    setLoading(true);
    setError(null);
    apiClient
      .get<BorrowRequestItem>(`/borrow-requests/${id}`)
      .then((res) => setReqItem(res.data))
      .catch((err) => setError(extractErrorMessage(err)))
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    load();
  }, [id]);

  if (loading) {
    return <div className="h-64 bg-white rounded-3xl animate-pulse" />;
  }

  if (error || !reqItem) {
    return (
      <div className="max-w-2xl mx-auto bg-white rounded-3xl p-8 border border-[#A8C3A0]/50 text-center space-y-3">
        <p className="text-sm font-bold text-red-700">{error || 'Borrow request not found.'}</p>
        <Link to="/borrowing" className="inline-block px-4 py-2 rounded-xl bg-[#164B3B] text-white text-xs font-semibold">
          ← Back to Borrowing
        </Link>
      </div>
    );
  }

  const isBorrower = user?.id === reqItem.borrowerId;
  const isLender = user?.id === reqItem.lenderId;

  const triggerAction = (
    action: 'approve' | 'reject' | 'cancel' | 'confirm-handover' | 'request-return' | 'confirm-return',
    label: string
  ) => {
    setModal({
      open: true,
      title: `${label} — Borrow Request #${reqItem.id}`,
      description: `Confirm "${label}" for resource "${reqItem.itemTitle}" (${reqItem.startDate} → ${reqItem.endDate}).`,
      confirmLabel: label,
      allowNote: action === 'approve' || action === 'reject',
      defaultNote: action === 'approve' ? `Approved! Meet at ${reqItem.campusLocation}.` : '',
      danger: action === 'reject' || action === 'cancel',
      onConfirm: async (note) => {
        try {
          await apiClient.put(`/borrow-requests/${reqItem.id}/${action}`, note ? { note } : {});
          showToast(`Borrow request updated (${action})!`, 'success');
          load();
        } catch (err) {
          showToast(extractErrorMessage(err), 'error');
        }
      },
    });
  };

  return (
    <div className="max-w-3xl mx-auto bg-white rounded-3xl p-6 sm:p-8 border border-[#A8C3A0]/50 space-y-6">
      <ConfirmActionModal modal={modal} onClose={() => setModal(null)} />

      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <span className="text-xs font-mono font-bold text-[#238B68]">BORROW REQUEST #{reqItem.id}</span>
          <h1 className="font-display text-2xl font-bold text-[#164B3B] mt-0.5">{reqItem.itemTitle}</h1>
        </div>
        <span className={`px-3.5 py-1 rounded-full text-xs font-bold ${statusBadgeClass(reqItem.status)}`}>
          {reqItem.status}
        </span>
      </div>

      <WorkflowTimeline
        steps={['PENDING', 'APPROVED', 'ACTIVE', 'RETURN_REQUESTED', 'COMPLETED']}
        currentStatus={reqItem.status}
      />

      <div className="grid sm:grid-cols-2 gap-4 text-xs">
        <div className="p-4 rounded-2xl bg-[#F8F7F0] border border-[#A8C3A0]/40 space-y-1.5">
          <span className="text-[10px] font-bold uppercase text-[#238B68]">Borrower</span>
          <p className="font-display font-bold text-sm text-[#202923]">{reqItem.borrowerName}</p>
          <p className="text-[#737B75]">Trust Score: {Number(reqItem.borrowerTrustScore).toFixed(1)} TS</p>
        </div>
        <div className="p-4 rounded-2xl bg-[#F8F7F0] border border-[#A8C3A0]/40 space-y-1.5">
          <span className="text-[10px] font-bold uppercase text-[#164B3B]">Resource Owner (Lender)</span>
          <p className="font-display font-bold text-sm text-[#202923]">{reqItem.lenderName}</p>
          <p className="text-[#737B75] inline-flex items-center gap-1">
            <MapPin className="w-3.5 h-3.5 text-[#238B68]" />
            {reqItem.campusLocation}
          </p>
        </div>
      </div>

      <div className="grid sm:grid-cols-3 gap-3 text-xs bg-[#F8F7F0] p-4 rounded-2xl">
        <div>
          <span className="text-[#737B75] block">Start Date</span>
          <span className="font-bold text-[#202923]">{reqItem.startDate}</span>
        </div>
        <div>
          <span className="text-[#737B75] block">Return Due Date</span>
          <span className="font-bold text-[#202923]">{reqItem.endDate}</span>
        </div>
        <div>
          <span className="text-[#737B75] block">Return Timeliness</span>
          <span className="font-bold text-[#202923]">
            {reqItem.returnedOnTime === null
              ? 'In Progress / Not Returned Yet'
              : reqItem.returnedOnTime
              ? 'Returned On Time ✓'
              : 'Returned After Due Date (Late)'}
          </span>
        </div>
        {reqItem.handoverConfirmedAt && (
          <div>
            <span className="text-[#737B75] block">Handover Confirmed At</span>
            <span className="font-semibold text-[#202923]">
              {new Date(reqItem.handoverConfirmedAt).toLocaleString()}
            </span>
          </div>
        )}
        {reqItem.returnRequestedAt && (
          <div>
            <span className="text-[#737B75] block">Return Requested At</span>
            <span className="font-semibold text-[#202923]">
              {new Date(reqItem.returnRequestedAt).toLocaleString()}
            </span>
          </div>
        )}
        {reqItem.returnedAt && (
          <div>
            <span className="text-[#737B75] block">Actual Return Confirmed At</span>
            <span className="font-semibold text-[#202923]">
              {new Date(reqItem.returnedAt).toLocaleString()}
            </span>
          </div>
        )}
      </div>

      <div className="p-4 rounded-2xl bg-[#E8F2EA]/70 border border-[#A8C3A0] text-xs space-y-2">
        <div>
          <span className="font-bold text-[#164B3B] block">Academic / Borrowing Purpose:</span>
          <p className="text-[#202923]">{reqItem.purpose}</p>
        </div>
        {reqItem.ownerResponseNote && (
          <div className="pt-2 border-t border-[#A8C3A0]/40">
            <span className="font-bold text-[#238B68] block">Owner Decision Note:</span>
            <p className="text-[#202923]">{reqItem.ownerResponseNote}</p>
          </div>
        )}
      </div>

      {/* Role-Aware Action Controls */}
      <div className="flex flex-wrap items-center justify-between gap-3 pt-2 border-t border-[#E8F2EA]">
        <div className="flex flex-wrap gap-2">
          <Link
            to={isLender ? '/lending' : '/borrowing'}
            className="px-4 py-2 rounded-xl bg-[#F8F7F0] border border-[#A8C3A0] text-xs font-semibold text-[#164B3B]"
          >
            ← Back
          </Link>
          <Link
            to={`/items/${reqItem.itemId}`}
            className="px-4 py-2 rounded-xl bg-[#E8F2EA] text-[#164B3B] text-xs font-semibold"
          >
            View Item Listing
          </Link>
          {reqItem.transactionId && (
            <Link
              to={`/transactions/${reqItem.transactionId}`}
              className="px-4 py-2 rounded-xl bg-[#E8F2EA] text-[#164B3B] text-xs font-semibold"
            >
              Ledger Record #{reqItem.transactionId}
            </Link>
          )}
        </div>

        <div className="flex flex-wrap gap-2">
          {isLender && reqItem.status === 'PENDING' && (
            <>
              <button
                onClick={() => triggerAction('approve', 'Approve Borrow Request')}
                className="px-4 py-2 rounded-xl bg-[#238B68] text-white text-xs font-semibold cursor-pointer"
              >
                Approve Request
              </button>
              <button
                onClick={() => triggerAction('reject', 'Decline Borrow Request')}
                className="px-4 py-2 rounded-xl border border-red-300 text-red-700 text-xs font-semibold cursor-pointer"
              >
                Decline
              </button>
            </>
          )}
          {(isBorrower || isLender) && reqItem.status === 'APPROVED' && (
            <button
              onClick={() => triggerAction('confirm-handover', 'Confirm Physical Handover')}
              className="px-4 py-2 rounded-xl bg-[#164B3B] text-white text-xs font-semibold cursor-pointer"
            >
              Confirm Physical Handover
            </button>
          )}
          {isBorrower && reqItem.status === 'ACTIVE' && (
            <button
              onClick={() => triggerAction('request-return', 'Mark Ready for Return')}
              className="px-4 py-2 rounded-xl bg-[#238B68] text-white text-xs font-semibold cursor-pointer"
            >
              Request Return
            </button>
          )}
          {isLender && (reqItem.status === 'ACTIVE' || reqItem.status === 'RETURN_REQUESTED') && (
            <button
              onClick={() => triggerAction('confirm-return', 'Confirm Item Returned')}
              className="px-4 py-2 rounded-xl bg-[#164B3B] text-white text-xs font-semibold cursor-pointer"
            >
              Confirm Item Returned
            </button>
          )}
          {(isBorrower || isLender) && (reqItem.status === 'PENDING' || reqItem.status === 'APPROVED') && (
            <button
              onClick={() => triggerAction('cancel', 'Cancel Borrow Request')}
              className="px-4 py-2 rounded-xl border border-red-300 text-red-700 text-xs font-semibold cursor-pointer"
            >
              Cancel Request
            </button>
          )}
        </div>
      </div>
    </div>
  );
};

// ============================================================================
// 4. EXCHANGE MARKETPLACE & PROPOSALS PAGE (/exchanges)
// ============================================================================
export const ExchangeMarketplacePage: React.FC = () => {
  const { user, showToast } = useAuth();
  const [exchanges, setExchanges] = useState<ExchangeOfferItem[]>([]);
  const [exchangeItems, setExchangeItems] = useState<ItemListing[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [modal, setModal] = useState<ConfirmModalState | null>(null);

  const load = () => {
    setLoading(true);
    setError(null);
    Promise.all([
      apiClient.get<ExchangeOfferItem[]>('/exchanges'),
      apiClient.get<{ items: ItemListing[] }>('/items', { params: { sharingType: 'EXCHANGE' } }),
    ])
      .then(([exRes, itemRes]) => {
        setExchanges(exRes.data);
        setExchangeItems(itemRes.data.items);
      })
      .catch((err) => setError(extractErrorMessage(err)))
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    load();
  }, []);

  const promptExchangeAction = (
    ex: ExchangeOfferItem,
    action: 'accept' | 'reject' | 'cancel' | 'confirm-handover' | 'complete',
    label: string
  ) => {
    setModal({
      open: true,
      title: `${label} — Exchange #${ex.id}`,
      description: `Confirm "${label}" for swap "${ex.offeredItemTitle}" ⇄ "${ex.requestedItemTitle}". Note: Both Proposer and Receiver must confirm handover before an exchange is completed.`,
      confirmLabel: label,
      danger: action === 'reject' || action === 'cancel',
      onConfirm: async () => {
        try {
          await apiClient.put(`/exchanges/${ex.id}/${action}`);
          showToast(`Exchange updated (${action})!`, 'success');
          load();
        } catch (err) {
          showToast(extractErrorMessage(err), 'error');
        }
      },
    });
  };

  return (
    <div className="space-y-8">
      <ConfirmActionModal modal={modal} onClose={() => setModal(null)} />

      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
        <div>
          <h1 className="font-display text-2xl font-bold text-[#164B3B]">
            Peer-to-Peer Item Exchange Hub
          </h1>
          <p className="text-xs text-[#737B75]">
            Swap resources directly with fellow students with ascending-ID lock safety and dual handover verification
          </p>
        </div>
        <button
          onClick={load}
          className="inline-flex items-center gap-1.5 px-3.5 py-2 rounded-xl bg-white border border-[#A8C3A0] text-xs font-semibold text-[#164B3B] self-start cursor-pointer"
        >
          <RefreshCw className="w-3.5 h-3.5" />
          Refresh
        </button>
      </div>

      {error && (
        <div className="p-4 rounded-2xl bg-red-50 border border-red-200 text-xs text-red-800">
          {error}
        </div>
      )}

      {/* Active Exchange Proposals */}
      <div className="space-y-4">
        <h2 className="font-display text-lg font-bold text-[#202923]">
          My Exchange Proposals ({exchanges.length})
        </h2>
        {exchanges.length === 0 ? (
          <div className="bg-white rounded-2xl p-6 text-xs text-[#737B75] border border-[#A8C3A0]/50">
            No active exchange proposals yet. Click any Exchange listing below to propose a swap!
          </div>
        ) : (
          <div className="space-y-3">
            {exchanges.map((ex) => {
              const isProposer = user?.id === ex.proposerId;
              const isReceiver = user?.id === ex.receiverId;
              const myHandoverConfirmed = isProposer
                ? ex.proposerHandoverConfirmed
                : ex.receiverHandoverConfirmed;

              return (
                <div
                  key={ex.id}
                  className="bg-white rounded-2xl p-5 border border-[#A8C3A0]/50 flex flex-col lg:flex-row lg:items-center justify-between gap-4"
                >
                  <div className="space-y-1.5">
                    <div className="flex flex-wrap items-center gap-2">
                      <span className={`px-2.5 py-0.5 rounded-full text-xs font-bold ${statusBadgeClass(ex.status)}`}>
                        {ex.status}
                      </span>
                      <span className="px-2 py-0.5 rounded-full bg-[#F8F7F0] border border-[#A8C3A0] text-[10px] font-semibold">
                        Proposer Handover: {ex.proposerHandoverConfirmed ? '✓ Confirmed' : 'Pending'}
                      </span>
                      <span className="px-2 py-0.5 rounded-full bg-[#F8F7F0] border border-[#A8C3A0] text-[10px] font-semibold">
                        Receiver Handover: {ex.receiverHandoverConfirmed ? '✓ Confirmed' : 'Pending'}
                      </span>
                      <Link
                        to={`/exchanges/${ex.id}`}
                        className="text-xs font-semibold text-[#238B68] hover:underline"
                      >
                        View Details →
                      </Link>
                    </div>
                    <p className="font-display font-bold text-sm text-[#202923]">
                      {ex.offeredItemTitle} <span className="text-[#238B68]">⇄</span>{' '}
                      {ex.requestedItemTitle}
                    </p>
                    <p className="text-xs text-[#737B75]">
                      Proposer: {ex.proposerName} ({Number(ex.proposerTrustScore).toFixed(0)} TS) •
                      Receiver: {ex.receiverName} ({Number(ex.receiverTrustScore).toFixed(0)} TS)
                    </p>
                    <p className="text-xs text-[#737B75]">&ldquo;{ex.proposalMessage}&rdquo;</p>
                  </div>

                  <div className="flex flex-wrap gap-2 shrink-0">
                    <Link
                      to={`/exchanges/${ex.id}`}
                      className="px-3.5 py-2 rounded-xl bg-[#F8F7F0] border border-[#A8C3A0] text-[#164B3B] text-xs font-semibold"
                    >
                      Details
                    </Link>
                    {ex.status === 'PENDING' && isReceiver && (
                      <>
                        <button
                          onClick={() => promptExchangeAction(ex, 'accept', 'Accept Swap Proposal')}
                          className="px-4 py-2 rounded-xl bg-[#238B68] text-white text-xs font-semibold cursor-pointer"
                        >
                          Accept Swap
                        </button>
                        <button
                          onClick={() => promptExchangeAction(ex, 'reject', 'Decline Swap Proposal')}
                          className="px-4 py-2 rounded-xl border border-red-300 text-red-700 text-xs font-semibold cursor-pointer"
                        >
                          Decline
                        </button>
                      </>
                    )}
                    {ex.status === 'PENDING' && isProposer && (
                      <button
                        onClick={() => promptExchangeAction(ex, 'cancel', 'Cancel Swap Proposal')}
                        className="px-3.5 py-2 rounded-xl border border-red-300 text-red-700 text-xs font-semibold cursor-pointer"
                      >
                        Cancel
                      </button>
                    )}
                    {(ex.status === 'ACCEPTED' || ex.status === 'HANDOVER_PENDING') && (
                      <>
                        {!myHandoverConfirmed && (
                          <button
                            onClick={() => promptExchangeAction(ex, 'confirm-handover', 'Confirm My Handover')}
                            className="px-4 py-2 rounded-xl bg-[#164B3B] text-white text-xs font-semibold cursor-pointer"
                          >
                            Confirm My Handover
                          </button>
                        )}
                        {myHandoverConfirmed && (
                          <span className="px-3 py-2 rounded-xl bg-[#E8F2EA] text-[#164B3B] text-xs font-semibold">
                            Awaiting Partner Handover
                          </span>
                        )}
                      </>
                    )}
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </div>

      {/* Available Exchange Listings */}
      <div className="space-y-4">
        <h2 className="font-display text-lg font-bold text-[#164B3B]">
          Campus Items Open for Exchange
        </h2>
        {loading ? (
          <div className="h-44 bg-white rounded-2xl animate-pulse" />
        ) : (
          <div className="grid sm:grid-cols-2 lg:grid-cols-4 gap-5">
            {exchangeItems.map((it) => (
              <ItemCard key={it.id} item={it} />
            ))}
          </div>
        )}
      </div>
    </div>
  );
};

// ============================================================================
// 5. EXCHANGE DETAILS PAGE (/exchanges/:id)
// ============================================================================
export const ExchangeDetailsPage: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const { user, showToast } = useAuth();
  const [exchange, setExchange] = useState<ExchangeOfferItem | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [modal, setModal] = useState<ConfirmModalState | null>(null);

  const load = () => {
    if (!id) return;
    setLoading(true);
    setError(null);
    apiClient
      .get<ExchangeOfferItem>(`/exchanges/${id}`)
      .then((res) => setExchange(res.data))
      .catch((err) => setError(extractErrorMessage(err)))
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    load();
  }, [id]);

  if (loading) {
    return <div className="h-48 bg-white rounded-3xl animate-pulse" />;
  }

  if (error || !exchange) {
    return (
      <div className="max-w-2xl mx-auto bg-white rounded-3xl p-8 border border-[#A8C3A0]/50 text-center space-y-3">
        <p className="text-sm font-bold text-red-700">{error || 'Exchange offer not found.'}</p>
        <Link to="/exchanges" className="inline-block px-4 py-2 rounded-xl bg-[#164B3B] text-white text-xs font-semibold">
          ← Back to Exchanges
        </Link>
      </div>
    );
  }

  const isProposer = user?.id === exchange.proposerId;
  const isReceiver = user?.id === exchange.receiverId;
  const myHandoverConfirmed = isProposer
    ? exchange.proposerHandoverConfirmed
    : exchange.receiverHandoverConfirmed;

  const triggerExchangeAction = (
    action: 'accept' | 'reject' | 'cancel' | 'confirm-handover' | 'complete',
    label: string
  ) => {
    setModal({
      open: true,
      title: `${label} — Exchange Proposal #${exchange.id}`,
      description: `Confirm "${label}" for swap "${exchange.offeredItemTitle}" ⇄ "${exchange.requestedItemTitle}".`,
      confirmLabel: label,
      danger: action === 'reject' || action === 'cancel',
      onConfirm: async () => {
        try {
          await apiClient.put(`/exchanges/${exchange.id}/${action}`);
          showToast(`Exchange updated (${action})!`, 'success');
          load();
        } catch (err) {
          showToast(extractErrorMessage(err), 'error');
        }
      },
    });
  };

  return (
    <div className="max-w-3xl mx-auto bg-white rounded-3xl p-6 sm:p-8 border border-[#A8C3A0]/50 space-y-6">
      <ConfirmActionModal modal={modal} onClose={() => setModal(null)} />

      <div className="flex items-center justify-between">
        <div>
          <span className="text-xs font-mono font-bold text-[#238B68]">EXCHANGE OFFER #{exchange.id}</span>
          <h1 className="font-display text-2xl font-bold text-[#164B3B]">
            Peer Resource Swap Details
          </h1>
        </div>
        <span className={`px-3 py-1 rounded-full text-xs font-bold ${statusBadgeClass(exchange.status)}`}>
          {exchange.status}
        </span>
      </div>

      <WorkflowTimeline
        steps={['PENDING', 'ACCEPTED', 'HANDOVER_PENDING', 'COMPLETED']}
        currentStatus={exchange.status}
      />

      <div className="grid sm:grid-cols-2 gap-4">
        <div className="p-4 rounded-2xl bg-[#F8F7F0] border border-[#A8C3A0]/40 space-y-2">
          <span className="text-[10px] font-bold uppercase text-[#238B68]">Offered Item (Proposer)</span>
          <Link
            to={`/items/${exchange.offeredItemId}`}
            className="font-display font-bold text-base hover:text-[#164B3B] block"
          >
            {exchange.offeredItemTitle}
          </Link>
          <p className="text-xs text-[#737B75]">
            Owner: <strong>{exchange.proposerName}</strong> ({Number(exchange.proposerTrustScore).toFixed(1)} TS)
          </p>
          <p className="text-xs font-semibold flex items-center gap-1.5">
            {exchange.proposerHandoverConfirmed ? (
              <CheckCircle2 className="w-4 h-4 text-emerald-700" />
            ) : (
              <Clock className="w-4 h-4 text-amber-700" />
            )}
            Handover Confirmed: {exchange.proposerHandoverConfirmed ? 'Yes ✓' : 'Pending'}
          </p>
        </div>

        <div className="p-4 rounded-2xl bg-[#F8F7F0] border border-[#A8C3A0]/40 space-y-2">
          <span className="text-[10px] font-bold uppercase text-[#164B3B]">Requested Item (Receiver)</span>
          <Link
            to={`/items/${exchange.requestedItemId}`}
            className="font-display font-bold text-base hover:text-[#164B3B] block"
          >
            {exchange.requestedItemTitle}
          </Link>
          <p className="text-xs text-[#737B75]">
            Owner: <strong>{exchange.receiverName}</strong> ({Number(exchange.receiverTrustScore).toFixed(1)} TS)
          </p>
          <p className="text-xs font-semibold flex items-center gap-1.5">
            {exchange.receiverHandoverConfirmed ? (
              <CheckCircle2 className="w-4 h-4 text-emerald-700" />
            ) : (
              <Clock className="w-4 h-4 text-amber-700" />
            )}
            Handover Confirmed: {exchange.receiverHandoverConfirmed ? 'Yes ✓' : 'Pending'}
          </p>
        </div>
      </div>

      <div className="p-4 rounded-2xl bg-[#E8F2EA] text-xs space-y-1">
        <span className="font-bold text-[#164B3B] block">Proposal Message:</span>
        <p className="text-[#202923]">{exchange.proposalMessage}</p>
        {exchange.completedAt && (
          <p className="text-[11px] text-[#238B68] font-semibold pt-1">
            Completed At: {new Date(exchange.completedAt).toLocaleString()}
          </p>
        )}
      </div>

      <div className="flex flex-wrap items-center justify-between gap-3 pt-2 border-t border-[#E8F2EA]">
        <div className="flex flex-wrap gap-2">
          <Link
            to="/exchanges"
            className="px-4 py-2 rounded-xl bg-[#F8F7F0] border border-[#A8C3A0] text-[#164B3B] text-xs font-semibold"
          >
            ← Back to Exchanges
          </Link>
          {exchange.transactionId && (
            <Link
              to={`/transactions/${exchange.transactionId}`}
              className="px-4 py-2 rounded-xl bg-[#E8F2EA] text-[#164B3B] text-xs font-semibold"
            >
              Ledger Record #{exchange.transactionId}
            </Link>
          )}
        </div>

        <div className="flex flex-wrap gap-2">
          {exchange.status === 'PENDING' && isReceiver && (
            <>
              <button
                onClick={() => triggerExchangeAction('accept', 'Accept Exchange Proposal')}
                className="px-4 py-2 rounded-xl bg-[#238B68] text-white text-xs font-semibold cursor-pointer"
              >
                Accept Swap
              </button>
              <button
                onClick={() => triggerExchangeAction('reject', 'Decline Exchange Proposal')}
                className="px-4 py-2 rounded-xl border border-red-300 text-red-700 text-xs font-semibold cursor-pointer"
              >
                Decline
              </button>
            </>
          )}
          {(exchange.status === 'PENDING' || exchange.status === 'ACCEPTED') && (isProposer || isReceiver) && (
            <button
              onClick={() => triggerExchangeAction('cancel', 'Cancel Exchange Proposal')}
              className="px-4 py-2 rounded-xl border border-red-300 text-red-700 text-xs font-semibold cursor-pointer"
            >
              Cancel Swap
            </button>
          )}
          {(exchange.status === 'ACCEPTED' || exchange.status === 'HANDOVER_PENDING') &&
            (isProposer || isReceiver) &&
            !myHandoverConfirmed && (
              <button
                onClick={() => triggerExchangeAction('confirm-handover', 'Confirm My Physical Handover')}
                className="px-4 py-2 rounded-xl bg-[#164B3B] text-white text-xs font-semibold cursor-pointer"
              >
                Confirm My Handover
              </button>
            )}
        </div>
      </div>
    </div>
  );
};

// ============================================================================
// 6. RENTAL MANAGEMENT PAGE (/rentals)
// ============================================================================
export const RentalManagementPage: React.FC = () => {
  const { user, showToast } = useAuth();
  const [rentals, setRentals] = useState<RentalItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [statusFilter, setStatusFilter] = useState<string>('ALL');
  const [modal, setModal] = useState<ConfirmModalState | null>(null);

  const load = () => {
    setLoading(true);
    setError(null);
    apiClient
      .get<RentalItem[]>('/rentals')
      .then((res) => setRentals(res.data))
      .catch((err) => setError(extractErrorMessage(err)))
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    load();
  }, []);

  const promptRentalAction = (
    r: RentalItem,
    action: 'approve' | 'reject' | 'cancel' | 'confirm-handover' | 'confirm-return',
    label: string
  ) => {
    setModal({
      open: true,
      title: `${label} — Rental #${r.id}`,
      description: `Confirm "${label}" for "${r.itemTitle}" (${r.startDate} to ${r.endDate}, ${r.rentalDays} days × ₹${Number(r.dailyRate).toFixed(0)}/day = ₹${Number(r.totalRentalAmount).toFixed(0)}). Note: Payment gateway is not integrated; payment status remains ${r.paymentStatus}.`,
      confirmLabel: label,
      danger: action === 'reject' || action === 'cancel',
      onConfirm: async () => {
        try {
          await apiClient.put(`/rentals/${r.id}/${action}`);
          showToast(`Rental updated (${action})!`, 'success');
          load();
        } catch (err) {
          showToast(extractErrorMessage(err), 'error');
        }
      },
    });
  };

  const filtered = rentals.filter((r) => statusFilter === 'ALL' || r.status === statusFilter);

  return (
    <div className="space-y-6">
      <ConfirmActionModal modal={modal} onClose={() => setModal(null)} />

      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
        <div>
          <h1 className="font-display text-2xl font-bold text-[#164B3B]">
            Campus Rental Management
          </h1>
          <p className="text-xs text-[#737B75]">
            Server-verified daily rental calculation with transparent payment status tracking (No Live Payment Gateway Integrated)
          </p>
        </div>
        <span className="px-3.5 py-1.5 rounded-xl bg-amber-50 border border-amber-300 text-amber-900 text-xs font-semibold">
          Payment Gateway: Not Integrated (PENDING / Simulated Demo Only)
        </span>
      </div>

      {/* Status Filter Pills */}
      <div className="flex flex-wrap gap-2">
        {['ALL', 'PENDING', 'APPROVED', 'ACTIVE', 'COMPLETED', 'REJECTED', 'CANCELLED'].map((st) => (
          <button
            key={st}
            onClick={() => setStatusFilter(st)}
            className={`px-3 py-1.5 rounded-xl text-xs font-semibold cursor-pointer ${
              statusFilter === st
                ? 'bg-[#164B3B] text-white'
                : 'bg-white text-[#737B75] border border-[#A8C3A0]/50'
            }`}
          >
            {st}
          </button>
        ))}
      </div>

      {error && (
        <div className="p-4 rounded-2xl bg-red-50 border border-red-200 text-xs text-red-800">
          {error}
        </div>
      )}

      {loading ? (
        <div className="h-44 bg-white rounded-3xl animate-pulse" />
      ) : filtered.length === 0 ? (
        <div className="bg-white rounded-3xl p-10 text-center border border-[#A8C3A0]/50">
          <p className="text-sm font-semibold">No rental transactions matching this filter.</p>
        </div>
      ) : (
        <div className="space-y-4">
          {filtered.map((r) => (
            <div
              key={r.id}
              className="bg-white rounded-2xl p-5 border border-[#A8C3A0]/50 flex flex-col lg:flex-row lg:items-center justify-between gap-4"
            >
              <div className="space-y-1.5">
                <div className="flex flex-wrap items-center gap-2">
                  <span className={`px-2.5 py-0.5 rounded-full text-xs font-bold ${statusBadgeClass(r.status)}`}>
                    {r.status}
                  </span>
                  <span className="px-2.5 py-0.5 rounded-full bg-amber-100 text-amber-900 text-xs font-bold">
                    Payment Status: {r.paymentStatus}
                  </span>
                  <span className="text-xs font-bold text-[#238B68]">
                    {r.rentalDays} Days × ₹{Number(r.dailyRate).toFixed(0)} = ₹
                    {Number(r.totalRentalAmount).toFixed(0)} (Security Deposit Spec: ₹
                    {Number(r.securityDeposit).toFixed(0)})
                  </span>
                </div>
                <Link
                  to={`/rentals/${r.id}`}
                  className="font-display font-bold text-base text-[#202923] hover:text-[#164B3B] block"
                >
                  {r.itemTitle}
                </Link>
                <p className="text-xs text-[#737B75]">
                  Renter: <strong>{r.renterName}</strong> ({Number(r.renterTrustScore).toFixed(1)} TS) • Owner:{' '}
                  <strong>{r.ownerName}</strong> • {r.startDate} to {r.endDate}
                </p>
              </div>

              <div className="flex flex-wrap items-center gap-2 shrink-0">
                <Link
                  to={`/rentals/${r.id}`}
                  className="px-3.5 py-2 rounded-xl bg-[#F8F7F0] border border-[#A8C3A0] text-[#164B3B] text-xs font-semibold"
                >
                  Details
                </Link>
                {r.status === 'PENDING' && user?.id === r.ownerId && (
                  <>
                    <button
                      onClick={() => promptRentalAction(r, 'approve', 'Approve Rental Request')}
                      className="px-4 py-2 rounded-xl bg-[#238B68] text-white text-xs font-semibold cursor-pointer"
                    >
                      Approve Rental
                    </button>
                    <button
                      onClick={() => promptRentalAction(r, 'reject', 'Decline Rental Request')}
                      className="px-4 py-2 rounded-xl border border-red-300 text-red-700 text-xs font-semibold cursor-pointer"
                    >
                      Reject
                    </button>
                  </>
                )}
                {(r.status === 'PENDING' || r.status === 'APPROVED') &&
                  (user?.id === r.renterId || user?.id === r.ownerId) && (
                    <button
                      onClick={() => promptRentalAction(r, 'cancel', 'Cancel Rental Request')}
                      className="px-3.5 py-2 rounded-xl border border-red-300 text-red-700 text-xs font-semibold cursor-pointer"
                    >
                      Cancel
                    </button>
                  )}
                {r.status === 'APPROVED' && (
                  <button
                    onClick={() => promptRentalAction(r, 'confirm-handover', 'Confirm Rental Handover')}
                    className="px-4 py-2 rounded-xl bg-[#164B3B] text-white text-xs font-semibold cursor-pointer"
                  >
                    Confirm Handover
                  </button>
                )}
                {r.status === 'ACTIVE' && user?.id === r.ownerId && (
                  <button
                    onClick={() => promptRentalAction(r, 'confirm-return', 'Confirm Rental Return')}
                    className="px-4 py-2 rounded-xl bg-[#238B68] text-white text-xs font-semibold cursor-pointer"
                  >
                    Confirm Rental Return
                  </button>
                )}
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
};

// ============================================================================
// 7. RENTAL DETAILS PAGE (/rentals/:id)
// ============================================================================
export const RentalDetailsPage: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const { user, showToast } = useAuth();
  const [rental, setRental] = useState<RentalItem | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [modal, setModal] = useState<ConfirmModalState | null>(null);

  const load = () => {
    if (!id) return;
    setLoading(true);
    setError(null);
    apiClient
      .get<RentalItem>(`/rentals/${id}`)
      .then((res) => setRental(res.data))
      .catch((err) => setError(extractErrorMessage(err)))
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    load();
  }, [id]);

  if (loading) {
    return <div className="h-64 bg-white rounded-3xl animate-pulse" />;
  }

  if (error || !rental) {
    return (
      <div className="max-w-2xl mx-auto bg-white rounded-3xl p-8 border border-[#A8C3A0]/50 text-center space-y-3">
        <p className="text-sm font-bold text-red-700">{error || 'Rental record not found.'}</p>
        <Link to="/rentals" className="inline-block px-4 py-2 rounded-xl bg-[#164B3B] text-white text-xs font-semibold">
          ← Back to Rentals
        </Link>
      </div>
    );
  }

  const isOwner = user?.id === rental.ownerId;
  const isRenter = user?.id === rental.renterId;

  const triggerRentalAction = (
    action: 'approve' | 'reject' | 'cancel' | 'confirm-handover' | 'confirm-return',
    label: string
  ) => {
    setModal({
      open: true,
      title: `${label} — Rental #${rental.id}`,
      description: `Confirm "${label}" for "${rental.itemTitle}" (${rental.startDate} → ${rental.endDate}).`,
      confirmLabel: label,
      danger: action === 'reject' || action === 'cancel',
      onConfirm: async () => {
        try {
          await apiClient.put(`/rentals/${rental.id}/${action}`);
          showToast(`Rental updated (${action})!`, 'success');
          load();
        } catch (err) {
          showToast(extractErrorMessage(err), 'error');
        }
      },
    });
  };

  return (
    <div className="max-w-3xl mx-auto bg-white rounded-3xl p-6 sm:p-8 border border-[#A8C3A0]/50 space-y-6">
      <ConfirmActionModal modal={modal} onClose={() => setModal(null)} />

      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <span className="text-xs font-mono font-bold text-[#238B68]">RENTAL BOOKING #{rental.id}</span>
          <h1 className="font-display text-2xl font-bold text-[#164B3B] mt-0.5">{rental.itemTitle}</h1>
        </div>
        <div className="flex items-center gap-2">
          <span className="px-3 py-1 rounded-full bg-amber-100 text-amber-900 text-xs font-bold">
            Payment: {rental.paymentStatus}
          </span>
          <span className={`px-3.5 py-1 rounded-full text-xs font-bold ${statusBadgeClass(rental.status)}`}>
            {rental.status}
          </span>
        </div>
      </div>

      <WorkflowTimeline
        steps={['PENDING', 'APPROVED', 'ACTIVE', 'COMPLETED']}
        currentStatus={rental.status}
      />

      {/* Transparent Payment Integrity Notice */}
      <div className="p-4 rounded-2xl bg-amber-50 border border-amber-300 text-xs text-amber-950 space-y-1">
        <span className="font-bold block">Payment Integrity Disclosure (Academic Demonstration Mode):</span>
        <p>
          No external payment gateway is integrated. Payment status is recorded as{' '}
          <strong>{rental.paymentStatus}</strong>. Approval of a rental request never marks a payment as
          collected or a deposit as refunded without a verified payment event.
        </p>
      </div>

      {/* Backend-Calculated Pricing Breakdown */}
      <div className="grid sm:grid-cols-4 gap-3 text-xs bg-[#E8F2EA] p-4 rounded-2xl border border-[#A8C3A0]">
        <div>
          <span className="text-[#737B75] block">Duration</span>
          <span className="font-bold text-sm text-[#164B3B]">{rental.rentalDays} Days</span>
        </div>
        <div>
          <span className="text-[#737B75] block">Daily Rate</span>
          <span className="font-bold text-sm text-[#164B3B]">₹{Number(rental.dailyRate).toFixed(2)}/day</span>
        </div>
        <div>
          <span className="text-[#737B75] block">Calculated Rental Fee</span>
          <span className="font-bold text-sm text-[#238B68]">₹{Number(rental.totalRentalAmount).toFixed(2)}</span>
        </div>
        <div>
          <span className="text-[#737B75] block">Specified Security Deposit</span>
          <span className="font-bold text-sm text-[#202923]">₹{Number(rental.securityDeposit).toFixed(2)}</span>
        </div>
      </div>

      <div className="grid sm:grid-cols-2 gap-4 text-xs">
        <div className="p-4 rounded-2xl bg-[#F8F7F0] border border-[#A8C3A0]/40 space-y-1">
          <span className="text-[10px] font-bold uppercase text-[#238B68]">Renter</span>
          <p className="font-display font-bold text-sm">{rental.renterName}</p>
          <p className="text-[#737B75]">Trust Score: {Number(rental.renterTrustScore).toFixed(1)} TS</p>
        </div>
        <div className="p-4 rounded-2xl bg-[#F8F7F0] border border-[#A8C3A0]/40 space-y-1">
          <span className="text-[10px] font-bold uppercase text-[#164B3B]">Resource Owner</span>
          <p className="font-display font-bold text-sm">{rental.ownerName}</p>
          <p className="text-[#737B75]">
            Window: {rental.startDate} → {rental.endDate}
          </p>
        </div>
      </div>

      <div className="flex flex-wrap items-center justify-between gap-3 pt-2 border-t border-[#E8F2EA]">
        <div className="flex flex-wrap gap-2">
          <Link
            to="/rentals"
            className="px-4 py-2 rounded-xl bg-[#F8F7F0] border border-[#A8C3A0] text-xs font-semibold text-[#164B3B]"
          >
            ← Back to Rentals
          </Link>
          <Link
            to={`/items/${rental.itemId}`}
            className="px-4 py-2 rounded-xl bg-[#E8F2EA] text-[#164B3B] text-xs font-semibold"
          >
            View Item Listing
          </Link>
          {rental.transactionId && (
            <Link
              to={`/transactions/${rental.transactionId}`}
              className="px-4 py-2 rounded-xl bg-[#E8F2EA] text-[#164B3B] text-xs font-semibold"
            >
              Ledger Record #{rental.transactionId}
            </Link>
          )}
        </div>

        <div className="flex flex-wrap gap-2">
          {isOwner && rental.status === 'PENDING' && (
            <>
              <button
                onClick={() => triggerRentalAction('approve', 'Approve Rental Request')}
                className="px-4 py-2 rounded-xl bg-[#238B68] text-white text-xs font-semibold cursor-pointer"
              >
                Approve Rental
              </button>
              <button
                onClick={() => triggerRentalAction('reject', 'Decline Rental Request')}
                className="px-4 py-2 rounded-xl border border-red-300 text-red-700 text-xs font-semibold cursor-pointer"
              >
                Reject
              </button>
            </>
          )}
          {(isRenter || isOwner) && (rental.status === 'PENDING' || rental.status === 'APPROVED') && (
            <button
              onClick={() => triggerRentalAction('cancel', 'Cancel Rental Request')}
              className="px-4 py-2 rounded-xl border border-red-300 text-red-700 text-xs font-semibold cursor-pointer"
            >
              Cancel Rental
            </button>
          )}
          {(isRenter || isOwner) && rental.status === 'APPROVED' && (
            <button
              onClick={() => triggerRentalAction('confirm-handover', 'Confirm Rental Handover')}
              className="px-4 py-2 rounded-xl bg-[#164B3B] text-white text-xs font-semibold cursor-pointer"
            >
              Confirm Handover
            </button>
          )}
          {isOwner && rental.status === 'ACTIVE' && (
            <button
              onClick={() => triggerRentalAction('confirm-return', 'Confirm Rental Return')}
              className="px-4 py-2 rounded-xl bg-[#238B68] text-white text-xs font-semibold cursor-pointer"
            >
              Confirm Rental Return
            </button>
          )}
        </div>
      </div>
    </div>
  );
};

// ============================================================================
// 8. DONATION MARKETPLACE & 9. DONATION REQUESTS PAGES (/donations & /donations/requests)
// ============================================================================
export const DonationMarketplacePage: React.FC = () => {
  const [items, setItems] = useState<ItemListing[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const load = () => {
    setLoading(true);
    setError(null);
    apiClient
      .get<{ items: ItemListing[] }>('/items', { params: { sharingType: 'DONATE' } })
      .then((res) => setItems(res.data.items))
      .catch((err) => setError(extractErrorMessage(err)))
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    load();
  }, []);

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="font-display text-2xl font-bold text-[#164B3B]">
            Campus Community Donations
          </h1>
          <p className="text-xs text-[#737B75]">
            Zero-cost textbooks, lab coats, and drawing instruments donated by senior students
          </p>
        </div>
        <Link
          to="/donations/requests"
          className="px-4 py-2.5 rounded-xl bg-[#164B3B] text-white text-xs font-semibold self-start"
        >
          Manage Donation Requests →
        </Link>
      </div>

      {error && (
        <div className="p-4 rounded-2xl bg-red-50 border border-red-200 text-xs text-red-800">
          {error}
        </div>
      )}

      {loading ? (
        <div className="h-44 bg-white rounded-3xl animate-pulse" />
      ) : items.length === 0 ? (
        <div className="bg-white rounded-3xl p-10 text-center border border-[#A8C3A0]/50 space-y-2">
          <HandHeart className="w-8 h-8 text-[#238B68] mx-auto" />
          <p className="text-sm font-semibold">No active donation listings available right now.</p>
        </div>
      ) : (
        <div className="grid sm:grid-cols-2 lg:grid-cols-4 gap-5">
          {items.map((it) => (
            <ItemCard key={it.id} item={it} />
          ))}
        </div>
      )}
    </div>
  );
};

export const DonationRequestsPage: React.FC = () => {
  const { user, showToast } = useAuth();
  const [requests, setRequests] = useState<DonationRequestItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [modal, setModal] = useState<ConfirmModalState | null>(null);

  const load = () => {
    setLoading(true);
    setError(null);
    apiClient
      .get<DonationRequestItem[]>('/donations')
      .then((res) => setRequests(res.data))
      .catch((err) => setError(extractErrorMessage(err)))
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    load();
  }, []);

  const promptDonationAction = (
    d: DonationRequestItem,
    action: 'approve' | 'reject' | 'cancel' | 'complete',
    label: string
  ) => {
    setModal({
      open: true,
      title: `${label} — Donation Request #${d.id}`,
      description:
        action === 'approve'
          ? `Select ${d.requesterName} (${Number(d.requesterTrustScore).toFixed(1)} TS) as the sole recipient for "${d.itemTitle}". All other competing pending requests for this item will be automatically declined.`
          : `Confirm "${label}" for donated item "${d.itemTitle}".`,
      confirmLabel: label,
      danger: action === 'reject' || action === 'cancel',
      onConfirm: async () => {
        try {
          await apiClient.put(`/donations/requests/${d.id}/${action}`);
          showToast(`Donation request updated (${action})!`, 'success');
          load();
        } catch (err) {
          showToast(extractErrorMessage(err), 'error');
        }
      },
    });
  };

  return (
    <div className="space-y-6">
      <ConfirmActionModal modal={modal} onClose={() => setModal(null)} />

      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
        <div>
          <h1 className="font-display text-2xl font-bold text-[#164B3B]">
            Donation Requests & Single-Recipient Approvals
          </h1>
          <p className="text-xs text-[#737B75]">
            Approve one recipient per donated resource (competing pending requests are atomically declined)
          </p>
        </div>
        <Link
          to="/donations"
          className="px-4 py-2 rounded-xl bg-[#E8F2EA] text-[#164B3B] text-xs font-semibold self-start"
        >
          ← Back to Donation Marketplace
        </Link>
      </div>

      {error && (
        <div className="p-4 rounded-2xl bg-red-50 border border-red-200 text-xs text-red-800">
          {error}
        </div>
      )}

      {loading ? (
        <div className="h-44 bg-white rounded-3xl animate-pulse" />
      ) : requests.length === 0 ? (
        <div className="bg-white rounded-3xl p-10 text-center border border-[#A8C3A0]/50">
          <p className="text-sm font-semibold">No donation requests recorded yet.</p>
        </div>
      ) : (
        <div className="space-y-4">
          {requests.map((d) => (
            <div
              key={d.id}
              className="bg-white rounded-2xl p-5 border border-[#A8C3A0]/50 flex flex-col sm:flex-row sm:items-center justify-between gap-4"
            >
              <div className="space-y-1.5">
                <div className="flex flex-wrap items-center gap-2">
                  <span className={`px-2.5 py-0.5 rounded-full text-xs font-bold ${statusBadgeClass(d.status)}`}>
                    {d.status}
                  </span>
                  <span className="text-xs font-semibold text-[#238B68]">
                    Requester Trust: {Number(d.requesterTrustScore).toFixed(1)} TS
                  </span>
                </div>
                <Link
                  to={`/items/${d.itemId}`}
                  className="font-display font-bold text-base text-[#202923] hover:text-[#164B3B] block"
                >
                  {d.itemTitle}
                </Link>
                <p className="text-xs text-[#737B75]">
                  Requester: <strong>{d.requesterName}</strong> ({d.requesterDepartment}) • Donor:{' '}
                  <strong>{d.donorName}</strong>
                </p>
                <p className="text-xs text-[#737B75]">&ldquo;{d.motivationMessage}&rdquo;</p>
              </div>

              <div className="flex flex-wrap gap-2 shrink-0">
                {d.status === 'PENDING' && user?.id === d.donorId && (
                  <>
                    <button
                      onClick={() => promptDonationAction(d, 'approve', 'Approve Recipient')}
                      className="px-4 py-2 rounded-xl bg-[#238B68] text-white text-xs font-semibold cursor-pointer"
                    >
                      Approve Recipient
                    </button>
                    <button
                      onClick={() => promptDonationAction(d, 'reject', 'Decline Request')}
                      className="px-4 py-2 rounded-xl border border-red-300 text-red-700 text-xs font-semibold cursor-pointer"
                    >
                      Decline
                    </button>
                  </>
                )}
                {d.status === 'PENDING' && user?.id === d.requesterId && (
                  <button
                    onClick={() => promptDonationAction(d, 'cancel', 'Cancel Donation Request')}
                    className="px-3.5 py-2 rounded-xl border border-red-300 text-red-700 text-xs font-semibold cursor-pointer"
                  >
                    Cancel
                  </button>
                )}
                {d.status === 'APPROVED' && (
                  <button
                    onClick={() => promptDonationAction(d, 'complete', 'Confirm Donation Handover')}
                    className="px-4 py-2 rounded-xl bg-[#164B3B] text-white text-xs font-semibold cursor-pointer"
                  >
                    Confirm Donation Handover
                  </button>
                )}
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
};

// ============================================================================
// 10. TRANSACTION HISTORY & 11. TRANSACTION DETAILS PAGES (/transactions & /transactions/:id)
// ============================================================================
export const TransactionHistoryPage: React.FC = () => {
  const [transactions, setTransactions] = useState<TransactionItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [typeFilter, setTypeFilter] = useState<string>('ALL');
  const [statusFilter, setStatusFilter] = useState<string>('ALL');

  const load = () => {
    setLoading(true);
    setError(null);
    apiClient
      .get<TransactionItem[]>('/transactions')
      .then((res) => setTransactions(res.data))
      .catch((err) => setError(extractErrorMessage(err)))
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    load();
  }, []);

  const filtered = transactions.filter((tx) => {
    const matchType = typeFilter === 'ALL' || tx.transactionType === typeFilter;
    const matchStatus = statusFilter === 'ALL' || tx.status === statusFilter;
    return matchType && matchStatus;
  });

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
        <div>
          <h1 className="font-display text-2xl font-bold text-[#164B3B]">
            Unified Campus Transaction Ledger
          </h1>
          <p className="text-xs text-[#737B75]">
            Synchronized record of all your BORROW, RENT, EXCHANGE, and DONATE activities
          </p>
        </div>
        <button
          onClick={load}
          className="inline-flex items-center gap-1.5 px-3.5 py-2 rounded-xl bg-white border border-[#A8C3A0] text-xs font-semibold text-[#164B3B] self-start cursor-pointer"
        >
          <RefreshCw className="w-3.5 h-3.5" />
          Refresh Ledger
        </button>
      </div>

      {/* Mode & Status Filters */}
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div className="flex flex-wrap gap-1.5">
          {['ALL', 'BORROW', 'RENT', 'EXCHANGE', 'DONATE'].map((t) => (
            <button
              key={t}
              onClick={() => setTypeFilter(t)}
              className={`px-3 py-1.5 rounded-xl text-xs font-semibold cursor-pointer ${
                typeFilter === t
                  ? 'bg-[#164B3B] text-white'
                  : 'bg-white text-[#737B75] border border-[#A8C3A0]/50'
              }`}
            >
              {t}
            </button>
          ))}
        </div>
        <div className="flex flex-wrap gap-1.5">
          {['ALL', 'PENDING', 'APPROVED', 'ACCEPTED', 'ACTIVE', 'COMPLETED', 'REJECTED', 'CANCELLED'].map(
            (s) => (
              <button
                key={s}
                onClick={() => setStatusFilter(s)}
                className={`px-2.5 py-1 rounded-lg text-[11px] font-semibold cursor-pointer ${
                  statusFilter === s
                    ? 'bg-[#238B68] text-white'
                    : 'bg-white text-[#737B75] border border-[#A8C3A0]/40'
                }`}
              >
                {s}
              </button>
            )
          )}
        </div>
      </div>

      {error && (
        <div className="p-4 rounded-2xl bg-red-50 border border-red-200 text-xs text-red-800">
          {error}
        </div>
      )}

      {loading ? (
        <div className="h-48 bg-white rounded-3xl animate-pulse" />
      ) : filtered.length === 0 ? (
        <div className="bg-white rounded-3xl p-10 text-center border border-[#A8C3A0]/50">
          <p className="text-sm font-semibold">No transactions match the selected filters.</p>
        </div>
      ) : (
        <div className="bg-white rounded-3xl border border-[#A8C3A0]/50 overflow-hidden shadow-xs">
          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse text-xs">
              <thead>
                <tr className="bg-[#E8F2EA] text-[#164B3B] font-bold border-b border-[#A8C3A0]/40">
                  <th className="p-4">Code</th>
                  <th className="p-4">Type</th>
                  <th className="p-4">Resource(s)</th>
                  <th className="p-4">Participants</th>
                  <th className="p-4">Dates</th>
                  <th className="p-4">Status</th>
                  <th className="p-4">Amount</th>
                  <th className="p-4">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-[#E8F2EA]">
                {filtered.map((tx) => (
                  <tr key={tx.id} className="hover:bg-[#F8F7F0]">
                    <td className="p-4 font-mono font-bold text-[#164B3B]">{tx.transactionCode}</td>
                    <td className="p-4">
                      <span className="px-2.5 py-0.5 rounded-full bg-[#E8F2EA] text-[#164B3B] font-bold">
                        {tx.transactionType}
                      </span>
                    </td>
                    <td className="p-4 font-semibold text-[#202923]">
                      {tx.primaryItemTitle}
                      {tx.secondaryItemTitle && (
                        <span className="block text-[11px] text-[#238B68]">
                          ⇄ {tx.secondaryItemTitle}
                        </span>
                      )}
                    </td>
                    <td className="p-4 text-[#737B75]">
                      {tx.initiatorUserName} ↔ {tx.counterpartyUserName}
                    </td>
                    <td className="p-4 text-[#737B75]">
                      {tx.startDate || '—'} {tx.dueDate ? `→ ${tx.dueDate}` : ''}
                    </td>
                    <td className="p-4">
                      <span className={`px-2.5 py-0.5 rounded-full font-bold ${statusBadgeClass(tx.status)}`}>
                        {tx.status}
                      </span>
                    </td>
                    <td className="p-4 font-semibold">
                      {Number(tx.monetaryAmount) > 0 ? `₹${tx.monetaryAmount}` : 'Free'}
                    </td>
                    <td className="p-4 flex items-center gap-2">
                      <Link
                        to={`/transactions/${tx.id}`}
                        className="px-3 py-1 rounded-lg bg-[#E8F2EA] text-[#164B3B] font-semibold"
                      >
                        Details
                      </Link>
                      {tx.status === 'COMPLETED' && !tx.reviewedByCurrentUser && (
                        <Link
                          to="/reviews"
                          className="px-3 py-1 rounded-lg bg-[#238B68] text-white font-semibold"
                        >
                          Rate ★
                        </Link>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}
    </div>
  );
};

export const TransactionDetailsPage: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const [tx, setTx] = useState<TransactionItem | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (id) {
      setLoading(true);
      apiClient
        .get<TransactionItem>(`/transactions/${id}`)
        .then((res) => setTx(res.data))
        .catch((err) => setError(extractErrorMessage(err)))
        .finally(() => setLoading(false));
    }
  }, [id]);

  if (loading) {
    return <div className="h-44 bg-white rounded-3xl animate-pulse" />;
  }

  if (error || !tx) {
    return (
      <div className="max-w-2xl mx-auto bg-white rounded-3xl p-8 border border-[#A8C3A0]/50 text-center space-y-3">
        <p className="text-sm font-bold text-red-700">{error || 'Transaction record not found.'}</p>
        <Link to="/transactions" className="inline-block px-4 py-2 rounded-xl bg-[#164B3B] text-white text-xs font-semibold">
          ← Back to Transaction Ledger
        </Link>
      </div>
    );
  }

  const workflowPath =
    tx.transactionType === 'BORROW'
      ? `/borrowing/${tx.referenceId}`
      : tx.transactionType === 'EXCHANGE'
      ? `/exchanges/${tx.referenceId}`
      : tx.transactionType === 'RENT'
      ? `/rentals/${tx.referenceId}`
      : '/donations/requests';

  const stepsByType: Record<string, string[]> = {
    BORROW: ['PENDING', 'APPROVED', 'ACTIVE', 'RETURN_REQUESTED', 'COMPLETED'],
    EXCHANGE: ['PENDING', 'ACCEPTED', 'HANDOVER_PENDING', 'COMPLETED'],
    RENT: ['PENDING', 'APPROVED', 'ACTIVE', 'COMPLETED'],
    DONATE: ['PENDING', 'APPROVED', 'COMPLETED'],
  };

  return (
    <div className="max-w-2xl mx-auto bg-white rounded-3xl p-6 sm:p-8 border border-[#A8C3A0]/50 space-y-5">
      <div className="flex items-center justify-between">
        <div>
          <span className="text-xs font-mono font-bold text-[#238B68]">{tx.transactionCode}</span>
          <h1 className="font-display text-2xl font-bold text-[#164B3B] mt-0.5">
            {tx.primaryItemTitle}
          </h1>
          {tx.secondaryItemTitle && (
            <p className="text-xs font-semibold text-[#238B68] mt-0.5">
              Swapped With: {tx.secondaryItemTitle}
            </p>
          )}
        </div>
        <span className={`px-3 py-1 rounded-full text-xs font-bold ${statusBadgeClass(tx.status)}`}>
          {tx.status}
        </span>
      </div>

      <WorkflowTimeline
        steps={stepsByType[tx.transactionType] || ['PENDING', 'APPROVED', 'COMPLETED']}
        currentStatus={tx.status}
      />

      <div className="grid grid-cols-2 gap-4 text-xs bg-[#F8F7F0] p-4 rounded-2xl">
        <div>
          <span className="text-[#737B75] block">Sharing Mode</span>
          <span className="font-bold text-[#202923]">{tx.transactionType}</span>
        </div>
        <div>
          <span className="text-[#737B75] block">Payment Status</span>
          <span className="font-bold text-[#202923]">{tx.paymentStatus}</span>
        </div>
        <div>
          <span className="text-[#737B75] block">Initiator</span>
          <span className="font-bold text-[#202923]">{tx.initiatorUserName}</span>
        </div>
        <div>
          <span className="text-[#737B75] block">Counterparty</span>
          <span className="font-bold text-[#202923]">{tx.counterpartyUserName}</span>
        </div>
        <div>
          <span className="text-[#737B75] block">Start Date</span>
          <span className="font-bold text-[#202923]">{tx.startDate || 'N/A'}</span>
        </div>
        <div>
          <span className="text-[#737B75] block">Due / Return Date</span>
          <span className="font-bold text-[#202923]">{tx.dueDate || 'N/A'}</span>
        </div>
        {tx.completedAt && (
          <div>
            <span className="text-[#737B75] block">Completed Timestamp</span>
            <span className="font-bold text-[#202923]">
              {new Date(tx.completedAt).toLocaleString()}
            </span>
          </div>
        )}
        {tx.returnedOnTime !== null && (
          <div>
            <span className="text-[#737B75] block">Return Timeliness</span>
            <span className="font-bold text-[#238B68]">
              {tx.returnedOnTime ? 'Returned On Time ✓' : 'Returned Late'}
            </span>
          </div>
        )}
      </div>

      {tx.notes && (
        <div className="p-4 rounded-2xl bg-[#E8F2EA] text-xs">
          <span className="font-bold text-[#164B3B] block mb-1">Transaction Notes:</span>
          <p>{tx.notes}</p>
        </div>
      )}

      <div className="flex flex-wrap items-center justify-between gap-3 pt-2">
        <div className="flex flex-wrap gap-2">
          <Link
            to="/transactions"
            className="px-4 py-2 rounded-xl bg-[#F8F7F0] border border-[#A8C3A0] text-[#164B3B] text-xs font-semibold"
          >
            ← Back to Ledger
          </Link>
          <Link
            to={workflowPath}
            className="px-4 py-2 rounded-xl bg-[#164B3B] text-white text-xs font-semibold"
          >
            Open {tx.transactionType} Workflow Record →
          </Link>
        </div>
        {tx.status === 'COMPLETED' && !tx.reviewedByCurrentUser && (
          <Link
            to="/reviews"
            className="px-4 py-2 rounded-xl bg-[#238B68] text-white text-xs font-semibold"
          >
            Rate Partner ★
          </Link>
        )}
      </div>
    </div>
  );
};
