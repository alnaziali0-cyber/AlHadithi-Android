const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const OWNER_UID = "owner_ali_uid";
const OTHER_UID = "intruder_bob_uid";
const OWNER_EMAIL = "alnaziali0@gmail.com";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated caller cannot read or create announcements", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("admin_announcements").get());
  await assertFails(
    unauthDb.collection("admin_announcements").doc("ad_1").set({
      title: "إعلان",
      body: "تفاصيل الإعلان",
      badgeLabel: "تنبيه",
      isActive: true,
      userId: OWNER_UID,
      authorEmail: OWNER_EMAIL,
      createdAt: new Date(),
      updatedAt: new Date(),
    })
  );
});

test("Authenticated non-owner email (even verified) is strictly rejected", async () => {
  const otherDb = testEnv
    .authenticatedContext(OTHER_UID, {
      email: "someoneelse@gmail.com",
      email_verified: true,
    })
    .firestore();

  await assertFails(
    otherDb.collection("admin_announcements").doc("ad_intruder").set({
      title: "إعلان غير مصرح",
      body: "محاولة دخول بحساب آخر",
      badgeLabel: "تنبيه",
      isActive: true,
      userId: OTHER_UID,
      authorEmail: "someoneelse@gmail.com",
      createdAt: new Date(),
      updatedAt: new Date(),
    })
  );
});

test("Email spoofing guard: unverified alnaziali0@gmail.com token is rejected", async () => {
  const spoofDb = testEnv
    .authenticatedContext(OWNER_UID, {
      email: OWNER_EMAIL,
      email_verified: false,
    })
    .firestore();

  await assertFails(
    spoofDb.collection("admin_announcements").doc("ad_spoof").set({
      title: "إعلان مزيف",
      body: "البريد غير مؤكد",
      badgeLabel: "تنبيه",
      isActive: true,
      userId: OWNER_UID,
      authorEmail: OWNER_EMAIL,
      createdAt: new Date(),
      updatedAt: new Date(),
    })
  );
});

test("Verified alnaziali0@gmail.com owner can create, query with userId filter, update, and delete", async () => {
  const ownerContext = testEnv.authenticatedContext(OWNER_UID, {
    email: OWNER_EMAIL,
    email_verified: true,
  });
  const ownerDb = ownerContext.firestore();
  const { serverTimestamp } = testEnv;

  const now = new Date(Date.now() - 1000);
  await assertSucceeds(
    ownerDb.collection("admin_announcements").doc("ad_valid_1").set({
      title: "مجلس حسيني مركزي",
      body: "يُقام مجلس العزاء الليلة بعد صلاة المغرب والعشاء",
      badgeLabel: "مجلس حسيني",
      actionUrl: "https://alhadithi.iq",
      isActive: true,
      userId: OWNER_UID,
      authorEmail: OWNER_EMAIL,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Query aligned with userId filter succeeds
  await assertSucceeds(
    ownerDb.collection("admin_announcements").where("userId", "==", OWNER_UID).get()
  );

  // Unfiltered list fails (rules are not filters)
  await assertFails(ownerDb.collection("admin_announcements").get());

  // Shadow update with ghost field fails
  await assertFails(
    ownerDb.collection("admin_announcements").doc("ad_valid_1").update({
      title: "عنوان محدث",
      ghostAdminFlag: true,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Valid update succeeds
  await assertSucceeds(
    ownerDb.collection("admin_announcements").doc("ad_valid_1").update({
      isActive: false,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Delete succeeds
  await assertSucceeds(
    ownerDb.collection("admin_announcements").doc("ad_valid_1").delete()
  );
});


test("Owner can publish an official prayer record and public clients can read it with the required query", async () => {
  const ownerDb = testEnv.authenticatedContext(OWNER_UID, {
    email: OWNER_EMAIL,
    email_verified: true,
  }).firestore();
  const now = new Date(Date.now() - 1000);
  const record = {
    governorateId: "karbala",
    dateKey: "20261110",
    fajr: "05:02",
    sunrise: "06:28",
    dhuhr: "11:48",
    asr: "14:47",
    maghrib: "17:08",
    isha: "18:29",
    midnight: "23:48",
    sourceUrl: "https://imamhussain.org/prayer-times/",
    sourceRecordId: "official-karbala-20261110",
    updatedAt: now,
    verifiedAt: now,
    timeZoneId: "Asia/Baghdad",
    sourceCode: "imamhussain_official",
    status: "published",
  };
  await assertSucceeds(ownerDb.collection("prayer_times_daily").doc("karbala_20261110").set(record));

  const publicDb = testEnv.unauthenticatedContext().firestore();
  await assertSucceeds(
    publicDb.collection("prayer_times_daily")
      .where("governorateId", "==", "karbala")
      .where("sourceCode", "==", "imamhussain_official")
      .where("status", "==", "published")
      .where("timeZoneId", "==", "Asia/Baghdad")
      .where("dateKey", ">=", "20261110")
      .where("dateKey", "<=", "20261110")
      .get()
  );
});

test("Prayer schedule writes reject non-official source or invalid chronology", async () => {
  const ownerDb = testEnv.authenticatedContext(OWNER_UID, {
    email: OWNER_EMAIL,
    email_verified: true,
  }).firestore();
  const now = new Date(Date.now() - 1000);
  const invalid = {
    governorateId: "karbala", dateKey: "20261110",
    fajr: "05:02", sunrise: "06:28", dhuhr: "11:48", asr: "14:47",
    maghrib: "17:08", isha: "16:29", midnight: "23:48",
    sourceUrl: "https://example.com/fake-times", sourceRecordId: "fake-record",
    updatedAt: now, verifiedAt: now, timeZoneId: "Asia/Baghdad",
    sourceCode: "calculated", status: "published",
  };
  await assertFails(ownerDb.collection("prayer_times_daily").doc("karbala_20261110").set(invalid));
});
