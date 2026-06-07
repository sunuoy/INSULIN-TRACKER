<div align="center">
<img width="1200" height="475" alt="GHBanner" src="https://ai.google.dev/static/site-assets/images/share-ais-513315318.png" />
</div>
A smart, local mobile application for tracking insulin doses, blood glucose levels, reminders, and daily/weekly/monthly summaries.


mobile app for insulin tracking, where users can record insulin doses with dates and times, here are some name ideas and core features. Add Insulin Record Insulin type (Rapid, Long-acting, etc.) Dose (Units) Date Time Notes History View all insulin entries Search by date Filter by insulin type Reminders Morning insulin reminder Lunch insulin reminder Evening insulin reminder Reports Daily insulin totals Weekly summaries Monthly summaries Export PDF report Excel/CSV export Share with doctor Features User Management Login (optional) Local profile Doctor information Insulin Tracking Add insulin dose Edit dose Delete dose View history Blood Sugar Tracking Add glucose readings Fasting Before meal After meal Bedtime Reminders Insulin reminders Medication reminders Blood sugar check reminders Reports Daily summary Weekly summary Monthly summary PDF export Cloud Sync (Optional) ONDRIVE, GOOGLE DRIVE


1. Open Android Studio
2. Select **Open** and choose the directory containing this project
3. Allow Android Studio to fix any incompatibilities as it imports the project.
4. Create a file named `.env` in the project directory and set `GEMINI_API_KEY` in that file to your Gemini API key (see `.env.example` for an example)
5. Remove this line from the app's `build.gradle.kts` file: `signingConfig = signingConfigs.getByName("debugConfig")`
6. Run the app on an emulator or physical device
