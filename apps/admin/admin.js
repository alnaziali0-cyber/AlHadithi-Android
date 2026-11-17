(() => {
  'use strict';
  const $ = (selector, root = document) => root.querySelector(selector);
  const loginView = $('#login-view');
  const dashboardView = $('#dashboard-view');
  const loginForm = $('#login-form');
  const loginError = $('#login-error');
  const rotation = $('#password-rotation');
  let csrfToken = readCookie('kh_admin_csrf');

  function readCookie(name) {
    const entry = document.cookie.split('; ').find(part => part.startsWith(`${name}=`));
    return entry ? decodeURIComponent(entry.slice(name.length + 1)) : '';
  }
  function element(tag, text, className) {
    const node = document.createElement(tag);
    if (text !== undefined) node.textContent = String(text);
    if (className) node.className = className;
    return node;
  }
  async function api(path, options = {}) {
    const method = (options.method || 'GET').toUpperCase();
    const headers = new Headers(options.headers || {});
    if (method !== 'GET' && method !== 'HEAD') {
      csrfToken = csrfToken || readCookie('kh_admin_csrf');
      if (csrfToken) headers.set('X-CSRF-Token', csrfToken);
    }
    const response = await fetch(path, { credentials: 'same-origin', ...options, headers });
    const type = response.headers.get('content-type') || '';
    const body = type.includes('application/json') ? await response.json() : await response.text();
    if (!response.ok) {
      const detail = body && typeof body === 'object' ? body.detail : body;
      const message = Array.isArray(detail)
        ? detail.map(item => item && typeof item === 'object' ? (item.msg || JSON.stringify(item)) : String(item)).join('؛ ')
        : (typeof detail === 'string' ? detail : (detail ? JSON.stringify(detail) : 'تعذر تنفيذ الطلب'));
      throw new Error(message);
    }
    return body;
  }
  function showDashboard(user) {
    loginView.hidden = true;
    dashboardView.hidden = false;
    $('#current-admin').textContent = user.username;
    rotation.hidden = !user.force_password_change;
    $('.tabs').hidden = Boolean(user.force_password_change);
    document.querySelectorAll('.panel').forEach(panel => { panel.hidden = Boolean(user.force_password_change) || panel.id !== 'panel-overview'; });
    document.querySelectorAll('.tab').forEach(tab => tab.addEventListener('click', () => activatePanel(tab.dataset.panel)));
    if (!user.force_password_change) {
      if (window.location.hash === '#wallpapers') activatePanel('wallpapers');
      else loadOverview();
    }
  }
  function activatePanel(name) {
    document.querySelectorAll('.tab').forEach(tab => tab.classList.toggle('active', tab.dataset.panel === name));
    document.querySelectorAll('.panel').forEach(panel => panel.hidden = panel.id !== `panel-${name}`);
    const loaders = { overview: loadOverview, content: loadContent, catalog: loadCatalog, wallpapers: loadWallpapers, radio: loadRadio, media: loadMedia, audit: loadAudit, services: loadServices };
    if (loaders[name] && rotation.hidden) loaders[name]();
  }
  loginForm.addEventListener('submit', async event => {
    event.preventDefault();
    loginError.textContent = '';
    const data = new FormData(loginForm);
    try {
      const result = await api('/api/v1/admin/auth/login', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ username: data.get('username'), password: data.get('password') }) });
      csrfToken = result.csrf_token;
      showDashboard(result.user);
    } catch (error) { loginError.textContent = error.message; }
  });
  $('#password-form').addEventListener('submit', async event => {
    event.preventDefault();
    const form = new FormData(event.currentTarget);
    try {
      await api('/api/v1/admin/auth/change-password', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ current_password: form.get('current_password'), new_password: form.get('new_password') }) });
      $('#password-message').textContent = 'تم تحديث كلمة المرور. أعد تسجيل الدخول بكلمتك الجديدة.';
      event.currentTarget.reset();
      window.setTimeout(() => window.location.reload(), 800);
    } catch (error) { $('#password-message').textContent = error.message; }
  });
  $('#logout-btn').addEventListener('click', async () => {
    try { await api('/api/v1/admin/auth/logout', { method: 'POST' }); } catch (_) { /* Session may already be invalid. */ }
    window.location.reload();
  });
  async function loadOverview() {
    const data = await api('/api/v1/admin/overview');
    const labels = { governorates: 'المحافظات', content_total: 'إجمالي المنشورات', content_published: 'منشورات منشورة', media_assets: 'ملفات الوسائط', quran_surahs: 'السور المستوردة', quran_ayahs: 'الآيات المستوردة', admin_users: 'الحسابات الإدارية' };
    const grid = $('#stats-grid'); grid.replaceChildren();
    Object.entries(labels).forEach(([key, label]) => {
      const card = element('div', undefined, 'stat');
      card.append(element('strong', data[key] ?? '—'), element('span', label)); grid.append(card);
    });
  }
  async function loadWallpapers() {
    const host = $('#wallpapers-list');
    const message = $('#wallpapers-message');
    host.replaceChildren();
    message.textContent = 'جارٍ تحميل سجل الصور المضمّنة…';
    try {
      const result = await api('/api/v1/admin/wallpapers');
      const rows = (result.items || []).map(item => {
        const action = element('button', item.published ? 'إخفاء' : 'نشر', 'small-btn');
        action.addEventListener('click', async () => {
          action.disabled = true;
          try {
            await api(`/api/v1/admin/wallpapers/${encodeURIComponent(item.id)}`, {
              method: 'PUT',
              headers: { 'Content-Type': 'application/json' },
              body: JSON.stringify({ published: !item.published })
            });
            message.textContent = item.published
              ? 'أُخفيت الخلفية. ستنعكس الحالة في التطبيق عند مزامنة الكتالوج.'
              : 'نُشرت الخلفية. ستظهر للمستخدمين عند مزامنة الكتالوج.';
            await loadWallpapers();
          } catch (error) {
            message.textContent = error.message;
            action.disabled = false;
          }
        });
        return [item.title, item.creator, item.license, item.published ? 'منشورة' : 'مسودة مخفية', action];
      });
      host.append(makeTable(['الخلفية', 'المصور', 'الرخصة', 'الحالة', 'إجراء'], rows));
      if (!rows.length) message.textContent = 'لا توجد صور معرفة في كتالوج الأصول المحلي.';
      else message.textContent = 'لا يمكن لهذه اللوحة رفع صور جديدة؛ أضيفي الصور المرخصة إلى أصول التطبيق ثم راجعيها قبل الإصدار.';
    } catch (error) {
      message.textContent = `تعذر تحميل الخلفيات: ${error.message}`;
    }
  }
  function makeTable(headers, rows) {
    const table = element('table', undefined, 'data-table');
    const thead = element('thead'); const tr = element('tr');
    headers.forEach(label => tr.append(element('th', label))); thead.append(tr); table.append(thead);
    const tbody = element('tbody');
    rows.forEach(values => { const row = element('tr'); values.forEach(value => { const td = element('td'); if (value instanceof Node) td.append(value); else td.textContent = String(value ?? '—'); row.append(td); }); tbody.append(row); });
    table.append(tbody); return table;
  }
  async function loadContent() {
    const data = await api('/api/v1/admin/content');
    const host = $('#content-list'); host.replaceChildren();
    host.append(makeTable(['المعرّف', 'النوع', 'العنوان', 'الحالة', 'آخر تحديث', 'إجراء'], data.map(item => {
      const archive = element('button', 'أرشفة', 'small-btn');
      archive.addEventListener('click', async () => { if (!window.confirm('أرشفة هذا المنشور؟')) return; try { await api(`/api/v1/admin/content/${item.id}`, { method: 'DELETE' }); loadContent(); } catch (error) { window.alert(error.message); } });
      return [item.id, item.content_type, item.title, item.status, new Date(item.updated_at).toLocaleString('ar-IQ'), archive];
    })));
  }
  $('#content-form').addEventListener('submit', async event => {
    event.preventDefault(); const form = new FormData(event.currentTarget);
    const value = Object.fromEntries(form.entries());
    value.starts_at = value.starts_at ? new Date(value.starts_at).toISOString() : null;
    value.ends_at = value.ends_at ? new Date(value.ends_at).toISOString() : null;
    value.image_asset_id = null;
    try { await api('/api/v1/admin/content', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(value) }); event.currentTarget.reset(); await loadContent(); window.alert('تم حفظ المنشور.'); }
    catch (error) { window.alert(error.message); }
  });
  const catalogType = $('#catalog-type');
  const catalogId = $('#catalog-record-id');
  const catalogJson = $('#catalog-json');
  const catalogMessage = $('#catalog-message');
  const catalogNames = {
    book: 'كتاب', hadith_source: 'مصدر حديثي', hadith: 'رواية', dua: 'دعاء',
    visit: 'زيارة', event: 'مناسبة', ethics_lesson: 'درس أخلاقي', audio: 'ملف صوتي'
  };
  function catalogRecordBody(item) {
    const body = { ...item };
    delete body.id;
    delete body.gradings;
    delete body.created_at;
    return body;
  }
  function resetCatalogEditor() {
    catalogId.value = '';
    catalogJson.value = '{\n  "publication_status": "draft"\n}';
    catalogMessage.textContent = 'سجل جديد. راجع الحقول المطلوبة لنوع السجل قبل الحفظ.';
  }
  async function loadCatalog() {
    const type = catalogType.value;
    const host = $('#catalog-list');
    host.replaceChildren();
    try {
      const result = await api(`/api/v1/admin/catalog/${encodeURIComponent(type)}?limit=100&offset=0`);
      host.append(makeTable(['المعرّف', 'العنوان', 'الحالة', 'إجراء'], (result.items || []).map(item => {
        const title = item.title || item.speaker || item.slug || `سجل ${item.id}`;
        const status = item.publication_status || (type === 'hadith_source' ? 'مرجع ببليوغرافي' : (item.is_published ? 'منشور' : 'مسودة'));
        const edit = element('button', 'تحرير', 'small-btn');
        edit.addEventListener('click', () => {
          catalogId.value = String(item.id);
          catalogJson.value = JSON.stringify(catalogRecordBody(item), null, 2);
          if (type === 'book') {
            $('#book-pages-id').value = String(item.id);
            loadBookPages(item.id);
          }
          if (type === 'hadith') $('#grading-hadith-id').value = String(item.id);
          catalogMessage.textContent = `تحرير ${catalogNames[type]} رقم ${item.id}. احفظ لتطبيق التعديلات.`;
          catalogJson.focus();
        });
        const archive = element('button', 'أرشفة', 'small-btn');
        archive.addEventListener('click', async () => {
          if (!window.confirm(`أرشفة ${catalogNames[type]} رقم ${item.id}؟`)) return;
          try {
            await api(`/api/v1/admin/catalog/${encodeURIComponent(type)}/${item.id}`, { method: 'DELETE' });
            if (catalogId.value === String(item.id)) resetCatalogEditor();
            catalogMessage.textContent = 'تمت الأرشفة.';
            await loadCatalog();
          } catch (error) { catalogMessage.textContent = error.message; }
        });
        const actions = element('div');
        actions.append(edit, archive);
        return [item.id, title, status, actions];
      })));
      if (!result.items?.length) host.append(element('p', 'لا توجد سجلات في هذا القسم بعد.', 'muted'));
    } catch (error) {
      host.append(element('p', error.message, 'error'));
      catalogMessage.textContent = 'تعذر تحميل سجلات القسم.';
    }
  }
  catalogType.addEventListener('change', () => { resetCatalogEditor(); loadCatalog(); });
  $('#catalog-new').addEventListener('click', resetCatalogEditor);
  $('#catalog-form').addEventListener('submit', async event => {
    event.preventDefault();
    let payload;
    try {
      payload = JSON.parse(catalogJson.value);
      if (!payload || Array.isArray(payload) || typeof payload !== 'object') throw new Error('يجب إدخال كائن JSON');
    } catch (error) {
      catalogMessage.textContent = `JSON غير صالح: ${error.message}`;
      return;
    }
    const type = catalogType.value;
    const editingId = catalogId.value;
    const path = editingId
      ? `/api/v1/admin/catalog/${encodeURIComponent(type)}/${encodeURIComponent(editingId)}`
      : `/api/v1/admin/catalog/${encodeURIComponent(type)}`;
    try {
      const result = await api(path, {
        method: editingId ? 'PUT' : 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload)
      });
      catalogId.value = String(result.id);
      catalogJson.value = JSON.stringify(catalogRecordBody(result.record), null, 2);
      if (type === 'book') {
        $('#book-pages-id').value = String(result.id);
        await loadBookPages(result.id);
      }
      if (type === 'hadith') $('#grading-hadith-id').value = String(result.id);
      catalogMessage.textContent = `تم حفظ ${catalogNames[type]} رقم ${result.id}. راجع حالة النشر وحقوق الاستخدام.`;
      await loadCatalog();
    } catch (error) {
      catalogMessage.textContent = error.message;
    }
  });

  async function loadBookPages(bookId) {
    const host = $('#book-pages-list');
    host.replaceChildren();
    try {
      const result = await api(`/api/v1/admin/books/${encodeURIComponent(bookId)}/pages?limit=1000&offset=0`);
      host.append(makeTable(['المجلد', 'الصفحة', 'النص'], (result.items || []).map(page => [page.volume_number, page.page_number, page.text_content])));
      if (!result.items?.length) host.append(element('p', 'لا توجد صفحات مستوردة لهذا الكتاب.', 'muted'));
    } catch (error) {
      host.append(element('p', error.message, 'error'));
    }
  }
  $('#book-pages-form').addEventListener('submit', async event => {
    event.preventDefault();
    const bookId = $('#book-pages-id').value;
    let payload;
    try {
      payload = JSON.parse($('#book-pages-json').value);
      if (!payload || !Array.isArray(payload.pages)) throw new Error('يجب إدخال كائن JSON يحتوي مصفوفة pages');
    } catch (error) {
      $('#book-pages-message').textContent = `JSON غير صالح: ${error.message}`;
      return;
    }
    try {
      const result = await api(`/api/v1/admin/books/${encodeURIComponent(bookId)}/pages`, {
        method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(payload)
      });
      $('#book-pages-message').textContent = `تم استيراد ${result.inserted} صفحة إلى الكتاب ${bookId}.`;
      $('#book-pages-json').value = '';
      await loadBookPages(bookId);
    } catch (error) {
      $('#book-pages-message').textContent = error.message;
    }
  });
  $('#hadith-grading-form').addEventListener('submit', async event => {
    event.preventDefault();
    const hadithId = $('#grading-hadith-id').value;
    const form = new FormData(event.currentTarget);
    const payload = {
      grading: form.get('grading'), scholar: form.get('scholar'),
      methodology: form.get('methodology'), citation: form.get('citation')
    };
    try {
      const result = await api(`/api/v1/admin/hadith/${encodeURIComponent(hadithId)}/gradings`, {
        method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(payload)
      });
      $('#grading-message').textContent = `تمت إضافة التقييم رقم ${result.id} للرواية ${hadithId}.`;
      event.currentTarget.reset();
      $('#grading-hadith-id').value = hadithId;
    } catch (error) {
      $('#grading-message').textContent = error.message;
    }
  });

  async function loadRadio() {
    const data = await api('/api/v1/admin/radio');
    if (!data) return;
    const form = $('#radio-form');
    ['name','description','stream_url','official_source_url','rights_evidence'].forEach(key => form.elements[key].value = data[key] || '');
    form.elements.is_verified.checked = Boolean(data.is_verified);
    form.elements.is_enabled.checked = Boolean(data.is_enabled);
  }
  $('#radio-form').addEventListener('submit', async event => {
    event.preventDefault(); const form = event.currentTarget;
    const payload = { name: form.elements.name.value, description: form.elements.description.value, stream_url: form.elements.stream_url.value || null, official_source_url: form.elements.official_source_url.value || null, rights_evidence: form.elements.rights_evidence.value.trim() || null, is_verified: form.elements.is_verified.checked, is_enabled: form.elements.is_enabled.checked };
    try { const result = await api('/api/v1/admin/radio', { method: 'PUT', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(payload) }); $('#radio-message').textContent = `تم الحفظ. مفعّل: ${result.is_enabled ? 'نعم' : 'لا'}؛ موثّق: ${result.is_verified ? 'نعم' : 'لا'}.`; }
    catch (error) { $('#radio-message').textContent = error.message; }
  });
  async function loadMedia() {
    const data = await api('/api/v1/admin/media'); const host = $('#media-list'); host.replaceChildren();
    host.append(makeTable(['المعرّف','الملف','النوع','الحجم','SHA-256','تاريخ الإضافة'], data.map(item => [item.id,item.original_name,item.mime_type,`${item.byte_size} B`,item.sha256.slice(0,16)+'…',new Date(item.created_at).toLocaleString('ar-IQ')])));
  }
  $('#media-form').addEventListener('submit', async event => {
    event.preventDefault(); const form = new FormData(event.currentTarget);
    try { const result = await api('/api/v1/admin/media', { method: 'POST', body: form }); $('#media-message').textContent = `تم رفع ${result.original_name} وتسجيل بصمته SHA-256.`; event.currentTarget.reset(); loadMedia(); }
    catch (error) { $('#media-message').textContent = error.message; }
  });
  async function loadAudit() {
    const data = await api('/api/v1/admin/audit'); const host = $('#audit-list'); host.replaceChildren();
    host.append(makeTable(['العملية','نوع المورد','المعرّف','التفاصيل','الوقت'], data.map(row => [row.action,row.resource_type,row.resource_id,JSON.stringify(row.details),new Date(row.created_at).toLocaleString('ar-IQ')])));
  }
  async function loadServices() {
    const data = await api('/api/v1/admin/services'); const host = $('#services-list'); host.replaceChildren();
    Object.entries(data).forEach(([key,value]) => { const card=element('div',undefined,'stat'); card.append(element('strong',String(value), value==='ok'||value==='connected'||value==='available'||value==='ready'?'service-good':'service-warn'),element('span',key));host.append(card); });
  }
  (async () => {
    try { const user = await api('/api/v1/admin/auth/me'); csrfToken = readCookie('kh_admin_csrf'); showDashboard(user); }
    catch (_) { loginView.hidden = false; dashboardView.hidden = true; }
  })();
})();
