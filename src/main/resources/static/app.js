async function submitTx() {
  const payload = {
    userId: USER_ID,
    type: document.getElementById('assetType').value,
    amount: parseLocaleNumber(document.getElementById('amount').value),
    currency: document.getElementById('currency').value || 'USD',
    price: parseLocaleNumber(document.getElementById('price').value),
    operationType: document.getElementById('operation').value,
    date: (function() {
      const v = document.getElementById('date')?.value;
      return v ? new Date(v).toISOString() : null;
    })()
  };

  if (!payload.amount || !payload.currency || !payload.price) {
    showMsg('Amount, currency and price are required', 'error');
    return;
  }

  const res = await fetch('/api/investments', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload)
  });
  if (!res.ok) {
    const msg = await res.text();
    showMsg('Failed: ' + msg, 'error');
    return;
  }
  document.getElementById('amount').value = '';
  document.getElementById('price').value = '';
  showMsg('Transaction recorded successfully', 'success');
  await loadPortfolio();
}

async function loadPortfolio() {
  const tbody = document.getElementById('portfolioBody');
  tbody.innerHTML = '';
  const res = await fetch(`/api/portfolio/${USER_ID}`);
  const data = await res.json();
  const valRes = await fetch(`/api/portfolio/${USER_ID}/values`);
  const values = await valRes.json();
  Object.keys(data).forEach(k => {
    const tr = document.createElement('tr');
    const td1 = document.createElement('td');
    const td2 = document.createElement('td');
    const td3 = document.createElement('td');
    td1.textContent = k;
    td2.textContent = formatNumber(data[k]);
    td3.textContent = values[k] != null ? formatNumber(values[k]) : '-';
    tr.appendChild(td1);
    tr.appendChild(td2);
    tr.appendChild(td3);
    tbody.appendChild(tr);
  });

  const chart = document.getElementById('chart');
  chart.src = `/api/portfolio/${USER_ID}/chart?ts=${Date.now()}`;
}

window.addEventListener('DOMContentLoaded', () => {
  loadPortfolio();
  if (window.$ && typeof $.fn.persianDatepicker === 'function') {
    $('#date-fa').persianDatepicker({
      format: 'YYYY/MM/DD HH:mm',
      timePicker: { enabled: true, meridiem: { enabled: false } },
      autoClose: true,
      initialValue: false,
      onSelect: function(unix) {
        try {
          const date = new Date(unix);
          document.getElementById('date').value = date.toISOString();
        } catch (_) {}
      }
    });

    $('#pnlStart-fa').persianDatepicker({
      format: 'YYYY/MM/DD HH:mm',
      timePicker: { enabled: true, meridiem: { enabled: false } },
      autoClose: true,
      initialValue: false,
      onSelect: function(unix) {
        try {
          const date = new Date(unix);
          document.getElementById('pnlStart').value = date.toISOString();
        } catch (_) {}
      }
    });

    $('#pnlEnd-fa').persianDatepicker({
      format: 'YYYY/MM/DD HH:mm',
      timePicker: { enabled: true, meridiem: { enabled: false } },
      autoClose: true,
      initialValue: false,
      onSelect: function(unix) {
        try {
          const date = new Date(unix);
          document.getElementById('pnlEnd').value = date.toISOString();
        } catch (_) {}
      }
    });
  }
});

function showMsg(text, type) {
  const el = document.getElementById('msg');
  el.textContent = text;
  el.className = 'notice ' + (type === 'error' ? 'error' : 'success');
  el.style.display = 'block';
  setTimeout(() => { el.style.display = 'none'; }, 4000);
}

async function loadPnl() {
  const out = document.getElementById('pnl');
  out.textContent = 'Loading...';
  const start = document.getElementById('pnlStart').value || new Date(Date.now() - 7*24*3600*1000).toISOString();
  const end = document.getElementById('pnlEnd').value || new Date().toISOString();
  const res = await fetch(`/api/portfolio/${USER_ID}/pnl?start=${encodeURIComponent(start)}&end=${encodeURIComponent(end)}`);
  if (!res.ok) { out.textContent = 'Failed to load P&L'; return; }
  const data = await res.json();
  out.innerHTML = '';
  const table = document.createElement('table');
  table.innerHTML = '<thead><tr><th>Asset</th><th>P&L (T)</th></tr></thead>';
  const tb = document.createElement('tbody');
  Object.keys(data).forEach(k => {
    const tr = document.createElement('tr');
    const a = document.createElement('td'); a.textContent = k;
    const b = document.createElement('td'); b.textContent = formatNumber(data[k]);
    tr.appendChild(a); tr.appendChild(b); tb.appendChild(tr);
  });
  table.appendChild(tb);
  out.appendChild(table);
}

