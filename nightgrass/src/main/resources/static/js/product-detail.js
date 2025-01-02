document.addEventListener("DOMContentLoaded", function () {

    const addToCartForm = document.getElementById("add-to-cart-form");

    addToCartForm.addEventListener("submit", function (event) {
        event.preventDefault(); // Prevent default form submission

        const formData = new FormData(addToCartForm);
        const payload = {};

        // Convert form data to JSON
        formData.forEach((value, key) => {
            if (key.startsWith("properties[")) {
                const propertyKey = key.match(/properties\[(.+)\]/)[1];
                payload.properties = payload.properties || {};
                payload.properties[propertyKey] = value;
            } else {
                payload[key] = value;
            }
        });

        // Optimistically update the cart count
        Alpine.store("cart").count++;

        // Send POST request to add item to the cart
        fetch("/cart/add", {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
            },
            body: JSON.stringify(payload),
        })
        .then((response) => {
            if (response.ok) {
                console.log("Product added to cart successfully.");
                return response.json(); // Expect server response to contain the latest cart count
            } else {
                console.error("Failed to add product to cart.");
                throw new Error("Failed to add product to cart");
            }
        })
        .then((data) => {
            // Update the cart count with the server-provided value
            Alpine.store("cart").count = data.cartItemCount;
            // Trigger the Fly-to-Cart animation
            triggerFlyToCartAnimation(addToCartForm.querySelector('.add-to-cart'));
        })
        .catch((error) => {
            // Revert the optimistic update if there's an error
            Alpine.store("cart").count--;
            alert('Error while adding product to cart. Please try again later. Error: ', error);
        });
    });

    function triggerFlyToCartAnimation(button) {
        const cartIcon = document.querySelector('.cart-icon');
        const rect = button.getBoundingClientRect();

        // Access the reusable animation element
        const flyingElement = document.getElementById('fly-animation');
        flyingElement.style.display = 'block'; // Make it visible
        flyingElement.style.top = `${rect.top}px`;
        flyingElement.style.left = `${rect.left}px`;
        flyingElement.style.width = '10px'; // Use a consistent size
        flyingElement.style.height = '10px'; // Customize as needed
        flyingElement.style.backgroundColor = '#ff6347'; // Tomato color or use a product image
        flyingElement.style.borderRadius = '50%'; // Make it circular
        flyingElement.style.transition = 'all 0.7s ease-in-out';
        flyingElement.style.zIndex = '1000';
        //flyingElement.style.backgroundImage = `url('/images/lake.jpg')`;

        // Calculate cart position
        const cartRect = cartIcon.getBoundingClientRect();
        const xOffset = cartRect.left + cartRect.width / 2 - rect.left - 10; // Center the "flying" element
        const yOffset = cartRect.top + cartRect.height / 2 - rect.top - 10;

        // Start animation
        setTimeout(() => {
            flyingElement.style.transform = `translate(${xOffset}px, ${yOffset}px)`;
            flyingElement.style.opacity = '0.5';
        }, 50);

        // Cleanup after animation
        setTimeout(() => {
            flyingElement.style.display = 'none'; // Hide it after animation
            flyingElement.style.transform = 'none'; // Reset position for the next animation
            flyingElement.style.opacity = '1'; // Reset opacity
            // Optional: Highlight the cart icon
            highlightCartIcon();
        }, 750);
    }

    // Handle video modal interactions
    const videoLinks = document.querySelectorAll(".video-icon a");
    const modal = document.getElementById("videoModal");
    const closeModal = document.getElementById("closeModal");
    const videoPlayer = document.getElementById("videoPlayer");
    const videoSource = document.getElementById("videoSource");

    videoLinks.forEach(link => {
        link.addEventListener("click", function (event) {
            event.preventDefault();
            const videoUrl = link.getAttribute("href");
            videoSource.src = videoUrl;
            videoPlayer.load();
            modal.style.display = "block";
        });
    });

    closeModal.addEventListener("click", function () {
        modal.style.display = "none";
        videoPlayer.pause();
    });

    window.addEventListener("click", function (event) {
        if (event.target === modal) {
            modal.style.display = "none";
            videoPlayer.pause();
        }
    });


    // Handle product option selection
    document.querySelectorAll(".option-button").forEach(button => {
        button.addEventListener("click", function () {
            const property = button.dataset.property;
            const value = button.dataset.value;

            document.querySelectorAll(`.option-button[data-property="${property}"]`).forEach(btn => {
                btn.classList.remove("selected");
            });

            button.classList.add("selected");

            // Update "Add to Cart" hidden input
            const addToCartHiddenInput = document.getElementById(`selected-${property}`);
            if (addToCartHiddenInput) {
                addToCartHiddenInput.value = value;
            }

            // Update "Buy Now" hidden input
            const buyNowHiddenInput = document.getElementById(`buy-now-selected-${property}`);
            if (buyNowHiddenInput) {
                buyNowHiddenInput.value = value;
            }
        });
    });

});

function openTab(event, tabId) {
    // Hide all tab content
    const contents = document.querySelectorAll('.tab-content');
    contents.forEach((content) => {
      content.classList.remove('active');
    });

    // Remove active class from all tab links
    const links = document.querySelectorAll('.tab-link');
    links.forEach((link) => {
      link.classList.remove('active');
    });

    // Show the clicked tab content and mark the tab as active
    document.getElementById(tabId).classList.add('active');
    event.currentTarget.classList.add('active');
}


const swiper = new Swiper('.swiper', {
  loop: true, // Optional: Enable infinite looping
  slidesPerView: 1, // One slide visible at a time
  spaceBetween: 10, // Optional: Space between slides
  pagination: {
    el: '.swiper-pagination',
    clickable: true, // Enables clickable pagination dots
  },
});
