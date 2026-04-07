import React, { useState, useEffect, useCallback, useRef } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import {
    MagnifyingGlassIcon,
    XMarkIcon,
    ArrowPathIcon,
    ChevronDownIcon,
    ChevronUpIcon,
    FunnelIcon,
    ArchiveBoxIcon,
} from '@heroicons/react/24/outline';
import axios from 'axios';

const API_URL = `http://${window.location.hostname}:8080/api/inventory`;

const formatDate = (dateVal) => {
    if (!dateVal) return '—';
    try {
        if (Array.isArray(dateVal)) {
            const d = new Date(dateVal[0], dateVal[1] - 1, dateVal[2], dateVal[3] || 0, dateVal[4] || 0);
            return d.toLocaleString('en-US', { year: '2-digit', month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' });
        }
        return new Date(dateVal).toLocaleString('en-US', { year: '2-digit', month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' });
    } catch {
        return '—';
    }
};

const InventoryOnline = () => {
    const [items, setItems]               = useState([]);
    const [categories, setCategories]     = useState([]);
    const [loading, setLoading]           = useState(true);
    const [searchQuery, setSearchQuery]   = useState('');
    const [selectedCategory, setSelectedCategory] = useState('');
    const [page, setPage]                 = useState(0);
    const [totalPages, setTotalPages]     = useState(0);
    const [totalElements, setTotalElements] = useState(0);
    const [expandedRows, setExpandedRows] = useState({});
    const [multiplesData, setMultiplesData]   = useState({});
    const [loadingMultiples, setLoadingMultiples] = useState({});
    const scrollRef = useRef(null);

    /* ── Categories ── */
    useEffect(() => {
        axios.get(`${API_URL}/categories`)
            .then(r => setCategories(r.data))
            .catch(e => console.error('Categories failed', e));
    }, []);

    /* ── Items ── */
    const fetchItems = useCallback(async () => {
        setLoading(true);
        try {
            const params = { page, size: 20, sortField: 'name', sortDir: 'ASC' };
            if (searchQuery)      params.search     = searchQuery;
            if (selectedCategory) params.categoryId = selectedCategory;
            const res = await axios.get(`${API_URL}/items`, { params });
            setItems(res.data.content);
            setTotalPages(res.data.totalPages);
            setTotalElements(res.data.totalElements);
        } catch (e) {
            console.error('Items failed', e);
        } finally {
            setLoading(false);
        }
    }, [searchQuery, selectedCategory, page]);

    useEffect(() => { setPage(0); }, [searchQuery, selectedCategory]);

    useEffect(() => {
        const t = setTimeout(fetchItems, 350);
        return () => clearTimeout(t);
    }, [fetchItems]);

    useEffect(() => {
        if (scrollRef.current) scrollRef.current.scrollTo({ top: 0, behavior: 'instant' });
    }, [page]);

    /* ── Multiples ── */
    const fetchMultiples = async (itemId) => {
        if (multiplesData[itemId] !== undefined) return;
        setLoadingMultiples(prev => ({ ...prev, [itemId]: true }));
        try {
            const res = await axios.get(`${API_URL}/${itemId}/multiples`);
            setMultiplesData(prev => ({ ...prev, [itemId]: res.data }));
        } catch (e) {
            setMultiplesData(prev => ({ ...prev, [itemId]: [] }));
        } finally {
            setLoadingMultiples(prev => ({ ...prev, [itemId]: false }));
        }
    };

    const toggleRow = (itemId) => {
        setExpandedRows(prev => ({
            ...prev,
            [itemId]: !prev[itemId]
        }));
        if (!expandedRows[itemId]) {
            fetchMultiples(itemId);
        }
    };

    const activeCatName = categories.find(c => c.id === Number(selectedCategory))?.name;

    return (
        <div className="h-full w-full flex flex-col overflow-hidden">

            {/* ══════════════════════════════════════════
                TOOLBAR
            ══════════════════════════════════════════ */}
            <header className="shrink-0 bg-white/60 dark:bg-zinc-900/50 backdrop-blur-md border-b border-zinc-200 dark:border-white/5 px-4 sm:px-6 md:px-8 py-3 flex flex-wrap items-center gap-3 transition-colors">
                <div className="hidden sm:block mr-2">
                    <h1 className="text-xl font-black tracking-tight text-zinc-900 dark:text-white leading-none">
                        Inventory <span className="text-blue-600 dark:text-blue-500">Online</span>
                    </h1>
                    <p className="text-[10px] text-zinc-500 uppercase tracking-widest mt-0.5">Table View</p>
                </div>

                <div className="flex-1 min-w-[160px] max-w-sm relative group">
                    <MagnifyingGlassIcon className="w-4 h-4 text-zinc-400 dark:text-zinc-500 absolute left-3.5 top-1/2 -translate-y-1/2 group-focus-within:text-blue-500 transition-colors pointer-events-none" />
                    <input
                        type="text"
                        value={searchQuery}
                        onChange={e => setSearchQuery(e.target.value)}
                        placeholder="Search code or name…"
                        className="w-full bg-zinc-50 dark:bg-zinc-950/60 border border-zinc-200 dark:border-zinc-800 text-base rounded-xl pl-10 pr-8 py-2.5 outline-none focus:ring-2 focus:ring-blue-500/40 focus:border-blue-500 transition-all text-zinc-900 dark:text-zinc-200 placeholder-zinc-400 dark:placeholder-zinc-600 focus:bg-white dark:focus:bg-zinc-950/60"
                    />
                    {searchQuery && (
                        <button onClick={() => setSearchQuery('')} className="absolute right-3 top-1/2 -translate-y-1/2 text-zinc-400 dark:text-zinc-500 hover:text-zinc-900 dark:hover:text-white">
                            <XMarkIcon className="w-4 h-4" />
                        </button>
                    )}
                </div>

                <div className="relative">
                    <FunnelIcon className="w-4 h-4 text-zinc-400 dark:text-zinc-500 absolute left-3 top-1/2 -translate-y-1/2 pointer-events-none" />
                    <select
                        value={selectedCategory}
                        onChange={e => setSelectedCategory(e.target.value)}
                        className="appearance-none bg-zinc-50 dark:bg-zinc-950/60 border border-zinc-200 dark:border-zinc-800 text-sm rounded-xl pl-9 pr-8 py-2.5 outline-none focus:ring-2 focus:ring-blue-500/40 focus:border-blue-500 transition-all text-zinc-900 dark:text-zinc-300 cursor-pointer min-w-[150px]"
                    >
                        <option value="">All Categories</option>
                        {categories.map(cat => (
                            <option key={cat.id} value={cat.id}>{cat.name}</option>
                        ))}
                    </select>
                    <ChevronDownIcon className="w-4 h-4 text-zinc-400 dark:text-zinc-500 absolute right-2.5 top-1/2 -translate-y-1/2 pointer-events-none" />
                </div>

                {selectedCategory && (
                    <button
                        onClick={() => setSelectedCategory('')}
                        className="flex items-center gap-1.5 px-3 py-2 rounded-xl bg-blue-500/10 border border-blue-500/20 text-blue-600 dark:text-blue-400 text-xs font-bold hover:bg-blue-500/20 transition-all"
                    >
                        {activeCatName}
                        <XMarkIcon className="w-3.5 h-3.5" />
                    </button>
                )}

                <button
                    onClick={fetchItems}
                    className="ml-auto p-2.5 rounded-xl bg-white dark:bg-zinc-900 border border-zinc-200 dark:border-zinc-800 text-zinc-500 hover:text-blue-600 dark:hover:text-blue-400 hover:border-blue-500/30 shadow-sm dark:shadow-none transition-all"
                >
                    <ArrowPathIcon className={`w-4 h-4 ${loading ? 'animate-spin text-blue-500' : ''}`} />
                </button>
            </header>

            {/* ══════════════════════════════════════════
                TABLE CONTAINER
            ══════════════════════════════════════════ */}
            <main className="flex-1 flex flex-col overflow-hidden p-4 sm:p-6 md:p-8">

                <div className="flex items-center justify-between mb-4 shrink-0 transition-colors">
                    <p className="text-zinc-500 text-sm">
                        Showing <span className="text-zinc-900 dark:text-white font-semibold">{items.length}</span> of <span className="text-zinc-900 dark:text-white font-semibold">{totalElements}</span> items
                        {activeCatName && <> in <span className="text-blue-600 dark:text-blue-400 font-semibold">{activeCatName}</span></>}
                    </p>
                    {totalPages > 1 && (
                        <p className="text-zinc-500 dark:text-zinc-600 text-sm font-mono">
                            Page {page + 1} / {totalPages}
                        </p>
                    )}
                </div>

                <div className="bg-white dark:bg-zinc-900/40 flex-1 backdrop-blur border border-zinc-200 dark:border-white/5 rounded-2xl flex flex-col overflow-hidden shadow-sm dark:shadow-xl transition-colors">
                    <div ref={scrollRef} className="overflow-auto flex-1 custom-scrollbar">
                        <table className="w-full text-left border-collapse whitespace-nowrap relative">
                            <thead className="sticky top-0 z-20 bg-zinc-50/95 dark:bg-zinc-950/95 backdrop-blur-xl shadow-[0_1px_0_rgba(0,0,0,0.05)] dark:shadow-[0_1px_0_rgba(255,255,255,0.1)] text-zinc-500 dark:text-zinc-400 text-xs uppercase tracking-wider font-bold transition-colors">
                                <tr>
                                    <th className="px-4 py-3">Item Details</th>
                                    <th className="px-4 py-3">Code</th>
                                    <th className="px-4 py-3 text-right">Selling</th>
                                    <th className="px-4 py-3 text-right">Wholesale</th>
                                    <th className="px-4 py-3 text-right">Cost</th>
                                    <th className="px-4 py-3 text-center">Total Stock</th>
                                    <th className="px-4 py-3 text-center">Batches</th>
                                </tr>
                            </thead>
                            <tbody className="divide-y divide-zinc-200 dark:divide-white/5 text-sm transition-colors">
                                {loading && items.length === 0 ? (
                                    <tr>
                                        <td colSpan="7" className="px-4 py-12 text-center text-zinc-500">
                                            <ArrowPathIcon className="w-8 h-8 animate-spin mx-auto mb-3 opacity-50" />
                                            Loading items...
                                        </td>
                                    </tr>
                                ) : items.length === 0 ? (
                                    <tr>
                                        <td colSpan="7" className="px-4 py-12 text-center text-zinc-500">
                                            No items found matching criteria.
                                        </td>
                                    </tr>
                                ) : (
                                    items.map(item => (
                                        <React.Fragment key={item.id}>
                                            <tr 
                                                className={`transition-colors hover:bg-zinc-50 dark:hover:bg-white/[0.02] ${item.multiPrice === 1 ? 'cursor-pointer' : ''} ${expandedRows[item.id] ? 'bg-blue-50 dark:bg-blue-500/[0.02]' : ''}`}
                                                onClick={() => item.multiPrice === 1 && toggleRow(item.id)}
                                            >
                                                <td className="px-4 py-3">
                                                    <div className="text-sm font-bold text-zinc-900 dark:text-white mb-0.5">{item.name}</div>
                                                    <div className="text-xs text-zinc-500">{item.supplier || 'Standard Item'}</div>
                                                </td>
                                                <td className="px-4 py-3 font-mono text-zinc-500 dark:text-zinc-300">
                                                    {item.code || '-'}
                                                </td>
                                                <td className="px-4 py-3 text-right font-mono font-bold text-emerald-600 dark:text-emerald-400">
                                                    {item.sellingPrice != null ? `Rs.${Number(item.sellingPrice).toLocaleString()}` : '—'}
                                                </td>
                                                <td className="px-4 py-3 text-right font-mono font-bold text-blue-600 dark:text-blue-400">
                                                    {item.wholesalePrice != null ? `Rs.${Number(item.wholesalePrice).toLocaleString()}` : '—'}
                                                </td>
                                                <td className="px-4 py-3 text-right font-mono font-bold text-amber-600 dark:text-amber-400">
                                                    {item.unitPrice != null ? `Rs.${Number(item.unitPrice).toLocaleString()}` : '—'}
                                                </td>
                                                <td className="px-4 py-3 text-center">
                                                    <span className={`inline-flex px-3 py-1 rounded-full font-bold font-mono text-xs ${
                                                        item.qty <= 0 ? 'bg-rose-100 dark:bg-rose-500/10 text-rose-600 dark:text-rose-400' : 'bg-emerald-100 dark:bg-emerald-500/10 text-emerald-700 dark:text-emerald-400'
                                                    }`}>
                                                        {item.qty ?? 0}
                                                    </span>
                                                </td>
                                                <td className="px-4 py-3 text-center">
                                                    {item.multiPrice === 1 && (
                                                        <button className="p-1.5 rounded-lg text-zinc-400 hover:text-zinc-900 dark:hover:text-white hover:bg-zinc-200 dark:hover:bg-white/10 transition-all">
                                                            {expandedRows[item.id] ? <ChevronUpIcon className="w-5 h-5 text-blue-600 dark:text-blue-400" /> : <ChevronDownIcon className="w-5 h-5" />}
                                                        </button>
                                                    )}
                                                </td>
                                            </tr>

                                            {/* Expandable Batch Row */}
                                            {expandedRows[item.id] && (
                                                <tr className="bg-zinc-50/50 dark:bg-zinc-950/40 border-b border-blue-100 dark:border-blue-500/10 transition-colors">
                                                    <td colSpan="7" className="p-0">
                                                        <div className="overflow-hidden">
                                                            <div className="px-10 py-5 flex flex-col">
                                                                <div className="flex items-center gap-2 mb-3 text-blue-600 dark:text-blue-400 uppercase text-xs font-bold tracking-widest transition-colors">
                                                                    <ArchiveBoxIcon className="w-4 h-4" />
                                                                    <span>Multiple Price Batches</span>
                                                                    {loadingMultiples[item.id] && <ArrowPathIcon className="w-3.5 h-3.5 animate-spin ml-2" />}
                                                                </div>

                                                                {multiplesData[item.id] === undefined || loadingMultiples[item.id] ? (
                                                                    <div className="text-zinc-500 text-sm py-2">Fetching batches...</div>
                                                                ) : multiplesData[item.id].length === 0 ? (
                                                                    <div className="text-zinc-500 text-sm py-2 italic bg-zinc-100 dark:bg-white/[0.02] inline-block px-4 rounded-lg self-start transition-colors">No additional price batches found for this item.</div>
                                                                ) : (
                                                                    <div className="inline-block self-start max-w-full overflow-x-auto rounded-xl border border-zinc-200 dark:border-white/5 bg-white dark:bg-zinc-900/50 shadow-sm dark:shadow-inner transition-colors">
                                                                        <table className="min-w-[400px] text-left text-sm transition-colors">
                                                                            <thead className="bg-zinc-50/80 dark:bg-zinc-950/50 text-zinc-500 text-xs uppercase transition-colors">
                                                                                <tr>
                                                                                    <th className="px-4 py-2.5 font-medium">Batch Date & Time</th>
                                                                                    <th className="px-4 py-2.5 font-medium">Batch Price (Selling)</th>
                                                                                    <th className="px-4 py-2.5 font-medium text-center">Qty Available</th>
                                                                                    <th className="px-4 py-2.5 font-medium text-right">Batch Unit Cost</th>
                                                                                </tr>
                                                                            </thead>
                                                                                <tbody className="divide-y divide-zinc-200 dark:divide-white/5 font-mono transition-colors">
                                                                                {multiplesData[item.id].map((batch, bidx) => (
                                                                                    <tr key={bidx} className="hover:bg-zinc-50 dark:hover:bg-white/[0.02] transition-colors">
                                                                                        <td className="px-4 py-3 text-zinc-500 dark:text-zinc-400 font-sans text-xs">
                                                                                            {formatDate(batch.date)}
                                                                                        </td>
                                                                                        <td className="px-4 py-3 text-emerald-600 dark:text-emerald-400 font-bold">
                                                                                            Rs.{batch.price != null ? Number(batch.price).toLocaleString() : '—'}
                                                                                        </td>
                                                                                        <td className="px-4 py-3 text-center">
                                                                                            <span className={`px-2.5 py-0.5 rounded text-xs font-bold ${
                                                                                                (batch.qty ?? 0) > 0 ? 'bg-emerald-100 dark:bg-emerald-500/15 text-emerald-700 dark:text-emerald-400' : 'bg-zinc-200 dark:bg-zinc-800 text-zinc-600 dark:text-zinc-500'
                                                                                            }`}>
                                                                                                {batch.qty ?? 0}
                                                                                            </span>
                                                                                        </td>
                                                                                        <td className="px-4 py-3 text-right text-amber-600 dark:text-amber-400/80">
                                                                                            {batch.unitPrice != null && batch.unitPrice > 0 ? `Rs.${Number(batch.unitPrice).toLocaleString()}` : '—'}
                                                                                        </td>
                                                                                    </tr>
                                                                                ))}
                                                                            </tbody>
                                                                        </table>
                                                                    </div>
                                                                )}
                                                            </div>
                                                        </div>
                                                    </td>
                                                </tr>
                                            )}
                                        </React.Fragment>
                                    ))
                                )}
                            </tbody>
                        </table>
                    </div>
                </div>

                {/* Pagination */}
                {totalPages > 1 && (
                    <div className="flex justify-center mt-4 shrink-0 pb-1">
                        <div className="flex items-center gap-2 bg-white dark:bg-zinc-900/50 border border-zinc-200 dark:border-zinc-800 rounded-2xl p-1.5 shadow-lg transition-colors">
                            <button
                                disabled={page === 0}
                                onClick={() => setPage(p => p - 1)}
                                className="px-5 py-2.5 rounded-xl text-sm font-medium bg-zinc-100 dark:bg-zinc-800 text-zinc-700 dark:text-zinc-300 disabled:opacity-40 disabled:cursor-not-allowed hover:bg-zinc-200 dark:hover:bg-zinc-700 transition"
                            >
                                ← Previous
                            </button>
                            <span className="px-4 font-mono text-sm text-zinc-500 dark:text-zinc-400">
                                <span className="text-zinc-900 dark:text-white">{page + 1}</span>
                                <span className="mx-1.5 text-zinc-400 dark:text-zinc-600">/</span>
                                <span className="text-zinc-900 dark:text-white">{totalPages}</span>
                            </span>
                            <button
                                disabled={page === totalPages - 1}
                                onClick={() => setPage(p => p + 1)}
                                className="px-5 py-2.5 rounded-xl text-sm font-medium bg-zinc-100 dark:bg-zinc-800 text-zinc-700 dark:text-zinc-300 disabled:opacity-40 disabled:cursor-not-allowed hover:bg-zinc-200 dark:hover:bg-zinc-700 transition"
                            >
                                Next →
                            </button>
                        </div>
                    </div>
                )}
            </main>
        </div>
    );
};

export default InventoryOnline;
