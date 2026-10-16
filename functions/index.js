const functions = require("firebase-functions");
const admin = require("firebase-admin");

admin.initializeApp();

/**
 * Cloud Function que se dispara automáticamente cada vez que alguien
 * crea un nuevo mensaje en Firestore en la colección 'mensajes_comunidad'.
 * Envía un Push FCM de alta prioridad a todos los teléfonos suscritos a 'chat_comunidad'.
 */
exports.sendChatPushNotification = functions.firestore
  .document("mensajes_comunidad/{messageId}")
  .onCreate(async (snapshot, context) => {
    try {
      const messageData = snapshot.data();
      if (!messageData) {
        console.log("No data in message snapshot");
        return null;
      }

      if (messageData.eliminado || !messageData.texto) {
        return null;
      }

      const senderId = messageData.remitenteId || "";
      const senderName = messageData.remitenteNombre || "Usuario";
      const senderRole = messageData.remitenteRol || "CIUDADANO";
      const messageText = messageData.texto || "";

      console.log(`Nuevo mensaje de ${senderName} (${senderRole}): ${messageText}`);

      const payload = {
        topic: "chat_comunidad",
        data: {
          senderId: String(senderId),
          senderName: String(senderName),
          senderRole: String(senderRole),
          message: String(messageText),
          timestamp: String(Date.now())
        },
        android: {
          priority: "high",
          notification: {
            title: senderName,
            body: messageText,
            channelId: "channel_community_chat",
            sound: "default"
          }
        }
      };

      const response = await admin.messaging().send(payload);
      console.log("Notificación push FCM enviada con éxito:", response);
      return response;
    } catch (error) {
      console.error("Error al enviar notificación push de chat:", error);
      return null;
    }
  });