async function loadValuation() {
  const el = document.getElementById('valuation');
  el.textContent = 'Loading...';
  const res = await fetch(`/api/portfolio/${USER_ID}/valuation`);
  if (!res.ok) {
    el.textContent = 'Failed to load valuation';
    return;
  }
  const v = await res.json();
  const roi = v.roiPercent != null ? `, ROI: ${formatNumber(v.roiPercent)}%` : '';
  el.textContent = `Initial: ${formatNumber(v.initialToman)} T, Current: ${formatNumber(v.currentToman)} T${roi}`;
}

function formatNumber(n) {
  try {
    const num = typeof n === 'number' ? n : parseFloat(n);
    if (isNaN(num)) return n;
    return num.toLocaleString('en-US');
  } catch (_) {
    return n;
  }
}

function parseLocaleNumber(value) {
  if (!value) return 0;
  // Remove common grouping separators and normalize decimal separator
  const normalized = value
    .toString()
    .replace(/[\u0660-\u0669]/g, d => String.fromCharCode(d.charCodeAt(0) - 0x0660 + 48)) // Arabic-Indic -> Latin
    .replace(/[\u06F0-\u06F9]/g, d => String.fromCharCode(d.charCodeAt(0) - 0x06F0 + 48)) // Eastern Arabic-Indic -> Latin
    .replace(/[,\s\u066C\u066B]/g, '') // remove commas, Arabic thousands (U+066C) and Arabic decimal (U+066B)
    .replace(/[\u066B\u060C]/g, '.')
    .replace(/٫/g, '.') // Arabic decimal mark
    .replace(/٬/g, ''); // Arabic thousands separator
  const num = parseFloat(normalized);
  return isNaN(num) ? 0 : num;
}

function attachInputFormatting(id) {
  const el = document.getElementById(id);
  if (!el) return;
  el.addEventListener('input', () => {
    const cursor = el.selectionStart;
    const raw = el.value.replace(/[^0-9.\-]/g, '');
    const parts = raw.split('.');
    const intPart = parts[0].replace(/\B(?=(\d{3})+(?!\d))/g, ',');
    const decPart = parts[1] ? '.' + parts[1].slice(0, 6) : '';
    el.value = intPart + decPart;
    try { el.setSelectionRange(cursor, cursor); } catch (_) {}
  });
  el.addEventListener('blur', () => {
    const num = parseLocaleNumber(el.value);
    el.value = formatNumber(num);
  });
}

// Attach formatting to amount and price inputs
attachInputFormatting('amount');
attachInputFormatting('price');

let currentPage = 0;
let totalPages = 0;

async function loadInvestmentsPage(direction) {
  if (direction === 'prev' && currentPage > 0) currentPage--;
  if (direction === 'next' && currentPage < totalPages - 1) currentPage++;
  const res = await fetch(`/api/investments/${USER_ID}/page?page=${currentPage}&size=10`);
  if (!res.ok) { showMsg('Failed to load investments', 'error'); return; }
  const page = await res.json();
  totalPages = page.totalPages;
  currentPage = page.number;
  document.getElementById('pageInfo').textContent = `Page ${currentPage + 1} / ${totalPages || 1}`;
  const tbody = document.getElementById('investmentsBody');
  tbody.innerHTML = '';
  page.content.forEach(inv => {
    const tr = document.createElement('tr');
    const tds = [
      inv.date ? new Date(inv.date).toLocaleString() : '-',
      inv.type,
      inv.operationType,
      formatNumber(inv.amount),
      inv.currency,
      formatNumber(inv.price)
    ];
    tds.forEach(val => { const td = document.createElement('td'); td.textContent = val; tr.appendChild(td); });
    tbody.appendChild(tr);
  });
}

// initial load
loadInvestmentsPage();


