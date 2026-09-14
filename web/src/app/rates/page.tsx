"use client";

import { useState, useEffect } from "react";

type MaterialCategory = {
  id: number;
  code: string;
  name_en: string;
  description: string;
};

type MaterialSubCategory = {
  id: number;
  category_id: number;
  code: string;
  name_en: string;
  name_hi: string;
  name_mr: string;
  hsn_code: string;
  current_spot_rate: number;
  default_purity: number;
  density_kg_m3: number;
  rust_deduction_factor: number;
  is_active: boolean;
};

export default function RatesPage() {
  const [categories, setCategories] = useState<MaterialCategory[]>([]);
  const [subcategories, setSubCategories] = useState<MaterialSubCategory[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [selectedCategory, setSelectedCategory] = useState<string>("all");

  useEffect(() => {
    const fetchRates = async () => {
      try {
        setLoading(true);
        // We will fetch from local host 8000 for local dev
        const catRes = await fetch("http://localhost:8000/api/v1/rates/categories");
        if (!catRes.ok) throw new Error("Failed to fetch categories");
        const catData = await catRes.json();
        setCategories(catData);

        const subRes = await fetch("http://localhost:8000/api/v1/rates/subcategories");
        if (!subRes.ok) throw new Error("Failed to fetch subcategories");
        const subData = await subRes.json();
        setSubCategories(subData);
      } catch (err: any) {
        setError(err.message || "An error occurred");
      } finally {
        setLoading(false);
      }
    };

    fetchRates();
  }, []);

  const filteredSubcategories =
    selectedCategory === "all"
      ? subcategories
      : subcategories.filter(
          (sub) =>
            categories.find((c) => c.code === selectedCategory)?.id ===
            sub.category_id
        );

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
        <p className="text-red-700 font-medium">Error loading rates: {error}</p>
        <p className="text-sm text-red-600 mt-1">Please make sure the backend server is running on localhost:8000.</p>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
        <div>
          <h1 className="text-3xl font-bold text-slate-900">Current Market Rates</h1>
          <p className="text-slate-600 mt-1">Daily Mandi Spot Prices for Scrap Commodities</p>
        </div>

        <div className="flex items-center space-x-2">
          <label htmlFor="category-filter" className="text-sm font-medium text-slate-700">
            Filter by Category:
          </label>
          <select
            id="category-filter"
            value={selectedCategory}
            onChange={(e) => setSelectedCategory(e.target.value)}
            className="block w-full pl-3 pr-10 py-2 text-base border-slate-300 focus:outline-none focus:ring-emerald-500 focus:border-emerald-500 sm:text-sm rounded-md shadow-sm border bg-white"
          >
            <option value="all">All Categories</option>
            {categories.map((cat) => (
              <option key={cat.code} value={cat.code}>
                {cat.name_en}
              </option>
            ))}
          </select>
        </div>
      </div>

      <div className="bg-white shadow overflow-hidden sm:rounded-lg border border-slate-200">
        <div className="overflow-x-auto">
          <table className="min-w-full divide-y divide-slate-200">
            <thead className="bg-slate-50">
              <tr>
                <th scope="col" className="px-6 py-3 text-left text-xs font-medium text-slate-500 uppercase tracking-wider">
                  Material Name
                </th>
                <th scope="col" className="px-6 py-3 text-left text-xs font-medium text-slate-500 uppercase tracking-wider">
                  Category
                </th>
                <th scope="col" className="px-6 py-3 text-left text-xs font-medium text-slate-500 uppercase tracking-wider">
                  Local Names
                </th>
                <th scope="col" className="px-6 py-3 text-right text-xs font-medium text-slate-500 uppercase tracking-wider">
                  Spot Rate (₹/kg)
                </th>
                <th scope="col" className="px-6 py-3 text-right text-xs font-medium text-slate-500 uppercase tracking-wider">
                  Base Purity
                </th>
              </tr>
            </thead>
            <tbody className="bg-white divide-y divide-slate-200">
              {filteredSubcategories.map((sub) => {
                const category = categories.find((c) => c.id === sub.category_id);
                return (
                  <tr key={sub.id} className="hover:bg-slate-50 transition-colors">
                    <td className="px-6 py-4 whitespace-nowrap">
                      <div className="text-sm font-medium text-slate-900">{sub.name_en}</div>
                      <div className="text-xs text-slate-500">HSN: {sub.hsn_code}</div>
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap">
                      <span className="px-2 inline-flex text-xs leading-5 font-semibold rounded-full bg-emerald-100 text-emerald-800">
                        {category?.name_en || 'Unknown'}
                      </span>
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap">
                      <div className="text-sm text-slate-600">HI: {sub.name_hi}</div>
                      <div className="text-sm text-slate-600">MR: {sub.name_mr}</div>
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-right text-sm font-bold text-emerald-700">
                      ₹{sub.current_spot_rate.toFixed(2)}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-right text-sm text-slate-600">
                      {(sub.default_purity * 100).toFixed(0)}%
                    </td>
                  </tr>
                );
              })}

              {filteredSubcategories.length === 0 && (
                <tr>
                  <td colSpan={5} className="px-6 py-8 text-center text-slate-500">
                    No materials found for the selected category.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}
