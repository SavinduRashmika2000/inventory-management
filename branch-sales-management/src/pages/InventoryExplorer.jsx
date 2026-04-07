import React, { useState, useEffect, useCallback, useRef } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import {
    MagnifyingGlassIcon,
    XMarkIcon,
    CubeIcon,
    TagIcon,
    ArrowPathIcon,
    ChevronDownIcon,
    ChevronUpIcon,
    ArchiveBoxIcon,
    FunnelIcon,
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

const InventoryExplorer = () => {
    const [items, setItems]               = useState([]);
    const [categories, setCategories]     = useState([]);
    const [loading, setLoading]           = useState(true);
    const [searchQuery, setSearchQuery]   = useState('');
    const [selectedCategory, setSelectedCategory] = useState('');
    const [page, setPage]                 = useState(0);
    const [totalPages, setTotalPages]     = useState(0);
    const [totalElements, setTotalElements] = useState(0);
    const [expandedItemId, setExpandedItemId] = useState(null);
    const [multiplesData, setMultiplesData]   = useState({});
    const [loadingMultiples, setLoadingMultiples] = useState(false);
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
        setLoadingMultiples(true);
        try {
            const res = await axios.get(`${API_URL}/${itemId}/multiples`);
            setMultiplesData(prev => ({ ...prev, [itemId]: res.data }));
        } catch (e) {
            setMultiplesData(prev => ({ ...prev, [itemId]: [] }));
        } finally {
            setLoadingMultiples(false);
        }
    };

    const toggleExpand = (itemId) => {
        if (expandedItemId === itemId) {
            setExpandedItemId(null);
        } else {
            setExpandedItemId(itemId);
            fetchMultiples(itemId);
        }
    };

    const activeCatName = categories.find(c => c.id === Number(selectedCategory))?.name;

    /* ─── Animation variants ─── */
    const containerVariants = {
        hidden: { opacity: 0 },
        show: { opacity: 1, transition: { staggerChildren: 0.035 } }
    };
    const cardVariants = {
        hidden: { opacity: 0, y: 16 },
        show: { opacity: 1, y: 0, transition: { type: 'spring', stiffness: 280, damping: 22 } }
    };

    return (
        <div className="h-full w-full flex flex-col overflow-hidden">

            {/* ══════════════════════════════════════════
                TOOLBAR  –  search + category dropdown
            ══════════════════════════════════════════ */}
            <header className="shrink-0 bg-white/60 dark:bg-zinc-900/50 backdrop-blur-md border-b border-zinc-200 dark:border-white/5 px-4 sm:px-6 md:px-8 py-3 flex flex-wrap items-center gap-3 transition-colors">

                {/* Title */}
                <div className="hidden sm:block">
                    <h1 className="text-xl font-black tracking-tight text-zinc-900 dark:text-white leading-none">
                        Inventory <span className="text-emerald-600 dark:text-emerald-500">Master</span>
                    </h1>
                    <p className="text-[10px] text-zinc-500 uppercase tracking-widest mt-0.5">Nut &amp; Bolt Catalog</p>
                </div>

                {/* ── Search ── */}
                <div className="flex-1 min-w-[160px] max-w-sm relative group">
                    <MagnifyingGlassIcon className="w-4 h-4 text-zinc-400 dark:text-zinc-500 absolute left-3.5 top-1/2 -translate-y-1/2 group-focus-within:text-emerald-500 transition-colors pointer-events-none" />
                    <input
                        type="text"
                        value={searchQuery}
                        onChange={e => setSearchQuery(e.target.value)}
                        placeholder="Search code or name…"
                        className="w-full bg-zinc-50 dark:bg-zinc-950/60 border border-zinc-200 dark:border-zinc-800 text-base rounded-xl pl-10 pr-8 py-2.5 outline-none focus:ring-2 focus:ring-emerald-500/40 focus:border-emerald-500 transition-all text-zinc-900 dark:text-zinc-200 placeholder-zinc-400 dark:placeholder-zinc-600 focus:bg-white dark:focus:bg-zinc-950/60"
                    />
                    {searchQuery && (
                        <button onClick={() => setSearchQuery('')} className="absolute right-3 top-1/2 -translate-y-1/2 text-zinc-400 dark:text-zinc-500 hover:text-zinc-900 dark:hover:text-white">
                            <XMarkIcon className="w-4 h-4" />
                        </button>
                    )}
                </div>

                {/* ── Category dropdown ── */}
                <div className="relative">
                    <FunnelIcon className="w-4 h-4 text-zinc-400 dark:text-zinc-500 absolute left-3 top-1/2 -translate-y-1/2 pointer-events-none" />
                    <select
                        value={selectedCategory}
                        onChange={e => setSelectedCategory(e.target.value)}
                        className="appearance-none bg-zinc-50 dark:bg-zinc-950/60 border border-zinc-200 dark:border-zinc-800 text-sm rounded-xl pl-9 pr-8 py-2.5 outline-none focus:ring-2 focus:ring-emerald-500/40 focus:border-emerald-500 transition-all text-zinc-900 dark:text-zinc-300 cursor-pointer min-w-[150px]"
                    >
                        <option value="">All Categories</option>
                        {categories.map(cat => (
                            <option key={cat.id} value={cat.id}>{cat.name}</option>
                        ))}
                    </select>
                    {/* Custom chevron */}
                    <ChevronDownIcon className="w-4 h-4 text-zinc-400 dark:text-zinc-500 absolute right-2.5 top-1/2 -translate-y-1/2 pointer-events-none" />
                </div>

                {/* Clear filter chip */}
                {selectedCategory && (
                    <button
                        onClick={() => setSelectedCategory('')}
                        className="flex items-center gap-1.5 px-3 py-2 rounded-xl bg-emerald-500/10 border border-emerald-500/20 text-emerald-600 dark:text-emerald-400 text-xs font-bold hover:bg-emerald-500/20 transition-all"
                    >
                        {activeCatName}
                        <XMarkIcon className="w-3.5 h-3.5" />
                    </button>
                )}

                {/* Refresh */}
                <button
                    onClick={fetchItems}
                    className="ml-auto p-2.5 rounded-xl bg-white dark:bg-zinc-900 border border-zinc-200 dark:border-zinc-800 text-zinc-500 hover:text-emerald-600 dark:hover:text-emerald-400 hover:border-emerald-500/30 shadow-sm dark:shadow-none transition-all"
                >
                    <ArrowPathIcon className={`w-4 h-4 ${loading ? 'animate-spin text-emerald-500' : ''}`} />
                </button>
            </header>

            {/* ══════════════════════════════════════════
                ITEM GRID
            ══════════════════════════════════════════ */}
            <main className="flex-1 flex flex-col overflow-hidden p-4 sm:p-6 md:p-8">

                {/* Stats row */}
                <div className="flex items-center justify-between mb-5 shrink-0">
                    <p className="text-zinc-500 text-xs">
                        Showing <span className="text-zinc-900 dark:text-white font-semibold">{items.length}</span> of <span className="text-zinc-900 dark:text-white font-semibold">{totalElements}</span> items
                        {activeCatName && <> in <span className="text-emerald-600 dark:text-emerald-400 font-semibold">{activeCatName}</span></>}
                    </p>
                    {totalPages > 1 && (
                        <p className="text-zinc-500 dark:text-zinc-600 text-xs font-mono">
                            Page {page + 1} / {totalPages}
                        </p>
                    )}
                </div>

                {/* Items Scroll Container */}
                <div ref={scrollRef} className="flex-1 flex flex-col overflow-y-auto custom-scrollbar">
                    {/* Loading skeletons */}
                    {loading && items.length === 0 ? (
                        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-4 md:gap-5 pb-4">
                            {[...Array(8)].map((_, i) => (
                                <div key={i} className="animate-pulse bg-zinc-200/50 dark:bg-zinc-900/50 border border-zinc-300/50 dark:border-zinc-800/50 rounded-2xl h-52 transition-colors" />
                            ))}
                        </div>
                    ) : items.length === 0 ? (
                    <div className="flex flex-col items-center justify-center h-64 text-zinc-400 dark:text-zinc-500">
                        <CubeIcon className="w-14 h-14 mb-4 opacity-40 text-zinc-400 dark:text-zinc-500" />
                        <p className="text-base font-medium text-zinc-500 dark:text-zinc-400">No items found</p>
                        <p className="text-sm mt-1 text-zinc-400">Try a different search or category.</p>
                    </div>
                ) : (
                    <>
                        <motion.div
                            variants={containerVariants}
                            initial="hidden"
                            animate="show"
                            className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-4 md:gap-5 pb-6"
                        >
                            {items.map(item => (
                                <motion.div key={item.id} variants={cardVariants} className="flex flex-col">

                                    {/* ── Item card ── */}
                                    <div
                                        onClick={() => item.multiPrice === 1 && toggleExpand(item.id)}
                                        className={`bg-white dark:bg-zinc-900/40 backdrop-blur border rounded-2xl p-5 flex flex-col transition-all duration-300 relative overflow-hidden shadow-sm dark:shadow-none
                                            ${item.multiPrice === 1 ? 'cursor-pointer hover:shadow-xl hover:shadow-emerald-900/20 group' : ''}
                                            ${expandedItemId === item.id 
                                                ? 'border-emerald-500/40 dark:border-emerald-500/50 rounded-b-none shadow-md' 
                                                : (item.multiPrice === 1 ? 'border-zinc-200 dark:border-white/5 hover:border-emerald-500/30' : 'border-zinc-200 dark:border-white/5')}`}
                                    >
                                        {/* glow blob */}
                                        <div className={`absolute top-0 right-0 w-28 h-28 rounded-full blur-3xl -mr-10 -mt-10 transition-colors pointer-events-none 
                                            ${item.multiPrice === 1 ? 'bg-emerald-500/10 dark:bg-emerald-500/5 group-hover:bg-emerald-500/20 dark:group-hover:bg-emerald-500/10' : 'bg-transparent'}`} />

                                        {/* Code + badges */}
                                        <div className="flex justify-between items-start mb-3.5 relative z-10">
                                            <div className="px-2.5 py-1.5 rounded-md bg-zinc-100 dark:bg-zinc-800 text-sm font-mono text-zinc-700 dark:text-zinc-300 border border-zinc-200 dark:border-zinc-700 flex items-center gap-1.5 transition-colors">
                                                <TagIcon className="w-3.5 h-3.5 text-emerald-600 dark:text-emerald-500" />
                                                {item.code || 'N/A'}
                                            </div>
                                            <div className="flex items-center gap-2">
                                                {item.multiPrice === 1 && (
                                                    <span className="px-2 py-0.5 rounded text-[11px] font-bold uppercase tracking-wider bg-purple-100 dark:bg-purple-500/15 text-purple-600 dark:text-purple-400 border border-purple-200 dark:border-purple-500/20">
                                                        Multi
                                                    </span>
                                                )}
                                                <span className={`flex h-3 w-3 rounded-full ${
                                                    item.qty <= 0 ? 'bg-rose-500' : item.qty < 5 ? 'bg-amber-500' : 'bg-emerald-500'
                                                }`} />
                                            </div>
                                        </div>

                                        {/* Name */}
                                        <h3 className={`font-bold text-lg text-zinc-900 dark:text-white mb-1 line-clamp-2 leading-snug transition-colors relative z-10 
                                            ${item.multiPrice === 1 ? 'group-hover:text-emerald-600 dark:group-hover:text-emerald-400' : ''}`}>
                                            {item.name}
                                        </h3>
                                        <p className="text-zinc-500 dark:text-zinc-400 text-sm mb-4 relative z-10 truncate transition-colors">{item.supplier || 'Standard Item'}</p>

                                        {/* ── main_item price grid ── */}
                                        <div className="relative z-10 bg-zinc-50 dark:bg-zinc-950/50 rounded-xl border border-zinc-200 dark:border-white/5 p-3 mb-4 grid grid-cols-3 divide-x divide-zinc-200 dark:divide-zinc-800 transition-colors">
                                            <div className="text-center px-1">
                                                <p className="text-[10px] uppercase font-bold text-zinc-500 tracking-wider mb-1.5">Selling</p>
                                                <p className="text-emerald-600 dark:text-emerald-400 font-bold text-base font-mono leading-none">
                                                    {item.sellingPrice != null ? `${Number(item.sellingPrice).toLocaleString()}` : '—'}
                                                </p>
                                            </div>
                                            <div className="text-center px-1">
                                                <p className="text-[10px] uppercase font-bold text-zinc-500 tracking-wider mb-1.5">Wholesale</p>
                                                <p className="text-blue-600 dark:text-blue-400 font-bold text-base font-mono leading-none">
                                                    {item.wholesalePrice != null ? `${Number(item.wholesalePrice).toLocaleString()}` : '—'}
                                                </p>
                                            </div>
                                            <div className="text-center px-1">
                                                <p className="text-[10px] uppercase font-bold text-zinc-500 tracking-wider mb-1.5">Cost</p>
                                                <p className="text-amber-600 dark:text-amber-400 font-bold text-base font-mono leading-none">
                                                    {item.unitPrice != null ? `${Number(item.unitPrice).toLocaleString()}` : '—'}
                                                </p>
                                            </div>
                                        </div>

                                        {/* Stock + expand hint */}
                                        <div className="flex items-center justify-between mt-auto relative z-10 pt-3 border-t border-zinc-200 dark:border-zinc-800/60 transition-colors">
                                            <div>
                                                <span className="text-[11px] uppercase font-bold text-zinc-500 tracking-wider mr-2">Stock</span>
                                                <span className={`font-mono font-bold text-base ${item.qty <= 0 ? 'text-rose-500 dark:text-rose-400' : 'text-zinc-900 dark:text-zinc-300'}`}>
                                                    {item.qty ?? 0}
                                                </span>
                                                <span className="text-zinc-500 text-sm ml-1.5">units</span>
                                            </div>
                                            {item.multiPrice === 1 && (
                                                <div className="text-zinc-500 text-[13px] flex items-center gap-1 font-medium">
                                                    {expandedItemId === item.id
                                                        ? <><ChevronUpIcon className="w-4 h-4 text-emerald-600 dark:text-emerald-500" /><span className="text-emerald-600 dark:text-emerald-500">Hide</span></>
                                                        : <><ChevronDownIcon className="w-4 h-4" /><span>Batches</span></>
                                                    }
                                                </div>
                                            )}
                                        </div>
                                    </div>

                                    {/* ── Batch expansion (main_item_multiple) ── */}
                                    <AnimatePresence>
                                        {expandedItemId === item.id && (
                                            <motion.div
                                                initial={{ height: 0, opacity: 0 }}
                                                animate={{ height: 'auto', opacity: 1 }}
                                                exit={{ height: 0, opacity: 0 }}
                                                transition={{ duration: 0.22, ease: 'easeInOut' }}
                                                className="overflow-hidden bg-zinc-950/70 border border-emerald-500/20 border-t-0 rounded-b-2xl"
                                            >
                                                <div className="p-4 border-t-0 bg-zinc-50/90 dark:bg-zinc-950/70 border border-emerald-500/40 dark:border-emerald-500/20 rounded-b-2xl shadow-inner transition-colors">
                                                    {/* Section header */}
                                                    <div className="flex items-center gap-2 mb-4">
                                                        <ArchiveBoxIcon className="w-4 h-4 text-emerald-600 dark:text-emerald-500 shrink-0" />
                                                        <span className="text-xs uppercase font-bold tracking-widest text-emerald-600 dark:text-emerald-500">
                                                            Batch Prices
                                                        </span>
                                                        {loadingMultiples && !multiplesData[item.id] && (
                                                            <ArrowPathIcon className="w-4 h-4 animate-spin text-zinc-400 dark:text-zinc-600 ml-auto" />
                                                        )}
                                                    </div>

                                                    {multiplesData[item.id] === undefined ? (
                                                        /* Loading */
                                                        <div className="flex justify-center py-5">
                                                            <ArrowPathIcon className="w-5 h-5 animate-spin text-zinc-400 dark:text-zinc-600" />
                                                        </div>
                                                    ) : multiplesData[item.id].length === 0 ? (
                                                        /* Empty */
                                                        <p className="text-center py-4 text-zinc-500 text-sm">
                                                            No batch records for this item
                                                        </p>
                                                    ) : (
                                                        /* Batch rows */
                                                        <div className="space-y-2">
                                                            {/* Column headers */}
                                                            <div className="grid grid-cols-3 gap-2 px-2 pb-1 text-[11px] uppercase font-bold text-zinc-500 tracking-wider">
                                                                <span>Batch Price</span>
                                                                <span className="text-center">Qty</span>
                                                                <span className="text-right">Unit Cost</span>
                                                            </div>

                                                            {/* Rows */}
                                                            <div className="space-y-2">
                                                                {multiplesData[item.id].map((batch, idx) => (
                                                                    <div key={idx} className="flex flex-col bg-white dark:bg-white/[0.04] border border-zinc-200 dark:border-white/5 rounded-lg overflow-hidden shadow-sm dark:shadow-none transition-colors">
                                                                        <div className="bg-zinc-100 dark:bg-black/20 px-2.5 py-1.5 border-b border-zinc-200 dark:border-white/5 text-[10px] text-zinc-500 font-sans tracking-wide transition-colors">
                                                                            Added: <span className="text-zinc-700 dark:text-zinc-300 font-medium ml-1">{formatDate(batch.date)}</span>
                                                                        </div>
                                                                        <div className="grid grid-cols-3 gap-2 items-center px-2 py-2 text-sm font-mono">
                                                                            <span className="text-emerald-600 dark:text-emerald-400 font-bold">
                                                                                Rs.{batch.price != null ? Number(batch.price).toLocaleString() : '—'}
                                                                            </span>
                                                                            <span className="text-center">
                                                                                <span className={`px-2.5 py-0.5 rounded-md text-xs font-bold ${
                                                                                    (batch.qty ?? 0) > 0
                                                                                        ? 'bg-emerald-100 dark:bg-emerald-500/15 text-emerald-700 dark:text-emerald-400'
                                                                                        : 'bg-zinc-200 dark:bg-zinc-800 text-zinc-500 dark:text-zinc-500'
                                                                                }`}>
                                                                                    {batch.qty ?? 0}
                                                                                </span>
                                                                            </span>
                                                                            <span className="text-right text-amber-600 dark:text-amber-400/80">
                                                                                {batch.unitPrice != null && batch.unitPrice > 0
                                                                                    ? `Rs.${Number(batch.unitPrice).toLocaleString()}`
                                                                                    : '—'}
                                                                            </span>
                                                                        </div>
                                                                    </div>
                                                                ))}
                                                            </div>

                                                            {/* Summary */}
                                                            <div className="flex justify-between items-center px-2 pt-3 border-t border-zinc-200 dark:border-white/10 text-xs transition-colors">
                                                                <span className="text-zinc-500">
                                                                    {multiplesData[item.id].length} batch{multiplesData[item.id].length !== 1 ? 'es' : ''}
                                                                </span>
                                                                <span className="text-zinc-600 dark:text-zinc-400 font-semibold">
                                                                    Total {multiplesData[item.id].reduce((a, b) => a + (b.qty ?? 0), 0)} units
                                                                </span>
                                                            </div>
                                                        </div>
                                                    )}
                                                </div>
                                            </motion.div>
                                        )}
                                    </AnimatePresence>

                                </motion.div>
                            ))}
                        </motion.div>
                    </>
                )}
            </div>

                {/* ── Pagination ── */}
                {totalPages > 1 && (
                    <div className="flex justify-center mt-4 shrink-0 pb-1">
                        <div className="flex items-center gap-2 bg-white dark:bg-zinc-900/50 border border-zinc-200 dark:border-zinc-800 rounded-2xl p-1.5 shadow-lg transition-colors">
                            <button
                                disabled={page === 0}
                                onClick={() => setPage(p => p - 1)}
                                className="px-4 py-2 rounded-xl text-sm font-medium bg-zinc-100 dark:bg-zinc-800 text-zinc-700 dark:text-zinc-300 disabled:opacity-40 disabled:cursor-not-allowed hover:bg-zinc-200 dark:hover:bg-zinc-700 transition"
                            >
                                ← Prev
                            </button>
                            <span className="px-3 font-mono text-sm text-zinc-500 dark:text-zinc-400">
                                <span className="text-zinc-900 dark:text-white">{page + 1}</span>
                                <span className="mx-1 text-zinc-400 dark:text-zinc-600">/</span>
                                <span className="text-zinc-900 dark:text-white">{totalPages}</span>
                            </span>
                            <button
                                disabled={page === totalPages - 1}
                                onClick={() => setPage(p => p + 1)}
                                className="px-4 py-2 rounded-xl text-sm font-medium bg-zinc-100 dark:bg-zinc-800 text-zinc-700 dark:text-zinc-300 disabled:opacity-40 disabled:cursor-not-allowed hover:bg-zinc-200 dark:hover:bg-zinc-700 transition"
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

export default InventoryExplorer;
