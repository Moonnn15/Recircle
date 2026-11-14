import React, { useEffect, useState, useCallback } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import {
  Search,
  Filter,
  RotateCcw,
  PlusCircle,
  MapPin,
  ShieldCheck,
  Star,
  Calendar,
  Edit3,
  Trash2,
  Upload,
  CheckCircle2,
  ArrowLeft,
  Clock,
  ArrowLeftRight,
  Receipt,
  HandHeart,
} from 'lucide-react';
import { apiClient, extractErrorMessage } from '../../api/client';
import { Category, ItemListing, ReviewItem } from '../../types';
import { ItemCard } from '../../components/cards/ItemCard';
import { useAuth } from '../../context/AuthContext';

// ============================================================================
// 1. DISCOVER ITEMS PAGE (Search, Filter, Sort, Pagination, Empty State)
// ============================================================================
export const DiscoverItemsPage: React.FC = () => {
  const [categories, setCategories] = useState<Category[]>([]);
  const [items, setItems] = useState<ItemListing[]>([]);
  const [totalElements, setTotalElements] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [loading, setLoading] = useState(true);

  const [query, setQuery] = useState('');
  const [categoryId, setCategoryId] = useState<string>('');
  const [department, setDepartment] = useState('ALL');
  const [sharingType, setSharingType] = useState('ALL');
  const [condition, setCondition] = useState('ALL');
  const [availability, setAvailability] = useState('ALL');
  const [maxPrice, setMaxPrice] = useState<string>('');
  const [sortBy, setSortBy] = useState('newest');
  const [page, setPage] = useState(0);

  useEffect(() => {
    apiClient.get<Category[]>('/categories').then((res) => setCategories(res.data));
  }, []);

  const fetchItems = useCallback(async () => {
    setLoading(true);
    try {
      const params: Record<string, string | number> = {
        sortBy,
        page,
        size: 8,
      };
      if (query.trim()) params.query = query.trim();
      if (categoryId) params.categoryId = Number(categoryId);
      if (department !== 'ALL') params.department = department;
      if (sharingType !== 'ALL') params.sharingType = sharingType;
      if (condition !== 'ALL') params.condition = condition;
      if (availability !== 'ALL') params.availability = availability;
      if (maxPrice) params.maxPrice = Number(maxPrice);

      const res = await apiClient.get<{
        items: ItemListing[];
        totalElements: number;
        totalPages: number;
      }>('/items', { params });

      setItems(res.data.items);
      setTotalElements(res.data.totalElements);
      setTotalPages(res.data.totalPages);
    } finally {
      setLoading(false);
    }
  }, [query, categoryId, department, sharingType, condition, availability, maxPrice, sortBy, page]);

  useEffect(() => {
    fetchItems();
  }, [fetchItems]);

  const clearFilters = () => {
    setQuery('');
    setCategoryId('');
    setDepartment('ALL');
    setSharingType('ALL');
    setCondition('ALL');
    setAvailability('ALL');
    setMaxPrice('');
    setSortBy('newest');
    setPage(0);
  };

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="font-display text-2xl sm:text-3xl font-bold text-[#164B3B]">
            Discover Campus Resources
          </h1>
          <p className="text-xs sm:text-sm text-[#737B75]">
            Browse {totalElements} verified textbooks, calculators, bicycles, lab kits, and instruments across SRM
          </p>
        </div>
        <Link
          to="/listings/create"
          className="inline-flex items-center gap-2 px-4 py-2.5 rounded-xl bg-[#164B3B] hover:bg-[#238B68] text-white text-xs font-semibold shadow-xs transition-colors self-start"
        >
          <PlusCircle className="w-4 h-4" />
          Publish Item Listing
        </Link>
      </div>

      {/* Search & Multi-Facet Filter Bar */}
      <div className="bg-white rounded-3xl p-5 border border-[#A8C3A0]/50 shadow-xs space-y-4">
        <div className="grid sm:grid-cols-12 gap-3">
          <div className="sm:col-span-5 relative">
            <Search className="w-4 h-4 text-[#737B75] absolute left-3.5 top-3" />
            <input
              type="text"
              value={query}
              onChange={(e) => {
                setQuery(e.target.value);
                setPage(0);
              }}
              placeholder="Search by item title, description, or campus spot..."
              className="w-full pl-10 pr-4 py-2 rounded-xl border border-[#A8C3A0] text-xs focus:outline-none focus:border-[#164B3B]"
            />
          </div>

          <div className="sm:col-span-3">
            <select
              value={categoryId}
              onChange={(e) => {
                setCategoryId(e.target.value);
                setPage(0);
              }}
              className="w-full px-3 py-2 rounded-xl border border-[#A8C3A0] text-xs bg-white"
            >
              <option value="">All 10 Campus Categories</option>
              {categories.map((c) => (
                <option key={c.id} value={c.id}>
                  {c.name}
                </option>
              ))}
            </select>
          </div>

          <div className="sm:col-span-2">
            <select
              value={sharingType}
              onChange={(e) => {
                setSharingType(e.target.value);
                setPage(0);
              }}
              className="w-full px-3 py-2 rounded-xl border border-[#A8C3A0] text-xs bg-white"
            >
              <option value="ALL">All Sharing Types</option>
              <option value="BORROW">Borrow</option>
              <option value="RENT">Rent</option>
              <option value="EXCHANGE">Exchange</option>
              <option value="DONATE">Donate</option>
            </select>
          </div>

          <div className="sm:col-span-2">
            <select
              value={sortBy}
              onChange={(e) => setSortBy(e.target.value)}
              className="w-full px-3 py-2 rounded-xl border border-[#A8C3A0] text-xs bg-white font-semibold text-[#164B3B]"
            >
              <option value="newest">Sort: Newest</option>
              <option value="oldest">Sort: Oldest</option>
              <option value="trust_score">Sort: Highest Trust Score</option>
              <option value="rating">Sort: Highest Owner Rating</option>
              <option value="price_asc">Sort: Lowest Rental Price</option>
            </select>
          </div>
        </div>

        <div className="grid grid-cols-2 sm:grid-cols-5 gap-3 pt-2 border-t border-[#E8F2EA]">
          <select
            value={department}
            onChange={(e) => {
              setDepartment(e.target.value);
              setPage(0);
            }}
            className="px-3 py-1.5 rounded-xl border border-[#A8C3A0]/60 text-xs bg-[#F8F7F0]"
          >
            <option value="ALL">All Departments</option>
            <option value="Computer Science">Computer Science (CSE)</option>
            <option value="Electronics">Electronics (ECE)</option>
            <option value="Mechanical">Mechanical</option>
            <option value="Biotechnology">Biotechnology</option>
          </select>

          <select
            value={condition}
            onChange={(e) => {
              setCondition(e.target.value);
              setPage(0);
            }}
            className="px-3 py-1.5 rounded-xl border border-[#A8C3A0]/60 text-xs bg-[#F8F7F0]"
          >
            <option value="ALL">All Conditions</option>
            <option value="NEW">New</option>
            <option value="LIKE_NEW">Like New</option>
            <option value="GOOD">Good</option>
            <option value="FAIR">Fair</option>
          </select>

          <select
            value={availability}
            onChange={(e) => {
              setAvailability(e.target.value);
              setPage(0);
            }}
            className="px-3 py-1.5 rounded-xl border border-[#A8C3A0]/60 text-xs bg-[#F8F7F0]"
          >
            <option value="ALL">All Availability</option>
            <option value="AVAILABLE">Available Now</option>
            <option value="RESERVED">Reserved</option>
          </select>

          <input
            type="number"
            min="0"
            value={maxPrice}
            onChange={(e) => {
              setMaxPrice(e.target.value);
              setPage(0);
            }}
            placeholder="Max Price ₹/day"
            className="px-3 py-1.5 rounded-xl border border-[#A8C3A0]/60 text-xs bg-[#F8F7F0]"
          />

          <button
            type="button"
            onClick={clearFilters}
            className="inline-flex items-center justify-center gap-1.5 px-3 py-1.5 rounded-xl bg-[#E8F2EA] hover:bg-[#A8C3A0]/40 text-[#164B3B] text-xs font-semibold cursor-pointer"
          >
            <RotateCcw className="w-3.5 h-3.5" />
            Clear Filters
          </button>
        </div>
      </div>

      {/* Results Grid */}
      {loading ? (
        <div className="grid sm:grid-cols-2 lg:grid-cols-4 gap-5">
          {[1, 2, 3, 4].map((n) => (
            <div key={n} className="h-72 bg-white rounded-2xl border border-[#A8C3A0]/40 animate-pulse" />
          ))}
        </div>
      ) : items.length === 0 ? (
        <div className="bg-white rounded-3xl p-12 text-center border border-[#A8C3A0]/50 space-y-3">
          <Filter className="w-10 h-10 text-[#238B68] mx-auto" />
          <h3 className="font-display text-lg font-bold text-[#202923]">
            No campus resources match your current filters
          </h3>
          <p className="text-xs text-[#737B75] max-w-md mx-auto">
            Try clearing your search query or switching to a broader category.
          </p>
          <button
            onClick={clearFilters}
            className="px-4 py-2 rounded-xl bg-[#164B3B] text-white text-xs font-semibold cursor-pointer"
          >
            Reset All Filters
          </button>
        </div>
      ) : (
        <>
          <div className="grid sm:grid-cols-2 lg:grid-cols-4 gap-5">
            {items.map((item) => (
              <ItemCard key={item.id} item={item} />
            ))}
          </div>

          {/* Pagination Controls */}
          {totalPages > 1 && (
            <div className="flex items-center justify-center gap-2 pt-4">
              <button
                disabled={page === 0}
                onClick={() => setPage((p) => Math.max(0, p - 1))}
                className="px-3.5 py-1.5 rounded-xl bg-white border border-[#A8C3A0] text-xs font-semibold disabled:opacity-40 cursor-pointer"
              >
                Previous
              </button>
              <span className="text-xs font-semibold text-[#164B3B] px-3">
                Page {page + 1} of {totalPages}
              </span>
              <button
                disabled={page + 1 >= totalPages}
                onClick={() => setPage((p) => p + 1)}
                className="px-3.5 py-1.5 rounded-xl bg-white border border-[#A8C3A0] text-xs font-semibold disabled:opacity-40 cursor-pointer"
              >
                Next
              </button>
            </div>
          )}
        </>
      )}
    </div>
  );
};

