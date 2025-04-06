// Dummy sentiment analysis function
function analyzeSentiment(message) {
    return message.toLowerCase().includes("happy") ? "positive" : "neutral";
}

module.exports = { analyzeSentiment };
