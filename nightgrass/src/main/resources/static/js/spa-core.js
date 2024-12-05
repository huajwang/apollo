window.globalState = function () {
    return {
        cartCount: 0,
        eventSource: null,
        isSSEConnected: false, // Prevent duplicate SSE connections

        init() {
            console.log("Global state initialized");

            if (!this.isSSEConnected) {
                this.connectToSSE();
                this.isSSEConnected = true;
            }
        },

        connectToSSE() {
            if (this.eventSource) {
                this.eventSource.close();
            }

            this.eventSource = new EventSource("/cart/updates");
            console.log("SSE connection established.");

            this.eventSource.addEventListener("cart-update", (event) => {
                const data = JSON.parse(event.data);
                console.log("Cart updated:", data);
                this.cartCount = data;
                Alpine.store("cart").count = data; // Sync with Alpine.js store
            });

            this.eventSource.addEventListener("error", () => {
                console.error("SSE connection error, reconnecting...");
                this.isSSEConnected = false; // Reset flag on error
                setTimeout(() => this.connectToSSE(), 5000);
            });
        },

        navigate(url) {
            fetch(url, { headers: { "X-Requested-With": "XMLHttpRequest" } })
                .then(response => {
                    if (!response.ok) {
                        throw new Error("Failed to load content.");
                    }
                    return response.text();
                })
                .then(html => {
                    document.getElementById("pageContent").innerHTML = html;

                    if (window.Alpine) {
                        window.Alpine.initTree(document.getElementById("pageContent"));
                    }
                })
                .catch(error => {
                    console.error("Navigation error:", error);
                });
        }
    };
};

document.addEventListener("alpine:init", () => {
    Alpine.store("cart", {
        count: 0
    });
});
