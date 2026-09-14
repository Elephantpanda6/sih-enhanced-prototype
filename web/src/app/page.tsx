import Link from "next/link";

export default function Home() {
  return (
    <div className="flex flex-col items-center justify-center py-12 px-4 sm:px-6 lg:px-8 space-y-12">
      <section className="text-center max-w-4xl">
        <h1 className="text-4xl sm:text-5xl font-extrabold text-emerald-900 tracking-tight mb-6">
          Smart Scrap & E-Waste Dynamic Valuation
        </h1>
        <p className="text-lg sm:text-xl text-slate-600 mb-8 leading-relaxed">
          A production-grade enterprise platform engineered for the circular economy, empowering informal scrap collection networks and government-regulated e-waste recycling hubs under CPCB mandates.
        </p>

        <div className="flex flex-col sm:flex-row gap-4 justify-center">
          <Link
            href="/calculator"
            className="inline-flex justify-center items-center px-6 py-3 border border-transparent text-base font-medium rounded-md text-white bg-emerald-600 hover:bg-emerald-700 shadow-sm transition-colors"
          >
            Calculate Scrap Price
          </Link>
          <Link
            href="/rates"
            className="inline-flex justify-center items-center px-6 py-3 border border-emerald-600 text-base font-medium rounded-md text-emerald-700 bg-white hover:bg-emerald-50 shadow-sm transition-colors"
          >
            View Market Rates
          </Link>
          <Link
            href="/recyclers"
            className="inline-flex justify-center items-center px-6 py-3 border border-emerald-600 text-base font-medium rounded-md text-emerald-700 bg-white hover:bg-emerald-50 shadow-sm transition-colors"
          >
            Find Recyclers
          </Link>
        </div>
      </section>

      <section className="grid md:grid-cols-3 gap-8 max-w-6xl mt-12 w-full">
        <div className="bg-white p-6 rounded-xl shadow-sm border border-slate-100 flex flex-col items-center text-center">
          <div className="h-12 w-12 bg-emerald-100 text-emerald-700 rounded-full flex items-center justify-center mb-4">
            <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M9 7h6m0 10v-3m-3 3h.01M9 17h.01M9 14h.01M12 14h.01M15 11h.01M12 11h.01M9 11h.01M7 21h10a2 2 0 002-2V5a2 2 0 00-2-2H7a2 2 0 00-2 2v14a2 2 0 002 2z"></path></svg>
          </div>
          <h3 className="text-xl font-bold text-slate-900 mb-2">Dynamic Pricing Engine</h3>
          <p className="text-slate-600">Calculates scrap valuation based on real-time mandi spot rates, purity deductions, and e-waste precious metal yields.</p>
        </div>

        <div className="bg-white p-6 rounded-xl shadow-sm border border-slate-100 flex flex-col items-center text-center">
          <div className="h-12 w-12 bg-emerald-100 text-emerald-700 rounded-full flex items-center justify-center mb-4">
            <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 002-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10"></path></svg>
          </div>
          <h3 className="text-xl font-bold text-slate-900 mb-2">Hierarchical Taxonomy</h3>
          <p className="text-slate-600">Organized classification of Ferrous, Non-Ferrous, E-Waste, Battery, and Plastics with metallurgical properties.</p>
        </div>

        <div className="bg-white p-6 rounded-xl shadow-sm border border-slate-100 flex flex-col items-center text-center">
          <div className="h-12 w-12 bg-emerald-100 text-emerald-700 rounded-full flex items-center justify-center mb-4">
            <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M17.657 16.657L13.414 20.9a1.998 1.998 0 01-2.827 0l-4.244-4.243a8 8 0 1111.314 0z"></path><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M15 11a3 3 0 11-6 0 3 3 0 016 0z"></path></svg>
          </div>
          <h3 className="text-xl font-bold text-slate-900 mb-2">CPCB Recycler Locator</h3>
          <p className="text-slate-600">Geospatial discovery of nearest authorized recycling hubs using the Haversine formula and Google Maps directions.</p>
        </div>
      </section>
    </div>
  );
}
