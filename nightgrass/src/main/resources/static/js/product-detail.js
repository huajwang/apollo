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
                })
                .catch((error) => {
                    // Revert the optimistic update if there's an error
                    Alpine.store("cart").count--;
                    alert('Error while adding product to cart. Please try again later. Error: ', error);
                });
        });

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

    // Initialize carousel
    const track = document.querySelector(".carousel-track");
    const prevBtn = document.getElementById("prevBtn");
    const nextBtn = document.getElementById("nextBtn");
    const slides = Array.from(track.children); // Carousel slides
    const slideWidth = slides[0].getBoundingClientRect().width; // Width of one slide
    let currentSlide = 0;

    slides.forEach((slide, index) => {
        slide.style.left = `${slideWidth * index}px`;
    });

    const moveToSlide = (currentSlideIndex) => {
        track.style.transform = `translateX(-${currentSlideIndex * slideWidth}px)`;
    };

    nextBtn.addEventListener("click", () => {
        if (currentSlide < slides.length - 1) {
            currentSlide++;
            moveToSlide(currentSlide);
        }
    });

    prevBtn.addEventListener("click", () => {
        if (currentSlide > 0) {
            currentSlide--;
            moveToSlide(currentSlide);
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

            const hiddenInput = document.getElementById(`selected-${property}`);
            if (hiddenInput) {
                hiddenInput.value = value;
            }
        });
    });

});
