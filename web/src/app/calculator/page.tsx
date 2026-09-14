"use client";

import { useState, useEffect } from "react";

type MaterialSubCategory = {
  id: number;
  code: string;
  name_en: string;
  current_spot_rate: number;
};

type PricingResponse = {
  gross_material_value_inr: number;
  logistics_and_haulage_cost_inr: number;
  aggregator_margin_inr: number;
  net_valuation_inr: number;
  final_payable_amount_inr: number;
  estimated_price_range_low_inr: number;
  estimated_price_range_high_inr: number;
  line_items: any[];
  audio_summary_hi?: string;
};

export default function CalculatorPage() {
  const [subcategories, setSubCategories] = useState<MaterialSubCategory[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Form state
  const [materialCode, setMaterialCode] = useState("");
  const [weightKg, setWeightKg] = useState("1");
  const [purityFactor, setPurityFactor] = useState("1.0");
  const [distanceKm, setDistanceKm] = useState("5");

  // Result state
  const [calculating, setCalculating] = useState(false);
  const [result, setResult] = useState<PricingResponse | null>(null);
  const [calcError, setCalcError] = useState<string | null>(null);

  useEffect(() => {
    const fetchRates = async () => {
      try {
        setLoading(true);
        const subRes = await fetch("http://localhost:8000/api/v1/rates/subcategories");
        if (!subRes.ok) throw new Error("Failed to fetch materials");
        const subData = await subRes.json();
        setSubCategories(subData);
        if (subData.length > 0) {
          setMaterialCode(subData[0].code);
        }
      } catch (err: any) {
        setError(err.message || "An error occurred fetching materials");
      } finally {
        setLoading(false);
      }
    };

    fetchRates();
  }, []);

  const handleCalculate = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!materialCode) return;

    try {
      setCalculating(true);
      setCalcError(null);
      setResult(null);

      const requestBody = {
        items: [
          {
            subcategory_code: materialCode,
            weight_kg: parseFloat(weightKg),
            purity_override: parseFloat(purityFactor),
            rust_level_percentage: 0.0
          }
        ],
        salvage_reusable_parts_value: 0.0,
        pickup_distance_km: parseFloat(distanceKm),
        custom_margin_percentage: 0.15
      };

      const res = await fetch("http://localhost:8000/api/v1/pricing/calculate-hierarchical", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify(requestBody),
      });

      if (!res.ok) {
        const errorData = await res.json();
        throw new Error(errorData.detail || "Failed to calculate price");
      }

      const data = await res.json();
      setResult(data);
    } catch (err: any) {
      setCalcError(err.message || "An error occurred during calculation");
    } finally {
      setCalculating(false);
    }
  };

  if (loading) {
    return (
      <div className="flex justify-center items-center h-64">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-emerald-700"></div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="bg-red-50 border-l-4 border-red-500 p-4 rounded">
        <p className="text-red-700 font-medium">Error loading calculator: {error}</p>
        <p className="text-sm text-red-600 mt-1">Please make sure the backend server is running on localhost:8000.</p>
      </div>
    );
  }

  return (
    <div className="max-w-5xl mx-auto space-y-8">
      <div>
        <h1 className="text-3xl font-bold text-slate-900">Dynamic Scrap Valuation</h1>
        <p className="text-slate-600 mt-1">Calculate real-time pricing with metallurgical purity and logistics deductions.</p>
      </div>

      <div className="grid md:grid-cols-2 gap-8">
        <div className="bg-white shadow sm:rounded-lg border border-slate-200 p-6">
          <h2 className="text-lg font-medium text-slate-900 mb-4 border-b pb-2">Calculation Parameters</h2>

          <form onSubmit={handleCalculate} className="space-y-4">
            <div>
              <label htmlFor="material" className="block text-sm font-medium text-slate-700">
                Scrap Material
              </label>
              <select
                id="material"
                value={materialCode}
                onChange={(e) => setMaterialCode(e.target.value)}
                className="mt-1 block w-full pl-3 pr-10 py-2 text-base border-slate-300 focus:outline-none focus:ring-emerald-500 focus:border-emerald-500 sm:text-sm rounded-md shadow-sm border bg-white"
                required
              >
                {subcategories.map((sub) => (
                  <option key={sub.code} value={sub.code}>
                    {sub.name_en} (₹{sub.current_spot_rate}/kg)
                  </option>
                ))}
              </select>
            </div>

            <div>
              <label htmlFor="weight" className="block text-sm font-medium text-slate-700">
                Weight (kg)
              </label>
              <input
                type="number"
                id="weight"
                min="0.1"
                step="0.1"
                value={weightKg}
                onChange={(e) => setWeightKg(e.target.value)}
                className="mt-1 block w-full border border-slate-300 rounded-md shadow-sm py-2 px-3 focus:outline-none focus:ring-emerald-500 focus:border-emerald-500 sm:text-sm"
                required
              />
            </div>

            <div>
              <label htmlFor="purity" className="block text-sm font-medium text-slate-700">
                Purity Factor (0.1 - 1.0)
              </label>
              <input
                type="number"
                id="purity"
                min="0.1"
                max="1.0"
                step="0.01"
                value={purityFactor}
                onChange={(e) => setPurityFactor(e.target.value)}
                className="mt-1 block w-full border border-slate-300 rounded-md shadow-sm py-2 px-3 focus:outline-none focus:ring-emerald-500 focus:border-emerald-500 sm:text-sm"
                required
              />
            </div>

            <div>
              <label htmlFor="distance" className="block text-sm font-medium text-slate-700">
                Pickup Distance (km)
              </label>
              <input
                type="number"
                id="distance"
                min="0"
                step="0.1"
                value={distanceKm}
                onChange={(e) => setDistanceKm(e.target.value)}
                className="mt-1 block w-full border border-slate-300 rounded-md shadow-sm py-2 px-3 focus:outline-none focus:ring-emerald-500 focus:border-emerald-500 sm:text-sm"
                required
              />
            </div>

            <button
              type="submit"
              disabled={calculating}
              className={`w-full flex justify-center py-2.5 px-4 border border-transparent rounded-md shadow-sm text-sm font-medium text-white ${
                calculating ? "bg-emerald-400" : "bg-emerald-600 hover:bg-emerald-700"
              } focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-emerald-500 transition-colors`}
            >
              {calculating ? "Calculating..." : "Calculate Valuation"}
            </button>
          </form>

          {calcError && (
            <div className="mt-4 bg-red-50 border-l-4 border-red-500 p-4 rounded">
              <p className="text-sm text-red-700">{calcError}</p>
            </div>
          )}
        </div>

        <div>
          {result ? (
            <div className="bg-emerald-50 shadow sm:rounded-lg border border-emerald-200 overflow-hidden h-full flex flex-col">
              <div className="px-6 py-5 border-b border-emerald-200 bg-emerald-600">
                <h3 className="text-lg leading-6 font-medium text-white">Valuation Summary</h3>
              </div>
              <div className="px-6 py-5 flex-1">
                <dl className="grid grid-cols-1 gap-x-4 gap-y-6 sm:grid-cols-2">
                  <div className="sm:col-span-2">
                    <dt className="text-sm font-medium text-emerald-800">Gross Material Value</dt>
                    <dd className="mt-1 text-2xl font-bold text-emerald-900">₹{result.gross_material_value_inr.toFixed(2)}</dd>
                  </div>

                  <div className="sm:col-span-1">
                    <dt className="text-sm font-medium text-emerald-800">Logistics Deduction</dt>
                    <dd className="mt-1 text-lg font-semibold text-red-600">-₹{result.logistics_and_haulage_cost_inr.toFixed(2)}</dd>
                  </div>

                  <div className="sm:col-span-1">
                    <dt className="text-sm font-medium text-emerald-800">Platform/Dealer Margin</dt>
                    <dd className="mt-1 text-lg font-semibold text-red-600">-₹{result.aggregator_margin_inr.toFixed(2)}</dd>
                  </div>

                  <div className="sm:col-span-2 border-t border-emerald-200 pt-4 mt-2">
                    <dt className="text-sm font-bold text-emerald-800 uppercase tracking-wider">Final Payable Amount (Payout)</dt>
                    <dd className="mt-1 text-4xl font-extrabold text-emerald-600">₹{result.final_payable_amount_inr.toFixed(2)}</dd>
                  </div>

                  <div className="sm:col-span-2 text-sm text-slate-500 bg-white p-3 rounded border border-slate-100">
                    <span className="font-medium">Estimated Market Range:</span> ₹{result.estimated_price_range_low_inr.toFixed(2)} - ₹{result.estimated_price_range_high_inr.toFixed(2)}
                  </div>

                  {result.audio_summary_hi && (
                    <div className="sm:col-span-2 mt-4 p-3 bg-emerald-100 rounded-md border border-emerald-300">
                      <dt className="text-xs font-bold text-emerald-800 uppercase mb-1">Vernacular Audio Summary (Hindi)</dt>
                      <dd className="text-sm text-emerald-900 italic font-medium">"{result.audio_summary_hi}"</dd>
                    </div>
                  )}
                </dl>
              </div>
            </div>
          ) : (
            <div className="bg-slate-50 shadow sm:rounded-lg border border-slate-200 h-full flex items-center justify-center p-6 text-center">
              <div className="text-slate-500">
                <svg className="mx-auto h-12 w-12 text-slate-400 mb-3" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="1" d="M9 7h6m0 10v-3m-3 3h.01M9 17h.01M9 14h.01M12 14h.01M15 11h.01M12 11h.01M9 11h.01M7 21h10a2 2 0 002-2V5a2 2 0 00-2-2H7a2 2 0 00-2 2v14a2 2 0 002 2z"></path></svg>
                <p className="text-lg font-medium">No valuation calculated yet</p>
                <p className="text-sm mt-1">Enter parameters and calculate to see the dynamic pricing breakdown.</p>
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
