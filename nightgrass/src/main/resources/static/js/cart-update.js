let eventSource;

// Function to initialize Server-Sent Events
function initializeCartUpdates() {
    if (!eventSource) {
        eventSource = new EventSource("/cart/updates");

        eventSource.addEventListener("cart-update", function (event) {
            const data = JSON.parse(event.data);
            console.log("Cart updated:", data);

            const cartCountElement = document.querySelector(".cart-count");
            if (cartCountElement) {
                cartCountElement.innerText = data; // Assuming the event sends the updated count as a number
            }
        });

        eventSource.addEventListener("heartbeat", function () {
            console.log("Heartbeat received");
        });

        eventSource.onerror = function (event) {
            console.error("SSE connection error:", event);
            console.error("Attempting to reconnect...");

            setTimeout(() => {
                eventSource.close();
                eventSource = new EventSource("/cart/updates");
            }, 5000);
        };

        eventSource.onopen = function () {
            console.log("SSE connection established");
        };
    }
}

// Initialize the cart updates logic on page load
document.addEventListener("DOMContentLoaded", initializeCartUpdates);
