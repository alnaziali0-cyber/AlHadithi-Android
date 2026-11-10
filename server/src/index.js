/**
 * Alhadithi — خدمة إرسال الإشعارات (Firebase Cloud Messaging).
 *
 * تعمل هذه الخدمة كخلفية مستقلة عن التطبيق:
 *  ١) تقرأ الإعلانات التي طلب المطوّر إرسالها (sendPush = true و pushedAt = null).
 *  ٢) تجمّع الأجهزة المسجّلة من مجموعة notification_devices.
 *  ٣) ترسل إشعار FCM لكل جهاز، وتسجّل النتيجة الفعلية في notification_log.
 *  ٤) تحدّث pushedAt في الإعلان حتى لا يُرسل مرتين.
 *
 * لا تُعتبر الإشعار "مرسلاً" لمجرد الضغط على زر النشر: حالة الإرسال تُقرأ من
 * استجابة خدمة FCM وتُخزَّن في سجل العمليات.
 *
 * التشغيل:
 *   cd server && npm install
 *   cp .env.example .env   ثم املأ GOOGLE_APPLICATION_CREDENTIALS و FIREBASE_PROJECT_ID
 *   npm start
 */
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { initializeApp, applicationDefault, cert } from 'firebase-admin/app';
import { getFirestore, Timestamp } from 'firebase-admin/firestore';
import { getMessaging } from 'firebase-admin/messaging';

const __dirname = path.dirname(fileURLToPath(import.meta.url));

