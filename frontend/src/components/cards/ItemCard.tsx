import React from 'react';
import { Link } from 'react-router-dom';
import { MapPin, ShieldCheck, Star, ArrowUpRight } from 'lucide-react';
import { ItemListing } from '../../types';

const SHARING_BADGE_STYLES: Record<string, string> = {
  BORROW: 'bg-[#E8F2EA] text-[#164B3B] border-[#A8C3A0]',
  RENT: 'bg-amber-50 text-amber-900 border-amber-300',
  EXCHANGE: 'bg-teal-50 text-teal-900 border-teal-300',
  DONATE: 'bg-emerald-100 text-[#164B3B] border-[#238B68]',
};

export const ItemCard: React.FC<{ item: ItemListing }> = ({ item }) => {
  return (
    <Link
      to={`/items/${item.id}`}
      className="group bg-white rounded-2xl border border-[#A8C3A0]/40 overflow-hidden shadow-xs hover:shadow-md hover:border-[#238B68] transition-all flex flex-col"
    >
      <div className="relative h-48 bg-[#E8F2EA] overflow-hidden">
        <img
          src={
            item.primaryImageUrl ||
            'https://images.unsplash.com/photo-1544716278-ca5e3f4abd8c?auto=format&fit=crop&w=800&q=80'
          }
          alt={item.title}
          className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-300"
        />
        <div className="absolute top-3 left-3 flex flex-wrap gap-1.5">
          <span
            className={`px-2.5 py-1 rounded-full text-[11px] font-bold border ${
              SHARING_BADGE_STYLES[item.sharingType] || 'bg-white text-[#202923]'
            }`}
          >
            {item.sharingType}
          </span>
          <span className="px-2.5 py-1 rounded-full text-[11px] font-semibold bg-[#202923]/80 text-white backdrop-blur-xs">
            {item.itemCondition.replace('_', ' ')}
          </span>
        </div>
        <div className="absolute bottom-3 right-3">
          <span
            className={`px-2.5 py-1 rounded-full text-[10px] font-bold uppercase tracking-wider ${
              item.availabilityStatus === 'AVAILABLE'
                ? 'bg-[#238B68] text-white'
                : 'bg-amber-600 text-white'
            }`}
          >
            {item.availabilityStatus}
          </span>
        </div>
      </div>

      <div className="p-4 flex-1 flex flex-col justify-between">
        <div>
          <p className="text-[11px] font-semibold text-[#238B68] uppercase tracking-wider">
            {item.categoryName}
          </p>
          <h3 className="mt-1 font-display font-bold text-base text-[#202923] group-hover:text-[#164B3B] line-clamp-1">
            {item.title}
          </h3>
          <p className="mt-1 text-xs text-[#737B75] line-clamp-2">{item.description}</p>
        </div>

        <div className="mt-4 pt-3 border-t border-[#E8F2EA] space-y-2">
          <div className="flex items-center justify-between text-xs">
            <span className="inline-flex items-center gap-1 text-[#737B75] truncate">
              <MapPin className="w-3.5 h-3.5 text-[#238B68] shrink-0" />
              <span className="truncate">{item.campusLocation}</span>
            </span>
            {item.sharingType === 'RENT' ? (
              <span className="font-display font-bold text-sm text-[#164B3B] shrink-0">
                ₹{Number(item.rentalPricePerDay).toFixed(0)}/day
              </span>
            ) : item.sharingType === 'DONATE' ? (
              <span className="font-semibold text-xs text-[#238B68] shrink-0">Free Donation</span>
            ) : (
              <span className="font-medium text-xs text-[#737B75] shrink-0">
                Max {item.maxBorrowDays}d
              </span>
            )}
          </div>

          <div className="flex items-center justify-between pt-1">
            <div className="flex items-center gap-1.5 min-w-0">
              <div className="w-6 h-6 rounded-full bg-[#164B3B] text-white text-[10px] font-bold flex items-center justify-center shrink-0">
                {item.ownerName.charAt(0)}
              </div>
              <span className="text-xs font-medium text-[#202923] truncate">{item.ownerName}</span>
              {item.ownerVerified && (
                <ShieldCheck className="w-3.5 h-3.5 text-[#238B68] shrink-0" title="Verified SRM Student" />
              )}
            </div>

            <div className="flex items-center gap-1.5 shrink-0">
              <span className="inline-flex items-center gap-0.5 px-2 py-0.5 rounded-md bg-[#E8F2EA] text-[#164B3B] text-[11px] font-bold">
                <Star className="w-3 h-3 fill-[#238B68] text-[#238B68]" />
                {Number(item.ownerTrustScore).toFixed(0)} TS
              </span>
              <ArrowUpRight className="w-4 h-4 text-[#737B75] group-hover:text-[#164B3B]" />
            </div>
          </div>
        </div>
      </div>
    </Link>
  );
};
