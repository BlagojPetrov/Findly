const functions = require("firebase-functions/v1");
const admin = require("firebase-admin");

const REGION = "europe-west3";

admin.initializeApp();

const db = admin.firestore();

// ─────────────────────────────────────────────
// TRIGGER 1: Send notification on new message
// ─────────────────────────────────────────────
exports.onNewMessage = functions.region(REGION).firestore
    .document("conversations/{conversationId}/messages/{messageId}")
    .onCreate(async (snap, context) => {
        const message = snap.data();
        const { conversationId } = context.params;

        if (!message) return null;

        const conversationSnap = await db
            .collection("conversations")
            .doc(conversationId)
            .get();

        const conversation = conversationSnap.data();
        if (!conversation) return null;

        const recipientId = conversation.participantIds.find(
            (id) => id !== message.senderId
        );

        if (!recipientId) return null;

        const recipientSnap = await db
            .collection("users")
            .doc(recipientId)
            .get();

        const recipientData = recipientSnap.data();
        const fcmToken = recipientData ? recipientData.fcmToken : null;

        if (!fcmToken) return null;

        await db
            .collection("conversations")
            .doc(conversationId)
            .update({
                ["unreadCount." + recipientId]:
                    admin.firestore.FieldValue.increment(1),
            });

        const payload = {
            token: fcmToken,
            notification: {
                title: message.senderDisplayName,
                body: message.text,
            },
            data: {
                type: "message",
                conversationId: conversationId,
                senderId: message.senderId,
                senderDisplayName: message.senderDisplayName,
            },
            android: {
                priority: "high",
                notification: {
                    channelId: "findly_messages",
                },
            },
        };

        try {
            await admin.messaging().send(payload);
        } catch (error) {
            if (
                error.code === "messaging/invalid-registration-token" ||
                error.code === "messaging/registration-token-not-registered"
            ) {
                await db
                    .collection("users")
                    .doc(recipientId)
                    .update({ fcmToken: null });
            }
        }

        return null;
    });

// ─────────────────────────────────────────────
// TRIGGER 2: Smart match on new item posted
// ─────────────────────────────────────────────
exports.onNewItem = functions.region(REGION).firestore
    .document("items/{itemId}")
    .onCreate(async (snap, context) => {
        const newItem = snap.data();
        const newItemId = context.params.itemId;

        if (!newItem) return null;

        const oppositeType = newItem.type === "LOST" ? "FOUND" : "LOST";

        const candidatesSnap = await db
            .collection("items")
            .where("type", "==", oppositeType)
            .where("status", "==", "ACTIVE")
            .where("category", "==", newItem.category)
            .get();

        if (candidatesSnap.empty) return null;

        const newItemWords = tokenize(
            newItem.title + " " + newItem.description
        );

        const matches = candidatesSnap.docs
            .filter((doc) => doc.id !== newItemId)
            .map((doc) => {
                const candidate = doc.data();
                const candidateWords = tokenize(
                    candidate.title + " " + candidate.description
                );
                const score = jaccardSimilarity(newItemWords, candidateWords);
                return { candidate: candidate, candidateId: doc.id, score: score };
            })
            .filter((match) => match.score >= MATCH_THRESHOLD)
            .sort((a, b) => b.score - a.score)
            .slice(0, MAX_MATCHES);

        if (matches.length === 0) return null;

        const notifyPromises = [];

        for (const match of matches) {
            const candidate = match.candidate;
            const candidateId = match.candidateId;

            const candidateUserSnap = await db
                .collection("users")
                .doc(candidate.userId)
                .get();

            const candidateUserData = candidateUserSnap.data();
            const candidateFcmToken = candidateUserData
                ? candidateUserData.fcmToken
                : null;

            if (candidateFcmToken) {
                notifyPromises.push(
                    admin.messaging().send({
                        token: candidateFcmToken,
                        notification: {
                            title: "Potential Match Found!",
                            body: "A new " + newItem.type.toLowerCase() +
                                " item \"" + newItem.title +
                                "\" may match your " +
                                candidate.type.toLowerCase() +
                                " item \"" + candidate.title + "\"",
                        },
                        data: {
                            type: "match",
                            matchedItemId: newItemId,
                            matchedItemTitle: newItem.title,
                            yourItemId: candidateId,
                        },
                        android: {
                            priority: "high",
                            notification: {
                                channelId: "findly_matches",
                            },
                        },
                    }).catch((error) => {
                        if (
                            error.code === "messaging/invalid-registration-token" ||
                            error.code === "messaging/registration-token-not-registered"
                        ) {
                            return db
                                .collection("users")
                                .doc(candidate.userId)
                                .update({ fcmToken: null });
                        }
                    })
                );
            }

            const newItemUserSnap = await db
                .collection("users")
                .doc(newItem.userId)
                .get();

            const newItemUserData = newItemUserSnap.data();
            const newItemFcmToken = newItemUserData
                ? newItemUserData.fcmToken
                : null;

            if (newItemFcmToken) {
                notifyPromises.push(
                    admin.messaging().send({
                        token: newItemFcmToken,
                        notification: {
                            title: "Potential Match Found!",
                            body: "Your " + newItem.type.toLowerCase() +
                                " item \"" + newItem.title +
                                "\" may match a " +
                                candidate.type.toLowerCase() +
                                " item \"" + candidate.title + "\"",
                        },
                        data: {
                            type: "match",
                            matchedItemId: candidateId,
                            matchedItemTitle: candidate.title,
                            yourItemId: newItemId,
                        },
                        android: {
                            priority: "high",
                            notification: {
                                channelId: "findly_matches",
                            },
                        },
                    }).catch((error) => {
                        if (
                            error.code === "messaging/invalid-registration-token" ||
                            error.code === "messaging/registration-token-not-registered"
                        ) {
                            return db
                                .collection("users")
                                .doc(newItem.userId)
                                .update({ fcmToken: null });
                        }
                    })
                );
            }
        }

        await Promise.all(notifyPromises);
        return null;
    });

// ─────────────────────────────────────────────
// HELPERS
// ─────────────────────────────────────────────
const MATCH_THRESHOLD = 0.2;
const MAX_MATCHES = 5;
const STOP_WORDS = new Set([
    "a", "an", "the", "and", "or", "but", "in", "on",
    "at", "to", "for", "of", "with", "my", "i", "it",
    "is", "was", "lost", "found", "item",
]);

function tokenize(text) {
    return new Set(
        text
            .toLowerCase()
            .replace(/[^a-z0-9\s]/g, "")
            .split(/\s+/)
            .filter((word) => word.length > 1 && !STOP_WORDS.has(word))
    );
}

function jaccardSimilarity(setA, setB) {
    if (setA.size === 0 && setB.size === 0) return 0;
    const intersection = new Set([...setA].filter((x) => setB.has(x)));
        const union = new Set([...setA, ...setB]);
    return intersection.size / union.size;
}