// ============================================================================
// 2. ITEM DETAILS PAGE (Gallery, Owner Trust, Action Workflows, Reviews)
// ============================================================================
export const ItemDetailsPage: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const { user, showToast } = useAuth();
  const navigate = useNavigate();

  const [item, setItem] = useState<ItemListing | null>(null);
  const [relatedItems, setRelatedItems] = useState<ItemListing[]>([]);
  const [reviews, setReviews] = useState<ReviewItem[]>([]);
  const [reservedDateRanges, setReservedDateRanges] = useState<
    Array<{ bookingId: number; type: string; status: string; startDate: string; endDate: string }>
  >([]);
  const [myItems, setMyItems] = useState<ItemListing[]>([]);
  const [loading, setLoading] = useState(true);

  // Action Form States
  const todayStr = new Date().toISOString().split('T')[0];
  const fourDaysLaterStr = new Date(Date.now() + 4 * 86400000).toISOString().split('T')[0];

  const [startDate, setStartDate] = useState(todayStr);
  const [endDate, setEndDate] = useState(fourDaysLaterStr);
  const [purpose, setPurpose] = useState('');
  const [offeredItemId, setOfferedItemId] = useState<string>('');
  const [simulatePayment, setSimulatePayment] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [datesAvailable, setDatesAvailable] = useState<boolean>(true);
  const [dateValidationMsg, setDateValidationMsg] = useState<string | null>(null);
  const [backendQuote, setBackendQuote] = useState<{
    rentalDays: number;
    dailyRate: number;
    totalRentalAmount: number;
    securityDeposit: number;
    availableForDates: boolean;
  } | null>(null);

  useEffect(() => {
    if (!id) return;
    setLoading(true);
    Promise.all([
      apiClient.get<{
        item: ItemListing;
        relatedItems: ItemListing[];
        reviews: ReviewItem[];
        reservedDateRanges?: Array<{
          bookingId: number;
          type: string;
          status: string;
          startDate: string;
          endDate: string;
        }>;
      }>(`/items/${id}`),
      user ? apiClient.get<ItemListing[]>('/items/my-listings') : Promise.resolve({ data: [] }),
    ])
      .then(([detailRes, myRes]) => {
        setItem(detailRes.data.item);
        setRelatedItems(detailRes.data.relatedItems || []);
        setReviews(detailRes.data.reviews || []);
        setReservedDateRanges(detailRes.data.reservedDateRanges || []);
        setMyItems(myRes.data || []);
        if (myRes.data && myRes.data.length > 0) {
          setOfferedItemId(String(myRes.data[0].id));
        }
      })
      .finally(() => setLoading(false));
  }, [id, user]);

  useEffect(() => {
    if (!item || !startDate || !endDate) return;
    if (new Date(endDate) < new Date(startDate)) {
      setDatesAvailable(false);
      setDateValidationMsg('End date cannot be earlier than start date.');
      return;
    }
    const requestedBorrowDays =
      Math.round((new Date(endDate).getTime() - new Date(startDate).getTime()) / 86400000) + 1;
    if (item.sharingType === 'BORROW' && requestedBorrowDays > item.maxBorrowDays) {
      setDatesAvailable(false);
      setDateValidationMsg(
        `Selected range (${requestedBorrowDays} days) exceeds owner's maximum limit of ${item.maxBorrowDays} days.`
      );
      return;
    }

    if (item.sharingType === 'BORROW' || item.sharingType === 'RENT') {
      apiClient
        .get<{ availableForRequestedDates: boolean }>(`/items/${item.id}/availability`, {
          params: { startDate, endDate },
        })
        .then((res) => {
          setDatesAvailable(res.data.availableForRequestedDates);
          setDateValidationMsg(
            res.data.availableForRequestedDates
              ? null
              : 'This item already has an approved or active reservation overlapping the selected dates.'
          );
        })
        .catch(() => {
          setDatesAvailable(true);
          setDateValidationMsg(null);
        });
    }

    if (item.sharingType === 'RENT') {
      apiClient
        .post<{
          rentalDays: number;
          dailyRate: number;
          totalRentalAmount: number;
          securityDeposit: number;
          availableForDates: boolean;
        }>('/rentals/calculate', { itemId: item.id, startDate, endDate })
        .then((res) => setBackendQuote(res.data))
        .catch(() => setBackendQuote(null));
    }
  }, [item, startDate, endDate]);

  if (loading || !item) {
    return <div className="h-96 bg-white rounded-3xl animate-pulse border border-[#A8C3A0]/40" />;
  }

  const isOwner = user?.id === item.ownerId;
  const diffDays = backendQuote
    ? backendQuote.rentalDays
    : Math.max(1, Math.round((new Date(endDate).getTime() - new Date(startDate).getTime()) / 86400000));
  const estimatedRentalCost = backendQuote
    ? Number(backendQuote.totalRentalAmount)
    : diffDays * Number(item.rentalPricePerDay || 50);

  const handleBorrowSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setSubmitting(true);
    try {
      await apiClient.post('/borrow-requests', {
        itemId: item.id,
        startDate,
        endDate,
        purpose: purpose || 'Required for semester coursework and lab preparation.',
      });
      showToast('Borrow request sent to owner!', 'success');
      navigate('/borrowing');
    } catch (err) {
      showToast(extractErrorMessage(err), 'error');
    } finally {
      setSubmitting(false);
    }
  };

  const handleRentalSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setSubmitting(true);
    try {
      await apiClient.post('/rentals', {
        itemId: item.id,
        startDate,
        endDate,
        simulateImmediatePayment: simulatePayment,
      });
      showToast(
        simulatePayment
          ? `Rental request submitted! Simulated demo total: ₹${estimatedRentalCost}`
          : `Rental request submitted (Payment Status: PENDING). Total: ₹${estimatedRentalCost}`,
        'success'
      );
      navigate('/rentals');
    } catch (err) {
      showToast(extractErrorMessage(err), 'error');
    } finally {
      setSubmitting(false);
    }
  };

  const handleExchangeSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!offeredItemId) {
      showToast('Please select one of your own listings to offer in exchange.', 'error');
      return;
    }
    setSubmitting(true);
    try {
      await apiClient.post('/exchanges', {
        requestedItemId: item.id,
        offeredItemId: Number(offeredItemId),
        proposalMessage: purpose || 'Interested in swapping resources at campus pickup point!',
      });
      showToast('Exchange proposal sent!', 'success');
      navigate('/exchanges');
    } catch (err) {
      showToast(extractErrorMessage(err), 'error');
    } finally {
      setSubmitting(false);
    }
  };

  const handleDonationSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setSubmitting(true);
    try {
      await apiClient.post(`/donations/${item.id}/request`, {
        itemId: item.id,
        motivationMessage: purpose || 'This donated resource will greatly help my semester lab work.',
      });
      showToast('Donation request submitted to donor!', 'success');
      navigate('/donations/requests');
    } catch (err) {
      showToast(extractErrorMessage(err), 'error');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="space-y-8">
      <Link
        to="/discover"
        className="inline-flex items-center gap-1.5 text-xs font-semibold text-[#164B3B] hover:text-[#238B68]"
      >
        <ArrowLeft className="w-4 h-4" />
        Back to Campus Marketplace
      </Link>

      <div className="grid lg:grid-cols-12 gap-8">
        {/* Left Column: Image Gallery & Full Specifications */}
        <div className="lg:col-span-7 space-y-6">
          <div className="bg-white rounded-3xl overflow-hidden border border-[#A8C3A0]/50 shadow-xs">
            <img
              src={item.primaryImageUrl}
              alt={item.title}
              className="w-full h-80 sm:h-96 object-cover"
            />
          </div>

          <div className="bg-white rounded-3xl p-6 border border-[#A8C3A0]/50 space-y-4">
            <div className="flex flex-wrap items-center gap-2">
              <span className="px-3 py-1 rounded-full bg-[#E8F2EA] text-[#164B3B] text-xs font-bold">
                {item.categoryName}
              </span>
              <span className="px-3 py-1 rounded-full bg-[#164B3B] text-white text-xs font-bold">
                {item.sharingType}
              </span>
              <span className="px-3 py-1 rounded-full bg-[#F8F7F0] border border-[#A8C3A0] text-xs font-semibold">
                Condition: {item.itemCondition.replace('_', ' ')}
              </span>
            </div>

            <h1 className="font-display text-2xl sm:text-3xl font-bold text-[#202923]">
              {item.title}
            </h1>
            <p className="text-sm text-[#737B75] leading-relaxed">{item.description}</p>

            <div className="grid sm:grid-cols-2 gap-4 pt-4 border-t border-[#E8F2EA] text-xs">
              <div className="p-3.5 rounded-2xl bg-[#F8F7F0]">
                <span className="text-[#737B75] block">Campus Handover Location</span>
                <span className="font-bold text-[#202923] inline-flex items-center gap-1 mt-0.5">
                  <MapPin className="w-3.5 h-3.5 text-[#238B68]" />
                  {item.campusLocation}
                </span>
              </div>
              <div className="p-3.5 rounded-2xl bg-[#F8F7F0]">
                <span className="text-[#737B75] block">Maximum Duration / Window</span>
                <span className="font-bold text-[#202923] inline-flex items-center gap-1 mt-0.5">
                  <Calendar className="w-3.5 h-3.5 text-[#238B68]" />
                  Up to {item.maxBorrowDays} days
                </span>
              </div>
            </div>

            {item.usageRules && (
              <div className="p-4 rounded-2xl bg-[#E8F2EA]/60 border border-[#A8C3A0] text-xs">
                <span className="font-bold text-[#164B3B] block mb-1">Owner Sharing Rules:</span>
                <p className="text-[#202923]">{item.usageRules}</p>
              </div>
            )}

            {item.exchangePreferences && (
              <div className="p-4 rounded-2xl bg-teal-50 border border-teal-200 text-xs">
                <span className="font-bold text-teal-950 block mb-1">Exchange Preferences:</span>
                <p className="text-teal-900">{item.exchangePreferences}</p>
              </div>
            )}
          </div>
        </div>

        {/* Right Column: Owner Trust Card & Transaction Request Box */}
        <div className="lg:col-span-5 space-y-6">
          {/* Owner Verification & Trust Score Card */}
          <div className="bg-white rounded-3xl p-6 border border-[#A8C3A0]/50 shadow-xs space-y-4">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-3">
                <div className="w-12 h-12 rounded-2xl bg-[#164B3B] text-white font-display font-bold text-lg flex items-center justify-center">
                  {item.ownerName.charAt(0)}
                </div>
                <div>
                  <div className="flex items-center gap-1.5">
                    <h3 className="font-display font-bold text-base text-[#202923]">
                      {item.ownerName}
                    </h3>
                    {item.ownerVerified && (
                      <ShieldCheck className="w-4 h-4 text-[#238B68]" title="Verified Campus Account" />
                    )}
                  </div>
                  <p className="text-xs text-[#737B75]">{item.ownerDepartment}</p>
                </div>
              </div>

              <div className="text-right">
                <span className="inline-flex items-center gap-1 px-3 py-1 rounded-xl bg-[#164B3B] text-white text-xs font-bold">
                  <Star className="w-3.5 h-3.5 fill-[#A8C3A0] text-[#A8C3A0]" />
                  {Number(item.ownerTrustScore).toFixed(1)} TS
                </span>
                <p className="text-[10px] font-bold text-[#238B68] mt-1">{item.ownerTrustLevel}</p>
              </div>
            </div>
          </div>

          {/* Interactive Action Panel */}
          <div className="bg-white rounded-3xl p-6 border border-[#A8C3A0]/50 shadow-sm space-y-4">
            {reservedDateRanges.length > 0 && (
              <div className="p-3.5 rounded-2xl bg-[#F8F7F0] border border-[#A8C3A0] text-xs space-y-1.5">
                <span className="font-bold text-[#164B3B] block">
                  Reserved Date Windows (Non-Overlapping Dates Remain Available):
                </span>
                {reservedDateRanges.map((r) => (
                  <div key={`${r.type}-${r.bookingId}`} className="flex items-center justify-between text-[#202923]">
                    <span>
                      {r.startDate} → {r.endDate}
                    </span>
                    <span className="px-2 py-0.5 rounded-full bg-[#E8F2EA] text-[#164B3B] font-semibold text-[10px]">
                      {r.type} ({r.status})
                    </span>
                  </div>
                ))}
              </div>
            )}
            {isOwner ? (
              <div className="space-y-3">
                <h3 className="font-display font-bold text-lg text-[#164B3B]">
                  You Own This Listing
                </h3>
                <p className="text-xs text-[#737B75]">
                  You can update availability, edit details, or manage incoming requests from your dashboard.
                </p>
                <div className="flex gap-3">
                  <Link
                    to={`/listings/edit/${item.id}`}
                    className="flex-1 py-2.5 px-4 rounded-xl bg-[#164B3B] text-white text-xs font-semibold text-center"
                  >
                    Edit Listing
                  </Link>
                  <Link
                    to="/my-listings"
                    className="flex-1 py-2.5 px-4 rounded-xl bg-[#E8F2EA] text-[#164B3B] text-xs font-semibold text-center"
                  >
                    My Listings
                  </Link>
                </div>
              </div>
            ) : item.availabilityStatus !== 'AVAILABLE' ? (
              <div className="p-4 rounded-2xl bg-amber-50 border border-amber-300 text-xs text-amber-900">
                This resource is currently <strong>{item.availabilityStatus}</strong> and cannot accept new
                conflicting reservations until returned.
              </div>
            ) : item.sharingType === 'BORROW' ? (
              <form onSubmit={handleBorrowSubmit} className="space-y-4">
                <div className="flex items-center gap-2 text-[#164B3B]">
                  <Clock className="w-5 h-5" />
                  <h3 className="font-display font-bold text-lg">Request to Borrow (Free)</h3>
                </div>
                <div className="grid grid-cols-2 gap-3">
                  <div>
                    <label className="block text-xs font-semibold mb-1">Start Date</label>
                    <input
                      type="date"
                      required
                      value={startDate}
                      onChange={(e) => setStartDate(e.target.value)}
                      className="w-full px-3 py-2 rounded-xl border border-[#A8C3A0] text-xs"
                    />
                  </div>
                  <div>
                    <label className="block text-xs font-semibold mb-1">Return Due Date</label>
                    <input
                      type="date"
                      required
                      value={endDate}
                      onChange={(e) => setEndDate(e.target.value)}
                      className="w-full px-3 py-2 rounded-xl border border-[#A8C3A0] text-xs"
                    />
                  </div>
                </div>
                <div>
                  <label className="block text-xs font-semibold mb-1">Academic / Campus Purpose *</label>
                  <textarea
                    rows={3}
                    required
                    value={purpose}
                    onChange={(e) => setPurpose(e.target.value)}
                    placeholder="Explain how long you need it and where on campus you can meet for handover..."
                    className="w-full px-3 py-2 rounded-xl border border-[#A8C3A0] text-xs"
                  />
                </div>
                {dateValidationMsg && (
                  <div className="p-3 rounded-xl bg-red-50 border border-red-300 text-xs text-red-800 font-semibold">
                    {dateValidationMsg}
                  </div>
                )}
                <button
                  type="submit"
                  disabled={submitting || !datesAvailable}
                  className="w-full py-3 rounded-xl bg-[#164B3B] hover:bg-[#238B68] disabled:opacity-50 text-white font-semibold text-xs cursor-pointer"
                >
                  {submitting ? 'Submitting Request...' : 'Submit Borrow Request'}
                </button>
              </form>
            ) : item.sharingType === 'RENT' ? (
              <form onSubmit={handleRentalSubmit} className="space-y-4">
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-2 text-[#164B3B]">
                    <Receipt className="w-5 h-5" />
                    <h3 className="font-display font-bold text-lg">Rent Campus Resource</h3>
                  </div>
                  <span className="font-display font-bold text-lg text-[#238B68]">
                    ₹{Number(item.rentalPricePerDay).toFixed(0)}/day
                  </span>
                </div>

                <div className="grid grid-cols-2 gap-3">
                  <div>
                    <label className="block text-xs font-semibold mb-1">Rental Start</label>
                    <input
                      type="date"
                      required
                      value={startDate}
                      onChange={(e) => setStartDate(e.target.value)}
                      className="w-full px-3 py-2 rounded-xl border border-[#A8C3A0] text-xs"
                    />
                  </div>
                  <div>
                    <label className="block text-xs font-semibold mb-1">Rental End</label>
                    <input
                      type="date"
                      required
                      value={endDate}
                      onChange={(e) => setEndDate(e.target.value)}
                      className="w-full px-3 py-2 rounded-xl border border-[#A8C3A0] text-xs"
                    />
                  </div>
                </div>

                {dateValidationMsg && (
                  <div className="p-3 rounded-xl bg-red-50 border border-red-300 text-xs text-red-800 font-semibold">
                    {dateValidationMsg}
                  </div>
                )}

                {/* Live Rental Cost Breakdown */}
                <div className="p-3.5 rounded-2xl bg-[#E8F2EA] border border-[#A8C3A0] space-y-1.5 text-xs">
                  <div className="flex justify-between">
                    <span>
                      Daily Rate (₹{Number(item.rentalPricePerDay).toFixed(0)} × {diffDays} days)
                    </span>
                    <span className="font-bold">₹{estimatedRentalCost}</span>
                  </div>
                  <div className="flex justify-between text-[#737B75]">
                    <span>Specified Security Deposit</span>
                    <span>₹{Number(item.securityDeposit).toFixed(0)}</span>
                  </div>
                  <div className="pt-1.5 border-t border-[#A8C3A0] flex justify-between font-bold text-[#164B3B]">
                    <span>Total Rental Amount (Backend Verified)</span>
                    <span>₹{estimatedRentalCost}</span>
                  </div>
                  <label className="flex items-center gap-2 pt-2 cursor-pointer text-[11px] text-[#202923]">
                    <input
                      type="checkbox"
                      checked={simulatePayment}
                      onChange={(e) => setSimulatePayment(e.target.checked)}
                      className="rounded border-[#A8C3A0]"
                    />
                    <span>
                      Simulate Demo Payment (PAID_SIMULATED — otherwise recorded as PENDING / Gateway Not Integrated)
                    </span>
                  </label>
                </div>

                <button
                  type="submit"
                  disabled={submitting || !datesAvailable}
                  className="w-full py-3 rounded-xl bg-[#164B3B] hover:bg-[#238B68] disabled:opacity-50 text-white font-semibold text-xs cursor-pointer"
                >
                  {submitting ? 'Processing Rental...' : `Confirm Rental Request (₹${estimatedRentalCost})`}
                </button>
              </form>
            ) : item.sharingType === 'EXCHANGE' ? (
              <form onSubmit={handleExchangeSubmit} className="space-y-4">
                <div className="flex items-center gap-2 text-[#164B3B]">
                  <ArrowLeftRight className="w-5 h-5" />
                  <h3 className="font-display font-bold text-lg">Propose Peer Item Exchange</h3>
                </div>
                <div>
                  <label className="block text-xs font-semibold mb-1">
                    Select One of Your Eligible Listings to Offer *
                  </label>
                  <select
                    value={offeredItemId}
                    onChange={(e) => setOfferedItemId(e.target.value)}
                    className="w-full px-3 py-2 rounded-xl border border-[#A8C3A0] text-xs bg-white"
                  >
                    <option value="">-- Choose your item --</option>
                    {myItems.map((mi) => (
                      <option key={mi.id} value={mi.id}>
                        {mi.title} ({mi.itemCondition})
                      </option>
                    ))}
                  </select>
                </div>
                <div>
                  <label className="block text-xs font-semibold mb-1">Exchange Proposal Note *</label>
                  <textarea
                    rows={3}
                    required
                    value={purpose}
                    onChange={(e) => setPurpose(e.target.value)}
                    placeholder="Describe your item's condition and suggest a campus handover spot..."
                    className="w-full px-3 py-2 rounded-xl border border-[#A8C3A0] text-xs"
                  />
                </div>
                <button
                  type="submit"
                  disabled={submitting}
                  className="w-full py-3 rounded-xl bg-[#164B3B] hover:bg-[#238B68] text-white font-semibold text-xs cursor-pointer"
                >
                  Send Exchange Proposal
                </button>
              </form>
            ) : (
              <form onSubmit={handleDonationSubmit} className="space-y-4">
                <div className="flex items-center gap-2 text-[#164B3B]">
                  <HandHeart className="w-5 h-5" />
                  <h3 className="font-display font-bold text-lg">Request Community Donation</h3>
                </div>
                <div>
                  <label className="block text-xs font-semibold mb-1">
                    How will this donated item help your coursework? *
                  </label>
                  <textarea
                    rows={3}
                    required
                    value={purpose}
                    onChange={(e) => setPurpose(e.target.value)}
                    placeholder="Share your department, semester subject, and how you will use this resource..."
                    className="w-full px-3 py-2 rounded-xl border border-[#A8C3A0] text-xs"
                  />
                </div>
                <button
                  type="submit"
                  disabled={submitting}
                  className="w-full py-3 rounded-xl bg-[#238B68] hover:bg-[#164B3B] text-white font-semibold text-xs cursor-pointer"
                >
                  Submit Donation Request
                </button>
              </form>
            )}
          </div>
        </div>
      </div>

      {/* Verified Transaction Reviews for this Item */}
      <div className="bg-white rounded-3xl p-6 border border-[#A8C3A0]/50 space-y-4">
        <h2 className="font-display text-lg font-bold text-[#164B3B]">
          Verified Reviews for This Resource ({reviews.length})
        </h2>
        {reviews.length === 0 ? (
          <p className="text-xs text-[#737B75]">
            No reviews yet for this specific item. Reviews are unlocked automatically after a completed
            handover and return.
          </p>
        ) : (
          <div className="grid sm:grid-cols-2 gap-4">
            {reviews.map((rev) => (
              <div key={rev.id} className="p-4 rounded-2xl bg-[#F8F7F0] border border-[#A8C3A0]/40 space-y-1.5">
                <div className="flex items-center justify-between">
                  <span className="text-xs font-bold text-[#202923]">{rev.reviewerName}</span>
                  <span className="text-xs font-bold text-[#238B68]">
                    {'★'.repeat(rev.rating)}{'☆'.repeat(5 - rev.rating)}
                  </span>
                </div>
                <p className="text-xs text-[#737B75]">{rev.comment}</p>
                <p className="text-[10px] text-[#737B75]">Tx Ref: {rev.transactionCode}</p>
              </div>
            ))}
          </div>
        )}
      </div>

      {/* Related Campus Resources */}
      {relatedItems.length > 0 && (
        <div className="space-y-4">
          <h2 className="font-display text-xl font-bold text-[#164B3B]">Related Campus Listings</h2>
          <div className="grid sm:grid-cols-2 lg:grid-cols-4 gap-5">
            {relatedItems.map((rel) => (
              <ItemCard key={rel.id} item={rel} />
            ))}
          </div>
        </div>
      )}
    </div>
  );
};

