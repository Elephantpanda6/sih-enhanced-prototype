import type { Metadata } from "next";
import { Geist, Geist_Mono } from "next/font/google";
import Link from "next/link";
import "./globals.css";

const geistSans = Geist({
  variable: "--font-geist-sans",
  subsets: ["latin"],
});

const geistMono = Geist_Mono({
  variable: "--font-geist-mono",
  subsets: ["latin"],
});

export const metadata: Metadata = {
  title: "SIH Smart Scrap & E-Waste Valuation",
  description: "Enterprise Production Backend API for SIH Smart Scrap & E-Waste Valuation",
};

export default function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <html
      lang="en"
      className={`${geistSans.variable} ${geistMono.variable} h-full antialiased`}
    >
      <body className="min-h-full flex flex-col bg-slate-50 text-slate-900 font-sans">
        <header className="bg-emerald-700 text-white shadow-md">
          <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
            <div className="flex justify-between h-16 items-center">
              <div className="flex-shrink-0 flex items-center">
                <Link href="/" className="font-bold text-xl tracking-tight">
                  SmartScrap
                </Link>
              </div>
              <nav className="hidden md:ml-6 md:flex md:space-x-8">
                <Link
                  href="/"
                  className="text-emerald-50 hover:bg-emerald-600 hover:text-white px-3 py-2 rounded-md text-sm font-medium transition-colors"
                >
                  Home
                </Link>
                <Link
                  href="/rates"
                  className="text-emerald-50 hover:bg-emerald-600 hover:text-white px-3 py-2 rounded-md text-sm font-medium transition-colors"
                >
                  Scrap Rates
                </Link>
                <Link
                  href="/calculator"
                  className="text-emerald-50 hover:bg-emerald-600 hover:text-white px-3 py-2 rounded-md text-sm font-medium transition-colors"
                >
                  Calculate Price
                </Link>
                <Link
                  href="/recyclers"
                  className="text-emerald-50 hover:bg-emerald-600 hover:text-white px-3 py-2 rounded-md text-sm font-medium transition-colors"
                >
                  Recyclers
                </Link>
              </nav>
            </div>
          </div>
        </header>

        <main className="flex-1 max-w-7xl w-full mx-auto p-4 sm:p-6 lg:p-8">
          {children}
        </main>

        <footer className="bg-emerald-900 text-emerald-200 py-6 text-center text-sm">
          <p>
            &copy; {new Date().getFullYear()} SIH Smart Scrap & E-Waste Valuation Engine. All rights reserved.
          </p>
        </footer>
      </body>
    </html>
  );
}
