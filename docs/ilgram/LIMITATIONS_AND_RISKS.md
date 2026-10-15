# Ma'lum cheklovlar va xavfsizlik xatarlari

## Asosiy tamoyil
ILGRAM cheklovlari **faqat shu ilova interfeysida** amal qiladi. Ular Telegram serveridagi huquqlarni
o'zgartirmaydi. Shuning uchun:

| Xatar | Izoh | Kim yopadi |
|---|---|---|
| O'quvchi shu akkaunt bilan boshqa qurilma/kompyuterda (Telegram Desktop, web.telegram.org) kiradi | U yerda hamma chatlar ko'rinadi | Maktab qoidasi; telefonda MDMesh boshqa messenjer va brauzerlarni bloklaydi. ILGRAM QR-login ("Link Desktop Device") ni o'chirgan. |
| Telegram xizmat xabarlari (777000) ko'rinadi | Login kodi ko'rinadi → o'quvchi boshqa qurilmada kira oladi | `allow_service_notifications=false` qilsa bo'ladi, lekin unda xavfsizlik ogohlantirishlari ham ko'rinmaydi. Tavsiya: `true`, va akkauntga 2FA parol qo'yish. |
| Begona odam ruxsatli guruhda yozadi | Guruh ichidagi xabarlar hamma uchun ko'rinadi (bu guruhning o'zi) | Guruh administratorlari (Telegram huquqlari saqlanadi) |
| Ruxsatsiz odam o'quvchiga shaxsiy xabar yozadi | Xabar serverga keladi, lekin ILGRAM'da ko'rinmaydi va bildirishnoma bermaydi | — (xabar o'chirilmaydi, soxtalashtirilmaydi) |
| O'quvchi ruxsatli guruh ichida inline bot (`@bot so'rov`) ishlatadi | Natija guruhga yuboriladi | Guruh admini "inline bot" huquqini o'chirishi mumkin |
| Guruh ichidagi xabarda tashqi havola | Matn sifatida ko'rinadi, bosilganda ochilmaydi | — |
| Video embed (YouTube va h.k.) | Web view bloklangani uchun ijro etilmaydi | Kutilgan cheklov |
| Instant View (telegra.ph maqolalar) | Bloklangan (tashqi havola) | Kerak bo'lsa `browser_enabled` alohida yechim talab qiladi |
| Maxfiy chat boshlash tugmasi (ruxsatli foydalanuvchi profilida) | Chat yaratiladi, lekin `allow_secret_chats=false` bo'lsa ILGRAM uni ochmaydi | Konfiguratsiya |
| Ovozli/video qo'ng'iroqlar | Ruxsatli foydalanuvchilar bilan ishlaydi; Qo'ng'iroqlar tabi yashirin | Prompt talab qilmagan; kerak bo'lsa keyingi bosqichda o'chirish mumkin |
| Guruh a'zolari ro'yxati | Ko'rinadi, lekin a'zo profili/shaxsiy chati ochilmaydi | NavigationGuard |

## Texnik cheklovlar
* **Push:** uchinchi tomon api_id uchun Telegram FCM push yubormaydi. ILGRAM foreground xizmat bilan ulanishni ushlaydi.
  Batareya optimizatsiyasi yoqilgan bo'lsa (ayniqsa Xiaomi/Redmi), xabarlar kechikishi mumkin — `MDMESH.md` §3.
  Android 15'da `dataSync` foreground xizmatlarga kunlik 6 soat limiti bor; shuning uchun Android 14+ da `remoteMessaging` turi ishlatiladi.
* **Konfiguratsiya:** prototipda ruxsatlar APK ichida (`ILGRAM_CONFIG` → build). MDMesh managed configuration'ni qo'llab-quvvatlamaguncha
  har bir o'zgarish yangi APK talab qiladi.
* **Bir nechta akkaunt:** UI yashirilgan. Agar qurilmada oldin bir nechta akkaunt bo'lgan bo'lsa (ILGRAM'dan oldin), ular ma'lumotlari saqlanadi,
  lekin almashtirish imkoni yo'q.
* **Til paketlari:** Telegram server tarjimalarida ba'zi joylarda "Telegram" so'zi qolishi mumkin (launcher va asosiy joylarda "ILGRAM").
* **Bildirishnoma ID'si:** Telegram tizim akkaunt turi (`org.telegram.messenger`) saqlangan; qurilmada rasmiy Telegram ham o'rnatilgan bo'lsa,
  kontakt sinxronizatsiyasi akkaunti bittasida ishlaydi (ILGRAM kontaktlarni umuman sinxronlamaydi).
* **Tablet layout:** asosan telefonlar uchun tekshirilgan.

## Tekshirilmagan (real qurilma va real akkaunt talab qiladi)
`TEST_RESULTS.md` dagi "Real qurilmada tekshirilishi kerak" bo'limiga qarang.

## Xavfsizlik amaliyotlari
* Imzolash kaliti GitHub secretlarida va `D:\python\ILGRAM_KEYS` da; repozitoriyda yo'q.
* api_id/api_hash va ruxsatlar ro'yxati secretlarda; ochiq kodda yo'q.
* `NotificationsService` eksport qilinmagan (`exported=false`) — boshqa ilovalar uni ishga tushira olmaydi.
* Debug menyu (versiya ustiga bosish) o'quvchi rejimida o'chirilgan.
* Ilova ruxsatlari Telegram'niki bilan bir xil; qo'shimcha faqat `FOREGROUND_SERVICE_REMOTE_MESSAGING`.
