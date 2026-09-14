"use client";

import { useState } from "react";

type Recycler = {
  id: number;
  company_name: string;
  facility_address: string;
  city: string;
  state: string;
  pincode: string;
  latitude: number;
  longitude: number;
  accepted_category_codes: string;
  cpcb_registration_number: string;
  regulatory_board: string;
  contact_number: string;
};

type RecyclerWithDistance = {
  recycler: Recycler;
  distance_km: number;
  estimated_transit_time_mins: number;
  google_maps_directions_url: string;
};

export default function RecyclersPage() {
  const [lat, setLat] = useState("19.0760"); // default Mumbai
  const [lon, setLon] = useState("72.8777");
  const [radius, setRadius] = useState("100");
  const [loading, setLoading] = useState(false);
  const [results, setResults] = useState<RecyclerWithDistance[]>([]);
  const [error, setError] = useState<string | null>(null);

  const handleSearch = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!lat || !lon) return;

    try {
      setLoading(true);
      setError(null);

      const res = await fetch(
        `http://localhost:8000/api/v1/recyclers/nearest?lat=${lat}&lon=${lon}&max_radius_km=${radius}&limit=10`
      );

      if (!res.ok) {
        throw new Error("Failed to fetch recyclers");
      }

      const data = await res.json();
      setResults(data.nearest_facilities || []);

      if (data.nearest_facilities?.length === 0) {
        setError("No authorized recyclers found within the specified radius.");
      }
    } catch (err: any) {
      setError(err.message || "An error occurred");
    } finally {
      setLoading(false);
    }
  };

  const getUserLocation = () => {
    if ("geolocation" in navigator) {
      setLoading(true);
      navigator.geolocation.getCurrentPosition(
        (position) => {
          setLat(position.coords.latitude.toString());
          setLon(position.coords.longitude.toString());
          setLoading(false);
        },
        (err) => {
          setError("Failed to get location: " + err.message);
          setLoading(false);
        }
      );
    } else {
      setError("Geolocation is not supported by your browser");
    }
  };

  return (
    <div className="max-w-6xl mx-auto space-y-8">
      <div>
        <h1 className="text-3xl font-bold text-slate-900">CPCB/SPCB Recycler Locator</h1>
        <p className="text-slate-600 mt-1">
          Find nearest authorized e-waste and hazmat dismantling facilities.
        </p>
      </div>

      <div className="bg-white shadow sm:rounded-lg border border-slate-200 p-6">
        <form onSubmit={handleSearch} className="grid grid-cols-1 md:grid-cols-4 gap-6 items-end">
          <div>
            <label htmlFor="latitude" className="block text-sm font-medium text-slate-700">Latitude</label>
            <input
              type="text"
              id="latitude"
              value={lat}
              onChange={(e) => setLat(e.target.value)}
              className="mt-1 block w-full border border-slate-300 rounded-md shadow-sm py-2 px-3 focus:outline-none focus:ring-emerald-500 focus:border-emerald-500 sm:text-sm"
              required
            />
          </div>
          <div>
            <label htmlFor="longitude" className="block text-sm font-medium text-slate-700">Longitude</label>
            <input
              type="text"
              id="longitude"
              value={lon}
              onChange={(e) => setLon(e.target.value)}
              className="mt-1 block w-full border border-slate-300 rounded-md shadow-sm py-2 px-3 focus:outline-none focus:ring-emerald-500 focus:border-emerald-500 sm:text-sm"
              required
            />
          </div>
          <div>
            <label htmlFor="radius" className="block text-sm font-medium text-slate-700">Radius (km)</label>
            <input
              type="number"
              id="radius"
              value={radius}
              onChange={(e) => setRadius(e.target.value)}
              className="mt-1 block w-full border border-slate-300 rounded-md shadow-sm py-2 px-3 focus:outline-none focus:ring-emerald-500 focus:border-emerald-500 sm:text-sm"
              required
            />
          </div>
          <div className="flex space-x-2">
            <button
              type="button"
              onClick={getUserLocation}
              className="px-3 py-2 border border-slate-300 rounded-md shadow-sm text-sm font-medium text-slate-700 bg-white hover:bg-slate-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-emerald-500 transition-colors flex items-center justify-center"
              title="Use my location"
            >
              <svg className="h-5 w-5 text-slate-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M17.657 16.657L13.414 20.9a1.998 1.998 0 01-2.827 0l-4.244-4.243a8 8 0 1111.314 0z" />
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 11a3 3 0 11-6 0 3 3 0 016 0z" />
              </svg>
            </button>
            <button
              type="submit"
              disabled={loading}
              className={`flex-1 flex justify-center py-2 px-4 border border-transparent rounded-md shadow-sm text-sm font-medium text-white ${
                loading ? "bg-emerald-400" : "bg-emerald-600 hover:bg-emerald-700"
              } focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-emerald-500 transition-colors`}
            >
              {loading ? "Searching..." : "Search"}
            </button>
          </div>
        </form>

        {error && (
          <div className="mt-4 bg-red-50 border-l-4 border-red-500 p-4 rounded">
            <p className="text-sm text-red-700">{error}</p>
          </div>
        )}
      </div>

      {results.length > 0 && (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {results.map((item, idx) => (
            <div key={item.recycler.id} className="bg-white rounded-lg shadow-sm border border-slate-200 overflow-hidden flex flex-col h-full hover:shadow-md transition-shadow">
              <div className="p-5 flex-1">
                <div className="flex justify-between items-start">
                  <h3 className="text-lg font-bold text-slate-900 line-clamp-2">{item.recycler.company_name}</h3>
                  <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-emerald-100 text-emerald-800 shrink-0 ml-2">
                    {item.distance_km.toFixed(1)} km
                  </span>
                </div>

                <div className="mt-2 flex items-center text-sm text-slate-500">
                  <span className="font-semibold text-slate-700 mr-2">Reg Board:</span>
                  <span className="bg-slate-100 px-2 py-0.5 rounded text-xs border border-slate-200">{item.recycler.regulatory_board}</span>
                </div>

                <div className="mt-4 text-sm text-slate-600 space-y-2">
                  <p className="flex items-start">
                    <svg className="h-5 w-5 text-slate-400 mr-2 shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M17.657 16.657L13.414 20.9a1.998 1.998 0 01-2.827 0l-4.244-4.243a8 8 0 1111.314 0z" />
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 11a3 3 0 11-6 0 3 3 0 016 0z" />
                    </svg>
                    <span className="line-clamp-2">{item.recycler.facility_address}, {item.recycler.city}, {item.recycler.state} - {item.recycler.pincode}</span>
                  </p>

                  {item.recycler.contact_number && (
                    <p className="flex items-center">
                      <svg className="h-5 w-5 text-slate-400 mr-2" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M3 5a2 2 0 012-2h3.28a1 1 0 01.948.684l1.498 4.493a1 1 0 01-.502 1.21l-2.257 1.13a11.042 11.042 0 005.516 5.516l1.13-2.257a1 1 0 011.21-.502l4.493 1.498a1 1 0 01.684.949V19a2 2 0 01-2 2h-1C9.716 21 3 14.284 3 6V5z" />
                      </svg>
                      {item.recycler.contact_number}
                    </p>
                  )}

                  <p className="flex items-center">
                    <svg className="h-5 w-5 text-slate-400 mr-2" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
                    </svg>
                    Est. Transit Time: ~{item.estimated_transit_time_mins} mins
                  </p>
                </div>

                <div className="mt-4 pt-4 border-t border-slate-100">
                  <p className="text-xs text-slate-500 mb-1 font-medium uppercase tracking-wider">Accepted Materials</p>
                  <div className="flex flex-wrap gap-1">
                    {item.recycler.accepted_category_codes.split(",").map((code) => (
                      <span key={code} className="inline-block bg-slate-100 text-slate-600 text-[10px] px-2 py-1 rounded border border-slate-200">
                        {code.trim().replace('_', ' ')}
                      </span>
                    ))}
                  </div>
                </div>
              </div>

              <div className="bg-slate-50 p-4 border-t border-slate-200">
                <a
                  href={item.google_maps_directions_url}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="w-full flex justify-center items-center px-4 py-2 border border-emerald-600 rounded-md shadow-sm text-sm font-medium text-emerald-700 bg-white hover:bg-emerald-50 transition-colors"
                >
                  <svg className="h-4 w-4 mr-2" fill="currentColor" viewBox="0 0 24 24">
                    <path d="M12 2C8.13 2 5 5.13 5 9c0 5.25 7 13 7 13s7-7.75 7-13c0-3.87-3.13-7-7-7zm0 9.5a2.5 2.5 0 010-5 2.5 2.5 0 010 5z" />
                  </svg>
                  Navigate (Google Maps)
                </a>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