// ============================================================================
// 3. CREATE & EDIT LISTING PAGE
// ============================================================================
export const CreateEditListingPage: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const isEdit = Boolean(id);
  const navigate = useNavigate();
  const { showToast } = useAuth();

  const [categories, setCategories] = useState<Category[]>([]);
  const [uploading, setUploading] = useState(false);
  const [submitting, setSubmitting] = useState(false);

  const [form, setForm] = useState({
    title: '',
    description: '',
    categoryId: 1,
    subcategory: '',
    itemCondition: 'LIKE_NEW',
    sharingType: 'BORROW',
    availabilityStatus: 'AVAILABLE',
    campusLocation: 'Tech Park Ground Floor Lobby',
    securityDeposit: 0,
    rentalPricePerDay: 0,
    availableFrom: new Date().toISOString().split('T')[0],
    availableUntil: new Date(Date.now() + 90 * 86400000).toISOString().split('T')[0],
    maxBorrowDays: 14,
    exchangePreferences: '',
    usageRules: 'Please return clean and on time at the agreed campus location.',
    primaryImageUrl:
      'https://images.unsplash.com/photo-1544716278-ca5e3f4abd8c?auto=format&fit=crop&w=800&q=80',
  });

  useEffect(() => {
    apiClient.get<Category[]>('/categories').then((res) => setCategories(res.data));
    if (isEdit && id) {
      apiClient.get<{ item: ItemListing }>(`/items/${id}`).then((res) => {
        const it = res.data.item;
        setForm({
          title: it.title,
          description: it.description,
          categoryId: it.categoryId,
          subcategory: it.subcategory || '',
          itemCondition: it.itemCondition,
          sharingType: it.sharingType,
          availabilityStatus: it.availabilityStatus,
          campusLocation: it.campusLocation,
          securityDeposit: Number(it.securityDeposit || 0),
          rentalPricePerDay: Number(it.rentalPricePerDay || 0),
          availableFrom: it.availableFrom || '',
          availableUntil: it.availableUntil || '',
          maxBorrowDays: it.maxBorrowDays || 14,
          exchangePreferences: it.exchangePreferences || '',
          usageRules: it.usageRules || '',
          primaryImageUrl: it.primaryImageUrl || '',
        });
      });
    }
  }, [id, isEdit]);

  const handleImageUpload = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;
    setUploading(true);
    try {
      const fd = new FormData();
      fd.append('file', file);
      const res = await apiClient.post<{ imageUrl: string }>('/items/upload-image', fd, {
        headers: { 'Content-Type': 'multipart/form-data' },
      });
      setForm((prev) => ({ ...prev, primaryImageUrl: res.data.imageUrl }));
      showToast('Photograph validated & uploaded!', 'success');
    } catch (err) {
      showToast(extractErrorMessage(err), 'error');
    } finally {
      setUploading(false);
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setSubmitting(true);
    try {
      if (isEdit && id) {
        await apiClient.put(`/items/${id}`, form);
        showToast('Listing updated successfully!', 'success');
      } else {
        await apiClient.post('/items', form);
        showToast('New campus resource published!', 'success');
      }
      navigate('/my-listings');
    } catch (err) {
      showToast(extractErrorMessage(err), 'error');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="max-w-3xl mx-auto space-y-6">
      <div>
        <h1 className="font-display text-2xl sm:text-3xl font-bold text-[#164B3B]">
          {isEdit ? 'Edit Campus Resource Listing' : 'Publish a Campus Resource'}
        </h1>
        <p className="text-xs sm:text-sm text-[#737B75]">
          Share textbooks, calculators, bicycles, or lab gear with verified SRM students
        </p>
      </div>

      <form
        onSubmit={handleSubmit}
        className="bg-white rounded-3xl p-6 sm:p-8 border border-[#A8C3A0]/50 shadow-xs space-y-5"
      >
        <div className="grid sm:grid-cols-2 gap-4">
          <div className="sm:col-span-2">
            <label className="block text-xs font-semibold mb-1">Resource Title *</label>
            <input
              type="text"
              required
              value={form.title}
              onChange={(e) => setForm({ ...form, title: e.target.value })}
              placeholder="e.g., Cormen Introduction to Algorithms (CLRS 4th Edition)"
              className="w-full px-3.5 py-2.5 rounded-xl border border-[#A8C3A0] text-sm"
            />
          </div>

          <div>
            <label className="block text-xs font-semibold mb-1">Category *</label>
            <select
              value={form.categoryId}
              onChange={(e) => setForm({ ...form, categoryId: Number(e.target.value) })}
              className="w-full px-3.5 py-2.5 rounded-xl border border-[#A8C3A0] text-sm bg-white"
            >
              {categories.map((c) => (
                <option key={c.id} value={c.id}>
                  {c.name}
                </option>
              ))}
            </select>
          </div>

          <div>
            <label className="block text-xs font-semibold mb-1">Subcategory</label>
            <input
              type="text"
              value={form.subcategory}
              onChange={(e) => setForm({ ...form, subcategory: e.target.value })}
              placeholder="e.g., CSE Textbooks / Calculators"
              className="w-full px-3.5 py-2.5 rounded-xl border border-[#A8C3A0] text-sm"
            />
          </div>

          <div>
            <label className="block text-xs font-semibold mb-1">Sharing Mode *</label>
            <select
              value={form.sharingType}
              onChange={(e) => setForm({ ...form, sharingType: e.target.value })}
              className="w-full px-3.5 py-2.5 rounded-xl border border-[#A8C3A0] text-sm bg-white font-semibold text-[#164B3B]"
            >
              <option value="BORROW">Borrow (Free Temporary Lending)</option>
              <option value="RENT">Rent (Paid Daily Rate)</option>
              <option value="EXCHANGE">Exchange (Peer Item Swap)</option>
              <option value="DONATE">Donate (Permanent Free Gift)</option>
            </select>
          </div>

          <div>
            <label className="block text-xs font-semibold mb-1">Item Condition *</label>
            <select
              value={form.itemCondition}
              onChange={(e) => setForm({ ...form, itemCondition: e.target.value })}
              className="w-full px-3.5 py-2.5 rounded-xl border border-[#A8C3A0] text-sm bg-white"
            >
              <option value="NEW">New</option>
              <option value="LIKE_NEW">Like New</option>
              <option value="GOOD">Good</option>
              <option value="FAIR">Fair</option>
            </select>
          </div>

          <div>
            <label className="block text-xs font-semibold mb-1">Campus Handover Location *</label>
            <input
              type="text"
              required
              value={form.campusLocation}
              onChange={(e) => setForm({ ...form, campusLocation: e.target.value })}
              placeholder="e.g., Tech Park Ground Floor Lobby"
              className="w-full px-3.5 py-2.5 rounded-xl border border-[#A8C3A0] text-sm"
            />
          </div>

          <div>
            <label className="block text-xs font-semibold mb-1">Max Borrowing / Rental Days</label>
            <input
              type="number"
              min="1"
              max="365"
              value={form.maxBorrowDays}
              onChange={(e) => setForm({ ...form, maxBorrowDays: Number(e.target.value) })}
              className="w-full px-3.5 py-2.5 rounded-xl border border-[#A8C3A0] text-sm"
            />
          </div>

          {form.sharingType === 'RENT' && (
            <>
              <div>
                <label className="block text-xs font-semibold mb-1">Daily Rental Price (₹) *</label>
                <input
                  type="number"
                  min="1"
                  required
                  value={form.rentalPricePerDay}
                  onChange={(e) => setForm({ ...form, rentalPricePerDay: Number(e.target.value) })}
                  className="w-full px-3.5 py-2.5 rounded-xl border border-[#A8C3A0] text-sm"
                />
              </div>
              <div>
                <label className="block text-xs font-semibold mb-1">Security Deposit (₹)</label>
                <input
                  type="number"
                  min="0"
                  value={form.securityDeposit}
                  onChange={(e) => setForm({ ...form, securityDeposit: Number(e.target.value) })}
                  className="w-full px-3.5 py-2.5 rounded-xl border border-[#A8C3A0] text-sm"
                />
              </div>
            </>
          )}

          {form.sharingType === 'EXCHANGE' && (
            <div className="sm:col-span-2">
              <label className="block text-xs font-semibold mb-1">Exchange Preferences</label>
              <input
                type="text"
                value={form.exchangePreferences}
                onChange={(e) => setForm({ ...form, exchangePreferences: e.target.value })}
                placeholder="What items would you like to receive in exchange?"
                className="w-full px-3.5 py-2.5 rounded-xl border border-[#A8C3A0] text-sm"
              />
            </div>
          )}

          <div className="sm:col-span-2">
            <label className="block text-xs font-semibold mb-1">Detailed Description *</label>
            <textarea
              rows={3}
              required
              value={form.description}
              onChange={(e) => setForm({ ...form, description: e.target.value })}
              className="w-full px-3.5 py-2.5 rounded-xl border border-[#A8C3A0] text-sm"
            />
          </div>

          <div className="sm:col-span-2">
            <label className="block text-xs font-semibold mb-1">
              Item Photograph (Upload JPEG/PNG/WebP max 5MB or paste Image URL)
            </label>
            <div className="flex flex-col sm:flex-row gap-3">
              <input
                type="text"
                value={form.primaryImageUrl}
                onChange={(e) => setForm({ ...form, primaryImageUrl: e.target.value })}
                className="flex-1 px-3.5 py-2.5 rounded-xl border border-[#A8C3A0] text-xs"
              />
              <label className="inline-flex items-center justify-center gap-2 px-4 py-2.5 rounded-xl bg-[#E8F2EA] text-[#164B3B] text-xs font-semibold cursor-pointer hover:bg-[#A8C3A0]/40 shrink-0">
                <Upload className="w-4 h-4" />
                {uploading ? 'Uploading...' : 'Upload Image File'}
                <input type="file" accept="image/*" onChange={handleImageUpload} className="hidden" />
              </label>
            </div>
          </div>
        </div>

        <div className="flex justify-end gap-3 pt-4 border-t border-[#E8F2EA]">
          <Link
            to="/my-listings"
            className="px-5 py-2.5 rounded-xl border border-[#A8C3A0] text-xs font-semibold"
          >
            Cancel
          </Link>
          <button
            type="submit"
            disabled={submitting}
            className="px-6 py-2.5 rounded-xl bg-[#164B3B] hover:bg-[#238B68] text-white text-xs font-semibold cursor-pointer"
          >
            {submitting ? 'Saving...' : isEdit ? 'Save Changes' : 'Publish Listing'}
          </button>
        </div>
      </form>
    </div>
  );
};

