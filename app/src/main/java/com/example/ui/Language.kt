package com.example.ui

object StringsDict {
    fun appTitle(lang: String): String = if (lang == "bn") "গুগল অ্যাডস রিসেট ফ্লোটিং" else "Google Ads Reset Floating"
    fun floatingActive(lang: String): String = if (lang == "bn") "ফ্লোটিং বাটন সক্রিয় রয়েছে" else "Floating Bubble Active"
    fun floatingInactive(lang: String): String = if (lang == "bn") "ফ্লোটিং বাটন বন্ধ আছে" else "Floating Bubble Inactive"
    fun startFloating(lang: String): String = if (lang == "bn") "ফ্লোটিং বাটন চালু করুন" else "Start Floating Bubble"
    fun stopFloating(lang: String): String = if (lang == "bn") "ফ্লোটিং বাটন বন্ধ করুন" else "Stop Floating Bubble"

    fun quickResetTitle(lang: String): String = if (lang == "bn") "সরাসরি গুগল অ্যাডস রিসেট" else "One-Tap Ads Reset"
    fun quickResetDesc(lang: String): String = if (lang == "bn")
        "১ ক্লিকে গুগল প্লে সার্ভিসের অ্যাডস সেটিংস পেজ খুলুন এবং রিসেট বা ডিলিট করুন।"
    else
        "Instantly launch Google Play Services Ads page to reset or delete your advertising ID."

    fun openAdsNow(lang: String): String = if (lang == "bn") "গুগল অ্যাডস পেজ খুলুন" else "Open Google Ads Settings"
    fun privacySandbox(lang: String): String = if (lang == "bn") "প্রাইভেসি স্যান্ডবক্স" else "Privacy Sandbox"
    fun playServicesInfo(lang: String): String = if (lang == "bn") "প্লে সার্ভিসেস ইনফো" else "Play Services Info"

    fun permRequiredTitle(lang: String): String = if (lang == "bn") "ফ্লোটিং পারমিশন প্রয়োজন" else "Overlay Permission Needed"
    fun permRequiredDesc(lang: String): String = if (lang == "bn")
        "স্ক্রিনের যেকোনো অ্যাপের উপর ফ্লোটিং বাটন দেখানোর জন্য 'Display over other apps' পারমিশন চালু করুন।"
    else
        "Enable 'Display over other apps' to keep the reset bubble visible over any app or game."

    fun grantPerm(lang: String): String = if (lang == "bn") "পারমিশন চালু করুন" else "Grant Permission"

    fun currentAaidTitle(lang: String): String = if (lang == "bn") "বর্তমান অ্যাডভার্টাইজিং আইডি (AAID)" else "Current Advertising ID (AAID)"
    fun copyId(lang: String): String = if (lang == "bn") "আইডি কপি করুন" else "Copy AAID"
    fun refreshId(lang: String): String = if (lang == "bn") "রিফ্রেশ" else "Refresh"
    fun statusNormal(lang: String): String = if (lang == "bn") "স্ট্যাটাস: সক্রিয় ট্র্যাকিং আইডি" else "Status: Active Tracking ID"
    fun statusZeroed(lang: String): String = if (lang == "bn") "স্ট্যাটাস: অ্যাড আইডি ডিলিট করা আছে (জিরো আইডি)" else "Status: Ad ID Deleted (Zeroed Out)"
    fun limitTrackingOn(lang: String): String = if (lang == "bn") "পার্সোনালাইজড অ্যাড বন্ধ আছে" else "Personalized Ads Opted Out"

    fun customizationTitle(lang: String): String = if (lang == "bn") "ফ্লোটিং বাটন কাস্টমাইজেশন" else "Floating Bubble Settings"
    fun bubbleSize(lang: String): String = if (lang == "bn") "বাটনের সাইজ" else "Bubble Size"
    fun bubbleAlpha(lang: String): String = if (lang == "bn") "স্বচ্ছতা (Opacity)" else "Opacity / Transparency"
    fun clickMode(lang: String): String = if (lang == "bn") "ক্লিক অ্যাকশন মোড" else "Tap Action Mode"
    fun clickModeDirect(lang: String): String = if (lang == "bn") "সরাসরি রিসেট ওপেন (১ ট্যাপ)" else "Direct Reset (1-Tap Open)"
    fun clickModeMenu(lang: String): String = if (lang == "bn") "কুইক মেনু পপআপ" else "Quick Controls Menu"
    fun snapEdges(lang: String): String = if (lang == "bn") "স্ক্রিনের পাশে স্বয়ংক্রিয়ভাবে স্ন্যাপ করুন" else "Auto-snap to screen edges"
    fun vibrateOnTap(lang: String): String = if (lang == "bn") "ট্যাপে ভাইব্রেশন" else "Vibrate on tap"
    fun bubbleColor(lang: String): String = if (lang == "bn") "বাটনের রঙ" else "Bubble Color"

    fun guideTitle(lang: String): String = if (lang == "bn") "কীভাবে রিসেট করবেন?" else "How to Reset Ads?"
    fun guideStep1(lang: String): String = if (lang == "bn")
        "১. ফ্লোটিং বাটন চালু করুন, এটি স্ক্রিনের উপরে ভেসে থাকবে।"
    else
        "1. Start the Floating Bubble, which stays on top of any app."
    fun guideStep2(lang: String): String = if (lang == "bn")
        "২. বাটনটিতে ট্যাপ করলেই গুগল প্লে সার্ভিসের 'অ্যাডস' পেজ সরাসরি ওপেন হবে।"
    else
        "2. Tap the floating bubble to instantly open the Google Ads page."
    fun guideStep3(lang: String): String = if (lang == "bn")
        "৩. সেখানে 'Reset advertising ID' বা 'Delete advertising ID' তে প্রেস করুন।"
    else
        "3. Tap 'Reset advertising ID' or 'Delete advertising ID'."
    fun guideStep4(lang: String): String = if (lang == "bn")
        "৪. অ্যাপে ফিরে এসে 'রিফ্রেশ' চাপলে দেখতে পাবেন নতুন আইডি তৈরি হয়েছে!"
    else
        "4. Return here and tap 'Refresh' to verify your new Advertising ID!"

    fun historyTitle(lang: String): String = if (lang == "bn") "রিসেট হিস্টোরি ও লগ" else "Reset History & Log"
    fun totalResets(lang: String, count: Int): String = if (lang == "bn") "মোট রিসেট ট্রিগার: $count বার" else "Total resets triggered: $count"
    fun clearHistory(lang: String): String = if (lang == "bn") "হিস্টোরি মুছুন" else "Clear History"
    fun noHistory(lang: String): String = if (lang == "bn") "এখনও কোনো হিস্টোরি নেই" else "No reset activity yet"
}
