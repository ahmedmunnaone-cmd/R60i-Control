# R60i Control

Soundcore **R60i NC (D1202C)** এর জন্য কাস্টম Android অ্যাপ। [OpenSCQ30](https://github.com/Oppzippy/OpenSCQ30) v2.13.0 এর উপর বানানো
(প্রোটোকল/Rust কোর অপরিবর্তিত, শুধু Android UI নতুন)। লাইসেন্স: GPL-3.0।

## যা আছে
- **ডার্ক AMOLED + নিয়ন গ্রিন UI**, সব জায়গায় অ্যানিমেশন
- **হোমস্ক্রিনে সব কন্ট্রোল**: ব্যাটারি রিং (L/R/কেস), ANC / Transparency / Normal, ANC লেভেল, উইন্ড নয়েজ,
  রিয়েল-টাইম অ্যাডাপটিভ ANC, গেমিং/স্পেশাল টগল, টাচ জেসচার, অটো পাওয়ার অফ, LDAC, ডুয়াল কানেকশন ইত্যাদি
- **Bass+ EQ**: লো-ফ্রিকোয়েন্সি বুস্টেড প্রোফাইল, কানেক্ট হলে অটো-অ্যাপ্লাই (বন্ধ করা যায়)
- **EQ Studio**: লাইভ কার্ভ, Bass boost স্লাইডার, বেস প্রোফাইল, সব প্রিসেট, কাস্টম প্রোফাইল সেভ/ডিলিট, ব্যান্ড-বাই-ব্যান্ড টিউনিং
- Quick presets, স্ট্যাটাস নোটিফিকেশন, ইম্পোর্ট/এক্সপোর্ট (All settings থেকে)

## GitHub থেকে APK বানানো
1. GitHub-এ নতুন repository খুলুন, এই ফোল্ডারের **ভেতরের সব ফাইল** (`.github` ফোল্ডারসহ) আপলোড/পুশ করুন
   (জিপ আনজিপ করে ভেতরের কন্টেন্ট repo root-এ রাখতে হবে)।
2. **Actions** ট্যাব → **Build R60i Control APK** → **Run workflow** (main/master-এ পুশ করলেও অটো চলে)।
3. ~১৫–৩০ মিনিট পর run-এর নিচে **Artifacts → R60i-Control-apk** থেকে `R60i-Control.apk` নামান।
4. ফোনে ইনস্টল করুন, আগে ইয়ারবাড Bluetooth-এ পেয়ার করে নিন, তারপর অ্যাপে **Add device → R60i NC** বেছে কানেক্ট করুন।

> APK শুধু **arm64** (প্রায় সব আধুনিক ফোন)। অন্য আর্কিটেকচার লাগলে workflow-তে `arm64-v8a` বদলান
> (`./gradlew assembleArmeabi-v7aRelease` ইত্যাদি)।

### আপডেটের সময় একই সাইনিং কী রাখতে চাইলে (ঐচ্ছিক)
ডিফল্টে প্রতি বিল্ডে নতুন কী তৈরি হয়, তাই পুরনো ভার্সন আনইনস্টল করে নতুনটা ইনস্টল করতে হতে পারে।
একই কী রাখতে repo **Settings → Secrets → Actions**-এ দিন: `KEYSTORE_BASE64` (`base64 -w0 my.jks`) ও `KEYSTORE_PASSWORD`
(key alias অবশ্যই `r60i` হতে হবে)।

## কোথায় কী বদলেছে
- `android/app/src/main/java/.../ui/devicesettings/screens/HomeDashboardScreen.kt` — হোম ড্যাশবোর্ড
- `.../screens/EqualizerStudioScreen.kt` — EQ Studio ও Bass profile মান (`BASS_PROFILES`; ইউনিট = ০.১ dB)
- `.../ui/theme/Color.kt`, `Theme.kt` — AMOLED গ্রিন থিম
- `.../devicesettings/DeviceSettingsScreen.kt` — নেভিগেশন
- `build.gradle.kts` — applicationId `com.r60i.control`
- `.github/workflows/build-r60i-apk.yml` — APK বিল্ড (পুরনো workflow গুলো `.github/workflows-disabled/`-এ)

## সতর্কতা
এই কোড এমন পরিবেশে লেখা যেখানে Android SDK/Rust চালিয়ে কম্পাইল করা যায়নি, তাই প্রথম বিল্ডে ছোটখাটো
কম্পাইল এরর আসতে পারে। Actions লগের এরর লাইনগুলো পাঠালে ঠিক করা যাবে।
