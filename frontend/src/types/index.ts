export type UserRole = 'STUDENT' | 'MODERATOR' | 'ADMIN';
export type SharingType = 'BORROW' | 'RENT' | 'EXCHANGE' | 'DONATE';
export type ItemCondition = 'NEW' | 'LIKE_NEW' | 'GOOD' | 'FAIR';
export type ListingStatus = 'AVAILABLE' | 'RESERVED' | 'UNAVAILABLE' | 'PENDING_APPROVAL' | 'COMPLETED' | 'ARCHIVED';
export type TrustLevel = 'EXCELLENT' | 'TRUSTED' | 'DEVELOPING' | 'LOW_TRUST';

export interface UserSummary {
  id: number;
  fullName: string;
  email: string;
  registrationNumber: string;
  role: UserRole;
  accountStatus: string;
  emailVerified: boolean;
  campusVerified: boolean;
  department: string;
  yearOfStudy: number;
  contactNumber: string;
  hostelOrBlock: string;
  bio: string;
  avatarUrl: string;
  preferredPickupSpot: string;
  profileCompletionPct: number;
  trustScore: number;
  trustLevel: TrustLevel;
  averageRating: number;
  completedTransactions: number;
  createdAt: string;
}

export interface Category {
  id: number;
  name: string;
  slug: string;
  description: string;
  iconName: string;
  active: boolean;
}

export interface ItemListing {
  id: number;
  title: string;
  description: string;
  categoryId: number;
  categoryName: string;
  subcategory: string;
  itemCondition: ItemCondition;
  sharingType: SharingType;
  availabilityStatus: ListingStatus;
  campusLocation: string;
  location?: string;
  city?: string;
  neighborhood?: string;
  landmark?: string;
  pickupInstructions?: string;
  securityDeposit: number;
  rentalPricePerDay: number;
  availableFrom: string | null;
  availableUntil: string | null;
  maxBorrowDays: number;
  exchangePreferences: string | null;
  usageRules: string | null;
  primaryImageUrl: string;
  galleryUrls: string[];
  viewCount: number;
  ownerId: number;
  ownerName: string;
  ownerDepartment: string;
  ownerVerified: boolean;
  ownerTrustScore: number;
  ownerTrustLevel: TrustLevel;
  ownerAverageRating: number;
  createdAt: string;
}

export interface BorrowRequestItem {
  id: number;
  itemId: number;
  itemTitle: string;
  itemImageUrl: string;
  campusLocation: string;
  borrowerId: number;
  borrowerName: string;
  borrowerTrustScore: number;
  lenderId: number;
  lenderName: string;
  startDate: string;
  endDate: string;
  purpose: string;
  status: string;
  ownerResponseNote: string | null;
  handoverConfirmedAt: string | null;
  returnRequestedAt: string | null;
  returnedAt: string | null;
  returnedOnTime: boolean | null;
  transactionId: number | null;
  createdAt: string;
}

export interface ExchangeOfferItem {
  id: number;
  requestedItemId: number;
  requestedItemTitle: string;
  requestedItemImage: string;
  offeredItemId: number;
  offeredItemTitle: string;
  offeredItemImage: string;
  proposerId: number;
  proposerName: string;
  proposerTrustScore: number;
  receiverId: number;
  receiverName: string;
  receiverTrustScore: number;
  proposalMessage: string;
  status: string;
  proposerHandoverConfirmed: boolean;
  receiverHandoverConfirmed: boolean;
  completedAt: string | null;
  transactionId: number | null;
  createdAt: string;
}

export interface RentalItem {
  id: number;
  itemId: number;
  itemTitle: string;
  itemImageUrl: string;
  renterId: number;
  renterName: string;
  renterTrustScore: number;
  ownerId: number;
  ownerName: string;
  startDate: string;
  endDate: string;
  rentalDays: number;
  dailyRate: number;
  securityDeposit: number;
  totalRentalAmount: number;
  paymentStatus: string;
  status: string;
  handoverConfirmedAt: string | null;
  returnedAt: string | null;
  returnedOnTime: boolean | null;
  transactionId: number | null;
  createdAt: string;
}

export interface DonationRequestItem {
  id: number;
  itemId: number;
  itemTitle: string;
  itemImageUrl: string;
  requesterId: number;
  requesterName: string;
  requesterDepartment: string;
  requesterTrustScore: number;
  donorId: number;
  donorName: string;
  motivationMessage: string;
  status: string;
  handoverConfirmedAt: string | null;
  completedAt: string | null;
  transactionId: number | null;
  createdAt: string;
}

export interface TransactionItem {
  id: number;
  transactionCode: string;
  transactionType: SharingType;
  referenceId: number;
  primaryItemId: number;
  primaryItemTitle: string;
  primaryItemImage: string;
  secondaryItemId: number | null;
  secondaryItemTitle: string | null;
  initiatorUserId: number;
  initiatorUserName: string;
  counterpartyUserId: number;
  counterpartyUserName: string;
  status: string;
  monetaryAmount: number;
  paymentStatus: string;
  startDate: string | null;
  dueDate: string | null;
  completedAt: string | null;
  returnedOnTime: boolean | null;
  notes: string | null;
  reviewedByCurrentUser: boolean;
  createdAt: string;
}

export interface ReviewItem {
  id: number;
  transactionId: number;
  transactionCode: string;
  itemId: number;
  itemTitle: string;
  reviewerId: number;
  reviewerName: string;
  revieweeId: number;
  revieweeName: string;
  rating: number;
  comment: string;
  flagged: boolean;
  createdAt: string;
}

export interface TrustHistoryEntry {
  id: number;
  previousScore: number;
  newScore: number;
  delta: number;
  triggerEvent: string;
  reasonSummary: string;
  createdAt: string;
}

export interface TrustScoreDetails {
  userId: number;
  fullName: string;
  campusVerified: boolean;
  totalScore: number;
  trustLevel: TrustLevel;
  verifiedAccountScore: number;
  completedTxScore: number;
  userReviewScore: number;
  onTimeReturnScore: number;
  completedTransactionsCount: number;
  averageRating: number;
  onTimeReturnPercentage: number;
  eligibleReturnCount: number;
  onTimeReturnCount: number;
  history: TrustHistoryEntry[];
}

export interface NotificationItem {
  id: number;
  notificationType: string;
  title: string;
  message: string;
  targetPath: string | null;
  relatedEntityId: number | null;
  read: boolean;
  createdAt: string;
}

export interface DisputeItem {
  id: number;
  transactionId: number | null;
  transactionCode: string;
  itemId: number | null;
  itemTitle: string;
  reporterId: number;
  reporterName: string;
  reportedUserId: number;
  reportedUserName: string;
  category: string;
  title: string;
  description: string;
  status: string;
  resolutionNotes: string | null;
  resolvedAt: string | null;
  createdAt: string;
  evidenceList: Array<{
    id: number;
    submittedBy: string;
    evidenceType: string;
    evidenceUrl: string | null;
    comment: string;
    createdAt: string;
  }>;
}

export interface AuditLogItem {
  id: number;
  actorUserId: number;
  actorName: string;
  actorRole: string;
  actionType: string;
  targetEntity: string;
  targetId: number | null;
  details: string;
  createdAt: string;
}