// ============================================================================
// 4. MY LISTINGS PAGE
// ============================================================================
export const MyListingsPage: React.FC = () => {
  const { showToast } = useAuth();
  const [items, setItems] = useState<ItemListing[]>([]);
  const [loading, setLoading] = useState(true);

  const loadListings = () => {
    setLoading(true);
    apiClient
      .get<ItemListing[]>('/items/my-listings')
      .then((res) => setItems(res.data))
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    loadListings();
  }, []);

  const handleArchive = async (itemId: number, title: string) => {
    if (!window.confirm(`Archive listing "${title}"? It will be hidden from search results.`)) {
      return;
    }
    try {
      await apiClient.delete(`/items/${itemId}`);
      showToast('Listing archived.', 'info');
      loadListings();
    } catch (err) {
      showToast(extractErrorMessage(err), 'error');
    }
  };

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="font-display text-2xl font-bold text-[#164B3B]">My Resource Listings</h1>
          <p className="text-xs text-[#737B75]">
            Manage items you have published for borrowing, rental, exchange, or donation
          </p>
        </div>
        <Link
          to="/listings/create"
          className="inline-flex items-center gap-2 px-4 py-2.5 rounded-xl bg-[#164B3B] text-white text-xs font-semibold"
        >
          <PlusCircle className="w-4 h-4" />
          Add New Listing
        </Link>
      </div>

      {loading ? (
        <div className="h-48 bg-white rounded-3xl animate-pulse" />
      ) : items.length === 0 ? (
        <div className="bg-white rounded-3xl p-12 text-center border border-[#A8C3A0]/50 space-y-3">
          <p className="font-display text-lg font-bold text-[#202923]">
            You haven&apos;t published any campus listings yet
          </p>
          <Link
            to="/listings/create"
            className="inline-block px-5 py-2.5 rounded-xl bg-[#164B3B] text-white text-xs font-semibold"
          >
            Publish Your First Item
          </Link>
        </div>
      ) : (
        <div className="grid sm:grid-cols-2 lg:grid-cols-3 gap-5">
          {items.map((item) => (
            <div key={item.id} className="space-y-2">
              <ItemCard item={item} />
              <div className="flex gap-2">
                <Link
                  to={`/listings/edit/${item.id}`}
                  className="flex-1 py-2 rounded-xl bg-white border border-[#A8C3A0] text-[#164B3B] text-xs font-semibold flex items-center justify-center gap-1.5 hover:bg-[#E8F2EA]"
                >
                  <Edit3 className="w-3.5 h-3.5" />
                  Edit
                </Link>
                <button
                  onClick={() => handleArchive(item.id, item.title)}
                  className="flex-1 py-2 rounded-xl bg-white border border-red-200 text-red-700 text-xs font-semibold flex items-center justify-center gap-1.5 hover:bg-red-50 cursor-pointer"
                >
                  <Trash2 className="w-3.5 h-3.5" />
                  Archive
                </button>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
};
