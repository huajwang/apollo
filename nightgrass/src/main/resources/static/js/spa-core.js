window.globalState = function () {
    return {
        cartCount: 0,
        eventSource: null,
    };
};

document.addEventListener("alpine:init", () => {
    Alpine.store("cart", {
        count: null
    });

     // Fetch the current cart count from the server
     fetch("/cart/cart-item-count")
        .then((response) => response.json())
        .then((data) => {
            console.log("cartItemCount = ", data.cartItemCount);
            Alpine.store("cart").count = data.cartItemCount; // Update store with server value
        })
        .catch((error) => {
            console.error("Error fetching cart count:", error);
            Alpine.store("cart").count = 0; // Fallback
        });
});

document.addEventListener("DOMContentLoaded", function () {
    const menuToggle = document.querySelector('.menu-toggle');
    const navbar = document.querySelector('.navbar');

    // Toggle the menu when the toggle button is clicked
    menuToggle.addEventListener('click', function (event) {
        event.stopPropagation(); // Prevent click from propagating to document
        navbar.classList.toggle('open');
    });

    // Close the menu when clicking outside of it
    document.addEventListener('click', function (event) {
        // Check if the click is outside the navbar or menu toggle
        if (!navbar.contains(event.target) && !menuToggle.contains(event.target)) {
            navbar.classList.remove('open');
        }
    });
});


