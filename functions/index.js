const express = require("express");
const admin = require("firebase-admin");

// Initialize Firebase Admin (Uses default credentials when deployed on Render/Vercel with env variable GOOGLE_APPLICATION_CREDENTIALS, or manual parsing)
admin.initializeApp();
const db = admin.firestore();

const app = express();
app.use(express.json());

// Basic health check
app.get("/", (req, res) => res.send("Campus Taxi Pooling Notification API is running."));

/**
 * 1: Notify ride poster when a seat request is created.
 * Body requires: { rideId, requesterName, posterUid, status: "PENDING" }
 */
app.post("/notifyJoinRequest", async (req, res) => {
  try {
    const { rideId, requesterName, posterUid, status } = req.body;
    if (status !== "PENDING") return res.status(200).send("Not pending.");

    // Get ride poster FCM token
    const posterDoc = await db.collection("users").doc(posterUid).get();
    if (!posterDoc.exists) return res.status(404).send("Poster not found.");
    const fcmToken = posterDoc.data().fcmToken;

    if (!fcmToken) return res.status(200).send("No FCM token.");

    const rideDoc = await db.collection("rides").doc(rideId).get();
    const ride = rideDoc.exists ? rideDoc.data() : { source: "Source", destination: "Destination" };

    const payload = {
      data: {
        type: "JOIN_REQUEST",
        rideId: rideId,
        senderName: requesterName || "Someone",
        rideRoute: `${ride.source} → ${ride.destination}`
      }
    };

    await admin.messaging().sendToDevice(fcmToken, payload);
    res.status(200).send("Notification sent.");
  } catch (error) {
    console.error(error);
    res.status(500).send("Error sending notification.");
  }
});

/**
 * 2: Notify joiner when their request is updated to ACCEPTED or REJECTED.
 * Body requires: { rideId, status, joinerUid }
 */
app.post("/notifyRequestUpdate", async (req, res) => {
  try {
    const { rideId, status, joinerUid } = req.body;

    if (status !== "ACCEPTED" && status !== "REJECTED") return res.status(200).send("No notification for this status.");

    // Get joiner FCM token
    const joinerDoc = await db.collection("users").doc(joinerUid).get();
    if (!joinerDoc.exists) return res.status(404).send("Joiner not found");
    const fcmToken = joinerDoc.data().fcmToken;

    if (!fcmToken) return res.status(200).send("No FCM token.");

    const rideDoc = await db.collection("rides").doc(rideId).get();
    const ride = rideDoc.exists ? rideDoc.data() : { source: "Ride", destination: "", postedByName: "Poster" };
    const route = `${ride.source} → ${ride.destination}`;

    let payload = {};

    if (status === "ACCEPTED") {
      const connections = await db.collection("connections")
        .where("rideId", "==", rideId)
        .where("participants", "array-contains", joinerUid)
        .limit(1).get();
      
      const connectionId = !connections.empty ? connections.docs[0].id : "";

      payload = {
        data: {
          type: "REQUEST_ACCEPTED",
          rideId: rideId,
          connectionId: connectionId,
          senderName: ride.postedByName || "Driver",
          rideRoute: route
        }
      };
    } else {
      payload = {
        data: {
          type: "REQUEST_REJECTED",
          rideId: rideId,
          rideRoute: route
        }
      };
    }

    await admin.messaging().sendToDevice(fcmToken, payload);
    res.status(200).send("Notification sent.");
  } catch (error) {
    console.error(error);
    res.status(500).send("Error sending notification.");
  }
});

/**
 * 3: Notify other participant when a chat message is sent.
 * Body requires: { connectionId, senderUid, senderName, text, isBlocked }
 */
app.post("/notifyChatMessage", async (req, res) => {
  try {
    const { connectionId, senderUid, senderName, text, isBlocked } = req.body;
    if (isBlocked) return res.status(200).send("Message is blocked");

    // Get connection to find receiver
    const connDoc = await db.collection("connections").doc(connectionId).get();
    if (!connDoc.exists) return res.status(404).send("Connection not found");
    
    const participants = connDoc.data().participants || [];
    const receiverUid = participants.find(uid => uid !== senderUid);
    if (!receiverUid) return res.status(200).send("No receiver found.");

    // Get receiver FCM token
    const rxDoc = await db.collection("users").doc(receiverUid).get();
    if (!rxDoc.exists) return res.status(404).send("Receiver not found");
    const fcmToken = rxDoc.data().fcmToken;

    if (!fcmToken) return res.status(200).send("No FCM Token");

    const payload = {
      data: {
        type: "CHAT_MESSAGE",
        connectionId: connectionId,
        senderName: senderName || "Ride Partner",
        messagePreview: text || "New message"
      }
    };

    await admin.messaging().sendToDevice(fcmToken, payload);
    res.status(200).send("Notification sent.");
  } catch (error) {
    console.error(error);
    res.status(500).send("Error sending notification.");
  }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`Server listening on port ${PORT}`);
});
