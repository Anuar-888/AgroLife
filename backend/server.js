const express = require('express');
const cors = require('cors');
const bodyParser = require('body-parser');
require('dotenv').config();

const app = express();
const PORT = process.env.PORT || 3000;

app.use(cors());
app.use(bodyParser.json());

// Mock Data
const crops = [
    { id: "raspberry", label: "Малина", icon: "🍓", category: "berries" },
    { id: "garlic", label: "Чеснок", icon: "🧄", category: "vegetables" },
    { id: "strawberry", label: "Клубника", icon: "🍓", category: "berries" },
    { id: "tomato", label: "Помидор", icon: "🍅", category: "vegetables" }
];

let userFields = [
    { id: "1", name: "Поле №1 (Малина)", cropId: "raspberry", areaHectares: 2.5, plantDateMillis: Date.now() - 86400000 * 30 },
    { id: "2", name: "Поле №2 (Чеснок)", cropId: "garlic", areaHectares: 1.2, plantDateMillis: Date.now() - 86400000 * 10 }
];

// Routes
app.get('/api/crops', (req, res) => {
    res.json(crops);
});

app.get('/api/fields', (req, res) => {
    res.json(userFields);
});

app.post('/api/fields', (req, res) => {
    const newField = {
        id: (userFields.length + 1).toString(),
        ...req.body,
        plantDateMillis: Date.now()
    };
    userFields.push(newField);
    res.status(201).json(newField);
});

app.get('/api/health', (req, res) => {
    res.json({ status: "ok", app: "AgroLife Backend" });
});

app.listen(PORT, () => {
    console.log(`AgroLife Backend server running on http://localhost:${PORT}`);
});