// ── الإعدادات ────────────────────────────────────────────────────────────────
const loadEnv = (file) => {
  if (!fs.existsSync(file)) return;
  for (const line of fs.readFileSync(file, 'utf8').split('\n')) {
    const m = line.match(/^\s*([A-Z0-9_]+)\s*=\s*(.*)\s*$/);
    if (!m) continue;
    const value = m[2].trim().replace(/^["']|["']$/g, '');
    if (process.env[m[1]] === undefined) process.env[m[1]] = value;
  }
};
loadEnv(path.join(__dirname, '.env'));

const PROJECT_ID = process.env.FIREBASE_PROJECT_ID || 'gen-lang-client-0008531256';
const DATABASE_ID = process.env.FIRESTORE_DATABASE_ID || 'ai-studio-android-0ccb0e51-02de-4d38-94ef-54f1255a8b5d';
const CREDENTIALS = process.env.GOOGLE_APPLICATION_CREDENTIALS;
const POLL_INTERVAL_MS = Number(process.env.POLL_INTERVAL_MS || 60_000);
const DRY_RUN = process.env.DRY_RUN === 'true';

const app = CREDENTIALS
  ? initializeApp({ credential: cert(CREDENTIALS), projectId: PROJECT_ID })
  : initializeApp({ credential: applicationDefault(), projectId: PROJECT_ID });

const db = getFirestore(app, DATABASE_ID);
const messaging = getMessaging(app);

// ── أدوات ───────────────────────────────────────────────────────────────────
const log = (level, message, meta = {}) =>
  console.log(JSON.stringify({ t: new Date().toISOString(), level, message, ...meta }));

async function devicesForTarget(ad) {
  let query = db.collection('notification_devices');
  if (ad.target === 'role' && ad.targetRole) {
    // الأدوار لا تُخزَّن على الأجهزة؛ نُبقي كل الأجهزة المسجّلة لهذه الفئة.
    query = query.where('platform', '==', 'android');
  }
  const snapshot = await query.get();
  return snapshot.docs.map((doc) => ({ id: doc.id, ...doc.data() }));
}

async function logDelivery({ userId, deviceId, kind, status, title, body, providerStatus, providerMessage, dedupeKey }) {
  await db.collection('notification_log').add({
    userId: userId || '',
    deviceId: deviceId || '',
    kind,
    status,
    title: title || '',
    body: body || '',
    providerStatus: providerStatus ?? null,
    providerMessage: providerMessage || '',
    dedupeKey: dedupeKey || null,
    createdAt: Timestamp.now(),
  });
}

/** إرسال إعلان واحد إلى كل الأجهزة المستهدفة. */
export async function pushAd(ad) {
  const devices = await devicesForTarget(ad);
  let sent = 0;
  let failed = 0;

  for (const device of devices) {
    const token = device.token || device.fcmToken;
    if (!token) continue;
    const dedupeKey = `ad:${ad.id}:${device.id}`;
    const existing = await db.collection('notification_log')
      .where('dedupeKey', '==', dedupeKey).limit(1).get();
    if (!existing.empty) continue; // لا تكرار

    if (DRY_RUN) {
      sent += 1;
      await logDelivery({
        userId: device.userId, deviceId: device.id, kind: 'ad', status: 'dry_run',
        title: ad.title, body: ad.shortDesc || ad.text, dedupeKey,
      });
      continue;
    }

    try {
      await messaging.send({
        token,
        notification: { title: ad.title, body: ad.shortDesc || ad.text },
        data: {
          kind: 'ad',
          screen: 'ads',
          adId: String(ad.id || ''),
          title: ad.title,
          body: ad.shortDesc || ad.text,
          takhrij: '',
        },
        android: {
          priority: 'normal',
          notification: {
            channelId: 'alhadithi_ads',
            icon: 'ic_notification_alhadithi',
            // الشعار الرسمي يظهر كصورة كبيرة داخل الإشعار على أندرويد.
            image: ad.imagePath || undefined,
          },
        },
      });
      sent += 1;
      await logDelivery({
        userId: device.userId, deviceId: device.id, kind: 'ad', status: 'sent',
        title: ad.title, body: ad.shortDesc || ad.text, providerStatus: 200, dedupeKey,
      });
    } catch (error) {
      failed += 1;
      const status = error?.code || 'unknown';
      await logDelivery({
        userId: device.userId, deviceId: device.id, kind: 'ad', status: 'failed',
        title: ad.title, body: ad.shortDesc || ad.text,
        providerStatus: status, providerMessage: error?.message || '', dedupeKey,
      });
      // رمز غير صالح أو جهاز لم يعد مسجّلاً: نُزيله من السجل.
      if (status === 'messaging/registration-token-not-registered' || status === 'messaging/invalid-argument') {
        await db.collection('notification_devices').doc(device.id).delete();
        log('warn', 'device removed after invalid token', { deviceId: device.id });
      }
    }
  }
  return { sent, failed, devices: devices.length };
}

/** يمسح الإعلانات التي طلبت إرسال إشعار ولم تُرسل بعد. */
export async function processPendingAds() {
  const snapshot = await db.collection('ads')
    .where('sendPush', '==', true)
    .where('pushedAt', '==', null)
    .limit(10)
    .get();

  if (snapshot.empty) return { processed: 0 };

  let processed = 0;
  for (const doc of snapshot.docs) {
    const ad = { id: doc.id, ...doc.data() };
    try {
      const result = await pushAd(ad);
      await doc.ref.update({ pushedAt: Timestamp.now(), pushResult: result });
      processed += 1;
      log('info', 'ad push completed', { adId: ad.id, ...result });
    } catch (error) {
      log('error', 'ad push failed', { adId: ad.id, message: error?.message });
    }
  }
  return { processed };
}

async function main() {
  if (!CREDENTIALS && !DRY_RUN) {
    console.error('❌ يجب ضبط GOOGLE_APPLICATION_CREDENTIALS (ملف حساب الخدمة) قبل التشغيل.');
    process.exit(1);
  }
  log('info', 'alhadithi notification service started', {
    projectId: PROJECT_ID, databaseId: DATABASE_ID, dryRun: DRY_RUN, pollIntervalMs: POLL_INTERVAL_MS,
  });
  const tick = async () => {
    try {
      await processPendingAds();
    } catch (error) {
      log('error', 'poll failed', { message: error?.message });
    }
  };
  await tick();
  setInterval(tick, POLL_INTERVAL_MS);
}

if (process.argv[1] && process.argv[1].endsWith('index.js')) {
  main().catch((error) => {
    log('error', 'fatal', { message: error?.message });
    process.exit(1);
  });
}
