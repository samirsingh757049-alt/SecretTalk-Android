const { initializeTestEnvironment, assertFails, assertSucceeds } = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

const PROJECT_ID = "demo-no-project";
const DATABASE_ID = "ai-studio-android-secretta-f2642449-1db1-40fa-bc2f-78653e604537";

let testEnv;

before(async () => {
  const rules = fs.readFileSync("firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: "127.0.0.1",
      port: 8085
    }
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

test("Unauthenticated access is denied on all collections", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore({ databaseId: DATABASE_ID });

  await assertFails(unauthDb.collection("system_config").doc("pairing").get());
  await assertFails(unauthDb.collection("users").doc("userA").get());
  await assertFails(unauthDb.collection("messages").doc("msg1").get());
  await assertFails(unauthDb.collection("calls").doc("call1").get());
});

test("User A can create initial pairing configuration", async () => {
  const userADb = testEnv.authenticatedContext("user_a").firestore({ databaseId: DATABASE_ID });
  const pairingRef = userADb.collection("system_config").doc("pairing");

  await assertSucceeds(
    pairingRef.set({
      userAId: "user_a",
      registeredCount: 1,
      isLocked: false,
      inviteCode: "SEC789"
    })
  );
});

test("User B cannot create duplicate pairing or set count > 2", async () => {
  const userBDb = testEnv.authenticatedContext("user_b").firestore({ databaseId: DATABASE_ID });
  const pairingRef = userBDb.collection("system_config").doc("pairing");

  // Attempting to create directly as user_b with invalid count
  await assertFails(
    pairingRef.set({
      userAId: "user_b",
      registeredCount: 3,
      isLocked: true
    })
  );
});

test("User can create their own profile and cannot edit other user profile", async () => {
  const userADb = testEnv.authenticatedContext("user_a").firestore({ databaseId: DATABASE_ID });
  const userBDb = testEnv.authenticatedContext("user_b").firestore({ databaseId: DATABASE_ID });

  await assertSucceeds(
    userADb.collection("users").doc("user_a").set({
      userId: "user_a",
      displayName: "Alice",
      secretCodeHash: "a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6",
      salt: "randomsalt123",
      role: "owner"
    })
  );

  // User B cannot edit User A's profile
  await assertFails(
    userBDb.collection("users").doc("user_a").update({
      displayName: "Hacked"
    })
  );
});

test("Message isolation: only sender and receiver can read messages", async () => {
  const userADb = testEnv.authenticatedContext("user_a").firestore({ databaseId: DATABASE_ID });
  const userBDb = testEnv.authenticatedContext("user_b").firestore({ databaseId: DATABASE_ID });
  const userCDb = testEnv.authenticatedContext("user_c").firestore({ databaseId: DATABASE_ID });

  const msgRef = userADb.collection("messages").doc("msg_123");
  await assertSucceeds(
    msgRef.set({
      senderId: "user_a",
      receiverId: "user_b",
      encryptedText: "Ciphertext12345==",
      mediaType: "text",
      status: "sent"
    })
  );

  // User B can read
  const msgDocForB = userBDb.collection("messages").doc("msg_123");
  await assertSucceeds(msgDocForB.get());

  // User C (eavesdropper) cannot read
  const msgDocForC = userCDb.collection("messages").doc("msg_123");
  await assertFails(msgDocForC.get());
});
