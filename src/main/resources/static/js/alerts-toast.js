(function() {
    function initToastContainer() {
        let container = document.getElementById('toast-container');
        if (!container) {
            container = document.createElement('div');
            container.id = 'toast-container';
            container.className = 'fixed top-6 right-6 z-[9999] flex flex-col gap-3 max-w-sm w-full pointer-events-none';
            document.body.appendChild(container);
        }
    }

    function showToast(alert) {
        initToastContainer();
        const container = document.getElementById('toast-container');

        const toast = document.createElement('div');
        toast.className = 'bg-slate-900/95 dark:bg-slate-950/95 border border-slate-700/50 text-white rounded-xl shadow-2xl p-4 pointer-events-auto flex items-start gap-3 transition-all duration-300 transform translate-x-full opacity-0';

        const isAbove = alert.conditionType === 'ABOVE';
        const badgeBg = isAbove ? 'bg-green-500/20 text-green-400' : 'bg-red-500/20 text-red-400';
        const icon = isAbove ? '📈' : '📉';
        const conditionText = isAbove ? 'wzrósł powyżej' : 'spadł poniżej';

        toast.innerHTML = `
            <div class="w-10 h-10 rounded-full ${badgeBg} flex items-center justify-center text-lg shrink-0">
                ${icon}
            </div>
            <div class="flex-grow space-y-0.5">
                <div class="text-xs font-bold uppercase tracking-wider text-slate-400">Alert Cenowy</div>
                <div class="text-sm font-semibold text-white">
                    ${alert.symbol} ${conditionText}
                </div>
                <div class="text-xs text-slate-300">
                    Cena docelowa: <span class="font-bold text-qv-accent">$${alert.targetPrice.toLocaleString('en-US', { minimumFractionDigits: 2 })}</span>
                </div>
            </div>
            <button class="text-slate-400 hover:text-white text-lg font-bold leading-none select-none focus:outline-none">&times;</button>
        `;

        toast.querySelector('button').addEventListener('click', () => {
            dismissToast(toast);
        });

        container.appendChild(toast);

        requestAnimationFrame(() => {
            toast.classList.remove('translate-x-full', 'opacity-0');
        });

        setTimeout(() => {
            dismissToast(toast);
        }, 8000);
    }

    function dismissToast(toast) {
        toast.classList.add('translate-x-full', 'opacity-0');
        setTimeout(() => {
            toast.remove();
        }, 300);
    }

    async function checkAlerts() {
        const tokenMeta = document.querySelector('meta[name="_csrf"]');
        const headerMeta = document.querySelector('meta[name="_csrf_header"]');
        const token = tokenMeta ? tokenMeta.content : '';
        const header = headerMeta ? headerMeta.content : '';

        const headers = {
            'Content-Type': 'application/json'
        };
        if (header && token) {
            headers[header] = token;
        }

        try {
            const res = await fetch('/api/alerts/check', {
                method: 'POST',
                headers: headers,
                body: JSON.stringify({})
            });
            if (res.ok) {
                const triggered = await res.json();
                if (Array.isArray(triggered)) {
                    triggered.forEach(alert => {
                        showToast(alert);
                    });
                }
            }
        } catch (e) {
            console.error('Failed to check price alerts:', e);
        }
    }

    window.addEventListener('DOMContentLoaded', () => {
        initToastContainer();
        setTimeout(checkAlerts, 1500);
        setInterval(checkAlerts, 6000);
    });
})();
