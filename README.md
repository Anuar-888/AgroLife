# AgroLife Project

AgroLife is an intelligent assistant for agronomists, designed to manage fields and optimize crop yields in Kazakhstan.

## Project Structure

- `app/`: Android application source code (Java/XML).
- `backend/`: Node.js/Express API for data synchronization and expert logic.
- `agrolife-website/`: Responsive web prototype for farmers and agricultural businesses.

## Key Features

- **Field Management**: Swipeable interface to track multiple fields.
- **Smart Irrigation**: Automated calculation of water volume based on field area and growth stage.
- **Expert Advice**: Fertilization schedules (NPK vs. Organic) tailored for local crops like raspberries and garlic.
- **Personalized Greeting**: Dynamic welcome screen and user profiles.

## Backend Setup

1. Go to `backend/`.
2. Run `npm install`.
3. Run `npm start`.
4. Default API is at `http://localhost:3000`.

## Android Setup

1. Open the project in Android Studio.
2. Build and run the `app` module.

## Website Setup

1. Go to `agrolife-website/`.
2. Run `npm test` to check the planning logic.
3. Run `npm run serve` and open `http://localhost:4173/demo.html`.

The web prototype currently supports raspberries, strawberries, and currants. A farmer selects crops, records the planting date for each field, and receives a work plan for watering, fertilizing, inspections, and harvest preparation.
