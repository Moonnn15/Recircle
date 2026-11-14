import React, { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import {
  Clock,
  ShieldCheck,
  ArrowLeftRight,
  Receipt,
  HandHeart,
  FileText,
  CheckCircle2,
  XCircle,
  MapPin,
  Star,
  AlertTriangle,
  ArrowRight,
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
// 1. BORROW REQUESTS PAGE (Borrower View)
// ============================================================================
export const BorrowingPage: React.FC = () => {
  const { showToast } = useAuth();
  const [requests, setRequests] = useState<BorrowRequestItem[]>([]);
  const [loading, setLoading] = useState(true);

  const load = () => {
    setLoading(true);
    apiClient
      .get<BorrowRequestItem[]>('/borrow-requests/my-requests')
      .then((res) => setRequests(res.data))
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    load();
  }, []);

  const handleAction = async (id: number, action: 'confirm-handover' | 'request-return' | 'cancel') => {
    try {
      await apiClient.put(`/borrow-requests/${id}/${action}`);
      showToast(`Borrow request updated (${action})!`, 'success');
      load();
    } catch (err) {
      showToast(extractErrorMessage(err), 'error');
    }
  };

  return (
    <div className="space-y-6">
      <div>
        <h1 className="font-display text-2xl font-bold text-[#164B3B]">My Borrowing Requests</h1>
        <p className="text-xs text-[#737B75]">
          Track resources you have requested to borrow, confirm handovers, and initiate timely returns
        </p>
      </div>

      {loading ? (
        <div className="h-44 bg-white rounded-3xl animate-pulse" />
      ) : requests.length === 0 ? (
        <div className="bg-white rounded-3xl p-10 text-center border border-[#A8C3A0]/50 space-y-3">
          <Clock className="w-8 h-8 text-[#238B68] mx-auto" />
          <p className="font-display text-base font-bold">No borrow requests sent yet</p>
          <Link
            to="/discover"
            className="inline-block px-4 py-2 rounded-xl bg-[#164B3B] text-white text-xs font-semibold"
          >
            Browse Available Items
          </Link>
        </div>
      ) : (
        <div className="space-y-4">
          {requests.map((r) => (
            <div
              key={r.id}
              className="bg-white rounded-2xl p-5 border border-[#A8C3A0]/50 shadow-xs flex flex-col sm:flex-row sm:items-center justify-between gap-4"
            >
              <div className="space-y-1">
                <div className="flex items-center gap-2">
                  <span className="px-2.5 py-0.5 rounded-full bg-[#E8F2EA] text-[#164B3B] text-xs font-bold">
                    {r.status}
                  </span>
                  <span className="text-xs text-[#737B75]">
                    {r.startDate} → {r.endDate}
                  </span>
                </div>
                <Link
                  to={`/items/${r.itemId}`}
                  className="font-display font-bold text-base text-[#202923] hover:text-[#164B3B] block"
                >
                  {r.itemTitle}
                </Link>
                <p className="text-xs text-[#737B75]">
                  Lender: <strong>{r.lenderName}</strong> • Location: {r.campusLocation}
                </p>
                <p className="text-xs text-[#737B75]">Purpose: {r.purpose}</p>
                {r.ownerResponseNote && (
                  <p className="text-xs text-[#238B68] font-medium">
                    Owner Note: &ldquo;{r.ownerResponseNote}&rdquo;
                  </p>
                )}
              </div>

              <div className="flex flex-wrap gap-2 shrink-0">
                {r.status === 'APPROVED' && (
                  <button
                    onClick={() => handleAction(r.id, 'confirm-handover')}
                    className="px-4 py-2 rounded-xl bg-[#164B3B] text-white text-xs font-semibold cursor-pointer"
                  >
                    Confirm Physical Handover
                  </button>
                )}
                {r.status === 'ACTIVE' && (
                  <button
                    onClick={() => handleAction(r.id, 'request-return')}
                    className="px-4 py-2 rounded-xl bg-[#238B68] text-white text-xs font-semibold cursor-pointer"
                  >
                    Mark Ready for Return
                  </button>
                )}
                {(r.status === 'PENDING' || r.status === 'APPROVED') && (
                  <button
                    onClick={() => handleAction(r.id, 'cancel')}
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
// 2. LENDING MANAGEMENT PAGE (Owner View)
// ============================================================================
export const LendingPage: React.FC = () => {
  const { showToast } = useAuth();
  const [requests, setRequests] = useState<BorrowRequestItem[]>([]);
  const [loading, setLoading] = useState(true);

  const load = () => {
    setLoading(true);
    apiClient
      .get<BorrowRequestItem[]>('/borrow-requests/received')
      .then((res) => setRequests(res.data))
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    load();
  }, []);

  const handleDecision = async (
    id: number,
    action: 'approve' | 'reject' | 'confirm-handover' | 'confirm-return'
  ) => {
    try {
      await apiClient.put(`/borrow-requests/${id}/${action}`, {
        note: action === 'approve' ? 'Approved! Meet at campus pickup location.' : undefined,
      });
      showToast(`Lending action (${action}) recorded!`, 'success');
      load();
    } catch (err) {
      showToast(extractErrorMessage(err), 'error');
    }
  };

  return (
    <div className="space-y-6">
      <div>
        <h1 className="font-display text-2xl font-bold text-[#164B3B]">
          Incoming Lending Requests
        </h1>
        <p className="text-xs text-[#737B75]">
          Review borrower Trust Scores, approve non-conflicting dates, and confirm item returns
        </p>
      </div>

      {loading ? (
        <div className="h-44 bg-white rounded-3xl animate-pulse" />
      ) : requests.length === 0 ? (
        <div className="bg-white rounded-3xl p-10 text-center border border-[#A8C3A0]/50">
          <p className="text-sm font-semibold">No incoming borrow requests for your items yet.</p>
        </div>
      ) : (
        <div className="space-y-4">
          {requests.map((r) => (
            <div
              key={r.id}
              className="bg-white rounded-2xl p-5 border border-[#A8C3A0]/50 shadow-xs flex flex-col sm:flex-row sm:items-center justify-between gap-4"
            >
              <div className="space-y-1">
                <div className="flex items-center gap-2">
                  <span className="px-2.5 py-0.5 rounded-full bg-[#164B3B] text-white text-xs font-bold">
                    {r.status}
                  </span>
                  <span className="px-2.5 py-0.5 rounded-full bg-[#E8F2EA] text-[#164B3B] text-xs font-bold">
                    Borrower Trust: {Number(r.borrowerTrustScore).toFixed(1)} TS
                  </span>
                </div>
                <h3 className="font-display font-bold text-base text-[#202923]">{r.itemTitle}</h3>
                <p className="text-xs text-[#737B75]">
                  Requested by <strong>{r.borrowerName}</strong> • {r.startDate} to {r.endDate}
                </p>
                <p className="text-xs text-[#737B75]">Purpose: &ldquo;{r.purpose}&rdquo;</p>
              </div>

              <div className="flex flex-wrap gap-2 shrink-0">
                {r.status === 'PENDING' && (
                  <>
                    <button
                      onClick={() => handleDecision(r.id, 'approve')}
                      className="px-4 py-2 rounded-xl bg-[#238B68] text-white text-xs font-semibold cursor-pointer"
                    >
                      Approve Request
                    </button>
                    <button
                      onClick={() => handleDecision(r.id, 'reject')}
                      className="px-4 py-2 rounded-xl border border-red-300 text-red-700 text-xs font-semibold cursor-pointer"
                    >
                      Decline
                    </button>
                  </>
                )}
                {r.status === 'APPROVED' && (
                  <button
                    onClick={() => handleDecision(r.id, 'confirm-handover')}
                    className="px-4 py-2 rounded-xl bg-[#164B3B] text-white text-xs font-semibold cursor-pointer"
                  >
                    Confirm Handover
                  </button>
                )}
                {(r.status === 'ACTIVE' || r.status === 'RETURN_REQUESTED') && (
                  <button
                    onClick={() => handleDecision(r.id, 'confirm-return')}
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
// 3. EXCHANGE MARKETPLACE & PROPOSALS PAGE
// ============================================================================
export const ExchangeMarketplacePage: React.FC = () => {
  const { user, showToast } = useAuth();
  const [exchanges, setExchanges] = useState<ExchangeOfferItem[]>([]);
  const [exchangeItems, setExchangeItems] = useState<ItemListing[]>([]);
  const [loading, setLoading] = useState(true);

  const load = () => {
    setLoading(true);
    Promise.all([
      apiClient.get<ExchangeOfferItem[]>('/exchanges'),
      apiClient.get<{ items: ItemListing[] }>('/items', { params: { sharingType: 'EXCHANGE' } }),
    ])
      .then(([exRes, itemRes]) => {
        setExchanges(exRes.data);
        setExchangeItems(itemRes.data.items);
      })
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    load();
  }, []);

  const handleExchangeAction = async (
    id: number,
    action: 'accept' | 'reject' | 'confirm-handover' | 'complete'
  ) => {
    try {
      await apiClient.put(`/exchanges/${id}/${action}`);
      showToast(`Exchange updated (${action})!`, 'success');
      load();
    } catch (err) {
      showToast(extractErrorMessage(err), 'error');
    }
  };

  return (
    <div className="space-y-8">
      <div>
        <h1 className="font-display text-2xl font-bold text-[#164B3B]">
          Peer-to-Peer Item Exchange Hub
        </h1>
        <p className="text-xs text-[#737B75]">
          Swap resources directly with fellow students with dual handover verification
        </p>
      </div>

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
            {exchanges.map((ex) => (
              <div
                key={ex.id}
                className="bg-white rounded-2xl p-5 border border-[#A8C3A0]/50 flex flex-col lg:flex-row lg:items-center justify-between gap-4"
              >
                <div className="space-y-1">
                  <div className="flex items-center gap-2">
                    <span className="px-2.5 py-0.5 rounded-full bg-[#E8F2EA] text-[#164B3B] text-xs font-bold">
                      {ex.status}
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
                  {ex.status === 'PENDING' && user?.id === ex.receiverId && (
                    <>
                      <button
                        onClick={() => handleExchangeAction(ex.id, 'accept')}
                        className="px-4 py-2 rounded-xl bg-[#238B68] text-white text-xs font-semibold cursor-pointer"
                      >
                        Accept Swap
                      </button>
                      <button
                        onClick={() => handleExchangeAction(ex.id, 'reject')}
                        className="px-4 py-2 rounded-xl border border-red-300 text-red-700 text-xs font-semibold cursor-pointer"
                      >
                        Decline
                      </button>
                    </>
                  )}
                  {(ex.status === 'ACCEPTED' || ex.status === 'HANDOVER_PENDING') && (
                    <>
                      <button
                        onClick={() => handleExchangeAction(ex.id, 'confirm-handover')}
                        className="px-4 py-2 rounded-xl bg-[#164B3B] text-white text-xs font-semibold cursor-pointer"
                      >
                        Confirm My Handover
                      </button>
                      <button
                        onClick={() => handleExchangeAction(ex.id, 'complete')}
                        className="px-4 py-2 rounded-xl bg-[#238B68] text-white text-xs font-semibold cursor-pointer"
                      >
                        Finalize Exchange
                      </button>
                    </>
                  )}
                </div>
              </div>
            ))}
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
// 4. EXCHANGE DETAILS PAGE
// ============================================================================
export const ExchangeDetailsPage: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const [exchange, setExchange] = useState<ExchangeOfferItem | null>(null);

  useEffect(() => {
    if (id) {
      apiClient.get<ExchangeOfferItem>(`/exchanges/${id}`).then((res) => setExchange(res.data));
    }
  }, [id]);

  if (!exchange) {
    return <div className="h-48 bg-white rounded-3xl animate-pulse" />;
  }

  return (
    <div className="max-w-3xl mx-auto bg-white rounded-3xl p-6 sm:p-8 border border-[#A8C3A0]/50 space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="font-display text-2xl font-bold text-[#164B3B]">
          Exchange Proposal #{exchange.id}
        </h1>
        <span className="px-3 py-1 rounded-full bg-[#164B3B] text-white text-xs font-bold">
          {exchange.status}
        </span>
      </div>

      <div className="grid sm:grid-cols-2 gap-4">
        <div className="p-4 rounded-2xl bg-[#F8F7F0] border border-[#A8C3A0]/40 space-y-2">
          <span className="text-[10px] font-bold uppercase text-[#238B68]">Offered Item</span>
          <p className="font-display font-bold text-base">{exchange.offeredItemTitle}</p>
          <p className="text-xs text-[#737B75]">
            Owner: {exchange.proposerName} ({Number(exchange.proposerTrustScore).toFixed(1)} TS)
          </p>
          <p className="text-xs font-semibold">
            Handover Confirmed: {exchange.proposerHandoverConfirmed ? '✓ Yes' : 'Pending'}
          </p>
        </div>

        <div className="p-4 rounded-2xl bg-[#F8F7F0] border border-[#A8C3A0]/40 space-y-2">
          <span className="text-[10px] font-bold uppercase text-[#164B3B]">Requested Item</span>
          <p className="font-display font-bold text-base">{exchange.requestedItemTitle}</p>
          <p className="text-xs text-[#737B75]">
            Owner: {exchange.receiverName} ({Number(exchange.receiverTrustScore).toFixed(1)} TS)
          </p>
          <p className="text-xs font-semibold">
            Handover Confirmed: {exchange.receiverHandoverConfirmed ? '✓ Yes' : 'Pending'}
          </p>
        </div>
      </div>

      <div className="p-4 rounded-2xl bg-[#E8F2EA] text-xs">
        <span className="font-bold text-[#164B3B] block mb-1">Proposal Message:</span>
        <p>{exchange.proposalMessage}</p>
      </div>

      <Link
        to="/exchanges"
        className="inline-block px-4 py-2 rounded-xl bg-[#164B3B] text-white text-xs font-semibold"
      >
        ← Back to Exchanges
      </Link>
    </div>
  );
};

// ============================================================================
// 5. RENTAL MANAGEMENT PAGE
// ============================================================================
export const RentalManagementPage: React.FC = () => {
  const { user, showToast } = useAuth();
  const [rentals, setRentals] = useState<RentalItem[]>([]);
  const [loading, setLoading] = useState(true);

  const load = () => {
    setLoading(true);
    apiClient
      .get<RentalItem[]>('/rentals')
      .then((res) => setRentals(res.data))
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    load();
  }, []);

  const handleRentalAction = async (
    id: number,
    action: 'approve' | 'reject' | 'confirm-handover' | 'confirm-return'
  ) => {
    try {
      await apiClient.put(`/rentals/${id}/${action}`);
      showToast(`Rental ${action} confirmed!`, 'success');
      load();
    } catch (err) {
      showToast(extractErrorMessage(err), 'error');
    }
  };

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
        <div>
          <h1 className="font-display text-2xl font-bold text-[#164B3B]">
            Campus Rental Management
          </h1>
          <p className="text-xs text-[#737B75]">
            Server-verified daily rental calculation with transparent simulated payment tracking
          </p>
        </div>
        <span className="px-3.5 py-1.5 rounded-xl bg-amber-50 border border-amber-300 text-amber-900 text-xs font-semibold">
          Payment Mode: Simulated Academic Sandbox
        </span>
      </div>

      {loading ? (
        <div className="h-44 bg-white rounded-3xl animate-pulse" />
      ) : rentals.length === 0 ? (
        <div className="bg-white rounded-3xl p-10 text-center border border-[#A8C3A0]/50">
          <p className="text-sm font-semibold">No rental transactions recorded yet.</p>
        </div>
      ) : (
        <div className="space-y-4">
          {rentals.map((r) => (
            <div
              key={r.id}
              className="bg-white rounded-2xl p-5 border border-[#A8C3A0]/50 flex flex-col lg:flex-row lg:items-center justify-between gap-4"
            >
              <div className="space-y-1">
                <div className="flex flex-wrap items-center gap-2">
                  <span className="px-2.5 py-0.5 rounded-full bg-[#164B3B] text-white text-xs font-bold">
                    {r.status}
                  </span>
                  <span className="px-2.5 py-0.5 rounded-full bg-amber-100 text-amber-900 text-xs font-bold">
                    Payment: {r.paymentStatus}
                  </span>
                  <span className="text-xs font-bold text-[#238B68]">
                    {r.rentalDays} Days × ₹{Number(r.dailyRate).toFixed(0)} = ₹
                    {Number(r.totalRentalAmount).toFixed(0)}
                  </span>
                </div>
                <h3 className="font-display font-bold text-base text-[#202923]">{r.itemTitle}</h3>
                <p className="text-xs text-[#737B75]">
                  Renter: <strong>{r.renterName}</strong> • Owner: <strong>{r.ownerName}</strong> •{' '}
                  {r.startDate} to {r.endDate}
                </p>
              </div>

              <div className="flex flex-wrap gap-2 shrink-0">
                {r.status === 'PENDING' && user?.id === r.ownerId && (
                  <>
                    <button
                      onClick={() => handleRentalAction(r.id, 'approve')}
                      className="px-4 py-2 rounded-xl bg-[#238B68] text-white text-xs font-semibold cursor-pointer"
                    >
                      Approve Rental
                    </button>
                    <button
                      onClick={() => handleRentalAction(r.id, 'reject')}
                      className="px-4 py-2 rounded-xl border border-red-300 text-red-700 text-xs font-semibold cursor-pointer"
                    >
                      Reject
                    </button>
                  </>
                )}
                {r.status === 'APPROVED' && (
                  <button
                    onClick={() => handleRentalAction(r.id, 'confirm-handover')}
                    className="px-4 py-2 rounded-xl bg-[#164B3B] text-white text-xs font-semibold cursor-pointer"
                  >
                    Confirm Handover
                  </button>
                )}
                {r.status === 'ACTIVE' && user?.id === r.ownerId && (
                  <button
                    onClick={() => handleRentalAction(r.id, 'confirm-return')}
                    className="px-4 py-2 rounded-xl bg-[#238B68] text-white text-xs font-semibold cursor-pointer"
                  >
                    Confirm Return & Release Deposit
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
// 6. DONATION MARKETPLACE & DONATION REQUESTS PAGES
// ============================================================================
export const DonationMarketplacePage: React.FC = () => {
  const [items, setItems] = useState<ItemListing[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    apiClient
      .get<{ items: ItemListing[] }>('/items', { params: { sharingType: 'DONATE' } })
      .then((res) => setItems(res.data.items))
      .finally(() => setLoading(false));
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

      {loading ? (
        <div className="h-44 bg-white rounded-3xl animate-pulse" />
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

  const load = () => {
    apiClient.get<DonationRequestItem[]>('/donations').then((res) => setRequests(res.data));
  };

  useEffect(() => {
    load();
  }, []);

  const handleDonationAction = async (id: number, action: 'approve' | 'reject' | 'complete') => {
    try {
      await apiClient.put(`/donations/requests/${id}/${action}`);
      showToast(`Donation request ${action}d!`, 'success');
      load();
    } catch (err) {
      showToast(extractErrorMessage(err), 'error');
    }
  };

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="font-display text-2xl font-bold text-[#164B3B]">
            Donation Requests & Recipient Approvals
          </h1>
          <p className="text-xs text-[#737B75]">
            Approve recipients and confirm handover of donated academic items
          </p>
        </div>
        <Link
          to="/donations"
          className="px-4 py-2 rounded-xl bg-[#E8F2EA] text-[#164B3B] text-xs font-semibold"
        >
          ← Back to Donation Marketplace
        </Link>
      </div>

      <div className="space-y-4">
        {requests.map((d) => (
          <div
            key={d.id}
            className="bg-white rounded-2xl p-5 border border-[#A8C3A0]/50 flex flex-col sm:flex-row sm:items-center justify-between gap-4"
          >
            <div className="space-y-1">
              <div className="flex items-center gap-2">
                <span className="px-2.5 py-0.5 rounded-full bg-[#164B3B] text-white text-xs font-bold">
                  {d.status}
                </span>
                <span className="text-xs font-semibold text-[#238B68]">
                  Recipient Trust: {Number(d.requesterTrustScore).toFixed(1)} TS
                </span>
              </div>
              <h3 className="font-display font-bold text-base text-[#202923]">{d.itemTitle}</h3>
              <p className="text-xs text-[#737B75]">
                Requester: <strong>{d.requesterName}</strong> ({d.requesterDepartment}) • Donor:{' '}
                <strong>{d.donorName}</strong>
              </p>
              <p className="text-xs text-[#737B75]">&ldquo;{d.motivationMessage}&rdquo;</p>
            </div>

            <div className="flex gap-2 shrink-0">
              {d.status === 'PENDING' && user?.id === d.donorId && (
                <>
                  <button
                    onClick={() => handleDonationAction(d.id, 'approve')}
                    className="px-4 py-2 rounded-xl bg-[#238B68] text-white text-xs font-semibold cursor-pointer"
                  >
                    Approve Recipient
                  </button>
                  <button
                    onClick={() => handleDonationAction(d.id, 'reject')}
                    className="px-4 py-2 rounded-xl border border-red-300 text-red-700 text-xs font-semibold cursor-pointer"
                  >
                    Decline
                  </button>
                </>
              )}
              {d.status === 'APPROVED' && (
                <button
                  onClick={() => handleDonationAction(d.id, 'complete')}
                  className="px-4 py-2 rounded-xl bg-[#164B3B] text-white text-xs font-semibold cursor-pointer"
                >
                  Confirm Donation Handover
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
// 7. TRANSACTION HISTORY & TRANSACTION DETAILS PAGES
// ============================================================================
export const TransactionHistoryPage: React.FC = () => {
  const [transactions, setTransactions] = useState<TransactionItem[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    apiClient
      .get<TransactionItem[]>('/transactions')
      .then((res) => setTransactions(res.data))
      .finally(() => setLoading(false));
  }, []);

  return (
    <div className="space-y-6">
      <div>
        <h1 className="font-display text-2xl font-bold text-[#164B3B]">
          Unified Campus Transaction Ledger
        </h1>
        <p className="text-xs text-[#737B75]">
          Immutable record of all your borrowing, lending, rental, exchange, and donation activities
        </p>
      </div>

      {loading ? (
        <div className="h-48 bg-white rounded-3xl animate-pulse" />
      ) : (
        <div className="bg-white rounded-3xl border border-[#A8C3A0]/50 overflow-hidden shadow-xs">
          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse text-xs">
              <thead>
                <tr className="bg-[#E8F2EA] text-[#164B3B] font-bold border-b border-[#A8C3A0]/40">
                  <th className="p-4">Code</th>
                  <th className="p-4">Type</th>
                  <th className="p-4">Resource</th>
                  <th className="p-4">Participants</th>
                  <th className="p-4">Status</th>
                  <th className="p-4">Amount</th>
                  <th className="p-4">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-[#E8F2EA]">
                {transactions.map((tx) => (
                  <tr key={tx.id} className="hover:bg-[#F8F7F0]">
                    <td className="p-4 font-mono font-bold text-[#164B3B]">{tx.transactionCode}</td>
                    <td className="p-4">
                      <span className="px-2.5 py-0.5 rounded-full bg-[#E8F2EA] text-[#164B3B] font-bold">
                        {tx.transactionType}
                      </span>
                    </td>
                    <td className="p-4 font-semibold text-[#202923]">{tx.primaryItemTitle}</td>
                    <td className="p-4 text-[#737B75]">
                      {tx.initiatorUserName} ↔ {tx.counterpartyUserName}
                    </td>
                    <td className="p-4">
                      <span className="px-2.5 py-0.5 rounded-full bg-[#164B3B] text-white font-bold">
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

  useEffect(() => {
    if (id) {
      apiClient.get<TransactionItem>(`/transactions/${id}`).then((res) => setTx(res.data));
    }
  }, [id]);

  if (!tx) {
    return <div className="h-44 bg-white rounded-3xl animate-pulse" />;
  }

  return (
    <div className="max-w-2xl mx-auto bg-white rounded-3xl p-6 sm:p-8 border border-[#A8C3A0]/50 space-y-5">
      <div className="flex items-center justify-between">
        <div>
          <span className="text-xs font-mono font-bold text-[#238B68]">{tx.transactionCode}</span>
          <h1 className="font-display text-2xl font-bold text-[#164B3B] mt-0.5">
            {tx.primaryItemTitle}
          </h1>
        </div>
        <span className="px-3 py-1 rounded-full bg-[#164B3B] text-white text-xs font-bold">
          {tx.status}
        </span>
      </div>

      <div className="grid grid-cols-2 gap-4 text-xs bg-[#F8F7F0] p-4 rounded-2xl">
        <div>
          <span className="text-[#737B75] block">Sharing Mode</span>
          <span className="font-bold text-[#202923]">{tx.transactionType}</span>
        </div>
        <div>
          <span className="text-[#737B75] block">Payment Simulation Status</span>
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
      </div>

      {tx.notes && (
        <div className="p-4 rounded-2xl bg-[#E8F2EA] text-xs">
          <span className="font-bold text-[#164B3B] block mb-1">Transaction Notes:</span>
          <p>{tx.notes}</p>
        </div>
      )}

      <div className="flex justify-between pt-3">
        <Link
          to="/transactions"
          className="px-4 py-2 rounded-xl bg-[#E8F2EA] text-[#164B3B] text-xs font-semibold"
        >
          ← Back to Ledger
        </Link>
        <Link
          to="/disputes"
          className="px-4 py-2 rounded-xl border border-amber-400 text-amber-900 text-xs font-semibold"
        >
          Report Issue / Dispute
        </Link>
      </div>
    </div>
  );
};
